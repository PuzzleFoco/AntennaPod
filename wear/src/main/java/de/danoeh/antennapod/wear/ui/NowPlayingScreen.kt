package de.danoeh.antennapod.wear.ui

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import de.danoeh.antennapod.wear.R
import de.danoeh.antennapod.wear.service.WearPlaybackService
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun NowPlayingScreen() {
    val context = LocalContext.current
    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var progress by remember { mutableFloatStateOf(0f) }
    var hasMedia by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    DisposableEffect(Unit) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, WearPlaybackService::class.java)
        )
        val controllerFuture: ListenableFuture<MediaController> =
            MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            val controller = controllerFuture.get()
            mediaController = controller
            updatePlaybackState(controller) { playing, t, a, pos, dur, has ->
                isPlaying = playing
                title = t
                artist = a
                currentPosition = pos
                duration = dur
                hasMedia = has
                progress = if (dur > 0) pos.toFloat() / dur.toFloat() else 0f
            }

            controller.addListener(object : Player.Listener {
                override fun onMediaMetadataChanged(metadata: MediaMetadata) {
                    title = metadata.title?.toString() ?: ""
                    artist = metadata.artist?.toString() ?: ""
                    hasMedia = true
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    hasMedia = controller.mediaItemCount > 0
                }
            })

            // Position update loop
            scope.launch {
                while (isActive) {
                    delay(1000)
                    controller.let { ctrl ->
                        currentPosition = ctrl.currentPosition
                        duration = ctrl.duration.coerceAtLeast(0)
                        progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                    }
                }
            }
        }, MoreExecutors.directExecutor())

        onDispose {
            MediaController.releaseFuture(controllerFuture)
        }
    }

    Scaffold(
        timeText = { TimeText() }
    ) {
        if (!hasMedia) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onRotaryScrollEvent {
                        val direction = if (it.verticalScrollPixels > 0) AudioManager.ADJUST_RAISE
                            else AudioManager.ADJUST_LOWER
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI
                        )
                        true
                    }
                    .focusRequester(focusRequester)
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.wear_nothing_playing),
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Progress indicator around the screen + volume via crown
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onRotaryScrollEvent {
                            val direction = if (it.verticalScrollPixels > 0) AudioManager.ADJUST_RAISE
                                else AudioManager.ADJUST_LOWER
                            audioManager.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI
                            )
                            true
                        }
                        .focusRequester(focusRequester)
                        .focusable()
                ) {
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxSize(),
                        startAngle = 270f,
                        indicatorColor = MaterialTheme.colors.primary,
                        trackColor = MaterialTheme.colors.surface,
                        strokeWidth = 4.dp
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title
                        Text(
                            text = title,
                            style = MaterialTheme.typography.body1,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Artist
                        if (artist.isNotEmpty()) {
                            Text(
                                text = artist,
                                style = MaterialTheme.typography.caption2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colors.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Time
                        Text(
                            text = formatTime(currentPosition) + " / " + formatTime(duration),
                            style = MaterialTheme.typography.caption2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Skip back
                            Button(
                                onClick = {
                                    mediaController?.seekTo(
                                        (mediaController?.currentPosition ?: 0) - 10000
                                    )
                                },
                                modifier = Modifier.size(36.dp),
                                colors = ButtonDefaults.secondaryButtonColors()
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_skip_back),
                                    contentDescription = stringResource(R.string.wear_skip_back),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Play/Pause
                            Button(
                                onClick = {
                                    mediaController?.let { ctrl ->
                                        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
                                    }
                                },
                                modifier = Modifier.size(48.dp),
                                colors = ButtonDefaults.primaryButtonColors()
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                                    ),
                                    contentDescription = stringResource(
                                        if (isPlaying) R.string.wear_pause else R.string.wear_play
                                    ),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Skip forward
                            Button(
                                onClick = {
                                    mediaController?.seekTo(
                                        (mediaController?.currentPosition ?: 0) + 30000
                                    )
                                },
                                modifier = Modifier.size(36.dp),
                                colors = ButtonDefaults.secondaryButtonColors()
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_skip_forward),
                                    contentDescription = stringResource(R.string.wear_skip_forward),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0) return "0:00"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

private fun updatePlaybackState(
    controller: MediaController,
    callback: (Boolean, String, String, Long, Long, Boolean) -> Unit
) {
    val isPlaying = controller.isPlaying
    val metadata = controller.mediaMetadata
    val title = metadata.title?.toString() ?: ""
    val artist = metadata.artist?.toString() ?: ""
    val position = controller.currentPosition
    val duration = controller.duration.coerceAtLeast(0)
    val hasMedia = controller.mediaItemCount > 0
    callback(isPlaying, title, artist, position, duration, hasMedia)
}
