# AntennaPod Wear OS - Setup Guide

This guide will help you set up and use the AntennaPod Wear OS app.

## Installation

### From Source
1. Build the Wear OS APK:
   ```bash
   ./gradlew :wear:assembleDebug
   ```

2. Install on your Wear OS device:
   ```bash
   adb install wear/build/outputs/apk/debug/wear-debug.apk
   ```

### Via Companion App (Future)
The Wear OS app will be available through the Play Store and will automatically install when you have the main AntennaPod app installed on your phone.

## Initial Setup

### Standalone Mode

If you want to use the Wear OS app independently:

1. **Connect to Wi-Fi**: 
   - Go to Settings on your watch
   - Select Connectivity → Wi-Fi
   - Connect to your network

2. **Configure Sync** (if using gpodder):
   - Open AntennaPod Wear
   - Tap Settings
   - Tap Sync Settings
   - Select your sync provider (gpodder.net or Nextcloud)
   - Enter your credentials

3. **Add Podcasts**:
   - (Coming in future updates)
   - For now, podcasts will sync from your configured sync service

### Companion Mode

If you want to control playback on your phone:

1. **Install AntennaPod on Phone**:
   - Make sure you have AntennaPod installed on your paired phone
   - The apps should detect each other automatically

2. **Pair Devices**:
   - Your watch should already be paired via Wear OS app
   - Both devices need to be connected

3. **Launch App**:
   - Open AntennaPod Wear
   - The app will connect to your phone's playback service
   - You can now control playback from your watch

## Using the App

### Home Screen

The main screen shows:
- **Connection Status**: Shows if connected to playback service
- **Now Playing Section**: Current episode and playback controls
- **Sync Section**: Sync status and manual sync button

### Playback Controls

- **Play/Pause Button**: Toggles playback
- Tap once to start/stop playback
- Works in both standalone and companion modes

### Sync

- **Manual Sync**: Tap "Sync Now" to immediately sync
- **Sync Status**: Shows current sync state
- Sync happens automatically in the background every 12 hours

## Syncing with gpodder.net

### Setup

1. Create an account at [gpodder.net](https://gpodder.net/)
2. Subscribe to podcasts on the website
3. Configure AntennaPod Wear with your gpodder.net credentials
4. Tap "Sync Now" to download your subscriptions

### What Gets Synced

- **Subscriptions**: Podcasts you're subscribed to
- **Listening Position**: Where you left off in each episode
- **Episode Status**: Played, unplayed, in progress

### Automatic Sync

The app automatically syncs:
- Every 12 hours (when connected to Wi-Fi)
- When you manually tap "Sync Now"
- After major playback events (episode completion)

## Syncing with Nextcloud GPodder

### Setup

1. Install Nextcloud GPodder app on your Nextcloud server
2. Get your server URL and credentials
3. In AntennaPod Wear Settings:
   - Select "Nextcloud GPodder" as provider
   - Enter server URL (e.g., https://cloud.example.com)
   - Enter username and password
4. Tap "Sync Now"

### Benefits of Nextcloud

- **Self-hosted**: Your data stays on your server
- **Privacy**: No third-party service
- **Control**: You manage the sync service

## Ambient Mode

The app supports Always-On Display on compatible watches:

- **Interactive Mode**: Full-color UI with all controls
- **Ambient Mode**: Simplified black & white display
  - Shows current playback status
  - Conserves battery
  - Updates once per minute

## Battery Tips

### Standalone Mode
- **Download episodes on Wi-Fi**: Streaming uses more battery
- **Use offline mode**: Download episodes beforehand
- **Disable auto-sync**: Sync manually when needed

### Companion Mode
- **Lower battery usage**: Phone does the heavy lifting
- **Longer battery life**: Watch just sends control commands
- **Recommended**: For all-day use

## Troubleshooting

### Cannot Connect to Phone

1. Check that both devices are paired
2. Ensure AntennaPod is running on phone
3. Try restarting both apps
4. Check Bluetooth connection

### Sync Fails

1. Verify internet connection (Wi-Fi or LTE)
2. Check credentials are correct
3. Try manual sync
4. Check sync service status (gpodder.net may be down)

### App Crashes

1. Clear app data:
   - Settings → Apps → AntennaPod Wear
   - Storage → Clear data
2. Reinstall the app
3. Report the issue on GitHub

### Playback Issues

1. **No sound**:
   - Check watch volume
   - Ensure episode is downloaded (standalone mode)
   - Check audio output (watch speaker vs Bluetooth)

2. **Playback stutters**:
   - Check internet connection
   - Try downloading episode first
   - Close other apps

## Limitations

### Current Version

- **No podcast browsing**: Must sync from gpodder or phone
- **Limited library view**: Shows recent episodes only
- **No search**: Search will be added in future update
- **No downloads management**: Auto-manages downloads

### Hardware Limitations

- **Storage**: Watch has limited space (download wisely)
- **Battery**: Streaming uses significant power
- **Speakers**: Small speaker may have limited volume

## Advanced Features

### Coming Soon

- **Complications**: Show now playing on watch face
- **Tiles**: Quick access to playback without opening app
- **Voice Control**: "Ok Google, play my podcast"
- **Sleep Timer**: Auto-stop playback
- **Playback Speed**: Adjust speed (1x, 1.5x, 2x)
- **Chapters**: Navigate episode chapters

## Support

For issues, questions, or feature requests:

1. **GitHub Issues**: [Report bugs here](https://github.com/AntennaPod/AntennaPod/issues)
2. **Forum**: [AntennaPod Forum](https://forum.antennapod.org/)
3. **Documentation**: [Wear OS README](README.md)

## Privacy

The Wear OS app follows the same privacy principles as AntennaPod:

- **No tracking**: We don't collect usage data
- **No ads**: Completely ad-free
- **Open source**: Code is public and auditable
- **Your data**: Syncs only to services you configure

## License

AntennaPod Wear OS is licensed under the MIT License.
See the main AntennaPod LICENSE file for details.
