# Restore CallViewModel (Termux / any machine)

`CallViewModel.kt` on `main` was truncated by a large API write. Restore the last good full file from git history, then apply the small hybrid patch.

## On this phone (Termux)

```bash
# 1. Clone (or cd into your existing Allo clone)
cd ~
git clone https://github.com/TheHighBrid/Allo.git
cd Allo

# 2. Restore full CallViewModel from last known-good commit (Oct 4, 2026)
git show 3cac62eaa5950e9062c30cb6dabfd392c53634d2:app/src/main/java/com/kenza/callsim/call/CallViewModel.kt \
  > app/src/main/java/com/kenza/callsim/call/CallViewModel.kt

# 3. Pull latest docs/patches if needed, then apply hybrid endpoint + dial-overlap
git pull origin main
patch -p1 < docs/patches/hybrid_endpoint.patch

# 4. Commit and push
git add app/src/main/java/com/kenza/callsim/call/CallViewModel.kt
git commit -m "fix(call): restore full CallViewModel + hybrid audioStreamEnd + dial-overlap"
git push origin main
```

## What the hybrid patch does

1. Local quiet ~520 ms → debounced `endAudioStream()` for Gemini (faster turn-taking).
2. Outgoing calls start the provider while still showing the calling UI (hides handshake latency).

## Verify

```bash
# if Android SDK / Gradle available on this device
./gradlew :app:compileDebugKotlin
```

If `patch` fails because the base file differs, open `docs/patches/hybrid_endpoint.patch` and apply the few hunks by hand (they are small).
