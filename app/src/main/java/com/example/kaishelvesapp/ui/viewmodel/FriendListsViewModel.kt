package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.repository.FriendBookListSummary
import com.example.kaishelvesapp.data.repository.FriendBookTagSummary
import com.example.kaishelvesapp.data.repository.FriendsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FriendListsUiState(
    val isLoading: Boolean = false,
    val lists: List<FriendBookListSummary> = emptyList(),
    val tags: List<FriendBookTagSummary> = emptyList(),
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
            val listsResult = repository.loadFriendLists(friendUid)
            val tagsResult = repository.loadFriendTags(friendUid)

            if (listsResult.isSuccess && tagsResult.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lists = listsResult.getOrDefault(emptyList()),
                    tags = tagsResult.getOrDefault(emptyList()),
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = listsResult.exceptionOrNull()?.message
                        ?: tagsResult.exceptionOrNull()?.message
                        ?: "No se pudieron cargar las listas"
                )
            }
        }
    }
}
