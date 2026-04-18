package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.repository.FriendActivityItem
import com.example.kaishelvesapp.data.repository.FriendsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val activities: List<FriendActivityItem> = emptyList(),
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: FriendsRepository = FriendsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadFeed() {
        fetchFeed(isRefresh = false)
    }

    fun refreshFeed() {
        fetchFeed(isRefresh = true)
    }

    private fun fetchFeed(isRefresh: Boolean) {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isRefreshing) return

        _uiState.value = currentState.copy(
            isLoading = !isRefresh && currentState.activities.isEmpty(),
            isRefreshing = isRefresh,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.loadHomeFeed()
                .onSuccess { activities ->
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        activities = activities,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message ?: "No se pudo cargar la actividad reciente"
                    )
                }
        }
    }
}
