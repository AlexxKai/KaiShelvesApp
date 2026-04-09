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

    fun cargarEstadoLectura(bookId: String) {
        if (bookId.isBlank()) return

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = repository.obtenerLibroLeido(bookId)

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

    fun refrescarLectura(bookId: String) {
        cargarEstadoLectura(bookId)
    }
}