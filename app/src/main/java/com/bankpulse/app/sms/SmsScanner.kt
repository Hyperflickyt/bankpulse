package com.bankpulse.app.sms

import android.content.Context
import android.provider.Telephony
import com.bankpulse.app.data.Transaction

/** One-time backfill: reads the existing SMS inbox so history shows up immediately. */
object SmsScanner {

    /** Parses the inbox and returns every transaction found. Run on an IO dispatcher. */
    fun scanInbox(context: Context): List<Transaction> {
        val parsed = mutableListOf<Transaction>()
        val cols = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI, cols, null, null,
            "${Telephony.Sms.DATE} DESC"
        )?.use { c ->
            val iA = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val iB = c.getColumnIndex(Telephony.Sms.BODY)
            val iD = c.getColumnIndex(Telephony.Sms.DATE)
            while (c.moveToNext()) {
                val sender = c.getString(iA) ?: continue
                val body = c.getString(iB) ?: continue
                val date = c.getLong(iD)
                SmsParser.parse(sender, body, date)?.let { parsed.add(it) }
            }
        }
        return parsed
    }
}
