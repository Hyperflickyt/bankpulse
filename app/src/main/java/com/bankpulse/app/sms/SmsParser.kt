package com.bankpulse.app.sms

import com.bankpulse.app.data.Transaction
import com.bankpulse.app.data.TxnType

/**
 * Turns a raw bank SMS string into a [Transaction], or null if the message
 * isn't a transaction alert (OTPs, promos, statements, etc).
 *
 * Indian bank SMS has no standard format, so this works on signals that are
 * common across HDFC / SBI / ICICI / Axis / Kotk / PNB rather than rigid
 * per-bank templates. Add patterns here as you spot ones it misses.
 */
object SmsParser {

    // ---- vocabulary signals -------------------------------------------------
    private val creditWords = listOf("credited", "received", "deposited", "added", "refund")
    private val debitWords  = listOf("debited", "spent", "sent", "withdrawn", "paid", "purchase")
    private val skipWords    = listOf("otp", "one time password", "will be credited",
        "statement", "e-statement", "min amt due", "due date", "offer", "loan offer",
        "request", "balance is", "available bal as on") // pure info, no movement

    // amount: ₹ / Rs. / INR  +  1,23,456.78  OR  plain 22000 (both are common in SMS)
    private val amountRe = Regex(
        """(?:rs\.?|inr|₹)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    // available balance after the txn
    private val balanceRe = Regex(
        """(?:avl|avail(?:able)?|a/c)?\s*bal(?:ance)?[:\s]*(?:rs\.?|inr|₹)?\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    // last 4 digits of the account/card: x4821, xx4821, XXXX4821, ...4821, A/c 4821
    private val acctRe = Regex(
        """(?:a/?c|acct?|card|ac)\D{0,6}?(\d{4})\b|x{1,4}(\d{4})\b|\*{2,}(\d{4})\b""",
        RegexOption.IGNORE_CASE
    )
    // counterparty after to/at/frm/from/by/via VPA or name
    private val partyRe = Regex(
        """(?:\bto\b|\bat\b|\bfrom\b|\bfrm\b|\bby\b|\bvia\b)\s+([A-Za-z0-9@._\- ]{3,40}?)(?=\s+(?:on|ref|upi|avl|bal|a/c|txn|info|\.|,|$))""",
        RegexOption.IGNORE_CASE
    )

    /** Sender ID (e.g. "VK-HDFCBK", "AD-SBIINB") mapped to a short bank code. */
    private val bankFromSender = mapOf(
        "HDFC" to "HDFC", "SBI" to "SBI", "ICICI" to "ICICI", "AXIS" to "AXIS",
        "KOTAK" to "Kotak", "KOTK" to "Kotak", "PNB" to "PNB", "BOB" to "BoB",
        "YESBNK" to "Yes", "IDFC" to "IDFC", "INDUS" to "IndusInd", "CANBNK" to "Canara"
    )

    fun parse(sender: String, body: String, receivedAt: Long): Transaction? {
        val lower = body.lowercase()

        // 1) Drop anything that isn't an actual money movement.
        if (skipWords.any { lower.contains(it) }) return null

        // Direction: many SMS contain both verbs ("a/c debited ... payee credited").
        // The bank leads with the action on YOUR account, so the earliest verb wins.
        val creditIdx = creditWords.mapNotNull { w -> lower.indexOf(w).takeIf { it >= 0 } }.minOrNull()
        val debitIdx  = debitWords.mapNotNull { w -> lower.indexOf(w).takeIf { it >= 0 } }.minOrNull()
        if (creditIdx == null && debitIdx == null) return null
        val type = when {
            debitIdx != null && (creditIdx == null || debitIdx < creditIdx) -> TxnType.DEBIT
            else -> TxnType.CREDIT
        }

        // 2) Amount is mandatory.
        val amount = amountRe.find(body)
            ?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
            ?: return null

        val balance = balanceRe.find(body)
            ?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

        val tail = acctRe.find(body)?.groupValues
            ?.drop(1)?.firstOrNull { it.isNotEmpty() } ?: "----"

        val bank = resolveBank(sender, lower)
        val merchant = cleanMerchant(partyRe.find(body)?.groupValues?.get(1))
        val category = Categorizer.of(merchant, type, body)

        return Transaction(
            smsHash = (sender + "|" + body).hashCode().toLong(),
            bank = bank,
            accountTail = tail,
            type = type,
            amount = amount,
            merchant = merchant,
            category = category,
            balanceAfter = balance,
            timestamp = receivedAt,
            rawSms = body
        )
    }

    private fun resolveBank(sender: String, lowerBody: String): String {
        val up = sender.uppercase()
        bankFromSender.forEach { (key, name) -> if (up.contains(key)) return name }
        bankFromSender.forEach { (key, name) -> if (lowerBody.contains(key.lowercase())) return name }
        return "Bank"
    }

    private fun cleanMerchant(raw: String?): String {
        if (raw.isNullOrBlank()) return "Unknown"
        var s = raw.trim().trim('.', ',', '-', ' ')
        // a UPI VPA like rahul@oksbi -> "rahul"
        if (s.contains("@")) s = s.substringBefore("@")
        return s.split(Regex("\\s+")).joinToString(" ") { w ->
            w.lowercase().replaceFirstChar { it.uppercase() }
        }.take(28).ifBlank { "Unknown" }
    }
}
