# Pocket AI - GGUF Inference Deployment & Testing Guide

## Overview
This guide covers deploying the Pocket AI APK with real GGUF inference to physical devices and capturing runtime logs to verify the native llama.cpp integration.

## Prerequisites

### Hardware Requirements
- **Target Devices:**
  - Xiaomi 14T (arm64-v8a)
  - Samsung A55 (arm64-v8a)
  - Any Android device with ARM64 architecture

### Software Requirements
- **On Device:**
  - Android 8.0 (API 26) or higher
  - USB Debugging enabled
  - Sufficient storage (~1.5GB for app + model)

- **On Computer:**
  - Android SDK Platform Tools (adb)
  - USB cable for device connection

## Deployment Steps

### 1. Connect Device

```bash
# Enable USB Debugging on device
# Settings > About Phone > Tap "Build Number" 7 times
# Settings > Developer Options > USB Debugging

# Verify connection
adb devices
```

**Expected Output:**
```
List of devices attached
XXXXXXXX    device
```

### 2. Install APK

```bash
# Install the APK
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Verify installation
adb shell pm list packages | findstr pocketai
```

**Expected Output:**
```
package:com.pocketai.app
```

### 3. Grant Permissions

```bash
# Grant camera permission
adb shell pm grant com.pocketai.app android.permission.CAMERA

# Grant storage permissions
adb shell pm grant com.pocketai.app android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant com.pocketai.app android.permission.WRITE_EXTERNAL_STORAGE

# Optional: Grant microphone permission (if needed)
adb shell pm grant com.pocketai.app android.permission.RECORD_AUDIO
```

### 4. Clear Previous Logs

```bash
# Clear logcat buffer
adb logcat -c
```

## Runtime Log Capture

### Method 1: Real-time Log Capture

```bash
# Capture logs in real-time (filter for Pocket AI)
adb logcat -v time | findstr "PocketAI\|pocketai\|LLM\|GGUF\|Llama\|JNI"
```

### Method 2: Save Logs to File

```bash
# Save logs to file for analysis
adb logcat -v time > pocket_ai_logs.txt

# In another terminal, run the app and perform scanning
# Then stop with Ctrl+C
```

### Method 3: Detailed Native Logs

```bash
# Capture all logs including native output
adb logcat -v time | findstr "PocketAI\|pocketai\|LLM\|GGUF\|Llama\|JNI\|llama.cpp\|native"
```

## Testing Scenarios

### Test 1: App Launch & Model Loading

**Steps:**
1. Launch Pocket AI app
2. Navigate to "Add Expense" screen
3. Observe logs for model loading

**Expected Log Pattern:**
```
PocketAI: NativeLLama class loaded
PocketAI: GGUF model path: /data/user/0/com.pocketai.app/files/models/gemma-3-1b-it-q4_0.gguf
PocketAI: Model file exists: true
PocketAI: Loading GGUF model...
PocketAI: Model loaded successfully, contextId: 12345
PocketAI: ModelLoadStatus: LOADED
```

### Test 2: Receipt Scanning with GGUF Inference

**Steps:**
1. Click "Scan Receipt" button
2. Point camera at a test receipt
3. Capture image
4. Wait for processing

**Expected Log Pattern:**
```
PocketAI: Starting OCR processing...
PocketAI: OCR text extracted: [receipt text]
PocketAI: Starting GGUF inference...
PocketAI: Tokenizing prompt...
PocketAI: Tokenization complete: 156 tokens
PocketAI: Starting llama inference...
PocketAI: Inference complete
PocketAI: Raw LLM output: {JSON output}
PocketAI: Parsing LLM output...
PocketAI: Parsed expense: Store=Test Store, Amount=45.50
PocketAI: Inference completed successfully
```

### Test 3: Fallback to Heuristic Parsing

**Steps:**
1. Use a malformed receipt or trigger LLM failure
2. Observe fallback behavior

**Expected Log Pattern:**
```
PocketAI: Starting GGUF inference...
PocketAI: Model load failed, contextId is null
PocketAI: Falling back to heuristic parsing...
PocketAI: Heuristic parsing result: Store=Test Store, Amount=45.50
PocketAI: Fallback parsing successful
```

### Test 4: Memory Usage Monitoring

**Steps:**
1. Monitor memory during model loading
2. Check for memory leaks

**Expected Behavior:**
- Model loading: < 500MB RAM usage
- Inference: < 300MB RAM usage
- Cleanup: Memory released after inference

**Log Pattern:**
```
PocketAI: Memory before load: 120MB
PocketAI: Memory after load: 450MB
PocketAI: Memory after inference: 280MB
PocketAI: Memory after cleanup: 130MB
```

## Log Analysis Checklist

### ✅ Native Library Loading
- [ ] `NativeLLama class loaded` appears
- [ ] No `UnsatisfiedLinkError` errors
- [ ] `System.loadLibrary("llama_jni")` succeeds

### ✅ Model Loading
- [ ] `Model file exists: true`
- [ ] `Loading GGUF model...` appears
- [ ] `Model loaded successfully` appears
- [ ] `contextId` is not null
- [ ] `ModelLoadStatus: LOADED` appears

### ✅ Tokenization
- [ ] `Tokenizing prompt...` appears
- [ ] `Tokenization complete: X tokens` appears
- [ ] Token count is reasonable (50-500 tokens)

### ✅ Inference
- [ ] `Starting llama inference...` appears
- [ ] `Inference complete` appears
- [ ] Raw JSON output is valid
- [ ] No segmentation faults

### ✅ Memory Management
- [ ] No memory leak warnings
- [ ] Memory usage stays within limits
- [ ] Cleanup completes successfully

### ✅ Error Handling
- [ ] Graceful fallback to heuristic parsing
- [ ] No app crashes
- [ ] Error messages are informative

## Common Issues & Solutions

### Issue 1: `UnsatisfiedLinkError`
**Symptom:**
```
java.lang.UnsatisfiedLinkError: couldn't find "libllama_jni.so"
```

**Solution:**
```bash
# Verify native library is in APK
adb shell unzip -l /data/app/com.pocketai.app/base.apk | grep libllama_jni.so

# Check device architecture
adb shell getprop ro.product.cpu.abi
```

### Issue 2: Model Not Found
**Symptom:**
```
PocketAI: Model file exists: false
```

**Solution:**
```bash
# Verify model is in assets
adb shell ls -lh /data/user/0/com.pocketai.app/files/models/

# If missing, reinstall APK
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Issue 3: Model Load Failure
**Symptom:**
```
PocketAI: Model load failed, contextId is null
```

**Possible Causes:**
- Insufficient memory
- Corrupted model file
- Architecture mismatch

**Debug Steps:**
```bash
# Check available memory
adb shell cat /proc/meminfo | grep MemAvailable

# Verify model file integrity
adb shell md5sum /data/user/0/com.pocketai.app/files/models/gemma-3-1b-it-q4_0.gguf
```

### Issue 4: Inference Timeout
**Symptom:**
```
PocketAI: Inference taking longer than expected...
```

**Solution:**
- Check device performance
- Consider using a smaller model
- Verify no other apps consuming CPU

## Performance Benchmarks

### Expected Performance

| Device | Model Load Time | Inference Time | Memory Usage |
|--------|----------------|----------------|--------------|
| Xiaomi 14T | 3-5s | 5-10s | ~400MB |
| Samsung A55 | 4-6s | 6-12s | ~420MB |
| Mid-range | 5-8s | 8-15s | ~450MB |

### Acceptable Performance
- ✅ Model load: < 10s
- ✅ Inference: < 20s
- ✅ Total processing: < 30s
- ✅ Memory peak: < 500MB

## Test Receipts

### Test Receipt 1: Simple
```
Walmart
Milk $3.50
Bread $2.50
Total: $6.00
```

### Test Receipt 2: Complex
```
Amazon Marketplace
Order #123-4567890
Item: Wireless Mouse $29.99
Item: Keyboard $49.99
Subtotal: $79.98
Tax: $6.40
Total: $86.38
USD
```

### Test Receipt 3: Multi-currency
```
Tesco UK
Coffee £4.50
Tea £3.20
Total: £7.70
```

## Automated Testing Script

Create `test_pocket_ai.bat`:

```batch
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
```

## Success Criteria

### ✅ Deployment Success
- [ ] APK installs without errors
- [ ] App launches successfully
- [ ] No immediate crashes

### ✅ Native Library Success
- [ ] `NativeLLama class loaded` appears in logs
- [ ] No `UnsatisfiedLinkError`

### ✅ Model Loading Success
- [ ] `Model loaded successfully` appears
- [ ] `contextId` is not null
- [ ] `ModelLoadStatus: LOADED` appears

### ✅ Inference Success
- [ ] `Inference complete` appears
- [ ] Valid JSON output is produced
- [ ] Expense is parsed correctly

### ✅ Fallback Success
- [ ] Heuristic parsing works when LLM fails
- [ ] No app crashes on errors

## Reporting Results

### Create Test Report

Create `TEST_RESULTS.md`:

```markdown
# Pocket AI GGUF Inference Test Results

## Device Information
- **Device:** [Device Name]
- **Model:** [Model Number]
- **Android Version:** [Version]
- **Architecture:** [arm64-v8a/armeabi-v7a]

## Test Date
[Date]

## Test Results

### Test 1: App Launch & Model Loading
- **Status:** ✅ PASS / ❌ FAIL
- **Load Time:** [X] seconds
- **Memory Usage:** [X] MB
- **Logs:** [Paste relevant logs]

### Test 2: Receipt Scanning
- **Status:** ✅ PASS / ❌ FAIL
- **Inference Time:** [X] seconds
- **Result Accuracy:** [High/Medium/Low]
- **Logs:** [Paste relevant logs]

### Test 3: Fallback Parsing
- **Status:** ✅ PASS / ❌ FAIL
- **Fallback Worked:** Yes/No
- **Logs:** [Paste relevant logs]

### Test 4: Memory Usage
- **Peak Memory:** [X] MB
- **Memory Leak:** Yes/No
- **Logs:** [Paste relevant logs]

## Issues Found
[Describe any issues]

## Recommendations
[Suggest improvements]

## Overall Status
✅ PASS / ❌ FAIL
```

## Next Steps

1. **Deploy to Xiaomi 14T** and capture logs
2. **Deploy to Samsung A55** and capture logs
3. **Analyze logs** for performance and errors
4. **Fix any issues** found
5. **Optimize** based on device-specific behavior
6. **Document** findings for future reference

## Quick Reference Commands

```bash
# Full deployment and test
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb logcat -c
adb logcat -v time | findstr "PocketAI\|pocketai\|LLM\|GGUF\|Llama\|JNI"

# Check device info
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.product.cpu.abi

# Monitor memory
adb shell dumpsys meminfo com.pocketai.app

# Monitor CPU
adb shell top -n 1 | findstr pocketai
```

## Support Resources

- **kotlinllamacpp GitHub:** https://github.com/ljcamargo/kotlinllamacpp
- **llama.cpp GitHub:** https://github.com/ggml-org/llama.cpp
- **Android NDK Docs:** https://developer.android.com/ndk
- **ADB Commands:** https://developer.android.com/studio/command-line/adb

---

**Good luck with testing!** 🚀

The native GGUF inference is now ready for real-world testing on physical devices.