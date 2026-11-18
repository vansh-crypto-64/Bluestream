package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class PickedVideoResult(
    val localVideoUri: String,
    val localThumbnailUri: String?,
    val durationSeconds: Long
)

object MediaUploadHelper {

    suspend fun copyPickedVideoAndExtractMetadata(
        context: Context,
        sourceUri: Uri
    ): Result<PickedVideoResult> = withContext(Dispatchers.IO) {
        try {
            val uploadsDir = File(context.filesDir, "uploads").apply { mkdirs() }
            val uniqueId = UUID.randomUUID().toString().replace("-", "").take(16)
            val videoFile = File(uploadsDir, "vid_$uniqueId.mp4")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(videoFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Could not open selected video."))

            var durationSeconds = 0L
            var thumbPath: String? = null

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(videoFile.absolutePath)
                val durationMs = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                durationSeconds = (durationMs / 1000L).coerceIn(0L, 43200L)

                val frame = retriever.getFrameAtTime(
                    1_000_000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                ) ?: retriever.getFrameAtTime(0L)

                if (frame != null) {
                    val thumbFile = File(uploadsDir, "thumb_$uniqueId.jpg")
                    FileOutputStream(thumbFile).use { out ->
                        frame.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    frame.recycle()
                    thumbPath = Uri.fromFile(thumbFile).toString()
                }
            } catch (_: Exception) {
                // Metadata extraction is best-effort
            } finally {
                runCatching { retriever.release() }
            }

            Result.success(
                PickedVideoResult(
                    localVideoUri = Uri.fromFile(videoFile).toString(),
                    localThumbnailUri = thumbPath,
                    durationSeconds = durationSeconds
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun copyPickedThumbnailImage(
        context: Context,
        sourceUri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val uploadsDir = File(context.filesDir, "uploads").apply { mkdirs() }
            val uniqueId = UUID.randomUUID().toString().replace("-", "").take(16)
            val thumbFile = File(uploadsDir, "custom_thumb_$uniqueId.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(thumbFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Could not open selected image."))
            Result.success(Uri.fromFile(thumbFile).toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
