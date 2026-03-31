package com.pocketai.app.data.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Unified receipt data model - the single source of truth for extracted receipt information.
 * Phase 17: Extended for Veryfi-style full receipt digitalization.
 */
data class ReceiptData(
    val storeName: String,
    val totalAmount: Double,
    val date: String,
    val category: String,
    val currency: String = "EUR",
    val items: List<LineItem> = emptyList(),
    val confidence: Float,
    val source: ExtractionSource,
    val rawText: String? = null,
    // Phase 17: Enriched fields
    val merchantAddress: String? = null,
    val vatId: String? = null,
    val receiptNumber: String? = null,
    val cashier: String? = null,
    val subtotal: Double? = null,
    val vatAmount: Double? = null,
    val vatRate: String? = null,
    val payments: List<PaymentInfo> = emptyList(),
    val rawJson: String? = null  // Full VLM response preserved
) {
    /**
     * Individual line item from receipt
     */
    data class LineItem(
        val name: String,
        val quantity: Int = 1,
        val unitPrice: Double,
        val totalPrice: Double = unitPrice * quantity,
        val productNumber: String? = null,
        val vatRate: String? = null,
        val discount: Double? = null
    )

    /**
     * Payment method information
     */
    data class PaymentInfo(
        val method: String,        // "Bar", "EC-Karte", "Kreditkarte"
        val amount: Double,
        val cardLastFour: String? = null
    )

    /**
     * Validation - checks if the receipt has minimum required data
     */
    fun isValid(): Boolean {
        return storeName.isNotBlank() && date.isNotBlank()
    }

    /**
     * Convert to Expense entity for database storage
     */
    fun toExpense(receiptImagePath: String? = null): Expense {
        val gson = Gson()
        return Expense(
            storeName = storeName,
            amount = totalAmount,
            date = date,
            category = category.ifBlank { "Other" },
            currency = currency,
            note = buildNote(),
            receiptImagePath = receiptImagePath,
            createdAt = System.currentTimeMillis().toString(),
            merchantAddress = merchantAddress,
            vatId = vatId,
            receiptNumber = receiptNumber,
            cashier = cashier,
            paymentMethod = payments.firstOrNull()?.method,
            subtotal = subtotal,
            vatAmount = vatAmount,
            vatRate = vatRate,
            itemsJson = if (items.isNotEmpty()) gson.toJson(items) else null,
            rawExtractedJson = rawJson
        )
    }

    private fun buildNote(): String {
        val parts = mutableListOf<String>()
        parts.add("Extracted via ${source.displayName}")
        if (confidence < 0.7f) {
            parts.add("Low confidence (${(confidence * 100).toInt()}%)")
        }
        if (items.isNotEmpty()) {
            parts.add("${items.size} items")
        }
        return parts.joinToString(" · ")
    }

    companion object {
        fun empty(): ReceiptData = ReceiptData(
            storeName = "",
            totalAmount = 0.0,
            date = "",
            category = "Other",
            confidence = 0f,
            source = ExtractionSource.MANUAL
        )

        /**
         * Deserialize line items from JSON (for displaying from Expense)
         */
        fun parseItemsJson(json: String?): List<LineItem> {
            if (json.isNullOrBlank()) return emptyList()
            return try {
                val gson = Gson()
                val type = object : TypeToken<List<LineItem>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

/**
 * Source of extraction - tracks how the data was obtained
 */
enum class ExtractionSource(val displayName: String) {
    LLM("AI"),
    HEURISTIC("Pattern Matching"),
    MANUAL("Manual Entry"),
    HYBRID("AI + Pattern Matching"),
    VLM("Vision AI")
}
