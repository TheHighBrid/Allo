package com.kenza.callsim.script

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OneSidedDialogueValidatorTest {

    @Test
    fun `flags listener labels and quoted listener replies`() {
        val warnings = OneSidedDialogueValidator.validate(
            """
            Kenza: I was thinking about you today.
            Listener: I missed you too.
            "You should come over," Mohamed said.
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                OneSidedDialogueWarningCode.SPEAKER_LABEL,
                OneSidedDialogueWarningCode.SPEAKER_LABEL,
                OneSidedDialogueWarningCode.QUOTED_LISTENER_REPLY,
            ),
            warnings.map { it.code },
        )
    }

    @Test
    fun `allows performance and listener pause directions without warnings`() {
        val warnings = OneSidedDialogueValidator.validate(
            "[soft laugh] You make it sound so easy. [listening pause 4 seconds] I know, I know.",
        )

        assertTrue(warnings.isEmpty())
    }
}
