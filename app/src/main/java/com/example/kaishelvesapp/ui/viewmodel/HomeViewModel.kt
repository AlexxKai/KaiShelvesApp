package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.repository.ActivityComment
import com.example.kaishelvesapp.data.repository.ActivitySocialSummary
import com.example.kaishelvesapp.data.repository.FriendActivityItem
import com.example.kaishelvesapp.data.repository.FriendsRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isCheckingForUpdates: Boolean = false,
    val activities: List<FriendActivityItem> = emptyList(),
    val commentsByActivityId: Map<String, List<ActivityComment>> = emptyMap(),
    val loadingCommentIds: Set<String> = emptySet(),
    val socialActionIds: Set<String> = emptySet(),
    val errorMessageRes: Int? = null,
    val isOfflineError: Boolean = false
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

    fun checkOnlineAccess(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            repository.loadHomeFeed()
                .onSuccess { activities ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isCheckingForUpdates = false,
                        activities = activities,
                        errorMessageRes = null,
                        isOfflineError = false
                    )
                    onResult(true)
                }
                .onFailure { error ->
                    val isOffline = error.isOfflineFailure()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isCheckingForUpdates = false,
                        errorMessageRes = if (isOffline) {
                            R.string.home_offline_dialog_body
                        } else {
                            R.string.home_recent_activity_load_error
                        },
                        isOfflineError = isOffline
                    )
                    onResult(!isOffline)
                }
        }
    }

    private fun fetchFeed(isRefresh: Boolean) {
        hydrateCachedFeed()
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isRefreshing || currentState.isCheckingForUpdates) return

        val isCheckingForUpdates = !isRefresh && currentState.activities.isNotEmpty()

        _uiState.value = currentState.copy(
            isLoading = !isRefresh && currentState.activities.isEmpty(),
            isRefreshing = isRefresh,
            isCheckingForUpdates = isCheckingForUpdates,
            errorMessageRes = null,
            isOfflineError = currentState.isOfflineError
        )

        viewModelScope.launch {
            repository.loadHomeFeed()
                .onSuccess { activities ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isCheckingForUpdates = false,
                        activities = activities,
                        errorMessageRes = null,
                        isOfflineError = false
                    )
                }
                .onFailure { error ->
                    val isOffline = error.isOfflineFailure()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isCheckingForUpdates = false,
                        errorMessageRes = if (isOffline) {
                            R.string.home_offline_dialog_body
                        } else {
                            R.string.home_recent_activity_load_error
                        },
                        isOfflineError = isOffline
                    )
                }
        }
    }

    private fun hydrateCachedFeed() {
        if (_uiState.value.activities.isNotEmpty()) return
        val cachedActivities = repository.cachedHomeFeed()
        if (cachedActivities.isEmpty()) return

        _uiState.value = _uiState.value.copy(
            activities = cachedActivities,
            isLoading = false,
            errorMessageRes = null,
            isOfflineError = false
        )
    }

    fun toggleLike(activityId: String) {
        if (activityId.isBlank() || activityId in _uiState.value.socialActionIds) return

        _uiState.value = _uiState.value.copy(
            socialActionIds = _uiState.value.socialActionIds + activityId,
            errorMessageRes = null,
            isOfflineError = false
        )

        viewModelScope.launch {
            repository.toggleActivityLike(activityId)
                .onSuccess { summary -> updateActivitySocial(activityId, summary) }
                .onFailure { error ->
                    val isOffline = error.isOfflineFailure()
                    _uiState.value = _uiState.value.copy(
                        errorMessageRes = if (isOffline) {
                            R.string.home_offline_dialog_body
                        } else {
                            R.string.home_like_update_error
                        },
                        isOfflineError = isOffline
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
            errorMessageRes = null,
            isOfflineError = false
        )

        viewModelScope.launch {
            repository.loadActivityComments(activityId)
                .onSuccess { comments ->
                    _uiState.value = _uiState.value.copy(
                        commentsByActivityId = _uiState.value.commentsByActivityId + (activityId to comments)
                    )
                }
                .onFailure { error ->
                    val isOffline = error.isOfflineFailure()
                    _uiState.value = _uiState.value.copy(
                        errorMessageRes = if (isOffline) {
                            R.string.home_offline_dialog_body
                        } else {
                            R.string.home_comments_load_error
                        },
                        isOfflineError = isOffline
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
            errorMessageRes = null,
            isOfflineError = false
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
                    val isOffline = error.isOfflineFailure()
                    _uiState.value = _uiState.value.copy(
                        errorMessageRes = if (isOffline) {
                            R.string.home_offline_dialog_body
                        } else {
                            R.string.home_comment_publish_error
                        },
                        isOfflineError = isOffline
                    )
                }

            _uiState.value = _uiState.value.copy(
                socialActionIds = _uiState.value.socialActionIds - activityId
            )
        }
    }

    private fun updateActivitySocial(activityId: String, summary: ActivitySocialSummary) {
        _uiState.value = _uiState.value.copy(
            activities = _uiState.value.activities.map { item ->
                if (item.id == activityId) item.copy(social = summary) else item
            }
        )
    }

    private fun Throwable.isOfflineFailure(): Boolean {
        if (this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.UNAVAILABLE) {
            return true
        }

        // Firestore devuelve este texto cuando intenta leer datos remotos sin conexión.
        val diagnosticText = listOfNotNull(message, cause?.message)
            .joinToString(separator = " ")
            .lowercase(Locale.ROOT)

        return diagnosticText.contains("client is offline") ||
            diagnosticText.contains("offline") ||
            diagnosticText.contains("unavailable")
    }
}
