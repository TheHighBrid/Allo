package com.kenza.callsim.voice

/**
 * Latency-sensitive values kept in one place so changes are visible, testable
 * and do not drift between microphone capture and Gemini session setup.
 */
object GeminiLiveTuning {
    /** Google recommends 20-40 ms microphone chunks for Live API audio. */
    const val INPUT_CHUNK_MS = 40

    /** Preserve a tiny amount of speech before server VAD detects its start. */
    const val VAD_PREFIX_PADDING_MS = 20

    /**
     * Server-side silence fallback when the client does not send audioStreamEnd.
     * Google's recommended window is ~500–800 ms; keep the high end so natural
     * mid-sentence pauses are not split when hybrid client endpointing misses.
     */
    const val VAD_SILENCE_DURATION_MS = 800

    /**
     * Local end-of-turn hangover used to fire audioStreamEnd (hybrid VAD).
     * 13 × 40 ms ≈ 520 ms — inside Google's recommended client endpoint zone,
     * faster than waiting for the full server silence timeout alone.
     */
    const val CLIENT_ENDPOINT_HANGOVER_CHUNKS = 13

    /** Default client playback buffer; adaptive controller may raise this on underruns. */
    const val OUTPUT_BUFFER_MS = 80

    /** Floor for adaptive playback buffering (lowest latency). */
    const val OUTPUT_BUFFER_MIN_MS = 40

    /** Ceiling for adaptive playback buffering (stability under jitter). */
    const val OUTPUT_BUFFER_MAX_MS = 160

    /** Soft diagnostics threshold. Crossing it must never discard valid speech. */
    const val MAX_PLAYBACK_QUEUE_MS = 400

    /** Bound each blocking AudioTrack write so true interruptions flush quickly. */
    const val PLAYBACK_WRITE_CHUNK_MS = 20

    /** Covers the hardware/output tail after the app queue has drained. */
    const val PLAYBACK_TAIL_GUARD_MS = 160

    /** Minimum gap between consecutive client audioStreamEnd signals. */
    const val CLIENT_ENDPOINT_MIN_INTERVAL_MS = 700L
}

/**
 * Raises or lowers the effective playback buffer from underrun / backlog signals.
 * Pure logic so JVM unit tests can drive it without AudioTrack.
 */
class AdaptivePlaybackBuffer(
    initialMs: Int = GeminiLiveTuning.OUTPUT_BUFFER_MS,
    private val minMs: Int = GeminiLiveTuning.OUTPUT_BUFFER_MIN_MS,
    private val maxMs: Int = GeminiLiveTuning.OUTPUT_BUFFER_MAX_MS,
) {
    @Volatile
    var currentMs: Int = initialMs.coerceIn(minMs, maxMs)
        private set

    /** Audible underrun: grow buffer toward the ceiling. */
    fun onUnderrun(delta: Int = 1) {
        if (delta <= 0) return
        val step = (10 * delta).coerceAtMost(40)
        currentMs = (currentMs + step).coerceAtMost(maxMs)
    }

    /** Clean turn with no underrun and light queue: ease toward the floor. */
    fun onHealthyTurn(queueMs: Int) {
        if (queueMs > GeminiLiveTuning.MAX_PLAYBACK_QUEUE_MS / 2) return
        if (currentMs > minMs) {
            currentMs = (currentMs - 5).coerceAtLeast(minMs)
        }
    }

    fun reset() {
        currentMs = GeminiLiveTuning.OUTPUT_BUFFER_MS.coerceIn(minMs, maxMs)
    }
}
