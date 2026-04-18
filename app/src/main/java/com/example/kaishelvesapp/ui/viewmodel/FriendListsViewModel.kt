package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.repository.FriendBookListSummary
import com.example.kaishelvesapp.data.repository.FriendsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FriendListsUiState(
    val isLoading: Boolean = false,
    val lists: List<FriendBookListSummary> = emptyList(),
    val errorMessage: String? = null
)

class FriendListsViewModel(
    private val repository: FriendsRepository = FriendsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendListsUiState())
    val uiState: StateFlow<FriendListsUiState> = _uiState.asStateFlow()

    fun loadFriendLists(friendUid: String) {
        if (friendUid.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "No se pudo identificar al usuario"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.loadFriendLists(friendUid)
                .onSuccess { lists ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lists = lists,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudieron cargar las listas"
                    )
                }
        }
    }
}
