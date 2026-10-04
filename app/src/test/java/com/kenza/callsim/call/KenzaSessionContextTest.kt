package com.kenza.callsim.call

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KenzaSessionContextTest {

    @Test
    fun mohamedCallingGetsAPickupLine() {
        repeat(20) { seed ->
            val line = KenzaCallOpening.openingLine(CallDirection.MOHAMED_CALLED, false, Random(seed))
            assertTrue(line in KenzaCallOpening.PICKUP_LINES)
            assertTrue(line.startsWith("Allô"))
        }
    }

    @Test
    fun kenzaCallingGetsACallerLine() {
        val line = KenzaCallOpening.openingLine(CallDirection.KENZA_CALLED, false, Random(1))
        assertTrue(line in KenzaCallOpening.CALLER_LINES)
    }

    @Test
    fun reconnectAfterAnythingWasHeardHasNoOpening() {
        assertEquals("", KenzaCallOpening.openingLine(CallDirection.MOHAMED_CALLED, true, Random(1)))
        assertEquals("", KenzaCallOpening.openingLine(CallDirection.KENZA_CALLED, true, Random(1)))
    }

    @Test
    fun dynamicVariablesCoverEveryNameAndTrimContext() {
        val inputs = KenzaCallInputs(
            persona = "  persona  ",
            continuityBriefing = "\n\nbriefing\n",
            callReason = "",
            direction = CallDirection.MOHAMED_CALLED,
            openingLine = "Allô?",
        )
        val vars = KenzaDynamicVariables.build(
            inputs,
            now = ZonedDateTime.of(2026, 10, 4, 21, 5, 0, 0, ZoneId.of("America/Toronto")),
        )
        assertEquals(KenzaDynamicVariables.ALL.toSet(), vars.keys)
        assertEquals("persona", vars[KenzaDynamicVariables.PERSONA])
        assertEquals("briefing", vars[KenzaDynamicVariables.CONTINUITY_BRIEFING])
        assertEquals(KenzaDynamicVariables.NONE, vars[KenzaDynamicVariables.CALL_REASON])
        assertEquals("Allô?", vars[KenzaDynamicVariables.OPENING_LINE])
        assertEquals("Sunday, October 4, 9:05 PM (America/Toronto)", vars[KenzaDynamicVariables.LOCAL_TIME])
        assertTrue(vars.getValue(KenzaDynamicVariables.CALL_DIRECTION).contains("picked up"))
    }

    @Test
    fun callerDirectionNeverThanksForCalling() {
        val text = KenzaDynamicVariables.describe(CallDirection.KENZA_CALLED)
        assertTrue(text.contains("You called Mohamed"))
        assertFalse(text.contains("picked up"))
    }

    @Test
    fun variableNamesAvoidReservedPrefixes() {
        KenzaDynamicVariables.ALL.forEach { name ->
            assertFalse(name.startsWith("system__"))
            assertFalse(name.startsWith("secret__"))
        }
    }
}
