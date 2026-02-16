package de.danoeh.antennapod.wear.ui

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import de.danoeh.antennapod.model.feed.Feed
import de.danoeh.antennapod.storage.database.DBReader
import de.danoeh.antennapod.wear.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SubscriptionsViewModel(application: Application) : AndroidViewModel(application) {
    private val _feeds = MutableStateFlow<List<Feed>>(emptyList())
    val feeds: StateFlow<List<Feed>> = _feeds

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadFeeds()
    }

    fun loadFeeds() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val feedList = DBReader.getFeedList()
                _feeds.value = feedList.filter { it.state == Feed.STATE_SUBSCRIBED }
            } catch (e: Exception) {
                _feeds.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@Composable
fun SubscriptionsScreen(
    onFeedClick: (Long) -> Unit,
    onNavigateToAddPodcast: () -> Unit,
    subscriptionsViewModel: SubscriptionsViewModel = viewModel()
) {
    val feeds by subscriptionsViewModel.feeds.collectAsState()
    val isLoading by subscriptionsViewModel.isLoading.collectAsState()
    val listState = rememberScalingLazyListState()

    // Reload feeds every time this screen becomes visible (e.g. after login/sync)
    LaunchedEffect(Unit) {
        subscriptionsViewModel.loadFeeds()
    }

    // Periodically reload feeds to pick up sync results from background WorkManager
    LaunchedEffect(Unit) {
        while (true) {
            delay(30000)
            subscriptionsViewModel.loadFeeds()
        }
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
                Text(
                    text = stringResource(R.string.wear_loading),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.body2
                )
            }
        } else if (feeds.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.wear_no_subscriptions),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(16.dp)
                )
                Chip(
                    onClick = onNavigateToAddPodcast,
                    label = {
                        Text(text = stringResource(R.string.wear_add_podcast))
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = MaterialTheme.colors.primary
                    )
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
                            text = stringResource(R.string.wear_subscriptions),
                            style = MaterialTheme.typography.title3
                        )
                    }
                }
                items(feeds, key = { it.id }) { feed ->
                    FeedChip(
                        feed = feed,
                        onClick = { onFeedClick(feed.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun FeedChip(feed: Feed, onClick: () -> Unit) {
    Chip(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        label = {
            Text(
                text = feed.title ?: "",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            val episodeCount = feed.items?.size ?: 0
            if (episodeCount > 0) {
                Text(
                    text = "$episodeCount episodes",
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
