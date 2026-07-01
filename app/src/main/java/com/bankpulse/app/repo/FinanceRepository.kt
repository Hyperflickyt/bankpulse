package com.bankpulse.app.repo

import android.content.Context
import com.bankpulse.app.data.AppDatabase
import com.bankpulse.app.sms.SmsScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FinanceRepository(context: Context) {
    private val dao = AppDatabase.get(context).txnDao()
    private val appContext = context.applicationContext

    val transactions = dao.observeAll()
    val balances = dao.observeLatestBalances()

    /** Read the existing inbox once (e.g. right after permission is granted). */
    suspend fun backfillFromInbox() = withContext(Dispatchers.IO) {
        val found = SmsScanner.scanInbox(appContext)
        dao.insertAll(found)
    }

    /** Wipe everything and re-parse the whole inbox with the current parser. */
    suspend fun rescan() = withContext(Dispatchers.IO) {
        dao.clearAll()
        dao.insertAll(SmsScanner.scanInbox(appContext))
    }

    suspend fun delete(hash: Long) = withContext(Dispatchers.IO) { dao.deleteByHash(hash) }

    suspend fun between(start: Long, end: Long) =
        withContext(Dispatchers.IO) { dao.between(start, end) }
}
