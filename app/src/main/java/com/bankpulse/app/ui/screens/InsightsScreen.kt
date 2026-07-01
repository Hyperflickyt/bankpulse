package com.bankpulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpulse.app.ui.theme.*
import com.bankpulse.app.vm.UiState

@Composable
fun InsightsScreen(state: UiState) {
    val ins = state.insights
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Kicker("// the answers you asked for")
        Text("Insights", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(14.dp))

        val provider = ins?.mainProvider
        val inTotal = (ins?.moneyIn ?: 0.0).coerceAtLeast(1.0)
        InsightCard(
            tag = "Main money provider",
            accent = InGreen,
            big = provider?.name ?: "—",
            sub = provider?.let {
                "${money(it.amount)} received — ${(it.amount / inTotal * 100).toInt()}% of all money coming in."
            } ?: "No income detected yet."
        )

        val dest = ins?.topDestination
        InsightCard(
            tag = "Where your money goes",
            accent = OutRed,
            big = dest?.name ?: "—",
            sub = dest?.let { "${money(it.amount)} is your single largest outflow this month." }
                ?: "No spending detected yet."
        )

        SectionHeader("Income sources", "top ${ins?.incomeSources?.size ?: 0}")
        ins?.incomeSources?.forEach { node ->
            FlowRow(node.name, money(node.amount), InGreen)
        }

        SectionHeader("Recurring expenses", "${ins?.recurring?.size ?: 0} detected")
        if (ins?.recurring.isNullOrEmpty()) {
            Card { Text("Recurring charges appear after a couple of billing cycles are seen.", color = TextLo) }
        } else ins!!.recurring.forEach { r ->
            Card(Modifier.padding(bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.merchant, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${r.cadence} · seen ${r.occurrences}×", color = TextLo,
                            fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                    Text(money(r.typicalAmount), fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(90.dp))
    }
}

@Composable
private fun InsightCard(tag: String, accent: Color, big: String, sub: String) {
    Card(Modifier.padding(bottom = 12.dp)) {
        Text(tag.uppercase(), color = TextLo, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        Spacer(Modifier.height(6.dp))
        Text(big, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = accent)
        Spacer(Modifier.height(6.dp))
        Text(sub, color = TextLo, fontSize = 12.sp)
    }
}

@Composable
private fun FlowRow(name: String, value: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.width(12.dp))
        Text(name, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
