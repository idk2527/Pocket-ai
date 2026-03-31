@echo off
REM Simple test script - install and run Pocket AI

echo Pocket AI - Quick Test
echo ======================
echo.

REM Check for device
adb devices | findstr /v "List" | findstr /v "^$" >nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] No device connected
    pause
    exit /b 1
)

REM Get first device
for /f "tokens=1" %%i in ('adb devices ^| findstr /v "List" ^| findstr /v "^$"') do (
    set "DEVICE=%%i"
    goto :found
)

:found
echo Device: %DEVICE%
echo.

REM Uninstall old
echo Uninstalling old version...
adb -s %DEVICE% uninstall com.pocketai.app >nul 2>&1

REM Install new
echo Installing APK...
adb -s %DEVICE% install -r app\build\outputs\apk\debug\app-debug.apk
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Installation failed
    pause
    exit /b 1
)

echo.
echo Starting app...
adb -s %DEVICE% shell am start -n com.pocketai.app/.MainActivity

echo.
echo App should be running now.
echo Open the app and try the scan feature.
echo.
echo To see logs, run in a separate terminal:
echo   adb -s %DEVICE% logcat -v time ^| findstr /C:"PocketAI"
echo.
pause