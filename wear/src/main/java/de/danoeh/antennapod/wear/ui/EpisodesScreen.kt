package de.danoeh.antennapod.wear.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
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
import de.danoeh.antennapod.wear.service.WearDownloadService
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
            // Seek to synced position from gpodder/nextcloud if available
            val savedPosition = media.position
            if (savedPosition > 0 && savedPosition < media.duration) {
                controller.seekTo(savedPosition.toLong())
            }
            controller.play()
            // The service owns the player; release our one-shot controller connection
            MediaController.releaseFuture(controllerFuture)
        }, MoreExecutors.directExecutor())
    }

    fun downloadEpisode(context: Context, episode: FeedItem) {
        val media = episode.media ?: return
        val url = media.downloadUrl ?: return
        if (url.isBlank()) return
        WearDownloadService.startDownload(context, url, episode.title ?: "", media.id)
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
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val scrollFocusModifier = Modifier
        .fillMaxSize()
        .onRotaryScrollEvent {
            coroutineScope.launch { listState.scroll { scrollBy(it.verticalScrollPixels) } }
            true
        }
        .focusRequester(focusRequester)
        .focusable()

    androidx.compose.runtime.LaunchedEffect(feedId) {
        episodesViewModel.loadEpisodes(feedId)
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        if (isLoading) {
            Column(
                modifier = scrollFocusModifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else if (episodes.isEmpty()) {
            Column(
                modifier = scrollFocusModifier,
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
                modifier = scrollFocusModifier,
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EpisodeChip(
                            episode = episode,
                            onClick = {
                                episodesViewModel.playEpisode(context, episode)
                                onNavigateToNowPlaying()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                episodesViewModel.downloadEpisode(context, episode)
                                Toast.makeText(context, R.string.wear_downloading, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(34.dp),
                            colors = ButtonDefaults.secondaryButtonColors()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_download),
                                contentDescription = stringResource(R.string.wear_download),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeChip(episode: FeedItem, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val media = episode.media

    Chip(
        modifier = modifier,
        onClick = onClick,
        label = {
            Text(
                text = episode.title ?: "",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Column {
                val parts = mutableListOf<String>()
                episode.pubDate?.let { parts.add(dateFormat.format(it)) }
                if (media != null && media.duration > 0) {
                    val minutes = media.duration / 60000
                    parts.add("${minutes}min")
                }
                // Show played state
                if (episode.isPlayed) {
                    parts.add("✓ Played")
                } else if (media != null && media.position > 0 && media.duration > 0) {
                    val remainingMin = (media.duration - media.position) / 60000
                    parts.add("${remainingMin}min left")
                }
                if (parts.isNotEmpty()) {
                    Text(
                        text = parts.joinToString(" · "),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Show progress bar if partially played
                if (media != null && media.position > 0 && media.duration > 0 && !episode.isPlayed) {
                    val playProgress = media.position.toFloat() / media.duration.toFloat()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                            .height(3.dp)
                            .background(MaterialTheme.colors.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(playProgress)
                                .height(3.dp)
                                .background(MaterialTheme.colors.primary)
                        )
                    }
                }
            }
        },
        colors = ChipDefaults.chipColors(
            backgroundColor = if (episode.isPlayed) MaterialTheme.colors.surface.copy(alpha = 0.5f)
            else MaterialTheme.colors.surface
        )
    )
}
