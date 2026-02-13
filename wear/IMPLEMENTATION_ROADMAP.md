# Wear OS Implementation Roadmap

## User Request (2026-02-13)
"Please implement all the missing features. Start with connecting to gpodder/nextcloud and then with episode overview, playing etc. It is ok if it takes time."

## Current Status
The Wear OS module has:
- ✅ Basic app structure (WearApplication, MainActivity)
- ✅ MediaSession framework (WearPlaybackService)
- ✅ Sync scheduling (WearSyncWorker, SyncManager)
- ✅ WorkManager automatic initialization
- ✅ Manifest configuration
- ✅ Build configuration with product flavors

Missing features:
- ❌ Sync configuration UI (can't set up gpodder/nextcloud)
- ❌ Episode browsing (can't see episodes)
- ❌ Actual media playback (play button doesn't work)
- ❌ Download management
- ❌ Queue management
- ❌ Settings screens

## Implementation Phases

### Phase 1: Sync Configuration ⏳ IN PROGRESS
**Priority**: Critical - Foundation for everything else

**Goal**: Enable users to configure gpodder.net or Nextcloud sync

**Tasks**:
1. Create `SettingsActivity`
   - Extend `androidx.appcompat.app.AppCompatActivity`
   - Use Wear OS styling
   
2. Create settings layout (`activity_settings.xml`)
   - Provider selection (gpodder.net / Nextcloud GPodder)
   - Server URL input (for Nextcloud)
   - Username input
   - Password input
   - Login/Test connection button
   - Status indicator

3. Integrate with existing classes:
   - `SynchronizationSettings` (from storage:preferences)
   - `SynchronizationCredentials` (from storage:preferences)
   
4. Implement connection testing:
   - Use `SyncService` from net:sync:service
   - Test authentication
   - Show success/error feedback

5. Add Settings button to MainActivity
   - Intent to launch SettingsActivity

**Files to create**:
- `wear/src/main/java/de/danoeh/antennapod/wear/ui/SettingsActivity.java`
- `wear/src/main/res/layout/activity_settings.xml`
- `wear/src/main/res/values/strings.xml` (add new strings)

**Dependencies**:
- Already has: `storage:preferences`, `net:sync:service`
- No new dependencies needed

**Testing**:
- Manual: Launch settings, enter credentials, test connection
- Verify credentials are saved
- Verify sync works after configuration

**Estimated effort**: 4-6 hours

---

### Phase 2: Episode Data & Browsing ⏳ NEXT
**Priority**: High - Core functionality

**Goal**: Display list of subscribed podcasts and their episodes

**Tasks**:
1. Create `EpisodesActivity`
   - Extend `androidx.appcompat.app.AppCompatActivity`
   - RecyclerView for episode list
   
2. Create episode list adapter
   - `EpisodeListAdapter extends RecyclerView.Adapter`
   - ViewHolder for episode items
   
3. Create episode item layout (`item_episode.xml`)
   - Episode title
   - Podcast name
   - Duration
   - Play status indicator
   - Thumbnail

4. Integrate with database:
   - Use `storage:database` module
   - Query `DBReader.getEpisodes()`
   - Use `FeedItem` model
   
5. Add navigation from MainActivity
   - "Browse Episodes" button
   - Intent to EpisodesActivity

6. Implement episode selection
   - Click listener
   - Pass episode to playback

**Files to create**:
- `wear/src/main/java/de/danoeh/antennapod/wear/ui/EpisodesActivity.java`
- `wear/src/main/java/de/danoeh/antennapod/wear/adapter/EpisodeListAdapter.java`
- `wear/src/main/res/layout/activity_episodes.xml`
- `wear/src/main/res/layout/item_episode.xml`

**Dependencies**:
- Already has: `storage:database`
- May need: RecyclerView (should be included in wear dependencies)

**Testing**:
- Manual: Launch episodes activity
- Verify episodes load from database
- Verify episode details display correctly
- Test episode selection

**Estimated effort**: 6-8 hours

---

### Phase 3: Media Playback Implementation ⏳ NEXT
**Priority**: Critical - Core functionality

**Goal**: Actually play podcast episodes

**Tasks**:
1. Implement `PlaybackManager`
   - ExoPlayer instance
   - Playback state management
   - Progress tracking
   
2. Update `WearPlaybackService`
   - Remove TODO placeholders
   - Implement `onPlay()`
   - Implement `onPause()`
   - Implement `onStop()`
   - Implement `onSeekTo()`
   - Implement `onSkipToNext()`
   - Implement `onSkipToPrevious()`
   
3. Create playback state management
   - Current episode tracking
   - Position tracking
   - Duration tracking
   - Queue management

4. Update MainActivity
   - Connect play button to actual playback
   - Display current episode info
   - Show playback progress
   - Update UI based on playback state
   
5. Add playback controls
   - Play/pause button (already exists, make functional)
   - Skip forward 30s button
   - Skip backward 30s button
   - Seek bar
   - Speed control (1x, 1.5x, 2x)

6. Integrate with MediaSession
   - Update metadata
   - Update playback state
   - Handle media button events

**Files to modify**:
- `wear/src/main/java/de/danoeh/antennapod/wear/playback/WearPlaybackService.java`
- `wear/src/main/java/de/danoeh/antennapod/wear/ui/MainActivity.java`
- `wear/src/main/res/layout/activity_main.xml`

**Files to create**:
- `wear/src/main/java/de/danoeh/antennapod/wear/playback/PlaybackManager.java`
- `wear/src/main/java/de/danoeh/antennapod/wear/playback/PlaybackState.java`

**Dependencies**:
- Add: ExoPlayer (androidx.media3:media3-exoplayer)
- Already has: MediaSession support

**Testing**:
- Manual: Select episode, press play
- Verify audio plays
- Test pause/resume
- Test seek functionality
- Test skip buttons
- Verify progress updates

**Estimated effort**: 10-12 hours

---

### Phase 4: Download Management ⏳ FUTURE
**Priority**: Medium - Nice to have

**Goal**: Download episodes for offline playback

**Tasks**:
1. Implement download UI
   - Download button per episode
   - Download progress indicator
   - Delete downloaded episodes
   
2. Integrate with DownloadService
   - Use existing download infrastructure
   - Queue downloads
   - Track progress

3. Storage management
   - Show available space
   - Auto-delete old episodes
   - Download settings

**Estimated effort**: 6-8 hours

---

### Phase 5: Queue Management ⏳ FUTURE
**Priority**: Medium - Nice to have

**Goal**: Manage episode playback queue

**Tasks**:
1. Queue UI
   - Show queue list
   - Reorder episodes
   - Remove from queue
   
2. Queue management
   - Add to queue
   - Auto-play next
   - Queue persistence

**Estimated effort**: 4-6 hours

---

### Phase 6: Enhanced Features ⏳ FUTURE
**Priority**: Low - Polish

**Tasks**:
1. Podcast browsing
2. Search functionality
3. Sleep timer
4. Playback notifications
5. Watch complications
6. Companion app integration
7. Variable playback speed
8. Chapter support

**Estimated effort**: 15-20 hours

---

## Technical Architecture

### Module Dependencies
```
wear (application)
├── storage:database (episode data)
├── storage:preferences (settings)
├── net:sync:service (sync logic)
├── net:sync:model (sync models)
├── net:common (networking)
├── event (EventBus)
└── model (data models)
```

### Key Classes to Reuse
- `SynchronizationSettings` - Sync configuration
- `SynchronizationCredentials` - Auth credentials
- `DBReader` - Database queries
- `FeedItem` - Episode model
- `Feed` - Podcast model
- `SyncService` - Sync operations
- `PlaybackPreferences` - Playback settings

### New Classes to Create
- `SettingsActivity` - Sync configuration UI
- `EpisodesActivity` - Episode browsing UI
- `EpisodeListAdapter` - Episode list adapter
- `PlaybackManager` - Media playback management
- `PlaybackState` - Playback state model

---

## Implementation Guidelines

### Code Style
- Follow existing AntennaPod code patterns
- Use EventBus for cross-component communication
- Implement proper error handling
- Add logging for debugging
- Use string resources (no hardcoded strings)

### UI Guidelines
- Use Wear OS design principles
- Keep UI simple for small screens
- Large touch targets (48dp minimum)
- Use Wear OS components (WearableRecyclerView, etc.)
- Support round and square watches
- Implement ambient mode where appropriate

### Testing Strategy
- Unit tests for business logic
- Manual testing on emulator
- Test on real Wear OS device
- Test different scenarios:
  - No internet connection
  - Sync failures
  - Playback errors
  - Storage full
  
### Error Handling
- Graceful degradation
- User-friendly error messages
- Retry mechanisms
- Offline support

---

## Progress Tracking

### Completed ✅
- [x] Basic app structure
- [x] WorkManager initialization
- [x] Manifest configuration
- [x] Build system
- [x] Sync scheduling infrastructure
- [x] MediaSession framework

### In Progress ⏳
- [ ] Phase 1: Sync Configuration

### Todo 📋
- [ ] Phase 2: Episode Browsing
- [ ] Phase 3: Media Playback
- [ ] Phase 4: Download Management
- [ ] Phase 5: Queue Management
- [ ] Phase 6: Enhanced Features

---

## Session Notes

### Session 2026-02-13
- User requested full implementation
- Priority: 1) Sync, 2) Episodes, 3) Playback
- User acknowledges it will take time
- Starting with Phase 1

### Key Decisions
- Use automatic WorkManager initialization (simpler)
- Reuse existing AntennaPod modules where possible
- Incremental implementation with testing
- Focus on MVP first, enhance later

### Blockers
None currently

### Next Session Priorities
1. Complete SettingsActivity implementation
2. Test sync configuration
3. Start EpisodesActivity
4. Begin PlaybackManager

---

## Resources

### Documentation
- Wear OS Design Guidelines: https://developer.android.com/design/ui/wear
- Media3 ExoPlayer: https://developer.android.com/media/media3/exoplayer
- MediaSession: https://developer.android.com/media/implement/surfaces/mobile

### Existing Documentation
- `wear/README.md` - Architecture overview
- `wear/CURRENT_STATUS.md` - Current app status
- `wear/INTEGRATION.md` - Integration guide

---

## Estimated Total Effort

| Phase | Effort | Status |
|-------|--------|--------|
| Phase 1: Sync | 4-6 hours | ⏳ In Progress |
| Phase 2: Episodes | 6-8 hours | 📋 Todo |
| Phase 3: Playback | 10-12 hours | 📋 Todo |
| Phase 4: Downloads | 6-8 hours | 📋 Todo |
| Phase 5: Queue | 4-6 hours | 📋 Todo |
| Phase 6: Polish | 15-20 hours | 📋 Todo |
| **Total** | **45-60 hours** | - |

---

## Contact & Support

For questions or issues during implementation:
- Check existing AntennaPod code for patterns
- Review main app implementations
- Test incrementally
- Document decisions and blockers

---

*Last updated: 2026-02-13*
*Status: Phase 1 in progress*
