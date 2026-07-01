package com.bankpulse.app

import android.app.Application
import com.bankpulse.app.work.WeeklyReportWorker

class BankPulseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Make sure the weekly report is always scheduled.
        WeeklyReportWorker.schedule(this)
    }
}
