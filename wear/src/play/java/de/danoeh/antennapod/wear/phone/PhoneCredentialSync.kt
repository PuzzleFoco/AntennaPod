package de.danoeh.antennapod.wear.phone

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.TimeUnit

/**
 * Play build: asks the paired phone's AntennaPod app to send its configured
 * gpodder.net / Nextcloud sync credentials to this watch over the Wearable
 * Data Layer. The reply is handled and stored by [WearCredentialSyncService].
 */
object PhoneCredentialSync {
    private const val TAG = "PhoneCredSync"
    private const val REQUEST_PATH = "/antennapod/get_sync_config"
    private const val TIMEOUT_SECONDS = 5L

    /**
     * Sends a request to the nearest connected phone node.
     *
     * @return true if a phone node was found and the request was dispatched.
     */
    fun request(context: Context): Boolean {
        return try {
            val nodeClient = Wearable.getNodeClient(context)
            val nodes = Tasks.await(nodeClient.connectedNodes, TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (nodes.isEmpty()) {
                Log.d(TAG, "No connected nodes (phone not paired?)")
                return false
            }
            val phone: Node? = nodes.firstOrNull { it.isNearby } ?: nodes.first()
            if (phone == null) {
                return false
            }
            val messageClient = Wearable.getMessageClient(context)
            Tasks.await(
                messageClient.sendMessage(phone.id, REQUEST_PATH, ByteArray(0)),
                TIMEOUT_SECONDS, TimeUnit.SECONDS
            )
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not reach phone", e)
            false
        }
    }
}
