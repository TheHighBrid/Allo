package com.kenza.callsim.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LocalSpeechEnergyTrackerTest {

    @Test
    fun quietPcmDoesNotStartSpeech() {
        val tracker = LocalSpeechEnergyTracker(speechRmsThreshold = 900, endHangoverChunks = 3)
        assertNull(tracker.onPcm(silence(640)))
        assertNull(tracker.onPcm(silence(640)))
    }

    @Test
    fun loudThenQuietEmitsStartAndEnd() {
        val tracker = LocalSpeechEnergyTracker(speechRmsThreshold = 900, endHangoverChunks = 2)
        assertEquals(LocalSpeechEnergyTracker.Event.START, tracker.onPcm(tone(640, amplitude = 8_000)))
        assertNull(tracker.onPcm(tone(640, amplitude = 8_000)))
        assertNull(tracker.onPcm(silence(640)))
        assertEquals(LocalSpeechEnergyTracker.Event.END, tracker.onPcm(silence(640)))
    }

    @Test
    fun forceEndClosesOpenSpeech() {
        val tracker = LocalSpeechEnergyTracker()
        assertEquals(LocalSpeechEnergyTracker.Event.START, tracker.onPcm(tone(640, amplitude = 10_000)))
        assertEquals(LocalSpeechEnergyTracker.Event.END, tracker.forceEnd())
        assertNull(tracker.forceEnd())
    }

    @Test
    fun lastVoicedMarksLastLoudChunkNotHangoverEnd() {
        var now = 1_000L
        val tracker = LocalSpeechEnergyTracker(speechRmsThreshold = 900, endHangoverChunks = 2, clockMs = { now })
        tracker.onPcm(tone(640, amplitude = 8_000))
        now = 1_040L
        tracker.onPcm(tone(640, amplitude = 8_000))
        now = 1_080L
        tracker.onPcm(silence(640))
        now = 1_120L
        assertEquals(LocalSpeechEnergyTracker.Event.END, tracker.onPcm(silence(640)))
        assertEquals(1_040L, tracker.lastVoicedMs())
        assertEquals(1_120L, tracker.lastEndMs())
        tracker.reset()
        assertNull(tracker.lastVoicedMs())
    }

    @Test
    fun rmsOfSilenceIsNearZero() {
        assertEquals(0, LocalSpeechEnergyTracker.pcm16Rms(silence(320)))
        assertTrue(LocalSpeechEnergyTracker.pcm16Rms(tone(320, amplitude = 5_000)) > 1_000)
    }

    private fun silence(bytes: Int): ByteArray = ByteArray(bytes)

    private fun tone(bytes: Int, amplitude: Int): ByteArray {
        val buffer = ByteBuffer.allocate(bytes).order(ByteOrder.LITTLE_ENDIAN)
        repeat(bytes / 2) { buffer.putShort(amplitude.toShort()) }
        return buffer.array()
    }
}
