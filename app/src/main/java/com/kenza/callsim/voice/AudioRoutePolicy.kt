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
    RESYNC,
}

/** Pure queue arithmetic shared by the Android player and JVM tests. */
class PlaybackQueuePolicy(
    private val sampleRate: Int,
    private val maximumMs: Int = GeminiLiveTuning.MAX_PLAYBACK_QUEUE_MS,
    private val hardLimitMs: Int = GeminiLiveTuning.HARD_PLAYBACK_QUEUE_MS,
) {
    private val warningBytes = bytesFor(maximumMs)
    private val hardLimitBytes = bytesFor(hardLimitMs)

    init {
        require(sampleRate > 0) { "sampleRate must be positive" }
        require(maximumMs > 0) { "maximumMs must be positive" }
        require(hardLimitMs > maximumMs) { "hardLimitMs must exceed maximumMs" }
    }

    fun durationMs(bytes: Int): Int =
        ((bytes.coerceAtLeast(0).toLong() * 1_000L) /
            (sampleRate.toLong() * AudioConfig.BYTES_PER_SAMPLE)).toInt()

    fun action(currentBytes: Int, incomingBytes: Int): PlaybackQueueAction {
        val totalBytes = currentBytes.coerceAtLeast(0).toLong() + incomingBytes.coerceAtLeast(0).toLong()
        return when {
            totalBytes > hardLimitBytes -> PlaybackQueueAction.RESYNC
            totalBytes > warningBytes -> PlaybackQueueAction.WARN
            else -> PlaybackQueueAction.ACCEPT
        }
    }

    fun accepts(currentBytes: Int, incomingBytes: Int): Boolean =
        action(currentBytes, incomingBytes) == PlaybackQueueAction.ACCEPT

    fun exceedsHardLimit(currentBytes: Int, incomingBytes: Int): Boolean =
        action(currentBytes, incomingBytes) == PlaybackQueueAction.RESYNC

    private fun bytesFor(durationMs: Int): Long =
        sampleRate.toLong() * AudioConfig.BYTES_PER_SAMPLE * durationMs / 1_000L
}
