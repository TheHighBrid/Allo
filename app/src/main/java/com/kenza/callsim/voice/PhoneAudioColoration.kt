package com.kenza.callsim.voice

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt

/** Optional streaming, wideband-phone coloration. No noise, extra latency or network calls. */
internal class PhoneAudioColoration(sampleRate: Int) {
    private val hp = exp(-2.0 * PI * 120 / sampleRate)
    private val lp = 1.0 - exp(-2.0 * PI * minOf(4500.0, sampleRate * 0.40) / sampleRate)
    private var previousInput = 0.0
    private var high = 0.0
    private var low = 0.0

    @Synchronized fun process(pcm: ByteArray): ByteArray {
        val result = pcm.copyOf()
        var i = 0
        while (i + 1 < pcm.size) {
            val sample = ((pcm[i].toInt() and 255) or (pcm[i + 1].toInt() shl 8)).toShort().toDouble()
            high = hp * (high + sample - previousInput)
            previousInput = sample
            low += lp * (high - low)
            val mixed = 0.35 * sample + 0.65 * low
            val output = (mixed / (1.0 + kotlin.math.abs(mixed) / 130_000.0))
                .roundToInt().coerceIn(-32768, 32767)
            result[i] = output.toByte()
            result[i + 1] = (output shr 8).toByte()
            i += 2
        }
        return result
    }

    @Synchronized fun reset() {
        previousInput = 0.0
        high = 0.0
        low = 0.0
    }
}
