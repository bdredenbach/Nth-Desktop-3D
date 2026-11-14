#!/bin/sh
# Each CI suite uses a fresh emulator. Preserve diagnostics before the next suite.
nth_suite=${1:-full}
case "$nth_suite" in workspace|polish|full) ;; *) exit 2 ;; esac
nth_output="app/build/native-verification/$nth_suite"
mkdir -p "$nth_output"
adb shell wm size 540x1200
adb shell wm density 210
./gradlew :widgetfixture:assembleDebug :app:assembleRelease :app:assembleDiscoverTest :app:assembleGoogle :app:assembleDebugAndroidTest -PnthCompatibilitySigning=true || exit $?
adb install -r widgetfixture/build/outputs/apk/debug/widgetfixture-debug.apk || exit $?
adb install -r app/build/outputs/apk/release/app-release.apk || exit $?
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk || exit $?
nth_smoke_status=0
if [ "$nth_suite" = workspace ]; then
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
fi
if [ "$nth_smoke_status" -eq 0 ]; then
    # Reset only this disposable emulator's installed application data.
    adb shell pm clear com.nth.desktop3d || exit $?
    case "$nth_suite" in
        workspace) timeout 3m ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.phase=workspace ;;
        polish) timeout 6m ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.phase=polish ;;
        full) timeout 14m ./gradlew :app:connectedDebugAndroidTest ;;
    esac
    nth_smoke_status=$?
fi
for nth_directory in app/build/reports/androidTests app/build/outputs/androidTest-results app/build/outputs/connected_android_test_additional_output; do
    if [ -d "$nth_directory" ]; then cp -R "$nth_directory" "$nth_output/"; fi
done
mkdir -p "$nth_output/screenshots"
timeout 15s adb pull /sdcard/Android/data/com.nth.desktop3d/files/verification "$nth_output/screenshots" 2>/dev/null || true
timeout 15s adb pull /sdcard/Android/media/com.nth.desktop3d/additional_test_output "$nth_output/screenshots" 2>/dev/null || true
if [ "$nth_smoke_status" -ne 0 ]; then timeout 10s adb exec-out screencap -p > "$nth_output/screenshots/timeout-or-failure.png" || true; fi
timeout 15s adb logcat -d > "$nth_output/logcat.txt" || true
exit "$nth_smoke_status"
