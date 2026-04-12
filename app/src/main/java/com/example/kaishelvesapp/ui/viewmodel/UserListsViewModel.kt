package com.example.kaishelvesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.UserBookList
import com.example.kaishelvesapp.data.repository.UserListsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserListsUiState(
    val isLoading: Boolean = false,
    val lists: List<UserBookList> = emptyList(),
    val errorMessageRes: Int? = null,
    val successMessageRes: Int? = null
)

class UserListsViewModel(
    private val repository: UserListsRepository = UserListsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        UserListsUiState()
    )
    val uiState: StateFlow<UserListsUiState> = _uiState.asStateFlow()

    fun loadLists() {
        fetchLists()
    }

    private fun fetchLists(successMessageRes: Int? = null) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessageRes = null,
            successMessageRes = successMessageRes
        )

        viewModelScope.launch {
            repository.getUserLists()
                .onSuccess { lists ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lists = lists,
                        errorMessageRes = null,
                        successMessageRes = successMessageRes
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessageRes = R.string.lists_load_error
                    )
                }
        }
    }

    fun createList(name: String, description: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessageRes = R.string.list_name_required)
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessageRes = null,
            successMessageRes = null
        )

        viewModelScope.launch {
            repository.createList(trimmedName, description.trim())
                .onSuccess {
                    fetchLists(successMessageRes = R.string.list_created)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessageRes = R.string.list_create_error
                    )
                }
        }
    }

    fun updateList(listId: String, name: String, description: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessageRes = R.string.list_name_required)
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessageRes = null,
            successMessageRes = null
        )

        viewModelScope.launch {
            repository.updateList(listId, trimmedName, description.trim())
                .onSuccess {
                    fetchLists(successMessageRes = R.string.list_updated)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessageRes = R.string.list_update_error
                    )
                }
        }
    }

    fun deleteList(listId: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessageRes = null,
            successMessageRes = null
        )

        viewModelScope.launch {
            repository.deleteList(listId)
                .onSuccess {
                    fetchLists(successMessageRes = R.string.list_deleted)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessageRes = R.string.list_delete_error
                    )
                }
        }
    }

    fun moveList(listId: String, moveUp: Boolean) {
        val currentLists = _uiState.value.lists
        val currentIndex = currentLists.indexOfFirst { it.id == listId }
        if (currentIndex == -1) return

        val targetIndex = if (moveUp) currentIndex - 1 else currentIndex + 1
        if (targetIndex !in currentLists.indices) return

        val reordered = currentLists.toMutableList().apply {
            val current = removeAt(currentIndex)
            add(targetIndex, current)
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            lists = reordered,
            errorMessageRes = null,
            successMessageRes = null
        )

        viewModelScope.launch {
            repository.updateListOrder(reordered.map { it.id })
                .onSuccess {
                    fetchLists(successMessageRes = R.string.list_order_updated)
                }
                .onFailure {
                    fetchLists()
                    _uiState.value = _uiState.value.copy(
                        errorMessageRes = R.string.list_order_update_error
                    )
                }
        }
    }

    fun updateListOrder(orderedListIds: List<String>) {
        val currentLists = _uiState.value.lists
        val reorderedLists = orderedListIds.mapNotNull { orderedId ->
            currentLists.firstOrNull { it.id == orderedId }
        }

        if (reorderedLists.size != currentLists.size || reorderedLists == currentLists) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            lists = reorderedLists,
            errorMessageRes = null,
            successMessageRes = null
        )

        viewModelScope.launch {
            repository.updateListOrder(orderedListIds)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lists = reorderedLists,
                        successMessageRes = R.string.list_order_updated
                    )
                }
                .onFailure {
                    fetchLists()
                    _uiState.value = _uiState.value.copy(
                        errorMessageRes = R.string.list_order_update_error
                    )
                }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessageRes = null,
            successMessageRes = null
        )
    }
}
