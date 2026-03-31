package com.pocketai.app.services

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clean simple OTA JSON manifest.
 * Host this file anywhere (GitHub Gist raw, Firebase, Vercel, etc).
 */
data class OtaManifest(
    val versionName: String,
    val versionCode: Int,
    val releaseNotes: String,
    val apkUrl: String,
    val minSdkVersion: Int
)

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient()
    private val gson = Gson()
    
    private val MANIFEST_URL = "${com.pocketai.app.BuildConfig.FIREBASE_HOSTING_URL}/update.json"

    suspend fun checkForUpdate(): OtaManifest? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(MANIFEST_URL)
                // Append a timestamp parameter to prevent aggressive CDN caching of the JSON
                .url("$MANIFEST_URL?t=${System.currentTimeMillis()}")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                
                val body = response.body?.string() ?: return@use null
                val manifest = gson.fromJson(body, OtaManifest::class.java)
                
                val currentVersion = getAppVersionCode()
                
                if (manifest.versionCode > currentVersion) {
                    return@use manifest
                }
                return@use null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }
    
    private fun getAppVersionCode(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            @Suppress("DEPRECATION")
            pInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            1
        }
    }

    fun downloadAndInstall(url: String, fileName: String): Long {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Downloading PocketAI Update")
                .setDescription("Downloading v$fileName")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                .setMimeType("application/vnd.android.package-archive")

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            return manager.enqueue(request)
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
    }
    
    fun installApk(downloadId: Long) {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = manager.getUriForDownloadedFile(downloadId) ?: return
        
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        context.startActivity(intent)
    }
}
