package com.example.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.BlueStreamPrimary
import com.example.ui.theme.BlueStreamSecondary
import kotlinx.coroutines.delay

@Composable
fun BlueStreamVideoPlayer(
    videoUrl: String,
    thumbnailUrl: String?,
    category: String,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    var isPlaying by remember(videoUrl) { mutableStateOf(true) }
    var isBuffering by remember(videoUrl) { mutableStateOf(true) }
    var hasError by remember(videoUrl) { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var positionMs by remember(videoUrl) { mutableLongStateOf(0L) }
    var durationMs by remember(videoUrl) { mutableLongStateOf(0L) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    // Poll playback progress smoothly
    LaunchedEffect(videoUrl, isPlaying, reloadNonce) {
        while (true) {
            val view = videoViewRef
            if (view != null && !hasError) {
                runCatching {
                    val dur = view.duration.toLong()
                    if (dur > 0) durationMs = dur
                    positionMs = view.currentPosition.toLong().coerceAtLeast(0L)
                }
            }
            delay(500L)
        }
    }

    // Auto-hide controls after 3.5 seconds while playing
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying && !hasError && !isBuffering) {
            delay(3500L)
            showControls = false
        }
    }

    DisposableEffect(videoUrl) {
        onDispose {
            runCatching { videoViewRef?.stopPlayback() }
            mediaPlayerRef = null
        }
    }

    val containerModifier = if (isFullscreen) {
        modifier.fillMaxSize().background(Color.Black)
    } else {
        modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
    }

    Box(
        modifier = containerModifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            showControls = !showControls
        }
    ) {
        if (hasError) {
            VideoThumbnailImage(
                thumbnailUrl = thumbnailUrl,
                category = category,
                contentDescription = "Video preview",
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC060E1F)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Unable to stream this video source",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Device-local uploads play on the uploading device. Use a direct HTTPS MP4 URL for cross-device streaming.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB8CEFF),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                    )
                    Button(
                        onClick = {
                            hasError = false
                            isBuffering = true
                            reloadNonce++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueStreamPrimary),
                        modifier = Modifier.testTag("player_retry_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Retry Playback")
                    }
                }
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setOnPreparedListener { mp ->
                            mediaPlayerRef = mp
                            mp.isLooping = false
                            val vol = if (isMuted) 0f else 1f
                            mp.setVolume(vol, vol)
                            durationMs = mp.duration.toLong().coerceAtLeast(0L)
                            isBuffering = false
                            hasError = false
                            start()
                            isPlaying = true
                        }
                        setOnInfoListener { _, what, _ ->
                            when (what) {
                                MediaPlayer.MEDIA_INFO_BUFFERING_START -> isBuffering = true
                                MediaPlayer.MEDIA_INFO_BUFFERING_END -> isBuffering = false
                            }
                            false
                        }
                        setOnCompletionListener {
                            isPlaying = false
                            showControls = true
                        }
                        setOnErrorListener { _, _, _ ->
                            isBuffering = false
                            hasError = true
                            true
                        }
                        setVideoURI(Uri.parse(videoUrl))
                        videoViewRef = this
                    }
                },
                update = { view ->
                    if (view.tag != "$videoUrl|$reloadNonce") {
                        view.tag = "$videoUrl|$reloadNonce"
                        isBuffering = true
                        hasError = false
                        view.setVideoURI(Uri.parse(videoUrl))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isBuffering && !hasError) {
            CircularProgressIndicator(
                color = BlueStreamSecondary,
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.Center)
            )
        }

        // Custom BlueStream Transport Overlay
        AnimatedVisibility(
            visible = showControls && !hasError,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Black.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.72f)
                            )
                        )
                    )
            ) {
                // Center Playback Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val target = (positionMs - 10_000L).coerceAtLeast(0L).toInt()
                            videoViewRef?.seekTo(target)
                            positionMs = target.toLong()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .testTag("player_rewind_10")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "Rewind 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val view = videoViewRef ?: return@IconButton
                            if (view.isPlaying) {
                                view.pause()
                                isPlaying = false
                            } else {
                                view.start()
                                isPlaying = true
                            }
                        },
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(BlueStreamPrimary.copy(alpha = 0.92f))
                            .testTag("player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val maxDur = durationMs.coerceAtLeast(positionMs)
                            val target = (positionMs + 10_000L).coerceAtMost(maxDur).toInt()
                            videoViewRef?.seekTo(target)
                            positionMs = target.toLong()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .testTag("player_forward_10")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Scrub Bar & Utility Actions
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val sliderMax = durationMs.coerceAtLeast(1000L).toFloat()
                    Slider(
                        value = positionMs.coerceIn(0L, sliderMax.toLong()).toFloat(),
                        onValueChange = { newVal ->
                            positionMs = newVal.toLong()
                            videoViewRef?.seekTo(newVal.toInt())
                        },
                        valueRange = 0f..sliderMax,
                        colors = SliderDefaults.colors(
                            thumbColor = BlueStreamSecondary,
                            activeTrackColor = BlueStreamPrimary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${formatDuration(positionMs / 1000L)} / ${formatDuration(durationMs / 1000L)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    isMuted = !isMuted
                                    val vol = if (isMuted) 0f else 1f
                                    runCatching { mediaPlayerRef?.setVolume(vol, vol) }
                                },
                                modifier = Modifier.testTag("player_mute_button")
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff
                                    else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = if (isMuted) "Unmute" else "Mute",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = onToggleFullscreen,
                                modifier = Modifier.testTag("player_fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Filled.FullscreenExit
                                    else Icons.Filled.Fullscreen,
                                    contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
