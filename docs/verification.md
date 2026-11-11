# Verification — 0.2.0

This records build evidence and its limits. No physical Pixel 10 testing was performed here.

## Completed

- `assembleDebug`, `assembleDebugAndroidTest`: successful with AGP 8.9.2, Gradle 8.11.1 and OpenJDK 21.
- `lintDebug`: successful with zero errors. Remaining warnings concern English text composition, guarded settings constants, backup policy and the adaptive-icon qualifier.
- `testDebugUnitTest`: 14 tests, zero failures/errors. Four new cases cover keyboard-visible panel bounds, restored bounds after keyboard dismissal, asymmetric landscape cutouts and dock clearance on a short screen. Existing tests cover arithmetic, invalid expressions, page wrapping, physics boundaries, tilt clamping and time-independent smoothing.
- `python3 tools/check_scenes.py`: all four shipped scene modes rendered on Mesa llvmpipe via surfaceless EGL/GLES; tilted pond/ripple rendered; updated lit-mesh shaders compiled and linked. This does not measure mobile GPU performance.

## Startup investigation

The supplied recording shows repeated returns to the previous launcher, followed by a successful launch after clearing app data. The 0.1.0 implementation adds restored shortcuts to a GridLayout before setting its eventual column count. Auto-assigned child column indices can exceed that count, causing Android to reject the layout. The replacement ShortcutGrid measures and places children directly without cached column specifications.

This is a concrete crash path removed from the source. Without device logcat from the recording, the exact exception on that phone remains inferred. The new Android fixture restores 24 shortcuts before the first Activity layout, recreates the Activity, verifies shortcut bounds, then covers all four scenes, app search, Calculator, notes, long press, page buttons and landscape controls. It uses an ActivityMonitor instead of startActivitySync, whose idle wait can stall on a continuously animated launcher.

## Android interaction verification

The local Android 35 emulator has no hardware acceleration and did not complete boot within the bounded verification attempt. No passing result is claimed for the updated Android interaction fixture. Previous GitHub smoke runs were blocked by a stale run/idle waits; current CI results should be checked independently:

https://github.com/bdredenbach/Nth-Desktop-3D/actions

## On-device checks still required

- Install over the supplied 0.1.0 build, retain shortcuts, return Home and cold-open several times.
- Switch all four scenes, swipe horizontally, scroll a populated desktop vertically and hold icons/background.
- Search with the keyboard visible; rotate portrait/landscape and check cutout/navigation clearance.
- Physical tilt, recentering, gravity drag/throw/pin, sustained frame times, heat and battery use.
- TalkBack and enlarged text.

The development APK uses the same certificate as the previously supplied build. No data reset or schema migration is required. The signing key is private and excluded from Git.
