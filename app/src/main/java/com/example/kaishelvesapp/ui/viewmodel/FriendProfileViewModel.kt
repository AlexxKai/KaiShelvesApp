package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.repository.FriendProfileData
import com.example.kaishelvesapp.data.repository.FriendsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FriendProfileUiState(
    val isLoading: Boolean = false,
    val isRemovingFriend: Boolean = false,
    val isSendingRequest: Boolean = false,
    val profile: FriendProfileData? = null,
    val errorMessage: String? = null
)

class FriendProfileViewModel(
    private val repository: FriendsRepository = FriendsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendProfileUiState())
    val uiState: StateFlow<FriendProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(friendUid: String) {
        if (friendUid.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "No se pudo identificar al amigo"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isRemovingFriend = false,
            isSendingRequest = false,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.loadFriendProfile(friendUid)
                .onSuccess { profile ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRemovingFriend = false,
                        isSendingRequest = false,
                        profile = profile,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRemovingFriend = false,
                        isSendingRequest = false,
                        errorMessage = error.message ?: "No se pudo cargar el perfil del amigo"
                    )
                }
        }
    }

    fun removeFriend(friendUid: String, onSuccess: () -> Unit = {}) {
        if (friendUid.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "No se pudo identificar al amigo"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isRemovingFriend = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.removeFriend(friendUid)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isRemovingFriend = false,
                        errorMessage = null
                    )
                    onSuccess()
                    loadProfile(friendUid)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isRemovingFriend = false,
                        errorMessage = error.message ?: "No se pudo eliminar la amistad"
                    )
                }
        }
    }

    fun sendFriendRequest(onSuccess: () -> Unit = {}) {
        val profile = _uiState.value.profile ?: return
        if (profile.isFriend || profile.isRequestSent) return

        _uiState.value = _uiState.value.copy(
            isSendingRequest = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.sendFriendRequest(profile.user)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSendingRequest = false,
                        errorMessage = null
                    )
                    onSuccess()
                    loadProfile(profile.user.uid)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSendingRequest = false,
                        errorMessage = error.message ?: "No se pudo enviar la solicitud"
                    )
                }
        }
    }
}
