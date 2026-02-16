package de.danoeh.antennapod.wear.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import de.danoeh.antennapod.wear.R

@Composable
fun HomeScreen(
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToQueue: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val listState = rememberScalingLazyListState()

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
                        text = "AntennaPod",
                        style = MaterialTheme.typography.title3,
                        color = MaterialTheme.colors.primary
                    )
                }
            }
            item {
                MenuChip(
                    label = stringResource(R.string.wear_now_playing),
                    iconRes = R.drawable.ic_play,
                    onClick = onNavigateToNowPlaying
                )
            }
            item {
                MenuChip(
                    label = stringResource(R.string.wear_subscriptions),
                    iconRes = R.drawable.ic_subscriptions,
                    onClick = onNavigateToSubscriptions
                )
            }
            item {
                MenuChip(
                    label = stringResource(R.string.wear_queue),
                    iconRes = R.drawable.ic_queue,
                    onClick = onNavigateToQueue
                )
            }
            item {
                MenuChip(
                    label = stringResource(R.string.wear_downloads),
                    iconRes = R.drawable.ic_download,
                    onClick = onNavigateToDownloads
                )
            }
            item {
                MenuChip(
                    label = stringResource(R.string.wear_settings),
                    iconRes = R.drawable.ic_settings,
                    onClick = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
fun MenuChip(
    label: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Chip(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )
        },
        icon = {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        },
        colors = ChipDefaults.chipColors(
            backgroundColor = MaterialTheme.colors.surface
        )
    )
}
