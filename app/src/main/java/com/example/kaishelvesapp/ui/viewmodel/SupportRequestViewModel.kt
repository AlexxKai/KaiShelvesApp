package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.repository.FriendsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SupportRequestUiState(
    val subject: String = "",
    val message: String = "",
    val imageUris: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class SupportRequestViewModel(
    private val repository: FriendsRepository = FriendsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupportRequestUiState())
    val uiState: StateFlow<SupportRequestUiState> = _uiState.asStateFlow()

    fun onSubjectChange(value: String) {
        _uiState.value = _uiState.value.copy(subject = value, errorMessage = null)
    }

    fun onMessageChange(value: String) {
        _uiState.value = _uiState.value.copy(message = value, errorMessage = null)
    }

    fun addImages(images: List<String>) {
        _uiState.value = _uiState.value.copy(
            imageUris = (_uiState.value.imageUris + images).distinct(),
            errorMessage = null
        )
    }

    fun removeImage(imageUri: String) {
        _uiState.value = _uiState.value.copy(
            imageUris = _uiState.value.imageUris - imageUri
        )
    }

    fun prepareNewRequest() {
        _uiState.value = SupportRequestUiState()
    }

    fun submit(onSuccess: () -> Unit = {}) {
        val state = _uiState.value
        if (state.isSaving) return
        if (state.subject.isBlank() || state.message.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Completa el asunto y el mensaje")
            return
        }

        _uiState.value = state.copy(isSaving = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            repository.submitSupportRequest(
                subject = state.subject,
                message = state.message,
                photoUris = state.imageUris
            )
                .onSuccess {
                    _uiState.value = SupportRequestUiState()
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "No se pudo enviar la solicitud"
                    )
                }
        }
    }
}
