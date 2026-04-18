package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.model.Usuario
import com.example.kaishelvesapp.data.repository.FriendSuggestion
import com.example.kaishelvesapp.data.repository.FriendsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FriendSuggestionsUiState(
    val isLoading: Boolean = false,
    val query: String = "",
    val suggestions: List<FriendSuggestion> = emptyList(),
    val sentRequestIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val filteredSuggestions: List<FriendSuggestion>
        get() {
            val trimmedQuery = query.trim()
            if (trimmedQuery.isBlank()) {
                return suggestions
            }

            return suggestions.filter { suggestion ->
                suggestion.user.usuario.contains(trimmedQuery, ignoreCase = true) ||
                    suggestion.user.email.contains(trimmedQuery, ignoreCase = true)
            }
        }
}

class FriendSuggestionsViewModel(
    private val repository: FriendsRepository = FriendsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendSuggestionsUiState())
    val uiState: StateFlow<FriendSuggestionsUiState> = _uiState.asStateFlow()

    init {
        loadSuggestions()
    }

    fun onQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun loadSuggestions() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.loadSuggestions()
                .onSuccess { data ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        suggestions = data.suggestions,
                        sentRequestIds = data.sentRequestIds,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudieron cargar las sugerencias"
                    )
                }
        }
    }

    fun sendFriendRequest(user: Usuario) {
        if (user.uid in _uiState.value.sentRequestIds) return

        viewModelScope.launch {
            repository.sendFriendRequest(user)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        sentRequestIds = _uiState.value.sentRequestIds + user.uid,
                        successMessage = "Solicitud enviada a ${user.usuario.ifBlank { user.email }}",
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "No se pudo enviar la solicitud",
                        successMessage = null
                    )
                }
        }
    }
}
