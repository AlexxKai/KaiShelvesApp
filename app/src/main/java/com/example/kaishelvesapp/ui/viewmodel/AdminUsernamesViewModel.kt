package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.repository.AuthRepository
import com.example.kaishelvesapp.data.repository.UsernameConflictGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AdminUsernamesUiState(
    val isLoading: Boolean = false,
    val groups: List<UsernameConflictGroup> = emptyList(),
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
