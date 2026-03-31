package com.pocketai.app.ui.theme

import androidx.compose.ui.graphics.Color

// ─── PocketAI Premium "Liquid Glass" Palette ────────────────────────────────

// Backgrounds — softer than pure black for a premium depth
val Obsidian = Color(0xFF0A0A0A) // True near-black
val DeepNavy = Color(0xFF0A0A0A) // Legacy name, mapped to new black
val Charcoal = Color(0xFF141414)

// Surfaces — solid dark cards, no translucent borders
val GlassDark = Color(0xFF1A1A1E)
val GlassLight = Color(0xFF242428)
val GlassBorder = Color.Transparent // Killed the visible border
val GlassBorderFocus = Color(0x1AFFFFFF) // 10% white only on focus

// Primary — Liquid Teal (main brand accent)
val LiquidTeal = Color(0xFF14B8A6)
val LiquidTealGlow = Color(0xFF2DD4BF)
val LiquidTealDim = Color(0xFF0D9488)
val LiquidTealBg = Color(0x1A14B8A6) // 10% teal

// Secondary — Trust Blue
val TrustBlue = Color(0xFF0EA5E9)
val TrustBlueDim = Color(0xFF0284C7)
val TrustBlueBg = Color(0x1A0EA5E9) // 10% blue

// Accent — Softer Mint Green (Success states)
val NeoGreen = Color(0xFF34D399) // Softer, more Apple-like
val NeoGreenDim = Color(0xFF10B981)
val NeoGreenBg = Color(0x1A34D399)

// Semantic — Status/Error
val CoralRed = Color(0xFFFF6B6B)
val CoralRedDim = Color(0xFFEF4444)
val CoralRedBg = Color(0x1AFF6B6B)

val AmberWarn = Color(0xFFFBBF24)
val AmberWarnBg = Color(0x1AFBBF24)

// Text
val TextPrimary = Color(0xFFF9FAFB) // 98% white
val TextSecondary = Color(0xFF9CA3AF) // Muted gray
val TextTertiary = Color(0xFF6B7280) // Hint text
val TextInverse = Color(0xFF0A0F1C) // For light surfaces / buttons

// Light Theme
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightBorder = Color(0xFFE2E8F0)

// Category Colors — curated to be vibrant but harmonious
val CategoryGroceries = Color(0xFF10B981)
val CategoryTransport = Color(0xFF3B82F6)
val CategoryFood = Color(0xFFF97316)
val CategoryElectronics = Color(0xFF8B5CF6)
val CategoryEntertainment = Color(0xFFEC4899)
val CategoryShopping = Color(0xFFE11D48)
val CategoryHealth = Color(0xFF06B6D4)
val CategoryHome = Color(0xFF84CC16)
val CategoryFashion = Color(0xFFF43F5E)
val CategoryOther = Color(0xFF6B7280)