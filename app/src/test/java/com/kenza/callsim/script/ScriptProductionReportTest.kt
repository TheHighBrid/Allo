package com.kenza.callsim.script

import com.kenza.callsim.memory.KenzaContext
import com.kenza.callsim.memory.MemoryItem
import com.kenza.callsim.memory.MemoryKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptProductionReportTest {

    @Test
    fun `report uses explicit request facts and keeps memory candidates review only`() {
        val request = ScriptRequest(
            requestedMinutes = 20,
            mainTopics = listOf("Melato launch", "Montréal weekend"),
            recentEvents = listOf("The pop-up moved to Friday"),
            currentProblems = listOf("Still choosing a hotel"),
            futurePlans = listOf("Book dinner in Montréal"),
            relationshipMood = "playful",
            endingStyle = "soft goodnight",
        )
        val context = KenzaContext(
            retrievedMemories = listOf(
                MemoryItem(
                    id = "memory-1",
                    kind = MemoryKind.FACT,
                    text = "Reviewed fact",
                    createdAt = 1L,
                )
            )
        )

        val report = ScriptProductionReportBuilder.build(request, context)

        assertTrue(report.continuityNotes.any { it.contains("Melato launch") })
        assertTrue(report.continuityNotes.any { it.contains("1 reviewed durable memory") })
        assertEquals(listOf("Still choosing a hotel", "Book dinner in Montréal"), report.unresolvedTopics)
        assertEquals(2, report.candidateMemories.size)
        assertTrue(report.candidateMemories.all { !it.shouldStore })
        assertTrue(report.candidateMemories.all { it.reason.contains("review") })
        assertTrue(report.pronunciationNotes.any { it.contains("Montréal") })
    }

    @Test
    fun `generated prose cannot enter production report because builder never accepts it`() {
        val request = ScriptRequest(requestedMinutes = 10, mainTopics = listOf("Work"))
        val report = ScriptProductionReportBuilder.build(request, KenzaContext())

        assertFalse(report.candidateMemories.any { it.text.contains("fiction", ignoreCase = true) })
        assertTrue(report.candidateMemories.isEmpty())
    }
}
