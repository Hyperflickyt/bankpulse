package com.bankpulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpulse.app.data.TxnType
import com.bankpulse.app.ui.theme.*

private val categories = listOf(
    "Auto", "Salary", "Food", "Shopping", "Fuel", "Subscription",
    "Rent", "Travel", "Bills", "Transfers", "Income", "Other"
)

@Composable
fun AddScreen(
    onSave: (TxnType, Double, String, String, String, String, String) -> Unit,
    onDone: () -> Unit
) {
    var type by remember { mutableStateOf(TxnType.DEBIT) }
    var amount by remember { mutableStateOf("") }
    var payee by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var bank by remember { mutableStateOf("") }
    var tail by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Auto") }
    var catOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)
    ) {
        Kicker("// add a transaction by hand")
        Text("Add money", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(16.dp))

        // Credit / Debit toggle
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TypePill("Money in", type == TxnType.CREDIT, InGreen, Modifier.weight(1f)) {
                type = TxnType.CREDIT
            }
            TypePill("Money out", type == TxnType.DEBIT, OutRed, Modifier.weight(1f)) {
                type = TxnType.DEBIT
            }
        }

        Spacer(Modifier.height(16.dp))
        Field("Amount (₹)", amount, KeyboardType.Number) { amount = it.filter { ch -> ch.isDigit() || ch == '.' } }
        Field(if (type == TxnType.CREDIT) "Sender / payer name" else "Payee name", payee) { payee = it }
        Field("Reason / note", note) { note = it }
        Field("Bank (optional)", bank) { bank = it }
        Field("Account last 4 (optional)", tail, KeyboardType.Number) { tail = it.filter { c -> c.isDigit() }.take(4) }

        Spacer(Modifier.height(6.dp))
        Text("Category", color = TextLo, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Box {
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface2)
                    .border(1.dp, Line, RoundedCornerShape(12.dp))
                    .clickable { catOpen = true }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(category, fontSize = 14.sp)
                Text("▾", color = TextLo)
            }
            DropdownMenu(expanded = catOpen, onDismissRequest = { catOpen = false }) {
                categories.forEach { c ->
                    DropdownMenuItem(text = { Text(c) }, onClick = { category = c; catOpen = false })
                }
            }
        }

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = OutRed, fontSize = 12.sp)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                val amt = amount.toDoubleOrNull()
                when {
                    amt == null || amt <= 0 -> error = "Enter a valid amount."
                    payee.isBlank() -> error = "Add a name so you know who it was."
                    else -> {
                        onSave(type, amt, payee.trim(), note.trim(), bank.trim(), tail.trim(),
                            if (category == "Auto") "" else category)
                        onDone()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)
        ) {
            Text("Save transaction", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(90.dp))
    }
}

@Composable
private fun TypePill(label: String, selected: Boolean, accent: androidx.compose.ui.graphics.Color, m: Modifier, onClick: () -> Unit) {
    Row(
        m.clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent.copy(alpha = 0.16f) else Surface2)
            .border(1.dp, if (selected) accent else Line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (selected) accent else TextLo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(label, color = TextLo, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold,
                unfocusedBorderColor = Line,
                focusedTextColor = TextHi,
                unfocusedTextColor = TextHi,
                cursorColor = Gold,
                focusedContainerColor = Surface2,
                unfocusedContainerColor = Surface2
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}
