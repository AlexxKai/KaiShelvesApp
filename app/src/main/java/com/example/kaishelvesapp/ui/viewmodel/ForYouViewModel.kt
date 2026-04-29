package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.repository.BookRecommendation
import com.example.kaishelvesapp.data.repository.ForYouRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ForYouUiState(
    val isLoading: Boolean = false,
    val recommendations: List<BookRecommendation> = emptyList(),
    val errorMessage: String? = null
)

class ForYouViewModel(
    private val repository: ForYouRepository = ForYouRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForYouUiState())
    val uiState: StateFlow<ForYouUiState> = _uiState.asStateFlow()

    fun loadRecommendations(personalizedSuggestionsEnabled: Boolean) {
        if (!personalizedSuggestionsEnabled) {
            _uiState.value = ForYouUiState()
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.loadRecommendations()
                .onSuccess { recommendations ->
                    _uiState.value = ForYouUiState(
                        isLoading = false,
                        recommendations = recommendations
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudieron cargar las recomendaciones"
                    )
                }
        }
    }
}
