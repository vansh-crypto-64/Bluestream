package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val PLATFORM_OWNER_EMAIL = "lakeskuke@gmail.com"

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

val VIDEO_CATEGORIES = listOf(
    "All",
    "Tech",
    "Music",
    "Gaming",
    "Travel",
    "Education",
    "Vlogs",
    "Sports",
    "Shorts"
)

val UPLOAD_CATEGORIES = VIDEO_CATEGORIES.filter { it != "All" }

@Keep
@IgnoreExtraProperties
data class Channel(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val subscriberCount: Long = 0L,
    val videoCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Video(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val channelName: String = "",
    val channelHandle: String = "",
    val channelAvatarUrl: String? = null,
    val title: String = "",
    val description: String = "",
    val category: String = "Tech",
    val videoUrl: String = "",
    val thumbnailUrl: String? = null,
    val durationSeconds: Long = 0L,
    val visibility: String = "public",
    val featured: Boolean = false,
    val viewsCount: Long = 0L,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VideoComment(
    @DocumentId val id: String = "",
    val videoId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val text: String = "",
    val likesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserLike(
    @DocumentId val id: String = "",
    val videoId: String = "",
    val videoTitle: String = "",
    val channelName: String = "",
    val thumbnailUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserSubscription(
    @DocumentId val id: String = "",
    val channelId: String = "",
    val channelName: String = "",
    val channelHandle: String = "",
    val channelAvatarUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WatchLaterItem(
    @DocumentId val id: String = "",
    val videoId: String = "",
    val videoTitle: String = "",
    val channelName: String = "",
    val thumbnailUrl: String? = null,
    val durationSeconds: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)
