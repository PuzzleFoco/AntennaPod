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
import de.danoeh.antennapod.net.sync.gpoddernet.GpodnetService
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationProvider
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue
import de.danoeh.antennapod.storage.database.FeedDatabaseWriter
import de.danoeh.antennapod.storage.preferences.SynchronizationCredentials
import de.danoeh.antennapod.storage.preferences.SynchronizationSettings
import de.danoeh.antennapod.wear.R
import de.danoeh.antennapod.wear.phone.PhoneCredentialSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class SyncLoginViewModel(application: Application) : AndroidViewModel(application) {
    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    private val _host = MutableStateFlow("")
    val host: StateFlow<String> = _host

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val executor = Executors.newSingleThreadExecutor()

    fun setHost(value: String) { _host.value = value }
    fun setUsername(value: String) { _username.value = value }
    fun setPassword(value: String) { _password.value = value }

    private fun getEffectiveHost(): String {
        return _host.value.ifBlank { DEFAULT_GPODDER_HOST }
    }

    private fun getDeviceId(): String {
        return android.os.Build.MODEL.replace(" ", "") + "-wear"
    }

    fun openLoginOnPhone() {
        try {
            val remoteActivityHelper = RemoteActivityHelper(getApplication(), executor)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://gpodder.net/register/")
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            remoteActivityHelper.startRemoteActivity(intent)
            _statusMessage.value = getApplication<Application>()
                .getString(R.string.wear_check_phone)
        } catch (e: Exception) {
            _statusMessage.value = getApplication<Application>()
                .getString(R.string.wear_open_on_phone_fallback, "https://gpodder.net/register/")
        }
    }

    fun fetchFromPhone() {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { PhoneCredentialSync.request(getApplication()) }
            _statusMessage.value = getApplication<Application>()
                .getString(if (ok) R.string.wear_check_phone else R.string.wear_phone_unavailable)
        }
    }

    fun login(onSuccess: () -> Unit) {
        if (_username.value.isBlank() || _password.value.isBlank()) {
            _statusMessage.value = getApplication<Application>()
                .getString(R.string.wear_enter_credentials)
            return
        }

        viewModelScope.launch {
            _isLoggingIn.value = true
            _statusMessage.value = null
            try {
                withContext(Dispatchers.IO) {
                    val hostUrl = getEffectiveHost()
                    val deviceId = getDeviceId()

                    val service = GpodnetService(
                        AntennapodHttpClient.getHttpClient(),
                        hostUrl, deviceId,
                        _username.value, _password.value
                    )
                    service.login()
                    try {
                        service.configureDevice(
                            deviceId, "AntennaPod Wear",
                            de.danoeh.antennapod.net.sync.gpoddernet.model.GpodnetDevice.DeviceType.MOBILE
                        )
                    } catch (e: Exception) {
                        // Device may already exist
                    }
                    // Fetch subscriptions
                    try {
                        val changes = service.getSubscriptionChanges(0)
                        for (feedUrl in changes.added) {
                            if (!feedUrl.startsWith("http")) continue
                            val feed = Feed(feedUrl, null)
                            feed.title = feedUrl
                            feed.state = Feed.STATE_SUBSCRIBED
                            FeedDatabaseWriter.updateFeed(getApplication(), feed, false)
                        }
                    } catch (e: Exception) {
                        Log.w("SyncLogin", "Could not fetch subscriptions: ${e.message}")
                    }

                    // Save credentials
                    SynchronizationCredentials.setUsername(_username.value)
                    SynchronizationCredentials.setPassword(_password.value)
                    SynchronizationCredentials.setHosturl(hostUrl)
                    SynchronizationCredentials.setDeviceId(deviceId)
                    SynchronizationSettings.setSelectedSyncProvider(
                        SynchronizationProvider.GPODDER_NET.identifier
                    )
                }

                _statusMessage.value = getApplication<Application>()
                    .getString(R.string.wear_sync_login_success)

                try {
                    FeedUpdateManager.getInstance()?.runOnce(getApplication())
                } catch (e: Exception) {
                    Log.w("SyncLogin", "Feed refresh failed: ${e.message}")
                }
                try {
                    SynchronizationQueue.getInstance().fullSync()
                } catch (e: Exception) { /* will sync later */ }

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                _statusMessage.value = getApplication<Application>()
                    .getString(R.string.wear_sync_login_error, e.message ?: "Unknown error")
            } finally {
                _isLoggingIn.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        executor.shutdown()
    }
}

private const val DEFAULT_GPODDER_HOST = "gpodder.net"
private const val INPUT_KEY_HOST = "sync_host"
private const val INPUT_KEY_USERNAME = "sync_username"
private const val INPUT_KEY_PASSWORD = "sync_password"

@Suppress("UNUSED_PARAMETER")
@Composable
fun SyncLoginScreen(
    provider: SynchronizationProvider,
    onLoginSuccess: () -> Unit,
    syncLoginViewModel: SyncLoginViewModel = viewModel()
) {
    val isLoggingIn by syncLoginViewModel.isLoggingIn.collectAsState()
    val statusMessage by syncLoginViewModel.statusMessage.collectAsState()
    val host by syncLoginViewModel.host.collectAsState()
    val username by syncLoginViewModel.username.collectAsState()
    val password by syncLoginViewModel.password.collectAsState()
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val hostLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val results = android.app.RemoteInput.getResultsFromIntent(result.data ?: return@rememberLauncherForActivityResult)
        results?.getCharSequence(INPUT_KEY_HOST)?.toString()?.let {
            syncLoginViewModel.setHost(it)
        }
    }

    val usernameLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val results = android.app.RemoteInput.getResultsFromIntent(result.data ?: return@rememberLauncherForActivityResult)
        results?.getCharSequence(INPUT_KEY_USERNAME)?.toString()?.let {
            syncLoginViewModel.setUsername(it)
        }
    }

    val passwordLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val results = android.app.RemoteInput.getResultsFromIntent(result.data ?: return@rememberLauncherForActivityResult)
        results?.getCharSequence(INPUT_KEY_PASSWORD)?.toString()?.let {
            syncLoginViewModel.setPassword(it)
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
                        text = stringResource(R.string.wear_sync_gpodder),
                        style = MaterialTheme.typography.title3
                    )
                }
            }

            if (isLoggingIn) {
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
                            text = stringResource(R.string.wear_sync_logging_in),
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                // Server URL field
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
                            Text(
                                text = stringResource(R.string.wear_sync_host),
                                maxLines = 1
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = host.ifBlank { DEFAULT_GPODDER_HOST },
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

                // Username field
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val remoteInputs = listOf(
                                android.app.RemoteInput.Builder(INPUT_KEY_USERNAME)
                                    .setLabel(context.getString(R.string.wear_sync_username))
                                    .build()
                            )
                            val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                            RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                            usernameLauncher.launch(intent)
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.wear_sync_username),
                                maxLines = 1
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = username.ifBlank { stringResource(R.string.wear_enter_username) },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (username.isBlank()) MaterialTheme.colors.onSurfaceVariant
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

                // Password field
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val remoteInputs = listOf(
                                android.app.RemoteInput.Builder(INPUT_KEY_PASSWORD)
                                    .setLabel(context.getString(R.string.wear_sync_password))
                                    .build()
                            )
                            val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                            RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                            passwordLauncher.launch(intent)
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.wear_sync_password),
                                maxLines = 1
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = if (password.isBlank()) stringResource(R.string.wear_enter_password)
                                else "••••••••",
                                maxLines = 1,
                                color = if (password.isBlank()) MaterialTheme.colors.onSurfaceVariant
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

                // Login button
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            syncLoginViewModel.login {
                                onLoginSuccess()
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.wear_sync_login),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        enabled = username.isNotBlank() && password.isNotBlank(),
                        colors = ChipDefaults.chipColors(
                            backgroundColor = MaterialTheme.colors.primary
                        )
                    )
                }

                // Fetch from phone
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { syncLoginViewModel.fetchFromPhone() },
                        label = {
                            Text(
                                text = stringResource(R.string.wear_fetch_from_phone),
                                maxLines = 1
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

                // Open on phone button
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { syncLoginViewModel.openLoginOnPhone() },
                        label = {
                            Text(
                                text = stringResource(R.string.wear_open_on_phone),
                                maxLines = 1
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = stringResource(R.string.wear_open_on_phone_hint),
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

                // Status message
                statusMessage?.let { msg ->
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
