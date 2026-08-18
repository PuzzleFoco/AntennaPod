package de.danoeh.antennapod.wear.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import de.danoeh.antennapod.wear.R
import de.danoeh.antennapod.model.feed.FeedMedia
import de.danoeh.antennapod.net.common.AntennapodHttpClient
import de.danoeh.antennapod.storage.database.DBReader
import de.danoeh.antennapod.storage.database.DBWriter
import de.danoeh.antennapod.storage.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * Downloads a single episode to the on-watch media folder and then updates the
 * local database so the episode is marked as downloaded (sets `file_url` and
 * `download_date`). This makes the episode appear in the Downloads screen and
 * enables fully offline playback.
 */
class WearDownloadService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var title: String = "Downloading…"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_DOWNLOAD_URL) ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        title = intent.getStringExtra(EXTRA_TITLE) ?: "Downloading…"
        val mediaId = intent.getLongExtra(EXTRA_MEDIA_ID, -1L)

        startForeground(NOTIFICATION_ID, createNotification(title, "Downloading…"))

        scope.launch {
            try {
                val filePath = downloadFile(url, mediaId)
                if (filePath != null) {
                    markDownloaded(mediaId, filePath)
                    updateNotification(title, getString(R.string.wear_downloaded))
                } else {
                    updateNotification(title, getString(R.string.wear_download_error))
                }
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    /**
     * Downloads [url] into the app's media folder. Returns the absolute path of
     * the written file, or null on failure.
     */
    private fun downloadFile(url: String, mediaId: Long): String? {
        val client = AntennapodHttpClient.getHttpClient()
        val request = Request.Builder().url(url).build()
        val response = try {
            client.newCall(request).execute()
        } catch (e: Exception) {
            return null
        }
        try {
            if (!response.isSuccessful) {
                return null
            }
            val mediaDir = UserPreferences.getDataFolder("media")
            if (mediaDir == null || !mediaDir.exists() && !mediaDir.mkdirs()) {
                return null
            }
            val file = File(mediaDir, "media_$mediaId.mp3")
            response.body?.byteStream()?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            return if (file.length() > 0) file.absolutePath else null
        } catch (e: Exception) {
            return null
        } finally {
            response.close()
        }
    }

    /**
     * Persists the downloaded path and download date in the local database so
     * the episode counts as downloaded.
     */
    private fun markDownloaded(mediaId: Long, filePath: String) {
        try {
            val media: FeedMedia = DBReader.getFeedMedia(mediaId) ?: return
            media.setLocalFileUrl(filePath)
            media.setDownloaded(true, System.currentTimeMillis())
            DBWriter.setFeedMedia(media).get()
        } catch (e: Exception) {
            // If we cannot update the database, at least keep the file on disk
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Downloads",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(title, content))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "wear_downloads"
        private const val NOTIFICATION_ID = 2001
        private const val EXTRA_DOWNLOAD_URL = "download_url"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_MEDIA_ID = "media_id"

        fun startDownload(context: Context, url: String, title: String, mediaId: Long) {
            val intent = Intent(context, WearDownloadService::class.java).apply {
                putExtra(EXTRA_DOWNLOAD_URL, url)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_MEDIA_ID, mediaId)
            }
            context.startForegroundService(intent)
        }
    }
}
