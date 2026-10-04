package com.kenza.callsim.voice

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ElevenLabsProtocolTest {

    private fun parse(raw: String) = ElevenLabsServerEvent.parse(JSONObject(raw))

    @Test
    fun parsesMetadataFormats() {
        val event = parse(
            """{"type":"conversation_initiation_metadata","conversation_initiation_metadata_event":
               {"conversation_id":"c","agent_output_audio_format":"pcm_24000",
                "user_input_audio_format":"pcm_16000"}}""",
        )
        assertEquals(ElevenLabsServerEvent.Metadata(outputFormat = "pcm_24000", inputFormat = PCM_16K), event)
    }

    @Test
    fun parsesAudioWithEventIdAndFinalFlag() {
        val event = parse(
            """{"type":"audio","audio_event":{"audio_base_64":"AAA=","event_id":7,"is_final":true}}""",
        )
        assertEquals(ElevenLabsServerEvent.Audio(base64 = "AAA=", eventId = 7, isFinal = true), event)
    }

    @Test
    fun audioWithoutEventIdHasNullId() {
        val event = parse("""{"type":"audio","audio_event":{"audio_base_64":"AAA="}}""")
        assertEquals(ElevenLabsServerEvent.Audio(base64 = "AAA=", eventId = null, isFinal = false), event)
    }

    @Test
    fun parsesCorrectionCompleteInterruptionAndPing() {
        assertEquals(
            ElevenLabsServerEvent.AgentResponseCorrection(original = "Hello there friend", corrected = "Hello there"),
            parse(
                """{"type":"agent_response_correction","agent_response_correction_event":
                   {"original_agent_response":"Hello there friend","corrected_agent_response":"Hello there",
                    "event_id":3}}""",
            ),
        )
        assertEquals(
            ElevenLabsServerEvent.AgentResponseComplete(eventId = 4),
            parse("""{"type":"agent_response_complete","agent_response_complete_event":{"event_id":4}}"""),
        )
        assertEquals(
            ElevenLabsServerEvent.Interruption(eventId = 5),
            parse("""{"type":"interruption","interruption_event":{"event_id":5}}"""),
        )
        assertEquals(ElevenLabsServerEvent.Ping(eventId = 9), parse("""{"type":"ping","ping_event":{"event_id":9}}"""))
    }

    @Test
    fun parsesTranscriptsAndUnknownTypes() {
        assertEquals(
            ElevenLabsServerEvent.UserTranscript("salut"),
            parse("""{"type":"user_transcript","user_transcription_event":{"user_transcript":"salut"}}"""),
        )
        assertEquals(
            ElevenLabsServerEvent.AgentResponse("allô?"),
            parse("""{"type":"agent_response","agent_response_event":{"agent_response":"allô?"}}"""),
        )
        assertEquals(ElevenLabsServerEvent.Other("vad_score"), parse("""{"type":"vad_score"}"""))
        assertEquals(ElevenLabsServerEvent.Other("ping"), parse("""{"type":"ping"}"""))
    }

    @Test
    fun filterDropsOnlyAudioOlderThanLatestInterruption() {
        val filter = InterruptedAudioFilter()
        assertTrue(filter.shouldPlay(1))
        filter.onInterruption(5)
        assertFalse(filter.shouldPlay(4))
        // Same id as the interruption plays, matching the official client (lastInterrupt <= event_id).
        assertTrue(filter.shouldPlay(5))
        assertTrue(filter.shouldPlay(6))
        assertTrue(filter.shouldPlay(null))
    }

    @Test
    fun filterKeepsHighestInterruptionAndResets() {
        val filter = InterruptedAudioFilter()
        filter.onInterruption(8)
        filter.onInterruption(3)
        filter.onInterruption(null)
        assertFalse(filter.shouldPlay(7))
        filter.reset()
        assertTrue(filter.shouldPlay(1))
    }

    private companion object {
        const val PCM_16K = "pcm_16000"
    }
}
