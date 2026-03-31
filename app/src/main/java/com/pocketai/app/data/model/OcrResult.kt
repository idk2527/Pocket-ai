package com.pocketai.app.data.model

data class OcrResult(
    val storeName: ParsedField<String>?,
    val totalAmount: ParsedField<Double>?,
    val date: ParsedField<String>?,
    val category: ParsedField<String>?
)

data class ParsedField<T>(
    val value: T,
    val confidence: Confidence
)

enum class Confidence {
    HIGH, MEDIUM, LOW, NONE
}