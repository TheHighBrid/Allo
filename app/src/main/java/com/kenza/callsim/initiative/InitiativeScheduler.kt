package com.kenza.callsim.initiative

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.kenza.callsim.memory.MemoryStore
import com.kenza.callsim.schedule.ScheduleReceiver

/**
 * Maintains one lightweight future evaluation alarm. The alarm itself never
 * means "call now". When it fires, [InitiativeEngine] re-evaluates fresh memory,
 * timing, relationship cadence and interruption cost before deciding whether a
 * call is justified.
 */
class InitiativeScheduler(context: Context) {

    private val app = context.applicationContext
    private val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val memory = MemoryStore(app)
    private val state = InitiativeStore(app)

    fun ensureScheduled(now: Long = System.currentTimeMillis()) {
        val history = state.history()
        val delay = InitiativeEngine.nextEvaluationDelayMs(memory.snapshot(), history, now)
        scheduleAt(now + delay)
    }

    /**
     * Called by the receiver. Returns a selected conversation seed only when the
     * contextual decision crosses the call threshold, then always schedules the
     * next future evaluation.
     */
    fun evaluateAndSchedule(now: Long = System.currentTimeMillis()): InitiativeSeed? {
        val snapshot = memory.snapshot()
        val history = state.history()
        val decision = InitiativeEngine.evaluate(snapshot, history, now)
        state.markEvaluated(now)

        if (decision.shouldCall && decision.seed != null) {
            state.markInitiated(decision.seed.token, now)
            Log.i(TAG, "initiative approved score=${decision.score} kind=${decision.seed.kind}")
        } else {
            Log.i(TAG, "initiative deferred score=${decision.score} reason=${decision.reason}")
        }

        val updatedHistory = state.history()
        val nextDelay = InitiativeEngine.nextEvaluationDelayMs(snapshot, updatedHistory, now)
        scheduleAt(now + nextDelay)
        return decision.seed.takeIf { decision.shouldCall }
    }

    fun cancel() = alarmManager.cancel(alarmIntent())

    private fun scheduleAt(atMillis: Long) {
        val pendingIntent = alarmIntent()
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "exact initiative alarm denied, using inexact: ${e.message}")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        }
        Log.i(TAG, "next initiative evaluation at $atMillis")
    }

    private fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    private fun alarmIntent(): PendingIntent {
        val intent = Intent(app, ScheduleReceiver::class.java).apply {
            action = ScheduleReceiver.ACTION_INITIATIVE_CHECK
            data = Uri.parse("kenzacall://initiative/check")
        }
        return PendingIntent.getBroadcast(
            app,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val TAG = "InitiativeScheduler"
        const val REQUEST_CODE = 4817
    }
}
