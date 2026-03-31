package com.pocketai.app.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Creates a content Uri for saving a photo capture.
 * Uses app-private internal storage ONLY — never saves to gallery/MediaStore.
 * Files are stored in: app_data/receipts/receipt_<timestamp>.jpg
 */
fun createImageUri(context: Context): Uri {
    val fileName = "receipt_${System.currentTimeMillis()}.jpg"
    val receiptsDir = File(context.filesDir, "receipts")
    if (!receiptsDir.exists()) {
        receiptsDir.mkdirs()
    }
    val file = File(receiptsDir, fileName)
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}

/**
 * Copies a content Uri (e.g. from ML Kit scanner) to app-private storage.
 * Returns the new private Uri, or null on failure.
 */
fun copyToPrivateStorage(context: Context, sourceUri: Uri): Uri? {
    return try {
        val fileName = "receipt_${System.currentTimeMillis()}.jpg"
        val receiptsDir = File(context.filesDir, "receipts")
        if (!receiptsDir.exists()) {
            receiptsDir.mkdirs()
        }
        val destFile = File(receiptsDir, fileName)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        FileProvider.getUriForFile(context, "${context.packageName}.provider", destFile)
    } catch (e: Exception) {
        android.util.Log.e("ImageUtils", "Failed to copy to private storage", e)
        null
    }
}
