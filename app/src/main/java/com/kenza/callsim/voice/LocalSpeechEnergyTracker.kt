package com.kenza.callsim.voice

/**
 * Lightweight local energy gate used only for content-free turn timing (#59).
 *
 * This is intentionally not a barge-in controller and does not change what audio
 * is streamed to the provider. It only reports when mic PCM that already passed
 * MicRecorder's mute/echo gates looks like speech start or speech end.
 */
internal class LocalSpeechEnergyTracker(
    private val speechRmsThreshold: Int = DEFAULT_SPEECH_RMS,
    private val endHangoverChunks: Int = DEFAULT_END_HANGOVER_CHUNKS,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) {
    enum class Event { START, END }

    private var speaking = false
    private var quietChunks = 0
    private var lastStartMs: Long? = null
    private var lastEndMs: Long? = null

    fun onPcm(pcm: ByteArray): Event? {
        if (pcm.isEmpty()) return null
        val energy = pcm16Rms(pcm)
        return if (energy >= speechRmsThreshold) {
            quietChunks = 0
            if (!speaking) {
                speaking = true
                lastStartMs = clockMs()
                Event.START
            } else {
                null
            }
        } else if (speaking) {
            quietChunks += 1
            if (quietChunks >= endHangoverChunks) {
                speaking = false
                quietChunks = 0
                lastEndMs = clockMs()
                Event.END
            } else {
                null
            }
        } else {
            null
        }
    }

    /** Force an END if the mic stops while speech was open (mute / session stop). */
    fun forceEnd(): Event? {
        if (!speaking) return null
        speaking = false
        quietChunks = 0
        lastEndMs = clockMs()
        return Event.END
    }

    fun reset() {
        speaking = false
        quietChunks = 0
        lastStartMs = null
        lastEndMs = null
    }

    fun lastStartMs(): Long? = lastStartMs
    fun lastEndMs(): Long? = lastEndMs
    fun isSpeaking(): Boolean = speaking

    companion object {
        /** ~relative PCM energy; calibrated for 16-bit mono speech, not absolute dB. */
        const val DEFAULT_SPEECH_RMS = 900
        /** ~320 ms of quiet (8 × 40 ms) before local speech is considered ended. */
        const val DEFAULT_END_HANGOVER_CHUNKS = 8

        fun pcm16Rms(pcm: ByteArray): Int {
            val samples = pcm.size / 2
            if (samples <= 0) return 0
            var sumSquares = 0.0
            var index = 0
            while (index + 1 < pcm.size) {
                val sample = (pcm[index].toInt() and 0xff) or (pcm[index + 1].toInt() shl 8)
                val signed = sample.toShort().toInt()
                sumSquares += signed.toDouble() * signed.toDouble()
                index += 2
            }
            return kotlin.math.sqrt(sumSquares / samples).toInt()
        }
    }
}
