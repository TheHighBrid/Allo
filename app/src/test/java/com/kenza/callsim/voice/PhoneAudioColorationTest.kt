package com.kenza.callsim.voice

import org.junit.Assert.*
import org.junit.Test

class PhoneAudioColorationTest {
    @Test fun filterKeepsPcmLengthAndInputAndSilence() {
        val filter = PhoneAudioColoration(24000)
        val silence = ByteArray(1280)
        assertArrayEquals(silence, filter.process(silence))
        val samples = ByteArray(1280) { if (it % 2 == 0) 0 else 32 }
        val copy = samples.copyOf()
        val out = filter.process(samples)
        assertEquals(samples.size, out.size)
        assertArrayEquals(copy, samples)
        assertFalse(out.contentEquals(samples))
        filter.reset()
        assertArrayEquals(silence, filter.process(silence))
    }

    @Test fun packetBoundariesDoNotChangeFilterOutput() {
        val pcm = ByteArray(1600) { (it % 127).toByte() }
        val whole = PhoneAudioColoration(24000).process(pcm)
        val split = PhoneAudioColoration(24000)
        assertArrayEquals(whole, split.process(pcm.copyOfRange(0, 800)) +
            split.process(pcm.copyOfRange(800, 1600)))
    }
}
