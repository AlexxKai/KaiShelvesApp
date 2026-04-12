package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.LibroLeido
import com.example.kaishelvesapp.data.model.UserBookList
import com.example.kaishelvesapp.data.repository.BookRepository
import com.example.kaishelvesapp.data.repository.UserListsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BookDetailUiState(
    val isLoading: Boolean = false,
    val isListsLoading: Boolean = false,
    val isSavingLists: Boolean = false,
    val isAlreadyRead: Boolean = false,
    val readBook: LibroLeido? = null,
    val availableLists: List<UserBookList> = emptyList(),
    val selectedListIds: Set<String> = emptySet(),
    val errorMessageRes: Int? = null,
    val successMessageRes: Int? = null
)

class BookDetailViewModel(
    private val repository: BookRepository = BookRepository(),
    private val userListsRepository: UserListsRepository = UserListsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    fun cargarEstadoLectura(bookId: String) {
        if (bookId.isBlank()) return

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessageRes = null
        )

        viewModelScope.launch {
            val result = repository.obtenerLibroLeido(bookId)

            result
                .onSuccess { libroLeido ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAlreadyRead = libroLeido != null,
                        readBook = libroLeido,
                        errorMessageRes = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessageRes = R.string.book_status_error
                    )
                }
        }
    }

    fun cargarListasParaLibro(bookId: String) {
        _uiState.value = _uiState.value.copy(
            isListsLoading = true,
            errorMessageRes = null
        )

        viewModelScope.launch {
            val listsResult = userListsRepository.getUserLists()
            val selectedResult = if (bookId.isBlank()) {
                Result.success(emptySet())
            } else {
                userListsRepository.getSelectedListIdsForBook(bookId)
            }

            if (listsResult.isSuccess && selectedResult.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isListsLoading = false,
                    availableLists = listsResult.getOrDefault(emptyList()),
                    selectedListIds = selectedResult.getOrDefault(emptySet()),
                    errorMessageRes = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isListsLoading = false,
                    errorMessageRes = R.string.book_lists_load_error
                )
            }
        }
    }

    fun guardarListas(libro: Libro, selectedListIds: Set<String>) {
        _uiState.value = _uiState.value.copy(
            isSavingLists = true,
            errorMessageRes = null,
            successMessageRes = null
        )

        viewModelScope.launch {
            userListsRepository.updateBookAssignments(libro, selectedListIds)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSavingLists = false,
                        selectedListIds = selectedListIds,
                        successMessageRes = R.string.book_lists_updated
                    )
                    cargarListasParaLibro(libro.id.ifBlank { libro.isbn })
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSavingLists = false,
                        errorMessageRes = R.string.book_lists_update_error
                    )
                }
        }
    }

    fun refrescarLectura(bookId: String) {
        cargarEstadoLectura(bookId)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessageRes = null,
            successMessageRes = null
        )
    }
}
