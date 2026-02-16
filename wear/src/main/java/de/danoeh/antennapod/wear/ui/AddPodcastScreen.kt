package de.danoeh.antennapod.wear.ui

import android.app.Application
import android.app.RemoteInput
import android.content.Intent
import android.net.Uri
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
import androidx.wear.remote.interactions.RemoteActivityHelper
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
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.StringReader
import java.util.concurrent.Executors
import javax.xml.parsers.SAXParserFactory

class AddPodcastViewModel(application: Application) : AndroidViewModel(application) {
    private val _isSubscribing = MutableStateFlow(false)
    val isSubscribing: StateFlow<Boolean> = _isSubscribing

    private val _resultMessage = MutableStateFlow<String?>(null)
    val resultMessage: StateFlow<String?> = _resultMessage

    fun openSearchOnPhone() {
        try {
            val remoteActivityHelper = RemoteActivityHelper(
                getApplication(), Executors.newSingleThreadExecutor()
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://gpodder.net/search")
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            remoteActivityHelper.startRemoteActivity(intent)
            _resultMessage.value = getApplication<Application>()
                .getString(R.string.wear_check_phone)
        } catch (e: Exception) {
            _resultMessage.value = getApplication<Application>()
                .getString(R.string.wear_phone_not_connected)
        }
    }

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
                                // Extract title using SAX parser
                                val title = parseFeedTitle(body)
                                if (title != null) {
                                    feed.title = title
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

    private fun parseFeedTitle(xml: String): String? {
        var title: String? = null
        try {
            val factory = SAXParserFactory.newInstance()
            val parser = factory.newSAXParser()
            parser.parse(InputSource(StringReader(xml)), object : DefaultHandler() {
                private var insideTitle = false
                private var depth = 0
                private val chars = StringBuilder()

                override fun startElement(
                    uri: String?, localName: String?,
                    qName: String?, attributes: Attributes?
                ) {
                    depth++
                    if ((localName.equals("title", ignoreCase = true)
                                || qName.equals("title", ignoreCase = true)) && depth <= 3
                    ) {
                        insideTitle = true
                        chars.setLength(0)
                    }
                }

                override fun characters(ch: CharArray, start: Int, length: Int) {
                    if (insideTitle) {
                        chars.append(ch, start, length)
                    }
                }

                override fun endElement(uri: String?, localName: String?, qName: String?) {
                    if (insideTitle) {
                        title = chars.toString().trim()
                        insideTitle = false
                        throw StopParsingException()
                    }
                    depth--
                }
            })
        } catch (e: StopParsingException) {
            // Expected: we stop after finding the first title
        } catch (e: Exception) {
            // Parsing failed, return null
        }
        return title
    }

    private class StopParsingException : RuntimeException()
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
                // Enter URL on watch
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

                // Search on phone
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { addPodcastViewModel.openSearchOnPhone() },
                        label = {
                            Text(
                                text = stringResource(R.string.wear_search_on_phone),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = stringResource(R.string.wear_search_on_phone_hint),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_phone),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = MaterialTheme.colors.surface
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
