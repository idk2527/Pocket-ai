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

// "Liquid Glass" Premium Dark Theme
private val DarkColorScheme = darkColorScheme(
    primary = LiquidTeal,
    onPrimary = Obsidian, // Dark text on bright accent for contrast
    primaryContainer = LiquidTealBg,
    onPrimaryContainer = LiquidTealGlow,
    secondary = TrustBlue,
    onSecondary = TextPrimary,
    secondaryContainer = TrustBlueBg,
    onSecondaryContainer = TrustBlueDim,
    tertiary = NeoGreen,
    onTertiary = Obsidian,
    background = DeepNavy,
    onBackground = TextPrimary,
    surface = DeepNavy, // Base surface matches background
    onSurface = TextPrimary,
    surfaceVariant = GlassDark, // Elevated cards
    onSurfaceVariant = TextSecondary,
    error = CoralRed,
    onError = TextPrimary,
    errorContainer = CoralRedBg,
    onErrorContainer = CoralRedDim,
    outline = GlassBorder
)

// We are forcing a Dark-First aesthetic for that premium AI "vibe".
// But we still map light colors thoughtfully if the user explicitly disables dark mode later.
private val LightColorScheme = lightColorScheme(
    primary = TrustBlue,
    onPrimary = TextPrimary,
    primaryContainer = TrustBlueBg,
    onPrimaryContainer = TrustBlueDim,
    secondary = LiquidTealDim,
    onSecondary = TextPrimary,
    tertiary = NeoGreenDim,
    onTertiary = TextPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    error = CoralRedDim,
    onError = TextPrimary,
    outline = LightBorder
)

@Composable
fun PocketAITheme(
    darkTheme: Boolean = true, // Force dark theme by default for premium feel
    dynamicColor: Boolean = false, // Disabled to enforce brand consistency
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
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