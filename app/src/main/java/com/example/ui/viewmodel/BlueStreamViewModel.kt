package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BlueStreamRepository
import com.example.data.Channel
import com.example.data.PLATFORM_OWNER_EMAIL
import com.example.data.UiState
import com.example.data.UserLike
import com.example.data.UserSubscription
import com.example.data.Video
import com.example.data.VideoComment
import com.example.data.WatchLaterItem
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BlueStreamViewModel(
    private val repository: BlueStreamRepository,
    val currentUserId: String,
    private val currentUser: FirebaseUser
) : ViewModel() {

    val isPlatformOwner: Boolean =
        currentUser.email.equals(PLATFORM_OWNER_EMAIL, ignoreCase = true) && currentUser.isEmailVerified

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeWatchVideoId = MutableStateFlow<String?>(null)
    val activeWatchVideoId: StateFlow<String?> = _activeWatchVideoId.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val viewedVideoIds = mutableSetOf<String>()

    private fun <T> kotlinx.coroutines.flow.Flow<T>.toUiState(
        tag: String,
        fallbackMessage: String
    ): StateFlow<UiState<T>> = this
        .map<T, UiState<T>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "$tag error", rootError)
            emit(UiState.Error(rootError.message ?: fallbackMessage))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val publicVideosState: StateFlow<UiState<List<Video>>> =
        repository.observePublicVideos(currentUserId)
            .toUiState("publicVideos", "Unable to load videos")

    val myVideosState: StateFlow<UiState<List<Video>>> =
        repository.observeUserVideos(currentUserId)
            .toUiState("myVideos", "Unable to load your studio videos")

    val myChannelState: StateFlow<Channel?> =
        repository.observeChannel(currentUserId, currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "myChannel error", firestoreError ?: error)
                emit(null)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null
            )

    val likesState: StateFlow<List<UserLike>> =
        repository.observeLikes(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val subscriptionsState: StateFlow<List<UserSubscription>> =
        repository.observeSubscriptions(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val watchLaterState: StateFlow<List<WatchLaterItem>> =
        repository.observeWatchLater(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val activeCommentsState: StateFlow<UiState<List<VideoComment>>> =
        _activeWatchVideoId.flatMapLatest { videoId ->
            if (videoId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeComments(videoId, currentUserId)
                    .map<List<VideoComment>, UiState<List<VideoComment>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        emit(UiState.Error(rootError.message ?: "Failed to load comments"))
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            repository.ensureChannelExists(currentUser)
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun openVideo(video: Video) {
        _activeWatchVideoId.value = video.id
        if (video.id.isNotBlank() && viewedVideoIds.add(video.id)) {
            viewModelScope.launch {
                repository.incrementVideoView(video)
            }
        }
    }

    fun closeVideo() {
        _activeWatchVideoId.value = null
    }

    fun uploadVideo(
        title: String,
        description: String,
        category: String,
        videoUrl: String,
        thumbnailUrl: String?,
        durationSeconds: Long,
        visibility: String,
        onSuccess: (String) -> Unit
    ) {
        if (_isUploading.value) return
        viewModelScope.launch {
            _isUploading.value = true
            val channel = myChannelState.value ?: repository.ensureChannelExists(currentUser).getOrNull()
                ?: Channel(
                    id = currentUserId,
                    ownerId = currentUserId,
                    displayName = currentUser.displayName ?: "BlueStream Creator",
                    handle = "@creator",
                    avatarUrl = currentUser.photoUrl?.toString()
                )
            val result = repository.uploadVideo(
                channel = channel,
                title = title,
                description = description,
                category = category,
                videoUrl = videoUrl,
                thumbnailUrl = thumbnailUrl,
                durationSeconds = durationSeconds,
                visibility = visibility
            )
            _isUploading.value = false
            result.onSuccess { newId ->
                _statusBanner.value = "Video published to BlueStream!"
                onSuccess(newId)
            }.onFailure { err ->
                _statusBanner.value = err.message ?: "Upload failed"
            }
        }
    }

    fun updateVideoMetadata(
        videoId: String,
        title: String,
        description: String,
        category: String,
        visibility: String
    ) {
        viewModelScope.launch {
            repository.updateVideoMetadata(videoId, title, description, category, visibility)
                .onSuccess { _statusBanner.value = "Video updated" }
                .onFailure { _statusBanner.value = it.message ?: "Could not update video" }
        }
    }

    fun deleteVideo(video: Video, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteVideo(video)
                .onSuccess {
                    if (_activeWatchVideoId.value == video.id) {
                        _activeWatchVideoId.value = null
                    }
                    _statusBanner.value = "Video deleted"
                    onDeleted()
                }
                .onFailure { _statusBanner.value = it.message ?: "Could not delete video" }
        }
    }

    fun toggleFeatured(video: Video) {
        if (!isPlatformOwner) return
        viewModelScope.launch {
            repository.toggleFeaturedVideo(video.id, !video.featured)
                .onSuccess {
                    _statusBanner.value = if (!video.featured) "Featured on Home Spotlight" else "Removed from Spotlight"
                }
                .onFailure { _statusBanner.value = it.message ?: "Could not update featured status" }
        }
    }

    fun toggleLike(video: Video) {
        val currentlyLiked = likesState.value.any { it.videoId == video.id }
        viewModelScope.launch {
            repository.toggleLike(video, currentlyLiked)
                .onFailure { _statusBanner.value = it.message ?: "Could not update like" }
        }
    }

    fun toggleSubscription(video: Video) {
        if (video.ownerId == currentUserId) {
            _statusBanner.value = "You cannot subscribe to your own channel."
            return
        }
        val currentlySubscribed = subscriptionsState.value.any { it.channelId == video.ownerId }
        viewModelScope.launch {
            repository.toggleSubscription(
                channelId = video.ownerId,
                channelName = video.channelName,
                channelHandle = video.channelHandle,
                channelAvatarUrl = video.channelAvatarUrl,
                currentlySubscribed = currentlySubscribed
            ).onSuccess {
                _statusBanner.value = if (currentlySubscribed) "Unsubscribed from ${video.channelName}"
                else "Subscribed to ${video.channelName}"
            }.onFailure {
                _statusBanner.value = it.message ?: "Could not update subscription"
            }
        }
    }

    fun toggleWatchLater(video: Video) {
        val currentlySaved = watchLaterState.value.any { it.videoId == video.id }
        viewModelScope.launch {
            repository.toggleWatchLater(video, currentlySaved)
                .onSuccess {
                    _statusBanner.value = if (currentlySaved) "Removed from Watch Later" else "Saved to Watch Later"
                }
                .onFailure { _statusBanner.value = it.message ?: "Could not update Watch Later" }
        }
    }

    fun addComment(video: Video, text: String) {
        val authorName = myChannelState.value?.displayName?.takeIf { it.isNotBlank() }
            ?: currentUser.displayName?.takeIf { it.isNotBlank() }
            ?: "BlueStream User"
        val avatar = myChannelState.value?.avatarUrl ?: currentUser.photoUrl?.toString()
        viewModelScope.launch {
            repository.addComment(video, text, authorName, avatar)
                .onFailure { _statusBanner.value = it.message ?: "Could not post comment" }
        }
    }

    fun deleteComment(video: Video, commentId: String) {
        viewModelScope.launch {
            repository.deleteComment(video, commentId)
                .onFailure { _statusBanner.value = it.message ?: "Could not delete comment" }
        }
    }

    fun updateChannelProfile(displayName: String, handle: String, bio: String) {
        viewModelScope.launch {
            repository.updateChannelProfile(displayName, handle, bio)
                .onSuccess { _statusBanner.value = "Channel profile updated" }
                .onFailure { _statusBanner.value = it.message ?: "Could not update profile" }
        }
    }

    private companion object {
        const val TAG = "BlueStreamVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
