package de.danoeh.antennapod.wear.phone

import android.util.Log
import android.widget.Toast
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue
import de.danoeh.antennapod.storage.preferences.SynchronizationCredentials
import de.danoeh.antennapod.storage.preferences.SynchronizationSettings

/**
 * Receives the sync configuration sent by the paired phone's AntennaPod app and
 * stores it locally, then triggers a feed refresh + sync so the watch is ready
 * to use without typing any credentials on the watch.
 */
class WearCredentialSyncService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            val path = event.dataItem.uri.path ?: continue
            if (event.type != DataEvent.TYPE_CHANGED || path != SYNC_CONFIG_PATH) {
                continue
            }
            val map = DataMapItem.fromDataItem(event.dataItem).dataMap
            importConfig(map)
            runCatching {
                Toast.makeText(
                    this,
                    de.danoeh.antennapod.wear.R.string.wear_phone_imported,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun importConfig(map: com.google.android.gms.wearable.DataMap) {
        Log.d(TAG, "Importing sync configuration from phone")
        map.getString(PROVIDER)?.let { SynchronizationSettings.setSelectedSyncProvider(it) }
        map.getString(HOST)?.let { SynchronizationCredentials.setHosturl(it) }
        map.getString(USERNAME)?.let { SynchronizationCredentials.setUsername(it) }
        map.getString(PASSWORD)?.let { SynchronizationCredentials.setPassword(it) }
        map.getString(DEVICE_ID)?.let { SynchronizationCredentials.setDeviceId(it) }

        // Refresh feeds and push progress right away.
        runCatching { FeedUpdateManager.getInstance()?.runOnce(applicationContext) }
        runCatching { SynchronizationQueue.getInstance().fullSync() }
    }

    companion object {
        private const val TAG = "WearCredSyncService"
        const val SYNC_CONFIG_PATH = "/antennapod/sync_config"

        private const val PROVIDER = "provider"
        private const val HOST = "host"
        private const val USERNAME = "username"
        private const val PASSWORD = "password"
        private const val DEVICE_ID = "device_id"
    }
}
