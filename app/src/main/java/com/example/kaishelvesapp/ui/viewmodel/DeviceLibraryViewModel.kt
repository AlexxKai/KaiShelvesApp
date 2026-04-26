package com.example.kaishelvesapp.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kaishelvesapp.data.repository.DeviceFileRepository
import com.example.kaishelvesapp.data.repository.DeviceLibraryFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceLibraryUiState(
    val selectedFolderUri: String? = null,
    val files: List<DeviceLibraryFile> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val filteredFiles: List<DeviceLibraryFile>
        get() {
            val query = searchQuery.trim()
            if (query.isBlank()) return files
            return files.filter { file ->
                file.name.contains(query, ignoreCase = true) ||
                    file.location.contains(query, ignoreCase = true) ||
                    file.mimeType.orEmpty().contains(query, ignoreCase = true)
            }
        }
}

class DeviceLibraryViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = DeviceFileRepository(application.applicationContext)
    private val preferences = application.getSharedPreferences("device_library", Application.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<DeviceLibraryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun useFolder(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(uri, flags)
        }
        preferences.edit()
            .putString(KEY_FOLDER_URI, uri.toString())
            .apply()
        _uiState.update {
            it.copy(
                selectedFolderUri = uri.toString(),
                errorMessage = null
            )
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val folderUri = state.selectedFolderUri?.let(Uri::parse)
                    ?: return@runCatching emptyList()
                repository.listFolderFiles(folderUri)
            }.onSuccess { files ->
                _uiState.update {
                    it.copy(
                        files = files,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        files = emptyList(),
                        isLoading = false,
                        errorMessage = throwable.localizedMessage ?: "No se pudieron cargar los archivos."
                    )
                }
            }
        }
    }

    private fun loadInitialState(): DeviceLibraryUiState {
        val folderUri = preferences.getString(KEY_FOLDER_URI, null)
        return DeviceLibraryUiState(
            selectedFolderUri = folderUri
        )
    }

    private companion object {
        const val KEY_FOLDER_URI = "folder_uri"
    }
}
