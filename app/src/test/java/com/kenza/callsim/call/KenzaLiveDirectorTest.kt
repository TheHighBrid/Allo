package com.kenza.callsim.call

import org.junit.Assert.*
import org.junit.Test

class KenzaLiveDirectorTest {
    @Test fun developsArcAndRespondsToSeriousInput() {
        val director = KenzaLiveDirector(2)
        assertEquals(KenzaLiveDirector.Mood.TIRED, director.snapshot().mood)
        director.onTurnComplete(1000)
        assertEquals(KenzaLiveDirector.Beat.LIGHT_EXCHANGE, director.snapshot().beat)
        director.onUserText("I feel sad")
        director.onTurnComplete(3000)
        assertEquals(KenzaLiveDirector.Mood.CONCERNED, director.snapshot().mood)
        assertEquals(KenzaLiveDirector.Beat.DEEPEN, director.snapshot().beat)
        director.onInterrupted()
        assertEquals(1, director.snapshot().interruptions)
        director.onUserText("okay")
        repeat(3) { director.onTurnComplete(5000) }
        assertEquals(KenzaLiveDirector.Beat.SHARE_OR_EXPLORE, director.snapshot().beat)
        director.onTurnComplete(26 * 60_000L)
        assertEquals(KenzaLiveDirector.Beat.WIND_DOWN, director.snapshot().beat)
        assertTrue(director.prompt().contains("not a script"))
    }
}
