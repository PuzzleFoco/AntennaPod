package de.danoeh.antennapod.wear.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import de.danoeh.antennapod.wear.R
import de.danoeh.antennapod.model.feed.FeedMedia
import de.danoeh.antennapod.net.common.AntennapodHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class WearDownloadService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Downloading…"
        val mediaId = intent.getLongExtra(EXTRA_MEDIA_ID, -1L)

        startForeground(NOTIFICATION_ID, createNotification(title))

        scope.launch {
            try {
                downloadFile(url, mediaId)
            } finally {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun downloadFile(url: String, mediaId: Long) {
        val client = AntennapodHttpClient.getHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val downloadDir = File(filesDir, "downloads")
            downloadDir.mkdirs()
            val file = File(downloadDir, "media_$mediaId")

            response.body?.byteStream()?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
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

    private fun createNotification(title: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Downloading…")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
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
