package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.model.LibroLeido
import com.example.kaishelvesapp.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BookDetailUiState(
    val isLoading: Boolean = false,
    val isAlreadyRead: Boolean = false,
    val readBook: LibroLeido? = null,
    val errorMessage: String? = null
)

class BookDetailViewModel(
    private val repository: BookRepository = BookRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    fun cargarEstadoLectura(isbn: String) {
        if (isbn.isBlank()) return

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = repository.obtenerLibroLeido(isbn)

            result
                .onSuccess { libroLeido ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAlreadyRead = libroLeido != null,
                        readBook = libroLeido,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudo comprobar el estado del libro"
                    )
                }
        }
    }

    fun marcarComoLeidoLocalmente(
        libroLeido: LibroLeido? = null
    ) {
        _uiState.value = _uiState.value.copy(
            isAlreadyRead = true,
            readBook = libroLeido ?: _uiState.value.readBook
        )
    }

    fun refrescarLectura(isbn: String) {
        cargarEstadoLectura(isbn)
    }
}