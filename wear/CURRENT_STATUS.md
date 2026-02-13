# Wear OS App - Current Status and Roadmap

## Current Status

### What Works ✅

1. **App Launch**
   - App starts without crashing
   - WorkManager initializes correctly (automatic initialization)
   - EventBus configured properly
   - Ambient mode support enabled

2. **UI Framework**
   - Basic UI layout renders correctly
   - Shows app name, status, and controls
   - Responsive to button taps
   - Ambient mode transitions work

3. **Media Session Framework**
   - MediaSession created and active
   - MediaBrowser connects successfully
   - Transport controls registered
   - Playback state management in place

4. **Sync Integration**
   - Sync button triggers WearSyncWorker
   - EventBus receives sync status updates
   - UI updates based on sync events

### What's Missing ❌

#### Critical Missing Features

**1. No Actual Media Playback**
- WearPlaybackService has all methods as TODOs
- No ExoPlayer or MediaPlayer implementation
- Play/pause buttons only toggle state, don't play audio
- No audio focus handling
- No media routing (speaker/bluetooth)

**2. No Content Access**
- Can't browse podcasts
- Can't see episodes
- Can't select what to play
- No episode metadata displayed

**3. No Configuration**
- No settings screen
- Can't configure sync provider (gpodder/nextcloud)
- Can't set credentials
- Can't manage downloads

**4. No Queue Management**
- Can't see play queue
- Can't add/remove episodes
- Can't reorder queue

#### Important Missing Features

**5. No Episode Information**
- No episode title displayed
- No podcast name shown
- No artwork
- No duration/progress
- No playback speed indicator

**6. No Playback Controls**
- No seek bar
- No skip forward/backward buttons
- No playback speed control
- No sleep timer

**7. No Download Management**
- Can't download episodes for offline
- Can't see download status
- Can't manage storage

**8. No Notifications**
- No media notifications
- Can't control from quick settings
- No watch face complications

## Why the App is Minimal

The current implementation is a **framework/skeleton** that:
- Demonstrates the app can launch on Wear OS
- Sets up the necessary infrastructure (MediaSession, WorkManager, etc.)
- Provides a UI shell to build upon

However, it **lacks the actual implementation** of:
- Media playback logic
- Content browsing and selection
- Settings and configuration
- Most user-facing features

## What Needs to Be Implemented

### Phase 1: Minimal Viable Product (Critical)

These features are essential for the app to be usable:

#### 1. Media Playback Implementation

**File**: `wear/src/main/java/de/danoeh/antennapod/wear/playback/WearMediaPlayer.java` (NEW)

```java
// Needs:
- ExoPlayer instance for audio playback
- Audio focus handling
- Playback state management
- Error handling
- Integration with WearPlaybackService
```

**Files to Modify**:
- `WearPlaybackService.java` - Implement all TODO methods
- Connect MediaSessionCallback to actual player

#### 2. Episode Browsing

**File**: `wear/src/main/java/de/danoeh/antennapod/wear/ui/EpisodeListActivity.java` (NEW)

```java
// Needs:
- Query episodes from database
- Display episode list
- Handle episode selection
- Launch playback
```

**Database Integration**:
- Reuse `storage:database` module
- Query PodcastEpisode entities
- Handle database operations

#### 3. Settings Screen

**File**: `wear/src/main/java/de/danoeh/antennapod/wear/ui/SettingsActivity.java` (NEW)

```java
// Needs:
- Sync provider configuration (gpodder.net/Nextcloud)
- Credentials input
- Sync settings
- Download preferences
- Playback preferences
```

#### 4. Now Playing Screen

**File**: `wear/src/main/java/de/danoeh/antennapod/wear/ui/NowPlayingActivity.java` (NEW)

```java
// Needs:
- Display current episode info
- Show artwork
- Playback controls (play/pause/skip)
- Seek bar
- Duration/position display
```

### Phase 2: Enhanced Features

After Phase 1 is working:

1. **Download Management**
   - Download episodes for offline
   - Manage storage
   - Auto-download settings

2. **Queue Management**
   - View play queue
   - Add/remove episodes
   - Reorder queue

3. **Advanced Playback**
   - Skip forward/backward buttons
   - Playback speed control
   - Sleep timer
   - Chapter support

4. **Notifications**
   - Media notifications
   - Quick settings integration
   - Watch face complications

### Phase 3: Polish

After core features work:

1. **UI/UX Improvements**
   - Better layouts for round/square watches
   - Swipe gestures
   - Haptic feedback
   - Animations

2. **Performance**
   - Optimize database queries
   - Reduce battery usage
   - Efficient image loading

3. **Companion Integration**
   - Sync with phone app
   - Share playback state
   - Remote control from phone

## Code Structure Needed

```
wear/src/main/java/de/danoeh/antennapod/wear/
├── playback/
│   ├── WearPlaybackService.java (EXISTS - needs implementation)
│   ├── WearMediaPlayer.java (NEW - actual player)
│   ├── PlaybackManager.java (NEW - coordinates playback)
│   └── AudioFocusHelper.java (NEW - audio focus)
├── ui/
│   ├── MainActivity.java (EXISTS - basic shell)
│   ├── EpisodeListActivity.java (NEW - browse episodes)
│   ├── NowPlayingActivity.java (NEW - playback screen)
│   ├── SettingsActivity.java (NEW - configuration)
│   └── QueueActivity.java (NEW - manage queue)
├── storage/
│   ├── WearPreferences.java (NEW - local preferences)
│   └── EpisodeRepository.java (NEW - data access)
└── sync/
    ├── WearSyncWorker.java (EXISTS - works)
    └── SyncManager.java (EXISTS - works)
```

## Effort Estimate

Based on the missing functionality:

- **Phase 1 (MVP)**: 40-60 hours of development
  - Media playback: 15-20 hours
  - Episode browsing: 10-15 hours
  - Settings: 8-10 hours
  - Now playing UI: 7-10 hours

- **Phase 2 (Enhanced)**: 30-40 hours
- **Phase 3 (Polish)**: 20-30 hours

**Total**: 90-130 hours for a complete, polished Wear OS app

## Recommendations

### Option A: Minimal Implementation
Implement just enough to make it usable:
- Basic episode list
- Simple playback with play/pause
- Basic settings
- ~20-30 hours of work

### Option B: Phased Development
Implement Phase 1 completely, then iterate:
- Full MVP functionality
- Usable but basic app
- ~40-60 hours of work

### Option C: Documentation First
Create detailed technical specifications:
- Detailed architecture docs
- API integration guides
- Implementation guides for contributors
- ~10-15 hours of documentation

## What to Do Next

Given the current state, I recommend:

1. **Acknowledge the gap**: The current implementation is a framework, not a working app

2. **Choose approach**:
   - If you need a working app soon: Go with Option A (minimal)
   - If you want it done right: Go with Option B (phased)
   - If you want community help: Go with Option C (documentation)

3. **Set expectations**: A fully functional Wear OS app requires significant development effort

4. **Start small**: Begin with one feature (e.g., episode browsing) and test it thoroughly

## Current Recommendation

I suggest starting with **episode browsing** as the first feature:
1. It's essential for usability
2. It doesn't depend on complex playback logic
3. It can be tested independently
4. It provides immediate value

Would you like me to:
- A) Implement episode browsing as a first step?
- B) Create detailed implementation guides?
- C) Focus on a different feature first?
