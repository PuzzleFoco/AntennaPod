package de.danoeh.antennapod.wear.ui

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue
import de.danoeh.antennapod.storage.preferences.SynchronizationCredentials
import de.danoeh.antennapod.storage.preferences.SynchronizationSettings
import de.danoeh.antennapod.wear.R
import de.danoeh.antennapod.wear.phone.PhoneCredentialSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val _syncProvider = MutableStateFlow<String?>(null)
    val syncProvider: StateFlow<String?> = _syncProvider

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    init {
        loadSyncSettings()
    }

    fun loadSyncSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val provider = SynchronizationSettings.getSelectedSyncProviderKey()
                _syncProvider.value = provider
            } catch (e: Exception) {
                _syncProvider.value = null
            }
        }
    }

    fun triggerSync() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            try {
                SynchronizationQueue.getInstance().fullSync()
                FeedUpdateManager.getInstance()?.runOnce(getApplication())
            } catch (e: Exception) {
                // Sync error handled by the service
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun fetchFromPhone() {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                PhoneCredentialSync.request(getApplication())
            }
            Toast.makeText(
                getApplication(),
                if (ok) R.string.wear_check_phone else R.string.wear_phone_unavailable,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            SynchronizationCredentials.clear()
            SynchronizationSettings.setSelectedSyncProvider(null)
            SynchronizationSettings.resetTimestamps()
            _syncProvider.value = null
        }
    }
}

@Composable
fun SettingsScreen(
    onNavigateToSyncLogin: (String) -> Unit,
    onNavigateToNextcloudLogin: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val syncProvider by settingsViewModel.syncProvider.collectAsState()
    val isSyncing by settingsViewModel.isSyncing.collectAsState()
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        settingsViewModel.loadSyncSettings()
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
                        text = stringResource(R.string.wear_settings),
                        style = MaterialTheme.typography.title3
                    )
                }
            }

            // Sync section
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.wear_sync),
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.primary
                    )
                }
            }

            if (syncProvider != null) {
                // Already connected
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { },
                        label = {
                            Text(
                                text = when {
                                    syncProvider?.contains("GPODDER_NET") == true ->
                                        stringResource(R.string.wear_sync_gpodder)
                                    syncProvider?.contains("NEXTCLOUD") == true ->
                                        stringResource(R.string.wear_sync_nextcloud)
                                    else -> syncProvider ?: ""
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        secondaryLabel = {
                            val user = SynchronizationCredentials.getUsername()
                            Text(
                                text = if (user != null) "Connected as $user" else "Connected",
                                maxLines = 1
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_sync),
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
                        onClick = { settingsViewModel.fetchFromPhone() },
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

                // Sync now
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            settingsViewModel.triggerSync()
                            Toast.makeText(context, R.string.wear_syncing, Toast.LENGTH_SHORT).show()
                        },
                        label = {
                            Text(
                                text = if (isSyncing) stringResource(R.string.wear_syncing)
                                else stringResource(R.string.wear_sync_now),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        enabled = !isSyncing,
                        colors = ChipDefaults.chipColors(
                            backgroundColor = MaterialTheme.colors.primary
                        )
                    )
                }

                // Logout
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            settingsViewModel.logout()
                            Toast.makeText(context, R.string.wear_logged_out, Toast.LENGTH_SHORT).show()
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.wear_sync_logout),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = MaterialTheme.colors.surface
                        )
                    )
                }
            } else {
                // Not connected - show login options
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onNavigateToSyncLogin("GPODDER_NET")
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.wear_sync_gpodder),
                                maxLines = 1
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = stringResource(R.string.wear_sync_login),
                                maxLines = 1
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_sync),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = MaterialTheme.colors.surface
                        )
                    )
                }

                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onNavigateToNextcloudLogin,
                        label = {
                            Text(
                                text = stringResource(R.string.wear_sync_nextcloud),
                                maxLines = 1
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = stringResource(R.string.wear_nc_login_v2),
                                maxLines = 1
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_sync),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = MaterialTheme.colors.surface
                        )
                    )
                }
            }

                // Fetch from phone (not connected)
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { settingsViewModel.fetchFromPhone() },
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

            // About section
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.wear_about),
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.primary
                    )
                }
            }

            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { },
                    label = {
                        Text(
                            text = stringResource(
                                R.string.wear_version,
                                de.danoeh.antennapod.wear.BuildConfig.VERSION_NAME
                            )
                        )
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = MaterialTheme.colors.surface
                    )
                )
            }
        }
    }
}
