# Allo 3.10.0

## iOS-style navigation and call experience

- Added persistent Call, Schedule, and Memory tabs with a native iOS-inspired bottom navigation bar.
- Preserved each top-level section as a peer destination instead of presenting Schedule and Memory as temporary overlays.
- Redesigned the active call screen with a deeper iOS-style gradient, larger caller identity, clearer live status, improved transcript treatment, and refined call controls.
- Refined incoming-call presentation for locked and unlocked states while preserving slide-to-answer and compact banner behavior.
- Added explicit navigation state and unit coverage for tab selection and Settings dismissal behavior.
- Updated the release defaults to version 3.10.0 / version code 30.

## Security and release hygiene

- GitHub Actions remain pinned to immutable commit SHAs.
- Checkout actions do not persist repository credentials.
- API-key fields remain masked in Settings.
