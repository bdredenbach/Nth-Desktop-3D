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

Android emulator testing is in progress. See the final build report for the
observed installation/UI outcome. Never infer Android interaction testing from
shader screenshots or unit tests.

## Remaining device checks

- Pixel 10 orientation, cutout, navigation gestures and keyboard insets.
- Physical tilt feel, recentering and gravity interactions.
- Actual touch/long-press behavior, lifecycle return Home, and app/profile changes.
- Sustained frame times, heat and battery use on device.
- TalkBack navigation and enlarged-font layouts.

The first release has no external widget hosting, `.nthdesktop` import, or the
complete utility suite from other Nth launchers. Those integrations are separate
future changes, rather than tested features in this build.
