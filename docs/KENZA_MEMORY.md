# Kenza memory and personality system

Allo 3.8 keeps conversational continuity locally on the Android device.

## What happens after a call

1. `CallViewModel` collects the user and agent transcript only while the call is active.
2. When a real two-way call ends, `MemoryExtractor` sends a bounded transcript to Gemini for structured extraction.
3. Gemini returns a concise call summary, mood, highlights, unresolved topics, a next-call follow-up, and durable memories.
4. The structured result is encrypted with AES-GCM using an Android Keystore key.
5. The raw transcript is cleared and is not persisted.

If Gemini is unavailable, Allo still records a limited local summary and preserves explicit phrases such as “remember that…”.

## What loads before a call

Immediately before an outgoing, incoming, or scheduled call, `MemoryContext` builds a private continuity briefing from:

- Kenza’s stable character profile
- Mohamed’s profile
- relationship and shared-history context
- Kenza’s ambitions and future goals
- boundaries and important context
- recent call summaries
- pinned and high-value memories
- open plans and goals
- unresolved topics and follow-ups
- normal call-time habits
- the current date and time

The briefing is treated as factual context, never as instructions to read aloud.

## Where the data lives

Memory is stored in `kenza_memory_v2.enc` inside the app-private files directory. The encryption key is generated and retained by Android Keystore. The public repository never contains personal profiles, memories, call summaries, transcripts, or encryption keys.

The first Allo 3.8 launch migrates the older SharedPreferences memory automatically.

## Memory screen

Open **Memory** from the home dialer to:

- edit Kenza, Mohamed, relationship, goal, and boundary profiles
- review recent call summaries
- see unresolved topics and next-call prompts
- add important memories manually
- pin or delete memories
- mark plans and goals complete
- delete the complete local memory store

## ElevenLabs

Gemini Live receives the memory briefing automatically.

For ElevenLabs, enable **Send persona + memory to ElevenLabs** in Allo Settings and enable **Security → Overrides → System prompt** in the ElevenLabs agent. ElevenLabs rejects system-prompt overrides when that dashboard permission is disabled.

## Privacy design

- no raw transcript retention
- no GitHub memory file
- no public cloud memory database
- encrypted app-private storage
- user-reviewable memories
- user-controlled deletion
- bounded summaries and context selection
- correction and confidence support
