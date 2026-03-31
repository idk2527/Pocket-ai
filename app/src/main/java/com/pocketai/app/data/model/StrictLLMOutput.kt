package com.pocketai.app.data.model

import com.google.gson.annotations.SerializedName

data class StrictLLMOutput(
    @SerializedName("store_name") val storeName: String?,
    @SerializedName("total_amount") val totalAmount: Double?,
    @SerializedName("date") val date: String?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("items") val items: List<ReceiptItem>?,
    @SerializedName("confidence") val confidence: ConfidenceScore?
) {
    data class ReceiptItem(
        @SerializedName("name") val name: String?,
        @SerializedName("quantity") val quantity: Int?,
        @SerializedName("unit_price") val unitPrice: Double?
    )

    data class ConfidenceScore(
        @SerializedName("overall") val overall: String?,
        @SerializedName("store_name") val storeName: String?,
        @SerializedName("total_amount") val totalAmount: String?,
        @SerializedName("date") val date: String?,
        @SerializedName("currency") val currency: String?,
        @SerializedName("category") val category: String?
    )

    // Confidence levels
    enum class ConfidenceLevel {
        HIGH, MEDIUM, LOW, NONE
    }

    // Extension function to convert to Expense
    fun toExpense(): Expense? {
        // Sanitize inputs
        val sanitizedStoreName = storeName?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val sanitizedTotalAmount = totalAmount ?: return null
        val sanitizedDate = date?.trim()?.takeIf { it.isNotBlank() } ?: return null
        
        if (sanitizedTotalAmount <= 0) return null
        
        val category = category?.trim()?.takeIf { it.isNotBlank() } ?: "Other"
        val currencySymbol = currency?.trim()?.takeIf { it.isNotBlank() } ?: ""
        
        return Expense(
            storeName = sanitizedStoreName,
            amount = sanitizedTotalAmount,
            date = sanitizedDate,
            category = category,
            note = "LLM Extracted${if (confidence?.overall?.uppercase() == ConfidenceLevel.LOW.name) " - Low Confidence" else ""}",
            currency = currencySymbol,
            receiptImagePath = null,
            createdAt = System.currentTimeMillis().toString()
        )
    }

    // Validation
    fun isValid(): Boolean {
        return storeName?.isNotBlank() == true 
            && totalAmount != null 
            && totalAmount > 0 
            && date?.isNotBlank() == true
    }

    // Sanitize invalid characters
    private fun String.sanitize(): String {
        return this.replace(Regex("[^a-zA-Z0-9.,\\s\\-]"), "")
    }

    // Fallback parsing for when LLM fails
    companion object {
        fun createFallback(ocrText: String): StrictLLMOutput {
            // Try to extract basic info from OCR text
            val totalAmount = extractTotal(ocrText)
            val storeName = extractStoreName(ocrText)
            val date = extractDate(ocrText)
            
            return StrictLLMOutput(
                storeName = storeName,
                totalAmount = totalAmount,
                date = date,
                currency = "EUR", // Default currency
                category = "Other",
                items = emptyList(),
                confidence = ConfidenceScore(
                    overall = ConfidenceLevel.MEDIUM.name,
                    storeName = ConfidenceLevel.MEDIUM.name,
                    totalAmount = totalAmount?.let { ConfidenceLevel.HIGH.name } ?: ConfidenceLevel.NONE.name,
                    date = date?.let { ConfidenceLevel.MEDIUM.name } ?: ConfidenceLevel.NONE.name,
                    currency = ConfidenceLevel.HIGH.name,
                    category = ConfidenceLevel.HIGH.name
                )
            )
        }

        private fun extractTotal(text: String): Double? {
            val regex = Regex("""(?:total|sum|amount|paid)\s*[:\-]?\s*€?(\d+[.,]\d{2})""", RegexOption.IGNORE_CASE)
            return regex.find(text)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()
        }

        private fun extractStoreName(text: String): String? {
            // Simple heuristic - look for capitalized words that might be store names
            val words = text.split(Regex("""[\s\-]+"""))
            return words.filter { it.isNotBlank() && it[0].isUpperCase() && it.length > 2 }
                .takeIf { it.isNotEmpty() }?.first()
        }

        private fun extractDate(text: String): String? {
            // Try to find date patterns
            val dateRegex = Regex("""\b(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{2,4})\b""")
            return dateRegex.find(text)?.value
        }
    }
}