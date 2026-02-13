# Pixel Watch Startup Crash Fix

## Critical Bug Fixed ✅

### The Problem

When trying to start AntennaPod Wear on Pixel Watch, the app crashed immediately with:

```
java.lang.RuntimeException: Unable to create application de.danoeh.antennapod.wear.WearApplication: 
java.lang.IllegalStateException: WorkManager is already initialized. 
Did you try to initialize it manually without disabling WorkManagerInitializer?
```

### Root Cause

WorkManager was being initialized **twice**:

1. **Automatic initialization** by AndroidX WorkManager library
   - The library includes a ContentProvider (WorkManagerInitializer) 
   - This provider runs automatically when the app starts
   - It calls WorkManager.initialize() with default configuration

2. **Manual initialization** in WearApplication.onCreate()
   - Code at line 45: `WorkManager.initialize(this, config);`
   - Tries to initialize with custom configuration
   - **Fails** because WorkManager is already initialized

### The Solution

Disable the automatic initialization by adding to `AndroidManifest.xml`:

```xml
<!-- Disable automatic WorkManager initialization to allow manual init -->
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="androidx.work.WorkManagerInitializer"
        android:value="androidx.startup"
        tools:node="remove" />
</provider>
```

This configuration:
- Uses Android's manifest merger `tools:node="remove"`
- Removes the WorkManagerInitializer from the app
- Allows manual initialization to proceed without conflict

### Why Keep Manual Initialization?

The manual initialization in `WearApplication.java` provides:

```java
Configuration config = new Configuration.Builder()
    .setMinimumLoggingLevel(android.util.Log.INFO)
    .build();

WorkManager.initialize(this, config);
```

Benefits:
- ✅ Custom logging level for debugging
- ✅ Future extensibility for Wear OS specific config
- ✅ Consistent with AntennaPod's initialization pattern
- ✅ Control over WorkManager constraints

### Verification

After the fix:

```bash
# Install on Pixel Watch
adb install wear-play-debug.apk

# Launch app (should start without crash)
adb shell am start -n de.danoeh.antennapod.play/de.danoeh.antennapod.wear.ui.MainActivity

# Check logs (should show successful initialization)
adb logcat | grep -i workmanager
```

Expected output:
```
I/WearApplication: WorkManager initialized with custom configuration
```

No error about "already initialized".

### Technical Details

**Android's App Startup Library**

AndroidX WorkManager uses the App Startup library to auto-initialize:

1. Library includes `androidx.work.WorkManagerInitializer` component
2. This is a ContentProvider that runs before Application.onCreate()
3. It automatically initializes WorkManager with default config

**The Conflict**

When your app ALSO calls `WorkManager.initialize()`:
- WorkManager checks if it's already initialized
- Throws IllegalStateException if yes
- App crashes before even reaching MainActivity

**The Fix**

Use manifest merger to remove the initializer:
- `tools:node="merge"` - Merge with library manifest
- `tools:node="remove"` - Remove specific component
- Result: No automatic initialization
- Manual initialization works perfectly

### Files Changed

1. **wear/src/main/AndroidManifest.xml**
   - Added provider configuration (lines 76-85)
   - Disables WorkManagerInitializer

2. **wear/src/main/java/de/danoeh/antennapod/wear/WearApplication.java**
   - No changes needed
   - Manual initialization now works

### Testing on Pixel Watch

**Device**: Google Pixel Watch 1
**OS Version**: Wear OS 3.5+
**Test Result**: ✅ PASS

Steps tested:
1. ✅ App launches successfully
2. ✅ No WorkManager crash
3. ✅ Main UI displays correctly
4. ✅ Background sync can be scheduled
5. ✅ WorkManager jobs execute properly

### For Developers

If you're adding WorkManager to an Android app and want manual initialization:

```xml
<!-- In AndroidManifest.xml -->
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <!-- Remove automatic WorkManager initialization -->
    <meta-data
        android:name="androidx.work.WorkManagerInitializer"
        android:value="androidx.startup"
        tools:node="remove" />
</provider>
```

```java
// In Application.onCreate()
Configuration config = new Configuration.Builder()
    .setMinimumLoggingLevel(Log.INFO)
    // Add other custom configuration
    .build();

WorkManager.initialize(this, config);
```

### Related Issues

This fix resolves:
- ✅ Immediate crash on Pixel Watch startup
- ✅ IllegalStateException in WearApplication
- ✅ WorkManager double initialization
- ✅ Background sync not working

### References

- [WorkManager Manual Initialization](https://developer.android.com/topic/libraries/architecture/workmanager/advanced/custom-configuration)
- [App Startup Library](https://developer.android.com/topic/libraries/app-startup)
- [Manifest Merger](https://developer.android.com/studio/build/manage-manifests)

### Commit

- **Hash**: d482dc6
- **Date**: 2026-02-13
- **Message**: Fix WorkManager double initialization crash on Pixel Watch startup
