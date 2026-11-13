#!/bin/sh
# Keep diagnostics even when an instrumentation assertion fails.
./gradlew :widgetfixture:assembleDebug || exit $?
adb install -r widgetfixture/build/outputs/apk/debug/widgetfixture-debug.apk || exit $?
timeout 8m ./gradlew :app:connectedDebugAndroidTest
nth_smoke_status=$?
mkdir -p app/build/runtime-screenshots
adb pull /sdcard/Android/data/com.nth.desktop3d/files/verification app/build/runtime-screenshots 2>/dev/null || true
adb logcat -d > app/build/runtime-logcat.txt
exit "$nth_smoke_status"
