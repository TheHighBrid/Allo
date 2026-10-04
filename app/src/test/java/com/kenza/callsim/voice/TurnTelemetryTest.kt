package com.kenza.callsim.voice

import org.junit.Assert.assertFalse
import org.junit.Test

class TurnTelemetryTest {
    @Test fun serializedMetricHasNoContentFields() {
        val text = LiveTurnJson.encode(LiveTurnMetrics(1)).toString()
        assertFalse(text.contains("transcript", ignoreCase = true))
        assertFalse(text.contains("audio", ignoreCase = true))
    }
}
