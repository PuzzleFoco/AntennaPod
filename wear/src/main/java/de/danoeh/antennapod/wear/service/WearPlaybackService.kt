package de.danoeh.antennapod.wear.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import de.danoeh.antennapod.model.feed.FeedItem
import de.danoeh.antennapod.model.feed.FeedMedia
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue
import de.danoeh.antennapod.storage.database.DBReader
import de.danoeh.antennapod.storage.database.DBWriter
import de.danoeh.antennapod.wear.activity.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Media3-based playback service for Wear OS.
 *
 * Besides exposing the media session to the Compose UI it persists the playback
 * position to the local database (so playback resumes where you stopped) and
 * reports episode actions (position, played) to the gpodder.net / Nextcloud
 * sync queue so the listening progress is shared with other devices.
 */
class WearPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var positionSaverJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Initialize the start position of a new episode so the sync
                // queue can build a valid episode action later.
                ioScope.launch {
                    val media = currentMedia() ?: return@launch
                    media.onPlaybackStart()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startPositionSaver()
                } else if (player.playbackState != Player.STATE_ENDED) {
                    saveAndSync(completed = false)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    saveAndSync(completed = true)
                }
            }
        })

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun currentMediaId(): Long? = player.currentMediaItem?.mediaId?.toLongOrNull()

    private fun currentMedia(): FeedMedia? {
        val id = currentMediaId() ?: return null
        return DBReader.getFeedMedia(id)
    }

    /**
     * Persists the current playback position to the database every few seconds
     * while audio is playing.
     */
    private fun startPositionSaver() {
        if (positionSaverJob?.isActive == true) {
            return
        }
        positionSaverJob = ioScope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    savePositionOnly()
                }
                delay(POSITION_SAVE_INTERVAL_MS)
            }
        }
    }

    private fun savePositionOnly() {
        val media = currentMedia() ?: return
        val position = player.currentPosition
        if (position > 0) {
            media.setPosition(position.toInt())
            media.setLastPlayedTimeStatistics(System.currentTimeMillis())
            media.setLastPlayedTimeHistory(Date(System.currentTimeMillis()))
            DBWriter.setFeedMediaPlaybackInformation(media)
        }
    }

    /**
     * Saves the final position and reports the episode action to the sync queue.
     * When [completed] the episode is additionally marked as played.
     */
    private fun saveAndSync(completed: Boolean) {
        ioScope.launch {
            val media = currentMedia() ?: return@launch
            val position = player.currentPosition
            if (position > 0) {
                media.setPosition(position.toInt())
                media.setLastPlayedTimeStatistics(System.currentTimeMillis())
                media.setLastPlayedTimeHistory(Date(System.currentTimeMillis()))
                DBWriter.setFeedMediaPlaybackInformation(media)
            }
            if (completed) {
                media.item?.let {
                    DBWriter.markItemPlayed(FeedItem.PLAYED, true, it)
                }
            }
            runCatching {
                SynchronizationQueue.getInstance().enqueueEpisodePlayed(media, completed)
            }
        }
    }

    override fun onDestroy() {
        // Final positions are already persisted via the pause / ended listeners.
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        ioScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val POSITION_SAVE_INTERVAL_MS = 15_000L
    }
}
