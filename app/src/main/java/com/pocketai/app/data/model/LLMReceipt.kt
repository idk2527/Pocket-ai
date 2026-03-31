package com.pocketai.app.data.model

import com.google.gson.annotations.SerializedName

// Main structure for the LLM's JSON output
data class LLMReceipt(
    val store: ParsedField<String>,
    val date: ParsedField<String>,
    val total: ParsedField<String>,
    val category: ParsedField<String>,
    val items: List<Item>
)

// Represents a single item on the receipt
data class Item(
    val name: String,
    val qty: Int,
    val price: String
)
