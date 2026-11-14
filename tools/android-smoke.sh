#!/bin/sh
# Keep diagnostics even when an instrumentation assertion fails.
# Preserve the Pixel 7 logical dp geometry with less software-GPU work.
adb shell wm size 540x1200
adb shell wm density 210
# Run the independent replacement experiment first. Connected-test cleanup
# removes its own instrumentation APK, so install our harness explicitly here.
./gradlew :widgetfixture:assembleDebug :app:assembleRelease :app:assembleDiscoverTest :app:assembleGoogle :app:assembleDebugAndroidTest -PnthCompatibilitySigning=true || exit $?
adb install -r widgetfixture/build/outputs/apk/debug/widgetfixture-debug.apk || exit $?
adb install -r app/build/outputs/apk/release/app-release.apk || exit $?
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk || exit $?
nth_smoke_status=0
for nth_phase in compat_seed compat_normal compat_discover compat_google compat_normal; do
    case "$nth_phase" in
        compat_normal) adb install -r app/build/outputs/apk/release/app-release.apk || { nth_smoke_status=1; break; } ;;
        compat_google) adb install -r app/build/outputs/apk/google/app-google.apk || { nth_smoke_status=1; break; } ;;
        compat_discover) adb install -r app/build/outputs/apk/discoverTest/app-discoverTest.apk || { nth_smoke_status=1; break; } ;;
    esac
    timeout 30s adb shell am instrument -w -e phase "$nth_phase" com.nth.desktop3d.test/com.nth.desktop3d.test.SmokeInstrumentation > app/build/replacement-check.log 2>&1
    nth_instrument_status=$?
    cat app/build/replacement-check.log
    if [ "$nth_instrument_status" -ne 0 ] || ! grep -Fq 'OK (in-place replacement' app/build/replacement-check.log; then nth_smoke_status=1; break; fi
done
if [ "$nth_smoke_status" -eq 0 ]; then
    # Reset only the disposable emulator's seeded replacement-proof data.
    adb shell pm clear com.nth.desktop3d || exit $?
    timeout 3m ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.phase=workspace
    nth_smoke_status=$?
fi
if [ "$nth_smoke_status" -eq 0 ]; then
    timeout 6m ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.phase=polish
    nth_smoke_status=$?
fi
if [ "$nth_smoke_status" -eq 0 ]; then
    timeout 14m ./gradlew :app:connectedDebugAndroidTest
    nth_smoke_status=$?
fi
mkdir -p app/build/runtime-screenshots
timeout 15s adb pull /sdcard/Android/data/com.nth.desktop3d/files/verification app/build/runtime-screenshots 2>/dev/null || true
timeout 15s adb pull /sdcard/Android/media/com.nth.desktop3d/additional_test_output app/build/runtime-screenshots 2>/dev/null || true
if [ "$nth_smoke_status" -ne 0 ]; then
    timeout 10s adb exec-out screencap -p > app/build/runtime-screenshots/timeout-or-failure.png || true
fi
timeout 15s adb logcat -d > app/build/runtime-logcat.txt || true
exit "$nth_smoke_status"
