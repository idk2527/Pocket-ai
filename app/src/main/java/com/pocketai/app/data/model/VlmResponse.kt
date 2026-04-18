package com.pocketai.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * v2.0 DTO for LiteRT-LM Multimodal Response.
 * Using SerializedName to ensure stability against R8/Proguard obfuscation.
 */
data class VlmResponse(
    @SerializedName("vendor") val vendor: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("total") val total: Double?,
    @SerializedName("payment") val payment: String?,
    @SerializedName("items") val items: List<VlmItem?>?
)

data class VlmItem(
    @SerializedName("n") val n: String?,      // Item Name
    @SerializedName("p") val p: Double?,      // Price
    @SerializedName("num") val num: String?   // Product Code / ID
)
