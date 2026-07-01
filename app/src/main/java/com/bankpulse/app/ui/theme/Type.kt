package com.bankpulse.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Uses system fonts so the project builds with zero font assets to drop in.
// Swap in Archivo / IBM Plex Mono via res/font for an exact prototype match.
val Mono = FontFamily.Monospace

val BankPulseType = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge     = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyMedium     = TextStyle(fontSize = 14.sp),
    bodySmall      = TextStyle(fontSize = 12.sp),
    labelSmall     = TextStyle(fontFamily = Mono, fontSize = 11.sp)
)
