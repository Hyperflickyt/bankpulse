package com.bankpulse.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.bankpulse.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Catches new bank SMS the instant they arrive and stores any transaction it finds. */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        // Multipart SMS arrive as several parts of one message; join them.
        val sender = msgs.firstOrNull()?.displayOriginatingAddress ?: return
        val body = msgs.joinToString("") { it.displayMessageBody ?: "" }
        val now = System.currentTimeMillis()

        val txn = SmsParser.parse(sender, body, now) ?: return
        val dao = AppDatabase.get(context).txnDao()
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { dao.insert(txn) } finally { pending.finish() }
        }
    }
}
