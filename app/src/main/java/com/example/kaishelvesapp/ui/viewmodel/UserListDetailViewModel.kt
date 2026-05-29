package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.DeviceBookFormat
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
    val readDate: String? = null,
    val addedOrder: Int = Int.MAX_VALUE,
    val ownedFormats: List<DeviceBookFormat> = emptyList(),
    val ownedUri: String = "",
    val ownedFileName: String = "",
    val ownedLocation: String = "",
    val ownedMimeType: String = "",
    val ownedSizeBytes: Long? = null,
    val ownedModifiedAtMillis: Long? = null
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
            if (listId.startsWith(USER_TAG_DETAIL_PREFIX)) {
                val tagId = listId.removePrefix(USER_TAG_DETAIL_PREFIX)
                val tagResult = repository.getTagById(tagId)
                val booksResult = repository.getBooksInTag(tagId)

                if (tagResult.isSuccess && booksResult.isSuccess) {
                    val books = booksResult.getOrDefault(emptyList()).mapIndexed { index, book ->
                        UserListDetailBookItem(
                            book = book,
                            addedOrder = index
                        )
                    }
                    val tag = tagResult.getOrNull()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userList = UserBookList(
                            id = listId,
                            name = tag?.name.orEmpty(),
                            bookCount = books.size
                        ),
                        books = books,
                        errorMessageRes = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessageRes = R.string.list_detail_load_error
                    )
                }
                return@launch
            }

            val listResult = repository.getListById(listId)
            val ownedBooksResult = if (listId == UserListsRepository.SYSTEM_LIST_OWNED_ID) {
                // La lista "Tengo" usa los registros locales de Biblioteca y conserva sus formatos.
                repository.getOwnedDeviceBooks()
            } else {
                Result.success(emptyList())
            }
            val readBooksResult = if (listId == UserListsRepository.SYSTEM_LIST_READ_ID) {
                bookRepository.obtenerListaLecturas()
            } else {
                Result.success(emptyList())
            }
            val booksResult = if (listId == UserListsRepository.SYSTEM_LIST_OWNED_ID) {
                Result.success(emptyList())
            } else {
                repository.getBooksInList(listId)
            }

            if (listResult.isSuccess && booksResult.isSuccess && readBooksResult.isSuccess && ownedBooksResult.isSuccess) {
                val addedOrderByBookId = booksResult.getOrDefault(emptyList())
                    .mapIndexed { index, book ->
                        book.id.ifBlank { book.isbn } to index
                    }
                    .toMap()
                val items = when (listId) {
                    UserListsRepository.SYSTEM_LIST_READ_ID -> {
                        readBooksResult.getOrDefault(emptyList()).mapIndexed { index, readBook ->
                            val readBookId = readBook.id.ifBlank { readBook.isbn }
                            UserListDetailBookItem(
                                book = Libro(
                                    id = readBookId,
                                    isbn = readBook.isbn,
                                    titulo = readBook.titulo,
                                    autor = readBook.autor,
                                    editorial = readBook.editorial,
                                    genero = readBook.genero,
                                    fechaPublicacion = readBook.fechaPublicacion,
                                    paginas = readBook.paginas,
                                    imagen = readBook.imagen,
                                    pdf = readBook.pdf
                                ),
                                rating = readBook.puntuacion,
                                readDate = readBook.fechaLeido,
                                addedOrder = addedOrderByBookId[readBookId] ?: index
                            )
                        }
                    }

                    UserListsRepository.SYSTEM_LIST_OWNED_ID -> {
                        ownedBooksResult.getOrDefault(emptyList()).mapIndexed { index, ownedBook ->
                            UserListDetailBookItem(
                                book = ownedBook.book,
                                addedOrder = index,
                                ownedFormats = ownedBook.formats,
                                ownedUri = ownedBook.uri,
                                ownedFileName = ownedBook.name,
                                ownedLocation = ownedBook.location,
                                ownedMimeType = ownedBook.mimeType,
                                ownedSizeBytes = ownedBook.sizeBytes,
                                ownedModifiedAtMillis = ownedBook.modifiedAtMillis
                            )
                        }
                    }

                    else -> {
                        booksResult.getOrDefault(emptyList()).mapIndexed { index, book ->
                            UserListDetailBookItem(
                                book = book,
                                rating = null,
                                readDate = null,
                                addedOrder = index
                            )
                        }
                    }
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

    fun updateBooksOrder(listId: String, orderedBookIds: List<String>) {
        if (listId != UserListsRepository.SYSTEM_LIST_PENDING_ID) return

        _uiState.value = _uiState.value.copy(
            books = orderedBookIds.mapNotNull { bookId ->
                _uiState.value.books.firstOrNull { item ->
                    item.book.id.ifBlank { item.book.isbn } == bookId
                }
            }
        )

        viewModelScope.launch {
            repository.updateBooksInListOrder(listId, orderedBookIds)
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        errorMessageRes = R.string.list_order_update_error
                    )
                    loadListDetail(listId)
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
