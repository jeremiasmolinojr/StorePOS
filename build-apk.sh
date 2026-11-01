#!/usr/bin/env bash
# Builds the app and puts StorePOS.apk in this StorePOS folder.
# Needs: JDK 17+ and the Android SDK (installing Android Studio gives you both).
set -e
cd "$(dirname "$0")"
chmod +x gradlew
./gradlew assembleRelease
cp app/build/outputs/apk/release/app-release.apk StorePOS.apk
echo ""
echo "Done: $(pwd)/StorePOS.apk"
