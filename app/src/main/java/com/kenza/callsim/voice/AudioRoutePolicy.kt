package com.kenza.callsim.voice

import android.media.AudioDeviceInfo
import android.os.Build

/** One auditable switch for echo-safe barge-in behavior. */
object AudioRoutePolicy {
    /**
     * Disabled by default: several phones feed earpiece audio back into Gemini
     * despite platform AEC, causing false interruptions and chopped speech.
     * Headsets remain full duplex; diagnostics builds may opt in per device.
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

/** Pure queue arithmetic shared by the Android player and JVM tests. */
class PlaybackQueuePolicy(
    private val sampleRate: Int,
    private val maximumMs: Int = GeminiLiveTuning.MAX_PLAYBACK_QUEUE_MS,
) {
    fun durationMs(bytes: Int): Int =
        ((bytes.toLong() * 1_000L) / (sampleRate * AudioConfig.BYTES_PER_SAMPLE)).toInt()

    fun accepts(currentBytes: Int, incomingBytes: Int): Boolean =
        durationMs(currentBytes + incomingBytes) <= maximumMs

    fun exceedsHardLimit(currentBytes: Int, incomingBytes: Int): Boolean =
        durationMs(currentBytes + incomingBytes) > GeminiLiveTuning.HARD_PLAYBACK_QUEUE_MS
}
