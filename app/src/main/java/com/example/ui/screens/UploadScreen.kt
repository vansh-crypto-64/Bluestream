package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LinearProgressIndicator
import com.example.data.MediaUploadHelper
import com.example.data.UPLOAD_CATEGORIES
import com.example.data.UploadTransferState
import com.example.ui.components.VideoThumbnailImage
import com.example.ui.components.formatDuration
import com.example.ui.theme.BlueStreamPrimary
import com.example.ui.theme.BlueStreamSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UploadScreen(
    uploadState: UploadTransferState,
    onCancelUpload: () -> Unit,
    onClearUploadError: () -> Unit,
    onPublish: (
        title: String,
        description: String,
        category: String,
        videoUrl: String,
        thumbnailUrl: String?,
        durationSeconds: Long,
        visibility: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUploading = uploadState.active
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceMode by remember { mutableStateOf("device") } // "device" or "url"
    var videoUrl by remember { mutableStateOf("") }
    var pickedVideoLabel by remember { mutableStateOf<String?>(null) }
    var thumbnailUrl by remember { mutableStateOf<String?>("preset:tech") }
    var durationSeconds by remember { mutableLongStateOf(0L) }
    var isProcessingMedia by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tech") }
    var visibility by remember { mutableStateOf("public") }
    var localError by remember { mutableStateOf<String?>(null) }

    // Zero-permission Android Photo Picker for Videos
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessingMedia = true
            localError = null
            scope.launch {
                MediaUploadHelper.copyPickedVideoAndExtractMetadata(context, uri)
                    .onSuccess { result ->
                        videoUrl = result.localVideoUri
                        durationSeconds = result.durationSeconds
                        if (result.localThumbnailUri != null) {
                            thumbnailUrl = result.localThumbnailUri
                        }
                        pickedVideoLabel = "Device video ready (${formatDuration(result.durationSeconds)})"
                    }
                    .onFailure { err ->
                        localError = err.message ?: "Failed to prepare selected video."
                    }
                isProcessingMedia = false
            }
        }
    }

    // Zero-permission Android Photo Picker for Custom Thumbnail Image
    val thumbPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                MediaUploadHelper.copyPickedThumbnailImage(context, uri)
                    .onSuccess { localThumb -> thumbnailUrl = localThumb }
                    .onFailure { err -> localError = err.message ?: "Could not load thumbnail." }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Upload to BlueStream",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Source Mode Switcher (Device Video vs Direct Stream URL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SourceModeCard(
                    title = "Device Gallery",
                    subtitle = "Pick MP4 from phone/laptop",
                    icon = Icons.Filled.VideoLibrary,
                    selected = sourceMode == "device",
                    onClick = { sourceMode = "device" },
                    tag = "upload_mode_device",
                    modifier = Modifier.weight(1f)
                )
                SourceModeCard(
                    title = "Stream URL",
                    subtitle = "Direct HTTPS MP4 / HLS",
                    icon = Icons.Filled.Link,
                    selected = sourceMode == "url",
                    onClick = { sourceMode = "url" },
                    tag = "upload_mode_url",
                    modifier = Modifier.weight(1f)
                )
            }

            // Video Source Picker or URL Field
            if (sourceMode == "device") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = BlueStreamSecondary.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isProcessingMedia) {
                            CircularProgressIndicator(color = BlueStreamSecondary, modifier = Modifier.size(32.dp))
                            Text(
                                text = "Extracting video metadata & thumbnail...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = null,
                                tint = BlueStreamSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = pickedVideoLabel ?: "Select a video from your device",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = {
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BlueStreamPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("pick_device_video_button")
                            ) {
                                Text(if (pickedVideoLabel == null) "Choose Video" else "Change Video")
                            }
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text("Direct Video URL (https://...mp4 or .m3u8)") },
                    placeholder = { Text("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_video_url_input")
                )
            }

            // Thumbnail Preview & Picker
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Thumbnail",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    VideoThumbnailImage(
                        thumbnailUrl = thumbnailUrl,
                        category = category,
                        contentDescription = "Video thumbnail preview",
                        modifier = Modifier.fillMaxSize()
                    )
                    OutlinedButton(
                        onClick = {
                            thumbPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.65f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .testTag("pick_thumbnail_button")
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Custom Cover")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "preset:tech" to "Studio Blue",
                        "preset:travel" to "City Twilight",
                        "preset:hero" to "Cobalt Wave"
                    ).forEach { (presetKey, label) ->
                        FilterChip(
                            selected = thumbnailUrl == presetKey,
                            onClick = { thumbnailUrl = presetKey },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BlueStreamPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Title & Description
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 120) title = it },
                label = { Text("Video Title *") },
                placeholder = { Text("Give your video an engaging title") },
                singleLine = true,
                supportingText = { Text("${title.length}/120") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_title_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 2000) description = it },
                label = { Text("Description") },
                placeholder = { Text("Tell viewers about your video...") },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_description_input")
            )

            // Category Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UPLOAD_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BlueStreamPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("upload_cat_$cat")
                        )
                    }
                }
            }

            // Visibility Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = visibility == "public",
                    onClick = { visibility = "public" },
                    label = { Text("Public") },
                    leadingIcon = { Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BlueStreamPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("upload_visibility_public")
                )
                FilterChip(
                    selected = visibility == "private",
                    onClick = { visibility = "private" },
                    label = { Text("Private") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BlueStreamPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("upload_visibility_private")
                )
            }

            if (localError != null) {
                Text(
                    text = localError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (uploadState.active) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = BlueStreamSecondary.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("upload_progress_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uploadState.stage.ifBlank { "Uploading to BlueStream..." },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(uploadState.progress.coerceIn(0f, 1f) * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = BlueStreamSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { uploadState.progress.coerceIn(0f, 1f) },
                            color = BlueStreamSecondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = onCancelUpload,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("cancel_upload_button")
                            ) {
                                Text("Cancel Upload")
                            }
                        }
                    }
                }
            }

            if (uploadState.error != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("upload_error_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = uploadState.error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    onClearUploadError()
                                    if (title.isNotBlank() && videoUrl.isNotBlank()) {
                                        onPublish(
                                            title,
                                            description,
                                            category,
                                            videoUrl,
                                            thumbnailUrl,
                                            durationSeconds,
                                            visibility
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BlueStreamPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("retry_upload_button")
                            ) {
                                Text("Retry Upload")
                            }
                            OutlinedButton(
                                onClick = onClearUploadError,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (title.isBlank()) {
                        localError = "Please enter a video title."
                        return@Button
                    }
                    if (videoUrl.isBlank()) {
                        localError = "Please select a video or paste a direct video URL."
                        return@Button
                    }
                    localError = null
                    onClearUploadError()
                    onPublish(
                        title,
                        description,
                        category,
                        videoUrl,
                        thumbnailUrl,
                        durationSeconds,
                        visibility
                    )
                },
                enabled = !isUploading && !isProcessingMedia,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlueStreamPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("publish_video_button")
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = uploadState.stage.ifBlank { "Uploading..." },
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("Publish Video", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SourceModeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) BlueStreamPrimary.copy(alpha = 0.2f)
        else MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) BlueStreamSecondary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) BlueStreamSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
