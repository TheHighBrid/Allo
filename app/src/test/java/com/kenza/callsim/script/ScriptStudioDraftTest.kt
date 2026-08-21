package com.kenza.callsim.script

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScriptStudioDraftTest {

    @Test
    fun `round trips every editable draft field without provider configuration`() {
        val draft = ScriptStudioDraft(
            requestedMinutes = 20,
            mode = ScriptMode.CUSTOM,
            language = "French",
            callReason = "A warm weekend catch-up",
            scriptText = "[soft laugh] I missed that story. [listening pause 4 seconds] Keep going.",
            generatedTitle = "Demo: Custom call",
        )

        val encoded = ScriptStudioDraftCodec.encode(draft)
        val restored = ScriptStudioDraftCodec.decode(encoded)

        assertEquals(draft, restored)
    }

    @Test
    fun `returns no draft when persisted payload is invalid`() {
        assertNull(ScriptStudioDraftCodec.decode("not-a-valid-draft"))
    }
}
