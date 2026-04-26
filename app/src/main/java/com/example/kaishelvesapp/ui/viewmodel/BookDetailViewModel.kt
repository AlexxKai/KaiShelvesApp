package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.LibroLeido
import com.example.kaishelvesapp.data.model.UserBookList
import com.example.kaishelvesapp.data.model.UserBookTag
import com.example.kaishelvesapp.data.repository.BookRepository
import com.example.kaishelvesapp.data.repository.UserListsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BookDetailUiState(
    val isLoading: Boolean = false,
    val isListsLoading: Boolean = false,
    val isBookSelectionLoading: Boolean = false,
    val isSavingLists: Boolean = false,
    val isAlreadyRead: Boolean = false,
    val readBook: LibroLeido? = null,
    val availableLists: List<UserBookList> = emptyList(),
    val selectedListIds: Set<String> = emptySet(),
    val availableTags: List<UserBookTag> = emptyList(),
    val selectedTagIds: Set<String> = emptySet(),
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
        val cachedLists = userListsRepository.getCachedUserListsOrDefault()
        val cachedTags = userListsRepository.getCachedUserTags()

        _uiState.value = _uiState.value.copy(
            isListsLoading = cachedLists.isEmpty(),
            isBookSelectionLoading = bookId.isNotBlank(),
            availableLists = cachedLists,
            availableTags = cachedTags,
            selectedListIds = emptySet(),
            selectedTagIds = emptySet(),
            errorMessageRes = null
        )

        viewModelScope.launch {
            val selectedResult = if (bookId.isBlank()) {
                Result.success(emptySet())
            } else {
                userListsRepository.getSelectedListIdsForBook(bookId)
            }
            val selectedTagsResult = if (bookId.isBlank()) {
                Result.success(emptySet())
            } else {
                userListsRepository.getSelectedTagIdsForBook(bookId)
            }
            val listsResult = userListsRepository.getUserLists()
            val tagsResult = userListsRepository.getUserTags()

            if (listsResult.isSuccess && selectedResult.isSuccess && tagsResult.isSuccess && selectedTagsResult.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isListsLoading = false,
                    isBookSelectionLoading = false,
                    availableLists = listsResult.getOrDefault(emptyList()),
                    selectedListIds = selectedResult.getOrDefault(emptySet()),
                    availableTags = tagsResult.getOrDefault(emptyList()),
                    selectedTagIds = selectedTagsResult.getOrDefault(emptySet()),
                    errorMessageRes = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isListsLoading = false,
                    isBookSelectionLoading = false,
                    errorMessageRes = R.string.book_lists_load_error
                )
            }
        }
    }

    fun guardarOrganizacion(libro: Libro, selectedListIds: Set<String>, selectedTagIds: Set<String>) {
        _uiState.value = _uiState.value.copy(
            isSavingLists = true,
            errorMessageRes = null,
            successMessageRes = null
        )

        viewModelScope.launch {
            val listsResult = userListsRepository.updateBookAssignments(libro, selectedListIds)
            val tagsResult = userListsRepository.updateBookTags(
                libro.id.ifBlank { libro.isbn },
                selectedTagIds
            )

            if (listsResult.isSuccess && tagsResult.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isSavingLists = false,
                    selectedListIds = selectedListIds,
                    selectedTagIds = selectedTagIds,
                    successMessageRes = R.string.book_lists_updated
                )
                cargarListasParaLibro(libro.id.ifBlank { libro.isbn })
            } else {
                _uiState.value = _uiState.value.copy(
                    isSavingLists = false,
                    errorMessageRes = R.string.book_lists_update_error
                )
            }
        }
    }

    fun guardarListas(libro: Libro, selectedListIds: Set<String>) {
        guardarOrganizacion(libro, selectedListIds, _uiState.value.selectedTagIds)
    }

    fun guardarLecturaConResena(
        libro: Libro,
        selectedTagIds: Set<String>,
        puntuacion: Int,
        resena: String,
        contieneSpoilers: Boolean
    ) {
        _uiState.value = _uiState.value.copy(
            isSavingLists = true,
            errorMessageRes = null,
            successMessageRes = null
        )

        viewModelScope.launch {
            val listsResult = userListsRepository.updateBookAssignments(
                libro,
                setOf(UserListsRepository.SYSTEM_LIST_READ_ID)
            )
            val tagsResult = userListsRepository.updateBookTags(
                libro.id.ifBlank { libro.isbn },
                selectedTagIds
            )
            val reviewResult = repository.actualizarResenaLectura(
                bookId = libro.id.ifBlank { libro.isbn },
                puntuacion = puntuacion,
                resena = resena,
                contieneSpoilers = contieneSpoilers
            )

            if (listsResult.isSuccess && tagsResult.isSuccess && reviewResult.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isSavingLists = false,
                    selectedListIds = setOf(UserListsRepository.SYSTEM_LIST_READ_ID),
                    selectedTagIds = selectedTagIds,
                    successMessageRes = R.string.book_lists_updated
                )
                cargarListasParaLibro(libro.id.ifBlank { libro.isbn })
                cargarEstadoLectura(libro.id.ifBlank { libro.isbn })
            } else {
                _uiState.value = _uiState.value.copy(
                    isSavingLists = false,
                    errorMessageRes = R.string.book_lists_update_error
                )
            }
        }
    }

    fun actualizarLecturaResena(
        bookId: String,
        puntuacion: Int,
        resena: String,
        contieneSpoilers: Boolean
    ) {
        _uiState.value = _uiState.value.copy(
            isSavingLists = true,
            errorMessageRes = null,
            successMessageRes = null
        )

        viewModelScope.launch {
            repository.actualizarResenaLectura(
                bookId = bookId,
                puntuacion = puntuacion,
                resena = resena,
                contieneSpoilers = contieneSpoilers
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSavingLists = false,
                        successMessageRes = R.string.book_lists_updated
                    )
                    cargarEstadoLectura(bookId)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSavingLists = false,
                        errorMessageRes = R.string.book_lists_update_error
                    )
                }
        }
    }

    fun clearBookOrganization(bookId: String) {
        _uiState.value = _uiState.value.copy(
            isSavingLists = true,
            errorMessageRes = null,
            successMessageRes = null
        )

        viewModelScope.launch {
            userListsRepository.clearBookOrganization(bookId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSavingLists = false,
                        selectedListIds = emptySet(),
                        selectedTagIds = emptySet(),
                        successMessageRes = R.string.book_lists_updated
                    )
                    cargarListasParaLibro(bookId)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSavingLists = false,
                        errorMessageRes = R.string.book_lists_update_error
                    )
                }
        }
    }

    fun createList(name: String, bookId: String, description: String = "") {
        viewModelScope.launch {
            userListsRepository.createList(name, description)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        successMessageRes = R.string.list_created
                    )
                    cargarListasParaLibro(bookId)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        errorMessageRes = R.string.list_create_error
                    )
                }
        }
    }

    fun createTag(name: String, bookId: String) {
        viewModelScope.launch {
            userListsRepository.createTag(name)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        successMessageRes = R.string.tag_created
                    )
                    cargarListasParaLibro(bookId)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        errorMessageRes = R.string.tag_create_error
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
