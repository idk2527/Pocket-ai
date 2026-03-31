package com.pocketai.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// "Cal AI" Minimalist Light Theme
private val LightColorScheme = lightColorScheme(
    primary = StarkBlack, // Core CTA buttons
    onPrimary = TextInverse,
    primaryContainer = StarkSilver,
    onPrimaryContainer = StarkBlack,
    secondary = BrandBlue,
    onSecondary = TextInverse,
    secondaryContainer = BrandBlueBg,
    onSecondaryContainer = BrandBlue,
    tertiary = SoftGreen,
    onTertiary = TextInverse,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = AppSurface,
    onSurface = TextPrimary,
    surfaceVariant = AppSurface, // Keep variant equal to surface for flatness
    onSurfaceVariant = TextSecondary,
    error = VibrantRed,
    onError = TextInverse,
    errorContainer = VibrantRedBg,
    onErrorContainer = VibrantRed,
    outline = StarkSilver
)

@Composable
fun PocketAITheme(
    darkTheme: Boolean = false, // Enforce light theme for the pure minimalist look
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    // Force light scheme unconditionally for visual uniformity
    val colorScheme = LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT // Draw behind status bar
            // Make navigation bar transparent as well for edge-to-edge
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
            
            // Allow drawing edge-to-edge
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PocketTypography,
        content = content
    )
}