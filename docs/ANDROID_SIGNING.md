# Android update signing

All installable Allo APKs must use one permanent signing identity.

## Canonical signer

- Package: `com.kenza.callsim`
- Certificate SHA-256: `A1:33:B6:08:E3:4B:50:E8:05:9D:51:8B:22:3F:6F:6E:BB:4F:87:F6:31:87:37:AA:24:95:6C:D7:48:D2:5C:B0`
- CI artifact to install: `allo-installable-update`
- GitHub Actions secret: `ALLO_SIGNING_BUNDLE`

The certificate fingerprint is public and is intentionally committed in
`signing/allo-update-cert.sha256`. The private key must never be committed.

## Why this is required

Android only installs an APK over an existing app when the package name matches,
the new `versionCode` is not lower, and the APK is signed by an accepted signing
certificate. Previous CI behavior fell back to a runner-generated debug key when
`keystore.properties` was absent. Hosted runners are disposable, so that signer
could change between builds.

The release build no longer falls back to the debug key. CI restores the same
permanent keystore from the encrypted `ALLO_SIGNING_BUNDLE` repository secret,
builds the release APK with an increasing CI `versionCode`, verifies its package
and signer, and uploads only that verified release APK.

## Recovery rule

Back up the permanent `.p12` private key and its credentials outside GitHub.
Losing the private key means future APKs cannot update installations signed by
this certificate.

An installation that was already signed by one of the old disposable debug keys
cannot be migrated to this new signer by rebuilding the APK. Android requires the
original private key for a same-package in-place update. Once a device is on the
permanent signer, future verified artifacts can update it in place.
