package com.example.kaishelvesapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.ui.screen.catalog.CatalogScreen
import com.example.kaishelvesapp.ui.screen.detail.BookDetailScreen
import com.example.kaishelvesapp.ui.screen.home.HomeScreen
import com.example.kaishelvesapp.ui.screen.login.LoginScreen
import com.example.kaishelvesapp.ui.screen.register.RegisterScreen
import com.example.kaishelvesapp.ui.viewmodel.AuthViewModel
import com.example.kaishelvesapp.ui.viewmodel.CatalogViewModel

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val CATALOG = "catalog"
    const val DETAIL = "detail"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = viewModel()
    val catalogViewModel: CatalogViewModel = viewModel()
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    val startDestination = if (uiState.isLoggedIn) Routes.HOME else Routes.LOGIN

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
                onGoToCatalog = {
                    navController.navigate(Routes.CATALOG)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
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
                }
            )
        }

        composable(Routes.DETAIL) {
            val libro = catalogViewModel.uiState.value.selectedBook
            if (libro != null) {
                BookDetailScreen(
                    libro = libro,
                    onBack = {
                        navController.popBackStack()
                    },
                    onMarkAsRead = {
                    }
                )
            }
        }
    }
}