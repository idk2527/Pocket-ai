package com.pocketai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.isSystemInDarkTheme
import com.pocketai.app.ui.theme.PocketAITheme
import com.pocketai.app.presentation.navigation.AppNavigation
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.pocketai.app.services.LiteRTService
import com.pocketai.app.services.ModelDownloadManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var preferencesManager: com.pocketai.app.data.preferences.PreferencesManager

    @Inject
    lateinit var liteRTService: LiteRTService

    @Inject
    lateinit var modelDownloadManager: ModelDownloadManager

    @Inject
    lateinit var updateManager: com.pocketai.app.services.UpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pre-warm the Qwen LiteRT model (NPU Burst Mode) only if already downloaded
        if (modelDownloadManager.areModelsDownloaded()) {
            val modelFile = java.io.File(filesDir, "Qwen3.5-0.8B-LiteRT.bin")
            if (modelFile.exists()) {
                lifecycleScope.launch {
                    liteRTService.initialize(modelFile.absolutePath)
                }
            }
        }

        setContent {
            val themeMode = preferencesManager.themeMode.collectAsState(initial = "auto").value
            val isDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            
            var otaManifest by remember { mutableStateOf<com.pocketai.app.services.OtaManifest?>(null) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                scope.launch {
                    val update = updateManager.checkForUpdate()
                    if (update != null) {
                        otaManifest = update
                    }
                }
            }

            PocketAITheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(downloadManager = modelDownloadManager)
                    
                    if (otaManifest != null) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { otaManifest = null },
                            title = { androidx.compose.material3.Text("Update Available: v${otaManifest?.versionName}") },
                            text = { androidx.compose.material3.Text(otaManifest?.releaseNotes ?: "A new version of PocketAI is available.") },
                            confirmButton = {
                                androidx.compose.material3.TextButton(onClick = {
                                    val manifest = otaManifest
                                    otaManifest = null
                                    if (manifest != null) {
                                        updateManager.downloadAndInstall(manifest.updateUrl, "pocketai-v${manifest.versionName}.apk")
                                        val toastMsg = "Downloading update in background..."
                                        android.widget.Toast.makeText(this@MainActivity, toastMsg, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }) {
                                    androidx.compose.material3.Text("Install Now")
                                }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(onClick = { otaManifest = null }) {
                                    androidx.compose.material3.Text("Remind Me Later")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}