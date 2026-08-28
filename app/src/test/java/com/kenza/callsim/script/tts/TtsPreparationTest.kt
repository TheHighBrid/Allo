package com.kenza.callsim.script.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsPreparationTest {

    @Test
    fun `pause and performance directions never remain in spoken text`() {
        val blocks = TtsTextTransformer.transform(
            """
            [soft laugh]
            You really did that?
            [listening pause 2.5 seconds]
            [more serious]
            Okay, tell me what happened next.
            """.trimIndent(),
        )

        assertEquals(2, blocks.size)
        assertEquals(listOf("soft laugh"), blocks[0].performanceDirections)
        assertEquals(0L, blocks[0].pauseBeforeMs)
        assertEquals(2_500L, blocks[1].pauseBeforeMs)
        assertEquals(listOf("more serious"), blocks[1].performanceDirections)
        assertFalse(blocks.joinToString { it.spokenText }.contains('['))
    }

    @Test
    fun `documented unquantified pauses use conservative defaults`() {
        assertEquals(1_000L, TtsTextTransformer.pauseMs("brief silence"))
        assertEquals(8_000L, TtsTextTransformer.pauseMs("longer listening pause"))
    }

    @Test
    fun `segmentation preserves internal pauses and carries non spoken continuity context`() {
        val first = "A".repeat(140)
        val second = "B".repeat(140)
        val third = "C".repeat(140)
        val script = "$first\n\n[listening pause 4 seconds]\n\n$second\n\n$third"

        val segments = TtsSegmenter.segment(
            scriptText = script,
            maxSpokenChars = 300,
            continuityChars = 40,
        )

        assertEquals(2, segments.size)
        assertEquals(2, segments[0].blocks.size)
        assertEquals(4_000L, segments[0].blocks[1].pauseBeforeMs)
        assertTrue(segments[1].continuityContext.isNotBlank())
        assertFalse(segments[1].spokenText.contains(segments[1].continuityContext))
        assertEquals(listOf(0, 1), segments.map { it.order })
    }
}
