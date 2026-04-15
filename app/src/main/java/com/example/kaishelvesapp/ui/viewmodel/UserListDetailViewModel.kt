package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.repository.BookRepository
import com.example.kaishelvesapp.data.model.UserBookList
import com.example.kaishelvesapp.data.repository.UserListsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserListDetailBookItem(
    val book: Libro,
    val rating: Int? = null,
    val readDate: String? = null
)

data class UserListDetailUiState(
    val isLoading: Boolean = false,
    val isRemoving: Boolean = false,
    val userList: UserBookList? = null,
    val books: List<UserListDetailBookItem> = emptyList(),
    val errorMessageRes: Int? = null,
    val successMessageRes: Int? = null
)

class UserListDetailViewModel(
    private val repository: UserListsRepository = UserListsRepository(),
    private val bookRepository: BookRepository = BookRepository()
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
            val readBooksResult = if (listId == UserListsRepository.SYSTEM_LIST_READ_ID) {
                bookRepository.obtenerListaLecturas()
            } else {
                Result.success(emptyList())
            }

            if (listResult.isSuccess && booksResult.isSuccess && readBooksResult.isSuccess) {
                val readBooksById = readBooksResult.getOrDefault(emptyList()).associateBy { it.isbn }
                val items = booksResult.getOrDefault(emptyList()).map { book ->
                    val readBook = readBooksById[book.id.ifBlank { book.isbn }]
                    UserListDetailBookItem(
                        book = book,
                        rating = readBook?.puntuacion,
                        readDate = readBook?.fechaLeido
                    )
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userList = listResult.getOrNull(),
                    books = items,
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
