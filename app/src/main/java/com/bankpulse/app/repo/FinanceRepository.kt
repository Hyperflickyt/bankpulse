package com.bankpulse.app.repo

import android.content.Context
import com.bankpulse.app.data.AppDatabase
import com.bankpulse.app.data.Prefs
import com.bankpulse.app.data.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FinanceRepository(context: Context) {
    private val dao = AppDatabase.get(context).txnDao()
    private val appContext = context.applicationContext

    val transactions = dao.observeAll()
    val balances = dao.observeLatestBalances()

    /** When tracking started (app install / first launch). */
    fun installTime() = Prefs.installTime(appContext)

    /** Add a hand-entered transaction. */
    suspend fun addManual(txn: Transaction) = withContext(Dispatchers.IO) { dao.insert(txn) }

    /** Delete a single row (long-press on the Transactions tab). */
    suspend fun delete(hash: Long) = withContext(Dispatchers.IO) { dao.deleteByHash(hash) }

    /** Wipe everything and start over. */
    suspend fun clearAll() = withContext(Dispatchers.IO) { dao.clearAll() }

    suspend fun between(start: Long, end: Long) =
        withContext(Dispatchers.IO) { dao.between(start, end) }
}
