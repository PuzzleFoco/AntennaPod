package de.danoeh.antennapod.wear.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import de.danoeh.antennapod.model.feed.Feed
import de.danoeh.antennapod.net.common.AntennapodHttpClient
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager
import de.danoeh.antennapod.parser.feed.FeedHandler
import de.danoeh.antennapod.storage.database.DBReader
import de.danoeh.antennapod.storage.database.FeedDatabaseWriter
import okhttp3.Request
import java.io.File

/**
 * Lightweight FeedUpdateManager implementation for Wear OS.
 * Downloads RSS feeds, parses them with the existing FeedHandler parser
 * (reusing the same parser the main app uses), and saves feed metadata
 * and episodes to the local database.
 *
 * This avoids the heavy phone-specific FeedUpdateManagerImpl which depends
 * on Material dialogs, Glide, and other phone UI components.
 */
class WearFeedUpdateManager : FeedUpdateManager() {

    override fun restartUpdateAlarm(context: Context, replace: Boolean) {
        // No periodic updates on Wear OS to save battery
    }

    override fun runOnce(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workRequest = OneTimeWorkRequest.Builder(WearFeedUpdateWorker::class.java)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_ID, ExistingWorkPolicy.REPLACE, workRequest)
    }

    override fun runOnce(context: Context, feed: Feed?) {
        runOnce(context)
    }

    override fun runOnce(context: Context, feed: Feed?, nextPage: Boolean) {
        runOnce(context)
    }

    override fun runOnceOrAsk(context: Context) {
        runOnce(context)
    }

    override fun runOnceOrAsk(context: Context, feed: Feed?) {
        runOnce(context)
    }

    companion object {
        private const val WORK_ID = "wear_feed_update"
    }
}

/**
 * Worker that downloads and parses all subscribed feeds on Wear OS
 * using the same FeedHandler parser as the main AntennaPod app.
 * After parsing, feed titles, descriptions, and all episodes (FeedItems
 * with FeedMedia) are saved to the local database.
 */
class WearFeedUpdateWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): ListenableWorker.Result {
        return try {
            val feeds = DBReader.getFeedList()
            val client = AntennapodHttpClient.getHttpClient()
            for (feed in feeds) {
                if (feed.state != Feed.STATE_SUBSCRIBED) continue
                if (feed.downloadUrl.isNullOrBlank()) continue
                try {
                    refreshFeed(client, feed)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to refresh feed: ${feed.downloadUrl}", e)
                }
            }
            ListenableWorker.Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Feed update failed", e)
            ListenableWorker.Result.failure()
        }
    }

    private fun refreshFeed(client: okhttp3.OkHttpClient, feed: Feed) {
        val request = Request.Builder().url(feed.downloadUrl!!).build()
        val response = client.newCall(request).execute()
        try {
            if (!response.isSuccessful) return
            val body = response.body?.string() ?: return

            // Write RSS XML to a temp file for the FeedHandler parser
            val tempFile = File.createTempFile("feed_", ".xml", appContext.cacheDir)
            try {
                tempFile.writeText(body)

                // Use the same FeedHandler parser as the main app
                val parsedFeed = Feed(feed.downloadUrl, null)
                parsedFeed.localFileUrl = tempFile.absolutePath
                parsedFeed.id = feed.id

                val handler = FeedHandler()
                val result = handler.parseFeed(parsedFeed)
                val parsed = result.feed

                // Copy parsed metadata onto the existing feed
                parsed.id = feed.id
                parsed.state = Feed.STATE_SUBSCRIBED
                parsed.lastRefreshAttempt = System.currentTimeMillis()
                parsed.preferences = feed.preferences

                // Save the fully parsed feed with episodes to the database
                FeedDatabaseWriter.updateFeed(appContext, parsed, false)

                Log.d(TAG, "Refreshed feed: ${parsed.title} with ${parsed.items?.size ?: 0} episodes")
            } finally {
                tempFile.delete()
            }
        } finally {
            response.close()
        }
    }

    companion object {
        private const val TAG = "WearFeedUpdateWorker"
    }
}
