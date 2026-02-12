package de.danoeh.antennapod.wear.playback;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Media playback service for Wear OS.
 * This service can work in two modes:
 * 1. Standalone mode: Plays media directly on the watch
 * 2. Companion mode: Connects to the phone's PlaybackService via MediaBrowser
 */
public class WearPlaybackService extends MediaBrowserServiceCompat {

    private static final String TAG = "WearPlaybackService";
    private static final String MEDIA_ROOT_ID = "antennapod_wear_root";
    
    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Create a MediaSessionCompat
        mediaSession = new MediaSessionCompat(this, TAG);
        
        // Set the session's token so that client activities can communicate with it
        setSessionToken(mediaSession.getSessionToken());
        
        // Enable callbacks from MediaButtons and TransportControls
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );
        
        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        stateBuilder = new PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_FAST_FORWARD |
                PlaybackStateCompat.ACTION_REWIND
            );
        mediaSession.setPlaybackState(stateBuilder.build());
        
        // Set callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSessionCallback());
        
        // Start the session
        mediaSession.setActive(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaSession.release();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, 
                                  @Nullable Bundle rootHints) {
        // Allow all clients to connect
        return new BrowserRoot(MEDIA_ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, 
                               @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        // Return empty list for now - will be populated with actual episodes
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        result.sendResult(mediaItems);
    }

    /**
     * Callback to handle media session updates from clients
     */
    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            // TODO: Implement play functionality
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
        }

        @Override
        public void onPause() {
            // TODO: Implement pause functionality
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
        }

        @Override
        public void onStop() {
            // TODO: Implement stop functionality
            updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
        }

        @Override
        public void onSkipToNext() {
            // TODO: Implement skip to next
        }

        @Override
        public void onSkipToPrevious() {
            // TODO: Implement skip to previous
        }

        @Override
        public void onFastForward() {
            // TODO: Implement fast forward
        }

        @Override
        public void onRewind() {
            // TODO: Implement rewind
        }

        @Override
        public void onSeekTo(long pos) {
            // TODO: Implement seek
        }
    }

    /**
     * Update the playback state of the media session
     */
    private void updatePlaybackState(int state) {
        long position = 0; // TODO: Get actual position
        
        stateBuilder.setState(state, position, 1.0f);
        mediaSession.setPlaybackState(stateBuilder.build());
    }
}
