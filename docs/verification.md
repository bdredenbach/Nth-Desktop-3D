# Verification — 0.1.0

This file records evidence for the initial development build. It is not a claim
of physical Pixel 10 testing.

## Completed

- `assembleDebug`: successful with AGP 8.9.2 / Gradle 8.11.1 / OpenJDK 21.
- `lintDebug`: successful, zero errors. Non-blocking warnings cover English-only
  text composition, guarded settings-panel constants, explicit backup policy,
  and the API-26 adaptive-icon resource qualifier.
- `testDebugUnitTest`: 10 tests, zero failures/errors. Tests cover arithmetic,
  invalid expressions, page wrapping, physics boundaries, tilt clamping and
  frame-rate-independent smoothing.
- `python tools/check_scenes.py`: the shipped scene shader compiled and all four
  modes rendered on Mesa llvmpipe via surfaceless EGL/GLES. The tilted pond with
  a live ripple also rendered, and the exact lit-mesh shaders compiled/linked.
  This establishes desktop shader compatibility, not mobile GPU performance.

## Runtime status

A GitHub Android 35 emulator reached the app during the initial smoke run.
That run stopped on a null comparison in the test helper, rather than an app
startup exception. The helper was corrected. A later run stalled while waiting
for a completely idle UI message queue; this launcher continuously animates.
The current helper uses bounded waits and a UI-thread barrier, with an 8-minute
command timeout and a 15-minute job timeout.

The corrected check was still queued behind the stale run at delivery. It has
**not** been recorded as a passing Android UI test. The current GitHub connection
cannot cancel the obsolete run. Check the Actions page for subsequent results:

https://github.com/bdredenbach/Nth-Desktop-3D/actions

The local unaccelerated Android 35 emulator could not provide stable package
services during first boot, so it was stopped. Desktop scene screenshots are
shader evidence, not Android UI screenshots. Pixel interaction testing remains
necessary.

## Remaining device checks

- Pixel 10 orientation, cutout, navigation gestures and keyboard insets.
- Physical tilt feel, recentering and gravity interactions.
- Actual touch/long-press behavior, lifecycle return Home, and app/profile changes.
- Sustained frame times, heat and battery use on device.
- TalkBack navigation and enlarged-font layouts.

The first release has no external widget hosting, `.nthdesktop` import, or the
complete utility suite from other Nth launchers. Those integrations are separate
future changes, rather than tested features in this build.
