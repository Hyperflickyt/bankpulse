package com.bankpulse.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette mirrors the prototype: midnight ink, gold brand, mint in, coral out.
val Ink       = Color(0xFF0B1220)
val Surface   = Color(0xFF131E33)
val Surface2  = Color(0xFF1B2740)
val Line      = Color(0xFF27344F)
val TextHi    = Color(0xFFEAF0F8)
val TextLo    = Color(0xFF8A99B5)
val Gold      = Color(0xFFF5B544)
val InGreen   = Color(0xFF34D399)
val OutRed    = Color(0xFFFB7185)

private val Scheme = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF241702),
    background = Ink,
    onBackground = TextHi,
    surface = Surface,
    onSurface = TextHi,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextLo,
    outline = Line
)

@Composable
fun BankPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = BankPulseType,
        content = content
    )
}
