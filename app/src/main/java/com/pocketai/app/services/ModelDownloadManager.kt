package com.pocketai.app.services

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadProgress(
    val currentFile: String = "",
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val fileIndex: Int = 0,
    val totalFiles: Int = 0,
    val isComplete: Boolean = false,
    val error: String? = null
)

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "ModelDownloadManager"
    
    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: StateFlow<DownloadProgress> = _progress.asStateFlow()

    companion object {
        private val BASE_URL get() = "${com.pocketai.app.BuildConfig.FIREBASE_HOSTING_URL}/models/"
        
        val MODEL_FILES = listOf(
            "Qwen3.5-0.8B-LiteRT.bin" to 682_517_120L
        )
    }

    fun areModelsDownloaded(): Boolean {
        return MODEL_FILES.all { (name, _) ->
            val file = File(context.filesDir, name)
            file.exists() && file.length() > 0
        }
    }

    suspend fun downloadModels() = withContext(Dispatchers.IO) {
        try {
            MODEL_FILES.forEachIndexed { index, (filename, expectedSize) ->
                val destFile = File(context.filesDir, filename)
                
                if (destFile.exists() && destFile.length() > 0) {
                    Log.i(TAG, "$filename already exists, skipping")
                    _progress.value = DownloadProgress(
                        currentFile = filename,
                        bytesDownloaded = destFile.length(),
                        totalBytes = destFile.length(),
                        fileIndex = index + 1,
                        totalFiles = MODEL_FILES.size
                    )
                    return@forEachIndexed
                }

                Log.i(TAG, "Downloading $filename...")
                _progress.value = DownloadProgress(
                    currentFile = filename,
                    bytesDownloaded = 0,
                    totalBytes = expectedSize,
                    fileIndex = index + 1,
                    totalFiles = MODEL_FILES.size
                )

                val url = URL("$BASE_URL$filename")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30_000
                connection.readTimeout = 30_000
                
                try {
                    connection.connect()
                    val totalBytes = if (connection.contentLengthLong > 0) connection.contentLengthLong else expectedSize
                    val tempFile = File(context.filesDir, "$filename.tmp")
                    
                    connection.inputStream.use { input ->
                        tempFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var downloaded = 0L
                            
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloaded += bytesRead
                                
                                _progress.value = DownloadProgress(
                                    currentFile = filename,
                                    bytesDownloaded = downloaded,
                                    totalBytes = totalBytes,
                                    fileIndex = index + 1,
                                    totalFiles = MODEL_FILES.size
                                )
                            }
                        }
                    }
                    
                    tempFile.renameTo(destFile)
                    Log.i(TAG, "Downloaded $filename (${destFile.length()} bytes)")
                    
                } finally {
                    connection.disconnect()
                }
            }

            _progress.value = _progress.value.copy(isComplete = true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            _progress.value = _progress.value.copy(error = e.message ?: "Download failed")
        }
    }
}
