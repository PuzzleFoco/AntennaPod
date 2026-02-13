package de.danoeh.antennapod.wear.sync;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.storage.preferences.SynchronizationSettings;

/**
 * Manages synchronization scheduling for the Wear OS app.
 * Handles both manual and periodic sync operations.
 */
public class SyncManager {

    private static final String TAG = "SyncManager";
    private static final String PERIODIC_SYNC_WORK_NAME = "wear_periodic_sync";
    private static final String ONE_TIME_SYNC_WORK_NAME = "wear_one_time_sync";

    /**
     * Schedule periodic synchronization based on user preferences
     */
    public static void schedulePeriodicSync(Context context) {
        if (!SynchronizationSettings.isProviderConnected()) {
            Log.d(TAG, "Sync not configured, canceling periodic sync");
            cancelPeriodicSync(context);
            return;
        }

        // Get sync interval from preferences (default to 12 hours)
        long syncIntervalHours = 12; // TODO: Make this configurable
        
        Log.d(TAG, "Scheduling periodic sync every " + syncIntervalHours + " hours");

        // Build constraints - require network connectivity
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Build periodic work request
        PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
                WearSyncWorker.class,
                syncIntervalHours,
                TimeUnit.HOURS
        )
                .setConstraints(constraints)
                .addTag(PERIODIC_SYNC_WORK_NAME)
                .build();

        // Enqueue the periodic work
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        PERIODIC_SYNC_WORK_NAME,
                        ExistingPeriodicWorkPolicy.UPDATE,
                        syncWorkRequest
                );
    }

    /**
     * Cancel periodic synchronization
     */
    public static void cancelPeriodicSync(Context context) {
        Log.d(TAG, "Canceling periodic sync");
        WorkManager.getInstance(context)
                .cancelUniqueWork(PERIODIC_SYNC_WORK_NAME);
    }

    /**
     * Trigger an immediate one-time sync
     */
    public static void triggerImmediateSync(Context context) {
        Log.d(TAG, "Triggering immediate sync");

        // Build constraints - require network connectivity
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Build one-time work request
        OneTimeWorkRequest syncWorkRequest = new OneTimeWorkRequest.Builder(WearSyncWorker.class)
                .setConstraints(constraints)
                .addTag(ONE_TIME_SYNC_WORK_NAME)
                .build();

        // Enqueue the work
        WorkManager.getInstance(context)
                .enqueue(syncWorkRequest);
    }

    /**
     * Cancel all sync work
     */
    public static void cancelAllSync(Context context) {
        Log.d(TAG, "Canceling all sync work");
        WorkManager.getInstance(context)
                .cancelAllWorkByTag(PERIODIC_SYNC_WORK_NAME);
        WorkManager.getInstance(context)
                .cancelAllWorkByTag(ONE_TIME_SYNC_WORK_NAME);
    }
}
