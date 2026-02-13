package de.danoeh.antennapod.wear.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import de.danoeh.antennapod.event.SyncServiceEvent;
import de.danoeh.antennapod.wear.R;
import de.danoeh.antennapod.wear.playback.WearPlaybackService;
import de.danoeh.antennapod.wear.sync.SyncManager;

/**
 * Main activity for AntennaPod Wear OS app.
 * Provides a simple interface for playback control and sync management.
 */
public class MainActivity extends FragmentActivity implements 
        AmbientModeSupport.AmbientCallbackProvider {

    private TextView statusText;
    private Button playPauseButton;
    private Button syncButton;
    private Button settingsButton;
    private TextView syncStatusText;
    
    private MediaBrowserCompat mediaBrowser;
    private MediaControllerCompat mediaController;
    private AmbientModeSupport.AmbientController ambientController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Enable ambient mode support
        ambientController = AmbientModeSupport.attach(this);
        
        // Initialize views
        statusText = findViewById(R.id.status_text);
        playPauseButton = findViewById(R.id.play_pause_button);
        syncButton = findViewById(R.id.sync_button);
        settingsButton = findViewById(R.id.settings_button);
        syncStatusText = findViewById(R.id.sync_status_text);
        
        // Set up button listeners
        playPauseButton.setOnClickListener(v -> togglePlayback());
        syncButton.setOnClickListener(v -> triggerSync());
        settingsButton.setOnClickListener(v -> openSettings());
        
        // Initialize media browser
        mediaBrowser = new MediaBrowserCompat(
                this,
                new ComponentName(this, WearPlaybackService.class),
                connectionCallback,
                null
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        mediaBrowser.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        if (mediaController != null) {
            mediaController.unregisterCallback(controllerCallback);
        }
        mediaBrowser.disconnect();
    }

    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new AmbientModeSupport.AmbientCallback() {
            @Override
            public void onEnterAmbient(Bundle ambientDetails) {
                // Update UI for ambient mode
                statusText.getPaint().setAntiAlias(false);
            }

            @Override
            public void onExitAmbient() {
                // Update UI for interactive mode
                statusText.getPaint().setAntiAlias(true);
            }
        };
    }

    private final MediaBrowserCompat.ConnectionCallback connectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    try {
                        mediaController = new MediaControllerCompat(
                                MainActivity.this,
                                mediaBrowser.getSessionToken()
                        );
                        MediaControllerCompat.setMediaController(MainActivity.this, mediaController);
                        mediaController.registerCallback(controllerCallback);

                        // Update UI based on current playback state
                        updatePlaybackUI();
                        statusText.setText(R.string.connected);
                    } catch (Exception e) {
                        statusText.setText(R.string.error_occurred);
                    }
                }

                @Override
                public void onConnectionFailed() {
                    statusText.setText(R.string.no_connection);
                }
            };

    private final MediaControllerCompat.Callback controllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    updatePlaybackUI();
                }
            };

    private void togglePlayback() {
        if (mediaController == null) {
            return;
        }

        PlaybackStateCompat state = mediaController.getPlaybackState();
        if (state != null) {
            if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
                mediaController.getTransportControls().pause();
            } else {
                mediaController.getTransportControls().play();
            }
        }
    }

    private void updatePlaybackUI() {
        if (mediaController == null) {
            playPauseButton.setText(R.string.play);
            return;
        }

        PlaybackStateCompat state = mediaController.getPlaybackState();
        if (state != null && state.getState() == PlaybackStateCompat.STATE_PLAYING) {
            playPauseButton.setText(R.string.pause);
        } else {
            playPauseButton.setText(R.string.play);
        }
    }

    private void triggerSync() {
        syncStatusText.setText(R.string.syncing);
        syncButton.setEnabled(false);
        SyncManager.triggerImmediateSync(this);
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSyncEvent(SyncServiceEvent event) {
        // Update sync status based on the message resource ID
        int messageResId = event.getMessageResId();
        
        // Check if it's one of our sync-related messages
        if (messageResId == R.string.syncing) {
            syncStatusText.setText(R.string.syncing);
            syncButton.setEnabled(false);
        } else if (messageResId == R.string.sync_complete) {
            syncStatusText.setText(R.string.sync_complete);
            syncButton.setEnabled(true);
        } else if (messageResId == R.string.sync_failed) {
            syncStatusText.setText(R.string.sync_failed);
            syncButton.setEnabled(true);
        }
        // For other message types from the main app, just ignore them
    }
}
