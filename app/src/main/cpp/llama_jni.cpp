#include <jni.h>
#include <string>
#include <vector>
#include <chrono>
#include <android/log.h>

#include "llama.h"
#include "mtmd.h"
#include "mtmd-helper.h"

#define TAG "LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#if defined(GGML_USE_VULKAN)
#include "ggml-vulkan.h"
#endif

struct LlamaContextWrapper {
    llama_model* model = nullptr;
    mtmd_context* mtmd_ctx = nullptr;
    llama_context* ctx = nullptr;
};

// Global reference to the LlamaCppService class to avoid ClassLoader issues in background threads
static jclass g_service_class = nullptr;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    LOGI("JNI_OnLoad: Caching LlamaCppService class reference...");
    jclass local_class = env->FindClass("com/pocketai/app/services/LlamaCppService");
    if (local_class == nullptr) {
        LOGE("JNI_OnLoad: CRITICAL - Could not find LlamaCppService class!");
        return JNI_ERR;
    }

    g_service_class = reinterpret_cast<jclass>(env->NewGlobalRef(local_class));
    env->DeleteLocalRef(local_class);
    
    LOGI("JNI_OnLoad: Global class reference cached successfully.");
    return JNI_VERSION_1_6;
}

static int64_t now_ms() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now().time_since_epoch()).count();
}

// Helper to check the Vulkan backend properties without fully initializing llama context
extern "C" JNIEXPORT jstring JNICALL
Java_com_pocketai_app_services_LlamaCppService_llamaGetGpuName(JNIEnv *env, jobject thiz) {
    // HARD DISABLE VULKAN PROBING
    // Some drivers crash just by asking for device count.
    return env->NewStringUTF("");
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_pocketai_app_services_LlamaCppService_llamaInit(JNIEnv *env, jobject thiz, jstring modelPath_, jstring mmprojPath_, jboolean useGpu) {
    const char *model_path = env->GetStringUTFChars(modelPath_, nullptr);
    const char *mmproj_path = env->GetStringUTFChars(mmprojPath_, nullptr);

    LOGI("llamaInit(): Loading model %s and mmproj %s (GPU=%d)", model_path, mmproj_path, (int)useGpu);

    llama_backend_init();

    auto* wrapper = new LlamaContextWrapper();

    // Load model with mmap for faster loading and lower memory usage
    int64_t t0 = now_ms();
    llama_model_params model_params = llama_model_default_params();
    model_params.use_mmap = true;  // Memory-map model file for faster load
    if (useGpu) {
        model_params.n_gpu_layers = 100; // Offload all layers to GPU (Vulkan)
        LOGI("GPU inference enabled (Vulkan)");
    }
    wrapper->model = llama_model_load_from_file(model_path, model_params);
    if (!wrapper->model) {
        LOGE("Failed to load model file");
        delete wrapper;
        env->ReleaseStringUTFChars(modelPath_, model_path);
        env->ReleaseStringUTFChars(mmprojPath_, mmproj_path);
        return 0;
    }
    LOGI("Model loaded in %lld ms", (long long)(now_ms() - t0));

    // Initialize mtmd (vision encoder)
    t0 = now_ms();
    mtmd_context_params mtmd_params = mtmd_context_params_default();
    mtmd_params.n_threads = 8;
    mtmd_params.use_gpu = useGpu;
    mtmd_params.print_timings = true;
    mtmd_params.warmup = false;         // Skip warmup — saves 5-10s on first load
    mtmd_params.image_max_tokens = 256; // Reduced from 512 for much faster vision encoding on CPU

    wrapper->mtmd_ctx = mtmd_init_from_file(mmproj_path, wrapper->model, mtmd_params);
    if (!wrapper->mtmd_ctx) {
         LOGE("Failed to load mmproj file / init mtmd_context");
    }
    LOGI("mtmd context created in %lld ms", (long long)(now_ms() - t0));

    // Create llama context with appropriate settings for mobile
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 2048;      // Increased for better vision handling
    ctx_params.n_threads = 8;     // Use 8 threads to maximize CPU on octa-core devices
    ctx_params.n_threads_batch = 8;
    ctx_params.n_batch = 512;     // Increased batch for faster prompt eval
    ctx_params.flash_attn_type = LLAMA_FLASH_ATTN_TYPE_ENABLED; // Enable flash attention for speed

    wrapper->ctx = llama_init_from_model(wrapper->model, ctx_params);

    if (!wrapper->ctx) {
        LOGE("Failed to create llama context");
        if (wrapper->mtmd_ctx) mtmd_free(wrapper->mtmd_ctx);
        llama_model_free(wrapper->model);
        delete wrapper;
        env->ReleaseStringUTFChars(modelPath_, model_path);
        env->ReleaseStringUTFChars(mmprojPath_, mmproj_path);
        return 0;
    }

    LOGI("Model loaded successfully, n_ctx=%d, n_threads=4", (int)ctx_params.n_ctx);

    env->ReleaseStringUTFChars(modelPath_, model_path);
    env->ReleaseStringUTFChars(mmprojPath_, mmproj_path);

    return reinterpret_cast<jlong>(wrapper);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_pocketai_app_services_LlamaCppService_llamaRun(JNIEnv *env, jobject thiz, jlong ctx_ptr, jbyteArray imageBytes_, jstring prompt_, jobject callback) {
    auto* wrapper = reinterpret_cast<LlamaContextWrapper*>(ctx_ptr);
    if (!wrapper) return env->NewStringUTF("Error: Invalid context");

    const char *prompt = env->GetStringUTFChars(prompt_, nullptr);
    LOGI("llamaRun() called. Prompt length: %d", (int)strlen(prompt));

    // Resolve callback method using the globally cached class reference
    jmethodID onTokenMethod = nullptr;
    if (callback != nullptr) {
        if (g_service_class != nullptr) {
            onTokenMethod = env->GetMethodID(g_service_class, "onNativeToken", "(Ljava/lang/String;)V");
            if (onTokenMethod) {
                LOGI("Callback method 'onNativeToken' resolved successfully from cached class.");
            } else {
                // IMPORTANT: GetMethodID throws a NoSuchMethodError if it fails. We MUST clear it before calling any other JNI functions.
                env->ExceptionClear();
                LOGE("Failed to find 'onNativeToken' method in cached LlamaCppService class. Trying fallback...");
                
                // Emergency fallback to instance-based lookup
                jclass instClass = env->GetObjectClass(thiz);
                if (instClass) {
                    onTokenMethod = env->GetMethodID(instClass, "onNativeToken", "(Ljava/lang/String;)V");
                    if (onTokenMethod) {
                        LOGI("Resolved 'onNativeToken' via GetObjectClass fallback.");
                    } else {
                        env->ExceptionClear(); // Clear the exception from the fallback as well
                        LOGE("Fallback failed to find 'onNativeToken'. Callbacks will not work this session.");
                    }
                    env->DeleteLocalRef(instClass);
                }
            }
        } else {
            LOGE("CRITICAL: g_service_class is null in llamaRun. JNI_OnLoad may have failed.");
        }
    }

    std::string response = "";
    int64_t t_start = now_ms();

    mtmd_input_chunks* input_chunks = mtmd_input_chunks_init();
    mtmd_input_text input_text;
    input_text.add_special = true;
    input_text.parse_special = true;

    // Replace <image> with the mtmd marker
    std::string processed_prompt = prompt;
    size_t pos = processed_prompt.find("<image>");
    if (pos != std::string::npos) {
        processed_prompt.replace(pos, 7, mtmd_default_marker());
    }
    input_text.text = processed_prompt.c_str();

    std::vector<const mtmd_bitmap*> bitmaps;
    mtmd_bitmap* bitmap = nullptr;

    if (imageBytes_ != nullptr && wrapper->mtmd_ctx != nullptr) {
        jsize length = env->GetArrayLength(imageBytes_);
        jbyte* bytes = env->GetByteArrayElements(imageBytes_, nullptr);

        bitmap = mtmd_helper_bitmap_init_from_buf(wrapper->mtmd_ctx, (const unsigned char*)bytes, length);
        if (bitmap) {
            bitmaps.push_back(bitmap);
        }
        env->ReleaseByteArrayElements(imageBytes_, bytes, JNI_ABORT);
    }

    // Tokenize text + images into chunks
    int tok_result = mtmd_tokenize(wrapper->mtmd_ctx, input_chunks, &input_text, bitmaps.data(), bitmaps.size());

    if (tok_result != 0) {
        LOGE("mtmd_tokenize failed with code %d", tok_result);
    } else {
        llama_pos n_past = 0;
        int eval_result = mtmd_helper_eval_chunks(
            wrapper->mtmd_ctx, wrapper->ctx, input_chunks,
            n_past, 0, 64, true, &n_past);

        if (eval_result != 0) {
            LOGE("mtmd_helper_eval_chunks failed");
        } else {
            // Generate tokens
            llama_sampler* smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
            llama_sampler_chain_add(smpl, llama_sampler_init_greedy());

            LOGI("Starting token generation...");
            int64_t t0 = now_ms();
            int count = 0;
            while (count < 600) {
                llama_token new_token_id = llama_sampler_sample(smpl, wrapper->ctx, -1);
                if (llama_vocab_is_eog(llama_model_get_vocab(wrapper->model), new_token_id)) {
                    break;
                }

                char buf[256];
                int n = llama_token_to_piece(llama_model_get_vocab(wrapper->model), new_token_id, buf, sizeof(buf), 0, false);
                if (n > 0) {
                    std::string piece(buf, n);
                    response += piece;
                    
                    // STREAMING CALLBACK
                    if (onTokenMethod != nullptr) {
                        jstring jpiece = env->NewStringUTF(piece.c_str());
                        env->CallVoidMethod(thiz, onTokenMethod, jpiece);
                        env->DeleteLocalRef(jpiece);
                        
                        // Check for exceptions in Kotlin to prevent native crash
                        if (env->ExceptionCheck()) {
                            LOGE("Exception occurred during Kotlin callback 'onNativeToken'");
                            env->ExceptionDescribe();
                            env->ExceptionClear();
                        }
                    }
                }

                llama_batch batch = llama_batch_get_one(&new_token_id, 1);
                if (llama_decode(wrapper->ctx, batch) != 0) {
                    break;
                }
                n_past++;
                count++;
            }

            LOGI("Token generation completed: %d tokens", count);
            llama_sampler_free(smpl);
        }
    }

    if (bitmap) mtmd_bitmap_free(bitmap);
    mtmd_input_chunks_free(input_chunks);

    env->ReleaseStringUTFChars(prompt_, prompt);
    return env->NewStringUTF(response.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_pocketai_app_services_LlamaCppService_llamaFree(JNIEnv *env, jobject thiz, jlong ctx_ptr) {
    auto* wrapper = reinterpret_cast<LlamaContextWrapper*>(ctx_ptr);
    if (!wrapper) return;

    LOGI("llamaFree() called");
    if (wrapper->ctx) llama_free(wrapper->ctx);
    if (wrapper->mtmd_ctx) mtmd_free(wrapper->mtmd_ctx);
    if (wrapper->model) llama_model_free(wrapper->model);

    delete wrapper;
    llama_backend_free();
}
