# Kenza realism phase 2

The existing provider already sets public v1beta contextWindowCompression.slidingWindow and sessionResumption. This change hardens the lifecycle rather than reimplementing it. The public protocol reference and Google Live session-management guide support these fields; no transparent or contextUpdate fields are sent.

- GoAway waits for a current resumable checkpoint, no caller speech and no model generation; deadline may force rotation with a prior checkpoint. Unknown/no checkpoint falls back to bounded fresh-session recovery. No speculative replay. Retry delays 250/500/1000/2000 ms. User sees Reconnecting audio; timer and microphone settings survive. Recent completed text is supplied to fresh Gemini sessions as context, not replayed audio.
- Reconnect attempts/success/failure, dropped speech chunks, at-risk turns, readiness and first subsequent audio are measured, without logging transcripts or resumption handles.
- AudioTrack cutoff is estimated in milliseconds from playback head frames; hardware flush after turnComplete is attributed to the completed turn. Generated-but-unheard Gemini output is conservatively excluded from memory rather than guessed from transcript length. Existing ElevenLabs correction behavior is retained.
- Optional same-voice listening reactions cache only completed short neutral acknowledgments from the current call. No extra model request or persistent clips. Require a headset with AEC, active neutral user monologue of six seconds or more, available interim transcription, idle provider playback, and randomized cooldown 22–45 seconds. Replay at -20 dB relative amplitude; cancel on user pause, mute, route change or provider speech. Disabled by default until tested on hardware. No synthetic room-tone bed and no mic threshold manipulation.
- Director tracks approximate speaking balance and call age; updates prompt on future fresh sessions, not via unsupported mid-stream control frames. Native spoken rules handle repair directly without asking the model to generate duplicate replies.
- Time-bound follow-up metadata is optional, compatible with older summaries, and suppresses matched initiative hooks until due. Existing quiet hours and cooldowns remain unchanged.

## Acceptance on a physical Android device

1. Keep a Gemini call active for 30 and 60 minutes, force a network reset between turns and during a monologue. Verify timer continuity, no repeated greeting, graceful recovery; record attempts, loss and readiness. Model-specific hard limits and token brokers still require real credentials/validation.
2. Interrupt ten times on a headset, record interruption-to-silence and heard-duration estimates; verify no stale buffered audio or false memories.
3. Enable same-voice reactions on an AEC-equipped headset, run a 10-minute conversation with long monologues. Confirm no extra provider turn, no overlap with Kenza speech, and no feedback. A compatible short neutral clip and interim text must exist; zero reactions is valid.
4. Compare normal/serious/patient pauses and phone coloration against the baseline, with no artificial noise or over-filtering.
5. Confirm a dated follow-up cannot trigger the matched initiative cue prematurely.

Do not interpret synthetic tests as proof of subjective voice realism or of unlimited call duration.
- Broker-authenticated Gemini reconnects now obtain a fresh ephemeral token per new WebSocket while retaining the latest resumable session handle; broker failure falls back through the existing bounded app recovery instead of reusing an expired token. This adds one broker request per rotation and requires broker availability.
- Fresh-session recovery retries now count consecutive failures, not total failures across the lifetime of a call. A confirmed ready event resets the four-attempt budget; tests cover exhaustion and reset.

## Additional validation

6. With a token broker, keep a call beyond 30 minutes and confirm that multiple GoAway rotations use refreshed tokens without an unsolicited new greeting. Broker refresh failure must surface recoverably, not spin or silently reuse a stale token.
7. Recover successfully from multiple spaced-out transport losses, then trigger another failure. Each successful reconnection must restore the four-attempt budget; four consecutive failed recoveries must still stop safely.
