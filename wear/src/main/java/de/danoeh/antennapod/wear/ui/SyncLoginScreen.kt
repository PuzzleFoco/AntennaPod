package de.danoeh.antennapod.wear.ui

import android.app.Application
import android.app.RemoteInput
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import de.danoeh.antennapod.net.common.AntennapodHttpClient
import de.danoeh.antennapod.net.sync.gpoddernet.GpodnetService
import de.danoeh.antennapod.net.sync.nextcloud.NextcloudSyncService
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationProvider
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue
import de.danoeh.antennapod.storage.preferences.SynchronizationCredentials
import de.danoeh.antennapod.storage.preferences.SynchronizationSettings
import de.danoeh.antennapod.wear.R
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

    fun setHost(value: String) { _host.value = value }
    fun setUsername(value: String) { _username.value = value }
    fun setPassword(value: String) { _password.value = value }

    private fun getEffectiveHost(provider: SynchronizationProvider): String {
        return _host.value.ifBlank {
            if (provider == SynchronizationProvider.GPODDER_NET) DEFAULT_GPODDER_HOST else ""
        }
    }

    private fun getDeviceId(): String {
        return android.os.Build.MODEL.replace(" ", "") + "-wear"
    }

    fun openLoginOnPhone(provider: SynchronizationProvider) {
        val url = when (provider) {
            SynchronizationProvider.GPODDER_NET -> "https://gpodder.net/register/"
            SynchronizationProvider.NEXTCLOUD_GPODDER -> "https://apps.nextcloud.com/apps/gpoddersync"
        }
        try {
            val remoteActivityHelper = RemoteActivityHelper(
                getApplication(), Executors.newSingleThreadExecutor()
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            remoteActivityHelper.startRemoteActivity(intent)
            _statusMessage.value = getApplication<Application>()
                .getString(R.string.wear_check_phone)
        } catch (e: Exception) {
            _statusMessage.value = getApplication<Application>()
                .getString(R.string.wear_phone_not_connected)
        }
    }

    fun login(provider: SynchronizationProvider, onSuccess: () -> Unit) {
        if (_username.value.isBlank() || _password.value.isBlank()) {
            _statusMessage.value = "Please enter username and password"
            return
        }

        viewModelScope.launch {
            _isLoggingIn.value = true
            _statusMessage.value = null
            try {
                withContext(Dispatchers.IO) {
                    val hostUrl = getEffectiveHost(provider)
                    val deviceId = getDeviceId()

                    when (provider) {
                        SynchronizationProvider.GPODDER_NET -> {
                            val service = GpodnetService(
                                AntennapodHttpClient.getHttpClient(),
                                hostUrl, deviceId,
                                _username.value, _password.value
                            )
                            service.login()
                            // Try to configure the device
                            try {
                                service.configureDevice(
                                    deviceId, "AntennaPod Wear",
                                    de.danoeh.antennapod.net.sync.gpoddernet.model.GpodnetDevice.DeviceType.MOBILE
                                )
                            } catch (e: Exception) {
                                // Device may already exist, ignore
                            }
                        }
                        SynchronizationProvider.NEXTCLOUD_GPODDER -> {
                            if (hostUrl.isBlank()) {
                                throw Exception("Server URL is required for Nextcloud")
                            }
                            val service = NextcloudSyncService(
                                AntennapodHttpClient.getHttpClient(),
                                hostUrl, _username.value, _password.value
                            )
                            service.login()
                        }
                    }

                    // Save credentials
                    SynchronizationCredentials.setUsername(_username.value)
                    SynchronizationCredentials.setPassword(_password.value)
                    SynchronizationCredentials.setHosturl(hostUrl)
                    SynchronizationCredentials.setDeviceId(deviceId)
                    SynchronizationSettings.setSelectedSyncProvider(provider.identifier)
                }

                _statusMessage.value = getApplication<Application>()
                    .getString(R.string.wear_sync_login_success)

                // Trigger initial sync
                try {
                    SynchronizationQueue.getInstance().fullSync()
                } catch (e: Exception) {
                    // Sync will happen later
                }

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
}

private const val DEFAULT_GPODDER_HOST = "gpodder.net"
private const val INPUT_KEY_HOST = "sync_host"
private const val INPUT_KEY_USERNAME = "sync_username"
private const val INPUT_KEY_PASSWORD = "sync_password"

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

    val hostLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val results = RemoteInput.getResultsFromIntent(result.data)
        results?.getCharSequence(INPUT_KEY_HOST)?.toString()?.let {
            syncLoginViewModel.setHost(it)
        }
    }

    val usernameLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val results = RemoteInput.getResultsFromIntent(result.data)
        results?.getCharSequence(INPUT_KEY_USERNAME)?.toString()?.let {
            syncLoginViewModel.setUsername(it)
        }
    }

    val passwordLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val results = RemoteInput.getResultsFromIntent(result.data)
        results?.getCharSequence(INPUT_KEY_PASSWORD)?.toString()?.let {
            syncLoginViewModel.setPassword(it)
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
                        text = if (provider == SynchronizationProvider.GPODDER_NET)
                            stringResource(R.string.wear_sync_gpodder)
                        else stringResource(R.string.wear_sync_nextcloud),
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
                // Open on phone button
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { syncLoginViewModel.openLoginOnPhone(provider) },
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

                // Server URL field
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val remoteInputs = listOf(
                                RemoteInput.Builder(INPUT_KEY_HOST)
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
                                text = host.ifBlank {
                                    if (provider == SynchronizationProvider.GPODDER_NET)
                                        DEFAULT_GPODDER_HOST else "your-server.com"
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (host.isBlank()) MaterialTheme.colors.onSurfaceVariant
                                else MaterialTheme.colors.onSurface
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
                                RemoteInput.Builder(INPUT_KEY_USERNAME)
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
                                RemoteInput.Builder(INPUT_KEY_PASSWORD)
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
                            syncLoginViewModel.login(provider) {
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
