package com.bankpulse.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpulse.app.ui.theme.Gold
import com.bankpulse.app.ui.theme.Ink
import com.bankpulse.app.ui.theme.TextHi
import com.bankpulse.app.ui.theme.TextLo
import kotlinx.coroutines.delay
import kotlin.math.cos

/**
 * Opening intro: a ₹1 coin spins on its vertical axis, decelerating to a
 * clean stop, then hands off to the app. Themed to match the dark/gold UI.
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val spin = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Four-and-a-bit turns, easing out so it looks like it's coming to rest.
        spin.animateTo(
            targetValue = 1440f,
            animationSpec = tween(durationMillis = 1700, easing = FastOutSlowInEasing)
        )
        delay(180)
        onDone()
    }

    Box(
        Modifier.fillMaxSize().background(Ink),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RupeeCoin(angleDeg = spin.value, modifier = Modifier.size(128.dp))
            Spacer(Modifier.height(22.dp))
            Text("BankPulse", color = Gold, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "// your money, on autopilot",
                color = TextLo, fontFamily = FontFamily.Monospace, fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun RupeeCoin(angleDeg: Float, modifier: Modifier) {
    val density = LocalDensity.current.density
    // cos of the spin angle drives the 3D "edge-on" squash and text mirroring.
    val c = cos(Math.toRadians(angleDeg.toDouble())).toFloat()

    Box(
        modifier.graphicsLayer {
            rotationY = angleDeg
            cameraDistance = 10f * density
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // coin body — warm gold radial so it reads as metal
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFD27A), Color(0xFFF5B544), Color(0xFFB9822A)),
                    center = Offset(center.x - r * 0.25f, center.y - r * 0.25f),
                    radius = r * 1.35f
                ),
                radius = r, center = center
            )
            // rim
            drawCircle(Color(0xFF8A5E18), radius = r, center = center, style = Stroke(width = r * 0.06f))
            drawCircle(Color(0xFFE0A63C), radius = r * 0.80f, center = center, style = Stroke(width = r * 0.03f))

            // milled edge ticks
            val ticks = 48
            for (i in 0 until ticks) {
                val a = Math.toRadians((360.0 / ticks) * i)
                val x1 = center.x + (r * 0.90f) * cos(a).toFloat()
                val y1 = center.y + (r * 0.90f) * kotlin.math.sin(a).toFloat()
                val x2 = center.x + r * cos(a).toFloat()
                val y2 = center.y + r * kotlin.math.sin(a).toFloat()
                drawLine(Color(0xFF8A5E18), Offset(x1, y1), Offset(x2, y2), strokeWidth = r * 0.02f)
            }
        }

        // ₹ face — counter-mirror so it stays legible through the back half of the spin
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { scaleX = if (c < 0f) -1f else 1f }
        ) {
            Text("₹", color = Color(0xFF5A3A0E), fontWeight = FontWeight.Black, fontSize = 46.sp)
            Text(
                "ONE RUPEE",
                color = Color(0xFF5A3A0E), fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, fontSize = 9.sp
            )
        }
    }
}
