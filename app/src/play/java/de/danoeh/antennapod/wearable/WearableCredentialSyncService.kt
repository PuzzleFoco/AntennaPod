package de.danoeh.antennapod.wearable

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import de.danoeh.antennapod.storage.preferences.SynchronizationCredentials
import de.danoeh.antennapod.storage.preferences.SynchronizationSettings

/**
 * On the phone (Play flavor): whenever a paired Wear OS watch asks for the sync
 * configuration, this service reads the currently configured gpodder.net /
 * Nextcloud credentials and pushes them to the watch as a data item, so the
 * user never has to type them on the watch.
 */
class WearableCredentialSyncService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        if (!REQUEST_PATH.equals(messageEvent.path)) {
            return
        }
        val map = DataMap().apply {
            SynchronizationSettings.getSelectedSyncProviderKey()?.let { putString(PROVIDER, it) }
            SynchronizationCredentials.getHosturl()?.let { putString(HOST, it) }
            SynchronizationCredentials.getUsername()?.let { putString(USERNAME, it) }
            SynchronizationCredentials.getPassword()?.let { putString(PASSWORD, it) }
            SynchronizationCredentials.getDeviceId()?.let { putString(DEVICE_ID, it) }
        }
        val request = PutDataMapRequest.create(SYNC_CONFIG_PATH)
            .setUrgent()
            .setDataMap(map)
            .asPutDataRequest()
        try {
            Tasks.await(Wearable.getDataClient(this).putDataItem(request))
            Log.d(TAG, "Sent sync configuration to watch")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send sync configuration", e)
        }
    }

    companion object {
        private const val TAG = "WearableCredSync"
        private const val REQUEST_PATH = "/antennapod/get_sync_config"
        private const val SYNC_CONFIG_PATH = "/antennapod/sync_config"

        private const val PROVIDER = "provider"
        private const val HOST = "host"
        private const val USERNAME = "username"
        private const val PASSWORD = "password"
        private const val DEVICE_ID = "device_id"
    }
}
