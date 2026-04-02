package com.pocketai.app.services

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Legacy Service for llama.cpp (Stubbed in v1.5.0).
 * Replaced by LiteRTService for hardware-accelerated performance.
 */
@Singleton
class LlamaCppService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _status = MutableStateFlow("Deprecated (Use LiteRTService)")
    val status: StateFlow<String> = _status.asStateFlow()

    fun prewarm() {
        Log.i("LlamaCppService", "Prewarm called on legacy service. Ignoring.")
    }

    suspend fun generateResponse(
        image: Bitmap?, 
        prompt: String,
        onTokenReceived: ((String) -> Unit)? = null
    ): String? {
        Log.e("LlamaCppService", "Legacy generateResponse called! Please migrate to LiteRTService.")
        return null
    }

    fun close() {}
}
