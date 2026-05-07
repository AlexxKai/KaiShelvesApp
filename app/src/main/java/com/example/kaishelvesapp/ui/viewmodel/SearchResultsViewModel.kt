package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.repository.BookRepository
import com.example.kaishelvesapp.data.repository.BookRepository.BookSearchSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SearchResultsSortMode {
    NEWEST,
    RATING
}

data class SearchResultsUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val libros: List<Libro> = emptyList(),
    val totalResults: Int = 0,
    val searchQuery: String = "",
    val submittedQuery: String = "",
    val sortMode: SearchResultsSortMode = SearchResultsSortMode.NEWEST,
    val nextStartIndex: Int = SEARCH_RESULTS_PAGE_SIZE,
    val canLoadMore: Boolean = false,
    val errorMessage: String? = null
)

class SearchResultsViewModel(
    private val repository: BookRepository = BookRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchResultsUiState())
    val uiState: StateFlow<SearchResultsUiState> = _uiState.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun clearSearchQuery() {
        _uiState.value = _uiState.value.copy(searchQuery = "")
    }

    fun onSortModeChange(sortMode: SearchResultsSortMode) {
        if (_uiState.value.sortMode == sortMode) return

        val currentQuery = _uiState.value.submittedQuery.ifBlank { _uiState.value.searchQuery }
        _uiState.value = _uiState.value.copy(sortMode = sortMode)
        if (currentQuery.isNotBlank()) {
            search(currentQuery, sortMode)
        }
    }

    fun search(
        query: String = _uiState.value.searchQuery,
        sortMode: SearchResultsSortMode = _uiState.value.sortMode
    ) {
        val cleanQuery = query.trim()
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isLoadingMore = false,
            libros = emptyList(),
            totalResults = 0,
            searchQuery = cleanQuery,
            submittedQuery = cleanQuery,
            sortMode = sortMode,
            nextStartIndex = SEARCH_RESULTS_PAGE_SIZE,
            canLoadMore = false,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = repository.searchBooksForResults(
                query = cleanQuery,
                sort = when (sortMode) {
                    SearchResultsSortMode.NEWEST -> BookSearchSort.NEWEST
                    SearchResultsSortMode.RATING -> BookSearchSort.RATING
                }
            )

            result
                .onSuccess { searchResult ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        libros = searchResult.books,
                        totalResults = searchResult.books.size,
                        nextStartIndex = SEARCH_RESULTS_PAGE_SIZE,
                        canLoadMore = searchResult.hasMore && searchResult.books.isNotEmpty(),
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        libros = emptyList(),
                        totalResults = 0,
                        nextStartIndex = SEARCH_RESULTS_PAGE_SIZE,
                        canLoadMore = false,
                        errorMessage = error.message ?: "Error al buscar libros"
                    )
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (
            state.isLoading ||
            state.isLoadingMore ||
            !state.canLoadMore ||
            state.submittedQuery.isBlank()
        ) {
            return
        }

        _uiState.value = state.copy(
            isLoadingMore = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = repository.searchBooksForResults(
                query = state.submittedQuery,
                sort = when (state.sortMode) {
                    SearchResultsSortMode.NEWEST -> BookSearchSort.NEWEST
                    SearchResultsSortMode.RATING -> BookSearchSort.RATING
                },
                startIndex = state.nextStartIndex,
                maxResults = SEARCH_RESULTS_PAGE_SIZE
            )

            result
                .onSuccess { searchResult ->
                    val currentState = _uiState.value
                    if (
                        currentState.submittedQuery != state.submittedQuery ||
                        currentState.sortMode != state.sortMode
                    ) {
                        return@onSuccess
                    }

                    val mergedBooks = (state.libros + searchResult.books).distinctBy(::searchResultKey)
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        libros = mergedBooks,
                        totalResults = mergedBooks.size,
                        nextStartIndex = state.nextStartIndex + SEARCH_RESULTS_PAGE_SIZE,
                        canLoadMore = searchResult.hasMore && searchResult.books.isNotEmpty(),
                        errorMessage = null
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        canLoadMore = false,
                        errorMessage = null
                    )
                }
        }
    }

    private fun searchResultKey(book: Libro): String {
        val title = book.titulo.trim().lowercase()
        val author = book.autor.trim().lowercase()
        return if (title.isNotBlank() && author.isNotBlank()) {
            "$title-$author"
        } else {
            book.id.ifBlank { book.isbn.ifBlank { "$title-$author" } }
        }
    }
}

private const val SEARCH_RESULTS_PAGE_SIZE = 40
