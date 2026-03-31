package com.pocketai.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlin.math.absoluteValue

/**
 * Displays a store brand logo from the Clearbit Logo API.
 * Falls back to a colored initial-avatar when the logo can't be loaded.
 */
@Composable
fun BrandLogo(
    storeName: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val domain = storeToDomain(storeName)
    val logoUrl = domain?.let { "https://img.logo.dev/$it?token=${com.pocketai.app.BuildConfig.LOGO_DEV_TOKEN}&size=128&format=png" }
    val initial = storeName.firstOrNull()?.uppercaseChar() ?: '?'
    val avatarColor = generateColorFromName(storeName)

    if (logoUrl != null) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(logoUrl)
                .crossfade(true)
                .build(),
            contentDescription = "$storeName logo",
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop,
            loading = { InitialAvatar(initial, avatarColor, size) },
            error = { InitialAvatar(initial, avatarColor, size) }
        )
    } else {
        InitialAvatar(initial, avatarColor, size, modifier)
    }
}

@Composable
private fun InitialAvatar(
    initial: Char,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.toString(),
            color = color,
            fontSize = (size.value * 0.4f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Maps common store names to their web domains for logo lookup.
 * Uses fuzzy matching (lowercase contains) so "SPAR Express" still matches "spar".
 */
private fun storeToDomain(storeName: String): String? {
    val name = storeName.lowercase().trim()
    return STORE_DOMAINS.entries.firstOrNull { (key, _) ->
        name.contains(key)
    }?.value
}

private fun generateColorFromName(name: String): Color {
    val hash = name.hashCode().absoluteValue
    val hue = (hash % 360).toFloat()
    return Color.hsl(hue, 0.6f, 0.55f)
}

/**
 * Domain mapping for ~40 common European & global stores.
 * Keys are lowercase substrings matched against storeName.
 */
private val STORE_DOMAINS = mapOf(
    // Grocery / Supermarket
    "spar" to "spar.at",
    "lidl" to "lidl.com",
    "aldi" to "aldi.com",
    "rewe" to "rewe.de",
    "edeka" to "edeka.de",
    "penny" to "penny.de",
    "netto" to "netto-online.de",
    "kaufland" to "kaufland.de",
    "hofer" to "hofer.at",
    "billa" to "billa.at",
    "merkur" to "billa.at",
    "tesco" to "tesco.com",
    "carrefour" to "carrefour.com",
    "albert" to "albert.cz",
    "migros" to "migros.ch",
    "coop" to "coop.ch",
    "dm" to "dm.de",
    "rossmann" to "rossmann.de",
    "müller" to "mueller.de",
    "muller" to "mueller.de",
    "walmart" to "walmart.com",
    "costco" to "costco.com",
    "target" to "target.com",
    "whole foods" to "wholefoodsmarket.com",
    "trader joe" to "traderjoes.com",

    // Fast Food / Restaurants
    "mcdonald" to "mcdonalds.com",
    "burger king" to "burgerking.com",
    "kfc" to "kfc.com",
    "subway" to "subway.com",
    "starbucks" to "starbucks.com",
    "domino" to "dominos.com",
    "pizza hut" to "pizzahut.com",
    "taco bell" to "tacobell.com",
    "wendy" to "wendys.com",
    "dunkin" to "dunkindonuts.com",
    "chipotle" to "chipotle.com",
    "five guys" to "fiveguys.com",
    "nordsee" to "nordsee.com",

    // Electronics / Tech
    "amazon" to "amazon.com",
    "apple" to "apple.com",
    "mediamarkt" to "mediamarkt.de",
    "media markt" to "mediamarkt.de",
    "saturn" to "saturn.de",
    "best buy" to "bestbuy.com",

    // Fashion / Retail
    "h&m" to "hm.com",
    "zara" to "zara.com",
    "ikea" to "ikea.com",
    "primark" to "primark.com",
    "uniqlo" to "uniqlo.com",
    "nike" to "nike.com",
    "adidas" to "adidas.com",

    // Transport
    "uber" to "uber.com",
    "bolt" to "bolt.eu",
    "shell" to "shell.com",
    "bp" to "bp.com",
    "omv" to "omv.com",

    // Entertainment
    "netflix" to "netflix.com",
    "spotify" to "spotify.com",
    "steam" to "store.steampowered.com",
    "cinema" to "cineplex.com"
)
