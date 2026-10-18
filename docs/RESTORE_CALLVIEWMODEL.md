# Restore CallViewModel (hybrid endpoint + dial-overlap)

A large GitHub Contents API write truncated `CallViewModel.kt` on `main`.
The complete hybrid file is checked into this repo under:

`docs/patches/CallViewModel_hybrid.kt`

## One-command restore

From the repo root:

```bash
cp docs/patches/CallViewModel_hybrid.kt \
  app/src/main/java/com/kenza/callsim/call/CallViewModel.kt
git add app/src/main/java/com/kenza/callsim/call/CallViewModel.kt
git commit -m "fix(call): restore full CallViewModel with hybrid audioStreamEnd + dial-overlap"
git push origin main
```

## What the hybrid adds

1. **Hybrid VAD** — local speech end (~520 ms quiet) calls `endAudioStream()` so Gemini does not wait the full 800 ms server silence window.
2. **Dial-overlap** — outgoing calls start the provider while still showing the calling UI, so handshake latency is hidden.
3. Debounce via `CLIENT_ENDPOINT_MIN_INTERVAL_MS` so mic noise does not spam endpoint signals.

After restore, verify:

```bash
./gradlew :app:compileDebugKotlin
```
