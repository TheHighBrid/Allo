package com.kenza.callsim.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class ReactiveListeningTest {
    private val pcm = ByteArray(4800).also { for (i in it.indices step 2) it[i + 1] = 40 }

    @Test fun onlyCompleteNeutralSameVoiceClipsAreCached() {
        val bank = ReactiveListening(24000) { 22_000L }
        bank.agentAudio(pcm)
        bank.agentText("I really need to tell you something")
        bank.complete(false)
        bank.speechStarted(1000)
        bank.userText("I went to the store today")
        assertNull(bank.reaction(8000, true, true))
        bank.agentAudio(pcm)
        bank.agentText("mm-hm")
        bank.complete(true)
        assertNull(bank.reaction(8000, true, true))
        bank.agentAudio(pcm)
        bank.agentText("mm-hm")
        bank.complete(false)
        assertNull(bank.reaction(6000, true, true))
        assertNull(bank.reaction(8000, false, true))
        assertNull(bank.reaction(8000, true, false))
        val clip = bank.reaction(8000, true, true)
        assertNotNull(clip)
        assertEquals(4.toByte(), clip!![1]) // -20 dB amplitude
        bank.played(8000)
        assertNull(bank.reaction(14000, true, true))
        bank.speechStarted(15000)
        bank.userText("I went to the store today")
        assertNull(bank.reaction(22000, true, true))
        assertNotNull(bank.reaction(31000, true, true))
    }
}
