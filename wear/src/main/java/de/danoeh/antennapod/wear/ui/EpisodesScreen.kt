package de.danoeh.antennapod.wear.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.google.common.util.concurrent.MoreExecutors
import de.danoeh.antennapod.model.feed.Feed
import de.danoeh.antennapod.model.feed.FeedItem
import de.danoeh.antennapod.storage.database.DBReader
import de.danoeh.antennapod.wear.R
import de.danoeh.antennapod.wear.service.WearPlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class EpisodesViewModel(application: Application) : AndroidViewModel(application) {
    private val _feed = MutableStateFlow<Feed?>(null)
    val feed: StateFlow<Feed?> = _feed

    private val _episodes = MutableStateFlow<List<FeedItem>>(emptyList())
    val episodes: StateFlow<List<FeedItem>> = _episodes

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadEpisodes(feedId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val loadedFeed = DBReader.getFeed(feedId, false, 0, Int.MAX_VALUE)
                _feed.value = loadedFeed
                _episodes.value = loadedFeed?.items?.sortedByDescending { it.pubDate } ?: emptyList()
            } catch (e: Exception) {
                _episodes.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun playEpisode(context: Context, episode: FeedItem) {
        val media = episode.media ?: return
        val sessionToken = SessionToken(
            context,
            ComponentName(context, WearPlaybackService::class.java)
        )
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            val controller = controllerFuture.get()
            val mediaItem = MediaItem.Builder()
                .setMediaId(media.id.toString())
                .setUri(media.localFileUrl ?: media.downloadUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(episode.title)
                        .setArtist(feed.value?.title)
                        .build()
                )
                .build()
            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
        }, MoreExecutors.directExecutor())
    }
}

@Composable
fun EpisodesScreen(
    feedId: Long,
    onNavigateToNowPlaying: () -> Unit,
    episodesViewModel: EpisodesViewModel = viewModel()
) {
    val feed by episodesViewModel.feed.collectAsState()
    val episodes by episodesViewModel.episodes.collectAsState()
    val isLoading by episodesViewModel.isLoading.collectAsState()
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current

    androidx.compose.runtime.LaunchedEffect(feedId) {
        episodesViewModel.loadEpisodes(feedId)
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else if (episodes.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.wear_no_episodes),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2
                )
            }
        } else {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                item {
                    ListHeader {
                        Text(
                            text = feed?.title ?: "",
                            style = MaterialTheme.typography.title3,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                items(episodes, key = { it.id }) { episode ->
                    EpisodeChip(
                        episode = episode,
                        onClick = {
                            episodesViewModel.playEpisode(context, episode)
                            onNavigateToNowPlaying()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EpisodeChip(episode: FeedItem, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    Chip(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        label = {
            Text(
                text = episode.title ?: "",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            val parts = mutableListOf<String>()
            episode.pubDate?.let { parts.add(dateFormat.format(it)) }
            episode.media?.let { media ->
                if (media.duration > 0) {
                    val minutes = media.duration / 60000
                    parts.add("${minutes}min")
                }
            }
            if (parts.isNotEmpty()) {
                Text(
                    text = parts.joinToString(" · "),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        colors = ChipDefaults.chipColors(
            backgroundColor = MaterialTheme.colors.surface
        )
    )
}
