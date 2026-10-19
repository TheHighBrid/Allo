package com.kenza.callsim.script

import org.junit.Assert.assertEquals
import org.junit.Test

class ScriptTextExportTest {

    @Test
    fun `removes bracketed performance and pause directions while preserving readable paragraphs`() {
        val source = """
            [soft laugh] I was hoping I would catch you.
            [listening pause 4 seconds]

            [more serious] Tell me what happened.
        """.trimIndent()

        assertEquals(
            "I was hoping I would catch you.\n\nTell me what happened.",
            ScriptTextExport.clean(source),
        )
    }

    @Test
    fun `trims empty text to a share-safe blank value`() {
        assertEquals("", ScriptTextExport.clean(" [brief silence] \n\n "))
    }
}
