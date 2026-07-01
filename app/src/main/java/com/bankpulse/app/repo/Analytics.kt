package com.bankpulse.app.repo

import com.bankpulse.app.data.Transaction
import com.bankpulse.app.data.TxnType
import java.util.Calendar
import kotlin.math.abs

data class BankBalance(val bank: String, val tail: String, val balance: Double)
data class CategorySpend(val category: String, val amount: Double)
data class Recurring(val merchant: String, val typicalAmount: Double, val occurrences: Int, val cadence: String)
data class FlowNode(val name: String, val amount: Double)
data class MonthFlow(val label: String, val credit: Double, val debit: Double)

data class Insights(
    val totalBalance: Double,
    val moneyIn: Double,
    val moneyOut: Double,
    val mainProvider: FlowNode?,        // biggest single income source
    val topDestination: FlowNode?,      // biggest single outflow target
    val byCategory: List<CategorySpend>,
    val recurring: List<Recurring>,
    val incomeSources: List<FlowNode>,
    val outflowTargets: List<FlowNode>
)

/** Pure functions over a transaction list — easy to unit test, no Android deps. */
object Analytics {

    fun build(all: List<Transaction>, balances: List<Transaction>, windowDays: Int = 30): Insights {
        // Totals and breakdowns run over ALL transactions so the numbers on the
        // dashboard always reconcile with the full list on the Transactions tab.
        val recent = all

        val totalBalance = balances.sumOf { it.balanceAfter ?: 0.0 }
        val moneyIn  = recent.filter { it.type == TxnType.CREDIT }.sumOf { it.amount }
        val moneyOut = recent.filter { it.type == TxnType.DEBIT }.sumOf { it.amount }

        val incomeSources = recent.filter { it.type == TxnType.CREDIT }
            .groupBy { it.merchant }
            .map { (m, txns) -> FlowNode(m, txns.sumOf { it.amount }) }
            .sortedByDescending { it.amount }

        val outflowTargets = recent.filter { it.type == TxnType.DEBIT }
            .groupBy { if (it.category == "Other") it.merchant else it.category }
            .map { (m, txns) -> FlowNode(m, txns.sumOf { it.amount }) }
            .sortedByDescending { it.amount }

        val byCategory = recent.filter { it.type == TxnType.DEBIT }
            .groupBy { it.category }
            .map { (c, txns) -> CategorySpend(c, txns.sumOf { it.amount }) }
            .sortedByDescending { it.amount }

        return Insights(
            totalBalance = totalBalance,
            moneyIn = moneyIn,
            moneyOut = moneyOut,
            mainProvider = incomeSources.firstOrNull(),
            topDestination = outflowTargets.firstOrNull(),
            byCategory = byCategory,
            recurring = detectRecurring(all),
            incomeSources = incomeSources.take(4),
            outflowTargets = outflowTargets.take(5)
        )
    }

    /**
     * A merchant is "recurring" when it's charged a similar amount on a regular
     * cadence. We look across ~3 months: >=2 hits with stable amount = recurring.
     */
    private fun detectRecurring(all: List<Transaction>): List<Recurring> {
        val since = System.currentTimeMillis() - 100L * 24 * 60 * 60 * 1000
        return all.filter { it.type == TxnType.DEBIT && it.timestamp >= since }
            .groupBy { it.merchant.lowercase() }
            .mapNotNull { (_, txns) ->
                if (txns.size < 2) return@mapNotNull null
                val amounts = txns.map { it.amount }
                val avg = amounts.average()
                // amounts must be within ~12% of each other to count as "the same" charge
                val stable = amounts.all { abs(it - avg) <= avg * 0.12 }
                if (!stable) return@mapNotNull null
                Recurring(
                    merchant = txns.first().merchant,
                    typicalAmount = avg,
                    occurrences = txns.size,
                    cadence = cadenceOf(txns.map { it.timestamp }.sorted())
                )
            }
            .sortedByDescending { it.typicalAmount }
    }

    private fun cadenceOf(sortedTimes: List<Long>): String {
        if (sortedTimes.size < 2) return "Recurring"
        val gaps = sortedTimes.zipWithNext { a, b -> (b - a) / (24.0 * 60 * 60 * 1000) }
        val avgGap = gaps.average()
        return when {
            avgGap <= 9    -> "Weekly"
            avgGap <= 45   -> "Monthly"
            avgGap <= 110  -> "Quarterly"
            else           -> "Yearly"
        }
    }

    fun dayOfWeekKey(ts: Long): Int =
        Calendar.getInstance().apply { timeInMillis = ts }.get(Calendar.DAY_OF_WEEK)

    /** Credit vs debit totals bucketed by the last [months] calendar months. */
    fun monthlyInOut(all: List<Transaction>, months: Int = 6): List<MonthFlow> {
        val keyFmt = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
        val labFmt = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
        val credit = LinkedHashMap<String, Double>()
        val debit = LinkedHashMap<String, Double>()
        val label = LinkedHashMap<String, String>()

        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -(months - 1))
        repeat(months) {
            val k = keyFmt.format(cal.time)
            credit[k] = 0.0; debit[k] = 0.0; label[k] = labFmt.format(cal.time)
            cal.add(Calendar.MONTH, 1)
        }

        all.forEach { t ->
            val k = keyFmt.format(java.util.Date(t.timestamp))
            if (!credit.containsKey(k)) return@forEach
            if (t.type == TxnType.CREDIT) credit[k] = credit[k]!! + t.amount
            else debit[k] = debit[k]!! + t.amount
        }
        return credit.keys.map { k -> MonthFlow(label[k]!!, credit[k]!!, debit[k]!!) }
    }
}
