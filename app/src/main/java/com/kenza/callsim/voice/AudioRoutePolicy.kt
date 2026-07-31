package com.kenza.callsim.voice

import android.media.AudioDeviceInfo
import android.os.Build

/** One auditable switch for echo-safe barge-in behavior. */
object AudioRoutePolicy {
    /**
     * Disabled by default: built-in phone routes can feed Kenza's playback back
     * into Gemini as false user speech even when platform AEC is available.
     * Dedicated headset microphones remain full duplex for real barge-in.
     */
    @Volatile var earpieceFullDuplexEnabled: Boolean = false

    fun allowsFullDuplex(inputDeviceType: Int?, speakerphoneOn: Boolean): Boolean {
        if (speakerphoneOn || inputDeviceType == null) return false
        return when (inputDeviceType) {
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> earpieceFullDuplexEnabled
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET -> true
            else -> Build.VERSION.SDK_INT >= 31 && inputDeviceType == AudioDeviceInfo.TYPE_BLE_HEADSET
        }
    }
}

enum class PlaybackQueueAction {
    ACCEPT,
    WARN,
}

/**
 * Pure queue arithmetic shared by the Android player and JVM tests.
 *
 * A backlog is diagnostic information, not permission to delete valid speech.
 * The player may flush only after an explicit provider interruption.
 */
class PlaybackQueuePolicy(
    private val sampleRate: Int,
    private val warningMs: Int = GeminiLiveTuning.MAX_PLAYBACK_QUEUE_MS,
) {
    private val warningBytes = bytesFor(warningMs)

    init {
        require(sampleRate > 0) { "sampleRate must be positive" }
        require(warningMs > 0) { "warningMs must be positive" }
    }

    fun durationMs(bytes: Int): Int =
        ((bytes.coerceAtLeast(0).toLong() * 1_000L) /
            (sampleRate.toLong() * AudioConfig.BYTES_PER_SAMPLE)).toInt()

    fun action(currentBytes: Int, incomingBytes: Int): PlaybackQueueAction {
        val totalBytes = currentBytes.coerceAtLeast(0).toLong() +
            incomingBytes.coerceAtLeast(0).toLong()
        return if (totalBytes > warningBytes) PlaybackQueueAction.WARN
        else PlaybackQueueAction.ACCEPT
    }

    fun accepts(currentBytes: Int, incomingBytes: Int): Boolean =
        action(currentBytes, incomingBytes) == PlaybackQueueAction.ACCEPT

    private fun bytesFor(durationMs: Int): Long =
        sampleRate.toLong() * AudioConfig.BYTES_PER_SAMPLE * durationMs / 1_000L
}
