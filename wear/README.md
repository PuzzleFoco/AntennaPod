# AntennaPod for Wear OS

A standalone Wear OS companion app for AntennaPod, enabling podcast listening directly from your wrist.

## Features

- **Standalone Operation**: Works independently without a phone connection
- **Subscription Management**: Browse your podcast subscriptions
- **Episode Browsing**: View episodes per feed with date and duration
- **Now Playing**: Full playback controls (play/pause, skip forward 30s, skip back 10s)
- **Queue**: View and play episodes from your queue
- **Downloads**: Browse and play downloaded episodes for offline listening
- **gpodder.net Sync**: Sync subscriptions and episode progress via gpodder.net
- **Nextcloud Sync**: Alternative sync via Nextcloud gpodder integration
- **Offline Playback**: Download episodes for playback without connectivity

## Architecture

The Wear OS app reuses core AntennaPod modules:

| Module | Usage |
|--------|-------|
| `model` | Feed, FeedItem, FeedMedia data classes |
| `storage:database` | SQLite database for feeds, episodes, queue |
| `storage:preferences` | User preferences and sync settings |
| `net:common` | HTTP client (OkHttp) |
| `net:sync:*` | gpodder.net and Nextcloud synchronization |
| `event` | EventBus events for reactive updates |
| `playback:base` | Playback status and utilities |

### Wear-Specific Components

- **WearPlaybackService**: Media3/ExoPlayer-based playback service optimized for Wear OS
- **WearDownloadService**: Lightweight foreground service for downloading episodes
- **Compose UI**: Built with Wear Compose Material for native round-screen experience

## Building

### Debug Build
```bash
./gradlew :wear:assemblePlayDebug
```

### Release Build
```bash
./gradlew :wear:assemblePlayRelease
```

### Free (F-Droid) Build
```bash
./gradlew :wear:assembleFreeDebug
```

The APK will be at `wear/build/outputs/apk/{flavor}/{buildType}/`.

## Testing

### Unit Tests
```bash
./gradlew :wear:testPlayDebugUnitTest
```

### Install on Watch
```bash
adb install wear/build/outputs/apk/play/debug/wear-play-debug.apk
```

## Configuration

### Sync Setup

The Wear OS app supports sync configuration via:
1. **gpodder.net**: Sync subscriptions and episode progress
2. **Nextcloud**: gpodder Nextcloud integration

Sync credentials can be configured from the phone app and shared via the sync settings storage.

### Login via Smartphone

Instead of typing on the small watch screen you can adopt the sync settings from your
paired phone (**Play build** only, uses the Wearable Data Layer):

1. In the watch's **gpodder.net** or **Nextcloud** login screen tap **"Fetch from phone"**.
2. The paired AntennaPod app automatically sends its configured server, username and
   password/app-key to the watch, which stores them and starts a feed refresh + sync.

On the **free / F-Droid** build (no Google Play Services) this falls back to entering the
details on the watch or confirming in the phone browser (Nextcloud Login v2, gpodder.net
"Open on Phone").

## Technical Details

- **Min SDK**: 26 (Wear OS 2.0+)
- **Target SDK**: 34
- **UI Framework**: Jetpack Compose for Wear OS
- **Playback**: AndroidX Media3 (ExoPlayer)
- **Build Flavors**: `play` (Google Play) and `free` (F-Droid)

## Screens

1. **Home**: Main menu with navigation to all sections
2. **Subscriptions**: List of subscribed podcasts
3. **Episodes**: Episode list for a selected podcast
4. **Now Playing**: Playback controls with progress
5. **Queue**: Playback queue management
6. **Downloads**: Downloaded episodes for offline playback
7. **Settings**: Sync configuration and app info
