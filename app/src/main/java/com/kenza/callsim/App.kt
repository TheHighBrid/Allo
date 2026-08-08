package com.kenza.callsim

import android.app.Application
import com.kenza.callsim.initiative.InitiativeScheduler
import com.kenza.callsim.schedule.CallScheduler
import com.kenza.callsim.schedule.IncomingCallNotifier

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        IncomingCallNotifier.ensureChannel(this)
        // Recover explicit alarms after a force-stop / update (boot is handled separately).
        CallScheduler(this).rescheduleAll()
        // Keep one future contextual evaluation armed. The evaluation may decide
        // not to call; the alarm is only permission for Kenza to reconsider.
        InitiativeScheduler(this).ensureScheduled()
    }
}
