# Google Discover overlay troubleshooting — 0.6.1

## What changed

The client resolves the installed Google overlay service and reads its `service.api.version`. Older APIs use `windowAttached` and onResume/onPause; modern APIs use `windowAttached2` and started/resumed state bits through `setActivityState`. It no longer sends both lifecycle styles. The bind URI remains the interoperable v7/cv9 form. Client options remain 15.

Startup allows 15 seconds, with a separate 8-second display allowance after Google reports readiness. A small launcher status strip offers Cancel and does not create a dialog/window that could cover Google's surface. Back, another panel, pause and stop cancel pending opening. Old connection generations and delayed callbacks cannot reopen a canceled request.

Each failure keeps its own outcome and message. A later timeout no longer replaces a permission denial or null binding. Reports record missing/disabled Google, service availability/permission, binder interface, negotiated API, ready bits, lifecycle/window state, error type and up to 40 connection events. No account data, feed history, serial number or device-wide logcat is collected. The latest attempt stays in private preferences. Settings > Feed page > Discover connection details can copy the report locally; nothing is sent automatically.

## Two builds, one installed app

- **Nth-Desktop-3D-0.6.1.apk**: normal non-debuggable release.
- **Nth-Desktop-3D-0.6.1-Discover-Test.apk**: explicitly labelled debuggable test, intended to test Google's direct-client restriction. It enables Android app debugging, so use the normal release when the experiment is complete.

Both have package `com.nth.desktop3d`, code 10083 and the owner's existing R2 certificate. Install either over 0.6.0 without uninstalling. They replace each other; there is no companion package. An emulator checks normal release -> Discover Test -> normal release at equal version code and verifies preferences, desktop layout and notes survive. Both final APK certificates are checked against the prior R2 APK.

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

The native separate-UID fixture enforces modern versus legacy method selection, delays readiness by 3.5 seconds, simulates null/silent/not-ready/refused-display connections, and renders a real child window using the launcher token. Native checks cover report copying and cancel. Full widget, shortcut, settings and native-feed regression remains enabled. Google app acceptance and actual Google content still require the owner-phone test above.
