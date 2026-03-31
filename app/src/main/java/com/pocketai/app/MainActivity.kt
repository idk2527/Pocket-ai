package com.pocketai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme
import com.pocketai.app.ui.theme.PocketAITheme
import com.pocketai.app.presentation.navigation.AppNavigation
import com.pocketai.app.services.LlamaCppService
import com.pocketai.app.services.ModelDownloadManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var preferencesManager: com.pocketai.app.data.preferences.PreferencesManager

    @Inject
    lateinit var llamaCppService: LlamaCppService

    @Inject
    lateinit var modelDownloadManager: ModelDownloadManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pre-warm the Qwen model only if already downloaded
        if (modelDownloadManager.areModelsDownloaded()) {
            llamaCppService.prewarm()
        }

        setContent {
            val themeMode = preferencesManager.themeMode.collectAsState(initial = "auto").value
            val isDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            PocketAITheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(downloadManager = modelDownloadManager)
                }
            }
        }
    }
}