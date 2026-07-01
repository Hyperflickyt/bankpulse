package com.bankpulse.app.sms

import com.bankpulse.app.data.TxnType

/** Maps a merchant / message into a spending bucket. Tune the keyword lists freely. */
object Categorizer {
    private val rules = linkedMapOf(
        "Salary"        to listOf("salary", "sal cr", "neft", "payroll"),
        "Food"          to listOf("swiggy", "zomato", "dominos", "kfc", "restaurant", "cafe", "eat"),
        "Shopping"      to listOf("amazon", "flipkart", "myntra", "ajio", "meesho", "mall"),
        "Fuel"          to listOf("petrol", "hpcl", "iocl", "bpcl", "fuel", "indian oil"),
        "Subscription"  to listOf("netflix", "spotify", "prime", "hotstar", "youtube", "jio", "airtel", "recharge"),
        "Rent"          to listOf("rent", "landlord"),
        "Travel"        to listOf("uber", "ola", "irctc", "makemytrip", "indigo", "redbus", "metro"),
        "Bills"         to listOf("electricity", "water", "gas", "bescom", "bill", "lic", "insurance", "premium"),
        "Transfers"     to listOf("imps", "transfer", "self")
    )

    fun of(merchant: String, type: TxnType, body: String): String {
        if (type == TxnType.CREDIT &&
            (body.contains("salary", true) || body.contains("payroll", true))) return "Salary"
        val hay = (merchant + " " + body).lowercase()
        rules.forEach { (cat, keys) -> if (keys.any { hay.contains(it) }) return cat }
        return if (type == TxnType.CREDIT) "Income" else "Other"
    }
}
