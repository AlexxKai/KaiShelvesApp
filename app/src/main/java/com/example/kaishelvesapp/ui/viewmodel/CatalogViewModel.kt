package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.remote.googlebooks.LibraryGenres
import com.example.kaishelvesapp.data.repository.BookRepository
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
    val selectedBook: Libro? = null,
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

    init {
        cargarLibros()
    }

    fun cargarLibros(refresh: Boolean = false) {
        val currentState = _uiState.value
        _uiState.value = _uiState.value.copy(
            isLoading = !refresh || currentState.libros.isEmpty(),
            isRefreshing = refresh && currentState.libros.isNotEmpty(),
            errorMessage = null
        )

        viewModelScope.launch {
            val result = repository.obtenerLibros()

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
            .replace("-", "")
            .replace(" ", "")

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isRefreshing = false,
            errorMessage = null,
            searchQuery = normalizedIsbn,
            selectedGenero = "Todos"
        )

        viewModelScope.launch {
            val result = repository.searchBooksByIsbn(normalizedIsbn)

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
                        errorMessage = error.message ?: "Error al buscar el ISBN"
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
}
