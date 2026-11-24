package com.kenza.callsim.voice

import java.io.ByteArrayOutputStream
import kotlin.random.Random

/**
 * Same-voice, call-local clip bank. Only naturally generated, complete, short
 * neutral acknowledgments are reusable; no external TTS, persistence or replay
 * of arbitrary conversational audio.
 */
internal class ReactiveListening(
    private val sampleRate: Int,
    private val cooldownMs: () -> Long = { Random.nextLong(22_000L, 45_001L) },
) {
    private val maxBytes = sampleRate * 2 * 1100 / 1000
    private val minBytes = sampleRate * 2 / 10
    private val capture = ByteArrayOutputStream()
    private var candidateText = ""
    private var oversized = false
    private var cached: ByteArray? = null
    private var userText = ""
    private var speechStart = 0L
    private var playedThisMonologue = false
    private var nextAllowedMs = 0L

    @Synchronized fun agentAudio(pcm: ByteArray) {
        if (oversized) return
        if (capture.size() + pcm.size > maxBytes) {
            oversized = true
            capture.reset()
        } else capture.write(pcm)
    }

    @Synchronized fun agentText(text: String) {
        candidateText = append(candidateText, text)
    }

    @Synchronized fun complete(interrupted: Boolean) {
        val text = candidateText.lowercase().replace(Regex("""[^a-z]"""), "")
        if (!interrupted && !oversized && capture.size() in minBytes..maxBytes &&
            text in setOf("mm", "mmm", "mhm", "mmhm", "hmm", "hm")) {
            cached = quiet(capture.toByteArray())
        }
        capture.reset()
        candidateText = ""
        oversized = false
    }

    @Synchronized fun speechStarted(nowMs: Long) {
        speechStart = nowMs
        userText = ""
        playedThisMonologue = false
    }

    @Synchronized fun userText(text: String) { userText = append(userText, text) }

    @Synchronized fun reaction(nowMs: Long, safeRoute: Boolean, neutralContext: Boolean): ByteArray? {
        if (!safeRoute || !neutralContext || playedThisMonologue || speechStart == 0L ||
            nowMs - speechStart < 6_000L || nowMs < nextAllowedMs) return null
        if (userText.trim().split(Regex("""\s+""")).size < 4 ||
            TurnPolicy.classify(userText) != TurnPolicy.Intent.NORMAL) return null
        return cached?.copyOf()
    }

    @Synchronized fun played(nowMs: Long) {
        playedThisMonologue = true
        nextAllowedMs = nowMs + cooldownMs()
    }

    private fun append(old: String, fragment: String): String {
        if (fragment.isBlank() || old.endsWith(fragment)) return old
        return if (fragment.startsWith(old)) fragment.takeLast(240) else (old + " " + fragment).takeLast(240)
    }

    private fun quiet(pcm: ByteArray): ByteArray {
        val output = pcm.copyOf()
        for (i in 0 until pcm.size - 1 step 2) {
            val value = ((pcm[i].toInt() and 255) or (pcm[i + 1].toInt() shl 8)).toShort().toInt() / 10
            output[i] = value.toByte()
            output[i + 1] = (value shr 8).toByte()
        }
        return output
    }
}
