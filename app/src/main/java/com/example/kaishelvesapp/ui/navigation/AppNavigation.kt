package com.example.kaishelvesapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.screen.catalog.CatalogScreen
import com.example.kaishelvesapp.ui.screen.detail.BookDetailScreen
import com.example.kaishelvesapp.ui.screen.library.LibraryScreen
import com.example.kaishelvesapp.ui.screen.login.LoginScreen
import com.example.kaishelvesapp.ui.screen.profile.ProfileScreen
import com.example.kaishelvesapp.ui.screen.readinglist.ReadingListScreen
import com.example.kaishelvesapp.ui.screen.register.RegisterScreen
import com.example.kaishelvesapp.ui.viewmodel.AuthViewModel
import com.example.kaishelvesapp.ui.viewmodel.BookDetailViewModel
import com.example.kaishelvesapp.ui.viewmodel.CatalogViewModel
import com.example.kaishelvesapp.ui.viewmodel.ReadingListViewModel

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val LIBRARY = "library"
    const val CATALOG = "catalog"
    const val DETAIL = "detail"
    const val READING_LIST = "reading_list"
    const val PROFILE = "profile"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = viewModel()
    val catalogViewModel: CatalogViewModel = viewModel()
    val readingListViewModel: ReadingListViewModel = viewModel()
    val bookDetailViewModel: BookDetailViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val catalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()

    val startDestination = if (authState.isLoggedIn) Routes.LIBRARY else Routes.LOGIN

    fun navigateSection(section: KaiSection) {
        when (section) {
            KaiSection.HOME -> navController.navigate(Routes.LIBRARY)
            KaiSection.CATALOG -> navController.navigate(Routes.CATALOG)
            KaiSection.READING -> navController.navigate(Routes.READING_LIST)
            KaiSection.PROFILE -> navController.navigate(Routes.PROFILE)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.LIBRARY) {
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
                    navController.navigate(Routes.LIBRARY) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    authViewModel.clearError()
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.LIBRARY) {
            val genres = catalogState.generos.filter { it != "Todos" }

            LibraryScreen(
                userName = authState.user?.usuario,
                genres = genres,
                onGenreClick = { genre ->
                    catalogViewModel.applyInitialGenre(genre)
                    navController.navigate(Routes.CATALOG)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSectionSelected = { navigateSection(it) }
            )
        }

        composable(Routes.CATALOG) {
            CatalogScreen(
                viewModel = catalogViewModel,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
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
                    onBack = { navController.popBackStack() },
                    onMarkAsRead = { selected ->
                        readingListViewModel.marcarComoLeido(selected) {
                            bookDetailViewModel.refrescarLectura(selected.isbn)
                            navController.navigate(Routes.READING_LIST)
                        }
                    },
                    onGoToReadingList = {
                        navController.navigate(Routes.READING_LIST)
                    }
                )
            }
        }

        composable(Routes.READING_LIST) {
            ReadingListScreen(
                viewModel = readingListViewModel,
                onBack = { navController.popBackStack() },
                onSectionSelected = { navigateSection(it) }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSectionSelected = { navigateSection(it) }
            )
        }
    }
}