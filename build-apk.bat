@echo off
REM Builds the app and puts StorePOS.apk in this StorePOS folder.
REM Needs: JDK 17+ and the Android SDK (installing Android Studio gives you both).
cd /d "%~dp0"
call gradlew.bat assembleRelease || exit /b 1
copy /Y app\build\outputs\apk\release\app-release.apk StorePOS.apk
echo.
echo Done: %~dp0StorePOS.apk
pause
