package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.UiState
import com.example.data.Video
import com.example.data.VideoComment
import com.example.ui.components.BlueStreamVideoPlayer
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.CompactUpNextCard
import com.example.ui.components.formatCount
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.BlueStreamOwnerGold
import com.example.ui.theme.BlueStreamPrimary
import com.example.ui.theme.BlueStreamSecondary

@Composable
fun WatchScreen(
    video: Video,
    upNextVideos: List<Video>,
    commentsState: UiState<List<VideoComment>>,
    isLiked: Boolean,
    isSubscribed: Boolean,
    isSavedWatchLater: Boolean,
    currentUserId: String,
    isPlatformOwner: Boolean,
    onBack: () -> Unit,
    onSelectVideo: (Video) -> Unit,
    onToggleLike: () -> Unit,
    onToggleSubscribe: () -> Unit,
    onToggleWatchLater: () -> Unit,
    onToggleFeatured: () -> Unit,
    onDeleteVideo: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFullscreen by remember { mutableStateOf(false) }
    var expandDescription by remember { mutableStateOf(false) }
    var commentDraft by remember { mutableStateOf("") }
    val context = LocalContext.current

    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            onBack()
        }
    }

    if (isFullscreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            BlueStreamVideoPlayer(
                videoUrl = video.videoUrl,
                thumbnailUrl = video.thumbnailUrl,
                category = video.category,
                isFullscreen = true,
                onToggleFullscreen = { isFullscreen = false },
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Watch Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("watch_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to feed",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideLayout = maxWidth >= 840.dp

            if (isWideLayout) {
                // Canonical Laptop / Tablet Two-Pane Watch Layout (68% Player & Comments | 32% Up Next)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.68f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 28.dp)
                    ) {
                        BlueStreamVideoPlayer(
                            videoUrl = video.videoUrl,
                            thumbnailUrl = video.thumbnailUrl,
                            category = video.category,
                            isFullscreen = false,
                            onToggleFullscreen = { isFullscreen = true }
                        )
                        Spacer(Modifier.height(14.dp))
                        WatchDetailsSection(
                            video = video,
                            isLiked = isLiked,
                            isSubscribed = isSubscribed,
                            isSavedWatchLater = isSavedWatchLater,
                            currentUserId = currentUserId,
                            isPlatformOwner = isPlatformOwner,
                            expandDescription = expandDescription,
                            onToggleDescription = { expandDescription = !expandDescription },
                            onToggleLike = onToggleLike,
                            onToggleSubscribe = onToggleSubscribe,
                            onToggleWatchLater = onToggleWatchLater,
                            onToggleFeatured = onToggleFeatured,
                            onDeleteVideo = onDeleteVideo,
                            onShare = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, video.title)
                                    putExtra(Intent.EXTRA_TEXT, "Watch \"${video.title}\" on BlueStream!")
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Video"))
                            }
                        )
                        Spacer(Modifier.height(18.dp))
                        WatchCommentsSection(
                            commentsState = commentsState,
                            commentDraft = commentDraft,
                            onDraftChange = { commentDraft = it },
                            onSubmitComment = {
                                if (commentDraft.isNotBlank()) {
                                    onAddComment(commentDraft)
                                    commentDraft = ""
                                }
                            },
                            currentUserId = currentUserId,
                            isPlatformOwner = isPlatformOwner,
                            onDeleteComment = onDeleteComment
                        )
                    }

                    // Right Sidebar: Up Next
                    Column(
                        modifier = Modifier
                            .weight(0.32f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Up Next",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (upNextVideos.isEmpty()) {
                            Text(
                                text = "More community videos will appear here as creators upload.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            upNextVideos.forEach { nextVid ->
                                CompactUpNextCard(
                                    video = nextVid,
                                    onClick = { onSelectVideo(nextVid) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Mobile Phone Single-Column Watch Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    BlueStreamVideoPlayer(
                        videoUrl = video.videoUrl,
                        thumbnailUrl = video.thumbnailUrl,
                        category = video.category,
                        isFullscreen = false,
                        onToggleFullscreen = { isFullscreen = true }
                    )
                    Spacer(Modifier.height(14.dp))
                    WatchDetailsSection(
                        video = video,
                        isLiked = isLiked,
                        isSubscribed = isSubscribed,
                        isSavedWatchLater = isSavedWatchLater,
                        currentUserId = currentUserId,
                        isPlatformOwner = isPlatformOwner,
                        expandDescription = expandDescription,
                        onToggleDescription = { expandDescription = !expandDescription },
                        onToggleLike = onToggleLike,
                        onToggleSubscribe = onToggleSubscribe,
                        onToggleWatchLater = onToggleWatchLater,
                        onToggleFeatured = onToggleFeatured,
                        onDeleteVideo = onDeleteVideo,
                        onShare = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, video.title)
                                putExtra(Intent.EXTRA_TEXT, "Watch \"${video.title}\" on BlueStream!")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Video"))
                        }
                    )
                    Spacer(Modifier.height(18.dp))
                    WatchCommentsSection(
                        commentsState = commentsState,
                        commentDraft = commentDraft,
                        onDraftChange = { commentDraft = it },
                        onSubmitComment = {
                            if (commentDraft.isNotBlank()) {
                                onAddComment(commentDraft)
                                commentDraft = ""
                            }
                        },
                        currentUserId = currentUserId,
                        isPlatformOwner = isPlatformOwner,
                        onDeleteComment = onDeleteComment
                    )
                    if (upNextVideos.isNotEmpty()) {
                        Spacer(Modifier.height(22.dp))
                        Text(
                            text = "Up Next",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            upNextVideos.forEach { nextVid ->
                                CompactUpNextCard(
                                    video = nextVid,
                                    onClick = { onSelectVideo(nextVid) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun WatchDetailsSection(
    video: Video,
    isLiked: Boolean,
    isSubscribed: Boolean,
    isSavedWatchLater: Boolean,
    currentUserId: String,
    isPlatformOwner: Boolean,
    expandDescription: Boolean,
    onToggleDescription: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleSubscribe: () -> Unit,
    onToggleWatchLater: () -> Unit,
    onToggleFeatured: () -> Unit,
    onDeleteVideo: () -> Unit,
    onShare: () -> Unit
) {
    Text(
        text = video.title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = "${formatCount(video.viewsCount)} views • ${formatRelativeTime(video.createdAt)} • #${video.category}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(Modifier.height(14.dp))

    // Channel Bar + Subscribe
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            ChannelAvatar(
                name = video.channelName,
                avatarUrl = video.channelAvatarUrl,
                size = 44.dp
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = video.channelName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.channelHandle.ifBlank { "@creator" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (video.ownerId != currentUserId) {
            Button(
                onClick = onToggleSubscribe,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSubscribed) MaterialTheme.colorScheme.surfaceVariant else BlueStreamPrimary,
                    contentColor = if (isSubscribed) MaterialTheme.colorScheme.onSurface else Color.White
                ),
                modifier = Modifier.testTag("watch_subscribe_button")
            ) {
                Text(
                    text = if (isSubscribed) "Subscribed" else "Subscribe",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    Spacer(Modifier.height(14.dp))

    // Engagement Action Pills
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ActionPill(
            icon = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
            label = formatCount(video.likesCount),
            highlighted = isLiked,
            onClick = onToggleLike,
            tag = "watch_like_button"
        )
        ActionPill(
            icon = if (isSavedWatchLater) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            label = if (isSavedWatchLater) "Saved" else "Save",
            highlighted = isSavedWatchLater,
            onClick = onToggleWatchLater,
            tag = "watch_save_button"
        )
        ActionPill(
            icon = Icons.Filled.Share,
            label = "Share",
            highlighted = false,
            onClick = onShare,
            tag = "watch_share_button"
        )
        if (isPlatformOwner) {
            ActionPill(
                icon = if (video.featured) Icons.Filled.Star else Icons.Filled.StarBorder,
                label = if (video.featured) "Featured" else "Feature",
                highlighted = video.featured,
                tintOverride = if (video.featured) BlueStreamOwnerGold else null,
                onClick = onToggleFeatured,
                tag = "watch_owner_feature_button"
            )
        }
        if (video.ownerId == currentUserId || isPlatformOwner) {
            ActionPill(
                icon = Icons.Filled.Delete,
                label = "Delete",
                highlighted = false,
                tintOverride = MaterialTheme.colorScheme.error,
                onClick = onDeleteVideo,
                tag = "watch_delete_button"
            )
        }
    }

    Spacer(Modifier.height(14.dp))

    // Expandable Description Card
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggleDescription)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = video.description.ifBlank { "No description provided by the creator." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expandDescription) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )
            if (video.description.length > 100) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (expandDescription) "Show less" else "Show more",
                    style = MaterialTheme.typography.labelLarge,
                    color = BlueStreamSecondary
                )
            }
        }
    }
}

@Composable
private fun ActionPill(
    icon: ImageVector,
    label: String,
    highlighted: Boolean,
    tintOverride: Color? = null,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (highlighted) BlueStreamPrimary.copy(alpha = 0.22f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tintOverride ?: if (highlighted) BlueStreamSecondary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = tintOverride ?: if (highlighted) BlueStreamSecondary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun WatchCommentsSection(
    commentsState: UiState<List<VideoComment>>,
    commentDraft: String,
    onDraftChange: (String) -> Unit,
    onSubmitComment: () -> Unit,
    currentUserId: String,
    isPlatformOwner: Boolean,
    onDeleteComment: (String) -> Unit
) {
    val count = (commentsState as? UiState.Success)?.data?.size ?: 0
    Text(
        text = "Comments ($count)",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = commentDraft,
            onValueChange = onDraftChange,
            placeholder = { Text("Add a public comment...") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("comment_input_field")
        )
        IconButton(
            onClick = onSubmitComment,
            enabled = commentDraft.isNotBlank(),
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (commentDraft.isNotBlank()) BlueStreamPrimary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .testTag("comment_send_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Post comment",
                tint = if (commentDraft.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(Modifier.height(14.dp))

    when (commentsState) {
        is UiState.Loading -> {
            CircularProgressIndicator(
                color = BlueStreamSecondary,
                modifier = Modifier.size(28.dp)
            )
        }

        is UiState.Error -> {
            Text(
                text = commentsState.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        is UiState.Success -> {
            if (commentsState.data.isEmpty()) {
                Text(
                    text = "Be the first to start the conversation!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    commentsState.data.forEach { comment ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            ChannelAvatar(
                                name = comment.authorName,
                                avatarUrl = comment.authorAvatarUrl,
                                size = 34.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = comment.authorName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = formatRelativeTime(comment.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = comment.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (comment.authorId == currentUserId || isPlatformOwner) {
                                IconButton(
                                    onClick = { onDeleteComment(comment.id) },
                                    modifier = Modifier.testTag("delete_comment_${comment.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete comment",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    }
                }
            }
        }
    }
}
