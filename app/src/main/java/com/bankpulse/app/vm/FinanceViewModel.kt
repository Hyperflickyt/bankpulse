package com.bankpulse.app.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bankpulse.app.data.Transaction
import com.bankpulse.app.repo.Analytics
import com.bankpulse.app.repo.FinanceRepository
import com.bankpulse.app.repo.Insights
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val transactions: List<Transaction> = emptyList(),
    val insights: Insights? = null,
    val loadingBackfill: Boolean = false,
    val rescanning: Boolean = false
)

class FinanceViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = FinanceRepository(app)
    private val busy = MutableStateFlow(false)

    val state: StateFlow<UiState> =
        combine(repo.transactions, repo.balances, busy) { txns, balances, rescanning ->
            UiState(
                transactions = txns,
                insights = Analytics.build(txns, balances),
                rescanning = rescanning
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    /** Call after the user grants SMS permission. */
    fun importInbox() = viewModelScope.launch { repo.backfillFromInbox() }

    /** Wipe and re-parse the whole inbox — use after a parser fix. */
    fun rescan() = viewModelScope.launch {
        busy.value = true
        try { repo.rescan() } finally { busy.value = false }
    }

    fun delete(hash: Long) = viewModelScope.launch { repo.delete(hash) }
}
