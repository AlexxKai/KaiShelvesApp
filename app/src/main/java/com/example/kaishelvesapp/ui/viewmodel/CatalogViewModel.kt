package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.remote.googlebooks.LibraryGenres
import com.example.kaishelvesapp.data.repository.BookRepository
import com.example.kaishelvesapp.data.repository.BookRepository.DiscoverCatalogMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CatalogUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val libros: List<Libro> = emptyList(),
    val generos: List<String> = listOf("Todos"),
    val selectedGenero: String = "Todos",
    val searchQuery: String = "",
    val discoverMode: DiscoverCatalogMode = DiscoverCatalogMode.SPECIAL,
    val selectedBook: Libro? = null,
    val scannedBook: Libro? = null,
    val scannedIsbn: String? = null,
    val scanHistory: List<Libro> = emptyList(),
    val isIsbnLookupLoading: Boolean = false,
    val isbnLookupError: String? = null,
    val errorMessage: String? = null
)

class CatalogViewModel(
    private val repository: BookRepository = BookRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CatalogUiState(
            generos = listOf("Todos") + LibraryGenres.all.map { it.label }
        )
    )
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private var isbnLookupJob: Job? = null
    private val isbnCache = mutableMapOf<String, Libro?>()

    init {
        cargarLibros()
        preloadDiscoverModes()
    }

    fun cargarLibros(refresh: Boolean = false) {
        val currentState = _uiState.value
        val cachedBooks = if (!refresh) {
            repository.getCachedDiscoverBooks(currentState.discoverMode)
        } else {
            emptyList()
        }

        if (cachedBooks.isNotEmpty()) {
            // Descubre abre desde caché persistida; la red queda reservada al gesto de refrescar.
            _uiState.value = currentState.copy(
                isLoading = false,
                isRefreshing = false,
                libros = cachedBooks,
                errorMessage = null
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = !refresh || currentState.libros.isEmpty(),
            isRefreshing = refresh && currentState.libros.isNotEmpty(),
            errorMessage = null
        )

        viewModelScope.launch {
            val result = repository.obtenerLibros(
                mode = currentState.discoverMode,
                previousBooks = if (refresh) currentState.libros else emptyList()
            )

            result
                .onSuccess { libros ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        libros = libros,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    val existingBooks = _uiState.value.libros
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = if (refresh && existingBooks.isNotEmpty()) {
                            null
                        } else {
                            error.message ?: "Error al cargar libros"
                        }
                    )
                }
        }
    }

    fun refrescarNovedades() {
        cargarLibros(refresh = true)
    }

    fun onDiscoverModeSelected(mode: DiscoverCatalogMode) {
        if (_uiState.value.discoverMode == mode) return

        _uiState.value = _uiState.value.copy(discoverMode = mode)
        cargarLibros(refresh = false)
    }

    private fun preloadDiscoverModes() {
        viewModelScope.launch {
            repository.preloadDiscoverBooks()
                .onSuccess {
                    val selectedMode = _uiState.value.discoverMode
                    val cachedBooks = repository.getCachedDiscoverBooks(selectedMode)

                    if (cachedBooks.isNotEmpty() && !_uiState.value.isRefreshing) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            libros = cachedBooks,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun resetGenreFilterForSearch() {
        _uiState.value = _uiState.value.copy(selectedGenero = "Todos")
    }

    fun ejecutarBusqueda() {
        val state = _uiState.value

        _uiState.value = state.copy(
            isLoading = true,
            isRefreshing = false,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = repository.searchBooks(
                genero = state.selectedGenero,
                query = state.searchQuery
            )

            result
                .onSuccess { libros ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        libros = libros,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error al buscar libros"
                    )
                }
        }
    }

    fun buscarPorIsbn(isbn: String) {
        val normalizedIsbn = isbn
            .trim()
            .uppercase()
            .removePrefix("ISBN")
            .replace(":", "")
            .replace("-", "")
            .replace(" ", "")

        if (normalizedIsbn.isBlank()) return

        val currentState = _uiState.value

        if (
            currentState.isIsbnLookupLoading &&
            currentState.scannedIsbn == normalizedIsbn
        ) {
            return
        }

        if (
            currentState.scannedBook != null &&
            currentState.scannedIsbn == normalizedIsbn
        ) {
            return
        }

        if (isbnCache.containsKey(normalizedIsbn)) {
            val cachedBook = isbnCache[normalizedIsbn]

            _uiState.value = currentState.copy(
                scannedIsbn = normalizedIsbn,
                scannedBook = cachedBook,
                scanHistory = cachedBook?.let { currentState.scanHistory.withScannedBook(it) }
                    ?: currentState.scanHistory,
                isIsbnLookupLoading = false,
                isbnLookupError = if (cachedBook == null) {
                    "No se encontró ningún libro para este ISBN"
                } else {
                    null
                }
            )

            return
        }

        isbnLookupJob?.cancel()

        _uiState.value = currentState.copy(
            scannedIsbn = normalizedIsbn,
            scannedBook = null,
            isIsbnLookupLoading = true,
            isbnLookupError = null,
            selectedGenero = "Todos"
        )

        isbnLookupJob = viewModelScope.launch {
            val result = repository.searchBooksByIsbn(normalizedIsbn)

            result
                .onSuccess { libros ->
                    val book = libros.firstOrNull()
                    isbnCache[normalizedIsbn] = book

                    _uiState.value = _uiState.value.copy(
                        isIsbnLookupLoading = false,
                        scannedIsbn = normalizedIsbn,
                        scannedBook = book,
                        scanHistory = book?.let { _uiState.value.scanHistory.withScannedBook(it) }
                            ?: _uiState.value.scanHistory,
                        isbnLookupError = if (book == null) {
                            "No se encontró ningún libro para este ISBN"
                        } else {
                            null
                        }
                    )
                }
                .onFailure { error ->
                    val message = error.message.orEmpty()

                    _uiState.value = _uiState.value.copy(
                        isIsbnLookupLoading = false,
                        scannedBook = null,
                        isbnLookupError = when {
                            message.contains("429", ignoreCase = true) ->
                                "Demasiadas búsquedas seguidas. Espera unos segundos y vuelve a intentarlo."

                            message.isNotBlank() -> message
                            else -> "Error al buscar el ISBN"
                        }
                    )
                }
        }
    }

    fun onGeneroSelected(genero: String) {
        _uiState.value = _uiState.value.copy(selectedGenero = genero)
        cargarGenero(genero)
    }

    fun applyInitialGenre(genero: String?) {
        if (genero.isNullOrBlank()) return
        _uiState.value = _uiState.value.copy(selectedGenero = genero)
        cargarGenero(genero)
    }

    fun clearGenreFilter() {
        _uiState.value = _uiState.value.copy(selectedGenero = "Todos")
        cargarLibros()
    }

    fun selectBook(libro: Libro) {
        _uiState.value = _uiState.value.copy(selectedBook = libro)
    }

    fun clearScannedBook() {
        isbnLookupJob?.cancel()

        _uiState.value = _uiState.value.copy(
            scannedBook = null,
            scannedIsbn = null,
            isIsbnLookupLoading = false,
            isbnLookupError = null
        )
    }

    fun getGenreCounts(): Map<String, Int> {
        return emptyMap()
    }

    private fun cargarGenero(genero: String) {
        if (genero == "Todos") {
            cargarLibros()
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isRefreshing = false,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = repository.getBooksByGenre(genero)

            result
                .onSuccess { libros ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        libros = libros,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error al cargar el género"
                    )
                }
        }
    }

    private fun List<Libro>.withScannedBook(book: Libro): List<Libro> {
        val key = book.id.ifBlank { book.isbn.ifBlank { book.titulo } }
        return listOf(book) + filter { existing ->
            val existingKey = existing.id.ifBlank { existing.isbn.ifBlank { existing.titulo } }
            existingKey != key
        }
    }
}

