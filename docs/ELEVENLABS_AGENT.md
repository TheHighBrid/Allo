# ElevenLabs agent configuration for Kenza

Gemini Live is still Allo's default engine. This page only matters if you use the optional ElevenLabs engine.

`agent_configs/kenza.patch.json` is the versioned, **partial** agent config the app expects. It is a
repo file only. The app never calls the ElevenLabs management API and nothing applies it
automatically. It contains no secrets and no `voice_id`. Keep your cloned voice selected in the
dashboard.

Every field in it exists in the official Agents API reference (`PATCH /v1/convai/agents/{agent_id}`,
checked October 2026).

| Field | Value | Why |
| --- | --- | --- |
| `agent.first_message` | `{{opening_line}}` | The app picks the first words. Mohamed calling → "Allô?". Kenza calling → a caller greeting. Reconnect → empty, so no replayed greeting. |
| `agent.prompt.prompt` | template using `{{kenza_persona}}`, `{{continuity_briefing}}`, `{{call_direction}}`, `{{call_reason}}`, `{{local_time}}` | Persona + memory without the Security prompt override |
| `agent.dynamic_variables.dynamic_variable_placeholders` | defaults for every variable | Dashboard testing and fallbacks |
| `tts.model_id` | `eleven_v4_turbo` | Low-latency v4 model with audio tags |
| `tts.stability` / `similarity_boost` / `speed` | 0.42 / 0.78 / 1.03 | Livelier delivery, still close to the clone, slightly brisker pace |
| `tts.agent_output_audio_format`, `asr.user_input_audio_format` | `pcm_16000` | Matches the app: 16 kHz mic, 16 kHz playback, no resampling |
| `turn.soft_timeout_config` | 2.0 s, LLM-generated 1-3 word filler, off until the first user message | A natural "mmh…" instead of dead air when the LLM is slow |
| `conversation.max_duration_seconds` | 3600 | Fewer forced reconnects (the default is 600) |
| `conversation.client_events` | includes `agent_response_correction` and `agent_response_complete` | The app uses them for truncated-reply correction and turn telemetry |

## Apply it (pick one)

1. **Dashboard (simplest):** set each value above by hand. The prompt text and placeholders are in the JSON.
2. **CLI:** `elevenlabs agents pull --agent "<name>"`. Merge the fields from `kenza.patch.json` into the
   pulled `agent_configs/<name>.json`, then run `elevenlabs agents push --agent "<name>"`. The patch file
   is named `*.patch.json` so a pull never overwrites it.
3. **API:** `curl -X PATCH https://api.elevenlabs.io/v1/convai/agents/$AGENT_ID -H "xi-api-key: $KEY"
   -H "Content-Type: application/json" -d @agent_configs/kenza.patch.json`. The docs don't say whether
   nested objects (for example `agent.prompt`) are merged or replaced. Pull the agent afterwards and check
   that the LLM choice, voice, and tools are unchanged.

Apply it to every backup agent listed in Settings → Backup keys as well.

## In-app settings (ElevenLabs section)

- **Send persona + memory as agent variables** (on by default) sends `dynamic_variables` on every
  connect. Turn it off if an agent that has not been updated rejects the variables. The app then shows a
  "rejected Allo's dynamic variables" error instead of retrying.
- **Expressive audio tags** (on by default) lets Kenza use a few documented lowercase v4 tags
  (`[laughs]`, `[sighs]`, `[exhales]`, `[whispers]`, `[curious]`, `[sarcastic]`, `[mischievously]`,
  `[excited]`), at most one per reply. **Turn it off if the agent uses Flash**, which reads tags aloud.
- **Replace the agent's whole prompt (advanced)** is the old full prompt override. It still needs
  Security → Overrides → System prompt. With the variable template it is not required.

## Check before relying on it

- **Cloned voice type.** v3 Conversational does not preserve Professional Voice Clones. For a PVC,
  A/B `eleven_v4_turbo` against `eleven_flash_v2_5` before keeping v4. If you switch to Flash, turn
  off Expressive audio tags.
- **Behaviour the docs do not state.** Test each of these once on a device:
  - Does an empty `{{opening_line}}` (a reconnect) make the agent wait like an empty `first_message`?
    Force a reconnect mid-call (toggle airplane mode briefly) and listen for a replayed greeting.
  - Does an agent that has not been updated accept the extra dynamic variables?
  - How large can a variable value be? A long continuity briefing could hit an undocumented limit.
  - Is `agent_response_complete` still sent after an interruption?
- **Latency numbers.** Turn on diagnostics and filter Logcat for `LiveTurnMetrics` (per turn) and
  `LiveSessionMetrics` (per session P50/P95). The logs contain only timings: no transcript and no media.
