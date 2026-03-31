package com.pocketai.app.services

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.pocketai.app.data.preferences.PreferencesManager

/**
 * Service for running multimodal LLM inference using llama.cpp and Qwen3-VL via JNI.
 */
@Singleton
class LlamaCppService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "LlamaCppService"

    // Real-time status reporting
    private val _status = MutableStateFlow("Idle")
    val status: StateFlow<String> = _status.asStateFlow()

    // Load the native library compiled by NDK
    init {
        try {
            System.loadLibrary("llama_jni")
            _status.value = "Native library loaded"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load libllama_jni.so", e)
            _status.value = "ERROR: Failed to load native library"
        }
    }

    private var nativeContext: Long = 0
    private val initLock = Object()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Pre-warm the model: extract files from assets and load into memory.
     * Call this from MainActivity.onCreate() so the model is ready when needed.
     */
    fun prewarm() {
        serviceScope.launch {
            try {
                _status.value = "Prewarming model..."
                Log.i(TAG, "Prewarming: starting model load in background...")
                val t0 = System.currentTimeMillis()
                ensureModelLoaded()
                val duration = System.currentTimeMillis() - t0
                _status.value = "Model ready (${duration / 1000}s)"
                Log.i(TAG, "Prewarm complete in ${duration}ms — model ready!")
            } catch (e: Exception) {
                _status.value = "ERROR: ${e.message}"
                Log.e(TAG, "Prewarm failed: ${e.message}", e)
            }
        }
    }

    // JNI Declarations
    private external fun llamaInit(
        modelPath: String,
        mmprojPath: String,
        useGpu: Boolean
    ): Long

    /**
     * Internal JNI call that generates tokens.
     * @param onToken A callback method name or object if we want to stream.
     * We'll use this synchronized to ensure only one run at a time.
     */
    private external fun llamaRun(ctx: Long, imageBytes: ByteArray?, prompt: String, callback: Any?): String
    private external fun llamaFree(ctx: Long)

    private external fun llamaGetGpuName(): String

    private var currentTokenCallback: ((String) -> Unit)? = null

    /**
     * Callback method called from C++ JNI when a new token is generated.
     */
    @Suppress("Unused")
    private fun onNativeToken(token: String) {
        currentTokenCallback?.invoke(token)
    }


    suspend fun generateResponse(
        image: Bitmap?, 
        prompt: String,
        onTokenReceived: ((String) -> Unit)? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            _status.value = "Loading model..."
            ensureModelLoaded()
            
            val ctx = synchronized(initLock) { nativeContext }
            if (ctx == 0L) {
                _status.value = "ERROR: Model failed to load"
                throw IllegalStateException("llama.cpp failed to initialize context.")
            }

            // Compress bitmap to bytes to send across JNI
            _status.value = "Compressing image..."
            var imageBytes: ByteArray? = null
            if (image != null) {
                val stream = ByteArrayOutputStream()
                image.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                imageBytes = stream.toByteArray()
                _status.value = "Image: ${image.width}x${image.height} (${imageBytes.size / 1024}KB)"
            }

            val formattedPrompt = getFormattedPrompt(prompt, image != null)
            Log.d(TAG, "Running llama inference with prompt length: ${formattedPrompt.length}")
            
            _status.value = "Running VLM inference..."
            val startTime = System.currentTimeMillis()
            
            currentTokenCallback = onTokenReceived
            try {
                // Pass 'this@LlamaCppService' so native code can call 'onNativeToken'
                val response = llamaRun(ctx, imageBytes, formattedPrompt, this@LlamaCppService)
                val duration = System.currentTimeMillis() - startTime
                
                _status.value = "Done! (${duration / 1000}s)"
                Log.d(TAG, "Inference completed in ${duration}ms")
                
                return@withContext response
            } finally {
                currentTokenCallback = null
            }
        } catch (e: Exception) {
            _status.value = "ERROR: ${e.message}"
            Log.e(TAG, "Llama generation failed: ${e.message}", e)
            return@withContext null
        }
    }

    private suspend fun ensureModelLoaded() {
        if (synchronized(initLock) { nativeContext != 0L }) return
        
        _status.value = "Loading model files..."
        Log.i(TAG, "Initializing llama.cpp with Qwen3-VL")
        val modelFile = getModelFile("Qwen3.5-0.8B-Q4_K_M.gguf")
        val mmprojFile = getModelFile("mmproj-F16.gguf")

        if (modelFile == null || mmprojFile == null) {
            _status.value = "ERROR: Model files not downloaded"
            throw IllegalStateException("Model files not found. Please download them first.")
        }

        val modelPath = modelFile.absolutePath
        val mmprojPath = mmprojFile.absolutePath
        
        // 1. Check GPU vendor (Mali GPUs are known to be slow with our Vulkan shaders)
        val gpuName = try {
            llamaGetGpuName()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get GPU name via JNI", e)
            ""
        }
        
        val useGpu = gpuName.isNotEmpty() && !gpuName.contains("Mali", ignoreCase = true)
        
        _status.value = "Loading model (GPU=${if (useGpu) "ON" else "OFF"})..."
        Log.i(TAG, "GPU Detection: Built-in GPU reported as '$gpuName'.")
        Log.i(TAG, "Initializing llama.cpp with GPU=$useGpu")
        
        val ctx = llamaInit(modelPath, mmprojPath, useGpu)
        
        synchronized(initLock) {
            nativeContext = ctx
        }
        
        if (ctx == 0L) {
            _status.value = "ERROR: llamaInit returned 0"
            throw IllegalStateException("llamaInit returned 0 (failed to load). Check paths or memory.")
        }
        _status.value = "Model loaded successfully"
    }

    /**
     * Looks for a downloaded model file in internal storage.
     */
    private fun getModelFile(filename: String): File? {
        val file = File(context.filesDir, filename)
        return if (file.exists() && file.length() > 0) file else null
    }

    /**
     * Wrap prompt in Qwen3-VL (ChatML) specific template.
     */
    private fun getFormattedPrompt(prompt: String, hasImage: Boolean): String {
        return if (hasImage) {
            """<|im_start|>system
You are a highly efficient receipt parser.
Do NOT use <think> tags. Do NOT explain. Disable all reasoning.
Extract receipt as STRICT JSON. Output ONLY JSON.
<|im_end|>
<|im_start|>user
<image>
${prompt}
<|im_end|>
<|im_start|>assistant
```json
"""
        } else {
            // For chat and text-only queries, bypass double-wrapping if the ViewModel passes raw ChatML.
            if (prompt.startsWith("<|im_start|>")) {
                prompt
            } else {
                """<|im_start|>user
${prompt}
<|im_end|>
<|im_start|>assistant
"""
            }
        }
    }


    fun close() {
        synchronized(initLock) {
            if (nativeContext != 0L) {
                llamaFree(nativeContext)
                nativeContext = 0
            }
        }
    }
}
