# Repository audit

Last reviewed: 2026-08-13

## Implemented in this review

- Disabled Android backup for the whole application. Settings currently include provider credentials and private persona context, so allowing OS backup was inconsistent with the app's device-private promise.
- Made `ScheduleReceiver` non-exported. Android and this application can still deliver the declared system and explicit broadcasts, while unrelated applications can no longer trigger Allo's call alarms.
- Escaped every local property before using it as a generated Java string literal. Quotes, slashes, and control characters can no longer corrupt `BuildConfig` generation.
- Added Android lint to pull-request validation, alongside unit tests and debug compilation.
- Restored lint's error gate for release builds; signed artifacts can no longer be produced when Android lint reports a correctness or security error.
- Limited the normal validation job to read-only repository access. Only the isolated, manual publish job receives permission to create a GitHub release, and it can run only after the signed APK has been verified and uploaded.

## Recommended next enhancements

### High priority

1. **Move runtime credentials to encrypted storage.** `ConfigRepository` uses ordinary private `SharedPreferences`. Backups are now disabled and app sandboxing limits access, but encryption at rest would improve protection on compromised or extracted devices.
2. **Remove API keys from production clients.** Finish the ephemeral-token broker path and make it the only distributable Gemini authentication mode. Apply the same short-lived credential design to any provider that supports it.
3. **Add instrumented security tests.** Verify backup policy, receiver exposure, Keystore migration, corrupted-memory recovery, and process recreation on supported Android API levels.
4. **Split the broad `CallViewModel`.** Extract session lifecycle, provider selection, memory finalization, and audio routing into focused controllers with explicit interfaces and unit tests.

### Medium priority

1. Add accessibility checks for call controls, including content descriptions, minimum touch targets, contrast, TalkBack order, and non-color status cues.
2. Add Compose screenshot tests for incoming, active, settings, schedule, consent, and memory states across font scales and narrow screens.
3. Add dependency update and static-analysis automation, with reviewed upgrade pull requests rather than unpinned automatic releases.
4. Add a user-controlled export/reset flow for memory and settings, with a clear warning that Android backups remain intentionally disabled.
5. Remove prebuilt APK/ZIP binaries from normal Git history and publish them as signed release artifacts to reduce repository size and ambiguity about canonical builds.

## Validation baseline

Every pull request affecting the Android app should pass unit tests, Android lint, and a debug APK build on JDK 17. Release publication should continue to require the permanent signing key and certificate verification in CI.
