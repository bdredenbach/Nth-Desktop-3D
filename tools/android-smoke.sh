#!/bin/sh
# Keep diagnostics even when an instrumentation assertion fails.
# Preserve the Pixel 7 logical dp geometry with less software-GPU work.
adb shell wm size 540x1200
adb shell wm density 210
./gradlew :widgetfixture:assembleDebug || exit $?
adb install -r widgetfixture/build/outputs/apk/debug/widgetfixture-debug.apk || exit $?
timeout 12m ./gradlew :app:connectedDebugAndroidTest
nth_smoke_status=$?
mkdir -p app/build/runtime-screenshots
timeout 15s adb pull /sdcard/Android/data/com.nth.desktop3d/files/verification app/build/runtime-screenshots 2>/dev/null || true
timeout 15s adb pull /sdcard/Android/media/com.nth.desktop3d/additional_test_output app/build/runtime-screenshots 2>/dev/null || true
if [ "$nth_smoke_status" -ne 0 ]; then
    timeout 10s adb exec-out screencap -p > app/build/runtime-screenshots/timeout-or-failure.png || true
fi
timeout 15s adb logcat -d > app/build/runtime-logcat.txt || true
exit "$nth_smoke_status"
