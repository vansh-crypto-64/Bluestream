package com.example.data

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class StorageUploadResult(
    val videoDownloadUrl: String,
    val thumbnailDownloadUrl: String?
)

class FirebaseStorageUploader {

    private var activeTask: UploadTask? = null

    fun cancelActiveUpload() {
        runCatching { activeTask?.cancel() }
        activeTask = null
    }

    suspend fun uploadMedia(
        uid: String,
        localVideoUri: String,
        localThumbnailUri: String?,
        onProgress: (progress: Float, stage: String) -> Unit
    ): Result<StorageUploadResult> {
        return try {
            val storage = Firebase.storage
            val uploadId = UUID.randomUUID().toString().replace("-", "").take(16)

            // 1. Upload Video if it is a local file/content URI (skip if already https://)
            val finalVideoUrl = if (localVideoUri.startsWith("http://") || localVideoUri.startsWith("https://")) {
                onProgress(0.75f, "Validating stream URL...")
                localVideoUri
            } else {
                onProgress(0.05f, "Uploading video to Firebase Storage...")
                val videoRef = storage.reference.child("videos/$uid/$uploadId.mp4")
                val videoMeta = StorageMetadata.Builder()
                    .setContentType("video/mp4")
                    .build()
                uploadFileWithProgress(
                    ref = videoRef,
                    fileUri = Uri.parse(localVideoUri),
                    metadata = videoMeta
                ) { fraction ->
                    // Video accounts for 0% -> 80% of overall progress
                    onProgress((fraction * 0.80f).coerceIn(0.05f, 0.80f), "Uploading video (${(fraction * 100).toInt()}%)...")
                }
                videoRef.downloadUrl.await().toString()
            }

            // 2. Upload Thumbnail if it is a local file/content URI
            val finalThumbUrl = when {
                localThumbnailUri.isNullOrBlank() -> null
                localThumbnailUri.startsWith("preset:") ||
                    localThumbnailUri.startsWith("http://") ||
                    localThumbnailUri.startsWith("https://") -> localThumbnailUri
                else -> {
                    onProgress(0.84f, "Uploading thumbnail...")
                    val thumbRef = storage.reference.child("thumbnails/$uid/$uploadId.jpg")
                    val thumbMeta = StorageMetadata.Builder()
                        .setContentType("image/jpeg")
                        .build()
                    uploadFileWithProgress(
                        ref = thumbRef,
                        fileUri = Uri.parse(localThumbnailUri),
                        metadata = thumbMeta
                    ) { fraction ->
                        onProgress(
                            (0.82f + fraction * 0.14f).coerceIn(0.82f, 0.96f),
                            "Uploading thumbnail (${(fraction * 100).toInt()}%)..."
                        )
                    }
                    thumbRef.downloadUrl.await().toString()
                }
            }

            onProgress(0.98f, "Publishing to BlueStream...")
            Result.success(
                StorageUploadResult(
                    videoDownloadUrl = finalVideoUrl,
                    thumbnailDownloadUrl = finalThumbUrl
                )
            )
        } catch (e: CancellationException) {
            cancelActiveUpload()
            throw e
        } catch (e: Exception) {
            activeTask = null
            Result.failure(e)
        }
    }

    private suspend fun uploadFileWithProgress(
        ref: com.google.firebase.storage.StorageReference,
        fileUri: Uri,
        metadata: StorageMetadata,
        onFraction: (Float) -> Unit
    ) = suspendCancellableCoroutine<Unit> { cont ->
        val task = ref.putFile(fileUri, metadata)
        activeTask = task

        task.addOnProgressListener { snapshot ->
            val total = snapshot.totalByteCount
            if (total > 0L) {
                val fraction = snapshot.bytesTransferred.toFloat() / total.toFloat()
                onFraction(fraction.coerceIn(0f, 1f))
            }
        }
        task.addOnSuccessListener {
            activeTask = null
            if (cont.isActive) cont.resume(Unit)
        }
        task.addOnFailureListener { error ->
            activeTask = null
            if (cont.isActive) cont.resumeWithException(error)
        }
        task.addOnCanceledListener {
            activeTask = null
            if (cont.isActive) cont.cancel()
        }

        cont.invokeOnCancellation {
            runCatching { task.cancel() }
            activeTask = null
        }
    }
}
