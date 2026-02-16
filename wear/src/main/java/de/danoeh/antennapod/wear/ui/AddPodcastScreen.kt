package de.danoeh.antennapod.wear.ui

import android.app.Application
import android.app.RemoteInput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
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
import androidx.wear.input.RemoteInputIntentHelper
import de.danoeh.antennapod.model.feed.Feed
import de.danoeh.antennapod.net.common.AntennapodHttpClient
import de.danoeh.antennapod.storage.database.FeedDatabaseWriter
import de.danoeh.antennapod.wear.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory

class AddPodcastViewModel(application: Application) : AndroidViewModel(application) {
    private val _isSubscribing = MutableStateFlow(false)
    val isSubscribing: StateFlow<Boolean> = _isSubscribing

    private val _resultMessage = MutableStateFlow<String?>(null)
    val resultMessage: StateFlow<String?> = _resultMessage

    fun subscribeToPodcast(url: String, onSuccess: () -> Unit) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _isSubscribing.value = true
            _resultMessage.value = null
            try {
                val feedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    "https://$url"
                } else {
                    url
                }

                withContext(Dispatchers.IO) {
                    val feed = Feed(feedUrl, null)
                    feed.title = feedUrl
                    feed.state = Feed.STATE_SUBSCRIBED

                    // Try to download and parse the feed to get proper title and items
                    try {
                        val client = AntennapodHttpClient.getHttpClient()
                        val request = Request.Builder().url(feedUrl).build()
                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            if (body != null) {
                                // Extract title from XML
                                val titleMatch = Regex("<title>([^<]+)</title>").find(body)
                                if (titleMatch != null) {
                                    feed.title = titleMatch.groupValues[1].trim()
                                }
                            }
                        }
                        response.close()
                    } catch (e: Exception) {
                        // If we can't download, we still save the feed URL
                    }

                    FeedDatabaseWriter.updateFeed(
                        getApplication(), feed, false
                    )
                }

                _resultMessage.value = getApplication<Application>()
                    .getString(R.string.wear_subscribe_success)
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                _resultMessage.value = getApplication<Application>()
                    .getString(R.string.wear_subscribe_error, e.message ?: "Unknown error")
            } finally {
                _isSubscribing.value = false
            }
        }
    }
}

private const val INPUT_KEY_URL = "podcast_url"

@Composable
fun AddPodcastScreen(
    onSubscribed: () -> Unit,
    addPodcastViewModel: AddPodcastViewModel = viewModel()
) {
    val isSubscribing by addPodcastViewModel.isSubscribing.collectAsState()
    val resultMessage by addPodcastViewModel.resultMessage.collectAsState()
    var enteredUrl by remember { mutableStateOf("") }
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current

    val inputLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val results = RemoteInput.getResultsFromIntent(result.data)
        val url = results?.getCharSequence(INPUT_KEY_URL)?.toString() ?: ""
        if (url.isNotBlank()) {
            enteredUrl = url
            addPodcastViewModel.subscribeToPodcast(url) {
                onSubscribed()
            }
        }
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.wear_add_podcast),
                        style = MaterialTheme.typography.title3
                    )
                }
            }

            if (isSubscribing) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.wear_subscribing),
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val remoteInputs = listOf(
                                RemoteInput.Builder(INPUT_KEY_URL)
                                    .setLabel(context.getString(R.string.wear_add_podcast_hint))
                                    .build()
                            )
                            val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                            RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                            inputLauncher.launch(intent)
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.wear_enter_url),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = if (enteredUrl.isNotBlank()) enteredUrl
                                else stringResource(R.string.wear_add_podcast_hint),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = MaterialTheme.colors.primary
                        )
                    )
                }

                resultMessage?.let { msg ->
                    item {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.body2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}
