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
    val libros: List<Libro> = emptyList(),
    val totalResults: Int = 0,
    val searchQuery: String = "",
    val submittedQuery: String = "",
    val sortMode: SearchResultsSortMode = SearchResultsSortMode.NEWEST,
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
            searchQuery = cleanQuery,
            submittedQuery = cleanQuery,
            sortMode = sortMode,
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
                        totalResults = searchResult.totalItems,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        libros = emptyList(),
                        totalResults = 0,
                        errorMessage = error.message ?: "Error al buscar libros"
                    )
                }
        }
    }
}
