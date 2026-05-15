package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.local.ReadingStatsLocalStore
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.LibroLeido
import com.example.kaishelvesapp.data.model.ReadingStatsSnapshot
import com.example.kaishelvesapp.data.repository.BookRepository
import com.example.kaishelvesapp.data.repository.UserListsRepository
import com.example.kaishelvesapp.data.statistics.ReadingStatsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReadingListUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val libros: List<LibroLeido> = emptyList(),
    val readListBooks: List<Libro> = emptyList(),
    val totalUniqueBooksInLists: Int = 0,
    val statsSnapshot: ReadingStatsSnapshot? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ReadingListViewModel(
    private val repository: BookRepository = BookRepository(),
    private val userListsRepository: UserListsRepository = UserListsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadingListUiState())
    val uiState: StateFlow<ReadingListUiState> = _uiState.asStateFlow()

    init {
        cargarEstadisticasCacheadas()
    }

    fun cargarLecturas() {
        cargarLecturas(showFullLoading = true)
    }

    fun refrescarLecturas() {
        cargarLecturas(showFullLoading = false)
    }

    private fun cargarLecturas(showFullLoading: Boolean) {
        val hasCachedStats = _uiState.value.statsSnapshot != null
        _uiState.value = _uiState.value.copy(
            isLoading = showFullLoading && !hasCachedStats,
            isRefreshing = !showFullLoading || hasCachedStats,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            val readsResult = repository.obtenerListaLecturas()
            val readListBooksResult = obtenerLibrosEnListaLeidos()
            val libros = readsResult.getOrDefault(emptyList())
            val readListBooks = readListBooksResult.getOrDefault(libros.map(::readBookToBook))
            val initialTotalBooksInLists = _uiState.value.statsSnapshot?.totalBooksInLists
                ?: libros.distinctBy(ReadingStatsCalculator::readBookIdentityKey).size
            val quickStats = buildStatsSnapshot(
                books = libros,
                readListBooks = readListBooks,
                totalUniqueBooksInLists = initialTotalBooksInLists
            )
            guardarEstadisticasCacheadas(quickStats)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                libros = libros,
                readListBooks = readListBooks,
                totalUniqueBooksInLists = quickStats.totalBooksInLists,
                statsSnapshot = quickStats,
                errorMessage = readsResult.exceptionOrNull()?.message
                    ?: readListBooksResult.exceptionOrNull()?.message
            )

            val totalUniqueBooksResult = obtenerTotalUnicoLibrosEnListas()
            val totalBooksInLists = totalUniqueBooksResult.getOrDefault(quickStats.totalBooksInLists)
            val updatedTotalStats = quickStats.copy(
                totalBooksInLists = totalBooksInLists,
                updatedAtMillis = System.currentTimeMillis()
            )
            guardarEstadisticasCacheadas(updatedTotalStats)
            _uiState.value = _uiState.value.copy(
                totalUniqueBooksInLists = totalBooksInLists,
                statsSnapshot = updatedTotalStats,
                errorMessage = _uiState.value.errorMessage
                    ?: totalUniqueBooksResult.exceptionOrNull()?.message
            )

            if (readListBooks.any { it.genero.isBlank() }) {
                actualizarGenerosCacheados(libros, readListBooks, totalBooksInLists)
            }
        }
    }

    private suspend fun actualizarGenerosCacheados(
        libros: List<LibroLeido>,
        readListBooks: List<Libro>,
        totalBooksInLists: Int
    ) {
        val enrichedReadListBooks = repository.completarGenerosFaltantes(readListBooks)
        val enrichedBooks = enrichedReadListBooks.getOrDefault(readListBooks)
        val enrichedStats = buildStatsSnapshot(
            books = libros,
            readListBooks = enrichedBooks,
            totalUniqueBooksInLists = totalBooksInLists
        )
        guardarEstadisticasCacheadas(enrichedStats)

        _uiState.value = _uiState.value.copy(
            readListBooks = enrichedBooks,
            statsSnapshot = enrichedStats,
            errorMessage = _uiState.value.errorMessage
                ?: enrichedReadListBooks.exceptionOrNull()?.message
        )
    }

    private fun cargarEstadisticasCacheadas() {
        val cachedStats = runCatching {
            ReadingStatsLocalStore.read(userListsRepository.currentLibraryOwnerId())
        }.getOrNull() ?: return
        _uiState.value = _uiState.value.copy(
            statsSnapshot = cachedStats,
            totalUniqueBooksInLists = cachedStats.totalBooksInLists
        )
    }

    private fun guardarEstadisticasCacheadas(snapshot: ReadingStatsSnapshot) {
        runCatching {
            ReadingStatsLocalStore.write(userListsRepository.currentLibraryOwnerId(), snapshot)
        }
    }

    private fun buildStatsSnapshot(
        books: List<LibroLeido>,
        readListBooks: List<Libro>,
        totalUniqueBooksInLists: Int
    ): ReadingStatsSnapshot {
        // La lógica queda aislada para poder verificar el flujo crítico de estadísticas con unit tests.
        return ReadingStatsCalculator.buildSnapshot(
            books = books,
            readListBooks = readListBooks,
            totalUniqueBooksInLists = totalUniqueBooksInLists
        )
    }

    private suspend fun obtenerLibrosEnListaLeidos(): Result<List<Libro>> {
        return userListsRepository.getBooksInList(UserListsRepository.SYSTEM_LIST_READ_ID)
    }

    private suspend fun obtenerTotalUnicoLibrosEnListas(): Result<Int> {
        return try {
            val lists = userListsRepository.getUserLists().getOrThrow()
            val books = lists.flatMap { list ->
                userListsRepository.getBooksInList(list.id).getOrThrow()
            }

            Result.success(books.distinctBy(ReadingStatsCalculator::bookIdentityKey).size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readBookToBook(book: LibroLeido): Libro {
        // Permite que Estadísticas use la colección de leídos aunque falle la lista del usuario.
        return Libro(
            id = book.id,
            isbn = book.isbn,
            titulo = book.titulo,
            autor = book.autor,
            editorial = book.editorial,
            genero = book.genero,
            fechaPublicacion = book.fechaPublicacion,
            paginas = book.paginas,
            imagen = book.imagen,
            pdf = book.pdf
        )
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
