package com.kenza.callsim.initiative

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log

/**
 * Calendar-based trigger: an event that ended in the last [WINDOW_MS] can give Kenza a
 * natural reason to call ("how did it go?").
 *
 * Not yet wired into [InitiativeScheduler]. READ_CALENDAR is intentionally not declared in
 * the manifest yet, so [hasRecentEventEnded] returns no trigger without querying anything
 * until that permission is added and granted.
 */
class CalendarTriggerProvider(private val context: Context) {

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun hasRecentEventEnded(now: Long = System.currentTimeMillis()): Pair<Boolean, String?> {
        if (!hasPermission()) return false to null

        val projection = arrayOf(CalendarContract.Events.TITLE)
        val selection = "${CalendarContract.Events.DTEND} >= ? AND ${CalendarContract.Events.DTEND} <= ?"
        val selectionArgs = arrayOf((now - WINDOW_MS).toString(), now.toString())

        return try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection, selection, selectionArgs, null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) reasonFor(cursor.getString(0)) else false to null
            } ?: (false to null)
        } catch (e: SecurityException) {
            // Calendar titles are private, so only the exception type is logged.
            Log.w(TAG, "Calendar query denied: ${e.javaClass.simpleName}")
            false to null
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Calendar query rejected: ${e.javaClass.simpleName}")
            false to null
        }
    }

    companion object {
        private const val TAG = "CalendarProvider"
        const val WINDOW_MS = 15 * 60 * 1000L

        fun reasonFor(title: String?): Pair<Boolean, String?> {
            val clean = title?.trim().orEmpty()
            return true to if (clean.isEmpty()) "Your event just ended" else "Your event '$clean' just ended"
        }
    }
}
