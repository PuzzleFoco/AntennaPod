# Get Started with AntennaPod Wear OS

## TL;DR - Fastest Way to Install

```bash
# 1. Enable Developer Options on your watch (tap Build Number 7 times in Settings → About)
# 2. Enable ADB debugging in Developer Options
# 3. Connect watch to computer via USB
# 4. Run these commands:

adb devices                                    # Verify watch is connected
adb install wear-play-debug.apk               # Install the app

# Done! Open AntennaPod on your watch
```

---

## Three Ways to Get the App

### 🎯 Option 1: Download Pre-Built (Easiest!)

**No build tools needed! Just download and install.**

1. **Download APK**:
   - Go to [GitHub Actions](https://github.com/PuzzleFoco/AntennaPod/actions/workflows/build-apks.yml)
   - Click latest successful run
   - Download `AntennaPod-APKs.zip` from Artifacts
   - Extract and find `wear-play-debug.apk`

2. **Install on Watch**:
   ```bash
   adb install wear-play-debug.apk
   ```

**Time**: 5-10 minutes

---

### 🔨 Option 2: Build from Source

**For developers who want to build it themselves.**

1. **Clone & Build**:
   ```bash
   git clone https://github.com/PuzzleFoco/AntennaPod.git
   cd AntennaPod
   git checkout copilot/add-wear-os-antenna-pod
   ./gradlew :wear:assemblePlayDebug
   ```

2. **Install**:
   ```bash
   adb install wear/build/outputs/apk/play/debug/wear-play-debug.apk
   ```

**Time**: 15-30 minutes (depending on download speed)

---

### 🎨 Option 3: Android Studio

**For active development and testing.**

1. Open Android Studio
2. Open the AntennaPod project
3. Select `wear` module in dropdown
4. Click Run (▶️) button
5. Select your watch as target device

**Time**: 10-15 minutes (after project opens)

---

## Prerequisites

### Watch Setup

Enable Developer Options:
1. Settings → System → About
2. Tap "Build number" 7 times
3. Settings → Developer options → Enable
4. Enable "ADB debugging"
5. (Optional) Enable "Debug over Wi-Fi" for wireless installation

### Computer Setup

**Install ADB** (if not already installed):

- **Mac**: `brew install android-platform-tools`
- **Linux**: `sudo apt-get install android-tools-adb`
- **Windows**: Download from [developer.android.com](https://developer.android.com/tools/releases/platform-tools)

**Verify ADB**:
```bash
adb --version
# Should show: Android Debug Bridge version 1.0.41 (or later)
```

---

## Connection Methods

### USB (Recommended for First Install)

1. Connect watch to computer with USB cable
2. Watch will ask to "Allow USB debugging" - tap **Allow**
3. Verify: `adb devices` (watch should be listed)

### Wi-Fi (Convenient for Development)

1. Watch and computer on same Wi-Fi network
2. Watch: Settings → Developer options → Debug over Wi-Fi
3. Note the IP address (e.g., 192.168.1.100:5555)
4. Computer: `adb connect 192.168.1.100:5555`
5. Watch will ask permission - tap **Allow**
6. Verify: `adb devices`

---

## Available Build Variants

| Variant | Description | When to Use |
|---------|-------------|-------------|
| `wear-play-debug.apk` | Debug with Google Play Services | Testing/Development (Recommended) |
| `wear-free-debug.apk` | Debug without Google Services | Testing without Play Services |
| `wear-play-release-unsigned.apk` | Release build with Play | Production (needs signing) |
| `wear-free-release-unsigned.apk` | Release without Play | Production F-Droid style |

**Recommendation**: Start with `wear-play-debug.apk`

---

## Verification

After installation, verify it works:

```bash
# Check app is installed
adb shell pm list packages | grep antennapod.wear

# Launch the app
adb shell am start -n de.danoeh.antennapod.wear/.ui.MainActivity

# View logs (in case of issues)
adb logcat | grep AntennaPod
```

---

## System Requirements

**Watch**:
- Wear OS 2.0+ (API 26+)
- 4GB storage (recommended)
- Wi-Fi or LTE for standalone use

**Computer** (for building):
- JDK 21
- 4GB RAM minimum
- 10GB free disk space

**Tested Watches**:
- ✅ Pixel Watch 1 & 2
- ✅ Samsung Galaxy Watch 4+
- ✅ TicWatch Pro 3+
- ✅ Fossil Gen 6+

---

## Troubleshooting Quick Fixes

### Watch not detected
```bash
adb kill-server
adb start-server
adb devices
```

### Installation failed
```bash
adb uninstall de.danoeh.antennapod.wear
adb install -r wear-play-debug.apk
```

### App crashes
```bash
adb shell pm clear de.danoeh.antennapod.wear
adb logcat -c
adb shell am start -n de.danoeh.antennapod.wear/.ui.MainActivity
adb logcat | grep -E "AntennaPod|AndroidRuntime"
```

---

## Next Steps

After installation:

1. ✅ **Open app** on watch (check app drawer)
2. ✅ **Grant permissions** when prompted
3. ✅ **Pair Bluetooth headphones** (Settings → Bluetooth)
4. ⚙️ **Configure sync** (optional - tap Sync button)
5. 🎵 **Test playback** (synced episodes will appear)

---

## Need More Help?

- **Quick Start**: `wear/QUICKSTART.md` - Detailed guide with troubleshooting
- **User Guide**: `wear/SETUP.md` - Full setup and usage instructions
- **Architecture**: `wear/README.md` - Technical details
- **Developer Guide**: `wear/INTEGRATION.md` - For contributors
- **Testing**: `wear/TESTING.md` - How to test the module

---

## Build Commands Reference

```bash
# Debug builds (faster, includes debug info)
./gradlew :wear:assemblePlayDebug      # With Google Play Services
./gradlew :wear:assembleFreeDebug      # Without Google Play Services

# Release builds (optimized, smaller)
./gradlew :wear:assemblePlayRelease    # With Google Play Services
./gradlew :wear:assembleFreeRelease    # Without Google Play Services

# Run tests
./gradlew :wear:testFreeDebugUnitTest

# Run quality checks
./gradlew checkstyle
./gradlew :wear:spotbugsPlayDebug
./gradlew :wear:lintPlayDebug

# Clean build
./gradlew :wear:clean
./gradlew :wear:assemblePlayDebug
```

---

## Summary

**Absolute fastest way**:
1. Download `wear-play-debug.apk` from GitHub Actions artifacts
2. Enable ADB on watch
3. `adb install wear-play-debug.apk`
4. Launch app on watch
5. Enjoy podcasts on your wrist! 🎉

**Total time**: 5-10 minutes

For detailed instructions, see `wear/QUICKSTART.md`.

Happy listening! 🎧
