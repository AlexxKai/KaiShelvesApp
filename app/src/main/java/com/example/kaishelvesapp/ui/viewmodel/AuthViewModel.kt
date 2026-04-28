package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.repository.AuthOperationResult
import com.example.kaishelvesapp.data.model.Usuario
import com.example.kaishelvesapp.data.model.UserPrivacySettings
import com.example.kaishelvesapp.data.repository.AuthRepository
import com.example.kaishelvesapp.data.repository.GuestMergeDecision
import com.example.kaishelvesapp.data.repository.GuestMergeStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loginIdentifier: String = "",
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val guestUsername: String = "",
    val profilePhotoUri: String = "",
    val isLoading: Boolean = false,
    val user: Usuario? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val isEditingProfile: Boolean = false,
    val pendingGuestMergeDecision: GuestMergeDecision? = null
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

    fun onGuestUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(guestUsername = value)
    }

    fun onProfilePhotoSelected(uri: String) {
        _uiState.value = _uiState.value.copy(profilePhotoUri = uri)
    }

    fun saveProfilePhoto(uri: String) {
        val currentUser = _uiState.value.user ?: return

        _uiState.value = _uiState.value.copy(
            profilePhotoUri = uri,
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val result = repository.updateProfile(
                newUsername = currentUser.usuario,
                newEmail = currentUser.email,
                selectedPhotoUri = uri
            )

            result
                .onSuccess { updatedUser ->
                    repository.syncPendingAccountNotifications()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = updatedUser,
                        profilePhotoUri = updatedUser.photoUrl,
                        successMessage = "Foto de perfil actualizada correctamente"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profilePhotoUri = currentUser.photoUrl,
                        errorMessage = error.message ?: "No se pudo actualizar la foto de perfil"
                    )
                }
        }
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
            email = _uiState.value.user?.email ?: "",
            profilePhotoUri = _uiState.value.user?.photoUrl ?: "",
            errorMessage = null,
            successMessage = null
        )
    }

    fun cancelEditingProfile() {
        _uiState.value = _uiState.value.copy(
            isEditingProfile = false,
            username = _uiState.value.user?.usuario ?: "",
            email = _uiState.value.user?.email ?: "",
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
                    repository.syncPendingAccountNotifications()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = usuario,
                        email = usuario.email,
                        username = usuario.usuario,
                        guestUsername = if (usuario.isGuest) usuario.usuario else _uiState.value.guestUsername,
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
                .onSuccess { authResult ->
                    handleAuthOperationResult(authResult)
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
                .onSuccess { authResult ->
                    handleAuthOperationResult(authResult)
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
                .onSuccess { authResult ->
                    handleAuthOperationResult(authResult)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error al registrar usuario"
                    )
                }
        }
    }

    fun continueAsGuest() {
        val guestUsername = _uiState.value.guestUsername.trim()

        if (guestUsername.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Introduce un nombre de usuario para continuar sin cuenta"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            repository.continueAsGuest(guestUsername)
                .onSuccess { usuario ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = usuario,
                        email = usuario.email,
                        username = usuario.usuario,
                        guestUsername = usuario.usuario,
                        profilePhotoUri = usuario.photoUrl,
                        isLoggedIn = true,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudo iniciar el modo invitado"
                    )
                }
        }
    }

    fun dismissPendingGuestMergeDecision() {
        repository.cancelPendingGuestMerge()
        _uiState.value = _uiState.value.copy(
            pendingGuestMergeDecision = null,
            isLoading = false,
            user = null,
            isLoggedIn = repository.isAuthenticated()
        )
        if (repository.isAuthenticated()) {
            loadCurrentUserProfile()
        }
    }

    fun resolvePendingGuestMerge(strategy: GuestMergeStrategy) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            repository.resolvePendingGuestMerge(strategy)
                .onSuccess { usuario ->
                    repository.syncPendingAccountNotifications()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = usuario,
                        email = usuario.email,
                        username = usuario.usuario,
                        profilePhotoUri = usuario.photoUrl,
                        isLoggedIn = true,
                        pendingGuestMergeDecision = null,
                        successMessage = "Datos locales sincronizados con tu cuenta"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudo completar la fusion de datos"
                    )
                }
        }
    }

    fun saveProfileChanges() {
        val username = _uiState.value.username.trim()
        val email = _uiState.value.email.trim()
        val profilePhotoUri = _uiState.value.profilePhotoUri.trim()

        if (username.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "El nombre de usuario no puede estar vacio"
            )
            return
        }

        if (_uiState.value.user?.isGuest != true && email.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "El correo electronico no puede estar vacio"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val result = repository.updateProfile(username, email, profilePhotoUri)

            result
                .onSuccess { updatedUser ->
                    repository.syncPendingAccountNotifications()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = updatedUser,
                        email = updatedUser.email,
                        username = updatedUser.usuario,
                        profilePhotoUri = updatedUser.photoUrl,
                        isEditingProfile = true,
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

    fun updatePrivacySettings(privacySettings: UserPrivacySettings) {
        val previousUser = _uiState.value.user
        val optimisticUser = previousUser?.copy(privacySettings = privacySettings)

        if (optimisticUser != null) {
            _uiState.value = _uiState.value.copy(
                user = optimisticUser,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            repository.updatePrivacySettings(privacySettings)
                .onSuccess { updatedUser ->
                    _uiState.value = _uiState.value.copy(
                        user = updatedUser,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        user = previousUser,
                        errorMessage = error.message ?: "No se pudo actualizar la privacidad"
                    )
                }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState(isLoggedIn = false)
    }

    private suspend fun handleAuthOperationResult(result: AuthOperationResult) {
        when (result) {
            is AuthOperationResult.Success -> {
                repository.syncPendingAccountNotifications()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = result.user,
                    email = result.user.email,
                    username = result.user.usuario,
                    profilePhotoUri = result.user.photoUrl,
                    isLoggedIn = true,
                    pendingGuestMergeDecision = null,
                    errorMessage = null
                )
            }

            is AuthOperationResult.PendingGuestMerge -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = result.decision.user,
                    email = result.decision.user.email,
                    username = result.decision.user.usuario,
                    profilePhotoUri = result.decision.user.photoUrl,
                    isLoggedIn = false,
                    pendingGuestMergeDecision = result.decision,
                    errorMessage = null
                )
            }
        }
    }
}
