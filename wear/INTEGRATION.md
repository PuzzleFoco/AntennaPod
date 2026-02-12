# Integration Guide: Wear OS ↔ Android App

This document describes how the Wear OS app integrates with the main AntennaPod Android application.

## Architecture Overview

```
┌─────────────────────────┐        ┌─────────────────────────┐
│   Wear OS Device        │        │   Android Phone         │
│                         │        │                         │
│  ┌──────────────────┐   │        │  ┌──────────────────┐   │
│  │  AntennaPod      │   │        │  │  AntennaPod      │   │
│  │  Wear App        │   │        │  │  Mobile App      │   │
│  └──────────────────┘   │        │  └──────────────────┘   │
│          │              │        │          │              │
│          ▼              │        │          ▼              │
│  ┌──────────────────┐   │        │  ┌──────────────────┐   │
│  │ WearPlayback     │───┼────────┼─▶│ PlaybackService  │   │
│  │ Service          │◀──┼────────┼───│                  │   │
│  └──────────────────┘   │        │  └──────────────────┘   │
│          │              │        │          │              │
│          ▼              │        │          ▼              │
│  ┌──────────────────┐   │        │  ┌──────────────────┐   │
│  │ Local Storage    │   │        │  │ Local Database   │   │
│  └──────────────────┘   │        │  └──────────────────┘   │
│          │              │        │          │              │
└──────────┼──────────────┘        └──────────┼──────────────┘
           │                                  │
           │        ┌──────────────┐          │
           └────────▶ gpodder.net  ◀──────────┘
                    │ or Nextcloud │
                    └──────────────┘
```

## Communication Methods

### 1. MediaBrowser/MediaController Pattern

The primary communication method uses Android's MediaBrowser framework:

#### From Wear → Phone

```java
// Wear app connects to phone's PlaybackService
MediaBrowserCompat mediaBrowser = new MediaBrowserCompat(
    context,
    new ComponentName("de.danoeh.antennapod", 
                     "de.danoeh.antennapod.playback.service.PlaybackService"),
    connectionCallback,
    null
);
mediaBrowser.connect();

// Send playback commands
MediaControllerCompat controller = MediaControllerCompat.getMediaController(activity);
controller.getTransportControls().play();
controller.getTransportControls().pause();
controller.getTransportControls().skipToNext();
```

#### From Phone → Wear

```java
// Phone's PlaybackService broadcasts state changes
MediaSessionCompat session = new MediaSessionCompat(context, TAG);
session.setPlaybackState(playbackState);
session.setMetadata(metadata);

// Wear app receives via MediaController callbacks
controller.registerCallback(new MediaControllerCompat.Callback() {
    @Override
    public void onPlaybackStateChanged(PlaybackStateCompat state) {
        // Update UI
    }
    
    @Override
    public void onMetadataChanged(MediaMetadataCompat metadata) {
        // Update UI with episode info
    }
});
```

### 2. Data Layer API (Optional Enhancement)

For richer data synchronization, the Wearable Data Layer API can be used:

```java
// Send data from phone to watch
PutDataMapRequest dataMap = PutDataMapRequest.create("/antennapod/queue");
dataMap.getDataMap().putString("current_episode", episodeTitle);
dataMap.getDataMap().putLong("position", currentPosition);
PutDataRequest request = dataMap.asPutDataRequest();
Wearable.getDataClient(context).putDataItem(request);

// Receive on watch
DataClient.OnDataChangedListener listener = dataEventBuffer -> {
    for (DataEvent event : dataEventBuffer) {
        if (event.getType() == DataEvent.TYPE_CHANGED) {
            DataItem item = event.getDataItem();
            if (item.getUri().getPath().equals("/antennapod/queue")) {
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                String episode = dataMap.getString("current_episode");
                long position = dataMap.getLong("position");
            }
        }
    }
};
```

## Sync Integration

Both apps sync with the same backend (gpodder.net or Nextcloud):

### Shared Sync Service

```
┌─────────────────────┐
│  SyncService        │  ← Used by both Wear and Mobile
├─────────────────────┤
│  - performSync()    │
│  - uploadActions()  │
│  - downloadActions()│
└─────────────────────┘
         │
         ▼
┌─────────────────────┐
│ ISyncService        │  ← Interface
├─────────────────────┤
│ - login()           │
│ - syncSubscriptions()│
│ - syncEpisodeActions│
└─────────────────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌─────────┐ ┌──────────────┐
│gpodder  │ │Nextcloud     │
│Service  │ │GpodderService│
└─────────┘ └──────────────┘
```

### Sync Coordination

- **Conflict Resolution**: Last-write-wins based on timestamp
- **Episode Actions**: 
  - Play position synced every 30 seconds during playback
  - Completion status synced immediately
- **Subscriptions**: 
  - Add/remove synced on next sync cycle
  - No real-time push (polling-based)

### UserPreferences Sharing

Both apps access the same preferences if using Data Layer:

```java
// Phone saves preference
SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
prefs.edit()
    .putString("gpodder_username", username)
    .putString("gpodder_password", password)
    .apply();

// Sync to watch via Message API
Wearable.getMessageClient(context)
    .sendMessage(nodeId, "/antennapod/prefs", prefsJson.getBytes());
```

## Feature Parity Matrix

| Feature | Mobile App | Wear App | Sync Method |
|---------|-----------|----------|-------------|
| Playback Controls | ✅ Full | ✅ Basic | MediaController |
| Episode Queue | ✅ | ⏳ Future | MediaBrowser |
| Podcast Search | ✅ | ❌ | N/A |
| Subscribe/Unsubscribe | ✅ | ⏳ Future | Sync Service |
| Download Management | ✅ | ⏳ Future | N/A |
| Playback History | ✅ | ✅ | Sync Service |
| Sleep Timer | ✅ | ⏳ Future | MediaController |
| Playback Speed | ✅ | ⏳ Future | MediaController |
| Chapter Navigation | ✅ | ⏳ Future | MediaController |

## Companion App Detection

The Wear app detects if it should run in companion or standalone mode:

```java
public class ConnectionManager {
    public boolean isPhoneAppAvailable() {
        // Check if MediaBrowser can connect to phone's service
        ComponentName service = new ComponentName(
            "de.danoeh.antennapod",
            "de.danoeh.antennapod.playback.service.PlaybackService"
        );
        
        PackageManager pm = context.getPackageManager();
        try {
            pm.getServiceInfo(service, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    
    public Mode determineMode() {
        if (isPhoneAppAvailable() && isPhoneConnected()) {
            return Mode.COMPANION;
        } else {
            return Mode.STANDALONE;
        }
    }
}
```

## Manifest Configuration

### Wear App Manifest

```xml
<!-- Declare as standalone capable -->
<meta-data
    android:name="com.google.android.wearable.standalone"
    android:value="true" />

<!-- Reference companion app -->
<meta-data
    android:name="com.google.android.wearable.companionApp"
    android:value="de.danoeh.antennapod" />

<!-- Declare MediaBrowserService client -->
<uses-permission android:name="android.permission.MEDIA_CONTENT_CONTROL" />
```

### Mobile App Manifest (No Changes Needed)

The mobile app already exports its PlaybackService for MediaBrowser clients:

```xml
<service
    android:name=".playback.service.PlaybackService"
    android:exported="true">
    <intent-filter>
        <action android:name="android.media.browse.MediaBrowserService" />
    </intent-filter>
</service>
```

## Data Flow Examples

### Example 1: Playing an Episode from Watch

```
1. User taps Play on Wear app
   └─> MainActivity.togglePlayback()
   
2. Wear app sends command
   └─> mediaController.getTransportControls().play()
   
3. Command sent via Bluetooth/Wi-Fi to phone
   
4. Phone's PlaybackService receives command
   └─> onPlay() in MediaSessionCallback
   
5. Phone starts playback
   └─> Updates PlaybackState
   
6. State change broadcast to Wear app
   └─> onPlaybackStateChanged() callback
   
7. Wear app updates UI
   └─> playPauseButton.setText("Pause")
```

### Example 2: Syncing Playback Position

```
1. User listens to episode on phone
   
2. PlaybackService updates position
   └─> Every 30 seconds
   
3. SyncQueue records action
   └─> EpisodeAction(PLAY, position, timestamp)
   
4. Sync triggered (automatic or manual)
   └─> SyncService.performSync()
   
5. Position uploaded to gpodder
   └─> POST /api/2/episodes/action
   
6. Wear app syncs independently
   └─> GET /api/2/episodes/action
   
7. Wear app receives position update
   └─> Updates local database
   
8. If episode plays on watch, resumes from synced position
```

### Example 3: Subscribing to Podcast

```
1. User subscribes on phone
   └─> FeedManager.subscribeFeed(url)
   
2. Subscription added to database
   
3. Sync triggered
   └─> SyncService uploads subscription change
   
4. Wear app syncs
   └─> Downloads subscription list
   
5. Wear app updates local database
   └─> New podcast appears in library
```

## Security Considerations

### Authentication

- **Shared Credentials**: Sync credentials stored in UserPreferences
- **Encryption**: Passwords encrypted in SharedPreferences
- **Token Refresh**: OAuth tokens refreshed as needed

### Permissions

- **Phone**: Needs FOREGROUND_SERVICE_MEDIA_PLAYBACK
- **Wear**: Same permissions as phone
- **Data Sharing**: User must authorize watch pairing

## Testing Integration

### Manual Testing

1. **Setup**: 
   - Install both apps
   - Pair watch with phone
   - Configure sync on both

2. **Test Companion Mode**:
   - Play episode on phone
   - Control from watch
   - Verify state syncs

3. **Test Standalone Mode**:
   - Disconnect watch from phone
   - Play episode on watch
   - Verify sync works independently

### Automated Testing

```java
@Test
public void testMediaBrowserConnection() {
    // Mock phone's PlaybackService
    ShadowMediaBrowserServiceCompat.addServiceToReturn(
        new ComponentName(context, MockPlaybackService.class)
    );
    
    // Connect from Wear app
    MediaBrowserCompat browser = new MediaBrowserCompat(/*...*/);
    browser.connect();
    
    // Verify connection
    assertTrue(browser.isConnected());
}
```

## Future Enhancements

### Planned Features

1. **Offline First**: Download queue sync
2. **Real-time Sync**: WebSocket for instant updates
3. **Rich Notifications**: Full playback control from notification
4. **Complications**: Watch face integration
5. **Voice Control**: Hands-free operation

### API Extensions

- **Custom Actions**: Additional MediaSession actions
- **Rich Metadata**: Album art, chapters, show notes
- **Analytics**: Privacy-respecting usage metrics

## Troubleshooting Integration Issues

### Connection Problems

```bash
# Check Bluetooth/Wi-Fi connectivity
adb shell dumpsys bluetooth_manager
adb shell dumpsys connectivity

# Verify MediaBrowser service
adb shell dumpsys media_session

# Check if service is exported
adb shell dumpsys package de.danoeh.antennapod | grep Service
```

### Sync Issues

```bash
# Check sync status
adb logcat | grep SyncService

# Verify network connectivity on watch
adb -s <watch-device-id> shell ping -c 3 gpodder.net

# Check sync preferences
adb shell run-as de.danoeh.antennapod.wear cat /data/data/de.danoeh.antennapod.wear/shared_prefs/*.xml
```

## Contributing

When adding features that span both apps:

1. Add interface to `service-interface` module
2. Implement in both `app` and `wear` modules
3. Ensure sync compatibility
4. Update this integration guide
5. Add integration tests

## References

- [MediaBrowser API](https://developer.android.com/reference/android/media/browse/MediaBrowser)
- [Wear OS Data Layer](https://developer.android.com/training/wearables/data/data-layer)
- [gpodder API](https://gpoddernet.readthedocs.io/)
