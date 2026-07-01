package com.bankpulse.app.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bankpulse.app.data.Transaction
import com.bankpulse.app.data.TxnType
import com.bankpulse.app.repo.Analytics
import com.bankpulse.app.repo.FinanceRepository
import com.bankpulse.app.repo.Insights
import com.bankpulse.app.sms.Categorizer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

data class UiState(
    val transactions: List<Transaction> = emptyList(),
    val insights: Insights? = null,
    val installTime: Long = 0L
)

class FinanceViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = FinanceRepository(app)

    val state: StateFlow<UiState> =
        combine(repo.transactions, repo.balances) { txns, balances ->
            UiState(
                transactions = txns,
                insights = Analytics.build(txns, balances),
                installTime = repo.installTime()
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    /** Add a transaction the user typed in by hand. */
    fun addManual(
        type: TxnType,
        amount: Double,
        payee: String,
        note: String,
        bank: String,
        accountTail: String,
        category: String
    ) {
        viewModelScope.launch {
            val cat = category.ifBlank { Categorizer.of(payee, type, note) }
            repo.addManual(
                Transaction(
                    smsHash = Random.nextLong(),      // unique key; not from an SMS
                    bank = bank.ifBlank { "Manual" },
                    accountTail = accountTail.ifBlank { "----" },
                    type = type,
                    amount = amount,
                    merchant = payee.ifBlank { "Unknown" },
                    category = cat,
                    balanceAfter = null,
                    timestamp = System.currentTimeMillis(),
                    rawSms = if (note.isBlank()) "Added manually" else "Manual entry · $note",
                    note = note,
                    manual = true
                )
            )
        }
    }

    fun delete(hash: Long) { viewModelScope.launch { repo.delete(hash) } }
    fun clearAll() { viewModelScope.launch { repo.clearAll() } }
}
