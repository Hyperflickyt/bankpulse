package com.bankpulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpulse.app.data.Transaction
import com.bankpulse.app.data.TxnType
import com.bankpulse.app.ui.theme.*
import com.bankpulse.app.vm.UiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(state: UiState, onDelete: (Long) -> Unit = {}) {
    val fmt = remember { SimpleDateFormat("h:mm a · d MMM", Locale.getDefault()) }
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }

    pendingDelete?.let { t ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove this transaction?") },
            text = { Text("${t.merchant} · ${money(t.amount)}. This only removes it from the app, not your SMS.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onDelete(t.smsHash); pendingDelete = null
                }) { Text("Remove", color = OutRed) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(18.dp))
        Kicker("// parsed from your sms · long-press to remove")
        Text("Transactions", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(12.dp))

        if (state.transactions.isEmpty()) {
            Card { Text("Nothing here yet. Once a bank SMS arrives, the transaction appears instantly.", color = TextLo) }
            return
        }
        LazyColumn {
            items(state.transactions) { t -> TxnRow(t, fmt, onLongPress = { pendingDelete = t }) }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TxnRow(t: Transaction, fmt: SimpleDateFormat, onLongPress: () -> Unit = {}) {
    var open by remember { mutableStateOf(false) }
    val credit = t.type == TxnType.CREDIT
    Column(
        Modifier.combinedClickable(onClick = { open = !open }, onLongClick = onLongPress)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Surface2),
                contentAlignment = Alignment.Center
            ) { Text(t.category.take(1), color = Gold, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(t.merchant, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                Text("${t.bank} ····${t.accountTail}  ·  ${t.category}  ·  ${fmt.format(Date(t.timestamp))}",
                    color = TextLo, fontFamily = FontFamily.Monospace, fontSize = 10.sp, maxLines = 1)
            }
            Text(
                (if (credit) "+" else "−") + money(t.amount),
                color = if (credit) InGreen else TextHi,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
            )
        }
        if (open) {
            Text(
                "SMS source › ${t.rawSms}",
                color = TextLo, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Line, RoundedCornerShape(10.dp))
                    .padding(11.dp).padding(bottom = 4.dp)
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}
