package com.bankpulse.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpulse.app.ui.theme.*
import com.bankpulse.app.vm.UiState
import kotlin.math.abs

private val catColors = listOf(
    Color(0xFF7C3AED), Color(0xFFFC8019), Color(0xFF3B82F6),
    Color(0xFFF5B544), Color(0xFF34D399), Color(0xFFFB7185)
)

@Composable
fun ReportsScreen(state: UiState) {
    val ins = state.insights
    val cats = ins?.byCategory ?: emptyList()
    val net = (ins?.moneyIn ?: 0.0) - (ins?.moneyOut ?: 0.0)
    val totalOut = cats.sumOf { it.amount }.coerceAtLeast(1.0)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Kicker("// auto-generated every monday")
        Text("Weekly report", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(14.dp))

        // ---- Credit vs Debit ------------------------------------------------
        val months = remember(state.transactions) {
            com.bankpulse.app.repo.Analytics.monthlyInOut(state.transactions)
        }
        Card {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Credit vs Debit", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(InGreen); Text(" in  ", color = TextLo, fontSize = 11.sp)
                    LegendDot(OutRed);  Text(" out", color = TextLo, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            InOutChart(months, Modifier.fillMaxWidth().height(150.dp))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                months.forEach { m ->
                    Text(m.label, color = TextLo, fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp, modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        Card {
            Text(if (net >= 0) "You saved ${money(abs(net))}" else "You overspent ${money(abs(net))}",
                fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Donut(cats.map { it.amount }, Modifier.size(92.dp))
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    cats.take(5).forEachIndexed { i, c ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(11.dp).clip(RoundedCornerShape(4.dp))
                                .background(catColors[i % catColors.size]))
                            Spacer(Modifier.width(9.dp))
                            Text(c.category, color = TextLo, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text(money(c.amount), fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        SectionHeader("Where it went", "${cats.size} categories")
        cats.forEachIndexed { i, c ->
            Column(Modifier.padding(vertical = 7.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(c.category, fontSize = 13.sp)
                    Text(money(c.amount), fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Spacer(Modifier.height(7.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(6.dp)).background(Surface2)) {
                    Box(Modifier.fillMaxWidth((c.amount / totalOut).toFloat()).height(8.dp)
                        .clip(RoundedCornerShape(6.dp)).background(catColors[i % catColors.size]))
                }
            }
        }
        Spacer(Modifier.height(90.dp))
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(color))
}

@Composable
private fun InOutChart(data: List<com.bankpulse.app.repo.MonthFlow>, modifier: Modifier) {
    val maxV = (data.maxOfOrNull { maxOf(it.credit, it.debit) } ?: 0.0).coerceAtLeast(1.0)
    Canvas(modifier) {
        if (data.isEmpty()) return@Canvas
        val slot = size.width / data.size
        val barW = slot * 0.30f
        val gap = slot * 0.10f
        val baseY = size.height
        data.forEachIndexed { i, m ->
            val cx = slot * i + slot / 2f
            val inH = (m.credit / maxV * size.height).toFloat()
            val outH = (m.debit / maxV * size.height).toFloat()
            // credit bar (left of centre)
            drawRoundRect(
                color = InGreen,
                topLeft = androidx.compose.ui.geometry.Offset(cx - barW - gap / 2, baseY - inH),
                size = Size(barW, inH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
            // debit bar (right of centre)
            drawRoundRect(
                color = OutRed,
                topLeft = androidx.compose.ui.geometry.Offset(cx + gap / 2, baseY - outH),
                size = Size(barW, outH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
        }
    }
}

@Composable
private fun Donut(values: List<Double>, modifier: Modifier) {
    val total = values.sum().coerceAtLeast(1.0)
    Canvas(modifier) {
        var start = -90f
        val stroke = 28f
        val s = Size(size.width - stroke, size.height - stroke)
        values.forEachIndexed { i, v ->
            val sweep = (v / total * 360f).toFloat()
            drawArc(
                color = catColors[i % catColors.size],
                startAngle = start, sweepAngle = sweep, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                size = s, style = Stroke(width = stroke)
            )
            start += sweep
        }
    }
}
