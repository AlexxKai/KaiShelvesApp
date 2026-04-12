package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.UserBookList
import com.example.kaishelvesapp.data.repository.UserListsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserListDetailUiState(
    val isLoading: Boolean = false,
    val isRemoving: Boolean = false,
    val userList: UserBookList? = null,
    val books: List<Libro> = emptyList(),
    val errorMessageRes: Int? = null,
    val successMessageRes: Int? = null
)

class UserListDetailViewModel(
    private val repository: UserListsRepository = UserListsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserListDetailUiState())
    val uiState: StateFlow<UserListDetailUiState> = _uiState.asStateFlow()

    fun loadListDetail(listId: String) {
        if (listId.isBlank()) return

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessageRes = null,
            successMessageRes = null
        )

        viewModelScope.launch {
            val listResult = repository.getListById(listId)
            val booksResult = repository.getBooksInList(listId)

            if (listResult.isSuccess && booksResult.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userList = listResult.getOrNull(),
                    books = booksResult.getOrDefault(emptyList()),
                    errorMessageRes = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessageRes = R.string.list_detail_load_error
                )
            }
        }
    }

    fun removeBookFromList(listId: String, bookId: String) {
        _uiState.value = _uiState.value.copy(
            isRemoving = true,
            errorMessageRes = null,
            successMessageRes = null
        )

        viewModelScope.launch {
            repository.removeBookFromList(listId, bookId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isRemoving = false,
                        successMessageRes = R.string.book_removed_from_list
                    )
                    loadListDetail(listId)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isRemoving = false,
                        errorMessageRes = R.string.remove_book_from_list_error
                    )
                }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessageRes = null,
            successMessageRes = null
        )
    }
}
