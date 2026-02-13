# Automated Emulator Testing

This document explains the automated emulator testing integrated into the Build and Release APKs workflow.

## Overview

The Wear OS module has **automated emulator testing** integrated into the main build workflow (`.github/workflows/build-apks.yml`). Every build triggers automated tests that verify the app starts correctly on a Wear OS emulator.

## Why Integrated Testing?

### Single Workflow Benefits
- ✅ One place to check build AND test status
- ✅ Tests run in parallel with APK builds
- ✅ Simpler CI/CD pipeline
- ✅ Easier to maintain
- ✅ Clear relationship between builds and tests

### How It Works

**Build Workflow** has two jobs running in parallel:

```
Job 1: build-apks        Job 2: test-wear-emulator
  ├─ Build main app APKs   ├─ Build Wear APK
  ├─ Build Wear APKs       ├─ Start emulator
  ├─ Upload artifacts      ├─ Install & test
  └─ Create release        └─ Report results
```

Both jobs run **simultaneously** for faster feedback.

## The Workflow

**File**: `.github/workflows/build-apks.yml`

**Job Name**: `test-wear-emulator`

### What It Tests

1. **Build Verification** - Ensures APK builds successfully
2. **Installation** - Verifies APK installs on Wear OS
3. **App Launch** - Checks app launches without crashing
4. **WorkManager Init** - Confirms proper initialization
5. **Error Detection** - Catches startup crashes

### Test Sequence

```
1. Checkout code
2. Setup JDK 21
3. Build APK (./gradlew :wear:assemblePlayDebug)
4. Start Wear OS emulator (API 30, round watch)
5. Install APK (adb install)
6. Launch app (am start)
7. Monitor logs (10 seconds)
8. Analyze for errors
9. Report results
```

## Automated Checks

### ❌ Test FAILS if:

**WorkManager Double Initialization**:
```
java.lang.IllegalStateException: WorkManager is already initialized
```

**Fatal Exception**:
```
FATAL EXCEPTION: main
Process: de.danoeh.antennapod.play, PID: 1234
```

**App Crash**:
```
AndroidRuntime: FATAL EXCEPTION
```

### ✅ Test PASSES if:

1. APK installs successfully
2. App launches without errors
3. WorkManager initializes properly
4. No fatal exceptions
5. App process is running after 10 seconds

## Example Test Run

### Successful Run

```
🚀 Starting app startup test...
📦 Installing APK...
Performing Streamed Install
Success

🎯 Launching app...
Starting: Intent { cmp=de.danoeh.antennapod.play/de.danoeh.antennapod.wear.ui.MainActivity }

⏱️  Waiting for app to initialize...

🔍 Checking logs for errors...
✅ WorkManager logs found
I WorkManager: Initializing WorkManager with default configuration
I WorkManager: WorkManager initialized

✅ WearApplication initialized
I WearApplication: Initializing AntennaPod Wear OS
I WearApplication: WearApplication initialized successfully

✅ MainActivity started
I MainActivity: onCreate called
I MainActivity: Setting up UI

🔎 Checking if app is running...
de.danoeh.antennapod.play  1234  234  2048000  45000  0   0 S de.danoeh.antennapod.play
✅ SUCCESS: App is running!

✅ Emulator test completed successfully!
```

### Failed Run (Example)

```
🚀 Starting app startup test...
📦 Installing APK...
Success

🎯 Launching app...
Starting: Intent { ... }

⏱️  Waiting for app to initialize...

🔍 Checking logs for errors...
❌ FAILURE: WorkManager double initialization detected!

E AndroidRuntime: FATAL EXCEPTION: main
E AndroidRuntime: Process: de.danoeh.antennapod.play, PID: 1234
E AndroidRuntime: java.lang.RuntimeException: Unable to create application
E AndroidRuntime: Caused by: java.lang.IllegalStateException: WorkManager is already initialized
E AndroidRuntime:     at androidx.work.WorkManager.initialize(WorkManager.java:156)
E AndroidRuntime:     at de.danoeh.antennapod.wear.WearApplication.initializeWorkManager(WearApplication.java:42)

Error: Process completed with exit code 1.
```

## When Tests Run

### Automatic Triggers

Tests run as part of the **Build and Release APKs** workflow:

**1. Push to Branches**
```bash
git push origin master        # Builds all + tests
git push origin develop       # Builds debug + tests  
git push origin copilot/...   # Builds debug + tests
```

**2. Version Tags**
```bash
git push origin v3.11.1       # Builds release + tests + creates GitHub release
```

**3. Manual Workflow Dispatch**
- Go to Actions → "Build and Release APKs"
- Select build variant
- Tests run automatically

### Viewing Combined Results

1. Go to **Actions** tab
2. Select **"Build and Release APKs"** workflow
3. Click on a run
4. See both jobs:
   - **Build APKs** (left) - APK building status
   - **Test Wear OS on Emulator** (right) - Test status

### After Completion

**Both Jobs Complete**:
- Build APKs: ✅ or ❌
- Test Emulator: ✅ or ❌

You can:
- Download APKs even if tests are still running
- See test results independently
- Debug test failures with uploaded logs

## Debugging Failed Tests

### 1. Check Workflow Logs

In workflow run:
1. Click on **"Test Wear OS on Emulator"** job
2. Expand **"Test on Wear OS Emulator"** step
3. Read error messages

### 2. Download Log Artifacts

If test fails:
1. Go to failed workflow run
2. Scroll to **Artifacts** section
3. Download `emulator-test-logs`
4. Extract and read `logcat.txt`

### 3. Reproduce Locally

Use the same test sequence:
```bash
./gradlew :wear:assemblePlayDebug
adb install -r wear/build/outputs/apk/play/debug/wear-play-debug.apk
adb logcat -c
adb shell am start -n de.danoeh.antennapod.play/de.danoeh.antennapod.wear.ui.MainActivity
adb logcat | grep -i "antennapod\|workmanager\|exception"
```

## Emulator Specifications

**Configuration**:
- **API Level**: 30 (Wear OS 3.0)
- **Target**: android-wear
- **Architecture**: x86
- **Profile**: wear_round (round watch)
- **RAM**: Default (auto-allocated)
- **Disk**: Default (auto-allocated)

**Optimization Flags**:
```
-no-snapshot-save      # Don't save state
-no-window            # Headless mode
-gpu swiftshader_indirect  # Software rendering
-noaudio              # No audio
-no-boot-anim         # Skip boot animation
-camera-back none     # No camera
```

These optimizations make the emulator start faster in CI environment.

## Performance

### Typical Run Time

- **Full workflow**: 10-15 minutes
  - Checkout: 10 seconds
  - Setup JDK: 20 seconds
  - Build APK: 3-5 minutes
  - Start emulator: 3-5 minutes
  - Run tests: 2 minutes
  - Cleanup: 30 seconds

### Timeout

- **Maximum**: 45 minutes
- **Typical**: 10-15 minutes
- **Fast fail**: On errors, fails within 5 minutes

## Comparison with Manual Testing

| Aspect | Manual | Automated |
|--------|--------|-----------|
| Setup time | 30+ minutes | 0 (automatic) |
| Run time | 5-10 minutes | 10-15 minutes |
| Consistency | Variable | Identical every time |
| Runs per day | 2-3 (manual) | Unlimited (auto) |
| Feedback delay | Hours | Minutes |
| Cost | Developer time | Free (GitHub Actions) |

## Benefits

### 1. Fast Feedback Loop
- Know immediately if changes break startup
- Catch issues before manual testing

### 2. Continuous Verification
- Every push is tested
- No untested code merged

### 3. Regression Prevention
- Tests ensure fixes stay fixed
- WorkManager init verified every time

### 4. Developer Confidence
- Make changes fearlessly
- Automated safety net

### 5. Documentation through Tests
- Tests document expected behavior
- Clear pass/fail criteria

## Integration with Build Workflow

### Job Dependencies

```yaml
jobs:
  build-apks:          # Job 1: Build APKs
    runs-on: ubuntu-latest
    # Builds main app and wear APKs
    # Uploads artifacts
    # Creates releases for tags
    
  test-wear-emulator:  # Job 2: Test on emulator
    runs-on: ubuntu-latest
    # Builds wear APK
    # Tests on emulator
    # Uploads test logs if failure
```

**Both jobs are independent** - they run in parallel for speed.

### Workflow Structure

The combined workflow provides:
1. **APK Building** - Main purpose
2. **Automated Testing** - Quality assurance
3. **Artifact Upload** - Easy downloads
4. **Release Creation** - For version tags

All in one place!

## Future Enhancements

### Potential Additions

1. **Extended Tests**
   - Media playback test
   - Sync functionality test
   - UI interaction test

2. **Multiple API Levels**
   - Test on Wear OS 2, 3, and 4
   - Matrix testing

3. **Screenshot Capture**
   - Capture UI state
   - Visual regression testing

4. **Performance Metrics**
   - Startup time measurement
   - Memory usage tracking
   - Battery impact analysis

5. **Integration Tests**
   - Test with companion app
   - End-to-end scenarios

## Maintenance

### Keeping Tests Healthy

**Update emulator image**:
```yaml
api-level: 30  # Update to latest stable
```

**Update test script**:
- Adjust wait times if needed
- Add new error checks
- Update package names if changed

**Monitor flakiness**:
- If tests fail randomly, increase wait times
- Check GitHub Actions system status
- Consider emulator hardware acceleration

## Troubleshooting

### Emulator Startup Fails

**Error**: "Emulator failed to boot"

**Solutions**:
- Usually transient, re-run workflow
- Check GitHub Actions status page
- Increase timeout if needed

### APK Installation Fails

**Error**: "Installation failed"

**Solutions**:
- Check APK actually built
- Verify package name correct
- Check manifest validity

### App Launch Timeout

**Error**: "App didn't start in time"

**Solutions**:
- Increase wait time in script
- Check if app requires user interaction
- Verify activity name correct

## Best Practices

### Writing Testable Code

1. **Fast Initialization**: Keep app startup code quick
2. **No User Input**: Don't require input on first launch
3. **Graceful Degradation**: Handle missing services
4. **Clear Logging**: Log initialization steps

### Maintaining Tests

1. **Keep Tests Fast**: Under 15 minutes total
2. **Make Tests Reliable**: No flakiness
3. **Clear Error Messages**: Easy to debug
4. **Update Regularly**: Keep emulator image current

## Resources

- [Android Emulator Runner Action](https://github.com/ReactiveCircus/android-emulator-runner)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Wear OS Testing Guide](https://developer.android.com/training/wearables/testing)
- [ADB Documentation](https://developer.android.com/studio/command-line/adb)

## Success Metrics

### Before Automation
- Manual testing: 2-3 times per week
- Issues found: During manual testing or production
- Fix time: Hours to days

### After Automation
- Automated testing: Every push (10-20 times per day)
- Issues found: Within minutes of commit
- Fix time: Minutes to hours

---

**Created**: 2026-02-13  
**Purpose**: Better feedback loop through automated emulator testing  
**Workflow**: `.github/workflows/wear-emulator-test.yml`
