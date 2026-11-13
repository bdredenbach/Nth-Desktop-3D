# Development handoff

## Current update signing (0.3.0 onward)

Use the dedicated test key in the owner's private saved file
`Nth-Desktop-3D-Private-Signing-Backup-20261113.zip`. Never regenerate it.
The expected certificate is pinned in `test-update-certificate.sha256`.
Read `SIGNED-APK-UPDATES.md` and increment `version.properties` before each
release. The public build helper verifies the exact signing identity.
The earlier development certificate below is historical and does not match
the new permanent test-update series. Moving from it requires a one-time migration.

## Historical initial development signing

The first supplied APK uses package `com.nth.desktop3d`, version 0.1.0/code 1,
with this development certificate SHA-256:

`9bdff58a646dcaa9518c14ece74455c2e441a7d8d90e6f998ba61103257dd3d9`

The private backup is named `Nth-Desktop-3D-Development-Signing.zip` in the
project owner's saved files. Restore that development keystore to the Android
user directory identified by `./gradlew signingReport` when producing an update
for users of the APK supplied in this chat. Do not put its private key in Git.
A fresh local debug keystore or a GitHub runner debug keystore creates a different
certificate. CI artifacts are reproducible build evidence, but their default
runner certificate is not an update certificate for the separately supplied APK.

The first implementation is intentionally independent of the other Nth variants.
Do not assume their backup schema, window framework, widgets, or complete utility
suite has already been ported. Inspect that source and add compatibility in later
integrations. Keep the canonical shortcut layout separate from scene settings.

Run:

```sh
./gradlew assembleDebug assembleDebugAndroidTest lintDebug testDebugUnitTest
./gradlew connectedDebugAndroidTest
python tools/check_scenes.py
```

The last command requires Linux Mesa EGL and Pillow; it validates the actual
shipped shaders using software GLES. It is not a phone frame-rate benchmark.
