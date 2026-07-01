package com.bankpulse.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpulse.app.data.TxnType
import com.bankpulse.app.ui.theme.*
import com.bankpulse.app.vm.UiState

@Composable
fun DashboardScreen(state: UiState, onClearAll: () -> Unit = {}) {
    val ins = state.insights
    var confirmClear by remember { mutableStateOf(false) }

    if (confirmClear) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all data?") },
            text = { Text("Removes every transaction from the app. Your SMS are untouched, and new ones will still be tracked from now on.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { onClearAll(); confirmClear = false }) {
                    Text("Clear", color = OutRed)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)
    ) {
        Kicker("// good evening")
        Text("Your money", fontWeight = FontWeight.Bold, fontSize = 22.sp)

        Spacer(Modifier.height(16.dp))
        Card {
            Kicker("Total balance · ${state.transactions.map { it.bank }.distinct().size} banks")
            Spacer(Modifier.height(8.dp))
            Text(
                money(ins?.totalBalance ?: 0.0),
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 32.sp
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IoTile("total credited", money(ins?.moneyIn ?: 0.0), InGreen, Modifier.weight(1f))
            IoTile("total debited", money(ins?.moneyOut ?: 0.0), OutRed, Modifier.weight(1f))
        }

        SectionHeader("Your banks", "auto-detected")
        val banks = state.transactions
            .filter { it.balanceAfter != null }
            .groupBy { it.bank to it.accountTail }
            .map { (k, v) -> Triple(k.first, k.second, v.maxByOrNull { it.timestamp }!!.balanceAfter!!) }
        if (banks.isEmpty()) {
            Card { Text("No bank SMS found yet. New transaction alerts will show up here automatically.", color = TextLo) }
        } else banks.forEach { (bank, tail, bal) ->
            Spacer(Modifier.height(10.dp))
            Card {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(bank, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Savings ····$tail", color = TextLo,
                            fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                    Text(money(bal), fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        val since = remember(state.installTime) {
            if (state.installTime == 0L) "" else
                java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date(state.installTime))
        }
        if (since.isNotEmpty()) {
            Text("Tracking since $since", color = TextLo,
                fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            Spacer(Modifier.height(8.dp))
        }
        androidx.compose.material3.OutlinedButton(
            onClick = { confirmClear = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear all data")
        }
        Spacer(Modifier.height(90.dp))
    }
}

@Composable
private fun IoTile(label: String, value: String, color: androidx.compose.ui.graphics.Color, m: Modifier) {
    Card(m) {
        Text(label, color = TextLo, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = color, fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    }
}
