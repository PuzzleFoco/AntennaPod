# GitHub Actions Workflow Guide

## Build and Release APKs Workflow

This guide explains how the automated build workflow works and how to download Wear OS APKs.

### Workflow Overview

The **"Build and Release APKs"** workflow automatically builds AntennaPod APKs including the Wear OS app.

**Location**: `.github/workflows/build-apks.yml`

### When Does It Run?

The workflow triggers automatically on:

1. **Push to `develop` branch** → Builds debug APKs
2. **Push to `master` branch** → Builds all APKs (debug + release)
3. **Version tags (v*)** → Builds release APKs + creates GitHub Release
4. **Manual trigger** → You choose which variant to build

### What Gets Built?

| Trigger | App APKs | Wear OS APKs |
|---------|----------|--------------|
| develop | app-play-debug.apk | wear-play-debug.apk |
| master | All 3 variants | All 3 variants |
| v* tag | play-release, free-release | play-release, free-release |
| Manual | Depends on selection | Depends on selection |

**Total APKs available**:
- Debug builds: 2 APKs (1 app + 1 wear)
- Release builds: 4 APKs (2 app + 2 wear)
- All builds: 6 APKs (3 app + 3 wear)

### How to Download Wear OS APKs

#### Method 1: Download from Workflow Runs (Easiest)

1. Go to the **Actions** tab in GitHub
2. Click on **"Build and Release APKs"** workflow
3. Select the latest successful run (green checkmark ✅)
4. Scroll down to **Artifacts** section
5. Download **"AntennaPod-APKs"** artifact (it's a zip file)
6. Extract the zip file
7. Find your Wear OS APK:
   - `wear-play-debug.apk` (for testing)
   - `wear-play-release-unsigned.apk` (production)
   - `wear-free-release-unsigned.apk` (F-Droid variant)

**Time estimate**: 1-2 minutes

#### Method 2: Download from GitHub Releases

If a version tag was pushed (e.g., `v3.11.1`):

1. Go to the **Releases** section
2. Find the latest release
3. Scroll to **Assets**
4. Download the wear APK directly

**Available on**: Version releases only

#### Method 3: Manual Trigger

You can manually trigger a build:

1. Go to **Actions** tab
2. Click **"Build and Release APKs"**
3. Click **"Run workflow"** button
4. Select branch and build variant:
   - **debug**: Fast build, 2 APKs (recommended for testing)
   - **release**: Production builds, 4 APKs
   - **all**: Everything, 6 APKs (takes longest)
5. Click **"Run workflow"**
6. Wait for completion (~10-20 minutes)
7. Download artifact as described in Method 1

### Workflow Features

#### Smart Build Selection

The workflow automatically determines what to build:

```bash
# On develop branch
./gradlew :app:assemblePlayDebug :wear:assemblePlayDebug

# On master branch
./gradlew :app:assemblePlayDebug :app:assemblePlayRelease :app:assembleFreeRelease \
          :wear:assemblePlayDebug :wear:assemblePlayRelease :wear:assembleFreeRelease

# On version tags
./gradlew :app:assemblePlayRelease :app:assembleFreeRelease \
          :wear:assemblePlayRelease :wear:assembleFreeRelease
```

#### Error Handling

The workflow includes robust error handling:
- ✅ Verifies APK files are generated
- ✅ Logs all collection steps
- ✅ Fails explicitly if APKs are missing
- ✅ Shows file sizes in build summary

#### Build Summary

After each build, check the workflow run summary for:
- List of all built APKs
- File sizes
- Links to download

### Troubleshooting

#### "No artifacts found"

**Problem**: The workflow completed but no artifacts are available.

**Solutions**:
1. Check the workflow logs for build errors
2. Look for "Collect APK files" step - did it find APKs?
3. Verify the wear module built successfully
4. Check if the build variant was correct for the trigger

#### "Workflow failed at build step"

**Problem**: The Gradle build failed.

**Solutions**:
1. Check the build logs for compilation errors
2. Verify all dependencies are available
3. Check if checkstyle/spotbugs passed
4. Ensure AGP version is compatible (8.6.0)

#### "APK won't install on watch"

**Problem**: Downloaded APK fails to install.

**Solutions**:
1. Verify you downloaded the correct variant (debug vs release)
2. Check minimum SDK (requires Wear OS 2.0+, API 26+)
3. Enable "Install unknown apps" on your watch
4. Use `adb install wear-play-debug.apk` for detailed error messages

### Build Times

Typical build durations:

| Build Type | Duration | APKs Generated |
|------------|----------|----------------|
| Debug only | 8-12 min | 2 APKs |
| Release only | 12-18 min | 4 APKs |
| All variants | 18-25 min | 6 APKs |

### GitHub Free Plan Optimizations

The workflow is optimized for GitHub free plan:

1. **Single artifact upload**: All APKs in one download (saves storage)
2. **Conditional builds**: Only builds what's needed (saves minutes)
3. **30-day retention**: Balances availability and storage limits
4. **45-minute timeout**: Prevents hanging builds
5. **Gradle caching**: Speeds up subsequent builds

### Advanced Usage

#### Local Testing

To test the workflow logic locally:

```bash
# Test debug build
./gradlew :app:assemblePlayDebug :wear:assemblePlayDebug

# Test release build
./gradlew :app:assemblePlayRelease :wear:assemblePlayRelease

# Collect APKs like the workflow does
mkdir -p apk-output
find app/build/outputs/apk -name "*.apk" -exec cp {} apk-output/ \;
find wear/build/outputs/apk -name "*.apk" -exec cp {} apk-output/ \;
ls -lh apk-output/
```

#### Workflow Modification

If you need to modify the workflow:

1. Edit `.github/workflows/build-apks.yml`
2. Test changes on a feature branch first
3. Verify the "Determine build variant" logic
4. Check the conditional steps work correctly
5. Ensure APK collection handles all cases

### Related Documentation

- **Quick Start**: `wear/QUICKSTART.md` - Building from source
- **Get Started**: `wear/GET_STARTED.md` - Quick reference
- **Setup Guide**: `wear/SETUP.md` - Installation instructions
- **Testing**: `wear/TESTING.md` - Test procedures

### Support

If you encounter issues with the workflow:

1. Check the workflow run logs
2. Review this guide
3. Check GitHub Actions status page
4. Open an issue with workflow run URL

---

**Last Updated**: 2026-02-13  
**Workflow Version**: 1.1 (Fixed variant selection and error handling)
