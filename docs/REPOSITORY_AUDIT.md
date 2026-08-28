# Repository audit and architecture map

Last reviewed: 2026-08-27

This document records the current Android architecture as verified from `main`. It distinguishes shipped behavior from risks and product decisions so future work does not treat guesses as defects.

## Validation baseline

The repository pull-request workflow runs unit tests, Android lint, debug APK compilation, canonical signed-update APK generation, package/version/signature verification, and artifact upload. Dependency-review also runs on applicable pull requests.

Current changes must continue to preserve the permanent Allo signing identity so an update APK can install over the existing app.

## Persona and prompt assembly paths

### Shared identity and user-editable persona

- `config/ConfigRepository.kt`
  - Owns provider selection, runtime configuration, contact name, consent, diagnostics preference, and the editable persona prompt.
  - `defaultPersona(...)` is the bundled baseline persona when no current-version user override exists.

### Shared continuity context

- `memory/KenzaContext.kt`
  - `KenzaContext` is the provider-neutral persona, user-profile, relationship, recent-call, retrieved-memory, and open-thread model.
  - `KenzaContextAssembler` selects bounded memory context. Script requests use topic/query relevance plus explicit memory selections; live calls use bounded scored memories.
- `memory/MemoryContext.kt`
  - Compatibility facade used by the existing live-call path. It delegates to `KenzaContextAssembler` rather than maintaining a second memory policy.

### Live Call Mode prompt path

- `call/CallViewModel.kt`
  - Reads the current persona from `ConfigRepository`.
  - Builds the shared continuity briefing through `MemoryContext`.
  - Adds call-direction/private director context and Gemini-specific delivery/agency guidance where applicable.
  - Constructs the selected `VoiceProvider` without changing the provider-neutral memory model.
- `voice/GeminiLiveProvider.kt`
  - Transports the live Gemini session configuration and audio/text events.
- `voice/ElevenLabsProvider.kt`
  - Transports the ElevenLabs conversational session. Optional memory/persona prompt override remains controlled by the ElevenLabs setting and provider capability.

### Script Studio prompt path

- `script/ScriptRequest.kt`
  - Provider-neutral structured request model.
- `script/DemoScriptGenerator.kt`
  - Provider-neutral `ScriptGenerator` boundary plus deterministic offline/demo implementation.
- `script/GeminiScriptGenerator.kt`
  - `ScriptStudioPrompt` combines the same `KenzaContext` continuity briefing with the current structured Script Studio request.
  - Gemini REST transport remains separate from Gemini Live audio transport.

## Runtime architecture map

### Navigation and screens

- `ui/AppNavigation.kt`: top-level routes and product-mode navigation.
- `ui/screens/HomeScreen.kt`: entry points for Live Call Mode and Script Studio.
- `ui/screens/ScriptStudioScreen.kt`: structured request, generation, editing, duration/warning feedback, draft persistence, clean text copy/export.
- `ui/screens/MemoryScreen.kt`: profiles, manual memories, recent call summaries, reviewable extracted-memory candidates, durable-memory correction/pinning/deletion.
- Settings, consent, schedule, incoming-call, and active-call screens remain under `ui/screens`.

### Call state and lifecycle

- `call/CallState.kt`: UI-facing call phases/state.
- `call/CallViewModel.kt`: current coordinator for dialing, provider lifecycle, microphone/playback, routing, transcripts, timers, memory finalization, reconnect decisions, and teardown.
- `stopVoiceSession()` releases microphone, provider client, audio player, conversation-repair jobs, and audio routing.
- `tearDown()` additionally cancels dialing/timer work and releases ringtone/DTMF resources.
- `onCleared()` calls `tearDown()`.

The broad coordinator works but remains a maintainability risk because many lifecycle responsibilities are concentrated in one class.

### Voice providers and audio

- `voice/VoiceProvider.kt`: provider-neutral live-voice contract.
- `voice/GeminiLiveProvider.kt`: Gemini Live implementation.
- `voice/ElevenLabsProvider.kt`: ElevenLabs implementation/failover behavior.
- The provider listener contract carries ready/connected, agent PCM, transcripts, interruption, usage, generation-complete, turn-complete, queue metrics, and close events.
- `voice/LiveTurnTelemetry.kt`: content-free live-turn metrics model and Logcat diagnostics.

### Memory and continuity

- `memory/MemoryModels.kt`: durable memory, call summary, profile, confidence/importance/source/timestamp metadata.
- `memory/MemoryStore.kt`: encrypted app-local durable memory and call-summary store, deduplication, correction, pinning/deletion, and review-candidate promotion/rejection.
- `memory/MemoryExtractor.kt`: post-call Gemini/local extraction. Extracted memories are candidates and require explicit Remember approval before durable storage.
- Raw call transcripts are not persisted.

### Settings and credential handling

- `config/ConfigRepository.kt`: runtime provider/user settings. User-entered values take precedence over build defaults.
- `app/build.gradle.kts`: local/debug credentials may come from `local.properties`; release builds explicitly blank reusable Gemini and ElevenLabs API keys.
- Gemini release authentication can use the configured ephemeral-token broker.
- User-entered provider credentials currently live in app-private `SharedPreferences`; Android backup is disabled, but encryption at rest remains an improvement opportunity.

### Storage

- Durable Kenza memory uses `SecureMemoryStorage` and remains encrypted app-local.
- Script Studio keeps an editable local draft in private app preferences and deliberately excludes provider secrets.
- General runtime settings use app-private preferences.

## Finding classification

### Verified defects or missing required behavior

1. Live timing instrumentation is incomplete. `LiveTurnMetrics` contains speech start/end fields, but the current code does not populate every timing required by issue #59, including a complete connection/speech-finalization/interruption/teardown sequence.
2. Script library/history is not yet implemented. Script Studio currently persists one editable draft, not a library of saved scripts (#36).
3. Production TTS rendering is not implemented. Provider-neutral TTS, long-script segmentation, render/playback controls, and render cleanup remain open (#53-#56, #64).
4. Physical-device audio validation is not complete (#62).

### Verified fixes already shipped during this issue pass

1. Shared Kenza context is used by both Live Call Mode and Script Studio.
2. Script request memory retrieval is bounded/relevance-based and supports explicit selection at the domain boundary.
3. Explicit durable-memory corrections replace stale text in place.
4. Post-call extracted memories require explicit user review before durable storage.
5. Script-generated fictional details have no automatic path into durable memory.
6. Release builds cannot inherit reusable Gemini or ElevenLabs API keys from developer `local.properties`.
7. Pull-request validation covers tests, lint, APK compilation, signed update generation, and signature verification.

### Likely risks / improvement opportunities

1. `CallViewModel` is still a broad coordinator. Splitting session lifecycle, provider selection, memory finalization, and audio routing would reduce regression risk.
2. Runtime user-entered credentials are app-private but not encrypted at rest in `ConfigRepository` preferences.
3. Memory extraction still depends on model quality for candidate suggestions. The mandatory review gate limits impact, but candidate quality should still be measured.
4. Script Studio currently exposes a single working draft. A real library should define retention, naming, duplication, deletion, and export behavior before adding more persistence.

### Unverified concerns that must not be presented as defects yet

1. Exact real-device barge-in latency.
2. Exact silence thresholds that feel most natural across speakerphone, earpiece, wired headsets, and Bluetooth.
3. Perceived speech quality on supported physical Android hardware.
4. Provider-specific interruption behavior under poor networks.

These require timing evidence and/or physical-device validation before tuning claims are made.

### Product decisions still required or intentionally deferred

1. Which production TTS provider(s) should back the provider-neutral rendering boundary.
2. Whether rendered audio should be retained permanently, cached temporarily, or regenerated on demand.
3. Final long-script segmentation limits for each chosen TTS provider.

## Priority order from this audit

1. Finish structured Script Studio request controls and memory-use traceability.
2. Complete live timing instrumentation before changing barge-in/silence behavior.
3. Add provider-neutral TTS boundary and pure segmentation/clean-text pipeline.
4. Add render lifecycle, disclosure, playback, cancellation, and cleanup.
5. Add Script Studio library/history.
6. Perform physical Android audio validation, then tune interruption/silence behavior from measured evidence.
