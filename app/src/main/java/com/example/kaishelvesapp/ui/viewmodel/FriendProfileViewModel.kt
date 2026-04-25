package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.repository.ActivityComment
import com.example.kaishelvesapp.data.repository.ActivitySocialSummary
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
    val commentsByActivityId: Map<String, List<ActivityComment>> = emptyMap(),
    val loadingCommentIds: Set<String> = emptySet(),
    val socialActionIds: Set<String> = emptySet(),
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

    fun toggleLike(activityId: String) {
        if (activityId.isBlank() || activityId in _uiState.value.socialActionIds) return

        _uiState.value = _uiState.value.copy(
            socialActionIds = _uiState.value.socialActionIds + activityId,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.toggleActivityLike(activityId)
                .onSuccess { summary -> updateActivitySocial(activityId, summary) }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "No se pudo actualizar el me gusta"
                    )
                }

            _uiState.value = _uiState.value.copy(
                socialActionIds = _uiState.value.socialActionIds - activityId
            )
        }
    }

    fun loadComments(activityId: String) {
        if (activityId.isBlank() || activityId in _uiState.value.loadingCommentIds) return

        _uiState.value = _uiState.value.copy(
            loadingCommentIds = _uiState.value.loadingCommentIds + activityId,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.loadActivityComments(activityId)
                .onSuccess { comments ->
                    _uiState.value = _uiState.value.copy(
                        commentsByActivityId = _uiState.value.commentsByActivityId + (activityId to comments)
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "No se pudieron cargar los comentarios"
                    )
                }

            _uiState.value = _uiState.value.copy(
                loadingCommentIds = _uiState.value.loadingCommentIds - activityId
            )
        }
    }

    fun addComment(activityId: String, text: String) {
        if (activityId.isBlank() || text.isBlank() || activityId in _uiState.value.socialActionIds) return

        _uiState.value = _uiState.value.copy(
            socialActionIds = _uiState.value.socialActionIds + activityId,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.addActivityComment(activityId, text)
                .onSuccess { (summary, comments) ->
                    updateActivitySocial(activityId, summary)
                    _uiState.value = _uiState.value.copy(
                        commentsByActivityId = _uiState.value.commentsByActivityId + (activityId to comments)
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "No se pudo publicar el comentario"
                    )
                }

            _uiState.value = _uiState.value.copy(
                socialActionIds = _uiState.value.socialActionIds - activityId
            )
        }
    }

    private fun updateActivitySocial(activityId: String, summary: ActivitySocialSummary) {
        val profile = _uiState.value.profile ?: return
        _uiState.value = _uiState.value.copy(
            profile = profile.copy(
                updates = profile.updates.map { item ->
                    if (item.id == activityId) item.copy(social = summary) else item
                }
            )
        )
    }
}
