package de.danoeh.antennapod.wear.ui

import android.app.Application
import android.widget.Toast
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
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import de.danoeh.antennapod.storage.preferences.SynchronizationSettings
import de.danoeh.antennapod.wear.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
                val syncQueue = de.danoeh.antennapod.net.sync.service.SynchronizationQueueImpl(getApplication())
                syncQueue.fullSync()
            } catch (e: Exception) {
                // Sync error handled by the service
            } finally {
                _isSyncing.value = false
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val syncProvider by settingsViewModel.syncProvider.collectAsState()
    val isSyncing by settingsViewModel.isSyncing.collectAsState()
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current

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

            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { /* Show sync provider info */ },
                    label = {
                        Text(
                            text = when {
                                syncProvider?.contains("gpodder") == true -> stringResource(R.string.wear_sync_gpodder)
                                syncProvider?.contains("nextcloud") == true -> stringResource(R.string.wear_sync_nextcloud)
                                else -> stringResource(R.string.wear_sync_none)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (syncProvider != null) "Connected" else "Set up via phone",
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

            // Sync now button
            if (syncProvider != null) {
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            settingsViewModel.triggerSync()
                            Toast.makeText(context, R.string.wear_sync_now, Toast.LENGTH_SHORT).show()
                        },
                        label = {
                            Text(
                                text = if (isSyncing) stringResource(R.string.wear_loading)
                                else stringResource(R.string.wear_sync_now)
                            )
                        },
                        enabled = !isSyncing,
                        colors = ChipDefaults.chipColors(
                            backgroundColor = MaterialTheme.colors.primary
                        )
                    )
                }
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
