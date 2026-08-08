package com.kenza.callsim.initiative

import android.content.Context

/**
 * Stores only initiative metadata. Conversation text remains in the encrypted
 * MemoryStore; this preference file keeps timestamps and opaque seed tokens so
 * Kenza can avoid repetitive or over-frequent autonomous calls.
 */
class InitiativeStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("kenza_initiative_state", Context.MODE_PRIVATE)

    fun history(): InitiativeHistory = InitiativeHistory(
        lastInitiatedAt = prefs.getLong(KEY_LAST_INITIATED, 0L),
        recentTokens = prefs.getStringSet(KEY_RECENT_TOKENS, emptySet()).orEmpty().toSet(),
    )

    fun markEvaluated(now: Long) {
        prefs.edit().putLong(KEY_LAST_EVALUATED, now).apply()
    }

    fun lastEvaluatedAt(): Long = prefs.getLong(KEY_LAST_EVALUATED, 0L)

    fun markInitiated(token: String, now: Long) {
        val tokens = buildList {
            add(token)
            addAll(history().recentTokens.filterNot { it == token })
        }.take(MAX_RECENT_TOKENS).toSet()

        prefs.edit()
            .putLong(KEY_LAST_INITIATED, now)
            .putStringSet(KEY_RECENT_TOKENS, tokens)
            .apply()
    }

    private companion object {
        const val KEY_LAST_EVALUATED = "last_evaluated_at"
        const val KEY_LAST_INITIATED = "last_initiated_at"
        const val KEY_RECENT_TOKENS = "recent_tokens"
        const val MAX_RECENT_TOKENS = 8
    }
}
