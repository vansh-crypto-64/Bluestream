package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Channel
import com.example.data.UiState
import com.example.data.UserLike
import com.example.data.Video
import com.example.data.VideoReport
import com.example.data.WatchLaterItem
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.CompactUpNextCard
import com.example.ui.components.formatCount
import com.example.ui.theme.BlueStreamOwnerGold
import com.example.ui.theme.BlueStreamPrimary
import com.example.ui.theme.BlueStreamSecondary

@Composable
fun LibraryProfileScreen(
    channel: Channel?,
    userEmail: String?,
    isPlatformOwner: Boolean,
    myVideosState: UiState<List<Video>>,
    publicVideosState: UiState<List<Video>>,
    pendingModerationVideos: List<Video>,
    openReports: List<VideoReport>,
    watchLaterItems: List<WatchLaterItem>,
    likedItems: List<UserLike>,
    onVideoClick: (Video) -> Unit,
    onDeleteVideo: (Video) -> Unit,
    onToggleFeatured: (Video) -> Unit,
    onApproveVideo: (Video) -> Unit,
    onRejectVideo: (Video) -> Unit,
    onDismissReport: (String) -> Unit,
    onRemoveReportedVideo: (VideoReport) -> Unit,
    onUpdateChannelProfile: (displayName: String, handle: String, bio: String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Your Videos, 1=Watch Later, 2=Liked
    var showEditChannelDialog by remember { mutableStateOf(false) }

    val myVideos = (myVideosState as? UiState.Success)?.data ?: emptyList()
    val allPublicVideos = (publicVideosState as? UiState.Success)?.data ?: emptyList()

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Channel Profile Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = BlueStreamSecondary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ChannelAvatar(
                            name = channel?.displayName ?: "Creator",
                            avatarUrl = channel?.avatarUrl,
                            size = 64.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = channel?.displayName?.ifBlank { "BlueStream Creator" } ?: "BlueStream Creator",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isPlatformOwner) {
                                    Surface(
                                        color = BlueStreamPrimary,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "OWNER",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BlueStreamOwnerGold,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = channel?.handle?.ifBlank { "@creator" } ?: "@creator",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BlueStreamSecondary
                            )
                            if (!userEmail.isNullOrBlank()) {
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (!channel?.bio.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = channel?.bio ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatBadge(
                            label = "Subscribers",
                            value = formatCount(channel?.subscriberCount ?: 0L),
                            modifier = Modifier.weight(1f)
                        )
                        StatBadge(
                            label = "Your Videos",
                            value = myVideos.size.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        StatBadge(
                            label = "Saved",
                            value = watchLaterItems.size.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showEditChannelDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueStreamPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_channel_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Edit Channel")
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }

            // Platform Owner Studio Control Panel (Unlocked exclusively for lakeskuke@gmail.com)
            if (isPlatformOwner) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF102A63), Color(0xFF0A1B42))
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = BlueStreamOwnerGold.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = null,
                                tint = BlueStreamOwnerGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Owner Control Center",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }

                        val totalViews = allPublicVideos.sumOf { it.viewsCount }
                        val totalLikes = allPublicVideos.sumOf { it.likesCount }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatBadge(label = "Live Videos", value = allPublicVideos.size.toString(), modifier = Modifier.weight(1f))
                            StatBadge(label = "Pending", value = pendingModerationVideos.size.toString(), modifier = Modifier.weight(1f))
                            StatBadge(label = "Reports", value = openReports.size.toString(), modifier = Modifier.weight(1f))
                        }

                        // Pending Moderation Queue (videos awaiting approval before public feed)
                        Text(
                            text = "Moderation Queue (${pendingModerationVideos.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = BlueStreamOwnerGold
                        )
                        if (pendingModerationVideos.isEmpty()) {
                            Text(
                                text = "All uploaded videos have been reviewed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9CB3D9)
                            )
                        } else {
                            pendingModerationVideos.forEach { vid ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.30f))
                                        .clickable { onVideoClick(vid) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = vid.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${vid.channelName} • ${vid.category} • Pending",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFDE68A)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onApproveVideo(vid) },
                                        modifier = Modifier.testTag("owner_approve_${vid.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Approve Video",
                                            tint = Color(0xFF4ADE80)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRejectVideo(vid) },
                                        modifier = Modifier.testTag("owner_reject_${vid.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Reject Video",
                                            tint = Color(0xFFF87171)
                                        )
                                    }
                                }
                            }
                        }

                        // Reported Videos Queue
                        if (openReports.isNotEmpty()) {
                            Text(
                                text = "Reported Videos (${openReports.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFFFCA5A5)
                            )
                            openReports.forEach { report ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x33EF4444))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Flag,
                                        contentDescription = null,
                                        tint = Color(0xFFF87171),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = report.videoTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${report.reason}${if (!report.details.isNullOrBlank()) " — ${report.details}" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFECACA),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    TextButton(
                                        onClick = { onDismissReport(report.id) },
                                        modifier = Modifier.testTag("owner_dismiss_report_${report.id}")
                                    ) {
                                        Text("Dismiss", color = Color(0xFFCAE0FF))
                                    }
                                    IconButton(
                                        onClick = { onRemoveReportedVideo(report) },
                                        modifier = Modifier.testTag("owner_remove_reported_${report.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Remove Reported Video",
                                            tint = Color(0xFFF87171)
                                        )
                                    }
                                }
                            }
                        }

                        if (allPublicVideos.isNotEmpty()) {
                            Text(
                                text = "Quick Spotlight & Moderation",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFFCAE0FF)
                            )
                            allPublicVideos.take(6).forEach { vid ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.25f))
                                        .clickable { onVideoClick(vid) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = vid.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${vid.channelName} • ${formatCount(vid.viewsCount)} views",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF9CB3D9)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onToggleFeatured(vid) },
                                        modifier = Modifier.testTag("owner_feature_${vid.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (vid.featured) Icons.Filled.Star else Icons.Filled.StarBorder,
                                            contentDescription = "Toggle Featured",
                                            tint = if (vid.featured) BlueStreamOwnerGold else Color.White
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteVideo(vid) },
                                        modifier = Modifier.testTag("owner_delete_${vid.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Moderate Delete",
                                            tint = Color(0xFFF87171)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Library Section Switcher
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Your Videos", "Watch Later", "Liked").forEachIndexed { idx, label ->
                    FilterChip(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlueStreamPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("profile_tab_$idx")
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    if (myVideos.isEmpty()) {
                        EmptyLibraryNotice("You haven't uploaded any videos yet. Tap + Upload to share your first video!")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            myVideos.forEach { vid ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CompactUpNextCard(
                                            video = vid,
                                            onClick = { onVideoClick(vid) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { onDeleteVideo(vid) },
                                            modifier = Modifier.testTag("my_video_delete_${vid.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Delete video",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    if (vid.moderationStatus != "approved") {
                                        val isRejected = vid.moderationStatus == "rejected"
                                        Surface(
                                            color = if (isRejected) MaterialTheme.colorScheme.errorContainer
                                            else BlueStreamPrimary.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (isRejected) "Moderation: Rejected"
                                                else "Moderation: Pending Owner Review",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isRejected) MaterialTheme.colorScheme.error
                                                else BlueStreamSecondary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    val savedVideos = watchLaterItems.mapNotNull { saved ->
                        allPublicVideos.firstOrNull { it.id == saved.videoId }
                            ?: myVideos.firstOrNull { it.id == saved.videoId }
                    }
                    if (savedVideos.isEmpty()) {
                        EmptyLibraryNotice("No Watch Later videos saved yet. Tap the bookmark icon on any video card to save it.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            savedVideos.forEach { vid ->
                                CompactUpNextCard(video = vid, onClick = { onVideoClick(vid) })
                            }
                        }
                    }
                }

                2 -> {
                    val likedVideos = likedItems.mapNotNull { liked ->
                        allPublicVideos.firstOrNull { it.id == liked.videoId }
                            ?: myVideos.firstOrNull { it.id == liked.videoId }
                    }
                    if (likedVideos.isEmpty()) {
                        EmptyLibraryNotice("Videos you like will appear here.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            likedVideos.forEach { vid ->
                                CompactUpNextCard(video = vid, onClick = { onVideoClick(vid) })
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showEditChannelDialog) {
        EditChannelDialog(
            initialName = channel?.displayName ?: "",
            initialHandle = channel?.handle ?: "@creator",
            initialBio = channel?.bio ?: "",
            onDismiss = { showEditChannelDialog = false },
            onSave = { name, handle, bio ->
                onUpdateChannelProfile(name, handle, bio)
                showEditChannelDialog = false
            }
        )
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyLibraryNotice(message: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp)
        )
    }
}

@Composable
private fun EditChannelDialog(
    initialName: String,
    initialHandle: String,
    initialBio: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var handle by remember { mutableStateOf(initialHandle) }
    var bio by remember { mutableStateOf(initialBio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Your Channel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 60) name = it },
                    label = { Text("Channel Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = handle,
                    onValueChange = { if (it.length <= 36) handle = it },
                    label = { Text("Handle (@name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 300) bio = it },
                    label = { Text("Bio") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, handle, bio) },
                enabled = name.isNotBlank() && handle.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BlueStreamPrimary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
