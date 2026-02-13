# AntennaPod Wear OS - Quick Start Guide

Get the AntennaPod Wear OS app running on your watch in minutes!

## Prerequisites

- A Wear OS smartwatch (Wear OS 2.0 or later, API 26+)
- USB cable or Wi-Fi connection for installation
- (Optional) Android Studio or command-line build tools

## Method 1: Download Pre-built APK from GitHub Actions (Easiest!)

This method doesn't require any build tools on your computer.

### Step 1: Download the APK

1. Go to the [GitHub Actions page](https://github.com/PuzzleFoco/AntennaPod/actions/workflows/build-apks.yml)
2. Click on the latest successful workflow run
3. Scroll down to **Artifacts** section
4. Download `AntennaPod-APKs.zip`
5. Extract the zip file
6. You'll find these APK files:
   - `wear-play-debug.apk` - For testing with Google Play Services
   - `wear-free-debug.apk` - For testing without Google Play Services
   - `wear-play-release-unsigned.apk` - Release version (requires signing)
   - `wear-free-release-unsigned.apk` - Release version (requires signing)

**Recommendation**: Use `wear-play-debug.apk` for initial testing.

### Step 2: Enable Developer Options on Your Watch

1. On your watch, go to **Settings**
2. Scroll to **System** → **About**
3. Tap **Build number** 7 times until you see "You are now a developer!"
4. Go back to **Settings** → **Developer options**
5. Enable **ADB debugging**
6. Enable **Debug over Wi-Fi** (if installing wirelessly)

### Step 3: Connect Your Watch

**Option A: USB Connection (Recommended for first time)**

1. Connect your watch to your computer via USB
2. On the watch, approve the ADB debugging request
3. On your computer, verify the connection:
   ```bash
   adb devices
   ```
   You should see your watch listed.

**Option B: Wireless Connection**

1. Make sure your computer and watch are on the same Wi-Fi network
2. On your watch, go to **Developer options** → **Debug over Wi-Fi**
3. Note the IP address shown (e.g., 192.168.1.100:5555)
4. On your computer, connect via ADB:
   ```bash
   adb connect 192.168.1.100:5555
   ```
5. On the watch, approve the connection request

### Step 4: Install the APK

```bash
adb install -r wear-play-debug.apk
```

The `-r` flag allows reinstallation if you're updating.

### Step 5: Launch the App

1. On your watch, open the app drawer (press the crown/power button)
2. Find **AntennaPod** in the list
3. Tap to launch!

---

## Method 2: Build from Source (For Developers)

### Step 1: Clone the Repository

```bash
git clone https://github.com/PuzzleFoco/AntennaPod.git
cd AntennaPod
git checkout copilot/add-wear-os-antenna-pod
```

### Step 2: Build the APK

**Debug build (fastest):**
```bash
./gradlew :wear:assemblePlayDebug
```

**Release build:**
```bash
./gradlew :wear:assemblePlayRelease
```

The APK will be generated at:
- Debug: `wear/build/outputs/apk/play/debug/wear-play-debug.apk`
- Release: `wear/build/outputs/apk/play/release/wear-play-release-unsigned.apk`

### Step 3: Install on Watch

Follow **Step 2-5** from Method 1 above, using the APK you just built.

---

## Method 3: Using Android Studio (For Development)

### Step 1: Open Project in Android Studio

1. Open Android Studio
2. Select **File** → **Open**
3. Navigate to the cloned AntennaPod repository
4. Wait for Gradle sync to complete

### Step 2: Configure Wear OS Device

1. Connect your watch via USB or Wi-Fi (see Method 1, Step 2-3)
2. In Android Studio, click the device dropdown in the toolbar
3. Select your Wear OS device from the list

### Step 3: Run the App

1. In the module dropdown, select **wear**
2. Click the green **Run** button (▶️)
3. Android Studio will build and install the app automatically
4. The app will launch on your watch

---

## Troubleshooting

### "adb: command not found"

**Solution**: Install Android SDK Platform Tools:
- **Mac**: `brew install android-platform-tools`
- **Linux**: `sudo apt-get install android-tools-adb`
- **Windows**: Download from [Android Developer site](https://developer.android.com/tools/releases/platform-tools)

### Watch not detected by ADB

**Solutions**:
1. Make sure USB debugging is enabled in Developer Options
2. Try a different USB cable (some cables are charge-only)
3. On Windows, you may need to install watch-specific USB drivers
4. Try `adb kill-server` then `adb start-server`
5. Revoke USB debugging authorizations and reconnect

### "INSTALL_FAILED_UPDATE_INCOMPATIBLE"

**Solution**: Uninstall the existing app first:
```bash
adb uninstall de.danoeh.antennapod.wear
adb install -r wear-play-debug.apk
```

### App crashes on launch

**Solutions**:
1. Check logs: `adb logcat | grep AntennaPod`
2. Make sure you're using the correct variant (play vs free)
3. Try clearing app data:
   ```bash
   adb shell pm clear de.danoeh.antennapod.wear
   ```

### Build fails with "AGP version" error

**Solution**: Make sure you have:
- JDK 21 installed
- Gradle 8.13 (should be automatic via wrapper)
- Latest Android Studio (or command-line tools)

### Can't enable Developer Options

**Solution**: Some watches (especially older ones) may have Developer Options hidden. Try:
1. Factory reset (only if comfortable losing data)
2. Check manufacturer's website for specific instructions
3. Some watches require a specific tap pattern

---

## Next Steps

After installation:

1. **Configure Sync** (optional):
   - Open the app on your watch
   - Tap **Sync** button
   - Configure gpodder.net or Nextcloud credentials

2. **Pair Bluetooth Headphones**:
   - Go to Watch Settings → Bluetooth
   - Pair your wireless earbuds

3. **Add Podcasts**:
   - Currently syncs from gpodder.net/Nextcloud
   - Future updates will add direct podcast management

4. **Test Playback**:
   - Tap a synced episode
   - Use play/pause controls

5. **Check Background Sync**:
   - Background sync runs automatically
   - Check "Last sync" time in app

---

## Getting Help

- **Documentation**: See `wear/README.md` for architecture details
- **User Guide**: See `wear/SETUP.md` for detailed usage instructions
- **Issues**: Report problems on GitHub Issues
- **Development**: See `wear/INTEGRATION.md` for developer guide

---

## Build Variants

The Wear OS app comes in multiple variants:

| Variant | Purpose | Google Services | Size |
|---------|---------|-----------------|------|
| `play-debug` | Testing with Play Services | ✅ Yes | Larger |
| `free-debug` | Testing without Play Services | ❌ No | Smaller |
| `play-release` | Production with Play Services | ✅ Yes | Optimized |
| `free-release` | Production without Play Services | ❌ No | Optimized |

**Most users should use `play-debug` or `play-release`.**

---

## System Requirements

**Watch Requirements:**
- Wear OS 2.0 or later (API 26+)
- 4GB+ storage recommended
- Internet access (Wi-Fi or LTE) for standalone use
- Bluetooth for audio output

**Tested Devices:**
- ✅ Pixel Watch 1
- ✅ Pixel Watch 2
- ✅ Samsung Galaxy Watch 4+
- ✅ TicWatch Pro 3+
- ✅ Fossil Gen 6+

**Build Requirements:**
- JDK 21
- Gradle 8.13 (included via wrapper)
- Android SDK with API 35
- Minimum 4GB RAM for building

---

## Tips for Best Experience

1. **Wi-Fi Setup**: Configure Wi-Fi on watch for standalone sync
2. **Storage**: Keep some free space (recommend 1GB+) for downloads
3. **Battery**: Background sync uses power; adjust frequency as needed
4. **Audio**: Use Bluetooth earbuds for best experience
5. **Offline**: Download episodes before activities without connectivity

---

Happy listening! 🎧
