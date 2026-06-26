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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val transactions: List<Transaction> = emptyList(),
    val insights: Insights? = null,
    val loadingBackfill: Boolean = false
)

class FinanceViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = FinanceRepository(app)

    val state: StateFlow<UiState> =
        combine(repo.transactions, repo.balances) { txns, balances ->
            UiState(
                transactions = txns,
                insights = Analytics.build(txns, balances)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    /** Call after the user grants SMS permission. */
    fun importInbox() = viewModelScope.launch { repo.backfillFromInbox() }
}
