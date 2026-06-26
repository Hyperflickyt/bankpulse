package com.bankpulse.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TxnType { CREDIT, DEBIT }

/**
 * One parsed bank transaction. `smsHash` prevents the same SMS being
 * stored twice if the live receiver and the initial bulk-scan overlap.
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val smsHash: Long,
    val bank: String,            // e.g. "HDFC"
    val accountTail: String,     // last 4 digits, e.g. "4821"
    val type: TxnType,
    val amount: Double,          // always positive; direction comes from `type`
    val merchant: String,        // best-effort counterparty / merchant
    val category: String,        // derived bucket: Food, Rent, Salary...
    val balanceAfter: Double?,   // available balance if the SMS reported it
    val timestamp: Long,         // epoch millis
    val rawSms: String           // original message, kept for the "SMS source" peek
)
