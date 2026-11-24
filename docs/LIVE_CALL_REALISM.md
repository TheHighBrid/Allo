# Live call realism: phase one

Gemini remains the default daily provider. ElevenLabs is unchanged as the optional alternate engine.

## Available changes

- Spoken delivery, concise reactions, interruption repair, implicit memory and flexible call arc.
- Per-session director tracks mood, patience, energy and a loose beat; initial and reconnect prompts use its snapshot. No extra text turn is injected during live replies. The model follows the arc instructions during the call.
- Settings > Call realism > Adaptive turn-taking (experimental): manual Gemini activity events and local PCM energy, 2 voiced frames to start, 3-frame pre-roll, 320/560/1200/2000/4000 ms quiet windows. Semantic hints use available input transcription; absent interim text, the normal or previous serious context applies. Bounded hold prevents a stuck call. The switch is off by default; the standard server-VAD path is the comparison baseline.
- On interruption, flush queued PCM before transcript work; ignore old audio bundled in the interruption message. Keep headset full-duplex behavior and the built-in echo gate.
- Settings > Call realism > Subtle phone audio: optional streaming filter, no added hiss or packet loss, no new service.
- Fix per turn gap/speech timing and log provider teardown/session summary when diagnostics are enabled.
- Call transcript fragments are committed and passed to the existing summary extractor at hangup. Concurrent summary completion targets the corresponding call start record. Candidate memories still need explicit review; raw transcripts are not persisted.

## Physical-device acceptance

Compare the adaptive switch off/on for each supported model and route:
1. 20 quick turns: count first-audible delay (median/p95), target improvement without clipping.
2. 10 thinking pauses and five explicit holds: count premature replies and false starts, confirm speech resumes.
3. Ten interruptions on a headset: measure interruption-to-silence and check for repeated answers.
4. Five serious turns and a 10-minute call: score turn length, question spam, initiative, callbacks and topic drift.
5. Replay the same conversational material with phone audio disabled/enabled. Prefer the version that sounds natural without degraded intelligibility.
6. Verify reconnects, mute, call end, and consecutive calls; confirm summaries attach to the right calls and no private transcript is written to logcat.

Reactive overlap / standalone backchannels are deliberately not enabled by this phase. Playing arbitrary TTS clips while caller speech is open risks mic echo and mismatched voice. A future gated PCM clip bank requires approved, voice-consistent clips and device validation; prompt-based reactions are present now. Do not synthesize extra provider turns during ongoing speech. Also, native-audio prosody is model-generated: no pitch shifting or fake dropouts by default.
