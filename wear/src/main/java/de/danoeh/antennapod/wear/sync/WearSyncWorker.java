package de.danoeh.antennapod.wear.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.greenrobot.eventbus.EventBus;

import de.danoeh.antennapod.event.SyncServiceEvent;
import de.danoeh.antennapod.net.sync.service.SyncService;
import de.danoeh.antennapod.storage.preferences.SynchronizationSettings;
import de.danoeh.antennapod.wear.R;

/**
 * Worker that triggers synchronization with gpodder or Nextcloud GPodder.
 * This is a simple wrapper that delegates to the main SyncService worker.
 */
public class WearSyncWorker extends Worker {

    private static final String TAG = "WearSyncWorker";
    private final WorkerParameters params;

    public WearSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.params = workerParams;
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Wear sync requested");

        // Check if sync is configured
        if (!SynchronizationSettings.isProviderConnected()) {
            Log.d(TAG, "Sync is not configured");
            return Result.success();
        }

        try {
            // Notify that sync is starting
            EventBus.getDefault().post(new SyncServiceEvent(
                    R.string.syncing
            ));

            // Create and execute the actual SyncService worker
            // The SyncService handles all the actual synchronization logic
            SyncService syncService = new SyncService(getApplicationContext(), params);
            Result result = syncService.doWork();

            if (result instanceof Result.Success) {
                Log.d(TAG, "Synchronization completed successfully");
                EventBus.getDefault().post(new SyncServiceEvent(
                        R.string.sync_complete
                ));
            } else {
                Log.w(TAG, "Synchronization did not complete successfully");
            }

            return result;

        } catch (Exception e) {
            Log.e(TAG, "Synchronization failed", e);
            
            // Notify that sync failed
            EventBus.getDefault().post(new SyncServiceEvent(
                    R.string.sync_failed
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
