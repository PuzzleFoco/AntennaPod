# Wear OS Emulator Testing Guide

This guide explains how to test the AntennaPod Wear OS app using Android emulators to verify startup and functionality.

## Why Emulator Testing?

Emulator testing provides:
- ✅ Fast feedback loop for development
- ✅ Verification of app startup and initialization
- ✅ Testing without physical device
- ✅ Reproducible test environment
- ✅ Ability to test on different Wear OS versions

## Prerequisites

### 1. Install Android Studio

Download from: https://developer.android.com/studio

### 2. Install Android SDK Tools

In Android Studio:
1. Go to **Tools → SDK Manager**
2. Install:
   - Android SDK Platform 34 (or your target)
   - Google Play Services
   - Wear OS system images

### 3. Install Wear OS Emulator Images

In SDK Manager → **SDK Tools** tab:
1. Enable **Show Package Details**
2. Under **Android SDK Build-Tools**, install latest version
3. Under **System Images** tab, install:
   - Wear OS 3 (API 30) system image
   - Wear OS 4 (API 33) system image

## Setting Up Wear OS Emulator

### Method 1: Using Android Studio AVD Manager (Recommended)

1. **Open AVD Manager**
   - Tools → Device Manager
   - Or click the device icon in toolbar

2. **Create Virtual Device**
   - Click **Create Device**
   - Category: **Wear OS**
   - Select device: **Wear OS Small Round** or **Wear OS Square**
   - Click **Next**

3. **Select System Image**
   - Choose **Wear OS 3** or **Wear OS 4**
   - Click **Download** if not installed
   - Click **Next**

4. **Configure AVD**
   - Name: `Wear_OS_Emulator`
   - Startup orientation: Portrait
   - Click **Finish**

### Method 2: Using Command Line

```bash
# List available system images
sdkmanager --list | grep wear

# Install Wear OS system image
sdkmanager "system-images;android-30;android-wear;x86"

# Create AVD
avdmanager create avd \
  -n Wear_OS_Test \
  -k "system-images;android-30;android-wear;x86" \
  -d "wear_round"

# List created AVDs
avdmanager list avd
```

## Testing Startup

### 1. Build the APK

```bash
cd /path/to/AntennaPod
./gradlew :wear:assemblePlayDebug
```

The APK will be at: `wear/build/outputs/apk/play/debug/wear-play-debug.apk`

### 2. Start the Emulator

**From Android Studio:**
- Device Manager → Select your Wear OS device → Click ▶️

**From Command Line:**
```bash
emulator -avd Wear_OS_Test
```

Wait for the emulator to fully boot (shows watch face).

### 3. Install the APK

```bash
# Connect to emulator
adb devices

# Install APK
adb install wear/build/outputs/apk/play/debug/wear-play-debug.apk

# Or reinstall if already installed
adb install -r wear/build/outputs/apk/play/debug/wear-play-debug.apk
```

### 4. Launch the App

**From Emulator:**
- Swipe up to open app drawer
- Find "AntennaPod" and tap

**From Command Line:**
```bash
adb shell am start -n de.danoeh.antennapod.play/de.danoeh.antennapod.wear.ui.MainActivity
```

### 5. Monitor Logs

```bash
# Watch all logs
adb logcat

# Filter for AntennaPod
adb logcat | grep -i antennapod

# Filter for WorkManager (verify initialization)
adb logcat | grep -i workmanager

# Filter for errors only
adb logcat *:E

# Clear logs and start fresh
adb logcat -c && adb logcat
```

## What to Verify

### ✅ Successful Startup Checklist

1. **App Launches**
   - ✅ App icon appears in launcher
   - ✅ Tapping icon opens the app
   - ✅ No crash on startup

2. **WorkManager Initialization**
   - ✅ No "WorkManager is already initialized" error
   - ✅ WorkManager initializes automatically
   - ✅ Logs show successful initialization

3. **UI Display**
   - ✅ Main screen displays
   - ✅ UI elements are visible
   - ✅ No layout errors
   - ✅ Ambient mode works

4. **Background Services**
   - ✅ WearPlaybackService can start
   - ✅ Sync worker can be scheduled
   - ✅ No service crashes

### Expected Log Output

**Good startup logs:**
```
I ActivityManager: Start proc de.danoeh.antennapod.play
I WearApplication: Initializing AntennaPod Wear OS
D EventBus: Installing default EventBus
I WorkManager: Initializing WorkManager with default configuration
I WearApplication: WearApplication initialized successfully
I MainActivity: onCreate called
```

**Bad startup logs (should NOT see):**
```
E AndroidRuntime: FATAL EXCEPTION: main
E AndroidRuntime: java.lang.IllegalStateException: WorkManager is already initialized
E AndroidRuntime: Caused by: ...
```

## Troubleshooting

### Issue: Emulator won't start

**Solution:**
```bash
# Check if emulator is running
adb devices

# Kill all emulator processes
adb kill-server
adb start-server

# Try starting with more verbose output
emulator -avd Wear_OS_Test -verbose
```

### Issue: APK won't install

**Solution:**
```bash
# Check connection
adb devices

# If "unauthorized", check watch for authorization prompt

# If "offline", restart ADB
adb kill-server
adb start-server

# Uninstall first, then install
adb uninstall de.danoeh.antennapod.play
adb install wear/build/outputs/apk/play/debug/wear-play-debug.apk
```

### Issue: App crashes on startup

**Solution:**
```bash
# Get crash logs
adb logcat -d | grep -A 50 AndroidRuntime

# Check for specific errors
adb logcat | grep -i "workmanager\|exception\|error"

# Get app-specific logs
adb logcat | grep "de.danoeh.antennapod"
```

### Issue: Can't find app in launcher

**Solution:**
```bash
# Verify installation
adb shell pm list packages | grep antennapod

# Launch manually
adb shell am start -n de.danoeh.antennapod.play/de.danoeh.antennapod.wear.ui.MainActivity

# Check if activity is registered
adb shell dumpsys package de.danoeh.antennapod.play | grep MainActivity
```

## Automated Testing Script

Save as `test_wear_startup.sh`:

```bash
#!/bin/bash
set -e

echo "🔨 Building Wear OS APK..."
./gradlew :wear:assemblePlayDebug

echo "📱 Checking emulator..."
if ! adb devices | grep -q "emulator"; then
    echo "❌ No emulator running. Please start emulator first."
    exit 1
fi

echo "🗑️  Uninstalling old version..."
adb uninstall de.danoeh.antennapod.play || true

echo "📦 Installing APK..."
adb install wear/build/outputs/apk/play/debug/wear-play-debug.apk

echo "🧹 Clearing logs..."
adb logcat -c

echo "🚀 Launching app..."
adb shell am start -n de.danoeh.antennapod.play/de.danoeh.antennapod.wear.ui.MainActivity

echo "📝 Monitoring logs (Ctrl+C to stop)..."
echo "Watching for startup and WorkManager initialization..."
adb logcat | grep -i "antennapod\|workmanager"
```

Make executable: `chmod +x test_wear_startup.sh`

Run: `./test_wear_startup.sh`

## Continuous Testing

For development, use this workflow:

1. **Make code changes**
2. **Build**: `./gradlew :wear:assemblePlayDebug`
3. **Install**: `adb install -r wear/build/outputs/apk/play/debug/wear-play-debug.apk`
4. **Test**: Launch from emulator
5. **Monitor**: `adb logcat | grep -i antennapod`
6. **Repeat**

## Performance Testing

Check app performance:

```bash
# Memory usage
adb shell dumpsys meminfo de.danoeh.antennapod.play

# CPU usage
adb shell top | grep antennapod

# Battery usage (on physical device)
adb shell dumpsys batterystats de.danoeh.antennapod.play
```

## Testing on Physical Device

Same steps work for physical Pixel Watch:

1. **Enable Developer Options** on watch
2. **Enable ADB Debugging**
3. **Connect via Bluetooth** or **WiFi**
4. Follow same install/test steps

## Verifying WorkManager Fix

Specifically for the WorkManager automatic initialization:

```bash
# 1. Install app
adb install -r wear/build/outputs/apk/play/debug/wear-play-debug.apk

# 2. Watch logs for WorkManager
adb logcat -c && adb logcat | grep -i workmanager

# 3. Launch app
adb shell am start -n de.danoeh.antennapod.play/de.danoeh.antennapod.wear.ui.MainActivity

# 4. Verify you see:
# - WorkManager initialization (automatic)
# - NO "already initialized" error
# - App starts successfully
```

## Additional Resources

- [Android Emulator Documentation](https://developer.android.com/studio/run/emulator)
- [Wear OS Emulator Guide](https://developer.android.com/training/wearables/get-started/creating)
- [ADB Documentation](https://developer.android.com/studio/command-line/adb)
- [WorkManager Testing](https://developer.android.com/topic/libraries/architecture/workmanager/how-to/testing)

## Success Criteria

The app passes emulator testing if:

- ✅ APK installs without errors
- ✅ App launches without crashing
- ✅ No WorkManager initialization errors in logs
- ✅ UI displays correctly on round and square screens
- ✅ Background sync can be scheduled
- ✅ Ambient mode transitions work
- ✅ No memory leaks or ANRs

## Next Steps

After successful emulator testing:

1. Test on physical Pixel Watch
2. Test sync functionality end-to-end
3. Test media playback
4. Perform battery life testing
5. Test various Wear OS versions

---

**Last Updated**: 2026-02-13 (After WorkManager automatic initialization change)
