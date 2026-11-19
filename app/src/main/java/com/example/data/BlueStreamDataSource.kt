package com.example.data

import kotlinx.coroutines.flow.Flow

interface BlueStreamDataSource {
    fun observePublicVideos(currentUserId: String): Flow<List<Video>>
    fun observeUserVideos(userId: String): Flow<List<Video>>
    fun observePendingModerationVideos(currentUserId: String): Flow<List<Video>>
    fun observeOpenReports(currentUserId: String): Flow<List<VideoReport>>
    fun observeChannel(channelId: String, currentUserId: String): Flow<Channel?>
    fun observeComments(videoId: String, currentUserId: String): Flow<List<VideoComment>>
    fun observeLikes(userId: String): Flow<List<UserLike>>
    fun observeSubscriptions(userId: String): Flow<List<UserSubscription>>
    fun observeWatchLater(userId: String): Flow<List<WatchLaterItem>>

    suspend fun ensureChannelExists(
        uid: String,
        displayName: String?,
        avatarUrl: String?
    ): Result<Channel>

    suspend fun updateChannelProfile(
        displayName: String,
        handle: String,
        bio: String
    ): Result<Unit>

    suspend fun uploadVideo(
        channel: Channel,
        title: String,
        description: String,
        category: String,
        videoUrl: String,
        thumbnailUrl: String?,
        durationSeconds: Long,
        visibility: String
    ): Result<String>

    suspend fun updateVideoMetadata(
        videoId: String,
        title: String,
        description: String,
        category: String,
        visibility: String
    ): Result<Unit>

    suspend fun moderateVideo(videoId: String, status: String): Result<Unit>
    suspend fun toggleFeaturedVideo(videoId: String, featured: Boolean): Result<Unit>
    suspend fun deleteVideo(video: Video): Result<Unit>
    suspend fun incrementVideoView(video: Video): Result<Unit>
    suspend fun toggleLike(video: Video, currentlyLiked: Boolean): Result<Unit>
    suspend fun toggleSubscription(
        channelId: String,
        channelName: String,
        channelHandle: String,
        channelAvatarUrl: String?,
        currentlySubscribed: Boolean
    ): Result<Unit>

    suspend fun toggleWatchLater(video: Video, currentlySaved: Boolean): Result<Unit>
    suspend fun addComment(
        video: Video,
        text: String,
        authorName: String,
        authorAvatarUrl: String?
    ): Result<Unit>

    suspend fun deleteComment(video: Video, commentId: String): Result<Unit>
    suspend fun reportVideo(video: Video, reason: String, details: String?): Result<Unit>
    suspend fun updateReportStatus(reportId: String, status: String): Result<Unit>
}
