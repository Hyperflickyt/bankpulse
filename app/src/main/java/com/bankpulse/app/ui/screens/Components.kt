package com.bankpulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpulse.app.ui.theme.Line
import com.bankpulse.app.ui.theme.Surface
import com.bankpulse.app.ui.theme.TextLo
import java.text.NumberFormat
import java.util.Locale

private val inr: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 0
}
fun money(v: Double): String = inr.format(v)

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .border(1.dp, Line, RoundedCornerShape(20.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
fun SectionHeader(title: String, trailing: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp, start = 2.dp, end = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        if (trailing != null)
            Text(trailing, color = TextLo, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}

@Composable
fun Kicker(text: String) =
    Text(text, color = TextLo, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
