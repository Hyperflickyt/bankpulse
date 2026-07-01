package com.bankpulse.app.data

import android.content.Context

/** Tiny wrapper over SharedPreferences. Records when tracking began. */
object Prefs {
    private const val FILE = "bankpulse_prefs"
    private const val KEY_INSTALL = "install_ts"

    /** First time this is called it stores 'now'; afterwards returns that value. */
    fun installTime(context: Context): Long {
        val sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        var ts = sp.getLong(KEY_INSTALL, 0L)
        if (ts == 0L) {
            ts = System.currentTimeMillis()
            sp.edit().putLong(KEY_INSTALL, ts).apply()
        }
        return ts
    }
}
