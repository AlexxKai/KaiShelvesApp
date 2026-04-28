package com.example.kaishelvesapp.data.local

import android.content.Context
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.LibroLeido
import com.example.kaishelvesapp.data.model.UserBookList
import com.example.kaishelvesapp.data.model.UserBookTag
import com.example.kaishelvesapp.data.model.Usuario
import com.example.kaishelvesapp.data.model.UserPrivacySettings
import com.google.gson.Gson
import com.google.gson.GsonBuilder

data class GuestLibraryState(
    val profile: Usuario? = null,
    val isSessionActive: Boolean = false,
    val lists: List<UserBookList> = emptyList(),
    val listBooks: Map<String, List<Libro>> = emptyMap(),
    val readBooks: List<LibroLeido> = emptyList(),
    val tags: List<UserBookTag> = emptyList(),
    val bookTagIds: Map<String, List<String>> = emptyMap()
)

object GuestLocalStore {
    private const val PREFS_NAME = "kai_guest_local_store"
    private const val KEY_STATE = "guest_library_state"

    const val GUEST_UID = "guest_local_user"
    const val SYSTEM_LIST_WANT_TO_READ_ID = "system_want_to_read"
    const val SYSTEM_LIST_READING_ID = "system_reading"
    const val SYSTEM_LIST_READ_ID = "system_read"

    const val SYSTEM_LIST_WANT_TO_READ_KEY = "want_to_read"
    const val SYSTEM_LIST_READING_KEY = "reading"
    const val SYSTEM_LIST_READ_KEY = "read"

    private val gson: Gson = GsonBuilder().create()

    private fun prefs() = AppContextProvider.requireContext().getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private fun defaultSystemLists() = listOf(
        UserBookList(
            id = SYSTEM_LIST_WANT_TO_READ_ID,
            name = "Quiero leer",
            description = "Libros que quieres empezar pronto.",
            position = 0,
            isSystem = true,
            systemKey = SYSTEM_LIST_WANT_TO_READ_KEY
        ),
        UserBookList(
            id = SYSTEM_LIST_READING_ID,
            name = "Leyendo",
            description = "Libros que tienes ahora mismo entre manos.",
            position = 1,
            isSystem = true,
            systemKey = SYSTEM_LIST_READING_KEY
        ),
        UserBookList(
            id = SYSTEM_LIST_READ_ID,
            name = "Leido",
            description = "Libros que ya forman parte de tu historial de lectura.",
            position = 2,
            isSystem = true,
            systemKey = SYSTEM_LIST_READ_KEY
        )
    )

    private fun normalizeState(state: GuestLibraryState): GuestLibraryState {
        val currentListsById = state.lists.associateBy { it.id }.toMutableMap()
        defaultSystemLists().forEach { defaultList ->
            val currentList = currentListsById[defaultList.id]
            currentListsById[defaultList.id] = if (currentList == null) {
                defaultList
            } else {
                defaultList.copy(
                    bookCount = currentList.bookCount,
                    previewImageUrls = currentList.previewImageUrls
                )
            }
        }

        val normalizedLists = currentListsById.values
            .sortedWith(
                compareBy<UserBookList> { !it.isSystem }
                    .thenBy { it.position }
                    .thenBy { it.name.lowercase() }
            )

        val normalizedProfile = state.profile?.copy(
            uid = GUEST_UID,
            isAdmin = false,
            isGuest = true
        )

        return state.copy(
            profile = normalizedProfile,
            lists = normalizedLists
        )
    }

    @Synchronized
    fun readState(): GuestLibraryState {
        val rawState = prefs().getString(KEY_STATE, null).orEmpty()
        val parsedState = runCatching {
            gson.fromJson(rawState, GuestLibraryState::class.java)
        }.getOrNull()

        return normalizeState(parsedState ?: GuestLibraryState())
    }

    @Synchronized
    fun writeState(state: GuestLibraryState) {
        prefs()
            .edit()
            .putString(KEY_STATE, gson.toJson(normalizeState(state)))
            .apply()
    }

    @Synchronized
    fun updateState(transform: (GuestLibraryState) -> GuestLibraryState): GuestLibraryState {
        val updated = normalizeState(transform(readState()))
        writeState(updated)
        return updated
    }

    fun isSessionActive(): Boolean {
        val state = readState()
        return state.isSessionActive && state.profile != null
    }

    fun getActiveProfile(): Usuario? {
        val state = readState()
        return state.profile?.takeIf { state.isSessionActive }
    }

    fun activateGuestProfile(username: String): Usuario {
        val trimmedUsername = username.trim()
        require(trimmedUsername.isNotBlank()) {
            "El nombre de usuario no puede estar vacio"
        }

        val updatedState = updateState { currentState ->
            val existingProfile = currentState.profile
            val nextProfile = existingProfile?.copy(
                usuario = trimmedUsername,
                isGuest = true
            ) ?: Usuario(
                uid = GUEST_UID,
                usuario = trimmedUsername,
                email = "",
                photoUrl = "",
                isAdmin = false,
                isGuest = true
            )

            currentState.copy(
                profile = nextProfile,
                isSessionActive = true
            )
        }

        return updatedState.profile
            ?: error("No se pudo activar el perfil invitado")
    }

    fun updateProfile(username: String, photoUrl: String): Usuario {
        val trimmedUsername = username.trim()
        require(trimmedUsername.isNotBlank()) {
            "El nombre de usuario no puede estar vacio"
        }

        val updatedState = updateState { currentState ->
            val profile = currentState.profile
                ?: throw IllegalStateException("No hay sesion invitada iniciada")

            currentState.copy(
                profile = profile.copy(
                    usuario = trimmedUsername,
                    photoUrl = photoUrl,
                    isGuest = true
                )
            )
        }

        return updatedState.profile
            ?: error("No se pudo actualizar el perfil invitado")
    }

    fun updatePrivacySettings(privacySettings: UserPrivacySettings): Usuario {
        val updatedState = updateState { currentState ->
            val profile = currentState.profile
                ?: throw IllegalStateException("No hay sesion invitada iniciada")

            currentState.copy(
                profile = profile.copy(
                    privacySettings = privacySettings,
                    isGuest = true
                )
            )
        }

        return updatedState.profile
            ?: error("No se pudo actualizar la privacidad del perfil invitado")
    }

    fun deactivateSession() {
        updateState { currentState ->
            currentState.copy(isSessionActive = false)
        }
    }

    fun hasLibraryData(): Boolean {
        val state = readState()
        return state.listBooks.values.any { it.isNotEmpty() } ||
            state.readBooks.isNotEmpty() ||
            state.tags.isNotEmpty() ||
            state.bookTagIds.isNotEmpty() ||
            state.lists.any { !it.isSystem }
    }

    fun clearAll() {
        writeState(GuestLibraryState())
    }
}
