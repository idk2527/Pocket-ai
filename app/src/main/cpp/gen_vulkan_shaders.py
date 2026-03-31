import os
import sys
import subprocess
import argparse

type_names = [
    "f32", "f16", "q4_0", "q4_1", "q5_0", "q5_1", "q8_0", "q2_k", 
    "q3_k", "q4_k", "q5_k", "q6_k", "iq1_s", "iq1_m", "iq2_xxs", 
    "iq2_xs", "iq2_s", "iq3_xxs", "iq3_s", "iq4_xs", "iq4_nl", "mxfp4", "bf16"
]

def to_uppercase(s): return s.upper()

def is_quantized_type(t): return t not in ["f32", "f16", "bf16"]
def is_legacy_quant(t): return t in ["q4_0", "q4_1", "q5_0", "q5_1", "q8_0"]
def is_k_quant(t): return t.endswith("_k")
def is_iq_quant(t): return t.startswith("iq")

shader_fnames = []

def string_to_spv(name, source, defines, fp16=True, coopmat=False, coopmat2=False, f16acc=False, args=None, no_fp32_suffix=False):
    name = name + ("_f16acc" if f16acc else "") + ("_cm1" if coopmat else "") + ("_cm2" if coopmat2 else ("" if fp16 or no_fp32_suffix else "_fp32"))
    out_path = os.path.join(args.output_dir, name + ".spv")

    if not args.source:
        shader_fnames.append((name, out_path))
        return
    elif os.path.basename(args.source) != source:
        return

    real_defines = dict(defines)
    if "FLOAT16" not in real_defines and fp16:
        real_defines["FLOAT16"] = "1"

    target_env = "--target-env=vulkan1.3" if "_cm2" in name else "--target-env=vulkan1.2"
    cmd = [args.glslc, "-fshader-stage=compute", target_env, args.source, "-o", out_path]
    if not (coopmat or "bf16" in name or "rope" in name):
        cmd.append("-O")
        
    for k, v in real_defines.items():
        cmd.append(f"-D{k}={v}")

    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error compiling {name}:\n{result.stderr}")
        sys.exit(1)
        
    shader_fnames.append((name, out_path))

def float_type_func(n, tname, fp16=True, coopmat=False, coopmat2=False):
    if tname == "bf16":
        if not (coopmat or coopmat2):
            return "float" if n == 1 else f"vec{n}" if n <= 4 else "mat2x4"
        return "bfloat16_t" if n == 1 else f"bf16vec{n}"
    
    if coopmat2 or fp16:
        return "float16_t" if n == 1 else f"f16vec{n}" if n <= 4 else "f16mat2x4"
    
    return f"float" if n == 1 else f"vec{n}" if n <= 4 else "mat2x4"

def matmul_shaders(fp16, matmul_id_type, coopmat, coopmat2, f16acc, args, base_dict):
    load_vec = "1" if coopmat2 else ("8" if fp16 else "4")
    aligned_b_type_f32 = "float" if coopmat2 else ("mat2x4" if fp16 else "vec4")
    aligned_b_type_f16 = "float16_t" if coopmat2 else ("f16mat2x4" if fp16 else "f16vec4")
    
    defines = dict(base_dict)
    shader_name = "matmul"
    
    if matmul_id_type == "DEFAULT":
        defines["MUL_MAT_ID"] = "1"
        shader_name = "matmul_id"
    elif matmul_id_type == "SUBGROUP":
        defines["MUL_MAT_ID"] = "1"
        defines["MUL_MAT_ID_USE_SUBGROUPS"] = "1"
        shader_name = "matmul_id_subgroup"
        
    defines["ACC_TYPE"] = "float16_t" if f16acc else "float"
    defines["ACC_TYPE_VEC2"] = "f16vec2" if f16acc else "vec2"
    if f16acc: defines["ACC_TYPE_MAX"] = "float16_t(65504.0)"
    if coopmat: defines["COOPMAT"] = "1"
    if coopmat2: defines["COOPMAT2"] = "1"
    
    source_name = "mul_mm_cm2.comp" if coopmat2 else "mul_mm.comp"
    
    # Standalone f16 variants (A=f32 or f16, B=f16)
    ft_f16 = {
        "FLOAT_TYPE":      float_type_func(1, "f16", fp16, coopmat, coopmat2),
        "FLOAT_TYPE_VEC2": float_type_func(2, "f16", fp16, coopmat, coopmat2),
        "FLOAT_TYPE_VEC4": float_type_func(4, "f16", fp16, coopmat, coopmat2),
        "FLOAT_TYPE_VEC8": float_type_func(8, "f16", fp16, coopmat, coopmat2),
    }
    abt_f16 = "float16_t" if coopmat2 else ("f16mat2x4" if fp16 else "f16vec4")
    
    # A=f32, B=f16 shaders
    string_to_spv(shader_name + "_f32_f16",         source_name, {**defines, **ft_f16, "DATA_A_F32": "1", "B_TYPE": "float16_t", "D_TYPE": "float"}, fp16, coopmat, coopmat2, f16acc, args)
    string_to_spv(shader_name + "_f32_f16_aligned", source_name, {**defines, **ft_f16, "DATA_A_F32": "1", "LOAD_VEC_A": load_vec, "LOAD_VEC_B": load_vec, "B_TYPE": abt_f16, "D_TYPE": "float", "ALIGNED": "1"}, fp16, coopmat, coopmat2, f16acc, args)

    # A=f16, B=f16 shaders
    string_to_spv(shader_name + "_f16",             source_name, {**defines, **ft_f16, "DATA_A_F16": "1", "B_TYPE": "float16_t", "D_TYPE": "float"}, fp16, coopmat, coopmat2, f16acc, args)
    string_to_spv(shader_name + "_f16_aligned",     source_name, {**defines, **ft_f16, "DATA_A_F16": "1", "LOAD_VEC_A": load_vec, "LOAD_VEC_B": load_vec, "B_TYPE": abt_f16, "D_TYPE": "float", "ALIGNED": "1"}, fp16, coopmat, coopmat2, f16acc, args)

    # Standalone f32 variants (A=f32 or f16, B=f32)
    if not coopmat2:
        ft_f32 = {
            "FLOAT_TYPE":      float_type_func(1, "f32", fp16, coopmat, coopmat2),
            "FLOAT_TYPE_VEC2": float_type_func(2, "f32", fp16, coopmat, coopmat2),
            "FLOAT_TYPE_VEC4": float_type_func(4, "f32", fp16, coopmat, coopmat2),
            "FLOAT_TYPE_VEC8": float_type_func(8, "f32", fp16, coopmat, coopmat2),
        }
        
        # A=f32, B=f32 shaders
        string_to_spv(shader_name + "_f32_f32",         source_name, {**defines, **ft_f32, "DATA_A_F32": "1", "B_TYPE": "float", "D_TYPE": "float"}, fp16, coopmat, coopmat2, f16acc, args)
        string_to_spv(shader_name + "_f32_f32_aligned", source_name, {**defines, **ft_f32, "DATA_A_F32": "1", "LOAD_VEC_A": load_vec, "LOAD_VEC_B": load_vec, "B_TYPE": aligned_b_type_f32, "D_TYPE": "float", "ALIGNED": "1"}, fp16, coopmat, coopmat2, f16acc, args)

        # A=f16, B=f32 shaders
        string_to_spv(shader_name + "_f16_f32",         source_name, {**defines, **ft_f32, "DATA_A_F16": "1", "B_TYPE": "float", "D_TYPE": "float"}, fp16, coopmat, coopmat2, f16acc, args)
        string_to_spv(shader_name + "_f16_f32_aligned", source_name, {**defines, **ft_f32, "DATA_A_F16": "1", "LOAD_VEC_A": load_vec, "LOAD_VEC_B": load_vec, "B_TYPE": aligned_b_type_f32, "D_TYPE": "float", "ALIGNED": "1"}, fp16, coopmat, coopmat2, f16acc, args)

    # bf16 standalone (scalar path, promote to float)
    ft_bf16 = {
        "FLOAT_TYPE":      float_type_func(1, "bf16", fp16, coopmat, coopmat2),
        "FLOAT_TYPE_VEC2": float_type_func(2, "bf16", fp16, coopmat, coopmat2),
        "FLOAT_TYPE_VEC4": float_type_func(4, "bf16", fp16, coopmat, coopmat2),
    }
    to_float_type = "uintBitsToBFloat16EXT" if (coopmat or coopmat2) else "bf16_to_fp32"
    b_type_bf16 = ("bfloat16_t" if coopmat2 else "uint16_t")
    b_type_bf16_aligned = ("bfloat16_t" if coopmat2 else "u16vec4")
    
    string_to_spv(shader_name + "_bf16",         source_name, {**defines, **ft_bf16, "TO_FLOAT_TYPE": to_float_type, "DATA_A_BF16": "1", "B_TYPE": b_type_bf16, "D_TYPE": "float", "B_IS_FLOAT": "1", "DATA_B_BF16": "1"}, fp16, coopmat, coopmat2, f16acc, args)
    string_to_spv(shader_name + "_bf16_aligned", source_name, {**defines, **ft_bf16, "TO_FLOAT_TYPE": to_float_type, "DATA_A_BF16": "1", "LOAD_VEC_A": "1" if coopmat2 else "4", "LOAD_VEC_B": "4", "B_TYPE": b_type_bf16_aligned, "D_TYPE": "float", "B_IS_FLOAT": "1", "DATA_B_BF16": "1", "ALIGNED": "1"}, fp16, coopmat, coopmat2, f16acc, args)

    # Per-type quantized shaders (f32 and f16 types handled above as standalone)
    for tname in type_names:
        if tname in ["f32", "f16", "bf16"]: continue
        
        load_vec_quant = "8" if tname in ["q4_0", "q4_1", "q5_1", "iq1_s", "iq1_m", "iq2_xxs", "iq2_xs", "iq2_s"] else \
                         "4" if tname in ["q5_0", "q8_0", "q2_k", "q4_k", "q5_k", "iq3_xxs", "iq3_s", "iq4_nl", "mxfp4"] else "2"
        
        float_types = {
            "FLOAT_TYPE":      float_type_func(1, tname, fp16, coopmat, coopmat2),
            "FLOAT_TYPE_VEC2": float_type_func(2, tname, fp16, coopmat, coopmat2),
            "FLOAT_TYPE_VEC4": float_type_func(4, tname, fp16, coopmat, coopmat2),
            "FLOAT_TYPE_VEC8": float_type_func(8, tname, fp16, coopmat, coopmat2),
        }
        data_a_key = "DATA_A_" + to_uppercase(tname)
        
        if not coopmat2:
            string_to_spv(shader_name + "_" + tname + "_f32",         source_name, {**defines, **float_types, data_a_key: "1", "LOAD_VEC_A": load_vec_quant, "B_TYPE": "float",    "D_TYPE": "float"}, fp16, coopmat, coopmat2, f16acc, args)
            string_to_spv(shader_name + "_" + tname + "_f32_aligned", source_name, {**defines, **float_types, data_a_key: "1", "LOAD_VEC_A": load_vec_quant, "LOAD_VEC_B": load_vec, "B_TYPE": aligned_b_type_f32, "D_TYPE": "float", "ALIGNED": "1"}, fp16, coopmat, coopmat2, f16acc, args)

        string_to_spv(shader_name + "_" + tname + "_f16",         source_name, {**defines, **float_types, data_a_key: "1", "LOAD_VEC_A": load_vec_quant, "B_TYPE": "float16_t", "D_TYPE": "float"}, fp16, coopmat, coopmat2, f16acc, args)
        string_to_spv(shader_name + "_" + tname + "_f16_aligned", source_name, {**defines, **float_types, data_a_key: "1", "LOAD_VEC_A": load_vec_quant, "LOAD_VEC_B": load_vec, "B_TYPE": aligned_b_type_f16, "D_TYPE": "float", "ALIGNED": "1"}, fp16, coopmat, coopmat2, f16acc, args)

def process_shaders(args):
    base_dict = {"FLOAT_TYPE": "float", "FLOAT_TYPE_VEC2": "vec2"}
    
    # matmuls
    for mid in ["NONE", "DEFAULT", "SUBGROUP"]:
        for fp16 in [False, True]:
            for f16acc in [False, True]:
                if not fp16 and f16acc: continue
                matmul_shaders(fp16, mid, False, False, f16acc, args, base_dict)
    
    for tname in type_names:
        data_a_key = "DATA_A_" + to_uppercase(tname)
        shader = "mul_mat_vec_" + tname + ".comp" if (tname.endswith("_k") or tname.startswith("iq1_") or tname.startswith("iq2_") or tname.startswith("iq3_")) else "mul_mat_vec.comp"

        string_to_spv("mul_mat_vec_" + tname + "_f32_f32", shader, {**base_dict, data_a_key: "1", "B_TYPE": "float", "B_TYPE_VEC2": "vec2", "B_TYPE_VEC4": "vec4", "D_TYPE": "float"}, True, False, False, False, args)
        string_to_spv("mul_mat_vec_" + tname + "_f16_f32", shader, {**base_dict, data_a_key: "1", "B_TYPE": "float16_t", "B_TYPE_VEC2": "f16vec2", "B_TYPE_VEC4": "f16vec4", "D_TYPE": "float"}, True, False, False, False, args)

        string_to_spv("mul_mat_vec_" + tname + "_f32_f32_subgroup", shader, {**base_dict, data_a_key: "1", "B_TYPE": "float", "B_TYPE_VEC2": "vec2", "B_TYPE_VEC4": "vec4", "D_TYPE": "float", "USE_SUBGROUP_ADD": "1"}, True, False, False, False, args)
        string_to_spv("mul_mat_vec_" + tname + "_f16_f32_subgroup", shader, {**base_dict, data_a_key: "1", "B_TYPE": "float16_t", "B_TYPE_VEC2": "f16vec2", "B_TYPE_VEC4": "f16vec4", "D_TYPE": "float", "USE_SUBGROUP_ADD": "1"}, True, False, False, False, args)

        string_to_spv("mul_mat_vec_" + tname + "_f32_f32_subgroup_no_shmem", shader, {**base_dict, data_a_key: "1", "B_TYPE": "float", "B_TYPE_VEC2": "vec2", "B_TYPE_VEC4": "vec4", "D_TYPE": "float", "USE_SUBGROUP_ADD_NO_SHMEM": "1"}, True, False, False, False, args)
        string_to_spv("mul_mat_vec_" + tname + "_f16_f32_subgroup_no_shmem", shader, {**base_dict, data_a_key: "1", "B_TYPE": "float16_t", "B_TYPE_VEC2": "f16vec2", "B_TYPE_VEC4": "f16vec4", "D_TYPE": "float", "USE_SUBGROUP_ADD_NO_SHMEM": "1"}, True, False, False, False, args)

        string_to_spv("mul_mat_vec_id_" + tname + "_f32_f32", shader, {**base_dict, "MUL_MAT_ID": "1", data_a_key: "1", "B_TYPE": "float", "B_TYPE_VEC2": "vec2", "B_TYPE_VEC4": "vec4", "D_TYPE": "float"}, True, False, False, False, args)
        string_to_spv("mul_mat_vec_id_" + tname + "_f32_f32_subgroup", shader, {**base_dict, "MUL_MAT_ID": "1", data_a_key: "1", "B_TYPE": "float", "B_TYPE_VEC2": "vec2", "B_TYPE_VEC4": "vec4", "D_TYPE": "float", "USE_SUBGROUP_ADD": "1"}, True, False, False, False, args)
        string_to_spv("mul_mat_vec_id_" + tname + "_f32_f32_subgroup_no_shmem", shader, {**base_dict, "MUL_MAT_ID": "1", data_a_key: "1", "B_TYPE": "float", "B_TYPE_VEC2": "vec2", "B_TYPE_VEC4": "vec4", "D_TYPE": "float", "USE_SUBGROUP_ADD_NO_SHMEM": "1"}, True, False, False, False, args)

        # Dequant
        if tname not in ["f16", "bf16"]:
            string_to_spv("dequant_" + tname, "dequant_" + tname + ".comp", {**base_dict, data_a_key: "1", "D_TYPE": "float16_t"}, True, False, False, False, args)

        # Get rows
        get_rows_shader = "get_rows.comp" if tname in ["f32", "f16", "bf16"] else "get_rows_quant.comp"
        if tname == "f16":
            string_to_spv("get_rows_" + tname, get_rows_shader, {**base_dict, "TEMP_TYPE": "FLOAT_TYPE", data_a_key: "1", "B_TYPE": "int", "D_TYPE": "float16_t", "OPTIMIZATION_ERROR_WORKAROUND": "1"}, True, False, False, False, args)
        else:
            string_to_spv("get_rows_" + tname, get_rows_shader, {**base_dict, "TEMP_TYPE": "FLOAT_TYPE", data_a_key: "1", "B_TYPE": "int", "D_TYPE": "float16_t"}, True, False, False, False, args)
        string_to_spv("get_rows_" + tname + "_f32", get_rows_shader, {**base_dict, "TEMP_TYPE": "FLOAT_TYPE", data_a_key: "1", "B_TYPE": "int", "D_TYPE": "float"}, True, False, False, False, args)

    string_to_spv("get_rows_i32", "get_rows.comp", {"TEMP_TYPE": "uint", "A_TYPE": "uint", "B_TYPE": "int", "D_TYPE": "uint"}, True, False, False, False, args)

    string_to_spv("mul_mat_vec_p021_f16_f32_subgroup_add", "mul_mat_vec_p021.comp", {"A_TYPE": "float16_t", "A_TYPE_VEC4": "f16vec4", "B_TYPE": "float", "B_TYPE_VEC4": "vec4", "D_TYPE": "float", "USE_SUBGROUP_ADD": "1"}, True, False, False, False, args)
    string_to_spv("mul_mat_vec_p021_f16_f32", "mul_mat_vec_p021.comp", {"A_TYPE": "float16_t", "A_TYPE_VEC4": "f16vec4", "B_TYPE": "float", "B_TYPE_VEC4": "vec4", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("mul_mat_vec_nc_f16_f32", "mul_mat_vec_nc.comp", {"A_TYPE": "float16_t", "A_TYPE_VEC4": "f16vec4", "B_TYPE": "float", "B_TYPE_VEC4": "vec4", "D_TYPE": "float"}, True, False, False, False, args)

    for op, comp in [("mul", "mul.comp"), ("div", "div.comp"), ("scale", "scale.comp"), ("sqr", "square.comp"), ("sqrt", "sqrt.comp"),
                     ("sin", "sin.comp"), ("cos", "cos.comp"), ("clamp", "clamp.comp"), ("pad", "pad.comp"), ("concat", "concat.comp"),
                     ("upscale", "upscale.comp")]:
        string_to_spv(op + "_f32", comp, {**base_dict, "A_TYPE": "float", "B_TYPE": "float", "D_TYPE": "float", "FLOAT_TYPE": "float"}, True, False, False, False, args)
    
    for t in ["f16", "f32"]:
        for suffix in ["", "_rte"]:
            dt = "float" if t=="f32" else "float16_t"
            rte = "1" if suffix else "0"
            string_to_spv("rope_norm_" + t + suffix, "rope_norm.comp", {"A_TYPE": dt, "ROPE_D_TYPE": dt, "RTE16": rte}, True, False, False, False, args)
            string_to_spv("rope_neox_" + t + suffix, "rope_neox.comp", {"A_TYPE": dt, "ROPE_D_TYPE": dt, "RTE16": rte}, True, False, False, False, args)
            string_to_spv("rope_multi_" + t + suffix, "rope_multi.comp", {"A_TYPE": dt, "ROPE_D_TYPE": dt, "RTE16": rte}, True, False, False, False, args)
        string_to_spv("rope_vision_" + t, "rope_vision.comp", {"A_TYPE": dt, "ROPE_D_TYPE": dt}, True, False, False, False, args)
        if t == "f16": string_to_spv("rope_vision_f16_rte", "rope_vision.comp", {"A_TYPE": "float16_t", "ROPE_D_TYPE": "float16_t", "RTE16": "1"}, True, False, False, False, args)
        
        string_to_spv("silu_" + t, "silu.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)
        string_to_spv("gelu_" + t, "gelu.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)

    string_to_spv("soft_max_f32", "soft_max.comp", {**base_dict, "A_TYPE": "float", "B_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("soft_max_f32_f16", "soft_max.comp", {**base_dict, "A_TYPE": "float", "B_TYPE": "float16_t", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("soft_max_back_f32", "soft_max_back.comp", {**base_dict, "A_TYPE": "float", "B_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)

    string_to_spv("add_id_f32", "add_id.comp", {**base_dict, "A_TYPE": "float", "B_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)

    # Flash Attention
    for f16acc in [False, True]:
        fa_base_dict = {**base_dict}
        fa_base_dict["ACC_TYPE"] = "float16_t" if f16acc else "float"
        fa_base_dict["ACC_TYPEV4"] = "f16vec4" if f16acc else "vec4"
        if f16acc:
            fa_base_dict["ACC_TYPE_MAX"] = "float16_t(65504.0)"
            
        for tname in type_names:
            if tname == "bf16": continue
            for aligned in [False, True]:
                base_name = "flash_attn_f32_f16" + ("_aligned" if aligned else "")
                aligned_def = {"ALIGNED": "1"} if aligned else {}
                
                # F16 uses f16acc intrinsically based on the pass, others require explicit naming depending on usage
                # But actually string_to_spv adds "_f16acc" for us automatically if f16acc=True!
                # Wait, string_to_spv does add it. It generates `flash_attn_f32_f16_q4_0_f16acc`. 
                # So we just need to make sure the name prefix passed to string_to_spv is correct.
                
                if tname == "f16":
                    string_to_spv(f"{base_name}_{tname}", "flash_attn.comp", {**fa_base_dict, **aligned_def, "Q_TYPE": "float", "D_TYPE": "float"}, True, False, False, f16acc, args, no_fp32_suffix=True)
                elif tname == "f32":
                    string_to_spv(f"{base_name}_{tname}", "flash_attn.comp", {**fa_base_dict, **aligned_def, "DATA_A_F32": "1", "Q_TYPE": "float", "D_TYPE": "float", "BLOCK_SIZE": "QUANT_K_F32"}, True, False, False, f16acc, args, no_fp32_suffix=True)
                elif tname in ["q4_0", "q8_0"]:
                    data_a_key = "DATA_A_" + to_uppercase(tname)
                    string_to_spv(f"{base_name}_{tname}", "flash_attn.comp", {**fa_base_dict, **aligned_def, data_a_key: "1", "Q_TYPE": "float", "D_TYPE": "float", "BLOCK_SIZE": "QUANT_K_" + to_uppercase(tname)}, True, False, False, f16acc, args, no_fp32_suffix=True)

    # im2col
    for dim_str in ["", "_3d"]:
        for bda in [False, True]:
            bda_str = "_bda" if bda else ""
            bda_def = "1" if bda else "0"
            string_to_spv("im2col" + dim_str + "_f32" + bda_str, "im2col" + dim_str + ".comp", {**base_dict, "A_TYPE": "float", "D_TYPE": "float", "D_SIZE": "4", "BDA": bda_def}, True, False, False, False, args)
            string_to_spv("im2col" + dim_str + "_f32_f16" + bda_str, "im2col" + dim_str + ".comp", {**base_dict, "A_TYPE": "float", "D_TYPE": "float16_t", "D_SIZE": "2", "BDA": bda_def}, True, False, False, False, args)
            string_to_spv("im2col" + dim_str + "_f32_f16_rte" + bda_str, "im2col" + dim_str + ".comp", {**base_dict, "A_TYPE": "float", "D_TYPE": "float16_t", "D_SIZE": "2", "RTE16": "1", "BDA": bda_def}, True, False, False, False, args)

    # Misc
    string_to_spv("split_k_reduce", "mul_mat_split_k_reduce.comp", {}, True, False, False, False, args)
    string_to_spv("fa_split_k_reduce", "flash_attn_split_k_reduce.comp", {}, True, False, False, False, args)
    string_to_spv("fa_mask_opt", "flash_attn_mask_opt.comp", {}, True, False, False, False, args)
    string_to_spv("diag_mask_inf_f32", "diag_mask_inf.comp", {"A_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("argsort_f32", "argsort.comp", {"A_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("argsort_large_f32", "argsort_large.comp", {"A_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("argmax_f32", "argmax.comp", {**base_dict, "A_TYPE": "float", "D_TYPE": "int"}, True, False, False, False, args)

    # get_type_str logic
    def get_type_str(f16): return "float16_t" if f16 else "float"
    def get_suffix(src0_f16, src1_f16, dst_f16): return ("_f16" if src0_f16 else "_f32") + ("_f16" if src1_f16 else "_f32") + ("_f16" if dst_f16 else "_f32")
    
    for op in ["add", "sub", "mul", "div", "add_rms"]:
        for src0_f16 in [False, True]:
            for src1_f16 in [False, True]:
                for dst_f16 in [False, True]:
                    for rte in [False, True]:
                        source = "add" if op == "add_rms" else op
                        name = op + get_suffix(src0_f16, src1_f16, dst_f16) + ("_rte" if rte else "")
                        add_rms = "1" if op == "add_rms" else "0"
                        string_to_spv(name, source + ".comp", {"A_TYPE": get_type_str(src0_f16), "B_TYPE": get_type_str(src1_f16), "D_TYPE": get_type_str(dst_f16), "FLOAT_TYPE": "float", "RTE16": "1" if rte else "0", "ADD_RMS": add_rms}, True, False, False, False, args)

    # More misc operations
    string_to_spv("pool2d_f32", "pool2d.comp", {**base_dict, "A_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("rwkv_wkv6_f32", "wkv6.comp", {**base_dict, "A_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("rwkv_wkv7_f32", "wkv7.comp", {**base_dict, "A_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("opt_step_adamw_f32", "opt_step_adamw.comp", {**base_dict, "A_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("opt_step_sgd_f32", "opt_step_sgd.comp", {**base_dict, "A_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("solve_tri_f32", "solve_tri.comp", {**base_dict, "A_TYPE": "float", "B_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)

    for transpose in [False, True]:
        for unroll in [False, True]:
            for a_f16 in [False, True]:
                defines = {
                    "A_TYPE": "float16_t" if a_f16 else "float",
                    "B_TYPE": "float", "D_TYPE": "float",
                    "USE_COLLECTIVES": "1",
                    "UNROLL": "[[unroll]]" if unroll else ""
                }
                if transpose: defines["TRANSPOSE"] = "1"
                name = ("conv_transpose_2d" if transpose else "conv2d") + ("_f16" if a_f16 else "") + "_f32"
                string_to_spv(name + ("_unroll" if unroll else ""), "conv2d_mm.comp", defines, True, False, False, False, args)

    string_to_spv("conv2d_dw_whcn_f32", "conv2d_dw.comp", {**base_dict, "A_TYPE": "float", "B_TYPE": "float", "D_TYPE": "float", "WHCN": "1"}, True, False, False, False, args)
    string_to_spv("conv2d_dw_cwhn_f32", "conv2d_dw.comp", {**base_dict, "A_TYPE": "float", "B_TYPE": "float", "D_TYPE": "float", "CWHN": "1"}, True, False, False, False, args)
    string_to_spv("conv2d_dw_whcn_f16_f32", "conv2d_dw.comp", {**base_dict, "A_TYPE": "float16_t", "B_TYPE": "float", "D_TYPE": "float", "WHCN": "1"}, True, False, False, False, args)
    string_to_spv("conv2d_dw_cwhn_f16_f32", "conv2d_dw.comp", {**base_dict, "A_TYPE": "float16_t", "B_TYPE": "float", "D_TYPE": "float", "CWHN": "1"}, True, False, False, False, args)

    string_to_spv("roll_f32", "roll.comp", {**base_dict, "A_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("multi_add_f32", "multi_add.comp", {"A_TYPE": "float", "B_TYPE": "float", "D_TYPE": "float", "FLOAT_TYPE": "float", "RTE16": "1", "ADD_RMS": "0"}, True, False, False, False, args)
    string_to_spv("multi_add_rms_f32", "multi_add.comp", {"A_TYPE": "float", "B_TYPE": "float", "D_TYPE": "float", "FLOAT_TYPE": "float", "RTE16": "1", "ADD_RMS": "1"}, True, False, False, False, args)
    string_to_spv("ssm_scan_f32", "ssm_scan.comp", {"A_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("ssm_scan_subgroup_f32", "ssm_scan.comp", {"A_TYPE": "float", "USE_SUBGROUP_ADD": "1"}, True, False, False, False, args)
    string_to_spv("ssm_conv_f32", "ssm_conv.comp", {"A_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("topk_moe_f32", "topk_moe.comp", {}, True, False, False, False, args)
    
    string_to_spv("sum_rows_f32", "sum_rows.comp", {**base_dict, "A_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("count_equal_i32", "count_equal.comp", {**base_dict, "A_TYPE": "int", "B_TYPE": "int", "D_TYPE": "int"}, True, False, False, False, args)
    string_to_spv("cumsum_f32", "cumsum.comp", {**base_dict, "A_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("cumsum_multipass1_f32", "cumsum_multipass1.comp", {**base_dict, "A_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("cumsum_multipass2_f32", "cumsum_multipass2.comp", {**base_dict, "A_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("count_experts", "count_experts.comp", {**base_dict, "A_TYPE": "uint", "D_TYPE": "uint"}, True, False, False, False, args)
    string_to_spv("timestep_embedding_f32", "timestep_embedding.comp", {**base_dict, "A_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("conv_transpose_1d_f32", "conv_transpose_1d.comp", {"A_TYPE": "float", "B_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)

    string_to_spv("topk_argsort_f32", "topk_argsort.comp", {"A_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("topk_nary_search_f32", "topk_nary_search.comp", {"A_TYPE": "float"}, True, False, False, False, args)
    
    for t in ["f16", "f32"]:
        for suffix in ["", "_rte"]:
            dt = "float" if t=="f32" else "float16_t"
            rte = "1" if suffix else "0"
            string_to_spv("geglu_" + t + suffix, "geglu.comp", {"A_TYPE": dt, "D_TYPE": dt, "RTE16": rte}, True, False, False, False, args)
            string_to_spv("reglu_" + t + suffix, "reglu.comp", {"A_TYPE": dt, "D_TYPE": dt, "RTE16": rte}, True, False, False, False, args)
            string_to_spv("swiglu_" + t + suffix, "swiglu.comp", {"A_TYPE": dt, "D_TYPE": dt, "RTE16": rte}, True, False, False, False, args)
            string_to_spv("swiglu_oai_" + t + suffix, "swiglu_oai.comp", {"A_TYPE": dt, "D_TYPE": dt, "RTE16": rte}, True, False, False, False, args)
            string_to_spv("geglu_erf_" + t + suffix, "geglu_erf.comp", {"A_TYPE": dt, "D_TYPE": dt, "RTE16": rte}, True, False, False, False, args)
            string_to_spv("geglu_quick_" + t + suffix, "geglu_quick.comp", {"A_TYPE": dt, "D_TYPE": dt, "RTE16": rte}, True, False, False, False, args)
            string_to_spv("exp_" + t + suffix, "exp.comp", {"A_TYPE": dt, "D_TYPE": dt, "RTE16": rte}, True, False, False, False, args)
            string_to_spv("log_" + t + suffix, "log.comp", {"A_TYPE": dt, "D_TYPE": dt, "RTE16": rte}, True, False, False, False, args)

        string_to_spv("gelu_erf_" + t, "gelu_erf.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)
        string_to_spv("gelu_quick_" + t, "gelu_quick.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)
        string_to_spv("relu_" + t, "relu.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)
        string_to_spv("neg_" + t, "neg.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)
        string_to_spv("tanh_" + t, "tanh.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)
        string_to_spv("sigmoid_" + t, "sigmoid.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)
        string_to_spv("hardsigmoid_" + t, "hardsigmoid.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)
        string_to_spv("hardswish_" + t, "hardswish.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)
        string_to_spv("abs_" + t, "abs.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)
        string_to_spv("xielu_" + t, "xielu.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)
        string_to_spv("tri_" + t, "tri.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)
        string_to_spv("diag_" + t, "diag.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)
        string_to_spv("softplus_" + t, "softplus.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)
        string_to_spv("step_" + t, "step.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)
        string_to_spv("round_" + t, "round.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)
        string_to_spv("ceil_" + t, "ceil.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)
        string_to_spv("floor_" + t, "floor.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)
        string_to_spv("trunc_" + t, "trunc.comp", {"A_TYPE": dt, "D_TYPE": dt}, True, False, False, False, args)

    string_to_spv("leaky_relu_f32", "leaky_relu.comp", {"A_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("silu_back_f32", "silu_back.comp", {"A_TYPE": "float", "B_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("sub_f32", "sub.comp", {"A_TYPE": "float", "B_TYPE": "float", "D_TYPE": "float", "FLOAT_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("acc_f32", "acc.comp", {"A_TYPE": "float", "B_TYPE": "float", "D_TYPE": "float", "FLOAT_TYPE": "float"}, True, False, False, False, args)

    # Missing Add1
    string_to_spv("add1_f16_f16", "add1.comp", {"A_TYPE": "float16_t", "B_TYPE": "float16_t", "D_TYPE": "float16_t", "FLOAT_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("add1_f16_f32", "add1.comp", {"A_TYPE": "float16_t", "B_TYPE": "float", "D_TYPE": "float16_t", "FLOAT_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("add1_f32_f32", "add1.comp", {"A_TYPE": "float", "B_TYPE": "float", "D_TYPE": "float", "FLOAT_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("arange_f32", "arange.comp", {"A_TYPE": "float", "D_TYPE": "float", "FLOAT_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("fill_f32", "fill.comp", {"D_TYPE": "float", "FLOAT_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("repeat_f32", "repeat.comp", {"A_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("repeat_back_f32", "repeat_back.comp", {"A_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)

    for i in [1, 2, 3]:
        string_to_spv("soft_max_large" + str(i) + "_f32", "soft_max_large" + str(i) + ".comp", {**base_dict, "A_TYPE": "float", "B_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)
        string_to_spv("soft_max_large" + str(i) + "_f32_f16", "soft_max_large" + str(i) + ".comp", {**base_dict, "A_TYPE": "float", "B_TYPE": "float16_t", "D_TYPE": "float"}, True, False, False, False, args)

    string_to_spv("group_norm_f32", "group_norm.comp", {**base_dict, "A_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("rms_norm_partials_f32", "rms_norm_partials.comp", {**base_dict, "A_TYPE": "float", "B_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("rms_norm_mul_rope_f32_f32", "rms_norm.comp", {**base_dict, "A_TYPE": "float", "B_TYPE": "float", "D_TYPE": "float", "ROPE_D_TYPE": "float", "RMS_NORM_ROPE_FUSION": "1"}, True, False, False, False, args)
    string_to_spv("rms_norm_mul_rope_f32_f16_rte", "rms_norm.comp", {**base_dict, "A_TYPE": "float", "B_TYPE": "float", "D_TYPE": "float", "ROPE_D_TYPE": "float16_t", "RMS_NORM_ROPE_FUSION": "1", "RTE16": "1"}, True, False, False, False, args)
    string_to_spv("rms_norm_back_f32", "rms_norm_back.comp", {**base_dict, "A_TYPE": "float", "B_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("l2_norm_f32", "l2_norm.comp", {**base_dict, "A_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)
    
    # Copies
    string_to_spv("cpy_f32_f32", "copy.comp", {"A_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("cpy_f32_f16", "copy.comp", {"A_TYPE": "float", "D_TYPE": "float16_t"}, True, False, False, False, args)
    string_to_spv("cpy_f16_f16", "copy.comp", {"A_TYPE": "float16_t", "D_TYPE": "float16_t", "OPTIMIZATION_ERROR_WORKAROUND": "1"}, True, False, False, False, args)
    string_to_spv("cpy_f16_f32", "copy.comp", {"A_TYPE": "float16_t", "D_TYPE": "float", "OPTIMIZATION_ERROR_WORKAROUND": "1"}, True, False, False, False, args)
    string_to_spv("cpy_f32_bf16", "copy.comp", {"A_TYPE": "float", "D_TYPE": "uint16_t", "DATA_D_BF16": "1"}, True, False, False, False, args)
    string_to_spv("cpy_f32_i32", "copy.comp", {"A_TYPE": "float", "D_TYPE": "int"}, True, False, False, False, args)
    string_to_spv("cpy_i32_f32", "copy.comp", {"A_TYPE": "int", "D_TYPE": "float"}, True, False, False, False, args)
    
    string_to_spv("contig_cpy_f32_f32", "contig_copy.comp", {"A_TYPE": "float", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("contig_cpy_f32_i32", "contig_copy.comp", {"A_TYPE": "float", "D_TYPE": "int"}, True, False, False, False, args)
    string_to_spv("contig_cpy_i32_f32", "contig_copy.comp", {"A_TYPE": "int", "D_TYPE": "float"}, True, False, False, False, args)
    string_to_spv("contig_cpy_f32_f16", "contig_copy.comp", {"A_TYPE": "float", "D_TYPE": "float16_t"}, True, False, False, False, args)
    string_to_spv("contig_cpy_f16_f16", "contig_copy.comp", {"A_TYPE": "float16_t", "D_TYPE": "float16_t", "OPTIMIZATION_ERROR_WORKAROUND": "1"}, True, False, False, False, args)
    string_to_spv("contig_cpy_f16_f32", "contig_copy.comp", {"A_TYPE": "float16_t", "D_TYPE": "float", "OPTIMIZATION_ERROR_WORKAROUND": "1"}, True, False, False, False, args)
    string_to_spv("contig_cpy_f32_bf16", "contig_copy.comp", {"A_TYPE": "float", "D_TYPE": "uint16_t", "DATA_D_BF16": "1"}, True, False, False, False, args)
    
    string_to_spv("cpy_transpose_16", "copy_transpose.comp", {"A_TYPE": "uint16_t", "D_TYPE": "uint16_t"}, True, False, False, False, args)
    string_to_spv("cpy_transpose_32", "copy_transpose.comp", {"A_TYPE": "uint", "D_TYPE": "uint"}, True, False, False, False, args)

    for qt in ["q4_0", "q4_1", "q5_0", "q5_1", "q8_0", "iq4_nl"]:
        k = "DATA_A_" + to_uppercase(qt)
        string_to_spv("cpy_f32_" + qt, "copy_to_quant.comp", {k: "1", "D_TYPE": "float", "FLOAT_TYPE": "float"}, True, False, False, False, args)
        string_to_spv("cpy_f32_" + qt + "_rte", "copy_to_quant.comp", {k: "1", "D_TYPE": "float", "FLOAT_TYPE": "float", "RTE16": "1"}, True, False, False, False, args)
        string_to_spv("cpy_" + qt + "_f32", "copy_from_quant.comp", {k: "1", "D_TYPE": "float", "FLOAT_TYPE": "float"}, True, False, False, False, args)
    
    for t in ["f32", "f16", "bf16", "q4_0", "q4_1", "q5_0", "q5_1", "q8_0", "iq4_nl"]:
        k = "DATA_A_" + to_uppercase(t)
        string_to_spv("set_rows_" + t + "_i32", "copy_to_quant.comp", {"SET_ROWS": "1", k: "1", "B_TYPE": "uint", "B_SIZE": "32", "D_TYPE": "float", "FLOAT_TYPE": "float"}, True, False, False, False, args)
        string_to_spv("set_rows_" + t + "_i32_rte", "copy_to_quant.comp", {"SET_ROWS": "1", k: "1", "B_TYPE": "uint", "B_SIZE": "32", "D_TYPE": "float", "FLOAT_TYPE": "float", "RTE16": "1"}, True, False, False, False, args)
        string_to_spv("set_rows_" + t + "_i64", "copy_to_quant.comp", {"SET_ROWS": "1", k: "1", "B_TYPE": "uvec2", "B_SIZE": "64", "D_TYPE": "float", "FLOAT_TYPE": "float"}, True, False, False, False, args)
        string_to_spv("set_rows_" + t + "_i64_rte", "copy_to_quant.comp", {"SET_ROWS": "1", k: "1", "B_TYPE": "uvec2", "B_SIZE": "64", "D_TYPE": "float", "FLOAT_TYPE": "float", "RTE16": "1"}, True, False, False, False, args)

def write_output(args):
    global shader_fnames
    shader_fnames.sort()
    
    if not args.source:
        with open(args.target_hpp, "w") as f:
            f.write("#include <cstdint>\n\n")
            for name, _ in shader_fnames:
                f.write(f"extern const uint64_t {name}_len;\n")
                f.write(f"extern const unsigned char {name}_data[];\n\n")
            
            suffixes = ["_f32", "_f16"]
            for op in ["add", "sub", "mul", "div", "add_rms"]:
                f.write(f"extern const void * {op}_data[2][2][2][2];\n")
                f.write(f"extern const uint64_t {op}_len[2][2][2][2];\n")
                
            btypes = ["f16", "f32"]
            for btype in btypes:
                for tname in type_names:
                    if btype == "q8_1" and not is_legacy_quant(tname) and tname != "mxfp4" and not is_k_quant(tname) and tname != "iq1_s" and tname != "iq1_m": continue
                    f.write(f"extern const void * arr_dmmv_{tname}_{btype}_f32_data[3];\n")
                    f.write(f"extern const uint64_t arr_dmmv_{tname}_{btype}_f32_len[3];\n")
                    if btype == "f16": continue
                    f.write(f"extern const void * arr_dmmv_id_{tname}_{btype}_f32_data[3];\n")
                    f.write(f"extern const uint64_t arr_dmmv_id_{tname}_{btype}_f32_len[3];\n")
    
    if args.target_cpp:
        with open(args.target_cpp, "w") as f:
            f.write(f'#include "{os.path.basename(args.target_hpp)}"\n\n')
            for name, path in shader_fnames:
                if not os.path.exists(path): continue
                with open(path, "rb") as spv:
                    data = spv.read()
                f.write(f"const uint64_t {name}_len = {len(data)};\n")
                f.write(f"const unsigned char {name}_data[{len(data)}] = {{\n")
                for i in range(0, len(data), 12):
                    chunk = data[i:i+12]
                    f.write("  " + ",".join([f"0x{b:02x}" for b in chunk]) + ",\n")
                f.write("};\n\n")
                
            suffixes = ["_f32", "_f16"]
            for op in ["add", "sub", "mul", "div", "add_rms"]:
                op_file = "add.comp" if op == "add_rms" else op + ".comp"
                if os.path.basename(args.source) != op_file: continue
                
                data_str = f"const void * {op}_data[2][2][2][2] = "
                len_str = f"const uint64_t {op}_len[2][2][2][2] = "
                for t0 in range(2):
                    if t0 == 0: data_str += "{"; len_str += "{"
                    for t1 in range(2):
                        if t1 == 0: data_str += "{"; len_str += "{"
                        for t2 in range(2):
                            if t2 == 0: data_str += "{"; len_str += "{"
                            for rte in range(2):
                                if rte == 0: data_str += "{"; len_str += "{"
                                name_suffix = suffixes[t0] + suffixes[t1] + suffixes[t2] + ("_rte" if rte else "")
                                data_str += f"{op}{name_suffix}_data,"
                                len_str += f"{op}{name_suffix}_len,"
                                if rte == 1: data_str += "}, "; len_str += "}, "
                            if t2 == 1: data_str += "}, "; len_str += "}, "
                        if t1 == 1: data_str += "}, "; len_str += "}, "
                    if t0 == 1: data_str += "};\n"; len_str += "};\n"
                f.write(data_str + len_str)

            btypes = ["f16", "f32"]
            for btype in btypes:
                for tname in type_names:
                    if btype == "q8_1" and not is_legacy_quant(tname) and tname != "mxfp4" and not is_k_quant(tname) and tname != "iq1_s" and tname != "iq1_m": continue
                    if os.path.basename(args.source) == "mul_mat_vec.comp":
                        f.write(f"const void * arr_dmmv_{tname}_{btype}_f32_data[3] = {{mul_mat_vec_{tname}_{btype}_f32_data, mul_mat_vec_{tname}_{btype}_f32_subgroup_data, mul_mat_vec_{tname}_{btype}_f32_subgroup_no_shmem_data}};\n")
                        f.write(f"const uint64_t arr_dmmv_{tname}_{btype}_f32_len[3] = {{mul_mat_vec_{tname}_{btype}_f32_len, mul_mat_vec_{tname}_{btype}_f32_subgroup_len, mul_mat_vec_{tname}_{btype}_f32_subgroup_no_shmem_len}};\n")
                    if btype == "f16": continue
                    if os.path.basename(args.source) == "mul_mat_vec.comp":
                        f.write(f"const void * arr_dmmv_id_{tname}_{btype}_f32_data[3] = {{mul_mat_vec_id_{tname}_{btype}_f32_data, mul_mat_vec_id_{tname}_{btype}_f32_subgroup_data, mul_mat_vec_id_{tname}_{btype}_f32_subgroup_no_shmem_data}};\n")
                        f.write(f"const uint64_t arr_dmmv_id_{tname}_{btype}_f32_len[3] = {{mul_mat_vec_id_{tname}_{btype}_f32_len, mul_mat_vec_id_{tname}_{btype}_f32_subgroup_len, mul_mat_vec_id_{tname}_{btype}_f32_subgroup_no_shmem_len}};\n")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--glslc", default="glslc")
    parser.add_argument("--source", default="")
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--target-hpp", required=True)
    parser.add_argument("--target-cpp", default="")
    args = parser.parse_args()
    
    if not os.path.exists(args.output_dir):
        os.makedirs(args.output_dir)
        
    process_shaders(args)
    write_output(args)
