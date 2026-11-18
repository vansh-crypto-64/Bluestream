package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.BlueStreamRepository
import com.example.data.LocalBlueStreamRepository
import com.example.data.PLATFORM_OWNER_EMAIL
import com.example.data.UiState
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.getSafeFirebaseAuth
import com.example.ui.auth.signOutUser
import com.example.ui.components.BlueStreamBrandMark
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryProfileScreen
import com.example.ui.screens.ShortsScreen
import com.example.ui.screens.SubscriptionsScreen
import com.example.ui.screens.UploadScreen
import com.example.ui.screens.WatchScreen
import com.example.ui.theme.BlueStreamPrimary
import com.example.ui.theme.BlueStreamSecondary
import com.example.ui.theme.BlueStreamTheme
import com.example.ui.viewmodel.BlueStreamViewModel
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.flowOf

private enum class BlueStreamTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Home("Home", Icons.Filled.Home, Icons.Outlined.Home),
    Shorts("Shorts", Icons.Filled.Whatshot, Icons.Outlined.Whatshot),
    Upload("Upload", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline),
    Subscriptions("Subs", Icons.Filled.Subscriptions, Icons.Outlined.Subscriptions),
    You("You", Icons.Filled.Person, Icons.Outlined.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkBlueTheme by rememberSaveable { mutableStateOf(true) }
            BlueStreamTheme(darkTheme = isDarkBlueTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        isDarkBlueTheme = isDarkBlueTheme,
                        onToggleTheme = { isDarkBlueTheme = !isDarkBlueTheme }
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    isDarkBlueTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val safeAuth = remember(context) { getSafeFirebaseAuth(context) }

    // Automatically open in Local Fallback Mode if Firebase / google-services.json is missing
    var useLocalMode by rememberSaveable { mutableStateOf(safeAuth == null) }

    val authFlow = remember(safeAuth) {
        safeAuth?.authStateFlow() ?: flowOf(null)
    }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = safeAuth?.currentUser)

    when {
        useLocalMode || safeAuth == null -> {
            key("local_mode") {
                BlueStreamShell(
                    firebaseUser = null,
                    isLocalMode = true,
                    isDarkBlueTheme = isDarkBlueTheme,
                    onToggleTheme = onToggleTheme,
                    onExitLocalMode = {
                        if (safeAuth != null) useLocalMode = false
                    }
                )
            }
        }

        currentUser == null -> {
            AuthScreen(
                onAuthSuccess = {},
                onContinueLocalMode = { useLocalMode = true }
            )
        }

        else -> {
            val user = currentUser!!
            key(user.uid) {
                BlueStreamShell(
                    firebaseUser = user,
                    isLocalMode = false,
                    isDarkBlueTheme = isDarkBlueTheme,
                    onToggleTheme = onToggleTheme,
                    onExitLocalMode = {}
                )
            }
        }
    }
}

@Composable
private fun BlueStreamShell(
    firebaseUser: FirebaseUser?,
    isLocalMode: Boolean,
    isDarkBlueTheme: Boolean,
    onToggleTheme: () -> Unit,
    onExitLocalMode: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val sessionKey = if (isLocalMode || firebaseUser == null) {
        LocalBlueStreamRepository.LOCAL_OWNER_UID
    } else {
        firebaseUser.uid
    }

    val viewModel: BlueStreamViewModel = viewModel(
        key = sessionKey,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                if (isLocalMode || firebaseUser == null) {
                    BlueStreamViewModel(
                        repository = LocalBlueStreamRepository(),
                        currentUserId = LocalBlueStreamRepository.LOCAL_OWNER_UID,
                        userDisplayName = "BlueStream Studio",
                        userAvatarUrl = null,
                        isPlatformOwner = true,
                        isLocalMode = true
                    )
                } else {
                    val dataSource = runCatching {
                        val databaseId = app.getString(R.string.firestore_database_id)
                        require(databaseId.isNotBlank()) { "Missing firestore_database_id" }
                        BlueStreamRepository(FirebaseFirestore.getInstance(databaseId))
                    }.getOrElse {
                        LocalBlueStreamRepository(firebaseUser.uid)
                    }
                    val isOwner = firebaseUser.email.equals(PLATFORM_OWNER_EMAIL, ignoreCase = true) &&
                        firebaseUser.isEmailVerified
                    BlueStreamViewModel(
                        repository = dataSource,
                        currentUserId = firebaseUser.uid,
                        userDisplayName = firebaseUser.displayName,
                        userAvatarUrl = firebaseUser.photoUrl?.toString(),
                        isPlatformOwner = isOwner,
                        isLocalMode = dataSource is LocalBlueStreamRepository
                    )
                }
            }
        }
    )

    val publicVideosState by viewModel.publicVideosState.collectAsStateWithLifecycle()
    val myVideosState by viewModel.myVideosState.collectAsStateWithLifecycle()
    val myChannel by viewModel.myChannelState.collectAsStateWithLifecycle()
    val likes by viewModel.likesState.collectAsStateWithLifecycle()
    val subscriptions by viewModel.subscriptionsState.collectAsStateWithLifecycle()
    val watchLater by viewModel.watchLaterState.collectAsStateWithLifecycle()
    val commentsState by viewModel.activeCommentsState.collectAsStateWithLifecycle()

    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeWatchVideoId by viewModel.activeWatchVideoId.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val isUploading by viewModel.isUploading.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(BlueStreamTab.Home) }
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusBanner) {
        val message = statusBanner ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearStatusBanner()
    }

    if (activeWatchVideoId == null && currentTab != BlueStreamTab.Home) {
        BackHandler {
            currentTab = BlueStreamTab.Home
        }
    }

    val likedIds = remember(likes) { likes.map { it.videoId }.toSet() }
    val subscribedIds = remember(subscriptions) { subscriptions.map { it.channelId }.toSet() }
    val watchLaterIds = remember(watchLater) { watchLater.map { it.videoId }.toSet() }

    val allKnownVideos = remember(publicVideosState, myVideosState) {
        val pub = (publicVideosState as? UiState.Success)?.data ?: emptyList()
        val mine = (myVideosState as? UiState.Success)?.data ?: emptyList()
        (pub + mine).distinctBy { it.id }
    }
    val activeWatchVideo = remember(activeWatchVideoId, allKnownVideos) {
        activeWatchVideoId?.let { id -> allKnownVideos.firstOrNull { it.id == id } }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 840.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (activeWatchVideo == null) {
                    BlueStreamTopBar(
                        isWideLayout = useNavigationRail,
                        isDarkBlueTheme = isDarkBlueTheme,
                        isSearchExpanded = isSearchExpanded,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                        onToggleSearch = {
                            isSearchExpanded = !isSearchExpanded
                            if (!isSearchExpanded) viewModel.updateSearchQuery("")
                        },
                        onToggleTheme = onToggleTheme,
                        onUploadShortcut = { currentTab = BlueStreamTab.Upload }
                    )
                }
            },
            bottomBar = {
                if (!useNavigationRail && activeWatchVideo == null) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        BlueStreamTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = BlueStreamPrimary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail && activeWatchVideo == null) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(Modifier.height(12.dp))
                        BlueStreamTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = BlueStreamPrimary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (activeWatchVideo != null) {
                        val upNext = allKnownVideos.filter { it.id != activeWatchVideo.id && it.visibility == "public" }
                        WatchScreen(
                            video = activeWatchVideo,
                            upNextVideos = upNext,
                            commentsState = commentsState,
                            isLiked = activeWatchVideo.id in likedIds,
                            isSubscribed = activeWatchVideo.ownerId in subscribedIds,
                            isSavedWatchLater = activeWatchVideo.id in watchLaterIds,
                            currentUserId = viewModel.currentUserId,
                            isPlatformOwner = viewModel.isPlatformOwner,
                            onBack = { viewModel.closeVideo() },
                            onSelectVideo = { viewModel.openVideo(it) },
                            onToggleLike = { viewModel.toggleLike(activeWatchVideo) },
                            onToggleSubscribe = { viewModel.toggleSubscription(activeWatchVideo) },
                            onToggleWatchLater = { viewModel.toggleWatchLater(activeWatchVideo) },
                            onToggleFeatured = { viewModel.toggleFeatured(activeWatchVideo) },
                            onDeleteVideo = { viewModel.deleteVideo(activeWatchVideo) },
                            onAddComment = { text -> viewModel.addComment(activeWatchVideo, text) },
                            onDeleteComment = { commentId -> viewModel.deleteComment(activeWatchVideo, commentId) },
                            modifier = Modifier.statusBarsPadding()
                        )
                    } else {
                        when (currentTab) {
                            BlueStreamTab.Home -> HomeScreen(
                                videosState = publicVideosState,
                                selectedCategory = selectedCategory,
                                searchQuery = searchQuery,
                                watchLaterIds = watchLaterIds,
                                onSelectCategory = { viewModel.selectCategory(it) },
                                onVideoClick = { viewModel.openVideo(it) },
                                onToggleWatchLater = { viewModel.toggleWatchLater(it) },
                                onNavigateToUpload = { currentTab = BlueStreamTab.Upload }
                            )

                            BlueStreamTab.Shorts -> ShortsScreen(
                                videosState = publicVideosState,
                                likedVideoIds = likedIds,
                                subscribedChannelIds = subscribedIds,
                                currentUserId = viewModel.currentUserId,
                                onWatchVideo = { viewModel.openVideo(it) },
                                onToggleLike = { viewModel.toggleLike(it) },
                                onToggleSubscribe = { viewModel.toggleSubscription(it) },
                                onNavigateToUpload = { currentTab = BlueStreamTab.Upload }
                            )

                            BlueStreamTab.Upload -> UploadScreen(
                                isUploading = isUploading,
                                onPublish = { title, desc, cat, url, thumb, dur, vis ->
                                    viewModel.uploadVideo(
                                        title = title,
                                        description = desc,
                                        category = cat,
                                        videoUrl = url,
                                        thumbnailUrl = thumb,
                                        durationSeconds = dur,
                                        visibility = vis,
                                        onSuccess = {
                                            currentTab = BlueStreamTab.Home
                                        }
                                    )
                                }
                            )

                            BlueStreamTab.Subscriptions -> SubscriptionsScreen(
                                subscriptions = subscriptions,
                                publicVideosState = publicVideosState,
                                watchLaterIds = watchLaterIds,
                                onVideoClick = { viewModel.openVideo(it) },
                                onToggleWatchLater = { viewModel.toggleWatchLater(it) },
                                onExploreHome = { currentTab = BlueStreamTab.Home }
                            )

                            BlueStreamTab.You -> LibraryProfileScreen(
                                channel = myChannel,
                                userEmail = if (viewModel.isLocalMode) "Local Mode (Offline)" else firebaseUser?.email,
                                isPlatformOwner = viewModel.isPlatformOwner,
                                myVideosState = myVideosState,
                                publicVideosState = publicVideosState,
                                watchLaterItems = watchLater,
                                likedItems = likes,
                                onVideoClick = { viewModel.openVideo(it) },
                                onDeleteVideo = { viewModel.deleteVideo(it) },
                                onToggleFeatured = { viewModel.toggleFeatured(it) },
                                onUpdateChannelProfile = { name, handle, bio ->
                                    viewModel.updateChannelProfile(name, handle, bio)
                                },
                                onSignOut = {
                                    if (isLocalMode) {
                                        onExitLocalMode()
                                    } else {
                                        signOutUser(context, credentialManager, scope)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlueStreamTopBar(
    isWideLayout: Boolean,
    isDarkBlueTheme: Boolean,
    isSearchExpanded: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onToggleTheme: () -> Unit,
    onUploadShortcut: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (!isWideLayout && isSearchExpanded) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search BlueStream videos...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        IconButton(onClick = onToggleSearch) {
                            Icon(Icons.Filled.Close, contentDescription = "Close search")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mobile_search_input")
                )
            } else {
                BlueStreamBrandMark()

                if (isWideLayout) {
                    Spacer(Modifier.width(24.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search videos, channels, categories...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("desktop_search_input")
                    )
                    Spacer(Modifier.width(16.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!isWideLayout) {
                        IconButton(
                            onClick = onToggleSearch,
                            modifier = Modifier.testTag("open_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search videos",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Button(
                            onClick = onUploadShortcut,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BlueStreamPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("topbar_upload_button")
                        ) {
                            Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create")
                        }
                        Spacer(Modifier.width(6.dp))
                    }

                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("toggle_theme_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkBlueTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (isDarkBlueTheme) "Switch to Light Blue theme" else "Switch to Dark Cobalt theme",
                            tint = BlueStreamSecondary
                        )
                    }
                }
            }
        }
    }
}
