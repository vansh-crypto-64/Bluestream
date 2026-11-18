package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.UiState
import com.example.data.VIDEO_CATEGORIES
import com.example.data.Video
import com.example.ui.components.VideoFeedCard
import com.example.ui.components.VideoThumbnailImage
import com.example.ui.components.formatCount
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.BlueStreamOwnerGold
import com.example.ui.theme.BlueStreamPrimary
import com.example.ui.theme.BlueStreamSecondary

@Composable
fun HomeScreen(
    videosState: UiState<List<Video>>,
    selectedCategory: String,
    searchQuery: String,
    watchLaterIds: Set<String>,
    onSelectCategory: (String) -> Unit,
    onVideoClick: (Video) -> Unit,
    onToggleWatchLater: (Video) -> Unit,
    onNavigateToUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Category Filter Strip
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(VIDEO_CATEGORIES) { category ->
                val selected = category == selectedCategory
                FilterChip(
                    selected = selected,
                    onClick = { onSelectCategory(category) },
                    label = {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BlueStreamPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("category_chip_$category")
                )
            }
        }

        when (videosState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BlueStreamSecondary)
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Could not load video feed",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = videosState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            is UiState.Success -> {
                val filteredVideos = videosState.data.filter { video ->
                    val matchesCategory = selectedCategory == "All" ||
                        video.category.equals(selectedCategory, ignoreCase = true)
                    val query = searchQuery.trim()
                    val matchesSearch = query.isEmpty() ||
                        video.title.contains(query, ignoreCase = true) ||
                        video.channelName.contains(query, ignoreCase = true) ||
                        video.category.contains(query, ignoreCase = true)
                    matchesCategory && matchesSearch
                }

                if (filteredVideos.isEmpty()) {
                    HomeEmptyState(
                        hasSearchOrFilter = searchQuery.isNotBlank() || selectedCategory != "All",
                        onUploadClick = onNavigateToUpload,
                        onClearFilters = { onSelectCategory("All") }
                    )
                } else {
                    val spotlight = filteredVideos.firstOrNull { it.featured }
                    val gridVideos = if (spotlight != null) {
                        filteredVideos.filterNot { it.id == spotlight.id }
                    } else {
                        filteredVideos
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columns = when {
                            maxWidth >= 980.dp -> 3
                            maxWidth >= 600.dp -> 2
                            else -> 1
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = 24.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (spotlight != null) {
                                item(span = { GridItemSpan(columns) }) {
                                    FeaturedSpotlightBanner(
                                        video = spotlight,
                                        onClick = { onVideoClick(spotlight) }
                                    )
                                }
                            }

                            items(gridVideos, key = { it.id }) { video ->
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
    }
}

@Composable
private fun FeaturedSpotlightBanner(
    video: Video,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("featured_spotlight_banner")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
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
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xE6050E21),
                                Color(0x990A1C3E),
                                Color(0x33050E21)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
                    .widthIn(max = 520.dp)
            ) {
                Surface(
                    color = BlueStreamPrimary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = BlueStreamOwnerGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "OWNER SPOTLIGHT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${video.channelName} • ${formatCount(video.viewsCount)} views • ${formatRelativeTime(video.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCAE0FF)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 22.dp)
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BlueStreamPrimary.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play featured video",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeEmptyState(
    hasSearchOrFilter: Boolean,
    onUploadClick: () -> Unit,
    onClearFilters: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.widthIn(max = 460.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_auth_hero_1795003257451),
                    contentDescription = "BlueStream Studio",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = if (hasSearchOrFilter) "No matching videos found" else "Welcome to BlueStream",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (hasSearchOrFilter) {
                        "Try another category or search term, or upload a new video."
                    } else {
                        "Your cobalt-blue social video platform is live. Upload your first video from your device or paste a direct stream URL."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = if (hasSearchOrFilter) onClearFilters else onUploadClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlueStreamPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("empty_state_primary_cta")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (hasSearchOrFilter) "Show All Videos" else "Upload Your First Video",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
