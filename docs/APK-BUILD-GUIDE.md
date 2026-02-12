# APK Build and Release Workflow

This document describes how to build and distribute installable APK files for AntennaPod.

## Automatic Builds

The repository is now configured with a GitHub Actions workflow that automatically builds APK files.

### When are builds triggered?

1. **On every push to master/develop**: APKs are automatically built
2. **On pull requests**: APKs are built (for testing)
3. **On version tags**: APKs are built and published as GitHub Releases
4. **Manually**: Workflow can be manually started via GitHub Actions

## Built APK Variants

The workflow creates the following APK files:

### Main App (AntennaPod):
1. **AntennaPod-play-debug.apk** - Play Store variant (Debug)
2. **AntennaPod-play-release.apk** - Play Store variant (Release)
3. **AntennaPod-free-release.apk** - F-Droid variant (Release)

### Wear OS App:
4. **AntennaPod-wear-debug.apk** - Wear OS App (Debug)
5. **AntennaPod-wear-release.apk** - Wear OS App (Release)

## Downloading APKs

### From GitHub Actions

1. Go to the **Actions** tab in the repository
2. Select a workflow run
3. Scroll to **Artifacts** at the bottom of the page
4. Download the desired APK

### From GitHub Releases (for tags)

1. Go to **Releases** in the repository
2. Select the desired version
3. Download the APK from **Assets**

## Starting a Manual Build

1. Go to the **Actions** tab
2. Select **"Build and Release APKs"** workflow
3. Click **"Run workflow"**
4. Select the branch (master/develop)
5. Click **"Run workflow"**
6. Wait for the build to complete
7. Download the APKs from artifacts

## Creating a Release

To create a new release with APKs:

```bash
# Create version tag
git tag -a v3.11.1 -m "Version 3.11.1"
git push origin v3.11.1
```

The workflow will automatically:
1. Build all APKs
2. Create a GitHub Release
3. Attach the APKs to the release
4. Generate release notes

## Installation on Android Devices

### Main App Installation

1. Download `AntennaPod-play-debug.apk` or `AntennaPod-free-release.apk`
2. Enable "Installation from unknown sources" in Android settings
3. Open the APK file and install it

### Wear OS App Installation

**Option 1: Via ADB**
```bash
adb install AntennaPod-wear-debug.apk
```

**Option 2: Via Wear OS Companion App**
1. Transfer the APK to your phone
2. Install it using a file manager app
3. The app will automatically sync to the watch (if configured)

**Option 3: Directly on the Watch**
1. Enable ADB debugging on the Wear OS watch
2. Connect the watch via ADB (USB or Wi-Fi)
3. Install with `adb install`

## Technical Details

### Android Gradle Plugin (AGP)

- **Version**: 9.0.0 (latest version)
- **Gradle Version**: 8.13
- **Java Version**: 21

### Build Configuration

The workflow:
- Uses Ubuntu Latest as build environment
- Caches Gradle dependencies for faster builds
- Creates temporary release keystores for unsigned builds
- Uploads all APKs as artifacts
- Timeout: 60 minutes

### Signing

**Important**: APKs built in GitHub Actions are:
- Debug builds: Signed with debug keystore
- Release builds: **UNSIGNED** (must be signed before publishing)

For signed release builds, you should:
1. Use a real keystore
2. Configure secrets in GitHub
3. Modify the workflow to use the keystore

## Troubleshooting

### Build fails

1. Check the build logs in GitHub Actions
2. Ensure all dependencies are available
3. Check if AGP 9.0.0 is compatible with all modules

### APK won't install

1. **"App not installed"**: Uninstall old versions first
2. **"Unknown sources"**: Enable installation from unknown sources
3. **"Signature mismatch"**: Old version has different keystore

### Wear OS app doesn't start

1. Check if Wear OS version >= 2.0 (API 26+)
2. Ensure all permissions are granted
3. Check Logcat for errors: `adb logcat | grep AntennaPod`

## Further Links

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android Build Variants](https://developer.android.com/studio/build/build-variants)
- [Wear OS Development](https://developer.android.com/training/wearables)
- [ADB Installation Guide](https://developer.android.com/studio/command-line/adb)
