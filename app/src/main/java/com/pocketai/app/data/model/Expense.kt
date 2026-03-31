package com.pocketai.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val storeName: String,
    val amount: Double,
    val date: String,
    val category: String,
    val note: String? = null,
    val receiptImagePath: String? = null,
    val currency: String? = null,
    val createdAt: String,
    // Phase 17: Veryfi-style enriched fields
    val merchantAddress: String? = null,
    val vatId: String? = null,
    val receiptNumber: String? = null,
    val cashier: String? = null,
    val paymentMethod: String? = null,
    val subtotal: Double? = null,
    val vatAmount: Double? = null,
    val vatRate: String? = null,
    val itemsJson: String? = null,       // Serialized JSON of line items
    val rawExtractedJson: String? = null  // Full VLM response for digital receipt
)
