package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.repository.AuthRepository
import com.example.kaishelvesapp.data.repository.UsernameConflictGroup
import com.example.kaishelvesapp.data.repository.UsernameRegistryStatus
import com.example.kaishelvesapp.data.repository.UsernameRegistryUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AdminUsernamesPanelMode {
    REGISTRY,
    DUPLICATES
}

enum class AdminUsernameRegistryFilter {
    ALL,
    NEW,
    MODIFIED
}

data class AdminUsernamesUiState(
    val isLoading: Boolean = false,
    val selectedMode: AdminUsernamesPanelMode = AdminUsernamesPanelMode.REGISTRY,
    val selectedFilter: AdminUsernameRegistryFilter = AdminUsernameRegistryFilter.ALL,
    val showNotifiedOnly: Boolean = false,
    val groups: List<UsernameConflictGroup> = emptyList(),
    val users: List<UsernameRegistryUser> = emptyList(),
    val draftUsernames: Map<String, String> = emptyMap(),
    val savingUserIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AdminUsernamesViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUsernamesUiState())
    val uiState: StateFlow<AdminUsernamesUiState> = _uiState.asStateFlow()

    fun loadAll() {
        when (_uiState.value.selectedMode) {
            AdminUsernamesPanelMode.REGISTRY -> loadUsernameRegistry()
            AdminUsernamesPanelMode.DUPLICATES -> loadDuplicateGroups()
        }
    }

    fun onModeChange(mode: AdminUsernamesPanelMode) {
        _uiState.value = _uiState.value.copy(
            selectedMode = mode,
            errorMessage = null,
            successMessage = null
        )
        loadAll()
    }

    fun onFilterChange(filter: AdminUsernameRegistryFilter) {
        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            showNotifiedOnly = false
        )
    }

    fun toggleNotifiedOnly() {
        _uiState.value = _uiState.value.copy(
            showNotifiedOnly = !_uiState.value.showNotifiedOnly
        )
    }

    fun loadUsernameRegistry() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.getUsernameRegistryUsers()
                .onSuccess { users ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        users = users,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudieron cargar los usuarios"
                    )
                }
        }
    }

    fun loadDuplicateGroups() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.getDuplicateUsernameGroups()
                .onSuccess { groups ->
                    val drafts = groups
                        .flatMap { group -> group.users }
                        .associate { user -> user.uid to user.username }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        groups = groups,
                        draftUsernames = drafts,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudieron cargar los conflictos"
                    )
                }
        }
    }

    fun markUsernameReviewed(uid: String) {
        if (uid.isBlank() || uid in _uiState.value.savingUserIds) return

        _uiState.value = _uiState.value.copy(
            savingUserIds = _uiState.value.savingUserIds + uid,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            repository.markUsernameReviewed(uid)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        savingUserIds = _uiState.value.savingUserIds - uid,
                        successMessage = "Nombre marcado como revisado"
                    )
                    loadUsernameRegistry()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        savingUserIds = _uiState.value.savingUserIds - uid,
                        errorMessage = error.message ?: "No se pudo marcar como revisado"
                    )
                }
        }
    }

    fun requestUsernameChange(uid: String) {
        if (uid.isBlank() || uid in _uiState.value.savingUserIds) return

        _uiState.value = _uiState.value.copy(
            savingUserIds = _uiState.value.savingUserIds + uid,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            repository.requestUsernameChange(uid)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        savingUserIds = _uiState.value.savingUserIds - uid,
                        successMessage = "Usuario notificado"
                    )
                    loadUsernameRegistry()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        savingUserIds = _uiState.value.savingUserIds - uid,
                        errorMessage = error.message ?: "No se pudo enviar la notificación"
                    )
                }
        }
    }

    fun onDraftUsernameChange(uid: String, value: String) {
        _uiState.value = _uiState.value.copy(
            draftUsernames = _uiState.value.draftUsernames + (uid to value)
        )
    }

    fun resolveUsername(uid: String, currentUsername: String) {
        val newUsername = _uiState.value.draftUsernames[uid]?.trim().orEmpty()
        if (newUsername.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "El nombre de usuario no puede estar vacio"
            )
            return
        }

        if (newUsername == currentUsername.trim()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Introduce un nombre distinto para resolver el conflicto"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            savingUserIds = _uiState.value.savingUserIds + uid,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            repository.adminRenameUser(uid, newUsername)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        savingUserIds = _uiState.value.savingUserIds - uid,
                        successMessage = "Usuario actualizado correctamente"
                    )
                    loadDuplicateGroups()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        savingUserIds = _uiState.value.savingUserIds - uid,
                        errorMessage = error.message ?: "No se pudo actualizar el usuario"
                    )
                }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}

fun UsernameRegistryUser.matchesAdminUsernameFilter(
    filter: AdminUsernameRegistryFilter,
    showNotifiedOnly: Boolean
): Boolean {
    if (showNotifiedOnly) {
        return status == UsernameRegistryStatus.NOTIFIED
    }

    val filterMatches = when (filter) {
        AdminUsernameRegistryFilter.ALL -> true
        AdminUsernameRegistryFilter.NEW -> status == UsernameRegistryStatus.NEW
        AdminUsernameRegistryFilter.MODIFIED -> status == UsernameRegistryStatus.MODIFIED
    }
    return filterMatches
}
