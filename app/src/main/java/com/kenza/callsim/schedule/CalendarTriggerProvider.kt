package com.kenza.callsim.initiative

import android.content.Context
import android.provider.CalendarContract
import android.util.Log

class CalendarTriggerProvider(private val context: Context) {
    fun hasRecentEventEnded(): Pair<Boolean, String?> {
        val now = System.currentTimeMillis()
        val fifteenMinsAgo = now - (15 * 60 * 1000)
        
        val projection = arrayOf(CalendarContract.Events.TITLE)
        val selection = "${CalendarContract.Events.DTEND} >= ? AND ${CalendarContract.Events.DTEND} <= ?"
        val selectionArgs = arrayOf(fifteenMinsAgo.toString(), now.toString())

        return try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection, selection, selectionArgs, null
            )?.use {
                if (it.moveToFirst()) {
                    val title = it.getString(0)
                    true to "Your event '$title' just ended"
                } else false to null
                }
            } ?: (false to null)
        } catch (e: Exception) {
            Log.e("CalendarProvider", "Query failed", e)
            false to null
        }
    }
}
