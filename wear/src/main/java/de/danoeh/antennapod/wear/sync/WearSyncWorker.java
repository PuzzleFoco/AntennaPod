package de.danoeh.antennapod.wear.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.greenrobot.eventbus.EventBus;

import de.danoeh.antennapod.event.SyncServiceEvent;
import de.danoeh.antennapod.net.sync.service.SyncService;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationProvider;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

/**
 * Worker that performs synchronization with gpodder or Nextcloud GPodder.
 * This allows the Wear OS app to sync listening history independently.
 */
public class WearSyncWorker extends Worker {

    private static final String TAG = "WearSyncWorker";

    public WearSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting synchronization");

        // Check if sync is enabled
        if (!UserPreferences.isGpodderEnabled()) {
            Log.d(TAG, "Sync is disabled");
            return Result.success();
        }

        try {
            // Notify that sync is starting
            EventBus.getDefault().post(new SyncServiceEvent(
                SyncServiceEvent.MessageType.SYNC_STARTED
            ));

            // Get the sync provider (gpodder.net or Nextcloud)
            SynchronizationProvider provider = UserPreferences.getGpodderProvider();
            Log.d(TAG, "Using sync provider: " + provider);

            // Perform the actual synchronization
            // This will sync subscriptions and episode actions (play position, etc.)
            SyncService.performFullSync(getApplicationContext());

            // Notify that sync completed successfully
            EventBus.getDefault().post(new SyncServiceEvent(
                SyncServiceEvent.MessageType.SYNC_COMPLETED
            ));

            Log.d(TAG, "Synchronization completed successfully");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Synchronization failed", e);
            
            // Notify that sync failed
            EventBus.getDefault().post(new SyncServiceEvent(
                SyncServiceEvent.MessageType.SYNC_FAILED
            ));

            // Retry if it's a network error
            if (e instanceof java.io.IOException) {
                return Result.retry();
            }
            
            return Result.failure();
        }
    }

    /**
     * Called when the worker is stopped (e.g., by the system or due to constraints)
     */
    @Override
    public void onStopped() {
        super.onStopped();
        Log.d(TAG, "Worker stopped");
    }
}
