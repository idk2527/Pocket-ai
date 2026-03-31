@echo off
echo ========================================
echo Pocket AI - Deploy and Test
echo ========================================
echo.

echo [1/4] Checking for connected devices...
adb devices

echo.
echo [2/4] Installing APK...
adb install -r app\build\outputs\apk\debug\app-debug.apk

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] APK installation failed!
    echo Please check if device is connected and USB debugging is enabled.
    pause
    exit /b 1
)

echo.
echo [3/4] Starting app...
adb shell am start -n com.pocketai.app/.presentation.MainActivity

echo.
echo [4/4] Monitoring logs...
echo Press Ctrl+C to stop monitoring
echo.
adb logcat -s "PocketAI"