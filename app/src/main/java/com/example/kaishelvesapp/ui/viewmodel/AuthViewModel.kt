package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.repository.AuthOperationResult
import com.example.kaishelvesapp.data.model.Usuario
import com.example.kaishelvesapp.data.model.UserPrivacySettings
import com.example.kaishelvesapp.data.repository.AuthRepository
import com.example.kaishelvesapp.data.repository.GuestMergeDecision
import com.example.kaishelvesapp.data.repository.GuestMergeStrategy
import com.example.kaishelvesapp.data.repository.LoginProviderState
import com.example.kaishelvesapp.ui.language.LanguageManager
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loginIdentifier: String = "",
    val email: String = "",
    val password: String = "",
    val accessUsername: String = "",
    val accessEmail: String = "",
    val accessCurrentPassword: String = "",
    val accessPassword: String = "",
    val accessPasswordConfirmation: String = "",
    val username: String = "",
    val guestUsername: String = "",
    val profilePhotoUri: String = "",
    val isLoading: Boolean = false,
    val user: Usuario? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val isEditingProfile: Boolean = false,
    val hasPasswordLogin: Boolean = false,
    val hasGoogleLogin: Boolean = false,
    val loginProviders: List<LoginProviderState> = emptyList(),
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

    fun onAccessUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(accessUsername = value)
    }

    fun onAccessEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(accessEmail = value)
    }

    fun onAccessCurrentPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(accessCurrentPassword = value)
    }

    fun onAccessPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(accessPassword = value)
    }

    fun onAccessPasswordConfirmationChange(value: String) {
        _uiState.value = _uiState.value.copy(accessPasswordConfirmation = value)
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
                        successMessage = authText(AuthMessage.ProfilePhotoUpdated)
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profilePhotoUri = currentUser.photoUrl,
                        errorMessage = authErrorText(error, AuthMessage.ProfilePhotoUpdateFailed)
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
            errorMessage = message.ifBlank { authText(AuthMessage.GoogleSignInFailed) },
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
                        accessUsername = usuario.usuario,
                        guestUsername = if (usuario.isGuest) usuario.usuario else _uiState.value.guestUsername,
                        profilePhotoUri = usuario.photoUrl,
                        isLoggedIn = true,
                        hasPasswordLogin = repository.hasPasswordLogin(),
                        hasGoogleLogin = repository.hasGoogleLogin(),
                        loginProviders = repository.getLoginProviders(),
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = authErrorText(error, AuthMessage.ProfileLoadFailed)
                    )
                }
        }
    }

    fun login() {
        val identifier = _uiState.value.loginIdentifier.trim()
        val password = _uiState.value.password.trim()

        if (identifier.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = authText(AuthMessage.LoginEmptyFields)
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
                        errorMessage = authErrorText(error, AuthMessage.LoginFailed)
                    )
                }
        }
    }

    fun loginWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = authText(AuthMessage.GoogleValidationFailed)
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
                        errorMessage = authErrorText(error, AuthMessage.GoogleSignInFailed)
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
                errorMessage = authText(AuthMessage.RegisterEmptyFields)
            )
            return
        }

        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(
                errorMessage = authText(AuthMessage.PasswordTooShort)
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
                        errorMessage = authErrorText(error, AuthMessage.RegisterFailed)
                    )
                }
        }
    }

    fun continueAsGuest() {
        val guestUsername = _uiState.value.guestUsername.trim()

        if (guestUsername.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = authText(AuthMessage.GuestUsernameRequired)
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
                        errorMessage = authErrorText(error, AuthMessage.GuestStartFailed)
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
                        successMessage = authText(AuthMessage.GuestMergeSuccess)
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = authErrorText(error, AuthMessage.GuestMergeFailed)
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
                errorMessage = authText(AuthMessage.UsernameRequired)
            )
            return
        }

        if (_uiState.value.user?.isGuest != true && email.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = authText(AuthMessage.EmailRequired)
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
                        accessUsername = updatedUser.usuario,
                        profilePhotoUri = updatedUser.photoUrl,
                        isEditingProfile = true,
                        successMessage = authText(AuthMessage.ProfileUpdated)
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = authErrorText(error, AuthMessage.ProfileUpdateFailed)
                    )
                }
        }
    }

    fun savePasswordLogin() {
        val email = _uiState.value.accessEmail.trim()
        val password = _uiState.value.accessPassword.trim()
        val confirmation = _uiState.value.accessPasswordConfirmation.trim()

        if (email.isBlank() || password.isBlank() || confirmation.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = authText(AuthMessage.PasswordLoginEmptyFields)
            )
            return
        }

        if (password != confirmation) {
            _uiState.value = _uiState.value.copy(
                errorMessage = authText(AuthMessage.PasswordsDoNotMatch)
            )
            return
        }

        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(
                errorMessage = authText(AuthMessage.PasswordTooShort)
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val hadPasswordLogin = _uiState.value.hasPasswordLogin
            val result = repository.savePasswordLogin(email, password)

            result
                .onSuccess { updatedUser ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = updatedUser,
                        email = updatedUser.email,
                        username = updatedUser.usuario,
                        accessUsername = updatedUser.usuario,
                        accessEmail = "",
                        accessPassword = "",
                        accessPasswordConfirmation = "",
                        hasPasswordLogin = true,
                        loginProviders = repository.getLoginProviders(),
                        successMessage = if (hadPasswordLogin) {
                            authText(AuthMessage.PasswordUpdated)
                        } else {
                            authText(AuthMessage.PasswordLoginLinked)
                        }
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = authErrorText(error, AuthMessage.PasswordLoginUpdateFailed)
                    )
                }
        }
    }

    fun changeCurrentPassword() {
        val email = _uiState.value.user?.email?.trim().orEmpty().ifBlank {
            _uiState.value.email.trim()
        }
        val currentPassword = _uiState.value.accessCurrentPassword.trim()
        val password = _uiState.value.accessPassword.trim()
        val confirmation = _uiState.value.accessPasswordConfirmation.trim()

        if (currentPassword.isBlank() || password.isBlank() || confirmation.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = authText(AuthMessage.ChangePasswordEmptyFields)
            )
            return
        }

        if (password != confirmation) {
            _uiState.value = _uiState.value.copy(
                errorMessage = authText(AuthMessage.PasswordsDoNotMatch)
            )
            return
        }

        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(
                errorMessage = authText(AuthMessage.PasswordTooShort)
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val result = repository.changePassword(email, currentPassword, password)

            result
                .onSuccess { updatedUser ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = updatedUser,
                        email = updatedUser.email,
                        username = updatedUser.usuario,
                        accessUsername = updatedUser.usuario,
                        accessCurrentPassword = "",
                        accessPassword = "",
                        accessPasswordConfirmation = "",
                        hasPasswordLogin = true,
                        loginProviders = repository.getLoginProviders(),
                        successMessage = authText(AuthMessage.PasswordUpdated)
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = authErrorText(error, AuthMessage.PasswordUpdateFailed)
                    )
                }
        }
    }

    fun unlinkLoginProvider(providerId: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            repository.unlinkLoginProvider(providerId)
                .onSuccess { updatedUser ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = updatedUser,
                        email = updatedUser.email,
                        username = updatedUser.usuario,
                        hasPasswordLogin = repository.hasPasswordLogin(),
                        hasGoogleLogin = repository.hasGoogleLogin(),
                        loginProviders = repository.getLoginProviders(),
                        successMessage = authText(AuthMessage.LoginProviderRemoved)
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = authErrorText(error, AuthMessage.LoginProviderRemoveFailed)
                    )
                }
        }
    }

    fun linkGoogleLogin(idToken: String) {
        if (idToken.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = authText(AuthMessage.GoogleValidationFailed)
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            repository.linkGoogleLogin(idToken)
                .onSuccess { updatedUser ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = updatedUser,
                        email = updatedUser.email,
                        username = updatedUser.usuario,
                        profilePhotoUri = updatedUser.photoUrl,
                        hasPasswordLogin = repository.hasPasswordLogin(),
                        hasGoogleLogin = repository.hasGoogleLogin(),
                        loginProviders = repository.getLoginProviders(),
                        successMessage = authText(AuthMessage.GoogleLoginLinked)
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = authErrorText(error, AuthMessage.GoogleLinkFailed)
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
                        errorMessage = authErrorText(error, AuthMessage.PrivacyUpdateFailed)
                    )
                }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState(isLoggedIn = false)
    }

    private fun authErrorText(error: Throwable, fallback: AuthMessage): String {
        val firebaseCode = (error as? FirebaseAuthException)?.errorCode
        val message = error.message.orEmpty()

        val mappedMessage = when {
            firebaseCode in invalidCredentialCodes ||
                message.contains("password is invalid", ignoreCase = true) ||
                message.contains("no user record", ignoreCase = true) -> {
                AuthMessage.InvalidCredentials
            }

            firebaseCode == "ERROR_INVALID_EMAIL" ||
                message.contains("email address is badly formatted", ignoreCase = true) -> {
                AuthMessage.InvalidEmail
            }

            firebaseCode == "ERROR_EMAIL_ALREADY_IN_USE" ||
                message.contains("email address is already in use", ignoreCase = true) -> {
                AuthMessage.EmailAlreadyInUse
            }

            message.contains("nombre de usuario ya esta en uso", ignoreCase = true) ||
                message.contains("nombre de usuario ya está en uso", ignoreCase = true) ||
                message.contains("username is already in use", ignoreCase = true) -> {
                AuthMessage.UsernameAlreadyInUse
            }

            firebaseCode == "ERROR_WEAK_PASSWORD" ||
                message.contains("password should be at least", ignoreCase = true) -> {
                AuthMessage.PasswordTooShort
            }

            firebaseCode == "ERROR_REQUIRES_RECENT_LOGIN" ||
                message.contains("requires recent authentication", ignoreCase = true) -> {
                AuthMessage.RecentLoginRequired
            }

            message.contains("already linked", ignoreCase = true) ||
                message.contains("already associated", ignoreCase = true) -> {
                AuthMessage.LoginProviderAlreadyLinked
            }

            message.contains("network", ignoreCase = true) -> {
                AuthMessage.NetworkError
            }

            else -> fallback
        }

        return authText(mappedMessage)
    }

    private fun authText(message: AuthMessage): String {
        val isEnglish = LanguageManager.getCurrentLanguage() == "en"
        return if (isEnglish) {
            message.english
        } else {
            message.spanish
        }
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
                    accessUsername = result.user.usuario,
                    profilePhotoUri = result.user.photoUrl,
                    isLoggedIn = true,
                    hasPasswordLogin = repository.hasPasswordLogin(),
                    hasGoogleLogin = repository.hasGoogleLogin(),
                    loginProviders = repository.getLoginProviders(),
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

private val invalidCredentialCodes = setOf(
    "ERROR_INVALID_CREDENTIAL",
    "ERROR_WRONG_PASSWORD",
    "ERROR_USER_NOT_FOUND",
    "ERROR_USER_DISABLED"
)

private enum class AuthMessage(
    val spanish: String,
    val english: String
) {
    ProfilePhotoUpdated(
        spanish = "Foto de perfil actualizada correctamente",
        english = "Profile photo updated successfully"
    ),
    ProfilePhotoUpdateFailed(
        spanish = "No se pudo actualizar la foto de perfil",
        english = "The profile photo could not be updated"
    ),
    ProfileLoadFailed(
        spanish = "No se pudo cargar el perfil",
        english = "The profile could not be loaded"
    ),
    LoginEmptyFields(
        spanish = "Completa correo o usuario y contraseña",
        english = "Please enter your email or username and password"
    ),
    LoginFailed(
        spanish = "Error al iniciar sesión",
        english = "Sign-in failed"
    ),
    InvalidCredentials(
        spanish = "El correo, usuario o contraseña no son correctos",
        english = "The email, username, or password is incorrect"
    ),
    InvalidEmail(
        spanish = "El correo electrónico no tiene un formato válido",
        english = "The email address is not valid"
    ),
    GoogleValidationFailed(
        spanish = "No se pudo validar la cuenta de Google",
        english = "The Google account could not be verified"
    ),
    GoogleSignInFailed(
        spanish = "Error al iniciar sesión con Google",
        english = "Google sign-in failed"
    ),
    RegisterEmptyFields(
        spanish = "Completa usuario, email y contraseña",
        english = "Please enter username, email, and password"
    ),
    RegisterFailed(
        spanish = "Error al registrar usuario",
        english = "User registration failed"
    ),
    EmailAlreadyInUse(
        spanish = "Ese correo electrónico ya está en uso",
        english = "That email address is already in use"
    ),
    UsernameAlreadyInUse(
        spanish = "Ese nombre de usuario ya está en uso",
        english = "That username is already in use"
    ),
    PasswordTooShort(
        spanish = "La contraseña debe tener al menos 6 caracteres",
        english = "Password must be at least 6 characters long"
    ),
    GuestUsernameRequired(
        spanish = "Introduce un nombre de usuario para continuar sin cuenta",
        english = "Enter a username to continue without an account"
    ),
    GuestStartFailed(
        spanish = "No se pudo iniciar el modo invitado",
        english = "Guest mode could not be started"
    ),
    GuestMergeSuccess(
        spanish = "Datos locales sincronizados con tu cuenta",
        english = "Local data synced with your account"
    ),
    GuestMergeFailed(
        spanish = "No se pudo completar la fusión de datos",
        english = "The data merge could not be completed"
    ),
    UsernameRequired(
        spanish = "El nombre de usuario no puede estar vacío",
        english = "Username cannot be empty"
    ),
    EmailRequired(
        spanish = "El correo electrónico no puede estar vacío",
        english = "Email cannot be empty"
    ),
    ProfileUpdated(
        spanish = "Perfil actualizado correctamente",
        english = "Profile updated successfully"
    ),
    ProfileUpdateFailed(
        spanish = "No se pudo actualizar el perfil",
        english = "The profile could not be updated"
    ),
    PasswordLoginEmptyFields(
        spanish = "Completa email, contraseña y confirmación",
        english = "Please enter email, password, and confirmation"
    ),
    ChangePasswordEmptyFields(
        spanish = "Completa contraseña actual, nueva contraseña y confirmación",
        english = "Please enter current password, new password, and confirmation"
    ),
    PasswordsDoNotMatch(
        spanish = "Las contraseñas no coinciden",
        english = "Passwords do not match"
    ),
    PasswordUpdated(
        spanish = "Contraseña actualizada correctamente",
        english = "Password updated successfully"
    ),
    PasswordLoginLinked(
        spanish = "Inicio de sesión con email y contraseña asociado correctamente",
        english = "Email and password sign-in linked successfully"
    ),
    PasswordLoginUpdateFailed(
        spanish = "No se pudo actualizar el acceso con contraseña",
        english = "Password access could not be updated"
    ),
    PasswordUpdateFailed(
        spanish = "No se pudo actualizar la contraseña",
        english = "The password could not be updated"
    ),
    RecentLoginRequired(
        spanish = "Vuelve a iniciar sesión para completar este cambio",
        english = "Please sign in again to complete this change"
    ),
    LoginProviderRemoved(
        spanish = "Inicio de sesión eliminado correctamente",
        english = "Sign-in method removed successfully"
    ),
    LoginProviderRemoveFailed(
        spanish = "No se pudo quitar ese inicio de sesión",
        english = "That sign-in method could not be removed"
    ),
    LoginProviderAlreadyLinked(
        spanish = "Ese inicio de sesión ya está asociado a una cuenta",
        english = "That sign-in method is already linked to an account"
    ),
    GoogleLoginLinked(
        spanish = "Inicio de sesión con Google asociado correctamente",
        english = "Google sign-in linked successfully"
    ),
    GoogleLinkFailed(
        spanish = "No se pudo asociar Google como inicio de sesión",
        english = "Google could not be linked as a sign-in method"
    ),
    PrivacyUpdateFailed(
        spanish = "No se pudo actualizar la privacidad",
        english = "Privacy settings could not be updated"
    ),
    NetworkError(
        spanish = "Revisa tu conexión e inténtalo de nuevo",
        english = "Check your connection and try again"
    )
}
