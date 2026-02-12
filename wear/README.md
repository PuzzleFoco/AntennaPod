# AntennaPod Wear OS

Wear OS companion and standalone app for AntennaPod podcast player.

## Features

### Current Implementation

- **Standalone Mode**: The app can run independently on Wear OS devices
- **Companion Mode**: Integrates with the Android app when available
- **Media Playback**: MediaBrowser service for podcast playback
- **Sync Support**: Synchronizes listening history with gpodder.net or Nextcloud GPodder
- **Wear Compose UI**: Modern UI built with Jetpack Compose for Wear OS

### Supported Screens

1. **Home**: Main navigation hub
2. **Now Playing**: Current episode playback controls
3. **Library**: Browse your podcast episodes
4. **Sync**: Manual sync trigger and status
5. **Settings**: App configuration

## Requirements

- **Minimum SDK**: 26 (Android 8.0 / Wear OS 2.0)
- **Target SDK**: 35
- **Supports**: Pixel Watch 1 and all current Wear OS versions

## Architecture

The Wear OS app follows AntennaPod's modular architecture:

### Dependencies

- **Core Modules**:
  - `event` - EventBus communication
  - `model` - Data models
  - `storage:database` - Local data storage
  - `storage:preferences` - User preferences
  - `net:sync:service` - gpodder/Nextcloud sync
  - `playback:base` - Playback abstractions

- **Wear OS Specific**:
  - `androidx.wear:wear` - Wear OS platform support
  - `androidx.wear.compose:compose-material` - Wear Compose UI
  - `androidx.media3:media3-session` - Media playback
  - `androidx.work:work-runtime` - Background sync

### Key Components

#### 1. WearApplication
Main application class that initializes:
- EventBus for inter-component communication
- UserPreferences for settings
- WorkManager for background sync

#### 2. MainActivity
Main UI activity using Jetpack Compose for Wear OS. Provides navigation between:
- Home screen
- Now Playing screen
- Library screen
- Sync screen
- Settings screen

#### 3. WearPlaybackService
MediaBrowserServiceCompat implementation that:
- Manages media playback on the watch
- Connects to phone's PlaybackService in companion mode
- Provides MediaSession for system controls
- Supports standard playback actions (play, pause, skip, seek)

#### 4. Sync Components

**WearSyncWorker**: WorkManager Worker that performs synchronization
- Syncs with gpodder.net or Nextcloud GPodder
- Posts sync events via EventBus
- Handles network errors with retry logic

**SyncManager**: Manages sync scheduling
- Schedules periodic sync (default: 12 hours)
- Triggers immediate one-time sync
- Cancels sync when disabled

## Sync Integration

The Wear OS app uses the same sync infrastructure as the Android app:

### Supported Providers
- **gpodder.net**: Open source podcast sync service
- **Nextcloud GPodder**: Self-hosted gpodder alternative

### Synced Data
- Subscription changes (add/remove podcasts)
- Episode actions (play position, completion status)
- Listening history

### Configuration
Sync settings are shared with the Android app through `UserPreferences`:
- Provider selection (gpodder.net vs. Nextcloud)
- Credentials (username, password, server URL)
- Sync interval
- Enable/disable sync

## Standalone vs. Companion Mode

### Standalone Mode
- App works independently without phone connection
- Directly downloads and plays podcasts
- Syncs listening history with gpodder/Nextcloud
- Requires internet connection on the watch

### Companion Mode
- Connects to phone's AntennaPod app via MediaBrowser
- Controls playback on the phone
- Accesses phone's podcast library
- Lower battery consumption on watch

The app automatically detects if the companion app is available and switches between modes accordingly.

## Documentation

- **[Setup Guide](SETUP.md)**: Detailed instructions for installing and configuring the Wear OS app
- **[Integration Guide](INTEGRATION.md)**: Technical details on how the Wear app integrates with the Android app

## Building

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 35
- Wear OS emulator or physical Wear OS device (API 26+)

### Build Commands

```bash
# Build debug APK
./gradlew :wear:assembleDebug

# Build release APK
./gradlew :wear:assembleRelease

# Install on connected Wear OS device
./gradlew :wear:installDebug
```

## Testing

### Emulator Setup
1. Open AVD Manager in Android Studio
2. Create a new Wear OS device (e.g., Wear OS Small Round)
3. Select API 30 or higher
4. Start the emulator

### Physical Device
1. Enable Developer Options on your Wear OS device
2. Enable ADB debugging
3. Connect via USB or Wi-Fi ADB
4. Run `adb devices` to verify connection

### Test Cases
- [ ] App launches successfully
- [ ] Navigation between screens works
- [ ] Playback controls respond correctly
- [ ] Sync can be triggered manually
- [ ] Settings can be accessed and modified
- [ ] Companion mode connects to phone app
- [ ] Standalone mode plays podcasts directly

## Known Limitations

1. **Compose for Wear OS**: Some advanced UI features may require newer Wear OS versions
2. **Battery Impact**: Standalone mode with direct playback consumes more battery
3. **Storage**: Limited storage on Wear OS devices may restrict downloaded episodes
4. **Network**: Standalone mode requires Wi-Fi or LTE connection

## Future Enhancements

- [ ] Complications for watch face integration
- [ ] Tiles for quick access to playback
- [ ] Voice control integration
- [ ] Enhanced library browsing with search
- [ ] Download management for offline playback
- [ ] Sleep timer
- [ ] Playback speed control
- [ ] Chapter navigation

## Contributing

Contributions are welcome! Please follow the main AntennaPod contribution guidelines.

## License

AntennaPod Wear OS is licensed under the MIT License. See the main AntennaPod LICENSE file for details.
