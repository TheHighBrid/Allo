package com.kenza.callsim.voice

import java.util.Locale

/** Local, deterministic pacing. No extra model request or transcript persistence. */
internal object TurnPolicy {
    enum class Intent(val silenceMs: Int) {
        QUICK(320), NORMAL(560), THINKING(1200), SERIOUS(2000), HOLD(4000)
    }

    fun classify(text: String, seriousContext: Boolean = false): Intent {
        val tail = text.lowercase(Locale.ROOT).trim()
        return when {
            Regex("""\b(hold on|one second|give me a second|wait(?: a second)?|let me think|attends|une seconde|chwia|sber)\b[.!?…]*$""").containsMatchIn(tail) -> Intent.HOLD
            tail.endsWith("...") || tail.endsWith("…") ||
                Regex("""\b(um|uh|euh|i mean|je pense|genre)$""").containsMatchIn(tail) -> Intent.THINKING
            Regex("""\b(upset|sad|scared|grief|worried|hurt|triste|inquiet|peur|deuil)\b""").containsMatchIn(tail) || seriousContext -> Intent.SERIOUS
            tail.endsWith("?") -> Intent.QUICK
            else -> Intent.NORMAL
        }
    }

    fun quietChunks(intent: Intent): Int =
        (intent.silenceMs + GeminiLiveTuning.INPUT_CHUNK_MS - 1) / GeminiLiveTuning.INPUT_CHUNK_MS
}

/**
 * Manual Gemini activity protocol: pre-roll, activityStart, PCM, activityEnd.
 * Never emits audio outside an activity. A bounded hold avoids dead calls.
 * Callers serialize access on the provider connection lock.
 */
internal class AdaptiveActivityGate(
    private val threshold: Int = LocalSpeechEnergyTracker.DEFAULT_SPEECH_RMS,
) {
    data class Decision(
        val start: Boolean = false,
        val packets: List<ByteArray> = emptyList(),
        val end: Boolean = false,
    )

    private val preroll = ArrayDeque<ByteArray>()
    private var voicedChunks = 0
    private var quietChunks = 0
    private var active = false
    private var seriousContext = false
    private var tail = ""

    fun onTranscript(fragment: String) {
        if (fragment.isBlank()) return
        if (active) {
            if (!tail.endsWith(fragment)) tail = (tail + " " + fragment).takeLast(240)
        } else {
            seriousContext = TurnPolicy.classify(fragment) == TurnPolicy.Intent.SERIOUS
        }
    }

    fun onAudio(pcm: ByteArray): Decision {
        if (pcm.isEmpty()) return Decision()
        val voiced = LocalSpeechEnergyTracker.pcm16Rms(pcm) >= threshold
        if (!active) {
            preroll.addLast(pcm.copyOf())
            while (preroll.size > 3) preroll.removeFirst()
            voicedChunks = if (voiced) voicedChunks + 1 else 0
            if (voicedChunks < 2) return Decision()
            active = true
            val packets = preroll.toList()
            preroll.clear()
            return Decision(start = true, packets = packets)
        }
        if (voiced && quietChunks >= 12 && TurnPolicy.classify(tail) == TurnPolicy.Intent.HOLD) {
            tail = ""
        }
        quietChunks = if (voiced) 0 else quietChunks + 1
        if (quietChunks >= TurnPolicy.quietChunks(TurnPolicy.classify(tail, seriousContext))) {
            finish()
            return Decision(end = true)
        }
        return Decision(packets = listOf(pcm))
    }

    fun finish(): Boolean {
        val wasActive = active
        if (tail.isNotBlank()) seriousContext = TurnPolicy.classify(tail) == TurnPolicy.Intent.SERIOUS
        active = false
        voicedChunks = 0
        quietChunks = 0
        tail = ""
        preroll.clear()
        return wasActive
    }

    fun reset() {
        finish()
        seriousContext = false
    }
}
