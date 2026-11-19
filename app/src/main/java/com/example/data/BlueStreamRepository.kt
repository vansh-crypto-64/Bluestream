package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BlueStreamRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : BlueStreamDataSource {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun isCurrentUserAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email.equals(PLATFORM_OWNER_EMAIL, ignoreCase = true) && user.isEmailVerified
    }

    private fun <T> Flow<T>.logFirestoreErrors(
        operationType: OperationType,
        path: String,
        expectedUid: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    override fun observePublicVideos(currentUserId: String): Flow<List<Video>> = flow {
        val path = "videos"
        emitAll(
            db.collection("videos")
                .whereEqualTo("visibility", "public")
                .whereEqualTo("moderationStatus", "approved")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Video::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<Video> { it.featured }
                                .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                        )
                }
                .logFirestoreErrors(OperationType.LIST, path, currentUserId)
        )
    }

    override fun observeUserVideos(userId: String): Flow<List<Video>> = flow {
        val path = "videos"
        emitAll(
            db.collection("videos")
                .whereEqualTo("ownerId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Video::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    override fun observePendingModerationVideos(currentUserId: String): Flow<List<Video>> = flow {
        val path = "videos"
        emitAll(
            db.collection("videos")
                .whereEqualTo("moderationStatus", "pending")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Video::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, path, currentUserId)
        )
    }

    override fun observeOpenReports(currentUserId: String): Flow<List<VideoReport>> = flow {
        val path = "reports"
        emitAll(
            db.collection("reports")
                .whereEqualTo("status", "open")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(VideoReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, path, currentUserId)
        )
    }

    override fun observeChannel(channelId: String, currentUserId: String): Flow<Channel?> = flow {
        val path = "channels/$channelId"
        emitAll(
            db.collection("channels").document(channelId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(Channel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .logFirestoreErrors(OperationType.GET, path, currentUserId)
        )
    }

    override fun observeComments(videoId: String, currentUserId: String): Flow<List<VideoComment>> = flow {
        val path = "videos/$videoId/comments"
        emitAll(
            db.collection("videos").document(videoId)
                .collection("comments")
                .whereEqualTo("videoId", videoId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(VideoComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, path, currentUserId)
        )
    }

    override fun observeLikes(userId: String): Flow<List<UserLike>> = flow {
        val path = "users/$userId/likes"
        emitAll(
            db.collection("users").document(userId)
                .collection("likes")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UserLike::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    override fun observeSubscriptions(userId: String): Flow<List<UserSubscription>> = flow {
        val path = "users/$userId/subscriptions"
        emitAll(
            db.collection("users").document(userId)
                .collection("subscriptions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UserSubscription::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    override fun observeWatchLater(userId: String): Flow<List<WatchLaterItem>> = flow {
        val path = "users/$userId/watch_later"
        emitAll(
            db.collection("users").document(userId)
                .collection("watch_later")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(WatchLaterItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    override suspend fun ensureChannelExists(
        uid: String,
        displayName: String?,
        avatarUrl: String?
    ): Result<Channel> {
        val path = "channels/$uid"
        return try {
            val docRef = db.collection("channels").document(uid)
            val existing = docRef.get().await()
            if (existing.exists()) {
                val channel = existing.toObject(Channel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                return Result.success(channel ?: Channel(id = uid, ownerId = uid))
            }
            val rawName = (displayName?.takeIf { it.isNotBlank() } ?: "BlueStream Creator").take(60)
            val slug = rawName.lowercase().replace(Regex("[^a-z0-9]"), "").ifBlank { "creator" }.take(20)
            val handle = "@${slug}${uid.take(4).lowercase()}"
            val avatar = avatarUrl?.take(500)
            val payload = mutableMapOf<String, Any>(
                "ownerId" to uid,
                "displayName" to rawName,
                "handle" to handle,
                "bio" to "Sharing videos on BlueStream.",
                "subscriberCount" to 0L,
                "videoCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!avatar.isNullOrBlank()) {
                payload["avatarUrl"] = avatar
            }
            docRef.set(payload).await()
            Result.success(
                Channel(
                    id = uid,
                    ownerId = uid,
                    displayName = rawName,
                    handle = handle,
                    bio = "Sharing videos on BlueStream.",
                    avatarUrl = avatar,
                    subscriberCount = 0L,
                    videoCount = 0L
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun updateChannelProfile(
        displayName: String,
        handle: String,
        bio: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "channels/$uid"
        return try {
            val cleanName = displayName.trim().take(60)
            val cleanHandle = handle.trim().let { if (it.startsWith("@")) it else "@$it" }.take(36)
            val cleanBio = bio.trim().take(300)
            require(cleanName.isNotEmpty()) { "Channel name cannot be empty." }
            require(cleanHandle.length >= 2) { "Handle must be at least 2 characters." }

            db.collection("channels").document(uid).update(
                mapOf(
                    "displayName" to cleanName,
                    "handle" to cleanHandle,
                    "bio" to cleanBio,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun uploadVideo(
        channel: Channel,
        title: String,
        description: String,
        category: String,
        videoUrl: String,
        thumbnailUrl: String?,
        durationSeconds: Long,
        visibility: String
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        var docPath = "videos"
        return try {
            val cleanTitle = title.trim().take(120)
            val cleanDesc = description.trim().take(2000)
            val cleanVideoUrl = videoUrl.trim().take(1500)
            val cleanCategory = if (category in UPLOAD_CATEGORIES) category else "Tech"
            val cleanVisibility = if (visibility == "private") "private" else "public"
            val cleanDuration = durationSeconds.coerceIn(0L, 43200L)
            // Regular creators enter pending moderation before public publishing; Owner is pre-approved
            val initialModerationStatus = if (isCurrentUserAdmin()) "approved" else "pending"

            require(cleanTitle.isNotEmpty()) { "Video title is required." }
            require(cleanVideoUrl.isNotEmpty()) { "Please select a video or paste a video URL." }

            val docRef = db.collection("videos").document()
            docPath = docRef.path

            val payload = mutableMapOf<String, Any>(
                "ownerId" to uid,
                "channelName" to channel.displayName.ifBlank { "BlueStream Creator" }.take(60),
                "channelHandle" to channel.handle.ifBlank { "@creator" }.take(36),
                "title" to cleanTitle,
                "description" to cleanDesc,
                "category" to cleanCategory,
                "videoUrl" to cleanVideoUrl,
                "durationSeconds" to cleanDuration,
                "visibility" to cleanVisibility,
                "moderationStatus" to initialModerationStatus,
                "featured" to false,
                "viewsCount" to 0L,
                "likesCount" to 0L,
                "commentsCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            channel.avatarUrl?.takeIf { it.isNotBlank() }?.let {
                payload["channelAvatarUrl"] = it.take(500)
            }
            thumbnailUrl?.takeIf { it.isNotBlank() }?.let {
                payload["thumbnailUrl"] = it.take(1500)
            }

            docRef.set(payload).await()

            runCatching {
                db.collection("channels").document(uid).update(
                    mapOf(
                        "videoCount" to FieldValue.increment(1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun updateVideoMetadata(
        videoId: String,
        title: String,
        description: String,
        category: String,
        visibility: String
    ): Result<Unit> {
        val path = "videos/$videoId"
        return try {
            requireUserId()
            val cleanTitle = title.trim().take(120)
            val cleanDesc = description.trim().take(2000)
            val cleanCategory = if (category in UPLOAD_CATEGORIES) category else "Tech"
            val cleanVisibility = if (visibility == "private") "private" else "public"
            require(cleanTitle.isNotEmpty()) { "Title cannot be empty." }

            db.collection("videos").document(videoId).update(
                mapOf(
                    "title" to cleanTitle,
                    "description" to cleanDesc,
                    "category" to cleanCategory,
                    "visibility" to cleanVisibility,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun moderateVideo(videoId: String, status: String): Result<Unit> {
        val path = "videos/$videoId"
        return try {
            requireUserId()
            require(isCurrentUserAdmin()) { "Only the platform owner can moderate videos." }
            require(status in listOf("approved", "rejected", "pending")) { "Invalid moderation status." }
            db.collection("videos").document(videoId).update(
                mapOf(
                    "moderationStatus" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun toggleFeaturedVideo(videoId: String, featured: Boolean): Result<Unit> {
        val path = "videos/$videoId"
        return try {
            requireUserId()
            require(isCurrentUserAdmin()) { "Only the platform owner can feature videos." }
            db.collection("videos").document(videoId).update(
                mapOf(
                    "featured" to featured,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteVideo(video: Video): Result<Unit> {
        val path = "videos/${video.id}"
        return try {
            val uid = requireUserId()
            db.collection("videos").document(video.id).delete().await()
            if (video.ownerId == uid) {
                runCatching {
                    db.collection("channels").document(uid).update(
                        mapOf(
                            "videoCount" to FieldValue.increment(-1),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    override suspend fun incrementVideoView(video: Video): Result<Unit> {
        if (video.visibility != "public" || video.moderationStatus != "approved" || video.id.isBlank()) {
            return Result.success(Unit)
        }
        val path = "videos/${video.id}"
        return try {
            requireUserId()
            db.collection("videos").document(video.id)
                .update("viewsCount", FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun toggleLike(video: Video, currentlyLiked: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val likePath = "users/$uid/likes/${video.id}"
        return try {
            val likeRef = db.collection("users").document(uid).collection("likes").document(video.id)
            val videoRef = db.collection("videos").document(video.id)
            val canUpdatePublicCounter = video.visibility == "public" && video.moderationStatus == "approved"
            if (currentlyLiked) {
                likeRef.delete().await()
                if (canUpdatePublicCounter && video.likesCount > 0L) {
                    runCatching { videoRef.update("likesCount", FieldValue.increment(-1)).await() }
                }
            } else {
                val payload = mutableMapOf<String, Any>(
                    "videoId" to video.id,
                    "videoTitle" to video.title.take(120),
                    "channelName" to video.channelName.take(60),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                video.thumbnailUrl?.takeIf { it.isNotBlank() }?.let {
                    payload["thumbnailUrl"] = it.take(1500)
                }
                likeRef.set(payload).await()
                if (canUpdatePublicCounter) {
                    runCatching { videoRef.update("likesCount", FieldValue.increment(1)).await() }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, likePath)
            Result.failure(e)
        }
    }

    override suspend fun toggleSubscription(
        channelId: String,
        channelName: String,
        channelHandle: String,
        channelAvatarUrl: String?,
        currentlySubscribed: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val subPath = "users/$uid/subscriptions/$channelId"
        return try {
            val subRef = db.collection("users").document(uid)
                .collection("subscriptions").document(channelId)
            val channelRef = db.collection("channels").document(channelId)
            if (currentlySubscribed) {
                subRef.delete().await()
                runCatching {
                    channelRef.update(
                        mapOf(
                            "subscriberCount" to FieldValue.increment(-1),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } else {
                val payload = mutableMapOf<String, Any>(
                    "channelId" to channelId,
                    "channelName" to channelName.ifBlank { "Creator" }.take(60),
                    "channelHandle" to channelHandle.ifBlank { "@creator" }.take(36),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                channelAvatarUrl?.takeIf { it.isNotBlank() }?.let {
                    payload["channelAvatarUrl"] = it.take(500)
                }
                subRef.set(payload).await()
                runCatching {
                    channelRef.update(
                        mapOf(
                            "subscriberCount" to FieldValue.increment(1),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, subPath)
            Result.failure(e)
        }
    }

    override suspend fun toggleWatchLater(video: Video, currentlySaved: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/watch_later/${video.id}"
        return try {
            val ref = db.collection("users").document(uid)
                .collection("watch_later").document(video.id)
            if (currentlySaved) {
                ref.delete().await()
            } else {
                val payload = mutableMapOf<String, Any>(
                    "videoId" to video.id,
                    "videoTitle" to video.title.take(120),
                    "channelName" to video.channelName.take(60),
                    "durationSeconds" to video.durationSeconds.coerceIn(0L, 43200L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                video.thumbnailUrl?.takeIf { it.isNotBlank() }?.let {
                    payload["thumbnailUrl"] = it.take(1500)
                }
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun addComment(
        video: Video,
        text: String,
        authorName: String,
        authorAvatarUrl: String?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        var path = "videos/${video.id}/comments"
        return try {
            val cleanText = text.trim().take(600)
            require(cleanText.isNotEmpty()) { "Comment cannot be empty." }
            val commentRef = db.collection("videos").document(video.id)
                .collection("comments").document()
            path = commentRef.path

            val payload = mutableMapOf<String, Any>(
                "videoId" to video.id,
                "authorId" to uid,
                "authorName" to authorName.ifBlank { "BlueStream User" }.take(60),
                "text" to cleanText,
                "likesCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp()
            )
            authorAvatarUrl?.takeIf { it.isNotBlank() }?.let {
                payload["authorAvatarUrl"] = it.take(500)
            }
            commentRef.set(payload).await()

            if (video.visibility == "public" && video.moderationStatus == "approved") {
                runCatching {
                    db.collection("videos").document(video.id)
                        .update("commentsCount", FieldValue.increment(1))
                        .await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteComment(video: Video, commentId: String): Result<Unit> {
        val path = "videos/${video.id}/comments/$commentId"
        return try {
            requireUserId()
            db.collection("videos").document(video.id)
                .collection("comments").document(commentId)
                .delete()
                .await()
            if (video.visibility == "public" && video.moderationStatus == "approved" && video.commentsCount > 0L) {
                runCatching {
                    db.collection("videos").document(video.id)
                        .update("commentsCount", FieldValue.increment(-1))
                        .await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    override suspend fun reportVideo(video: Video, reason: String, details: String?): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        var path = "reports"
        return try {
            val cleanReason = reason.trim().take(100)
            require(cleanReason.isNotEmpty()) { "Please select a report reason." }
            val reportRef = db.collection("reports").document()
            path = reportRef.path
            val payload = mutableMapOf<String, Any>(
                "videoId" to video.id,
                "videoTitle" to video.title.take(120),
                "videoOwnerId" to video.ownerId,
                "reporterId" to uid,
                "reason" to cleanReason,
                "status" to "open",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            details?.trim()?.takeIf { it.isNotEmpty() }?.let {
                payload["details"] = it.take(500)
            }
            reportRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun updateReportStatus(reportId: String, status: String): Result<Unit> {
        val path = "reports/$reportId"
        return try {
            requireUserId()
            require(isCurrentUserAdmin()) { "Only the platform owner can resolve reports." }
            require(status in listOf("resolved", "dismissed")) { "Invalid report status." }
            db.collection("reports").document(reportId).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
