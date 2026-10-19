package com.kenza.callsim.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackTurnTrackerTest {

    private class Recorder : PlaybackMetrics {
        var headAdvances = 0
        val underruns = mutableListOf<Int>()
        override fun onPlaybackHeadAdvanced() { headAdvances++ }
        override fun onUnderruns(delta: Int) { underruns += delta }
    }

    @Test
    fun reportsHeadAdvanceOncePerTurnAfterItsFirstSample() {
        val recorder = Recorder()
        val tracker = PlaybackTurnTracker(recorder)
        tracker.reset(headFrames = 0, underruns = 0)

        tracker.beforeWrite(turnStart = true, underruns = 0)
        assertTrue(tracker.awaitingHeadAdvance)
        tracker.afterWrite(framesWritten = 480, headFrames = 0)
        assertEquals(0, recorder.headAdvances)
        tracker.onHeadPosition(120)
        assertEquals(1, recorder.headAdvances)
        assertFalse(tracker.awaitingHeadAdvance)

        tracker.beforeWrite(turnStart = false, underruns = 0)
        tracker.afterWrite(framesWritten = 480, headFrames = 600)
        assertEquals(1, recorder.headAdvances)
    }

    @Test
    fun secondTurnWaitsUntilHeadPassesItsStartFrame() {
        val recorder = Recorder()
        val tracker = PlaybackTurnTracker(recorder)
        tracker.reset(headFrames = 0, underruns = 0)
        tracker.beforeWrite(turnStart = true, underruns = 0)
        tracker.afterWrite(framesWritten = 1_000, headFrames = 10)
        assertEquals(1, recorder.headAdvances)

        // Turn 2 starts at frame 1000 while the head is still playing turn 1.
        tracker.beforeWrite(turnStart = true, underruns = 0)
        tracker.afterWrite(framesWritten = 500, headFrames = 900)
        assertEquals(1, recorder.headAdvances)
        tracker.onHeadPosition(1_001)
        assertEquals(2, recorder.headAdvances)
    }

    @Test
    fun countsUnderrunsWithinTurnButNotTheDrainBetweenTurns() {
        val recorder = Recorder()
        val tracker = PlaybackTurnTracker(recorder)
        tracker.reset(headFrames = 0, underruns = 2)
        tracker.beforeWrite(turnStart = true, underruns = 3) // drain before this turn: ignored
        tracker.beforeWrite(turnStart = false, underruns = 5) // mid-turn stutter: reported
        tracker.beforeWrite(turnStart = false, underruns = 5)
        tracker.beforeWrite(turnStart = true, underruns = 9) // next turn: re-baselined
        assertEquals(listOf(2), recorder.underruns)
    }

    @Test
    fun resetAfterFlushCancelsPendingTurn() {
        val recorder = Recorder()
        val tracker = PlaybackTurnTracker(recorder)
        tracker.reset(headFrames = 0, underruns = 0)
        tracker.beforeWrite(turnStart = true, underruns = 0)
        tracker.afterWrite(framesWritten = 480, headFrames = 0)
        tracker.reset(headFrames = 0, underruns = 0)
        assertFalse(tracker.awaitingHeadAdvance)
        tracker.onHeadPosition(480)
        assertEquals(0, recorder.headAdvances)
    }
}
