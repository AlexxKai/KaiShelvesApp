package com.example.kaishelvesapp.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.components.GuestRestrictedAccessNotice
import com.example.kaishelvesapp.ui.components.GuestUiRestrictions
import com.example.kaishelvesapp.ui.components.LocalGuestUiRestrictions
import com.example.kaishelvesapp.ui.screen.catalog.CatalogScreen
import com.example.kaishelvesapp.ui.screen.detail.BookDetailScreen
import com.example.kaishelvesapp.ui.screen.friends.FriendSuggestionsScreen
import com.example.kaishelvesapp.ui.screen.friends.FriendListDetailScreen
import com.example.kaishelvesapp.ui.screen.friends.FriendProfileScreen
import com.example.kaishelvesapp.ui.screen.friends.FriendListsScreen
import com.example.kaishelvesapp.ui.screen.friends.FriendsScreen
import com.example.kaishelvesapp.ui.screen.friends.NotificationCenterScreen
import com.example.kaishelvesapp.ui.screen.home.HomeScreen
import com.example.kaishelvesapp.ui.screen.library.DeviceLibraryScreen
import com.example.kaishelvesapp.ui.screen.library.LibraryScreen
import com.example.kaishelvesapp.ui.screen.lists.UserListDetailScreen
import com.example.kaishelvesapp.ui.screen.lists.UserListsScreen
import com.example.kaishelvesapp.ui.screen.login.LoginScreen
import com.example.kaishelvesapp.ui.screen.placeholder.PlaceholderScreen
import com.example.kaishelvesapp.ui.screen.profile.ProfileScreen
import com.example.kaishelvesapp.ui.screen.readinglist.ReadingListScreen
import com.example.kaishelvesapp.ui.screen.register.RegisterScreen
import com.example.kaishelvesapp.ui.screen.settings.SettingsPrivacyScreen
import com.example.kaishelvesapp.ui.screen.settings.AdminUsernamesScreen
import com.example.kaishelvesapp.ui.screen.placeholder.PlaceholderScreen
import com.example.kaishelvesapp.ui.viewmodel.AdminUsernamesViewModel
import com.example.kaishelvesapp.ui.screen.stats.ReadingStatsScreen
import com.example.kaishelvesapp.ui.viewmodel.AuthViewModel
import com.example.kaishelvesapp.ui.viewmodel.BookDetailViewModel
import com.example.kaishelvesapp.ui.viewmodel.CatalogViewModel
import com.example.kaishelvesapp.ui.viewmodel.FriendSuggestionsViewModel
import com.example.kaishelvesapp.ui.viewmodel.FriendListDetailViewModel
import com.example.kaishelvesapp.ui.viewmodel.FriendProfileViewModel
import com.example.kaishelvesapp.ui.viewmodel.FriendListsViewModel
import com.example.kaishelvesapp.ui.viewmodel.FriendsViewModel
import com.example.kaishelvesapp.ui.viewmodel.FriendRequestsViewModel
import com.example.kaishelvesapp.ui.viewmodel.HomeViewModel
import com.example.kaishelvesapp.ui.viewmodel.ReadingListViewModel
import com.example.kaishelvesapp.ui.viewmodel.UserListDetailViewModel
import com.example.kaishelvesapp.ui.viewmodel.UserListsViewModel

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val SEARCH = "search"
    const val DISCOVER = "discover"
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

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = viewModel()
    val catalogViewModel: CatalogViewModel = viewModel()
    val readingListViewModel: ReadingListViewModel = viewModel()
    val friendSuggestionsViewModel: FriendSuggestionsViewModel = viewModel()
    val friendProfileViewModel: FriendProfileViewModel = viewModel()
    val friendListsViewModel: FriendListsViewModel = viewModel()
    val friendListDetailViewModel: FriendListDetailViewModel = viewModel()
    val friendsViewModel: FriendsViewModel = viewModel()
    val friendRequestsViewModel: FriendRequestsViewModel = viewModel()
    val adminUsernamesViewModel: AdminUsernamesViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val userListsViewModel: UserListsViewModel = viewModel()
    val userListDetailViewModel: UserListDetailViewModel = viewModel()
    val bookDetailViewModel: BookDetailViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val catalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val friendRequestsState by friendRequestsViewModel.uiState.collectAsStateWithLifecycle()
    val isGuestUser = authState.user?.isGuest == true
    val guestRestrictedSections = remember(isGuestUser) {
        if (isGuestUser) {
            setOf(KaiSection.HOME, KaiSection.FRIENDS, KaiSection.GROUPS)
        } else {
            emptySet()
        }
    }
    var showGuestRestrictedNotice by remember { mutableStateOf(false) }
    var initialLoggedInRouteResolved by remember { mutableStateOf(false) }

    val startDestination = if (authState.isLoggedIn) Routes.DISCOVER else Routes.LOGIN

    fun authenticatedStartRoute(isGuest: Boolean): String {
        return if (isGuest) Routes.DISCOVER else Routes.HOME
    }

    LaunchedEffect(authState.isLoggedIn, authState.user?.isGuest) {
        if (!authState.isLoggedIn) {
            initialLoggedInRouteResolved = false
            return@LaunchedEffect
        }

        val user = authState.user ?: return@LaunchedEffect
        if (initialLoggedInRouteResolved) return@LaunchedEffect

        val targetRoute = authenticatedStartRoute(user.isGuest)
        initialLoggedInRouteResolved = true

        if (navController.currentBackStackEntry?.destination?.route != targetRoute) {
            navController.navigate(targetRoute) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    fun navigateSection(section: KaiSection) {
        if (guestRestrictedSections.contains(section)) {
            showGuestRestrictedNotice = true
            return
        }

        when (section) {
            KaiSection.HOME -> navController.navigate(Routes.HOME)
            KaiSection.MY_BOOKS -> navController.navigate(Routes.LISTS)
            KaiSection.DISCOVER -> {
                catalogViewModel.refrescarNovedades()
                navController.navigate(Routes.DISCOVER)
            }
            KaiSection.SEARCH -> navController.navigate(Routes.SEARCH)
            KaiSection.PROFILE -> navController.navigate(Routes.PROFILE)
            KaiSection.STATS -> navController.navigate(Routes.READING_STATS)
            KaiSection.LIBRARY -> navController.navigate(Routes.LIBRARY)
            KaiSection.FRIENDS -> navController.navigate(Routes.FRIENDS)
            KaiSection.GROUPS -> navController.navigate(Routes.GROUPS)
            KaiSection.CHALLENGES -> navController.navigate(Routes.CHALLENGES)
            KaiSection.FOR_YOU -> navController.navigate(Routes.FOR_YOU)
            KaiSection.HELP -> navController.navigate(Routes.HELP)
        }
    }

    fun openCatalogAndSearch() {
        catalogViewModel.resetGenreFilterForSearch()
        catalogViewModel.ejecutarBusqueda()
        navController.navigate(Routes.DISCOVER)
    }

    fun searchFromSharedTopBar(query: String) {
        catalogViewModel.onSearchQueryChange(query)
    }

    fun scanFromSharedTopBar(isbn: String) {
        catalogViewModel.buscarPorIsbn(isbn)
        navController.navigate(Routes.DISCOVER)
    }

    fun logoutToLogin() {
        authViewModel.logout()
        navController.navigate(Routes.LOGIN) {
            popUpTo(0) { inclusive = true }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalGuestUiRestrictions provides GuestUiRestrictions(
            disabledSections = guestRestrictedSections,
            onBlockedSectionClick = {
                showGuestRestrictedNotice = true
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { isGuest ->
                    navController.navigate(authenticatedStartRoute(isGuest)) {
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
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    authViewModel.clearError()
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                viewModel = homeViewModel,
                subtitle = "Aquí mostraremos la actividad de tus amigos muy pronto.",
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

        composable(Routes.DISCOVER) {
            CatalogScreen(
                viewModel = catalogViewModel,
                userName = authState.user?.usuario,
                profileImageUrl = authState.user?.photoUrl,
                onGoToProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onGoToSettingsPrivacy = {
                    navController.navigate(Routes.SETTINGS_PRIVACY)
                },
                onLogout = ::logoutToLogin,
                onBookClick = { libro ->
                    catalogViewModel.selectBook(libro)
                    navController.navigate(Routes.DETAIL)
                },
                pendingRequestCount = friendRequestsState.pendingCount,
                onOpenNotifications = {
                    navController.navigate(Routes.NOTIFICATION_CENTER)
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

        composable(Routes.PROFILE) {
            ProfileScreen(
                viewModel = authViewModel,
                userName = authState.user?.usuario,
                profileImageUrl = authState.user?.photoUrl,
                onBack = { navController.popBackStack() },
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
                    title = "Acceso restringido",
                    subtitle = "Necesitas permisos de administrador para entrar en este panel.",
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
                subtitle = "Aquí mostraremos tu red, su actividad y nuevas conexiones.",
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
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.NOTIFICATION_CENTER) {
            NotificationCenterScreen(
                viewModel = friendRequestsViewModel,
                onBack = { navController.popBackStack() },
                onRequestsChanged = {
                    friendRequestsViewModel.loadReceivedRequests()
                    friendsViewModel.loadFriends()
                },
                onSectionSelected = { navigateSection(it) }
            )
        }

        composable(Routes.GROUPS) {
            PlaceholderScreen(
                title = "Grupos",
                subtitle = "Aquí reuniremos tus grupos de lectura y sus conversaciones.",
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

        composable(Routes.CHALLENGES) {
            PlaceholderScreen(
                title = "Desafíos de lectura",
                subtitle = "Aquí aparecerán tus retos, objetivos y progreso lector.",
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
            PlaceholderScreen(
                title = "Seleccionados para ti",
                subtitle = "Aquí prepararemos recomendaciones y selecciones personalizadas.",
                currentSection = KaiSection.FOR_YOU,
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

        composable(Routes.HELP) {
            PlaceholderScreen(
                title = "Ayuda",
                subtitle = "Aquí reuniremos asistencia, preguntas frecuentes y soporte.",
                currentSection = KaiSection.HELP,
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
        }
    }
}
