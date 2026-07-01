package com.bankpulse.app.sms

import com.bankpulse.app.data.Transaction
import com.bankpulse.app.data.TxnType
import kotlin.math.abs

/**
 * Turns a raw bank SMS string into a [Transaction], or null if the message
 * isn't an actual money movement (OTPs, promos, EMI/limit/reward alerts, etc).
 *
 * Indian bank SMS has no standard format, so this works on signals common
 * across HDFC / SBI / ICICI / Axis / Kotak / PNB rather than rigid per-bank
 * templates. Two things it is careful about:
 *   1. It never mistakes the "available balance" figure for the txn amount.
 *   2. It aggressively skips non-transaction noise (offers, dues, cashback...).
 */
object SmsParser {

    // ---- vocabulary signals -------------------------------------------------
    private val creditWords = listOf("credited", "received", "deposited", "added", "refund", "refunded")
    private val debitWords  = listOf("debited", "spent", "sent", "withdrawn", "paid", "purchase", "purchased", "deducted")

    // If any of these appear, it's not a real completed debit/credit on your account.
    private val skipWords = listOf(
        "otp", "one time password", "will be credited",
        "statement", "e-statement",
        "min amt due", "minimum amount due", "amount due", "total due", "due on", "due date",
        "offer", "loan offer", "pre-approved", "pre approved", "eligible", "apply now",
        "cashback", "reward", "rewards", "earned", "points", "voucher", "coupon", "discount",
        "% off", "congratulations", "you have won", "won ",
        "request received", "requested",
        "e-mandate", "mandate", "registered", "autopay",
        "balance is", "avl bal as on", "available bal as on",
        "credit limit", "available limit",
        "failed", "declined", "unsuccessful", "not been", "could not", "insufficient"
    )

    // amount: ₹ / Rs. / INR  +  1,23,456.78  OR  plain 22000
    private val amountRe = Regex(
        """(?:rs\.?|inr|₹)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )
    // the "available/closing balance" clause — used to EXCLUDE that number from the amount.
    private val balanceRe = Regex(
        """(?:avl|avail(?:able)?|a/c|clear|closing)?\s*bal(?:ance|\b)[:\s]*(?:rs\.?|inr|₹)?\s*[0-9,]+(?:\.[0-9]{1,2})?""",
        RegexOption.IGNORE_CASE
    )
    // last 3-4 digits of the account/card
    private val acctRe = Regex(
        """(?:a/?c|acct?|account|card|ac)\D{0,6}?(\d{3,4})\b|x{1,4}(\d{3,4})\b|\*{2,}(\d{3,4})\b""",
        RegexOption.IGNORE_CASE
    )
    // counterparty after to/at/frm/from/by/via
    private val partyRe = Regex(
        """(?:\bto\b|\bat\b|\bfrom\b|\bfrm\b|\bby\b|\bvia\b)\s+([A-Za-z0-9@._\- ]{3,40}?)(?=\s+(?:on|ref|upi|avl|bal|a/c|txn|info|dt|\.|,|$))""",
        RegexOption.IGNORE_CASE
    )
    // a real transaction almost always carries one of these tokens
    private val strongTokens = listOf("a/c", "acct", "account", "upi", "imps", "neft", "rtgs", "txn", "card", "vpa", "ref")

    private val bankFromSender = mapOf(
        "HDFC" to "HDFC", "SBI" to "SBI", "ICICI" to "ICICI", "AXIS" to "Axis",
        "KOTAK" to "Kotak", "KOTK" to "Kotak", "PNB" to "PNB", "BOB" to "BoB",
        "YESBNK" to "Yes", "IDFC" to "IDFC", "INDUS" to "IndusInd", "CANBNK" to "Canara",
        "UNION" to "Union", "IOB" to "IOB", "CBIN" to "Central", "FEDBNK" to "Federal"
    )

    fun parse(sender: String, body: String, receivedAt: Long): Transaction? {
        val lower = body.lowercase()

        // 1) Drop non-transaction noise.
        if (skipWords.any { lower.contains(it) }) return null

        // 2) Direction — earliest action verb wins (bank leads with the action on YOUR account).
        val creditIdx = creditWords.mapNotNull { w -> lower.indexOf(w).takeIf { it >= 0 } }.minOrNull()
        val debitIdx  = debitWords.mapNotNull { w -> lower.indexOf(w).takeIf { it >= 0 } }.minOrNull()
        if (creditIdx == null && debitIdx == null) return null
        val type = if (debitIdx != null && (creditIdx == null || debitIdx < creditIdx)) TxnType.DEBIT else TxnType.CREDIT
        val verbPos = if (type == TxnType.DEBIT) debitIdx!! else creditIdx!!

        // 3) Amount — collect every currency figure, drop the balance figure,
        //    then take the one nearest the action verb. This kills the
        //    "balance mistaken for amount" bug.
        val allAmounts = amountRe.findAll(body).map { it.range.first to it.groupValues[1] }.toList()
        if (allAmounts.isEmpty()) return null
        val balSpan = balanceRe.find(body)?.range
        var candidates = allAmounts.filter { (pos, _) -> balSpan == null || pos !in balSpan }
        if (candidates.isEmpty()) candidates = allAmounts
        val amount = candidates
            .minByOrNull { (pos, _) -> abs(pos - verbPos) }!!
            .second.replace(",", "").toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        // 4) Account tail + accept gate: must actually look like a bank txn.
        val tail = acctRe.find(body)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }
        val hasStrong = tail != null || balSpan != null || strongTokens.any { lower.contains(it) }
        if (!hasStrong) return null

        val balance = balanceRe.find(body)
            ?.let { amountRe.find(it.value)?.groupValues?.get(1) }
            ?.replace(",", "")?.toDoubleOrNull()

        val bank = resolveBank(sender, lower)
        val merchant = cleanMerchant(partyRe.find(body)?.groupValues?.get(1))
        val category = Categorizer.of(merchant, type, body)

        return Transaction(
            smsHash = fnv1a(sender + "|" + body),
            bank = bank,
            accountTail = tail ?: "----",
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
        if (s.contains("@")) s = s.substringBefore("@")
        return s.split(Regex("\\s+")).joinToString(" ") { w ->
            w.lowercase().replaceFirstChar { it.uppercase() }
        }.take(28).ifBlank { "Unknown" }
    }

    /** 64-bit FNV-1a — far fewer collisions than String.hashCode() for dedup. */
    private fun fnv1a(s: String): Long {
        var h = -0x340d631b7bdddcdbL // 14695981039346656037 unsigned
        for (c in s) { h = h xor c.code.toLong(); h *= 0x100000001b3L }
        return h
    }
}
