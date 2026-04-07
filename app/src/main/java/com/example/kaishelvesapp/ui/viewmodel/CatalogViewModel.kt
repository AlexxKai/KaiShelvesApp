package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CatalogUiState(
    val isLoading: Boolean = false,
    val libros: List<Libro> = emptyList(),
    val allBooks: List<Libro> = emptyList(),
    val generos: List<String> = listOf("Todos"),
    val selectedGenero: String = "Todos",
    val searchQuery: String = "",
    val selectedBook: Libro? = null,
    val errorMessage: String? = null
)

class CatalogViewModel(
    private val repository: BookRepository = BookRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        cargarLibros()
    }

    fun cargarLibros() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = repository.obtenerLibros()

            result
                .onSuccess { libros ->
                    val generos = libros
                        .map { it.genero }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        libros = libros,
                        allBooks = libros,
                        generos = listOf("Todos") + generos,
                        selectedGenero = "Todos",
                        searchQuery = "",
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error al cargar libros"
                    )
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        filtrarLibros()
    }

    fun onGeneroSelected(genero: String) {
        _uiState.value = _uiState.value.copy(selectedGenero = genero)
        filtrarLibros()
    }

    fun selectBook(libro: Libro) {
        _uiState.value = _uiState.value.copy(selectedBook = libro)
    }

    private fun filtrarLibros() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()

        val filtrados = state.allBooks.filter { libro ->
            val coincideGenero = state.selectedGenero == "Todos" || libro.genero == state.selectedGenero

            val coincideTexto = query.isBlank() ||
                    libro.titulo.lowercase().contains(query) ||
                    libro.autor.lowercase().contains(query) ||
                    libro.editorial.lowercase().contains(query)

            coincideGenero && coincideTexto
        }

        _uiState.value = state.copy(libros = filtrados)
    }
}