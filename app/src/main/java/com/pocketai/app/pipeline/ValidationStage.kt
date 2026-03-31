package com.pocketai.app.pipeline

import com.pocketai.app.data.model.ReceiptData
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Validation Stage - Validates and normalizes extracted receipt data.
 * 
 * Input: ReceiptData (potentially incomplete)
 * Output: ReceiptData (validated and normalized)
 * 
 * Phase 10 Re-architecture: Centralized validation logic.
 */
class ValidationStage : PipelineStage<ReceiptData, ReceiptData> {
    
    override val name = "VALIDATE"
    
    override suspend fun process(input: ReceiptData): PipelineResult<ReceiptData> {
        val startTime = System.currentTimeMillis()
        
        return try {
            val errors = mutableListOf<String>()
            
            // We only fail if EVERYTHING is missing
            val hasAnyData = input.storeName.isNotBlank() || input.totalAmount > 0.0 || input.date.isNotBlank()
            
            val duration = System.currentTimeMillis() - startTime
            
            if (!hasAnyData) {
                return PipelineResult.Failure(
                    stageName = name,
                    durationMs = duration,
                    error = "Validation failed: No data extracted from receipt.",
                    partialData = input
                )
            }
            
            // Normalize data
            val normalized = input.copy(
                storeName = input.storeName.trim().take(100),
                date = normalizeDate(input.date),
                category = input.category.ifBlank { "Other" }
            )
            
            PipelineResult.Success(
                stageName = name,
                durationMs = duration,
                data = normalized
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            PipelineResult.Failure(
                stageName = name,
                durationMs = duration,
                error = "Validation exception: ${e.message}",
                exception = e
            )
        }
    }
    
    /**
     * Normalize date to yyyy-MM-dd format
     */
    private fun normalizeDate(date: String): String {
        val inputFormats = listOf(
            "yyyy-MM-dd", "yyyy/MM/dd", // ISO first
            "dd-MM-yyyy", "dd.MM.yyyy", "dd/MM/yyyy", // Common EU
            "MM-dd-yyyy", "MM/dd/yyyy", // US
            "dd-MM-yy", "dd.MM.yy", "dd/MM/yy" // Short
        )
        
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        for (format in inputFormats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.isLenient = false // CRITICAL: Prevent 2026-01-10 matching dd-MM-yyyy
                val parsed = sdf.parse(date)
                if (parsed != null) {
                    return outputFormat.format(parsed)
                }
            } catch (e: Exception) {
                // Try next format
            }
        }
        
        // Return original if no format matches
        return date
    }
}
