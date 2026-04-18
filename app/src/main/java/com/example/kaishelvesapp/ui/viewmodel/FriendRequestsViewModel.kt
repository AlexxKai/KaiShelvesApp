package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.model.Usuario
import com.example.kaishelvesapp.data.repository.FriendsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FriendRequestsUiState(
    val isLoading: Boolean = false,
    val receivedRequests: List<Usuario> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val pendingCount: Int
        get() = receivedRequests.size
}

class FriendRequestsViewModel(
    private val repository: FriendsRepository = FriendsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendRequestsUiState())
    val uiState: StateFlow<FriendRequestsUiState> = _uiState.asStateFlow()

    init {
        loadReceivedRequests()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun loadReceivedRequests() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.loadReceivedFriendRequests()
                .onSuccess { data ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        receivedRequests = data.receivedRequests,
                        errorMessage = null,
                        successMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudieron cargar las solicitudes"
                    )
                }
        }
    }

    fun acceptRequest(user: Usuario, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.acceptFriendRequest(user)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        receivedRequests = _uiState.value.receivedRequests.filterNot { it.uid == user.uid },
                        successMessage = "Solicitud aceptada",
                        errorMessage = null
                    )
                    onSuccess?.invoke()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "No se pudo aceptar la solicitud",
                        successMessage = null
                    )
                }
        }
    }

    fun rejectRequest(user: Usuario, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.rejectFriendRequest(user)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        receivedRequests = _uiState.value.receivedRequests.filterNot { it.uid == user.uid },
                        successMessage = "Solicitud rechazada",
                        errorMessage = null
                    )
                    onSuccess?.invoke()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "No se pudo rechazar la solicitud",
                        successMessage = null
                    )
                }
        }
    }
}
