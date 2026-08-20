package com.kenza.callsim.script

import org.junit.Assert.assertEquals
import org.junit.Test

class ScriptDurationEstimatorTest {

    @Test
    fun `combines conversational speech time with quantified and descriptive pauses`() {
        val text = """
            ${List(150) { "word" }.joinToString(" ")}
            [pause 2.5 seconds]
            [listening pause 6 seconds]
            [brief silence]
            [longer listening pause]
        """.trimIndent()

        val estimate = ScriptDurationEstimator.estimate(text)

        assertEquals(60, estimate.speechSeconds)
        assertEquals(18, estimate.listenerPauseSeconds)
        assertEquals(78, estimate.totalSeconds)
    }

    @Test
    fun `does not count pause directions as spoken words`() {
        val estimate = ScriptDurationEstimator.estimate(
            "Okay, I hear you. [pause 4 seconds] Tell me what happened.",
        )

        assertEquals(3, estimate.speechSeconds)
        assertEquals(4, estimate.listenerPauseSeconds)
        assertEquals(7, estimate.totalSeconds)
    }
}
