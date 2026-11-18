package com.example.data

import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Safe in-memory fallback repository used when Firebase / google-services.json
 * is not initialized or when the user chooses Local Mode.
 */
class LocalBlueStreamRepository(
    private val localUserId: String = LOCAL_OWNER_UID
) : BlueStreamDataSource {

    private val channels = MutableStateFlow(
        mapOf(
            localUserId to Channel(
                id = localUserId,
                ownerId = localUserId,
                displayName = "BlueStream Studio",
                handle = "@bluestream",
                bio = "Official BlueStream channel running in Local Mode.",
                subscriberCount = 1240L,
                videoCount = 2L,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            ),
            "creator_horizon" to Channel(
                id = "creator_horizon",
                ownerId = "creator_horizon",
                displayName = "Horizon Cinema",
                handle = "@horizoncinema",
                bio = "4K travel films and aerial twilight cinematography.",
                subscriberCount = 8420L,
                videoCount = 1L,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        )
    )

    private val videos = MutableStateFlow(
        listOf(
            Video(
                id = "local_vid_1",
                ownerId = localUserId,
                channelName = "BlueStream Studio",
                channelHandle = "@bluestream",
                title = "Inside the Electric Blue Creator Desk Setup",
                description = "Welcome to BlueStream! Upload videos from your device gallery or paste any direct MP4/HLS stream URL to watch smoothly on phones and laptops.",
                category = "Tech",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                thumbnailUrl = "preset:tech",
                durationSeconds = 596L,
                visibility = "public",
                featured = true,
                viewsCount = 4820L,
                likesCount = 342L,
                commentsCount = 1L,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            ),
            Video(
                id = "local_vid_2",
                ownerId = "creator_horizon",
                channelName = "Horizon Cinema",
                channelHandle = "@horizoncinema",
                title = "Sapphire Bridges: Coastal Metropolis at Twilight",
                description = "Aerial twilight footage over glowing cobalt bridges and calm ocean waters.",
                category = "Travel",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                thumbnailUrl = "preset:travel",
                durationSeconds = 653L,
                visibility = "public",
                featured = false,
                viewsCount = 1950L,
                likesCount = 128L,
                commentsCount = 0L,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            ),
            Video(
                id = "local_short_1",
                ownerId = localUserId,
                channelName = "BlueStream Studio",
                channelHandle = "@bluestream",
                title = "BlueStream Shorts: 15-Second Studio Glow",
                description = "Quick vertical highlight from our cobalt-blue studio.",
                category = "Shorts",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                thumbnailUrl = "preset:hero",
                durationSeconds = 15L,
                visibility = "public",
                featured = false,
                viewsCount = 910L,
                likesCount = 87L,
                commentsCount = 0L,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        )
    )

    private val commentsByVideo = MutableStateFlow(
        mapOf(
            "local_vid_1" to listOf(
                VideoComment(
                    id = "local_comment_1",
                    videoId = "local_vid_1",
                    authorId = "creator_horizon",
                    authorName = "Horizon Cinema",
                    text = "Love the cobalt blue theme on both phone and laptop!",
                    likesCount = 5L,
                    createdAt = Timestamp.now()
                )
            )
        )
    )

    private val likes = MutableStateFlow<List<UserLike>>(emptyList())
    private val subscriptions = MutableStateFlow<List<UserSubscription>>(emptyList())
    private val watchLater = MutableStateFlow<List<WatchLaterItem>>(emptyList())

    override fun observePublicVideos(currentUserId: String): Flow<List<Video>> =
        videos.map { list ->
            list.filter { it.visibility == "public" }
                .sortedWith(
                    compareByDescending<Video> { it.featured }
                        .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                )
        }

    override fun observeUserVideos(userId: String): Flow<List<Video>> =
        videos.map { list ->
            list.filter { it.ownerId == userId }
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    override fun observeChannel(channelId: String, currentUserId: String): Flow<Channel?> =
        channels.map { it[channelId] }

    override fun observeComments(videoId: String, currentUserId: String): Flow<List<VideoComment>> =
        commentsByVideo.map { map ->
            (map[videoId] ?: emptyList()).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }

    override fun observeLikes(userId: String): Flow<List<UserLike>> = likes
    override fun observeSubscriptions(userId: String): Flow<List<UserSubscription>> = subscriptions
    override fun observeWatchLater(userId: String): Flow<List<WatchLaterItem>> = watchLater

    override suspend fun ensureChannelExists(
        uid: String,
        displayName: String?,
        avatarUrl: String?
    ): Result<Channel> {
        val existing = channels.value[uid]
        if (existing != null) return Result.success(existing)
        val created = Channel(
            id = uid,
            ownerId = uid,
            displayName = displayName?.ifBlank { "BlueStream Studio" } ?: "BlueStream Studio",
            handle = "@bluestream",
            bio = "Sharing videos on BlueStream.",
            avatarUrl = avatarUrl,
            subscriberCount = 0L,
            videoCount = 0L,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        channels.update { it + (uid to created) }
        return Result.success(created)
    }

    override suspend fun updateChannelProfile(
        displayName: String,
        handle: String,
        bio: String
    ): Result<Unit> {
        val cleanName = displayName.trim().take(60)
        val cleanHandle = handle.trim().let { if (it.startsWith("@")) it else "@$it" }.take(36)
        val cleanBio = bio.trim().take(300)
        if (cleanName.isEmpty()) return Result.failure(IllegalArgumentException("Channel name cannot be empty."))

        channels.update { map ->
            val current = map[localUserId] ?: Channel(id = localUserId, ownerId = localUserId)
            map + (localUserId to current.copy(
                displayName = cleanName,
                handle = cleanHandle,
                bio = cleanBio,
                updatedAt = Timestamp.now()
            ))
        }
        return Result.success(Unit)
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
        val cleanTitle = title.trim().take(120)
        val cleanVideoUrl = videoUrl.trim().take(1000)
        if (cleanTitle.isEmpty()) return Result.failure(IllegalArgumentException("Video title is required."))
        if (cleanVideoUrl.isEmpty()) return Result.failure(IllegalArgumentException("Video source is required."))

        val newId = "local_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val now = Timestamp.now()
        val newVideo = Video(
            id = newId,
            ownerId = localUserId,
            channelName = channel.displayName.ifBlank { "BlueStream Studio" },
            channelHandle = channel.handle.ifBlank { "@bluestream" },
            channelAvatarUrl = channel.avatarUrl,
            title = cleanTitle,
            description = description.trim().take(2000),
            category = if (category in UPLOAD_CATEGORIES) category else "Tech",
            videoUrl = cleanVideoUrl,
            thumbnailUrl = thumbnailUrl,
            durationSeconds = durationSeconds.coerceIn(0L, 43200L),
            visibility = if (visibility == "private") "private" else "public",
            featured = false,
            viewsCount = 0L,
            likesCount = 0L,
            commentsCount = 0L,
            createdAt = now,
            updatedAt = now
        )
        videos.update { listOf(newVideo) + it }
        channels.update { map ->
            val current = map[localUserId] ?: return@update map
            map + (localUserId to current.copy(videoCount = current.videoCount + 1))
        }
        return Result.success(newId)
    }

    override suspend fun updateVideoMetadata(
        videoId: String,
        title: String,
        description: String,
        category: String,
        visibility: String
    ): Result<Unit> {
        videos.update { list ->
            list.map {
                if (it.id == videoId) {
                    it.copy(
                        title = title.trim().take(120),
                        description = description.trim().take(2000),
                        category = if (category in UPLOAD_CATEGORIES) category else it.category,
                        visibility = if (visibility == "private") "private" else "public",
                        updatedAt = Timestamp.now()
                    )
                } else it
            }
        }
        return Result.success(Unit)
    }

    override suspend fun toggleFeaturedVideo(videoId: String, featured: Boolean): Result<Unit> {
        videos.update { list ->
            list.map { if (it.id == videoId) it.copy(featured = featured, updatedAt = Timestamp.now()) else it }
        }
        return Result.success(Unit)
    }

    override suspend fun deleteVideo(video: Video): Result<Unit> {
        videos.update { list -> list.filterNot { it.id == video.id } }
        return Result.success(Unit)
    }

    override suspend fun incrementVideoView(video: Video): Result<Unit> {
        videos.update { list ->
            list.map { if (it.id == video.id) it.copy(viewsCount = it.viewsCount + 1) else it }
        }
        return Result.success(Unit)
    }

    override suspend fun toggleLike(video: Video, currentlyLiked: Boolean): Result<Unit> {
        if (currentlyLiked) {
            likes.update { list -> list.filterNot { it.videoId == video.id } }
            videos.update { list ->
                list.map {
                    if (it.id == video.id) it.copy(likesCount = (it.likesCount - 1).coerceAtLeast(0L))
                    else it
                }
            }
        } else {
            val item = UserLike(
                id = video.id,
                videoId = video.id,
                videoTitle = video.title,
                channelName = video.channelName,
                thumbnailUrl = video.thumbnailUrl,
                createdAt = Timestamp.now()
            )
            likes.update { listOf(item) + it.filterNot { existing -> existing.videoId == video.id } }
            videos.update { list ->
                list.map { if (it.id == video.id) it.copy(likesCount = it.likesCount + 1) else it }
            }
        }
        return Result.success(Unit)
    }

    override suspend fun toggleSubscription(
        channelId: String,
        channelName: String,
        channelHandle: String,
        channelAvatarUrl: String?,
        currentlySubscribed: Boolean
    ): Result<Unit> {
        if (currentlySubscribed) {
            subscriptions.update { list -> list.filterNot { it.channelId == channelId } }
            channels.update { map ->
                val ch = map[channelId] ?: return@update map
                map + (channelId to ch.copy(subscriberCount = (ch.subscriberCount - 1).coerceAtLeast(0L)))
            }
        } else {
            val sub = UserSubscription(
                id = channelId,
                channelId = channelId,
                channelName = channelName,
                channelHandle = channelHandle,
                channelAvatarUrl = channelAvatarUrl,
                createdAt = Timestamp.now()
            )
            subscriptions.update { listOf(sub) + it.filterNot { existing -> existing.channelId == channelId } }
            channels.update { map ->
                val ch = map[channelId] ?: return@update map
                map + (channelId to ch.copy(subscriberCount = ch.subscriberCount + 1))
            }
        }
        return Result.success(Unit)
    }

    override suspend fun toggleWatchLater(video: Video, currentlySaved: Boolean): Result<Unit> {
        if (currentlySaved) {
            watchLater.update { list -> list.filterNot { it.videoId == video.id } }
        } else {
            val item = WatchLaterItem(
                id = video.id,
                videoId = video.id,
                videoTitle = video.title,
                channelName = video.channelName,
                thumbnailUrl = video.thumbnailUrl,
                durationSeconds = video.durationSeconds,
                createdAt = Timestamp.now()
            )
            watchLater.update { listOf(item) + it.filterNot { existing -> existing.videoId == video.id } }
        }
        return Result.success(Unit)
    }

    override suspend fun addComment(
        video: Video,
        text: String,
        authorName: String,
        authorAvatarUrl: String?
    ): Result<Unit> {
        val cleanText = text.trim().take(600)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Comment cannot be empty."))
        val comment = VideoComment(
            id = "local_c_${UUID.randomUUID().toString().replace("-", "").take(10)}",
            videoId = video.id,
            authorId = localUserId,
            authorName = authorName.ifBlank { "BlueStream Studio" },
            authorAvatarUrl = authorAvatarUrl,
            text = cleanText,
            likesCount = 0L,
            createdAt = Timestamp.now()
        )
        commentsByVideo.update { map ->
            val current = map[video.id] ?: emptyList()
            map + (video.id to (listOf(comment) + current))
        }
        videos.update { list ->
            list.map { if (it.id == video.id) it.copy(commentsCount = it.commentsCount + 1) else it }
        }
        return Result.success(Unit)
    }

    override suspend fun deleteComment(video: Video, commentId: String): Result<Unit> {
        commentsByVideo.update { map ->
            val current = map[video.id] ?: emptyList()
            map + (video.id to current.filterNot { it.id == commentId })
        }
        videos.update { list ->
            list.map {
                if (it.id == video.id) it.copy(commentsCount = (it.commentsCount - 1).coerceAtLeast(0L))
                else it
            }
        }
        return Result.success(Unit)
    }

    companion object {
        const val LOCAL_OWNER_UID = "local_owner_bluestream"
    }
}
