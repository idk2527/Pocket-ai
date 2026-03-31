package com.pocketai.app.services

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.pocketai.app.data.model.Expense
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to export expenses to CSV file in Downloads folder.
 */
@Singleton
class ExportService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Export expenses to CSV file.
     * @return The file path if successful, null otherwise.
     */
    suspend fun exportToCsv(expenses: List<Expense>): Result<String> {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "pocket_ai_expenses_$timestamp.csv"
            
            val csvContent = buildCsvContent(expenses)
            
            val filePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ uses MediaStore
                saveToMediaStore(fileName, csvContent)
            } else {
                // Legacy approach
                saveToLegacyDownloads(fileName, csvContent)
            }
            
            Result.success(filePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun buildCsvContent(expenses: List<Expense>): String {
        val sb = StringBuilder()
        
        // Header
        sb.appendLine("Date,Store,Category,Amount,Currency,Note")
        
        // Data rows
        expenses.forEach { expense ->
            val note = expense.note?.replace("\"", "\"\"")?.replace(",", " ") ?: ""
            sb.appendLine("\"${expense.date}\",\"${expense.storeName}\",\"${expense.category}\",${expense.amount},EUR,\"$note\"")
        }
        
        return sb.toString()
    }
    
    private fun saveToMediaStore(fileName: String, content: String): String {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw Exception("Failed to create file in Downloads")
        
        resolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content.toByteArray())
        } ?: throw Exception("Failed to open output stream")
        
        return "Downloads/$fileName"
    }
    
    private fun saveToLegacyDownloads(fileName: String, content: String): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        
        FileOutputStream(file).use { outputStream ->
            outputStream.write(content.toByteArray())
        }
        
        return file.absolutePath
    }
}
