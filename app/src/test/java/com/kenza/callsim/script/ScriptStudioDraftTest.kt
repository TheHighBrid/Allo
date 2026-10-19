package com.kenza.callsim.script

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Base64

class ScriptStudioDraftTest {

    @Test
    fun `round trips every editable draft field without provider configuration`() {
        val draft = ScriptStudioDraft(
            requestedMinutes = 20,
            mode = ScriptMode.CUSTOM,
            language = "French",
            callReason = "A warm weekend catch-up",
            mood = "playful but tired",
            topicsText = "Melato launch\nweekend plans",
            selectedMemoryIds = listOf("memory-1", "memory-2"),
            affection = IntensityLevel.HIGH,
            humor = IntensityLevel.MODERATE,
            flirtation = IntensityLevel.LOW,
            boundariesText = "Do not invent plans\nKeep teasing gentle",
            endingStyle = "soft goodnight",
            scriptText = "[soft laugh] I missed that story. [listening pause 4 seconds] Keep going.",
            generatedTitle = "Demo: Custom call",
            memoryIdsUsed = listOf("memory-2"),
        )

        val encoded = ScriptStudioDraftCodec.encode(draft)
        val restored = ScriptStudioDraftCodec.decode(encoded)

        assertEquals(draft, restored)
    }

    @Test
    fun `migrates a v1 draft without losing existing user work`() {
        fun field(value: String): String = Base64.getEncoder()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
        val legacy = listOf(
            "v1",
            "15",
            ScriptMode.PLAYFUL.name,
            field("English"),
            field("Catch up after work"),
            field("Hey baby. [listening pause 3 seconds]"),
            field("Playful call"),
        ).joinToString(".")

        val restored = ScriptStudioDraftCodec.decode(legacy)!!

        assertEquals(15, restored.requestedMinutes)
        assertEquals(ScriptMode.PLAYFUL, restored.mode)
        assertEquals("Catch up after work", restored.callReason)
        assertEquals("Hey baby. [listening pause 3 seconds]", restored.scriptText)
        assertEquals(IntensityLevel.MODERATE, restored.affection)
        assertEquals(emptyList<String>(), restored.selectedMemoryIds)
    }

    @Test
    fun `returns no draft when persisted payload is invalid`() {
        assertNull(ScriptStudioDraftCodec.decode("not-a-valid-draft"))
    }
}
