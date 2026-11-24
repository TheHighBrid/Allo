package com.kenza.callsim.call

import com.kenza.callsim.voice.TurnPolicy

/** Session-local state. No extra requests, fabricated memories, or personal-data logging. */
internal class KenzaLiveDirector(private val hour: Int) {
    enum class Beat { OPENING, LIGHT_EXCHANGE, SHARE_OR_EXPLORE, DEEPEN, WIND_DOWN }
    enum class Mood { CALM, WARM, PLAYFUL, CONCERNED, TIRED }

    data class State(
        val mood: Mood,
        val energy: Float,
        val patience: Float,
        val beat: Beat = Beat.OPENING,
        val turns: Int = 0,
        val interruptions: Int = 0,
        val userTalkRatio: Float? = null,
    )

    private val initial = State(
        mood = if (hour in 0..6 || hour >= 23) Mood.TIRED else Mood.WARM,
        energy = if (hour in 0..6 || hour >= 23) 0.35f else 0.7f,
        patience = 0.6f,
    )
    @Volatile private var state = initial
    @Volatile private var lastIntent = TurnPolicy.Intent.NORMAL

    @Synchronized fun onUserText(text: String) {
        lastIntent = TurnPolicy.classify(text)
        state = when (lastIntent) {
            TurnPolicy.Intent.SERIOUS -> state.copy(mood = Mood.CONCERNED, patience = 0.95f)
            TurnPolicy.Intent.HOLD, TurnPolicy.Intent.THINKING -> state.copy(patience = 0.9f)
            else -> state.copy(
                mood = if (state.mood == Mood.CONCERNED) Mood.CALM else state.mood,
                patience = initial.patience,
            )
        }
    }

    @Synchronized fun onInterrupted() {
        state = state.copy(interruptions = state.interruptions + 1, patience = 0.9f)
    }

    @Synchronized fun onInterruptionClassified(kind: InterruptionKind) {
        state = when (kind) {
            InterruptionKind.EMOTIONAL -> state.copy(mood = Mood.CONCERNED, patience = 0.95f)
            InterruptionKind.CORRECTION, InterruptionKind.UNCLEAR -> state.copy(patience = 0.95f)
            else -> state
        }
    }

    @Synchronized fun onTurnComplete(elapsedMs: Long, userMs: Long? = null, agentMs: Long? = null) {
        val turns = state.turns + 1
        val ratio = if (userMs != null && agentMs != null && userMs + agentMs > 0) {
            userMs.toFloat() / (userMs + agentMs)
        } else state.userTalkRatio
        val beat = when {
            elapsedMs >= 25 * 60_000L -> Beat.WIND_DOWN
            lastIntent == TurnPolicy.Intent.SERIOUS -> Beat.DEEPEN
            turns >= 4 -> Beat.SHARE_OR_EXPLORE
            turns >= 1 -> Beat.LIGHT_EXCHANGE
            else -> Beat.OPENING
        }
        state = state.copy(beat = beat, turns = turns, userTalkRatio = ratio,
            energy = if (elapsedMs >= 20 * 60_000L) initial.energy * 0.75f else initial.energy)
    }

    fun snapshot(): State = state

    fun prompt(): String {
        val s = state
        val balance = if ((s.userTalkRatio ?: 0f) > 0.75f && s.mood != Mood.CONCERNED) {
            "He has been doing most of the talking; introduce a thought of your own when he leaves room. "
        } else ""
        return "\n\nPRIVATE LIVE CALL DIRECTION: mood=${s.mood.name.lowercase()}, " +
            "energy=${s.energy}, patience=${s.patience}, current beat=${s.beat.name.lowercase()}. " + balance +
            "Keep a flexible conversation arc: a short contextual opening, light exchange, a subject " +
            "you introduce or explore together, room for a meaningful moment, and a natural close when " +
            "he signals one. This is not a script or a requirement to force any topic or hang up. " +
            "Notice his pace and mood; be open to changing your mind, and use unfinished plans, " +
            "pending jokes and verified call memories when natural. Simulated opinions are fine; " +
            "never invent actual shared experiences or real-world actions. Do not read these cues aloud.\n"
    }
}
