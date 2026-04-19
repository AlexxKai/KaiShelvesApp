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
    val loginIdentifier: String = "",
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val profilePhotoUri: String = "",
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
        viewModelScope.launch {
            runCatching {
                repository.migrateLegacyUsernames()
            }
        }

        if (repository.isAuthenticated()) {
            loadCurrentUserProfile()
        }
    }

    fun onLoginIdentifierChange(value: String) {
        _uiState.value = _uiState.value.copy(loginIdentifier = value)
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

    fun onProfilePhotoSelected(uri: String) {
        _uiState.value = _uiState.value.copy(profilePhotoUri = uri)
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

    fun showError(message: String) {
        _uiState.value = _uiState.value.copy(
            errorMessage = message,
            successMessage = null,
            isLoading = false
        )
    }

    fun startEditingProfile() {
        _uiState.value = _uiState.value.copy(
            isEditingProfile = true,
            username = _uiState.value.user?.usuario ?: "",
            profilePhotoUri = _uiState.value.user?.photoUrl ?: "",
            errorMessage = null,
            successMessage = null
        )
    }

    fun cancelEditingProfile() {
        _uiState.value = _uiState.value.copy(
            isEditingProfile = false,
            username = _uiState.value.user?.usuario ?: "",
            profilePhotoUri = _uiState.value.user?.photoUrl ?: "",
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
                        profilePhotoUri = usuario.photoUrl,
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
        val identifier = _uiState.value.loginIdentifier.trim()
        val password = _uiState.value.password.trim()

        if (identifier.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Completa correo o usuario y contrasena"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val result = repository.login(identifier, password)

            result
                .onSuccess { usuario ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = usuario,
                        username = usuario.usuario,
                        profilePhotoUri = usuario.photoUrl,
                        isLoggedIn = true,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error al iniciar sesion"
                    )
                }
        }
    }

    fun loginWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "No se pudo validar la cuenta de Google"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val result = repository.signInWithGoogle(idToken)

            result
                .onSuccess { usuario ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = usuario,
                        username = usuario.usuario,
                        profilePhotoUri = usuario.photoUrl,
                        isLoggedIn = true,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error al iniciar sesion con Google"
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
                errorMessage = "Completa usuario, email y contrasena"
            )
            return
        }

        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "La contrasena debe tener al menos 6 caracteres"
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
                        profilePhotoUri = usuario.photoUrl,
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
        val profilePhotoUri = _uiState.value.profilePhotoUri.trim()

        if (username.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "El nombre de usuario no puede estar vacio"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val result = repository.updateProfile(username, profilePhotoUri)

            result
                .onSuccess { updatedUser ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = updatedUser,
                        username = updatedUser.usuario,
                        profilePhotoUri = updatedUser.photoUrl,
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
