package com.bankpulse.app.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bankpulse.app.MainActivity
import com.bankpulse.app.R
import com.bankpulse.app.data.AppDatabase
import com.bankpulse.app.data.TxnType
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Runs once a week, sums the last 7 days, and posts a summary notification. */
class WeeklyReportWorker(
    appContext: Context,
    params: androidx.work.WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.get(applicationContext).txnDao()
        val end = System.currentTimeMillis()
        val start = end - 7L * 24 * 60 * 60 * 1000
        val txns = dao.between(start, end)

        val inr = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val moneyIn  = txns.filter { it.type == TxnType.CREDIT }.sumOf { it.amount }
        val moneyOut = txns.filter { it.type == TxnType.DEBIT }.sumOf { it.amount }
        val net = moneyIn - moneyOut

        val topCat = txns.filter { it.type == TxnType.DEBIT }
            .groupBy { it.category }
            .maxByOrNull { e -> e.value.sumOf { it.amount } }?.key ?: "—"

        val verb = if (net >= 0) "saved" else "overspent by"
        val title = "Weekly report ready"
        val text = "You $verb ${inr.format(kotlin.math.abs(net))}. " +
                   "In ${inr.format(moneyIn)} · Out ${inr.format(moneyOut)}. Top: $topCat."

        notify(title, text)
        return Result.success()
    }

    private fun notify(title: String, text: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Weekly reports", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val n = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        nm.notify(2025, n)
    }

    companion object {
        private const val CHANNEL = "weekly_reports"

        /** Schedule for every Monday ~9 AM. Call once from Application.onCreate. */
        fun schedule(context: Context) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0)
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                if (before(now)) add(Calendar.WEEK_OF_YEAR, 1)
            }
            val delay = target.timeInMillis - now.timeInMillis
            val req = PeriodicWorkRequestBuilder<WeeklyReportWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "weekly_report", ExistingPeriodicWorkPolicy.UPDATE, req
            )
        }
    }
}
