package com.pocketai.app.services

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Backend
import java.io.ByteArrayOutputStream
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRTService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var engine: Engine? = null
    private var activeConversation: Conversation? = null
    private val TAG = "LiteRTService"

    private val _status = MutableStateFlow("Engine Ready")
    val status: StateFlow<String> = _status

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded

    /**
     * Initializes the LiteRT-LM engine with correct 2026 API names.
     */
    suspend fun initialize(modelPath: String) = withContext(Dispatchers.IO) {
        try {
            _status.value = "Initializing NPU (Burst Mode)..."
            
            val cacheDir = File(context.cacheDir, "litert_aot_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            var config = EngineConfig(
                modelPath = modelPath,
                backend = Backend.NPU(),
                maxNumTokens = 2048,
                cacheDir = cacheDir.absolutePath
            )

            try {
                engine = Engine(config)
                engine?.initialize()
            } catch (e: Exception) {
                Log.w(TAG, "NPU Initialization failed, falling back to CPU. Reason: ${e.message}")
                // Fallback to CPU if NPU is unsupported on this device
                config = EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.CPU(),
                    maxNumTokens = 2048,
                    cacheDir = cacheDir.absolutePath
                )
                engine = Engine(config)
                engine?.initialize()
            }
            
            activeConversation = engine?.createConversation(com.google.ai.edge.litertlm.ConversationConfig())
            
            _isLoaded.value = true
            _status.value = "LiteRT-LM Ready (Fallback/Active)"
            Log.i(TAG, "LiteRT-LM Engine loaded successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize LiteRT-LM completely", e)
            _status.value = "Error: ${e.message}"
            _isLoaded.value = false
        }
    }

    /**
     * Ensures the model is loaded before any inference.
     */
    private suspend fun ensureLoaded() {
        if (engine != null && activeConversation != null) return
        
        val modelFile = File(context.filesDir, "model_multimodal.litertlm")
        if (modelFile.exists()) {
            initialize(modelFile.absolutePath)
        } else {
            throw IllegalStateException("Model not found. Please download it first.")
        }
    }

    /**
     * Runs multimodal inference (Image + Text) using the correct 1.0 SDK API.
     */
    suspend fun generateResponseWithImage(
        prompt: String,
        image: Bitmap? = null,
        onToken: (String) -> Unit
    ): String = withContext(Dispatchers.Default) {
        try {
            ensureLoaded()
        } catch (e: Exception) {
            throw Exception("Model initialization failed: ${e.message}", e)
        }
        
        val session = activeConversation ?: throw IllegalStateException("Session not active. Status: ${_status.value}")

        try {
            val parts = mutableListOf<Content>()
            parts.add(Content.Text(prompt))

            if (image != null) {
                val stream = ByteArrayOutputStream()
                image.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                parts.add(Content.ImageBytes(stream.toByteArray()))
            }

            val result = StringBuilder()
            session.sendMessageAsync(Contents.of(parts)).collect { message ->
                message.contents.contents.filterIsInstance<Content.Text>().forEach { textContent ->
                    result.append(textContent.text)
                    onToken(textContent.text)
                }
            }

            result.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Inference error", e)
            "Error during generation: ${e.message}"
        }
    }

    fun release() {
        activeConversation = null
        engine?.close()
        engine = null
        _isLoaded.value = false
    }
}
