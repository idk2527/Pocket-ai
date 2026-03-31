package com.pocketai.app.pipeline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.pocketai.app.data.model.Expense
import com.pocketai.app.data.model.ExtractionSource
import com.pocketai.app.data.model.ReceiptData
import com.pocketai.app.services.LlamaCppService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * v2.0 Receipt Processing Pipeline
 * Phase 17: Veryfi-style comprehensive German receipt extraction via Gemma 3n VLM.
 */
@Singleton
class ReceiptPipeline @Inject constructor(
    @ApplicationContext private val context: Context,
    private val inferenceService: LlamaCppService
) {
    private val TAG = "ReceiptPipeline"
    private val gson = Gson()
    
    // Pipeline components
    private val validationStage = ValidationStage()
    
    // State
    private val _state = MutableStateFlow<PipelineState>(PipelineState.Idle)
    val state: StateFlow<PipelineState> = _state.asStateFlow()

    private val _partialResult = MutableStateFlow("")
    val partialResult: StateFlow<String> = _partialResult.asStateFlow()

    // Last raw JSON response (preserved for digital receipt display)
    private var _lastRawJson: String? = null
    val lastRawJson: String? get() = _lastRawJson
    
    suspend fun processImage(imageUri: Uri): PipelineOutput = withContext(Dispatchers.IO) {
        val pipelineStart = System.currentTimeMillis()
        val stageTimings = mutableMapOf<String, Long>()
        
        try {
            _state.value = PipelineState.Processing("Scanning Receipt")
            _partialResult.value = ""
            PipelineLogger.pipelineStart("Image: $imageUri")

            // 1. Load/Resize (448px — balance between speed on CPU and detail for OCR)
            _state.value = PipelineState.Processing("Loading image...")
            val bitmap = loadResizedBitmap(imageUri, 448) 
                ?: throw IllegalStateException("Failed to load image")
            _state.value = PipelineState.Processing("Image: ${bitmap.width}x${bitmap.height}")
            
            // 2. VLM Inference — with elapsed timer and timeout
            val inferenceStart = System.currentTimeMillis()
            
            // Launch a timer coroutine that updates UI every second
            val timerJob = CoroutineScope(Dispatchers.Default).launch {
                var elapsed = 0
                while (true) {
                    _state.value = PipelineState.Processing("AI processing... ${elapsed}s")
                    kotlinx.coroutines.delay(1000)
                    elapsed++
                }
            }
            
            // Run inference with a 120-second timeout
            val prompt = buildExtractionPrompt()
            val jsonResponse = try {
                kotlinx.coroutines.withTimeout(120_000) {
                    inferenceService.generateResponse(bitmap, prompt) { token ->
                        _partialResult.value += token
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e(TAG, "VLM inference timed out after 120 seconds")
                timerJob.cancel()
                bitmap.recycle()
                throw Exception("AI processing timed out (>2min). The model may be too slow on this device.")
            }
            
            timerJob.cancel()
            
            val inferenceTime = System.currentTimeMillis() - inferenceStart
            stageTimings["VLM Inference"] = inferenceTime
            Log.i(TAG, "VLM inference completed in ${inferenceTime}ms")
            _state.value = PipelineState.Processing("Done! (${inferenceTime/1000}s)")
            
            bitmap.recycle()

            if (jsonResponse.isNullOrBlank()) {
                throw Exception("Empty VLM response")
            }

            // Store raw response for digital receipt display
            _lastRawJson = jsonResponse

            // 3. Parse enriched response
            _state.value = PipelineState.Processing("Extracting Data")
            val rawData = try {
                parseVlmJson(jsonResponse)
            } catch (e: Exception) {
                Log.e(TAG, "Parsing failed but raw JSON exists", e)
                // Fallback: Create minimal ReceiptData with rawJson preserved
                ReceiptData(
                    storeName = "Parsing Error",
                    totalAmount = 0.0,
                    date = "",
                    category = "Other",
                    confidence = 0f,
                    source = ExtractionSource.VLM,
                    rawJson = jsonResponse // CRITICAL: Save this so we can debug/view it
                )
            }
            
            // 4. Validate (skip if we already know it's a fallback)
            val validationResult = if (rawData.storeName == "Parsing Error") {
                PipelineResult.Success(
                    stageName = "Parsing Fallback",
                    durationMs = 0,
                    data = rawData
                ) // Treat as success so we save the raw data
            } else {
                validationStage.executeWithLogging(rawData)
            }
            
            stageTimings["Validation"] = validationResult.durationMs
            
            val totalTime = System.currentTimeMillis() - pipelineStart
            
            if (validationResult is PipelineResult.Success) {
                PipelineLogger.pipelineComplete(totalTime, stageTimings, true)
                _state.value = PipelineState.Success(validationResult.data)
                return@withContext PipelineOutput.success(validationResult.data, totalTime, stageTimings)
            } else {
                // Even on validation failure, try to salvage partial data
                // Cast partialData if available, otherwise just fail
                val partialData = (validationResult as PipelineResult.Failure).partialData as? ReceiptData
                
                if (partialData != null) {
                    PipelineLogger.pipelineComplete(totalTime, stageTimings, false) // Log as failed but return success
                    _state.value = PipelineState.Success(partialData)
                    return@withContext PipelineOutput.success(partialData, totalTime, stageTimings)
                } else {
                    val error = validationResult.error
                    PipelineLogger.pipelineComplete(totalTime, stageTimings, false)
                    _state.value = PipelineState.Failed(error)
                    return@withContext PipelineOutput.failure(error, totalTime, stageTimings)
                }
            }

        } catch (e: Exception) {
            val totalTime = System.currentTimeMillis() - pipelineStart
            Log.e(TAG, "Pipeline failed", e)
            PipelineLogger.pipelineComplete(totalTime, stageTimings, false)
            _state.value = PipelineState.Failed(e.message ?: "Unknown error")
            return@withContext PipelineOutput.failure(e.message ?: "Pipeline Error", totalTime, stageTimings)
        }
    }

    private fun buildExtractionPrompt(): String = """
Extract information from the receipt as a strictly formatted JSON object. 
Read carefully even small text to find:
- Store name (vendor)
- All purchased items: exact name (n), price (p), and product code (num) if present.
- Total amount (total)
- Date of purchase (date) in YYYY-MM-DD
- Payment type (payment): "Card" or "Cash"

Use this exact JSON structure:
{
  "vendor": "Name",
  "total": 0.00,
  "date": "YYYY-MM-DD",
  "payment": "Card/Cash",
  "items": [
    {"n": "item", "p": 0.00, "num": "code"}
  ]
}
""".trimIndent()

    private fun loadResizedBitmap(uri: Uri, maxDim: Int): Bitmap? {
        return try {
            val stream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, options)
            stream?.close()

            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDim || options.outHeight / sampleSize > maxDim) {
                sampleSize *= 2
            }

            val stream2 = context.contentResolver.openInputStream(uri)
            val options2 = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeStream(stream2, null, options2)
            stream2?.close()
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap", e)
            null
        }
    }

    /**
     * Parse enriched VLM JSON response into ReceiptData
     */
    private fun parseVlmJson(json: String): ReceiptData {
        var cleanJson = json.replace("```json", "").replace("```", "").trim()
        if (cleanJson.contains("{")) {
            cleanJson = cleanJson.substring(cleanJson.indexOf("{"), cleanJson.lastIndexOf("}") + 1)
        }

        try {
            val raw = gson.fromJson(cleanJson, VlmResponse::class.java)

            // Map items (Lite schema: n=name, p=price, num=product number)
            val items = raw.items?.mapNotNull { item ->
                item ?: return@mapNotNull null
                val unitP = item.p ?: 0.0
                ReceiptData.LineItem(
                    name = item.n ?: "Unknown Item",
                    quantity = 1, // Reset to 1 explicitly for lite extraction
                    unitPrice = unitP,
                    totalPrice = unitP,
                    productNumber = item.num,
                    vatRate = null,
                    discount = null
                )
            } ?: emptyList()

            // Map payment (Lite schema: simple string)
            val payments = if (raw.payment != null) {
                listOf(ReceiptData.PaymentInfo(method = raw.payment, amount = raw.total ?: 0.0))
            } else emptyList()

            return ReceiptData(
                storeName = raw.vendor ?: "Unknown",
                date = raw.date ?: "",
                totalAmount = raw.total ?: 0.0,
                currency = "EUR",
                category = "Groceries", // Default for Lite mode
                items = items,
                confidence = if (items.isNotEmpty()) 0.95f else 0.80f,
                source = ExtractionSource.VLM,
                merchantAddress = null,
                vatId = null,
                receiptNumber = null,
                cashier = null,
                subtotal = raw.total, // Approx for Lite
                vatAmount = null,
                vatRate = null,
                payments = payments,
                rawJson = cleanJson
            )
        } catch (e: Exception) {
            Log.e(TAG, "JSON Parse Error: $cleanJson", e)
            throw Exception("JSON Parse Error: ${e.message}")
        }
    }

    fun reset() {
        _state.value = PipelineState.Idle
        _lastRawJson = null
    }

    // --- VLM Response data classes (Lite Mode) ---

    private data class VlmResponse(
        val vendor: String?,
        val date: String?,
        val total: Double?,
        val payment: String?, // Simple string in Lite mode
        val items: List<VlmItem?>?
    )

    private data class VlmItem(
        val n: String?, // name
        val p: Double?, // price
        val num: String?  // product number / id
    )
}

sealed class PipelineState {
    object Idle : PipelineState()
    data class Processing(val stage: String) : PipelineState()
    data class Success(val data: ReceiptData) : PipelineState()
    data class Failed(val error: String) : PipelineState()
}

data class PipelineOutput(
    val success: Boolean,
    val data: ReceiptData?,
    val expense: Expense?,
    val error: String?,
    val totalTimeMs: Long,
    val stageTimings: Map<String, Long>
) {
    companion object {
        fun success(data: ReceiptData, totalTimeMs: Long, stageTimings: Map<String, Long>): PipelineOutput {
            return PipelineOutput(
                success = true,
                data = data,
                expense = data.toExpense(),
                error = null,
                totalTimeMs = totalTimeMs,
                stageTimings = stageTimings
            )
        }
        
        fun failure(error: String, totalTimeMs: Long, stageTimings: Map<String, Long>): PipelineOutput {
            return PipelineOutput(
                success = false,
                data = null,
                expense = null,
                error = error,
                totalTimeMs = totalTimeMs,
                stageTimings = stageTimings
            )
        }
    }
}
