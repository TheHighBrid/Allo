# Allo 3.9.5

## Highlights

- Restores complete, continuous Gemini Live speech during network bursts.
- Keeps blocking playback work off the WebSocket callback.
- Uses short worker-side AudioTrack writes for responsive interruption handling.
- Defaults built-in phone routes to echo-safe behavior while headsets retain barge-in.
- Fixes repeated transcript turns in post-call memory.
- Refreshes the in-call connection indicator and live-caption presentation.
- Retains ephemeral-token support for distributed Gemini Live builds.

## Upgrade note

Release APKs do not embed a long-lived Gemini key. Configure an ephemeral-token
broker for distributed builds, or enter a private developer key in a local debug
installation.
