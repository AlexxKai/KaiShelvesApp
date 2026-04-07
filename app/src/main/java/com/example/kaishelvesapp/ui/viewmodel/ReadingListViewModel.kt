package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.LibroLeido
import com.example.kaishelvesapp.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReadingListUiState(
    val isLoading: Boolean = false,
    val libros: List<LibroLeido> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ReadingListViewModel(
    private val repository: BookRepository = BookRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadingListUiState())
    val uiState: StateFlow<ReadingListUiState> = _uiState.asStateFlow()

    fun cargarLecturas() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val result = repository.obtenerListaLecturas()

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
                        errorMessage = error.message ?: "Error al cargar lecturas"
                    )
                }
        }
    }

    fun marcarComoLeido(libro: Libro, onSuccess: (() -> Unit)? = null) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val result = repository.marcarLibroComoLeido(libro)

            result
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Libro añadido a tus lecturas"
                    )
                    onSuccess?.invoke()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudo marcar como leído"
                    )
                }
        }
    }

    fun actualizarPuntuacion(isbn: String, puntuacion: Int) {
        viewModelScope.launch {
            val result = repository.actualizarPuntuacion(isbn, puntuacion)
            result
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Puntuación actualizada"
                    )
                    cargarLecturas()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "No se pudo actualizar la puntuación"
                    )
                }
        }
    }

    fun eliminarLibro(isbn: String) {
        viewModelScope.launch {
            val result = repository.eliminarLibroLeido(isbn)
            result
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Libro eliminado de tus lecturas"
                    )
                    cargarLecturas()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "No se pudo eliminar el libro"
                    )
                }
        }
    }

    fun limpiarMensajes() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}