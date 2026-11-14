# Google Discover overlay troubleshooting — 0.7.2

## Primary Google-enabled build

The owner confirmed real Discover content works on the Pixel 10 with 0.6.2 Discover Test after preserving the activity token. Version 0.7.0 makes Google the default feed and promotes the same direct-client build mode into the primary APK, with the regular Nth Desktop 3D app name. The primary APK enables Android app debugging. No USB-debugging setting or separate companion is needed. Google still supplies the content and controls availability on other phones.

Use `:app:assembleGoogle` / `:app:lintGoogle` and privately sign the exact unsigned Google artifact with `tools/build-signed-apk.py --google`. It keeps package `com.nth.desktop3d` and the existing R2 key. Standard release and Discover Test variants remain buildable for comparison; they are not the primary 0.7.0 download. CI also checks standard -> Discover Test -> Google-enabled -> standard replacement without losing saved data.

A one-time provider migration changes the old built-in-news default to Google. Explicit feed-off, Google-app and already-Google choices survive. The built-in Nth Feed stays selectable.

## 0.6.2 investigation and owner test

The owner's Pixel 10 report from 0.6.1 showed successful binding to Google app 17.72.17 / overlay API 11, followed by status 0. This does not prove that Google created an overlay window. Comparison with the reference client found that Nth replaced the activity token in the window attributes with `View.getWindowToken()`. Version 0.6.2 preserves the original activity token. The report records the token source, window type and whether it differs from the view token, without recording Binder identifiers. A missing token has its own outcome. Attachment now means the one-way request was sent, not that Google acknowledged a surface. Whether this mismatch caused Google's status 0 remains an owner-phone test.

The client resolves the installed Google overlay service and reads its `service.api.version`. Older APIs use `windowAttached` and onResume/onPause; modern APIs use `windowAttached2` and started/resumed state bits through `setActivityState`. It no longer sends both lifecycle styles. The bind URI remains the interoperable v7/cv9 form. Client options remain 15.

Startup allows 15 seconds, with a separate 8-second display allowance after Google reports readiness. A small launcher status strip offers Cancel and does not create a dialog/window that could cover Google's surface. Back, another panel, pause and stop cancel pending opening. Old connection generations and delayed callbacks cannot reopen a canceled request.

Each failure keeps its own outcome and message. A later timeout no longer replaces a permission denial or null binding. Reports record missing/disabled Google, service availability/permission, binder interface, negotiated API, ready bits, lifecycle/window state, error type and up to 40 connection events. No account data, feed history, serial number or device-wide logcat is collected. The latest attempt stays in private preferences. Settings > Feed page > Discover connection details can copy the report locally; nothing is sent automatically.

## Two builds, one installed app

- **Nth-Desktop-3D-0.6.2.apk**: normal non-debuggable release.
- **Nth-Desktop-3D-0.6.2-Discover-Test.apk**: explicitly labelled debuggable test, intended to test Google's direct-client restriction. It enables Android app debugging, so use the normal release when the experiment is complete.

Both have package `com.nth.desktop3d`, code 10084 and the owner's existing R2 certificate. Install either over 0.6.1 without uninstalling. They replace each other; there is no companion package. An emulator checks normal release -> Discover Test -> normal release at equal version code and verifies preferences, desktop layout and notes survive. Both final APK certificates are checked against the prior R2 APK.

## Owner test on Pixel

1. Install the Discover Test APK as an update and open Nth Desktop 3D.
2. Open 3D settings > Feed page > Test Google Discover. This explicit action enables the edge feed and selects Google as provider.
3. Allow startup to finish. If it opens, close it and swipe right from Desktop 1 to repeat the test.
4. If it does not open, choose Connection details > Copy connection report and paste the report into the project chat.
5. Install the normal APK in the ZIP to return to the normal build. No uninstall is needed.

Nth Feed remains available throughout. Google controls the actual Discover content and whether a caller is accepted. A successful connection against the test fixture is not proof that a production Google app accepts the launcher on a particular phone.

## Reference comparison

Reviewed the current Lawnchair 16-dev LauncherClient.java, BaseClientService.java, FeedBridge.kt and OverlayCallbackImpl.kt. Its direct/bridge selection checks system/debuggable application flags, and normal callers use a bridge. This explains why documentation about built-in nightly support alone does not establish compatibility for a normal release. The Nth implementation remains original project code; no Google app code is shipped.

https://github.com/LawnchairLauncher/lawnchair/blob/16-dev/lawnchair/src/app/lawnchair/FeedBridge.kt
https://github.com/LawnchairLauncher/lawnchair/blob/16-dev/lawnchair/src/com/google/android/libraries/launcherclient/LauncherClient.java

## Validation boundaries

The native separate-UID fixture enforces modern versus legacy method selection, delays readiness by 3.5 seconds, simulates null/silent/not-ready/refused-display connections, and renders a real TYPE_APPLICATION window using the activity token. The modern fixture exposes API 11; a view-window token cannot satisfy this window contract. Native checks cover report copying and cancel. Full widget, shortcut, settings and native-feed regression remains enabled. Google app acceptance and actual Google content still require the owner-phone test above.

## Silent service recovery in 0.7.1

The owner recording 226925.mp4 shows 0.7.0 (code 10085) is correctly debuggable, binds Google 17.74.18 API 11, and submits a distinct activity token, but receives no readiness callback (`NO_STATUS`, bits -1). This does not identify a permission denial or establish that the launcher update caused the failure. The protocol code differs from the working 0.6.2 only in report text; Google's installed version also differs from the earlier diagnostic.

For a missing readiness callback only, 0.7.1 refreshes registration at four seconds and reconnects with the legacy window-attachment entry point at eight seconds. Modern API lifecycle calls remain modern. Both stages share the original 15-second startup budget; no infinite reconnect loop. Each attachment has its own callback epoch so an old response cannot clear a newer ready session. A strong callback reference lives with the active connection. Explicit status 0, permission errors, display errors and canceled requests retain their own result. Separate-UID fixtures exercise the two recoverable cases and a delayed stale callback, but are not the real Google app.

## Healthy session behavior in 0.7.2

Recording 226944.mp4 shows real Google content while the launcher still displays Connecting and later reports display timeout. Version 0.7.2 separates readiness from visibility: a valid active-request scroll callback is accepted even before status bit 1; transient status 0 cannot restore desktop icons over a visible feed. Startup recovery stops once the session is usable. The client sends a completed scroll sequence as well as openOverlay, using the existing protocol.

When display confirmation remains absent for eight seconds, keep the binding and pause automatic recovery. The nonmodal strip offers Keep feed open, Retry and Close. Keep feed open is a manual acknowledgement when the feed is already visible; it clears launcher visuals without rebuilding Google's connection. Pause during startup also keeps the bind, with Retry available later. Back/Close restores the launcher. Actual service disconnection and startup errors remain distinct outcomes; there is no infinite timer-based reconnect.

Separate-UID fixtures cover visible-before-ready, status changing while visible, a gesture-only provider, missing-display confirmation retained, manual pause/retry, and cancellation. Their content is not Google's production feed; final Pixel behavior remains a phone check.
