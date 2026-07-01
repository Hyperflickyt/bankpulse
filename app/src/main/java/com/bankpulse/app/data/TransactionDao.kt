package com.bankpulse.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(txn: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(txns: List<Transaction>)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()

    @Query("DELETE FROM transactions WHERE smsHash = :hash")
    suspend fun deleteByHash(hash: Long)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun between(start: Long, end: Long): List<Transaction>

    /** Latest reported balance per bank/account, used for the dashboard cards. */
    @Query(
        """
        SELECT * FROM transactions t
        WHERE balanceAfter IS NOT NULL
          AND timestamp = (
            SELECT MAX(timestamp) FROM transactions
            WHERE bank = t.bank AND accountTail = t.accountTail AND balanceAfter IS NOT NULL
          )
        GROUP BY bank, accountTail
        """
    )
    fun observeLatestBalances(): Flow<List<Transaction>>
}
