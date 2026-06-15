package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.repository.AccountReport
import com.example.kaishelvesapp.data.repository.AccountReportKind
import com.example.kaishelvesapp.data.repository.AccountReportStatus
import com.example.kaishelvesapp.data.repository.FriendsRepository
import com.example.kaishelvesapp.data.repository.REPORT_SENDER_ADMIN
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AdminReportFilter {
    ALL,
    NEW,
    IN_PROGRESS,
    PROCESSED,
    CLOSED,
    WAITING_USER_INFO
}

data class AdminReportsUiState(
    val reports: List<AccountReport> = emptyList(),
    val selectedReport: AccountReport? = null,
    val reportKind: AccountReportKind = AccountReportKind.REPORT,
    val selectedStatus: AccountReportStatus = AccountReportStatus.NEW,
    val filter: AdminReportFilter = AdminReportFilter.ALL,
    val adminReplyDraft: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AdminReportsViewModel(
    private val repository: FriendsRepository = FriendsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminReportsUiState())
    val uiState: StateFlow<AdminReportsUiState> = _uiState.asStateFlow()
    private var reportsListener: ListenerRegistration? = null

    init {
        observeReports()
    }

    override fun onCleared() {
        reportsListener?.remove()
        reportsListener = null
        super.onCleared()
    }

    fun observeReports() {
        reportsListener?.remove()
        _uiState.value = _uiState.value.copy(isLoading = _uiState.value.reports.isEmpty())
        reportsListener = repository.observeAdminReports { result ->
            result
                .onSuccess { reports ->
                    val selectedId = _uiState.value.selectedReport?.id
                    val selectedReport = reports.firstOrNull { it.id == selectedId }
                    _uiState.value = _uiState.value.copy(
                        reports = reports,
                        selectedReport = selectedReport,
                        selectedStatus = selectedReport?.status ?: _uiState.value.selectedStatus,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudieron escuchar las denuncias"
                    )
                }
        }
    }

    fun setReportKind(kind: AccountReportKind) {
        _uiState.value = _uiState.value.copy(
            reportKind = kind,
            selectedReport = _uiState.value.selectedReport?.takeIf { it.kind == kind }
        )
    }

    fun loadReports(clearMessages: Boolean = true) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = if (clearMessages) null else _uiState.value.successMessage
        )

        viewModelScope.launch {
            repository.loadAdminReports()
                .onSuccess { reports ->
                    val selectedId = _uiState.value.selectedReport?.id
                    val selectedReport = reports.firstOrNull { it.id == selectedId }
                    _uiState.value = _uiState.value.copy(
                        reports = reports,
                        selectedReport = selectedReport,
                        selectedStatus = selectedReport?.status ?: _uiState.value.selectedStatus,
                        isLoading = false,
                        errorMessage = null,
                        successMessage = _uiState.value.successMessage
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "No se pudieron cargar las denuncias"
                    )
                }
        }
    }

    fun selectReport(report: AccountReport) {
        _uiState.value = _uiState.value.copy(
            selectedReport = report,
            selectedStatus = report.status,
            adminReplyDraft = "",
            errorMessage = null,
            successMessage = null
        )
    }

    fun closeReportDetail() {
        _uiState.value = _uiState.value.copy(
            selectedReport = null,
            adminReplyDraft = "",
            errorMessage = null,
            successMessage = null
        )
    }

    fun onStatusChange(status: AccountReportStatus) {
        _uiState.value = _uiState.value.copy(selectedStatus = status)
    }

    fun onFilterChange(filter: AdminReportFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun onAdminMessageChange(message: String) {
        _uiState.value = _uiState.value.copy(adminReplyDraft = message)
    }

    fun saveSelectedReport() {
        val report = _uiState.value.selectedReport ?: return
        if (_uiState.value.isSaving) return

        _uiState.value = _uiState.value.copy(
            isSaving = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            repository.updateAdminReport(
                reportId = report.id,
                status = _uiState.value.selectedStatus,
                adminMessage = report.adminMessage
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        successMessage = "Denuncia actualizada"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "No se pudo actualizar la denuncia"
                    )
                }
        }
    }

    fun sendAdminReply(imageUris: List<String> = emptyList()) {
        val report = _uiState.value.selectedReport ?: return
        val draft = _uiState.value.adminReplyDraft.trim()
        if (_uiState.value.isSaving || (draft.isBlank() && imageUris.isEmpty())) return

        _uiState.value = _uiState.value.copy(
            isSaving = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            repository.addAdminReportMessage(
                reportId = report.id,
                text = draft,
                imageUris = imageUris
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        adminReplyDraft = "",
                        successMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "No se pudo enviar el mensaje"
                    )
                }
        }
    }
}

fun AccountReport.matchesAdminFilter(filter: AdminReportFilter, kind: AccountReportKind = AccountReportKind.REPORT): Boolean {
    return this.kind == kind && when (filter) {
        AdminReportFilter.ALL -> true
        AdminReportFilter.NEW -> status == AccountReportStatus.NEW
        AdminReportFilter.IN_PROGRESS -> status == AccountReportStatus.IN_PROGRESS
        AdminReportFilter.PROCESSED -> status == AccountReportStatus.PROCESSED
        AdminReportFilter.CLOSED -> status == AccountReportStatus.CLOSED
        AdminReportFilter.WAITING_USER_INFO -> {
            status == AccountReportStatus.IN_PROGRESS && chatMessages.lastOrNull()?.sender == REPORT_SENDER_ADMIN
        }
    }
}
