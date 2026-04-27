package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.repository.FriendBookListDetailBookItem
import com.example.kaishelvesapp.data.repository.FriendsRepository
import com.example.kaishelvesapp.data.model.UserBookList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FriendListDetailUiState(
    val isLoading: Boolean = false,
    val userList: UserBookList? = null,
    val books: List<FriendBookListDetailBookItem> = emptyList(),
    val errorMessage: String? = null
)

class FriendListDetailViewModel(
    private val repository: FriendsRepository = FriendsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendListDetailUiState())
    val uiState: StateFlow<FriendListDetailUiState> = _uiState.asStateFlow()

    fun loadListDetail(friendUid: String, listId: String) {
        if (friendUid.isBlank() || listId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "No se pudo identificar la lista"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.loadFriendListDetail(friendUid, listId)
                .onSuccess { detail ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userList = detail.list,
                        books = detail.books,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudo cargar el detalle de la lista"
                    )
                }
        }
    }
}
