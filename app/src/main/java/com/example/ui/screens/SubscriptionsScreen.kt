package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.UiState
import com.example.data.UserSubscription
import com.example.data.Video
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.VideoFeedCard
import com.example.ui.theme.BlueStreamPrimary
import com.example.ui.theme.BlueStreamSecondary

@Composable
fun SubscriptionsScreen(
    subscriptions: List<UserSubscription>,
    publicVideosState: UiState<List<Video>>,
    watchLaterIds: Set<String>,
    onVideoClick: (Video) -> Unit,
    onToggleWatchLater: (Video) -> Unit,
    onExploreHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedChannelId by remember { mutableStateOf<String?>(null) }

    if (subscriptions.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Subscriptions,
                        contentDescription = null,
                        tint = BlueStreamSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "No Subscriptions Yet",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Subscribe to your favorite creators on BlueStream to see their latest videos right here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onExploreHome,
                        colors = ButtonDefaults.buttonColors(containerColor = BlueStreamPrimary),
                        modifier = Modifier.testTag("subs_explore_home_button")
                    ) {
                        Text("Explore Videos")
                    }
                }
            }
        }
        return
    }

    val subscribedIds = subscriptions.map { it.channelId }.toSet()
    val allVideos = (publicVideosState as? UiState.Success)?.data ?: emptyList()
    val feedVideos = allVideos.filter { vid ->
        if (selectedChannelId != null) {
            vid.ownerId == selectedChannelId
        } else {
            vid.ownerId in subscribedIds
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Horizontal Subscribed Channels Strip
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(68.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedChannelId = null }
                        .padding(vertical = 4.dp)
                        .testTag("subs_filter_all")
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (selectedChannelId == null) 2.5.dp else 1.dp,
                                color = if (selectedChannelId == null) BlueStreamSecondary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selectedChannelId == null) BlueStreamSecondary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "All",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            items(subscriptions, key = { it.channelId }) { sub ->
                val isSelected = selectedChannelId == sub.channelId
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            selectedChannelId = if (isSelected) null else sub.channelId
                        }
                        .padding(vertical = 4.dp)
                        .testTag("subs_channel_${sub.channelId}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 2.5.dp else 0.dp,
                                color = if (isSelected) BlueStreamSecondary else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        ChannelAvatar(
                            name = sub.channelName,
                            avatarUrl = sub.channelAvatarUrl,
                            size = 50.dp
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = sub.channelName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) BlueStreamSecondary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (feedVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recent public videos from this channel yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val columns = when {
                    maxWidth >= 980.dp -> 3
                    maxWidth >= 600.dp -> 2
                    else -> 1
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(feedVideos, key = { it.id }) { video ->
                        VideoFeedCard(
                            video = video,
                            isSavedWatchLater = video.id in watchLaterIds,
                            onClick = { onVideoClick(video) },
                            onToggleWatchLater = { onToggleWatchLater(video) }
                        )
                    }
                }
            }
        }
    }
}
