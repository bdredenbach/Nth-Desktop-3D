# Development handoff

## 0.7.2 primary APK

Use the `google` build type and the signing helper `--google` flag for the primary APK. It deliberately enables app debugging to retain the phone-tested all-in-one Google connector. Normal app name, same package, existing R2 certificate, code 10087. Do not generate a key. Standard release still builds non-debuggable for comparison. The owner confirmed Google works in 0.6.2 Discover Test; preserving the activity window token was the successful correction. The 0.7.0 recording shows a silent connection with Google 17.74.18. Version 0.7.1 retains the same build mode and adds bounded registration/rebind recovery. Actual Google content must be checked on the owner’s phone.

Desktop page count is now persisted (1–24; three initially). Folder-pattern pages have their own five-column ten-row grid, including two empty top rows. Auto-sort adds a separate page and records generated folder IDs for Undo. Drawer native drag local state never accepts another app's payload. Edge-created empty pages roll back on cancel. All app keys retain profile serial identity. Folder previews use real first-four child icons.

## Desktop management and healthy Discover sessions

`DesktopManagerView` previews saved coordinates without binding any widgets. `LauncherStore.movePage` and `removeEmptyPage` move per-page widget IDs, pattern flags, names, Home index and sort Undo index together with the canonical item lists. Management is unavailable while widget placement is pending. Home intent/Back use `homePage()`; Google remains left of the first screen. Desktop names currently default to numbered names.

Discover accepts current-request scroll callbacks before readiness; cancellation and superseded callback epochs still win. A temporary status 0 cannot move the launcher back over a visible feed. The eight-second display watchdog pauses and retains the connection instead of failing/closing it. Keep feed open is an explicit user acknowledgement for a feed already visible behind the launcher. Retry remains an explicit fresh reconnect; genuine missing-service/readiness failures retain the 15-second recovery budget.

`Move to folder` transfers an item object and all pinned-shortcut metadata. The duplicate case removes the old placement while preserving the destination entry; a full destination leaves the source untouched. Menu label wrapping is separate from desktop `IconLabel.display`.

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
