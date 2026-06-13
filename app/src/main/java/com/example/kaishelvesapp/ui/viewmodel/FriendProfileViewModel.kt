package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.repository.ActivityComment
import com.example.kaishelvesapp.data.repository.ActivitySocialSummary
import com.example.kaishelvesapp.data.repository.AccountReport
import com.example.kaishelvesapp.data.repository.BlockedMember
import com.example.kaishelvesapp.data.repository.FriendProfileData
import com.example.kaishelvesapp.data.repository.FriendsRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FriendProfileUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isRemovingFriend: Boolean = false,
    val isSendingRequest: Boolean = false,
    val isRespondingRequest: Boolean = false,
    val profile: FriendProfileData? = null,
    val commentsByActivityId: Map<String, List<ActivityComment>> = emptyMap(),
    val loadingCommentIds: Set<String> = emptySet(),
    val socialActionIds: Set<String> = emptySet(),
    val deletingActivityIds: Set<String> = emptySet(),
    val blockedMembers: List<BlockedMember> = emptyList(),
    val accountReports: List<AccountReport> = emptyList(),
    val isBlockingMember: Boolean = false,
    val isLoadingBlockedMembers: Boolean = false,
    val isLoadingReports: Boolean = false,
    val isSubmittingReport: Boolean = false,
    val isSavingReportReply: Boolean = false,
    val errorMessage: String? = null
)

class FriendProfileViewModel(
    private val repository: FriendsRepository = FriendsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendProfileUiState())
    val uiState: StateFlow<FriendProfileUiState> = _uiState.asStateFlow()
    private var myReportsListener: ListenerRegistration? = null

    override fun onCleared() {
        myReportsListener?.remove()
        myReportsListener = null
        super.onCleared()
    }

    fun loadProfile(friendUid: String, refresh: Boolean = false) {
        if (friendUid.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                isRespondingRequest = false,
                errorMessage = "No se pudo identificar al amigo"
            )
            return
        }

        val currentState = _uiState.value
        val canRefreshInPlace = refresh && currentState.profile != null
        _uiState.value = _uiState.value.copy(
            isLoading = !canRefreshInPlace,
            isRefreshing = canRefreshInPlace,
            isRemovingFriend = false,
            isSendingRequest = false,
            isRespondingRequest = false,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.loadFriendProfile(friendUid)
                .onSuccess { profile ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isRemovingFriend = false,
                        isSendingRequest = false,
                        isRespondingRequest = false,
                        profile = profile,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isRemovingFriend = false,
                        isSendingRequest = false,
                        isRespondingRequest = false,
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
        if (profile.isFriend || profile.isRequestSent || profile.isRequestReceived) return

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

    fun cancelSentFriendRequest(onSuccess: () -> Unit = {}) {
        val profile = _uiState.value.profile ?: return
        if (profile.isFriend || !profile.isRequestSent) return

        _uiState.value = _uiState.value.copy(
            isSendingRequest = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.cancelSentFriendRequest(profile.user)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSendingRequest = false,
                        profile = profile.copy(isRequestSent = false),
                        errorMessage = null
                    )
                    onSuccess()
                    loadProfile(profile.user.uid)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSendingRequest = false,
                        errorMessage = error.message ?: "No se pudo cancelar la solicitud"
                    )
                }
        }
    }

    fun acceptFriendRequest(onSuccess: () -> Unit = {}) {
        val profile = _uiState.value.profile ?: return
        if (profile.isFriend || !profile.isRequestReceived || _uiState.value.isRespondingRequest) return

        _uiState.value = _uiState.value.copy(
            isRespondingRequest = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.acceptFriendRequest(profile.user)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isRespondingRequest = false,
                        errorMessage = null
                    )
                    onSuccess()
                    loadProfile(profile.user.uid)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isRespondingRequest = false,
                        errorMessage = error.message ?: "No se pudo aceptar la solicitud"
                    )
                }
        }
    }

    fun rejectFriendRequest(onSuccess: () -> Unit = {}) {
        val profile = _uiState.value.profile ?: return
        if (profile.isFriend || !profile.isRequestReceived || _uiState.value.isRespondingRequest) return

        _uiState.value = _uiState.value.copy(
            isRespondingRequest = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.rejectFriendRequest(profile.user)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isRespondingRequest = false,
                        profile = profile.copy(isRequestReceived = false),
                        errorMessage = null
                    )
                    onSuccess()
                    loadProfile(profile.user.uid)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isRespondingRequest = false,
                        errorMessage = error.message ?: "No se pudo cancelar la solicitud"
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

    fun toggleCommentLike(activityId: String, commentId: String) {
        val actionId = "$activityId:$commentId:comment_like"
        if (activityId.isBlank() || commentId.isBlank() || actionId in _uiState.value.socialActionIds) return

        _uiState.value = _uiState.value.copy(
            socialActionIds = _uiState.value.socialActionIds + actionId,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.toggleActivityCommentLike(activityId, commentId)
                .onSuccess { comments ->
                    _uiState.value = _uiState.value.copy(
                        commentsByActivityId = _uiState.value.commentsByActivityId + (activityId to comments)
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "No se pudo actualizar el me gusta del comentario"
                    )
                }

            _uiState.value = _uiState.value.copy(
                socialActionIds = _uiState.value.socialActionIds - actionId
            )
        }
    }

    fun replyToComment(activityId: String, commentId: String, text: String) {
        val actionId = "$activityId:$commentId:comment_reply"
        if (activityId.isBlank() || commentId.isBlank() || text.isBlank() || actionId in _uiState.value.socialActionIds) return

        _uiState.value = _uiState.value.copy(
            socialActionIds = _uiState.value.socialActionIds + actionId,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.addActivityCommentReply(activityId, commentId, text)
                .onSuccess { comments ->
                    _uiState.value = _uiState.value.copy(
                        commentsByActivityId = _uiState.value.commentsByActivityId + (activityId to comments)
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "No se pudo publicar la respuesta"
                    )
                }

            _uiState.value = _uiState.value.copy(
                socialActionIds = _uiState.value.socialActionIds - actionId
            )
        }
    }

    fun blockCurrentProfile(onSuccess: () -> Unit = {}) {
        val profile = _uiState.value.profile ?: return
        if (profile.user.uid.isBlank() || _uiState.value.isBlockingMember) return

        _uiState.value = _uiState.value.copy(
            isBlockingMember = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.blockMember(profile.user.uid)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isBlockingMember = false,
                        profile = null,
                        errorMessage = null
                    )
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isBlockingMember = false,
                        errorMessage = error.message ?: "No se pudo bloquear este perfil"
                    )
                }
        }
    }

    fun submitReport(
        subject: String,
        message: String,
        photoUris: List<String>,
        onSuccess: () -> Unit = {}
    ) {
        val profile = _uiState.value.profile ?: return
        if (_uiState.value.isSubmittingReport) return

        _uiState.value = _uiState.value.copy(
            isSubmittingReport = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.reportMember(
                targetUid = profile.user.uid,
                subject = subject,
                message = message,
                photoUris = photoUris
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingReport = false,
                        errorMessage = null
                    )
                    onSuccess()
                    loadMyReports()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSubmittingReport = false,
                        errorMessage = error.message ?: "No se pudo enviar la denuncia"
                    )
                }
        }
    }

    fun loadBlockedMembers() {
        _uiState.value = _uiState.value.copy(
            isLoadingBlockedMembers = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.loadBlockedMembers()
                .onSuccess { members ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingBlockedMembers = false,
                        blockedMembers = members,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingBlockedMembers = false,
                        errorMessage = error.message ?: "No se pudieron cargar las personas bloqueadas"
                    )
                }
        }
    }

    fun unblockMember(uid: String) {
        if (uid.isBlank()) return

        viewModelScope.launch {
            repository.unblockMember(uid)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        blockedMembers = _uiState.value.blockedMembers.filterNot { it.user.uid == uid },
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "No se pudo desbloquear este perfil"
                    )
                }
        }
    }

    fun loadMyReports() {
        _uiState.value = _uiState.value.copy(
            isLoadingReports = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.loadMyReports()
                .onSuccess { reports ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingReports = false,
                        accountReports = reports,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingReports = false,
                        errorMessage = error.message ?: "No se pudieron cargar las denuncias"
                    )
                }
        }
    }

    fun observeMyReports() {
        myReportsListener?.remove()
        _uiState.value = _uiState.value.copy(
            isLoadingReports = _uiState.value.accountReports.isEmpty(),
            errorMessage = null
        )
        myReportsListener = repository.observeMyReports { result ->
            result
                .onSuccess { reports ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingReports = false,
                        accountReports = reports,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingReports = false,
                        errorMessage = error.message ?: "No se pudieron escuchar las denuncias"
                    )
                }
        }
    }

    fun replyToReportReview(reportId: String, reply: String, imageUris: List<String> = emptyList()) {
        if (_uiState.value.isSavingReportReply) return

        _uiState.value = _uiState.value.copy(
            isSavingReportReply = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.replyToReportReview(reportId, reply, imageUris)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSavingReportReply = false,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSavingReportReply = false,
                        errorMessage = error.message ?: "No se pudo enviar la respuesta"
                    )
                }
        }
    }

    fun hideActivityUpdate(activityId: String) {
        if (activityId.isBlank() || activityId in _uiState.value.deletingActivityIds) return

        _uiState.value = _uiState.value.copy(
            deletingActivityIds = _uiState.value.deletingActivityIds + activityId,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.hideActivityUpdate(activityId)
                .onSuccess {
                    val profile = _uiState.value.profile
                    _uiState.value = _uiState.value.copy(
                        profile = profile?.copy(
                            updates = profile.updates.filterNot { item -> item.id == activityId }
                        ),
                        commentsByActivityId = _uiState.value.commentsByActivityId - activityId,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "No se pudo eliminar la actualización"
                    )
                }

            _uiState.value = _uiState.value.copy(
                deletingActivityIds = _uiState.value.deletingActivityIds - activityId
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
