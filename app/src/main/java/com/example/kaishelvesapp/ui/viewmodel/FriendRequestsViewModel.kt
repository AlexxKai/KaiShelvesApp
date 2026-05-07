package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.repository.ActivityComment
import com.example.kaishelvesapp.data.model.Usuario
import com.example.kaishelvesapp.data.repository.ActivityNotificationItem
import com.example.kaishelvesapp.data.repository.ActivitySocialSummary
import com.example.kaishelvesapp.data.repository.FriendsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FriendRequestsUiState(
    val isLoading: Boolean = false,
    val isLoadingNotifications: Boolean = false,
    val receivedRequests: List<Usuario> = emptyList(),
    val notifications: List<ActivityNotificationItem> = emptyList(),
    val commentsByActivityId: Map<String, List<ActivityComment>> = emptyMap(),
    val loadingCommentIds: Set<String> = emptySet(),
    val socialActionIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val pendingCount: Int
        get() = receivedRequests.size
    val unreadNotifications: List<ActivityNotificationItem>
        get() = notifications.filterNot { it.isRead }
    val readNotifications: List<ActivityNotificationItem>
        get() = notifications.filter { it.isRead }
}

class FriendRequestsViewModel(
    private val repository: FriendsRepository = FriendsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendRequestsUiState())
    val uiState: StateFlow<FriendRequestsUiState> = _uiState.asStateFlow()

    init {
        loadReceivedRequests()
        loadActivityNotifications()
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

    fun loadActivityNotifications() {
        _uiState.value = _uiState.value.copy(
            isLoadingNotifications = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.loadActivityNotifications()
                .onSuccess { notifications ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingNotifications = false,
                        notifications = notifications,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingNotifications = false,
                        errorMessage = error.message ?: "No se pudieron cargar las notificaciones"
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

    fun toggleLike(activityId: String) {
        if (activityId.isBlank() || activityId in _uiState.value.socialActionIds) return

        _uiState.value = _uiState.value.copy(
            socialActionIds = _uiState.value.socialActionIds + activityId,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.toggleActivityLike(activityId)
                .onSuccess { summary -> updateNotificationActivitySocial(activityId, summary) }
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
                    updateNotificationActivitySocial(activityId, summary)
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

    fun markNotificationAsRead(notificationId: String) {
        if (notificationId.isBlank()) return
        val currentNotifications = _uiState.value.notifications
        val notification = currentNotifications.firstOrNull { it.id == notificationId } ?: return
        if (notification.isRead) return

        _uiState.value = _uiState.value.copy(
            notifications = currentNotifications.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
        )

        viewModelScope.launch {
            repository.markActivityNotificationRead(notificationId)
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        notifications = _uiState.value.notifications.map {
                            if (it.id == notificationId) it.copy(isRead = false) else it
                        },
                        errorMessage = error.message ?: "No se pudo marcar la notificacion como leida"
                    )
                }
        }
    }

    private fun updateNotificationActivitySocial(activityId: String, summary: ActivitySocialSummary) {
        _uiState.value = _uiState.value.copy(
            notifications = _uiState.value.notifications.map { notification ->
                if (notification.activityId == activityId) {
                    notification.copy(
                        activity = notification.activity.copy(social = summary)
                    )
                } else {
                    notification
                }
            }
        )
    }
}
