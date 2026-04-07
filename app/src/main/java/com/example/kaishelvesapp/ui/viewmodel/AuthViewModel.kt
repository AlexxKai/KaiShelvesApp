package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.model.Usuario
import com.example.kaishelvesapp.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val isLoading: Boolean = false,
    val user: Usuario? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val isEditingProfile: Boolean = false
)

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isLoggedIn = repository.isAuthenticated()
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        if (repository.isAuthenticated()) {
            loadCurrentUserProfile()
        }
    }

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun startEditingProfile() {
        _uiState.value = _uiState.value.copy(
            isEditingProfile = true,
            username = _uiState.value.user?.usuario ?: "",
            errorMessage = null,
            successMessage = null
        )
    }

    fun cancelEditingProfile() {
        _uiState.value = _uiState.value.copy(
            isEditingProfile = false,
            username = _uiState.value.user?.usuario ?: "",
            errorMessage = null
        )
    }

    fun loadCurrentUserProfile() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = repository.getCurrentUserProfile()

            result
                .onSuccess { usuario ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = usuario,
                        username = usuario.usuario,
                        isLoggedIn = true,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudo cargar el perfil"
                    )
                }
        }
    }

    fun login() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password.trim()

        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Completa email y contraseña"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val result = repository.login(email, password)

            result
                .onSuccess { usuario ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = usuario,
                        username = usuario.usuario,
                        isLoggedIn = true,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error al iniciar sesión"
                    )
                }
        }
    }

    fun register() {
        val username = _uiState.value.username.trim()
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password.trim()

        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Completa usuario, email y contraseña"
            )
            return
        }

        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "La contraseña debe tener al menos 6 caracteres"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val result = repository.register(username, email, password)

            result
                .onSuccess { usuario ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = usuario,
                        username = usuario.usuario,
                        isLoggedIn = true,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error al registrar usuario"
                    )
                }
        }
    }

    fun saveProfileChanges() {
        val username = _uiState.value.username.trim()

        if (username.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "El nombre de usuario no puede estar vacío"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val result = repository.updateUsername(username)

            result
                .onSuccess { updatedUser ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = updatedUser,
                        username = updatedUser.usuario,
                        isEditingProfile = false,
                        successMessage = "Perfil actualizado correctamente"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudo actualizar el perfil"
                    )
                }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState(isLoggedIn = false)
    }
}