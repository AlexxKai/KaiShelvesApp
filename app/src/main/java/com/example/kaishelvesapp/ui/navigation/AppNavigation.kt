package com.example.kaishelvesapp.ui.navigation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.kaishelvesapp.BuildConfig
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.help.HelpScreenContext
import com.example.kaishelvesapp.ui.components.GothicBackground
import com.example.kaishelvesapp.ui.components.GuestRestrictedAccessNotice
import com.example.kaishelvesapp.ui.components.GuestUiRestrictions
import com.example.kaishelvesapp.ui.components.HelpChatOverlay
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.components.LocalGuestUiRestrictions
import com.example.kaishelvesapp.ui.components.LocalOpenScanner
import com.example.kaishelvesapp.ui.components.OfflineAccessDialog
import com.example.kaishelvesapp.ui.language.LanguageManager
import com.example.kaishelvesapp.ui.screen.catalog.CatalogScreen
import com.example.kaishelvesapp.ui.screen.catalog.IsbnScannerScreen
import com.example.kaishelvesapp.ui.screen.catalog.SearchResultsScreen
import com.example.kaishelvesapp.ui.screen.detail.BookDetailScreen
import com.example.kaishelvesapp.ui.screen.foryou.ForYouScreen
import com.example.kaishelvesapp.ui.screen.friends.FriendListDetailScreen
import com.example.kaishelvesapp.ui.screen.friends.FriendListsScreen
import com.example.kaishelvesapp.ui.screen.friends.FriendProfileScreen
import com.example.kaishelvesapp.ui.screen.friends.FriendSuggestionsScreen
import com.example.kaishelvesapp.ui.screen.friends.FriendsScreen
import com.example.kaishelvesapp.ui.screen.friends.NotificationCenterScreen
import com.example.kaishelvesapp.ui.screen.help.HelpScreen
import com.example.kaishelvesapp.ui.screen.home.HomeScreen
import com.example.kaishelvesapp.ui.screen.library.DeviceLibraryScreen
import com.example.kaishelvesapp.ui.screen.library.LibraryScreen
import com.example.kaishelvesapp.ui.screen.library.PdfConverterScreen
import com.example.kaishelvesapp.ui.screen.lists.UserListDetailScreen
import com.example.kaishelvesapp.ui.screen.lists.UserListsScreen
import com.example.kaishelvesapp.ui.screen.login.EmailVerificationScreen
import com.example.kaishelvesapp.ui.screen.login.LoginScreen
import com.example.kaishelvesapp.ui.screen.placeholder.PlaceholderScreen
import com.example.kaishelvesapp.ui.screen.profile.ProfileScreen
import com.example.kaishelvesapp.ui.screen.readinglist.ReadingListScreen
import com.example.kaishelvesapp.ui.screen.register.RegisterScreen
import com.example.kaishelvesapp.ui.screen.settings.AdminUsernamesScreen
import com.example.kaishelvesapp.ui.screen.settings.SettingsPrivacyScreen
import com.example.kaishelvesapp.ui.screen.stats.ReadingStatsScreen
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.AdminUsernamesViewModel
import com.example.kaishelvesapp.ui.viewmodel.AuthViewModel
import com.example.kaishelvesapp.ui.viewmodel.BookDetailViewModel
import com.example.kaishelvesapp.ui.viewmodel.CatalogViewModel
import com.example.kaishelvesapp.ui.viewmodel.ForYouViewModel
import com.example.kaishelvesapp.ui.viewmodel.FriendListDetailViewModel
import com.example.kaishelvesapp.ui.viewmodel.FriendListsViewModel
import com.example.kaishelvesapp.ui.viewmodel.FriendProfileViewModel
import com.example.kaishelvesapp.ui.viewmodel.FriendRequestsViewModel
import com.example.kaishelvesapp.ui.viewmodel.FriendSuggestionsViewModel
import com.example.kaishelvesapp.ui.viewmodel.FriendsViewModel
import com.example.kaishelvesapp.ui.viewmodel.HelpChatViewModel
import com.example.kaishelvesapp.ui.viewmodel.HomeViewModel
import com.example.kaishelvesapp.ui.viewmodel.ReadingListViewModel
import com.example.kaishelvesapp.ui.viewmodel.SearchResultsViewModel
import com.example.kaishelvesapp.ui.viewmodel.UserListDetailViewModel
import com.example.kaishelvesapp.ui.viewmodel.UserListsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Routes {
    const val AUTH_LOADING = "auth_loading"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val EMAIL_VERIFICATION = "email_verification"
    const val HOME = "home"
    const val SEARCH = "search"
    const val SEARCH_RESULTS = "search_results"
    const val DISCOVER = "discover"
    const val SCAN_BOOKS = "scan_books"
    const val LISTS = "lists"
    const val LIST_DETAIL = "list_detail/{listId}"
    const val DETAIL = "detail"
    const val READING_LIST = "reading_list"
    const val PROFILE = "profile"
    const val SETTINGS_PRIVACY = "settings_privacy"
    const val ADMIN_USERNAMES = "admin_usernames"
    const val READING_STATS = "reading_stats"
    const val LIBRARY = "library"
    const val FRIENDS = "friends"
    const val FRIEND_SUGGESTIONS = "friend_suggestions"
    const val FRIEND_PROFILE = "friend_profile/{friendUid}"
    const val FRIEND_LISTS = "friend_lists/{friendUid}/{friendName}"
    const val FRIEND_LIST_DETAIL = "friend_list_detail/{friendUid}/{listId}"
    const val NOTIFICATION_CENTER = "notification_center"
    const val GROUPS = "groups"
    const val FILE_CONVERTER = "file_converter"
    const val CHALLENGES = "challenges"
    const val FOR_YOU = "for_you"
    const val HELP = "help"
}

fun listDetailRoute(listId: String): String = "list_detail/$listId"
fun friendProfileRoute(friendUid: String): String = "friend_profile/$friendUid"
fun friendListsRoute(friendUid: String, friendName: String): String =
    "friend_lists/$friendUid/${Uri.encode(friendName)}"
fun friendListDetailRoute(friendUid: String, listId: String): String =
    "friend_list_detail/$friendUid/${Uri.encode(listId)}"

private fun Context.hasActiveInternetConnection(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    activityNotificationToOpen: String? = null,
    onActivityNotificationOpenConsumed: () -> Unit = {},
    deviceLibraryBookToOpen: String? = null,
    onDeviceLibraryBookOpenConsumed: () -> Unit = {}
) {
    val authViewModel: AuthViewModel = viewModel()
    val catalogViewModel: CatalogViewModel = viewModel()
    val searchResultsViewModel: SearchResultsViewModel = viewModel()
    val readingListViewModel: ReadingListViewModel = viewModel()
    val friendSuggestionsViewModel: FriendSuggestionsViewModel = viewModel()
    val friendProfileViewModel: FriendProfileViewModel = viewModel()
    val friendListsViewModel: FriendListsViewModel = viewModel()
    val friendListDetailViewModel: FriendListDetailViewModel = viewModel()
    val friendsViewModel: FriendsViewModel = viewModel()
    val friendRequestsViewModel: FriendRequestsViewModel = viewModel()
    val adminUsernamesViewModel: AdminUsernamesViewModel = viewModel()
    val helpChatViewModel: HelpChatViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val forYouViewModel: ForYouViewModel = viewModel()
    val userListsViewModel: UserListsViewModel = viewModel()
    val userListDetailViewModel: UserListDetailViewModel = viewModel()
    val bookDetailViewModel: BookDetailViewModel = viewModel()
    val context = LocalContext.current
    val offlineRetryScope = rememberCoroutineScope()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val catalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val friendsState by friendsViewModel.uiState.collectAsStateWithLifecycle()
    val friendRequestsState by friendRequestsViewModel.uiState.collectAsStateWithLifecycle()
    val helpChatState by helpChatViewModel.uiState.collectAsStateWithLifecycle()
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val isGuestUser = authState.user?.isGuest == true
    var showGuestRestrictedNotice by remember { mutableStateOf(false) }
    var showOfflineAccessNotice by remember { mutableStateOf(false) }
    var pendingOfflineRoute by remember { mutableStateOf<String?>(null) }
    var initialLoggedInRouteResolved by remember { mutableStateOf(false) }
    var pendingActivityNotificationToOpen by remember { mutableStateOf<String?>(null) }
    var pendingDeviceLibraryBookUri by remember { mutableStateOf<String?>(null) }
    var activeDeviceLibraryBookUri by remember { mutableStateOf<String?>(null) }
    var deviceBookShortcutLaunchActive by remember { mutableStateOf(!deviceLibraryBookToOpen.isNullOrBlank()) }
    var deviceShortcutConnectivityBlocked by remember { mutableStateOf(false) }
    var deviceShortcutConnectivityCheckRequested by remember { mutableStateOf(false) }
    var deviceShortcutRemoteAccessConfirmed by remember { mutableStateOf(false) }
    var offlineAccessRetryInProgress by remember { mutableStateOf(false) }
    val isRegisteredOffline =
        authState.isLoggedIn && (homeState.isOfflineError || deviceShortcutConnectivityBlocked)
    val shouldBlockRemoteNavigation =
        isRegisteredOffline ||
            (authState.isLoggedIn &&
                !isGuestUser &&
                deviceBookShortcutLaunchActive &&
                !deviceShortcutRemoteAccessConfirmed)
    val guestRestrictedSections = remember(isGuestUser) {
        if (isGuestUser) {
            setOf(KaiSection.HOME, KaiSection.FRIENDS, KaiSection.GROUPS)
        } else {
            emptySet()
        }
    }

    val startDestination = when {
        authState.pendingEmailVerificationEmail != null -> Routes.EMAIL_VERIFICATION
        authState.isLoggedIn && authState.user == null -> Routes.AUTH_LOADING
        authState.isLoggedIn && deviceBookShortcutLaunchActive -> Routes.LIBRARY
        authState.isLoggedIn -> if (authState.user?.isGuest == true) Routes.DISCOVER else Routes.HOME
        else -> Routes.LOGIN
    }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: startDestination

    fun authenticatedStartRoute(isGuest: Boolean): String {
        return if (isGuest) Routes.DISCOVER else Routes.HOME
    }

    fun routeForSection(section: KaiSection): String {
        return when (section) {
            KaiSection.HOME -> Routes.HOME
            KaiSection.MY_BOOKS -> Routes.LISTS
            KaiSection.DISCOVER -> Routes.DISCOVER
            KaiSection.SEARCH -> Routes.SEARCH
            KaiSection.LIBRARY -> Routes.LIBRARY
            KaiSection.PROFILE -> Routes.PROFILE
            KaiSection.STATS -> Routes.READING_STATS
            KaiSection.FRIENDS -> Routes.FRIENDS
            KaiSection.GROUPS -> Routes.GROUPS
            KaiSection.FILE_CONVERTER -> Routes.FILE_CONVERTER
            KaiSection.CHALLENGES -> Routes.CHALLENGES
            KaiSection.FOR_YOU -> Routes.FOR_YOU
            KaiSection.HELP -> Routes.HELP
        }
    }

    fun showOfflineAccessFor(route: String) {
        pendingOfflineRoute = route
        showOfflineAccessNotice = true
    }

    fun shouldOpenOfflineAccessInstead(route: String): Boolean {
        if (!authState.isLoggedIn || route == Routes.LIBRARY) return false

        if (shouldBlockRemoteNavigation || !context.hasActiveInternetConnection()) {
            deviceShortcutConnectivityBlocked = true
            deviceShortcutRemoteAccessConfirmed = false
            showOfflineAccessFor(route)
            return true
        }

        return false
    }

    fun navigateRoute(route: String) {
        if (shouldOpenOfflineAccessInstead(route)) return

        if (route == Routes.LIBRARY) {
            showOfflineAccessNotice = false
        }
        navController.navigate(route)
    }

    fun navigateToDeviceLibraryShortcut() {
        showOfflineAccessNotice = false
        navController.navigate(Routes.LIBRARY) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun refreshRecoveredRoute(route: String) {
        // Al recuperar conexión, limpia errores remotos antiguos y muestra carga en el destino solicitado.
        when (route) {
            Routes.DISCOVER -> catalogViewModel.cargarLibros()
            Routes.HOME -> homeViewModel.loadFeed()
            Routes.FRIENDS -> friendsViewModel.loadFriends()
            Routes.NOTIFICATION_CENTER -> {
                friendRequestsViewModel.loadReceivedRequests()
                friendRequestsViewModel.loadActivityNotifications()
            }
            Routes.FOR_YOU -> forYouViewModel.loadRecommendations(
                personalizedSuggestionsEnabled = authState.user?.privacySettings?.personalizedSuggestions != false
            )
        }
    }

    fun onShortcutOnlineAccessChecked(hasOnlineAccess: Boolean) {
        offlineAccessRetryInProgress = false
        deviceShortcutRemoteAccessConfirmed = hasOnlineAccess
        deviceShortcutConnectivityBlocked = !hasOnlineAccess

        if (hasOnlineAccess && (showOfflineAccessNotice || pendingOfflineRoute != null)) {
            val targetRoute = pendingOfflineRoute ?: Routes.HOME
            showOfflineAccessNotice = false
            pendingOfflineRoute = null
            refreshRecoveredRoute(targetRoute)
            navController.navigate(targetRoute)
        }
    }

    LaunchedEffect(authState.pendingEmailVerificationEmail) {
        if (authState.pendingEmailVerificationEmail == null) return@LaunchedEffect

        initialLoggedInRouteResolved = false
        if (navController.currentBackStackEntry?.destination?.route != Routes.EMAIL_VERIFICATION) {
            navController.navigate(Routes.EMAIL_VERIFICATION) {
                popUpTo(Routes.LOGIN) { inclusive = false }
            }
        }
    }

    LaunchedEffect(
        authState.isLoggedIn,
        authState.user?.isGuest,
        authState.pendingEmailVerificationEmail,
        deviceLibraryBookToOpen,
        pendingDeviceLibraryBookUri,
        deviceBookShortcutLaunchActive
    ) {
        if (authState.pendingEmailVerificationEmail != null) {
            initialLoggedInRouteResolved = false
            return@LaunchedEffect
        }

        if (!authState.isLoggedIn) {
            initialLoggedInRouteResolved = false
            return@LaunchedEffect
        }

        val user = authState.user ?: return@LaunchedEffect
        if (initialLoggedInRouteResolved) return@LaunchedEffect
        if (deviceBookShortcutLaunchActive || !deviceLibraryBookToOpen.isNullOrBlank() || !pendingDeviceLibraryBookUri.isNullOrBlank()) {
            initialLoggedInRouteResolved = true
            return@LaunchedEffect
        }

        val targetRoute = authenticatedStartRoute(user.isGuest)
        initialLoggedInRouteResolved = true

        if (navController.currentBackStackEntry?.destination?.route != targetRoute) {
            navController.navigate(targetRoute) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(authState.isLoggedIn, authState.user?.uid, authState.user?.isGuest) {
        if (authState.isLoggedIn && authState.user?.isGuest != true) {
            friendsViewModel.loadFriends()
            friendRequestsViewModel.loadReceivedRequests()
            friendRequestsViewModel.loadActivityNotifications()
            friendRequestsViewModel.observeActivityNotificationChanges()
        }
    }

    LaunchedEffect(
        deviceBookShortcutLaunchActive,
        authState.isLoggedIn,
        authState.user?.uid,
        authState.user?.isGuest,
        deviceShortcutConnectivityCheckRequested
    ) {
        if (!deviceBookShortcutLaunchActive ||
            !authState.isLoggedIn ||
            deviceShortcutConnectivityCheckRequested
        ) {
            return@LaunchedEffect
        }

        deviceShortcutConnectivityCheckRequested = true
        if (context.hasActiveInternetConnection()) {
            if (authState.user?.isGuest == true) {
                deviceShortcutConnectivityBlocked = false
                deviceShortcutRemoteAccessConfirmed = true
            } else {
                deviceShortcutConnectivityBlocked = false
                homeViewModel.checkOnlineAccess(::onShortcutOnlineAccessChecked)
            }
        } else {
            deviceShortcutRemoteAccessConfirmed = false
            deviceShortcutConnectivityBlocked = true
        }
    }

    LaunchedEffect(activityNotificationToOpen, authState.isLoggedIn, authState.user?.isGuest) {
        val notificationId = activityNotificationToOpen?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (!authState.isLoggedIn || authState.user?.isGuest == true) return@LaunchedEffect

        pendingActivityNotificationToOpen = notificationId
        if (currentRoute != Routes.NOTIFICATION_CENTER) {
            navController.navigate(Routes.NOTIFICATION_CENTER)
        }
        onActivityNotificationOpenConsumed()
    }

    LaunchedEffect(deviceLibraryBookToOpen, authState.isLoggedIn) {
        val bookUri = deviceLibraryBookToOpen?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        deviceBookShortcutLaunchActive = true
        deviceShortcutConnectivityBlocked = false
        deviceShortcutConnectivityCheckRequested = false
        deviceShortcutRemoteAccessConfirmed = false
        if (!authState.isLoggedIn) return@LaunchedEffect

        initialLoggedInRouteResolved = true
        activeDeviceLibraryBookUri = bookUri
        pendingDeviceLibraryBookUri = bookUri
        navigateToDeviceLibraryShortcut()
        onDeviceLibraryBookOpenConsumed()
    }

    LaunchedEffect(currentRoute, helpChatState.isActive, catalogState.selectedBook?.titulo) {
        helpChatViewModel.updateScreenContext(
            buildHelpScreenContext(
                route = currentRoute,
                selectedBookTitle = catalogState.selectedBook?.titulo
            )
        )

        if (helpChatState.isActive && currentRoute != Routes.HELP) {
            helpChatViewModel.minimizeChat()
        }
    }

    LaunchedEffect(authState.isLoggedIn, authState.user?.uid, currentRoute) {
        if (!authState.isLoggedIn ||
            authState.user == null ||
            currentRoute == Routes.LIBRARY ||
            currentRoute == Routes.LOGIN ||
            currentRoute == Routes.REGISTER ||
            currentRoute == Routes.EMAIL_VERIFICATION ||
            currentRoute == Routes.AUTH_LOADING ||
            context.hasActiveInternetConnection()
        ) {
            return@LaunchedEffect
        }

        deviceShortcutConnectivityBlocked = true
        deviceShortcutRemoteAccessConfirmed = false
        showOfflineAccessFor(currentRoute)
    }

    LaunchedEffect(homeState.isOfflineError, authState.isLoggedIn, authState.user?.isGuest) {
        if (!authState.isLoggedIn || authState.user?.isGuest == true || homeState.isOfflineError) {
            return@LaunchedEffect
        }

        deviceShortcutConnectivityBlocked = false
        if (deviceBookShortcutLaunchActive && deviceShortcutConnectivityCheckRequested) {
            deviceShortcutRemoteAccessConfirmed = true
        }
        if (showOfflineAccessNotice || pendingOfflineRoute != null) {
            val targetRoute = pendingOfflineRoute ?: Routes.HOME
            showOfflineAccessNotice = false
            pendingOfflineRoute = null
            refreshRecoveredRoute(targetRoute)
            navController.navigate(targetRoute)
        }
    }

    fun navigateSection(section: KaiSection) {
        if (guestRestrictedSections.contains(section)) {
            showGuestRestrictedNotice = true
            return
        }

        if (shouldOpenOfflineAccessInstead(routeForSection(section))) return

        when (section) {
            KaiSection.HOME -> navController.navigate(Routes.HOME)
            KaiSection.MY_BOOKS -> navController.navigate(Routes.LISTS)
            KaiSection.DISCOVER -> navController.navigate(Routes.DISCOVER)
            KaiSection.SEARCH -> navController.navigate(Routes.SEARCH)
            KaiSection.PROFILE -> navController.navigate(Routes.PROFILE)
            KaiSection.STATS -> navController.navigate(Routes.READING_STATS)
            KaiSection.LIBRARY -> {
                showOfflineAccessNotice = false
                navController.navigate(Routes.LIBRARY)
            }
            KaiSection.FRIENDS -> navController.navigate(Routes.FRIENDS)
            KaiSection.GROUPS -> navController.navigate(Routes.GROUPS)
            KaiSection.FILE_CONVERTER -> navController.navigate(Routes.FILE_CONVERTER)
            KaiSection.CHALLENGES -> navController.navigate(Routes.CHALLENGES)
            KaiSection.FOR_YOU -> navController.navigate(Routes.FOR_YOU)
            KaiSection.HELP -> navController.navigate(Routes.HELP)
        }
    }

    fun openCatalogAndSearch() {
        val query = catalogState.searchQuery
        searchResultsViewModel.search(query)
        catalogViewModel.onSearchQueryChange("")
        navController.navigate(Routes.SEARCH_RESULTS)
    }

    fun searchFromSharedTopBar(query: String) {
        catalogViewModel.onSearchQueryChange(query)
    }

    fun scanFromSharedTopBar(isbn: String) {
        catalogViewModel.buscarPorIsbn(isbn)
        navController.navigate(Routes.SCAN_BOOKS)
    }

    fun logoutToLogin() {
        activeDeviceLibraryBookUri = null
        pendingDeviceLibraryBookUri = null
        deviceBookShortcutLaunchActive = false
        deviceShortcutConnectivityBlocked = false
        deviceShortcutConnectivityCheckRequested = false
        deviceShortcutRemoteAccessConfirmed = false
        offlineAccessRetryInProgress = false
        initialLoggedInRouteResolved = false
        authViewModel.logout()
        navController.navigate(Routes.LOGIN) {
            popUpTo(0) { inclusive = true }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalOpenScanner provides {
            if (navController.currentBackStackEntry?.destination?.route != Routes.SCAN_BOOKS) {
                navController.navigate(Routes.SCAN_BOOKS)
            }
        },
        LocalGuestUiRestrictions provides GuestUiRestrictions(
            disabledSections = guestRestrictedSections,
            onBlockedSectionClick = {
                showGuestRestrictedNotice = true
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val shouldShowOfflineAccessNotice =
                shouldBlockRemoteNavigation && (currentRoute != Routes.LIBRARY || showOfflineAccessNotice)

            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
        composable(Routes.AUTH_LOADING) {
            AuthLoadingScreen()
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { isGuest ->
                    val targetRoute = if (deviceBookShortcutLaunchActive) Routes.LIBRARY else authenticatedStartRoute(isGuest)
                    navController.navigate(targetRoute) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onGoToRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    val targetRoute = if (deviceBookShortcutLaunchActive) Routes.LIBRARY else Routes.HOME
                    navController.navigate(targetRoute) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    authViewModel.clearError()
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.EMAIL_VERIFICATION) {
            EmailVerificationScreen(
                email = authState.pendingEmailVerificationEmail.orEmpty(),
                isLoading = authState.isLoading,
                message = authState.errorMessage ?: authState.successMessage,
                isError = authState.errorMessage != null,
                onAlreadyVerified = authViewModel::confirmEmailVerification,
                onResendVerificationEmail = authViewModel::resendEmailVerification,
                onDismiss = {
                    authViewModel.dismissEmailVerification()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.EMAIL_VERIFICATION) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                viewModel = homeViewModel,
                subtitle = stringResource(R.string.home_placeholder_subtitle),
                searchQuery = catalogState.searchQuery,
                onSearchQueryChange = ::searchFromSharedTopBar,
                onSearch = ::openCatalogAndSearch,
                onScanResult = ::scanFromSharedTopBar,
                userName = authState.user?.usuario,
                profileImageUrl = authState.user?.photoUrl,
                onGoToProfile = { navigateRoute(Routes.PROFILE) },
                onGoToSettingsPrivacy = { navigateRoute(Routes.SETTINGS_PRIVACY) },
                onLogout = ::logoutToLogin,
                pendingRequestCount = friendRequestsState.pendingCount,
                onOpenNotifications = { navigateRoute(Routes.NOTIFICATION_CENTER) },
                hasAddedFriends = friendsState.takeIf { it.hasLoadedFriends && it.errorMessage == null }
                    ?.friends
                    ?.isNotEmpty(),
                onOpenFriendSuggestions = {
                    navController.navigate(Routes.FRIEND_SUGGESTIONS)
                },
                onOpenFriendProfile = { friendUid ->
                    navController.navigate(friendProfileRoute(friendUid))
                },
                onOpenBook = { libro ->
                    catalogViewModel.selectBook(libro)
                    navController.navigate(Routes.DETAIL)
                },
                onSectionSelected = { navigateSection(it) }
            )
        }

        composable(Routes.SEARCH) {
            val genres = catalogState.generos.filter { it != "Todos" }

            LibraryScreen(
                userName = authState.user?.usuario,
                profileImageUrl = authState.user?.photoUrl,
                genres = genres,
                onGenreClick = { genre ->
                    catalogViewModel.applyInitialGenre(genre)
                    navController.navigate(Routes.DISCOVER)
                },
                searchQuery = catalogState.searchQuery,
                onSearchQueryChange = ::searchFromSharedTopBar,
                onSearch = ::openCatalogAndSearch,
                onScanResult = ::scanFromSharedTopBar,
                onGoToProfile = { navigateRoute(Routes.PROFILE) },
                onGoToSettingsPrivacy = { navigateRoute(Routes.SETTINGS_PRIVACY) },
                onLogout = ::logoutToLogin,
                pendingRequestCount = friendRequestsState.pendingCount,
                onOpenNotifications = { navigateRoute(Routes.NOTIFICATION_CENTER) },
                searchIntroAnimationEnabled = authState.user?.privacySettings?.searchIntroAnimationEnabled != false,
                onSectionSelected = { navigateSection(it) }
            )
        }

        composable(Routes.SEARCH_RESULTS) {
            SearchResultsScreen(
                viewModel = searchResultsViewModel,
                onBack = {
                    searchResultsViewModel.clearSearchQuery()
                    catalogViewModel.onSearchQueryChange("")
                    navController.popBackStack()
                },
                onBookClick = { libro ->
                    catalogViewModel.selectBook(libro)
                    navController.navigate(Routes.DETAIL)
                }
            )
        }

        composable(Routes.DISCOVER) {
            CatalogScreen(
                viewModel = catalogViewModel,
                userName = authState.user?.usuario,
                profileImageUrl = authState.user?.photoUrl,
                onGoToProfile = { navigateRoute(Routes.PROFILE) },
                onGoToSettingsPrivacy = { navigateRoute(Routes.SETTINGS_PRIVACY) },
                onLogout = ::logoutToLogin,
                onBookClick = { libro ->
                    catalogViewModel.selectBook(libro)
                    navController.navigate(Routes.DETAIL)
                },
                pendingRequestCount = friendRequestsState.pendingCount,
                onOpenNotifications = { navigateRoute(Routes.NOTIFICATION_CENTER) },
                onSectionSelected = { navigateSection(it) },
                onOpenScanner = { navController.navigate(Routes.SCAN_BOOKS) }
            )
        }

        composable(Routes.SCAN_BOOKS) {
            IsbnScannerScreen(
                viewModel = catalogViewModel,
                onBack = { navController.popBackStack() },
                onBookClick = { libro ->
                    catalogViewModel.selectBook(libro)
                    navController.navigate(Routes.DETAIL)
                },
                onSectionSelected = { navigateSection(it) }
            )
        }

        composable(Routes.DETAIL) {
            val libro = catalogState.selectedBook
            if (libro != null) {
                BookDetailScreen(
                    libro = libro,
                    viewModel = bookDetailViewModel,
                    userName = authState.user?.usuario,
                    profileImageUrl = authState.user?.photoUrl,
                    onBack = { navController.popBackStack() },
                    onMarkAsRead = { selected ->
                        readingListViewModel.marcarComoLeido(selected) {
                            bookDetailViewModel.refrescarLectura(selected.isbn)
                            navController.navigate(Routes.LISTS)
                        }
                    },
                    onGoToReadingList = {
                        navController.navigate(Routes.LISTS)
                    }
                )
            }
        }

        composable(Routes.READING_LIST) {
            ReadingListScreen(
                viewModel = readingListViewModel,
                userName = authState.user?.usuario,
                profileImageUrl = authState.user?.photoUrl,
                onBack = { navController.popBackStack() },
                searchQuery = catalogState.searchQuery,
                onSearchQueryChange = ::searchFromSharedTopBar,
                onSearch = ::openCatalogAndSearch,
                onScanResult = ::scanFromSharedTopBar,
                onGoToProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onGoToSettingsPrivacy = {
                    navController.navigate(Routes.SETTINGS_PRIVACY)
                },
                onLogout = ::logoutToLogin,
                pendingRequestCount = friendRequestsState.pendingCount,
                onOpenNotifications = {
                    navController.navigate(Routes.NOTIFICATION_CENTER)
                },
                onSectionSelected = { navigateSection(it) }
            )
        }

        composable(Routes.LISTS) {
            UserListsScreen(
                viewModel = userListsViewModel,
                userName = authState.user?.usuario,
                profileImageUrl = authState.user?.photoUrl,
                onOpenList = { listId ->
                    navController.navigate(listDetailRoute(listId))
                },
                searchQuery = catalogState.searchQuery,
                onSearchQueryChange = ::searchFromSharedTopBar,
                onSearch = ::openCatalogAndSearch,
                onScanResult = ::scanFromSharedTopBar,
                onGoToProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onGoToSettingsPrivacy = {
                    navController.navigate(Routes.SETTINGS_PRIVACY)
                },
                onLogout = ::logoutToLogin,
                pendingRequestCount = friendRequestsState.pendingCount,
                onOpenNotifications = {
                    navController.navigate(Routes.NOTIFICATION_CENTER)
                },
                onSectionSelected = { navigateSection(it) }
            )
        }

        composable(
            route = Routes.LIST_DETAIL,
            arguments = listOf(navArgument("listId") { defaultValue = "" })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString("listId").orEmpty()
            UserListDetailScreen(
                listId = listId,
                viewModel = userListDetailViewModel,
                onBack = { navController.popBackStack() },
                onBookClick = { libro ->
                    catalogViewModel.selectBook(libro)
                    navController.navigate(Routes.DETAIL)
                },
                onReadOwnedBook = { item ->
                    pendingDeviceLibraryBookUri = item.ownedUri
                    navigateRoute(Routes.LIBRARY)
                }
            )
        }

        composable(Routes.READING_STATS) {
            ReadingStatsScreen(
                viewModel = readingListViewModel,
                userName = authState.user?.usuario,
                profileImageUrl = authState.user?.photoUrl,
                searchQuery = catalogState.searchQuery,
                onSearchQueryChange = ::searchFromSharedTopBar,
                onSearch = ::openCatalogAndSearch,
                onScanResult = ::scanFromSharedTopBar,
                onGoToProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onGoToSettingsPrivacy = {
                    navController.navigate(Routes.SETTINGS_PRIVACY)
                },
                onLogout = ::logoutToLogin,
                pendingRequestCount = friendRequestsState.pendingCount,
                onOpenNotifications = {
                    navController.navigate(Routes.NOTIFICATION_CENTER)
                },
                onSectionSelected = { navigateSection(it) }
            )
        }

        composable(Routes.LIBRARY) {
            DeviceLibraryScreen(
                //searchQuery = catalogState.searchQuery,
                // onSearchQueryChange = ::searchFromSharedTopBar,
                // onSearch = ::openCatalogAndSearch,
                // onScanResult = ::scanFromSharedTopBar,
                userName = authState.user?.usuario,
                profileImageUrl = authState.user?.photoUrl,
                onGoToProfile = { navigateRoute(Routes.PROFILE) },
                onGoToSettingsPrivacy = { navigateRoute(Routes.SETTINGS_PRIVACY) },
                onLogout = ::logoutToLogin,
                // pendingRequestCount = friendRequestsState.pendingCount,
                // onOpenNotifications = { navigateRoute(Routes.NOTIFICATION_CENTER) },
                onSectionSelected = { navigateSection(it) },
                openBookUri = activeDeviceLibraryBookUri ?: pendingDeviceLibraryBookUri,
                onOpenBookUriConsumed = { pendingDeviceLibraryBookUri = null },
                onReaderClosed = {
                    activeDeviceLibraryBookUri = null
                    pendingDeviceLibraryBookUri = null
                }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                viewModel = authViewModel,
                myProfileViewModel = friendProfileViewModel,
                userName = authState.user?.usuario,
                profileImageUrl = authState.user?.photoUrl,
                searchQuery = catalogState.searchQuery,
                onSearchQueryChange = ::searchFromSharedTopBar,
                onSearch = ::openCatalogAndSearch,
                onScanResult = ::scanFromSharedTopBar,
                onGoToSettingsPrivacy = {
                    navController.navigate(Routes.SETTINGS_PRIVACY)
                },
                onGoToRegister = {
                    navController.navigate(Routes.REGISTER)
                },
                onLogout = ::logoutToLogin,
                pendingRequestCount = friendRequestsState.pendingCount,
                onOpenNotifications = {
                    navController.navigate(Routes.NOTIFICATION_CENTER)
                },
                onOpenFriendProfile = { friendUid ->
                    navController.navigate(friendProfileRoute(friendUid))
                },
                onOpenFriendLists = { uid, name ->
                    navController.navigate(friendListsRoute(uid, name))
                },
                onOpenBook = { libro ->
                    catalogViewModel.selectBook(libro)
                    navController.navigate(Routes.DETAIL)
                },
                onSectionSelected = { navigateSection(it) }
            )
        }

        composable(Routes.SETTINGS_PRIVACY) {
            SettingsPrivacyScreen(
                userName = authState.user?.usuario,
                profileImageUrl = authState.user?.photoUrl,
                isAdmin = authState.user?.isAdmin == true,
                searchQuery = catalogState.searchQuery,
                onSearchQueryChange = ::searchFromSharedTopBar,
                onSearch = ::openCatalogAndSearch,
                onScanResult = ::scanFromSharedTopBar,
                onGoToProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onOpenAdminUsernames = {
                    if (authState.user?.isAdmin == true) {
                        navController.navigate(Routes.ADMIN_USERNAMES)
                    }
                },
                onLogout = ::logoutToLogin,
                pendingRequestCount = friendRequestsState.pendingCount,
                onOpenNotifications = {
                    navController.navigate(Routes.NOTIFICATION_CENTER)
                },
                onSectionSelected = { navigateSection(it) }
            )
        }

        composable(Routes.ADMIN_USERNAMES) {
            if (authState.user?.isAdmin == true) {
                AdminUsernamesScreen(
                    viewModel = adminUsernamesViewModel,
                    onBack = { navController.popBackStack() }
                )
            } else {
                PlaceholderScreen(
                    title = stringResource(R.string.restricted_access_title),
                    subtitle = stringResource(R.string.admin_restricted_access_subtitle),
                    currentSection = KaiSection.PROFILE,
                    searchQuery = catalogState.searchQuery,
                    onSearchQueryChange = ::searchFromSharedTopBar,
                    onSearch = ::openCatalogAndSearch,
                    onScanResult = ::scanFromSharedTopBar,
                    userName = authState.user?.usuario,
                    profileImageUrl = authState.user?.photoUrl,
                    onGoToProfile = {
                        navController.navigate(Routes.PROFILE)
                    },
                    onGoToSettingsPrivacy = {
                        navController.navigate(Routes.SETTINGS_PRIVACY)
                    },
                    onLogout = ::logoutToLogin,
                    pendingRequestCount = friendRequestsState.pendingCount,
                    onOpenNotifications = {
                        navController.navigate(Routes.NOTIFICATION_CENTER)
                    },
                    onSectionSelected = { navigateSection(it) }
                )
            }
        }

        composable(Routes.FRIENDS) {
            FriendsScreen(
                subtitle = stringResource(R.string.friends_placeholder_subtitle),
                searchQuery = catalogState.searchQuery,
                onSearchQueryChange = ::searchFromSharedTopBar,
                onSearch = ::openCatalogAndSearch,
                onScanResult = ::scanFromSharedTopBar,
                userName = authState.user?.usuario,
                profileImageUrl = authState.user?.photoUrl,
                onGoToProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onGoToSettingsPrivacy = {
                    navController.navigate(Routes.SETTINGS_PRIVACY)
                },
                onLogout = ::logoutToLogin,
                pendingRequestCount = friendRequestsState.pendingCount,
                onOpenNotifications = {
                    navController.navigate(Routes.NOTIFICATION_CENTER)
                },
                onOpenSuggestions = {
                    navController.navigate(Routes.FRIEND_SUGGESTIONS)
                },
                onOpenFriendProfile = { friendUid ->
                    navController.navigate(friendProfileRoute(friendUid))
                },
                viewModel = friendsViewModel,
                onSectionSelected = { navigateSection(it) }
            )
        }

        composable(
            route = Routes.FRIEND_PROFILE,
            arguments = listOf(navArgument("friendUid") { defaultValue = "" })
        ) { backStackEntry ->
            FriendProfileScreen(
                friendUid = backStackEntry.arguments?.getString("friendUid").orEmpty(),
                viewModel = friendProfileViewModel,
                onBack = { navController.popBackStack() },
                onOpenFriendProfile = { nextFriendUid ->
                    navController.navigate(friendProfileRoute(nextFriendUid))
                },
                searchQuery = catalogState.searchQuery,
                onSearchQueryChange = ::searchFromSharedTopBar,
                onSearch = ::openCatalogAndSearch,
                pendingRequestCount = friendRequestsState.pendingCount,
                onOpenNotifications = {
                    navController.navigate(Routes.NOTIFICATION_CENTER)
                },
                onFriendshipChanged = {
                    friendsViewModel.loadFriends()
                    friendRequestsViewModel.loadReceivedRequests()
                },
                onOpenFriendLists = { uid, name ->
                    navController.navigate(friendListsRoute(uid, name))
                },
                onOpenBook = { libro ->
                    catalogViewModel.selectBook(libro)
                    navController.navigate(Routes.DETAIL)
                },
                onSectionSelected = { navigateSection(it) }
            )
        }

        composable(
            route = Routes.FRIEND_LISTS,
            arguments = listOf(
                navArgument("friendUid") { defaultValue = "" },
                navArgument("friendName") { defaultValue = "" }
            )
        ) { backStackEntry ->
            FriendListsScreen(
                friendUid = backStackEntry.arguments?.getString("friendUid").orEmpty(),
                friendName = backStackEntry.arguments?.getString("friendName").orEmpty(),
                viewModel = friendListsViewModel,
                onBack = { navController.popBackStack() },
                onOpenList = { listId ->
                    navController.navigate(
                        friendListDetailRoute(
                            backStackEntry.arguments?.getString("friendUid").orEmpty(),
                            listId
                        )
                    )
                },
                onSectionSelected = { navigateSection(it) }
            )
        }

        composable(
            route = Routes.FRIEND_LIST_DETAIL,
            arguments = listOf(
                navArgument("friendUid") { defaultValue = "" },
                navArgument("listId") { defaultValue = "" }
            )
        ) { backStackEntry ->
            FriendListDetailScreen(
                friendUid = backStackEntry.arguments?.getString("friendUid").orEmpty(),
                listId = backStackEntry.arguments?.getString("listId").orEmpty(),
                viewModel = friendListDetailViewModel,
                onBack = { navController.popBackStack() },
                onBookClick = { libro ->
                    catalogViewModel.selectBook(libro)
                    navController.navigate(Routes.DETAIL)
                }
            )
        }

        composable(Routes.FRIEND_SUGGESTIONS) {
            FriendSuggestionsScreen(
                viewModel = friendSuggestionsViewModel,
                onBack = { navController.popBackStack() },
                onOpenFriendProfile = { friendUid ->
                    navController.navigate(friendProfileRoute(friendUid))
                },
                onFriendshipChanged = {
                    friendRequestsViewModel.loadReceivedRequests()
                    friendsViewModel.loadFriends()
                }
            )
        }

        composable(Routes.NOTIFICATION_CENTER) {
            NotificationCenterScreen(
                viewModel = friendRequestsViewModel,
                initialSelectedNotificationId = pendingActivityNotificationToOpen,
                onInitialSelectedNotificationHandled = {
                    pendingActivityNotificationToOpen = null
                },
                onBack = { navController.popBackStack() },
                onOpenFriendProfile = { friendUid ->
                    navController.navigate(friendProfileRoute(friendUid))
                },
                onRequestsChanged = {
                    friendRequestsViewModel.loadReceivedRequests()
                    friendsViewModel.loadFriends()
                },
                onSectionSelected = { navigateSection(it) }
            )
        }

        composable(Routes.GROUPS) {
            PlaceholderScreen(
                title = stringResource(R.string.groups),
                subtitle = stringResource(R.string.groups_placeholder_subtitle),
                currentSection = KaiSection.GROUPS,
                searchQuery = catalogState.searchQuery,
                onSearchQueryChange = ::searchFromSharedTopBar,
                onSearch = ::openCatalogAndSearch,
                onScanResult = ::scanFromSharedTopBar,
                userName = authState.user?.usuario,
                profileImageUrl = authState.user?.photoUrl,
                onGoToProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onGoToSettingsPrivacy = {
                    navController.navigate(Routes.SETTINGS_PRIVACY)
                },
                onLogout = ::logoutToLogin,
                pendingRequestCount = friendRequestsState.pendingCount,
                onOpenNotifications = {
                    navController.navigate(Routes.NOTIFICATION_CENTER)
                },
                onSectionSelected = { navigateSection(it) }
            )
        }

        composable(Routes.FILE_CONVERTER) {
            PdfConverterScreen(
                userName = authState.user?.usuario,
                profileImageUrl = authState.user?.photoUrl,
                onGoToProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onGoToSettingsPrivacy = {
                    navController.navigate(Routes.SETTINGS_PRIVACY)
                },
                onLogout = ::logoutToLogin,
                pendingRequestCount = friendRequestsState.pendingCount,
                onOpenNotifications = {
                    navController.navigate(Routes.NOTIFICATION_CENTER)
                },
                onSectionSelected = { navigateSection(it) }
            )
        }

        composable(Routes.CHALLENGES) {
            PlaceholderScreen(
                title = stringResource(R.string.reading_challenges),
                subtitle = stringResource(R.string.reading_challenges_placeholder_subtitle),
                currentSection = KaiSection.CHALLENGES,
                searchQuery = catalogState.searchQuery,
                onSearchQueryChange = ::searchFromSharedTopBar,
                onSearch = ::openCatalogAndSearch,
                onScanResult = ::scanFromSharedTopBar,
                userName = authState.user?.usuario,
                profileImageUrl = authState.user?.photoUrl,
                onGoToProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onGoToSettingsPrivacy = {
                    navController.navigate(Routes.SETTINGS_PRIVACY)
                },
                onLogout = ::logoutToLogin,
                pendingRequestCount = friendRequestsState.pendingCount,
                onOpenNotifications = {
                    navController.navigate(Routes.NOTIFICATION_CENTER)
                },
                onSectionSelected = { navigateSection(it) }
            )
        }

        composable(Routes.FOR_YOU) {
            ForYouScreen(
                viewModel = forYouViewModel,
                subtitle = stringResource(R.string.for_you_placeholder_subtitle),
                personalizedSuggestionsEnabled = authState.user?.privacySettings?.personalizedSuggestions == true,
                searchQuery = catalogState.searchQuery,
                onSearchQueryChange = ::searchFromSharedTopBar,
                onSearch = ::openCatalogAndSearch,
                onScanResult = ::scanFromSharedTopBar,
                userName = authState.user?.usuario,
                profileImageUrl = authState.user?.photoUrl,
                onGoToProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onGoToSettingsPrivacy = {
                    navController.navigate(Routes.SETTINGS_PRIVACY)
                },
                onLogout = ::logoutToLogin,
                pendingRequestCount = friendRequestsState.pendingCount,
                onOpenNotifications = {
                    navController.navigate(Routes.NOTIFICATION_CENTER)
                },
                onOpenBook = { libro ->
                    catalogViewModel.selectBook(libro)
                    navController.navigate(Routes.DETAIL)
                },
                onSectionSelected = { navigateSection(it) }
            )
        }

        composable(Routes.HELP) {
            HelpScreen(
                searchQuery = catalogState.searchQuery,
                onSearchQueryChange = ::searchFromSharedTopBar,
                onSearch = ::openCatalogAndSearch,
                onScanResult = ::scanFromSharedTopBar,
                userName = authState.user?.usuario,
                profileImageUrl = authState.user?.photoUrl,
                onGoToProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onGoToSettingsPrivacy = {
                    navController.navigate(Routes.SETTINGS_PRIVACY)
                },
                onLogout = ::logoutToLogin,
                pendingRequestCount = friendRequestsState.pendingCount,
                onOpenNotifications = {
                    navController.navigate(Routes.NOTIFICATION_CENTER)
                },
                onStartChat = {
                    helpChatViewModel.startChat()
                },
                onSectionSelected = { navigateSection(it) }
            )
        }
            }

            if (showGuestRestrictedNotice) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GuestRestrictedAccessNotice(
                        onDismiss = { showGuestRestrictedNotice = false },
                        onCreateAccountAndSync = {
                            showGuestRestrictedNotice = false
                            navController.navigate(Routes.REGISTER)
                        }
                    )
                }
            }

            if (shouldShowOfflineAccessNotice) {
                // Bloquea las secciones remotas y mantiene como única salida la biblioteca local.
                GothicBackground(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(8f),
                    imageAlpha = 0.58f,
                    mainScrimAlpha = 0.34f,
                    secondaryScrimAlpha = 0.04f
                ) {}

                OfflineAccessDialog(
                    isRetrying = offlineAccessRetryInProgress || homeState.isLoading || homeState.isRefreshing,
                    onRetry = {
                        offlineAccessRetryInProgress = true
                        if (pendingOfflineRoute == null) {
                            pendingOfflineRoute = Routes.HOME
                        }
                        if (context.hasActiveInternetConnection()) {
                            deviceShortcutConnectivityBlocked = false
                            if (authState.user?.isGuest == true) {
                                onShortcutOnlineAccessChecked(true)
                            } else {
                                homeViewModel.checkOnlineAccess(::onShortcutOnlineAccessChecked)
                            }
                        } else {
                            deviceShortcutRemoteAccessConfirmed = false
                            deviceShortcutConnectivityBlocked = true
                            offlineRetryScope.launch {
                                delay(800)
                                offlineAccessRetryInProgress = false
                            }
                        }
                    },
                    onOpenLibrary = {
                        showOfflineAccessNotice = false
                        navController.navigate(Routes.LIBRARY)
                    }
                )
            }

            HelpChatOverlay(
                state = helpChatState,
                onExpand = { helpChatViewModel.expandChat() },
                onMinimize = { helpChatViewModel.minimizeChat() },
                onClose = { helpChatViewModel.closeChat() },
                onInputChange = { helpChatViewModel.onInputChange(it) },
                onSend = { helpChatViewModel.sendMessage() }
            )
        }
    }
}

@Composable
private fun AuthLoadingScreen() {
    GothicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_kaishelves),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .size(168.dp)
                    .clip(RoundedCornerShape(42.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.titleMedium,
                color = TarnishedGold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(color = TarnishedGold)
        }
    }
}

private fun buildHelpScreenContext(
    route: String,
    selectedBookTitle: String?
): HelpScreenContext {
    val spanish = LanguageManager.getCurrentLanguage() == "es"
    fun text(es: String, en: String) = if (spanish) es else en
    fun actions(vararg pairs: Pair<String, String>) = pairs.map { (es, en) -> text(es, en) }

    return when (route) {
        Routes.HOME -> HelpScreenContext(
            route = route,
            screenName = text("Inicio", "Home"),
            description = text(
                "Actividad principal del usuario, accesos a red lectora y libros recientes.",
                "Main user activity, reading network shortcuts, and recent books."
            ),
            availableActions = actions(
                "Abrir notificaciones" to "Open notifications",
                "Buscar libros" to "Search books",
                "Ir al perfil" to "Go to profile",
                "Abrir el menú lateral" to "Open the side menu"
            )
        )
        Routes.SEARCH -> HelpScreenContext(
            route = route,
            screenName = text("Búsqueda", "Search"),
            description = text(
                "Entrada visual para explorar géneros y lanzar búsquedas en el catálogo.",
                "Visual entry point to explore genres and search the catalog."
            ),
            availableActions = actions(
                "Buscar por título o autor" to "Search by title or author",
                "Escanear ISBN" to "Scan ISBN",
                "Elegir género" to "Choose a genre",
                "Abrir un resultado" to "Open a result"
            )
        )
        Routes.SEARCH_RESULTS -> HelpScreenContext(
            route = route,
            screenName = text("Resultados de búsqueda", "Search results"),
            description = text(
                "Listado dedicado para los resultados de una búsqueda de libros.",
                "Dedicated list for book search results."
            ),
            availableActions = actions(
                "Volver" to "Go back",
                "Buscar otro texto" to "Search another text",
                "Borrar el texto" to "Clear the text",
                "Añadir un libro a listas" to "Add a book to lists"
            )
        )
        Routes.DISCOVER -> HelpScreenContext(
            route = route,
            screenName = text("Descubrir", "Discover"),
            description = text(
                "Catálogo de libros y resultados de búsqueda.",
                "Book catalog and search results."
            ),
            availableActions = actions(
                "Filtrar por género" to "Filter by genre",
                "Buscar en la barra superior" to "Search in the top bar",
                "Escanear ISBN" to "Scan ISBN",
                "Abrir detalle de libro" to "Open book details"
            )
        )
        Routes.DETAIL -> HelpScreenContext(
            route = route,
            screenName = text("Detalle de libro", "Book details"),
            description = text(
                "Ficha del libro seleccionado${selectedBookTitle?.let { ": $it" }.orEmpty()}.",
                "Details for the selected book${selectedBookTitle?.let { ": $it" }.orEmpty()}."
            ),
            availableActions = actions(
                "Volver" to "Go back",
                "Marcar como leído" to "Mark as read",
                "Ir a listas" to "Go to lists",
                "Revisar información del libro" to "Review book information"
            )
        )
        Routes.READING_LIST, Routes.LISTS -> HelpScreenContext(
            route = route,
            screenName = text("Mis libros", "My books"),
            description = text(
                "Gestión de lecturas, libros guardados y listas personales.",
                "Reading management, saved books, and personal lists."
            ),
            availableActions = actions(
                "Abrir una lista" to "Open a list",
                "Buscar libros" to "Search books",
                "Escanear ISBN" to "Scan ISBN",
                "Revisar libros guardados" to "Review saved books"
            )
        )
        Routes.LIST_DETAIL -> HelpScreenContext(
            route = route,
            screenName = text("Detalle de lista", "List details"),
            description = text(
                "Contenido de una lista personal de libros.",
                "Contents of a personal book list."
            ),
            availableActions = actions(
                "Volver" to "Go back",
                "Abrir un libro" to "Open a book",
                "Revisar los libros de la lista" to "Review the books in the list"
            )
        )
        Routes.READING_STATS -> HelpScreenContext(
            route = route,
            screenName = text("Estadísticas", "Statistics"),
            description = text(
                "Resumen de progreso lector y actividad de lectura.",
                "Summary of reading progress and reading activity."
            ),
            availableActions = actions(
                "Revisar progreso" to "Review progress",
                "Buscar nuevo libro" to "Search for a new book",
                "Cambiar de sección desde el menú" to "Change sections from the menu"
            )
        )
        Routes.LIBRARY -> HelpScreenContext(
            route = route,
            screenName = text("Biblioteca del dispositivo", "Device library"),
            description = text(
                "Gestión de biblioteca local y archivos disponibles en el teléfono.",
                "Local library management and files available on the phone."
            ),
            availableActions = actions(
                "Buscar libros" to "Search books",
                "Escanear ISBN" to "Scan ISBN",
                "Gestionar portadas" to "Manage covers",
                "Abrir menú lateral" to "Open the side menu"
            )
        )
        Routes.PROFILE -> HelpScreenContext(
            route = route,
            screenName = text("Perfil", "Profile"),
            description = text(
                "Datos del usuario, privacidad, actividad y conexiones.",
                "User details, privacy, activity, and connections."
            ),
            availableActions = actions(
                "Abrir privacidad" to "Open privacy",
                "Ver amigos" to "View friends",
                "Abrir libros" to "Open books",
                "Cerrar sesión desde el menú" to "Sign out from the menu"
            )
        )
        Routes.SETTINGS_PRIVACY -> HelpScreenContext(
            route = route,
            screenName = text("Privacidad y ajustes", "Privacy and settings"),
            description = text(
                "Configuración de privacidad y opciones de la cuenta.",
                "Privacy settings and account options."
            ),
            availableActions = actions(
                "Cambiar preferencias" to "Change preferences",
                "Volver al perfil" to "Return to profile",
                "Abrir panel de administrador si corresponde" to "Open the admin panel if available"
            )
        )
        Routes.FRIENDS -> HelpScreenContext(
            route = route,
            screenName = text("Amigos", "Friends"),
            description = text(
                "Red social lectora, amistades, sugerencias y actividad de otros usuarios.",
                "Reading social network, friendships, suggestions, and other users' activity."
            ),
            availableActions = actions(
                "Abrir sugerencias" to "Open suggestions",
                "Abrir perfil de amigo" to "Open a friend's profile",
                "Revisar notificaciones" to "Review notifications",
                "Buscar libros" to "Search books"
            )
        )
        Routes.FRIEND_PROFILE -> HelpScreenContext(
            route = route,
            screenName = text("Perfil de amigo", "Friend profile"),
            description = text(
                "Perfil público de otro lector y su actividad visible.",
                "Public profile of another reader and their visible activity."
            ),
            availableActions = actions(
                "Volver" to "Go back",
                "Abrir listas del amigo" to "Open friend's lists",
                "Abrir libros visibles" to "Open visible books"
            )
        )
        Routes.FRIEND_LISTS, Routes.FRIEND_LIST_DETAIL -> HelpScreenContext(
            route = route,
            screenName = text("Listas de amigo", "Friend lists"),
            description = text(
                "Listas compartidas o visibles de otro lector.",
                "Shared or visible lists from another reader."
            ),
            availableActions = actions(
                "Volver" to "Go back",
                "Abrir lista" to "Open list",
                "Abrir libro" to "Open book"
            )
        )
        Routes.NOTIFICATION_CENTER -> HelpScreenContext(
            route = route,
            screenName = text("Notificaciones", "Notifications"),
            description = text(
                "Centro de solicitudes y avisos de la app.",
                "Center for app requests and notices."
            ),
            availableActions = actions(
                "Aceptar solicitud" to "Accept request",
                "Rechazar solicitud" to "Reject request",
                "Volver" to "Go back"
            )
        )
        Routes.GROUPS -> HelpScreenContext(
            route = route,
            screenName = text("Grupos", "Groups"),
            description = text(
                "Sección preparada para grupos de lectura.",
                "Section prepared for reading groups."
            ),
            availableActions = actions(
                "Cambiar a otra sección" to "Change to another section",
                "Buscar libros" to "Search books",
                "Abrir menú lateral" to "Open the side menu"
            )
        )
        Routes.FILE_CONVERTER -> HelpScreenContext(
            route = route,
            screenName = text("Conversor de documentos", "Document converter"),
            description = text(
                "Sección preparada para convertir archivos a otros formatos.",
                "Section prepared to convert files into other formats."
            ),
            availableActions = actions(
                "Abrir menú lateral" to "Open the side menu",
                "Volver a Biblioteca" to "Return to Library",
                "Buscar libros" to "Search books"
            )
        )
        Routes.CHALLENGES -> HelpScreenContext(
            route = route,
            screenName = text("Desafíos de lectura", "Reading challenges"),
            description = text(
                "Sección preparada para retos, objetivos y progreso lector.",
                "Section prepared for challenges, goals, and reading progress."
            ),
            availableActions = actions(
                "Cambiar a Estadísticas" to "Switch to Statistics",
                "Buscar libros" to "Search books",
                "Abrir menú lateral" to "Open the side menu"
            )
        )
        Routes.FOR_YOU -> HelpScreenContext(
            route = route,
            screenName = text("Para ti", "For you"),
            description = text(
                "Recomendaciones y selección personalizada según ajustes de privacidad.",
                "Recommendations and personalized picks based on privacy settings."
            ),
            availableActions = actions(
                "Abrir un libro" to "Open a book",
                "Activar recomendaciones desde privacidad" to "Enable recommendations from privacy",
                "Buscar libros" to "Search books"
            )
        )
        Routes.HELP -> HelpScreenContext(
            route = route,
            screenName = text("Ayuda", "Help"),
            description = text(
                "Pantalla de asistencia, FAQ, flujos guiados y chat de ayuda.",
                "Help screen with FAQ, guided flows, and help chat."
            ),
            availableActions = actions(
                "Iniciar chat" to "Start chat",
                "Consultar preguntas frecuentes" to "Read FAQ",
                "Revisar errores frecuentes" to "Review common issues"
            )
        )
        else -> HelpScreenContext(
            route = route,
            screenName = "KaiShelves",
            description = text(
                "Pantalla de la aplicación KaiShelves.",
                "KaiShelves app screen."
            ),
            availableActions = actions(
                "Abrir el menú lateral" to "Open the side menu",
                "Buscar libros" to "Search books",
                "Ir a Ayuda" to "Go to Help"
            )
        )
    }
}

