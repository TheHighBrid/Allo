package com.kenza.callsim.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kenza.callsim.MainActivity
import com.kenza.callsim.initiative.InitiativeScheduler

/**
 * Receives fired explicit schedules, contextual initiative checks, and boot
 * events. Initiative checks are not calls by themselves: fresh memory and
 * timing are evaluated first, and the phone rings only when the decision engine
 * approves a meaningful reason to interrupt.
 */
class ScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (intent.action) {
            ACTION_FIRE -> {
                val id = intent.getStringExtra(EXTRA_ID) ?: return
                ring(app, initiativeToken = null)
                CallScheduler(app).onFired(id)
            }

            ACTION_INITIATIVE_CHECK -> {
                val seed = InitiativeScheduler(app).evaluateAndSchedule()
                if (seed != null) ring(app, initiativeToken = seed.token)
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                CallScheduler(app).rescheduleAll()
                InitiativeScheduler(app).ensureScheduled()
            }
        }
    }

    private fun ring(context: Context, initiativeToken: String?) {
        // 1) Ring + vibrate + wake immediately, and post the full-screen intent
        // notification. The opaque token contains no memory text.
        IncomingCallService.start(context, initiativeToken)
        // 2) Also try to launch directly. This is the most reliable path over
        // the lock screen when "appear on top" is granted.
        runCatching {
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(MainActivity.EXTRA_INCOMING_CALL, true)
                    initiativeToken?.let { putExtra(MainActivity.EXTRA_INITIATIVE_TOKEN, it) }
                }
            )
        }.onFailure { Log.w("ScheduleReceiver", "direct launch blocked: ${it.message}") }
    }

    companion object {
        const val ACTION_FIRE = "com.kenza.callsim.SCHEDULE_FIRE"
        const val ACTION_INITIATIVE_CHECK = "com.kenza.callsim.INITIATIVE_CHECK"
        const val EXTRA_ID = "id"
    }
}
