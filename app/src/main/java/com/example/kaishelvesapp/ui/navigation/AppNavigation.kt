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
import com.example.kaishelvesapp.ui.screen.home.HomeScreen
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
    const val HOME = "home"
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
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    val startDestination = if (uiState.isLoggedIn) Routes.HOME else Routes.LOGIN

    fun navigateSection(section: KaiSection) {
        when (section) {
            KaiSection.HOME -> navController.navigate(Routes.HOME)
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
                    navController.navigate(Routes.HOME) {
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
                userName = uiState.user?.usuario,
                onGoToCatalog = { navController.navigate(Routes.CATALOG) },
                onGoToReadingList = { navController.navigate(Routes.READING_LIST) },
                onGoToProfile = { navController.navigate(Routes.PROFILE) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
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
            val libro = catalogViewModel.uiState.value.selectedBook
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