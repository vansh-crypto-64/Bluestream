package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.UiState
import com.example.data.Video
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.VideoThumbnailImage
import com.example.ui.components.formatCount
import com.example.ui.theme.BlueStreamPrimary
import com.example.ui.theme.BlueStreamSecondary

@Composable
fun ShortsScreen(
    videosState: UiState<List<Video>>,
    likedVideoIds: Set<String>,
    subscribedChannelIds: Set<String>,
    currentUserId: String,
    onWatchVideo: (Video) -> Unit,
    onToggleLike: (Video) -> Unit,
    onToggleSubscribe: (Video) -> Unit,
    onNavigateToUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    when (videosState) {
        is UiState.Loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BlueStreamSecondary)
            }
        }

        is UiState.Error -> {
            Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = videosState.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }

        is UiState.Success -> {
            val shortsList = videosState.data.filter {
                it.category.equals("Shorts", ignoreCase = true) ||
                    (it.durationSeconds in 1L..60L)
            }.ifEmpty { videosState.data }

            if (shortsList.isEmpty()) {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.widthIn(max = 380.dp)
                    ) {
                        Text(
                            text = "No Shorts Yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Upload a quick vertical clip or choose the Shorts category to launch the BlueStream Shorts feed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = onNavigateToUpload,
                            colors = ButtonDefaults.buttonColors(containerColor = BlueStreamPrimary),
                            modifier = Modifier.testTag("shorts_upload_cta")
                        ) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create a Short")
                        }
                    }
                }
            } else {
                val pagerState = rememberPagerState(pageCount = { shortsList.size })
                VerticalPager(
                    state = pagerState,
                    modifier = modifier.fillMaxSize()
                ) { page ->
                    val video = shortsList[page]
                    val isLiked = video.id in likedVideoIds
                    val isSubscribed = video.ownerId in subscribedChannelIds

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 460.dp)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF061026))
                                .clickable { onWatchVideo(video) }
                                .testTag("short_card_${video.id}")
                        ) {
                            VideoThumbnailImage(
                                thumbnailUrl = video.thumbnailUrl,
                                category = video.category,
                                contentDescription = video.title,
                                modifier = Modifier.fillMaxSize()
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.25f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.82f)
                                            )
                                        )
                                    )
                            )

                            // Top badge
                            Surface(
                                color = BlueStreamPrimary.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "BlueStream Shorts",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            // Center Play CTA
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(BlueStreamPrimary.copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play Short",
                                    tint = Color.White,
                                    modifier = Modifier.size(42.dp)
                                )
                            }

                            // Right Action Rail
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 14.dp, bottom = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ShortRailAction(
                                    icon = {
                                        Icon(
                                            imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                            contentDescription = "Like",
                                            tint = if (isLiked) BlueStreamSecondary else Color.White
                                        )
                                    },
                                    label = formatCount(video.likesCount),
                                    onClick = { onToggleLike(video) },
                                    tag = "short_like_${video.id}"
                                )
                                ShortRailAction(
                                    icon = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Comment,
                                            contentDescription = "Comments",
                                            tint = Color.White
                                        )
                                    },
                                    label = formatCount(video.commentsCount),
                                    onClick = { onWatchVideo(video) },
                                    tag = "short_comments_${video.id}"
                                )
                                ShortRailAction(
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Filled.Share,
                                            contentDescription = "Share",
                                            tint = Color.White
                                        )
                                    },
                                    label = "Share",
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, video.title)
                                            putExtra(Intent.EXTRA_TEXT, "Watch \"${video.title}\" by ${video.channelName} on BlueStream!")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                                    },
                                    tag = "short_share_${video.id}"
                                )
                            }

                            // Bottom Channel & Caption
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth(0.76f)
                                    .padding(start = 16.dp, bottom = 24.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ChannelAvatar(
                                        name = video.channelName,
                                        avatarUrl = video.channelAvatarUrl,
                                        size = 38.dp
                                    )
                                    Text(
                                        text = video.channelHandle.ifBlank { video.channelName },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (video.ownerId != currentUserId) {
                                        Surface(
                                            color = if (isSubscribed) Color.White.copy(alpha = 0.2f) else BlueStreamPrimary,
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .clickable { onToggleSubscribe(video) }
                                                .testTag("short_subscribe_${video.id}")
                                        ) {
                                            Text(
                                                text = if (isSubscribed) "Subscribed" else "Subscribe",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = video.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (video.description.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = video.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD6E4FF),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortRailAction(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    tag: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .testTag(tag)
        ) {
            icon()
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}
