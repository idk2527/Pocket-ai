@echo off
echo Pocket AI GGUF Inference Test
echo ==============================
echo.

echo 1. Clearing logs...
adb logcat -c
echo.

echo 2. Starting log capture...
start "Pocket AI Logs" cmd /k "adb logcat -v time | findstr \"PocketAI\|pocketai\|LLM\|GGUF\|Llama\|JNI\" > pocket_ai_test_%date:~-4,4%%date:~-10,2%%date:~-7,2%.txt"
echo.

echo 3. Installing APK...
adb install -r app\build\outputs\apk\debug\app-debug.apk
echo.

echo 4. Granting permissions...
adb shell pm grant com.pocketai.app android.permission.CAMERA
adb shell pm grant com.pocketai.app android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant com.pocketai.app android.permission.WRITE_EXTERNAL_STORAGE
echo.

echo 5. Launching app...
adb shell am start -n com.pocketai.app/.MainActivity
echo.

echo.
echo ========================================
echo TEST INSTRUCTIONS:
echo 1. Navigate to "Add Expense" screen
echo 2. Click "Scan Receipt"
echo 3. Scan a test receipt
echo 4. Observe logs in the captured file
echo 5. Press Ctrl+C in log window when done
echo ========================================
echo.

pause