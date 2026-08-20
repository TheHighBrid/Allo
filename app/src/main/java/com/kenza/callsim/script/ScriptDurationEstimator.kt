package com.kenza.callsim.script

import kotlin.math.roundToInt

/** A duration estimate that separates Kenza's speech from the listener's response time. */
data class ScriptDurationEstimate(
    val speechSeconds: Int,
    val listenerPauseSeconds: Int,
) {
    val totalSeconds: Int get() = speechSeconds + listenerPauseSeconds
}

/**
 * Estimates a one-sided call from words spoken by Kenza and explicit listener-pause directions.
 * Bracketed directions are intentionally excluded from the speech-word count because they are
 * production metadata rather than spoken dialogue.
 */
object ScriptDurationEstimator {

    fun estimate(text: String): ScriptDurationEstimate {
        val spokenText = BRACKETED_DIRECTION.replace(text, " ")
        val wordCount = WORD.findAll(spokenText).count()
        val speechSeconds = if (wordCount == 0) 0 else {
            (wordCount * SECONDS_PER_MINUTE / WORDS_PER_MINUTE).roundToInt().coerceAtLeast(1)
        }
        val listenerPauseSeconds = pauseSeconds(text).roundToInt()

        return ScriptDurationEstimate(
            speechSeconds = speechSeconds,
            listenerPauseSeconds = listenerPauseSeconds,
        )
    }

    private fun pauseSeconds(text: String): Double {
        val quantifiedPauses = QUANTIFIED_PAUSE.findAll(text).sumOf { match ->
            match.groupValues[1].toDouble()
        }
        val normalized = text.lowercase()
        val briefSilences = BRIEF_SILENCE.findAll(normalized).count() * BRIEF_SILENCE_SECONDS
        val longerListeningPauses = LONGER_LISTENING_PAUSE.findAll(normalized).count() *
            LONGER_LISTENING_PAUSE_SECONDS
        return quantifiedPauses + briefSilences + longerListeningPauses
    }

    private const val WORDS_PER_MINUTE = 150.0
    private const val SECONDS_PER_MINUTE = 60.0
    private const val BRIEF_SILENCE_SECONDS = 1.0
    private const val LONGER_LISTENING_PAUSE_SECONDS = 8.0

    private val BRACKETED_DIRECTION = Regex("\\[[^]]*]", RegexOption.IGNORE_CASE)
    private val WORD = Regex("\\b[\\p{L}\\p{N}][\\p{L}\\p{N}'’-]*\\b")
    private val QUANTIFIED_PAUSE = Regex(
        "\\[(?:listening\\s+)?pause\\s+(\\d+(?:\\.\\d+)?)\\s+seconds?]",
        RegexOption.IGNORE_CASE,
    )
    private val BRIEF_SILENCE = Regex("\\[brief silence]", RegexOption.IGNORE_CASE)
    private val LONGER_LISTENING_PAUSE = Regex("\\[longer listening pause]", RegexOption.IGNORE_CASE)
}
