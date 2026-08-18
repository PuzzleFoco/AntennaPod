package de.danoeh.antennapod.wear.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager
import de.danoeh.antennapod.net.sync.HostnameParser
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationProvider
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue
import de.danoeh.antennapod.storage.database.FeedDatabaseWriter
import de.danoeh.antennapod.storage.preferences.SynchronizationCredentials
import de.danoeh.antennapod.storage.preferences.SynchronizationSettings
import de.danoeh.antennapod.wear.R
import de.danoeh.antennapod.wear.phone.PhoneCredentialSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.Executors

private const val TAG = "NextcloudLogin"
private const val INPUT_KEY_HOST = "nc_host"
private const val POLL_TIMEOUT_MS = 5 * 60 * 1000L
private const val POLL_INTERVAL_MS = 1000L

class NextcloudLoginViewModel(application: Application) : AndroidViewModel(application) {

    private val _host = MutableStateFlow("")
    val host: StateFlow<String> = _host

    private val _state = MutableStateFlow(LoginState.ENTER_HOST)
    val state: StateFlow<LoginState> = _state

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    private val executor = Executors.newSingleThreadExecutor()

    fun setHost(value: String) { _host.value = value }

    fun fetchFromPhone() {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { PhoneCredentialSync.request(getApplication()) }
            _statusMessage.value = getApplication<Application>()
                .getString(if (ok) R.string.wear_check_phone else R.string.wear_phone_unavailable)
        }
    }

    /**
     * Initiates Nextcloud Login v2:
     * 1. POST /index.php/login/v2 to get login URL + poll token
     * 2. Open login URL on phone browser via RemoteActivityHelper
     * 3. Poll the endpoint until user authenticates (returns server, loginName, appPassword)
     */
    fun startLoginFlow(onSuccess: () -> Unit) {
        val hostValue = _host.value.trim()
        if (hostValue.isBlank()) {
            _statusMessage.value = getApplication<Application>()
                .getString(R.string.wear_nc_enter_server)
            return
        }

        viewModelScope.launch {
            _state.value = LoginState.INITIATING
            _statusMessage.value = null
            try {
                val hostname = HostnameParser(hostValue)
                val loginV2Url = URI(
                    hostname.scheme, null, hostname.host, hostname.port,
                    hostname.subfolder + "/index.php/login/v2", null, null
                ).toURL().toString()

                // Step 1: Initiate login flow
                val (loginUrl, token, endpoint) = withContext(Dispatchers.IO) {
                    val client = AntennapodHttpClient.getHttpClient()
                    val body = "".toRequestBody("application/x-www-form-urlencoded".toMediaType())
                    val request = Request.Builder().url(loginV2Url).post(body).build()
                    val response = client.newCall(request).execute()
                    if (response.code != 200) {
                        throw Exception("Server returned ${response.code}")
                    }
                    val json = JSONObject(response.body!!.string())
                    Triple(
                        json.getString("login"),
                        json.getJSONObject("poll").getString("token"),
                        json.getJSONObject("poll").getString("endpoint")
                    )
                }

                // Step 2: Open login URL on phone (or on watch browser)
                _state.value = LoginState.WAITING_FOR_PHONE
                try {
                    val remoteActivityHelper = RemoteActivityHelper(getApplication(), executor)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(loginUrl)
                        addCategory(Intent.CATEGORY_BROWSABLE)
                    }
                    remoteActivityHelper.startRemoteActivity(intent)
                    _statusMessage.value = getApplication<Application>()
                        .getString(R.string.wear_nc_check_phone)
                } catch (e: Exception) {
                    // No phone or no Play Services - try opening on watch
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(loginUrl))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        getApplication<Application>().startActivity(intent)
                        _statusMessage.value = getApplication<Application>()
                            .getString(R.string.wear_nc_login_browser)
                    } catch (e2: Exception) {
                        _statusMessage.value = getApplication<Application>()
                            .getString(R.string.wear_open_on_phone_fallback, loginUrl)
                    }
                }

                // Step 3: Poll for authentication result
                val result = withContext(Dispatchers.IO) {
                    pollForAuth(endpoint, token)
                }

                if (result != null) {
                    val server = result.getString("server")
                    val loginName = result.getString("loginName")
                    val appPassword = result.getString("appPassword")

                    withContext(Dispatchers.IO) {
                        // Save credentials
                        SynchronizationCredentials.setUsername(loginName)
                        SynchronizationCredentials.setPassword(appPassword)
                        SynchronizationCredentials.setHosturl(server)
                        SynchronizationCredentials.setDeviceId(
                            android.os.Build.MODEL.replace(" ", "") + "-wear"
                        )
                        SynchronizationSettings.setSelectedSyncProvider(
                            SynchronizationProvider.NEXTCLOUD_GPODDER.identifier
                        )

                        // Fetch subscriptions
                        try {
                            val syncService = de.danoeh.antennapod.net.sync.nextcloud.NextcloudSyncService(
                                AntennapodHttpClient.getHttpClient(), server, loginName, appPassword
                            )
                            val changes = syncService.getSubscriptionChanges(0)
                            for (feedUrl in changes.added) {
                                if (!feedUrl.startsWith("http")) continue
                                val feed = Feed(feedUrl, null)
                                feed.title = feedUrl
                                feed.state = Feed.STATE_SUBSCRIBED
                                FeedDatabaseWriter.updateFeed(getApplication(), feed, false)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not fetch subscriptions: ${e.message}")
                        }
                    }

                    _state.value = LoginState.SUCCESS
                    _statusMessage.value = getApplication<Application>()
                        .getString(R.string.wear_sync_login_success)

                    // Trigger feed refresh and sync
                    try {
                        FeedUpdateManager.getInstance()?.runOnce(getApplication())
                    } catch (e: Exception) { /* will sync later */ }
                    try {
                        SynchronizationQueue.getInstance().fullSync()
                    } catch (e: Exception) { /* will sync later */ }

                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    _state.value = LoginState.ENTER_HOST
                    _statusMessage.value = getApplication<Application>()
                        .getString(R.string.wear_nc_timeout)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login flow failed", e)
                _state.value = LoginState.ENTER_HOST
                _statusMessage.value = getApplication<Application>()
                    .getString(R.string.wear_sync_login_error, e.message ?: "Unknown error")
            }
        }
    }

    private fun pollForAuth(endpoint: String, token: String): JSONObject? {
        val client = AntennapodHttpClient.getHttpClient()
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < POLL_TIMEOUT_MS) {
            try {
                val body = "token=$token"
                    .toRequestBody("application/x-www-form-urlencoded".toMediaType())
                val request = Request.Builder().url(endpoint).post(body).build()
                val response = client.newCall(request).execute()
                if (response.code == 200) {
                    return JSONObject(response.body!!.string())
                }
                response.close()
            } catch (e: Exception) {
                // Server not ready yet, keep polling
            }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return null
    }

    override fun onCleared() {
        super.onCleared()
        executor.shutdown()
    }

    enum class LoginState {
        ENTER_HOST, INITIATING, WAITING_FOR_PHONE, SUCCESS
    }
}

@Composable
fun NextcloudLoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: NextcloudLoginViewModel = viewModel()
) {
    val host by viewModel.host.collectAsState()
    val state by viewModel.state.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val hostLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val results = android.app.RemoteInput.getResultsFromIntent(result.data ?: return@rememberLauncherForActivityResult)
        results?.getCharSequence(INPUT_KEY_HOST)?.toString()?.let {
            viewModel.setHost(it)
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .onRotaryScrollEvent {
                    coroutineScope.launch { listState.scroll { scrollBy(it.verticalScrollPixels) } }
                    true
                }
                .focusRequester(focusRequester)
                .focusable(),
            state = listState
        ) {
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.wear_sync_nextcloud),
                        style = MaterialTheme.typography.title3
                    )
                }
            }

            when (state) {
                NextcloudLoginViewModel.LoginState.INITIATING -> {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = stringResource(R.string.wear_nc_connecting),
                                style = MaterialTheme.typography.body2,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                NextcloudLoginViewModel.LoginState.WAITING_FOR_PHONE -> {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = stringResource(R.string.wear_nc_waiting),
                                style = MaterialTheme.typography.body2,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                NextcloudLoginViewModel.LoginState.SUCCESS -> {
                    item {
                        Text(
                            text = stringResource(R.string.wear_sync_login_success),
                            style = MaterialTheme.typography.body1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        )
                    }
                }

                NextcloudLoginViewModel.LoginState.ENTER_HOST -> {
                    // Server URL input
                    item {
                        Chip(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val remoteInputs = listOf(
                                    android.app.RemoteInput.Builder(INPUT_KEY_HOST)
                                        .setLabel(context.getString(R.string.wear_sync_host))
                                        .build()
                                )
                                val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                                RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                                hostLauncher.launch(intent)
                            },
                            label = {
                                Text(text = stringResource(R.string.wear_sync_host), maxLines = 1)
                            },
                            secondaryLabel = {
                                Text(
                                    text = host.ifBlank { stringResource(R.string.wear_nextcloud_host_hint) },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (host.isBlank()) MaterialTheme.colors.onSurfaceVariant
                                    else MaterialTheme.colors.onSurface
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

                    // Fetch from phone
                    item {
                        Chip(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.fetchFromPhone() },
                            label = {
                                Text(
                                    text = stringResource(R.string.wear_fetch_from_phone),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            secondaryLabel = {
                                Text(
                                    text = stringResource(R.string.wear_fetch_from_phone_hint),
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

                    // Login button - initiates Login v2 flow
                    item {
                        Chip(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.startLoginFlow(onLoginSuccess) },
                            label = {
                                Text(
                                    text = stringResource(R.string.wear_nc_login_button),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            enabled = host.isNotBlank(),
                            colors = ChipDefaults.chipColors(
                                backgroundColor = MaterialTheme.colors.primary
                            )
                        )
                    }
                }
            }

            statusMessage?.let { msg ->
                item {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }
            }
        }
    }
}
