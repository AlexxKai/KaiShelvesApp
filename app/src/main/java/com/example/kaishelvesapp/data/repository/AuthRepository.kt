package com.example.kaishelvesapp.data.repository

import android.net.Uri
import com.example.kaishelvesapp.data.local.GuestLocalStore
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.LibroLeido
import com.example.kaishelvesapp.data.model.UserBookList
import com.example.kaishelvesapp.data.model.UserBookTag
import com.example.kaishelvesapp.data.model.Usuario
import com.example.kaishelvesapp.data.model.UserPrivacySettings
import com.example.kaishelvesapp.data.notifications.DeviceNotificationManager
import com.example.kaishelvesapp.data.security.ProfileImageCodec
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class UsernameConflictUser(
    val uid: String,
    val username: String,
    val email: String,
    val photoUrl: String
)

data class UsernameConflictGroup(
    val normalizedUsername: String,
    val users: List<UsernameConflictUser>
)

data class LibraryDataSummary(
    val customLists: Int = 0,
    val organizedBooks: Int = 0,
    val readBooks: Int = 0,
    val tags: Int = 0
)

enum class GuestMergeStrategy {
    MERGE,
    KEEP_CLOUD,
    REPLACE_CLOUD
}

data class GuestMergeDecision(
    val user: Usuario,
    val localSummary: LibraryDataSummary,
    val cloudSummary: LibraryDataSummary
)

sealed interface AuthOperationResult {
    data class Success(val user: Usuario) : AuthOperationResult
    data class PendingGuestMerge(val decision: GuestMergeDecision) : AuthOperationResult
}

data class LoginProviderState(
    val providerId: String,
    val isLinked: Boolean,
    val isPrimary: Boolean
)

private data class BootstrapAdminAccount(
    val uid: String,
    val email: String
)

private data class CloudLibrarySnapshot(
    val lists: List<UserBookList>,
    val listBooks: Map<String, List<Libro>>,
    val readBooks: List<LibroLeido>,
    val tags: List<UserBookTag>,
    val bookTagIds: Map<String, List<String>>
)

private data class PendingGuestMergeState(
    val user: Usuario,
    val localState: com.example.kaishelvesapp.data.local.GuestLibraryState
)

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private var pendingGuestMergeState: PendingGuestMergeState? = null

    private val bootstrapAdminAccounts = listOf(
        BootstrapAdminAccount(
            uid = "npVZkecTBzLHU9r9Tof4FRLdn0k2",
            email = "admin@admin.com"
        )
    )

    fun isAuthenticated(): Boolean {
        return auth.currentUser != null || GuestLocalStore.isSessionActive()
    }

    fun getCurrentUid(): String? {
        return auth.currentUser?.uid ?: GuestLocalStore.getActiveProfile()?.uid
    }

    fun hasPasswordLogin(): Boolean {
        return auth.currentUser
            ?.providerData
            ?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } == true
    }

    fun hasGoogleLogin(): Boolean {
        return auth.currentUser
            ?.providerData
            ?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true
    }

    suspend fun getLoginProviders(): List<LoginProviderState> {
        val currentUser = auth.currentUser ?: return emptyList()
        val linkedProviderIds = currentUser.providerData
            .map { it.providerId }
            .filter { it != "firebase" }
            .toSet()
        val userRef = firestore.collection("usuarios").document(currentUser.uid)
        val storedPrimaryProviderId = userRef
            .get()
            .await()
            .getString("primaryLoginProvider")
            .orEmpty()
        val primaryProviderId = storedPrimaryProviderId
            .takeIf { it in linkedProviderIds }
            ?: linkedProviderIds.firstOrNull().orEmpty()

        if (storedPrimaryProviderId.isBlank() && primaryProviderId.isNotBlank()) {
            userRef
                .set(mapOf("primaryLoginProvider" to primaryProviderId), SetOptions.merge())
                .await()
        }

        return supportedLoginProviderIds.map { providerId ->
            LoginProviderState(
                providerId = providerId,
                isLinked = providerId in linkedProviderIds,
                isPrimary = providerId == primaryProviderId
            )
        }
    }

    suspend fun migrateLegacyUsernames() {
        val usersSnapshot = firestore.collection("usuarios")
            .get()
            .await()

        val reservedUsernames = firestore.collection("usernames")
            .get()
            .await()
            .documents
            .associateBy { it.id }
            .toMutableMap()

        val pendingReservations = usersSnapshot.documents.mapNotNull { document ->
            val username = document.getString("usuario").orEmpty()
            val normalizedUsername = normalizeUsername(username)
            if (normalizedUsername.isBlank()) {
                return@mapNotNull null
            }

            val existingReservation = reservedUsernames[normalizedUsername]
            val reservedUid = existingReservation?.getString("uid").orEmpty()
            if (existingReservation != null && reservedUid.isNotBlank()) {
                return@mapNotNull null
            }

            reservedUsernames[normalizedUsername] = existingReservation ?: document
            normalizedUsername to mapOf(
                "uid" to document.id,
                "usuario" to username
            )
        }

        pendingReservations
            .chunked(400)
            .forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { (normalizedUsername, payload) ->
                    val usernameRef = firestore.collection("usernames").document(normalizedUsername)
                    batch.set(usernameRef, payload, SetOptions.merge())
                }
                batch.commit().await()
            }
    }

    suspend fun getDuplicateUsernameGroups(): Result<List<UsernameConflictGroup>> {
        return try {
            requireAdminAccess()

            val usersSnapshot = firestore.collection("usuarios")
                .get()
                .await()

            val duplicateGroups = usersSnapshot.documents
                .mapNotNull { document ->
                    val username = document.getString("usuario").orEmpty()
                    val normalizedUsername = normalizeUsername(username)
                    if (normalizedUsername.isBlank()) {
                        null
                    } else {
                        normalizedUsername to UsernameConflictUser(
                            uid = document.id,
                            username = username,
                            email = document.getString("email").orEmpty(),
                            photoUrl = document.getString("photoUrl").orEmpty()
                        )
                    }
                }
                .groupBy(
                    keySelector = { it.first },
                    valueTransform = { it.second }
                )
                .filterValues { it.size > 1 }
                .map { (normalizedUsername, users) ->
                    UsernameConflictGroup(
                        normalizedUsername = normalizedUsername,
                        users = users.sortedBy { it.email.ifBlank { it.uid } }
                    )
                }
                .sortedBy { it.normalizedUsername }

            Result.success(duplicateGroups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminRenameUser(uid: String, newUsername: String): Result<Usuario> {
        return try {
            requireAdminAccess()

            val snapshot = firestore.collection("usuarios")
                .document(uid)
                .get()
                .await()

            if (!snapshot.exists()) {
                return Result.failure(Exception("No se encontro el usuario"))
            }

            val existingUser = snapshot.toObject(Usuario::class.java)
                ?: return Result.failure(Exception("No se pudo leer el usuario"))

            val updatedUser = existingUser.copy(
                uid = uid,
                usuario = newUsername.trim()
            )

            ensureUsernameIsUnique(updatedUser.usuario, currentUid = uid)
            saveUserProfile(
                user = updatedUser,
                previousUsername = existingUser.usuario
            )
            notifyUsernameChanged(
                targetUser = updatedUser,
                previousUsername = existingUser.usuario,
                changedByAdmin = true
            )

            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserProfile(): Result<Usuario> {
        return try {
            val firebaseUser = auth.currentUser
            if (firebaseUser == null) {
                val guestProfile = GuestLocalStore.getActiveProfile()
                    ?: return Result.failure(Exception("No hay sesion iniciada"))
                return Result.success(guestProfile)
            }

            val uid = firebaseUser?.uid
                ?: return Result.failure(Exception("No hay sesion iniciada"))

            val snapshot = firestore.collection("usuarios")
                .document(uid)
                .get()
                .await()

            val storedUser = snapshot.toObject(Usuario::class.java)
            val usuario = Usuario(
                uid = uid,
                usuario = storedUser?.usuario?.takeIf { it.isNotBlank() }
                    ?: firebaseUser?.displayName.orEmpty(),
                email = storedUser?.email?.takeIf { it.isNotBlank() }
                    ?: firebaseUser?.email.orEmpty(),
                photoUrl = storedUser?.photoUrl?.takeIf { it.isNotBlank() }
                    ?: firebaseUser?.photoUrl?.toString().orEmpty(),
                isAdmin = storedUser?.isAdmin ?: false,
                privacySettings = storedUser?.privacySettings ?: UserPrivacySettings()
            )

            Result.success(syncBootstrapAdminAccess(usuario))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(identifier: String, password: String): Result<AuthOperationResult> {
        return try {
            val resolvedEmail = resolveEmailForLogin(identifier)
                .getOrElse { error -> return Result.failure(error) }

            val authResult = auth.signInWithEmailAndPassword(resolvedEmail, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("No se pudo obtener el usuario autenticado"))
            val usuario = getOrCreateUserProfile(firebaseUser)
            Result.success(resolvePostAuthResult(usuario))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(usuario: String, email: String, password: String): Result<AuthOperationResult> {
        return try {
            ensureUsernameIsUnique(usuario)

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("No se pudo obtener el UID del usuario"))

            val nuevoUsuario = Usuario(
                uid = firebaseUser.uid,
                usuario = usuario,
                email = email,
                photoUrl = firebaseUser.photoUrl?.toString().orEmpty(),
                isAdmin = false
            )

            try {
                saveUserProfile(nuevoUsuario)
            } catch (e: Exception) {
                firebaseUser.delete().await()
                throw e
            }

            Result.success(resolvePostAuthResult(nuevoUsuario))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<AuthOperationResult> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("No se pudo obtener el usuario de Google"))

            val usuario = getOrCreateUserProfile(firebaseUser)
            Result.success(resolvePostAuthResult(usuario))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(newUsername: String, newEmail: String, selectedPhotoUri: String): Result<Usuario> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                val currentProfile = GuestLocalStore.getActiveProfile()
                    ?: return Result.failure(Exception("No hay sesion iniciada"))
                val resolvedPhotoUrl = when {
                    selectedPhotoUri.isBlank() -> currentProfile.photoUrl
                    selectedPhotoUri.startsWith("content://") -> uploadProfilePhoto(
                        GuestLocalStore.GUEST_UID,
                        Uri.parse(selectedPhotoUri)
                    )
                    else -> selectedPhotoUri
                }

                return Result.success(
                    GuestLocalStore.updateProfile(
                        username = newUsername,
                        photoUrl = resolvedPhotoUrl
                    )
                )
            }

            val uid = currentUser.uid
            val currentProfile = getCurrentUserProfile().getOrNull()
            ensureUsernameIsUnique(newUsername, currentUid = uid)

            val resolvedEmail = newEmail.trim()
            if (resolvedEmail.isBlank()) {
                return Result.failure(Exception("El correo electronico no puede estar vacio"))
            }

            if (!currentUser.email.equals(resolvedEmail, ignoreCase = true)) {
                currentUser.updateEmail(resolvedEmail).await()
            }

            val resolvedPhotoUrl = when {
                selectedPhotoUri.isBlank() -> currentProfile?.photoUrl.orEmpty()
                selectedPhotoUri.startsWith("content://") -> uploadProfilePhoto(uid, Uri.parse(selectedPhotoUri))
                else -> selectedPhotoUri
            }

            val updatedUser = Usuario(
                uid = uid,
                usuario = newUsername,
                email = resolvedEmail,
                photoUrl = resolvedPhotoUrl,
                isAdmin = currentProfile?.isAdmin ?: false,
                privacySettings = currentProfile?.privacySettings ?: UserPrivacySettings()
            )

            saveUserProfile(
                user = updatedUser,
                previousUsername = currentProfile?.usuario.orEmpty()
            )
            notifyUsernameChanged(
                targetUser = updatedUser,
                previousUsername = currentProfile?.usuario.orEmpty(),
                changedByAdmin = false
            )

            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePrivacySettings(privacySettings: UserPrivacySettings): Result<Usuario> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.success(GuestLocalStore.updatePrivacySettings(privacySettings))
            }

            val currentProfile = getCurrentUserProfile().getOrNull()
                ?: return Result.failure(Exception("No hay sesion iniciada"))
            val updatedUser = currentProfile.copy(privacySettings = privacySettings)

            firestore.collection("usuarios")
                .document(currentUser.uid)
                .set(
                    mapOf("privacySettings" to privacySettings),
                    SetOptions.merge()
                )
                .await()

            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePasswordLogin(email: String, password: String): Result<Usuario> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("No hay sesion iniciada"))
            val credentialEmail = email.trim()

            if (credentialEmail.isBlank()) {
                return Result.failure(Exception("El correo electrónico no puede estar vacío"))
            }

            if (password.length < 6) {
                return Result.failure(Exception("La contraseña debe tener al menos 6 caracteres"))
            }

            val currentProfile = getCurrentUserProfile().getOrNull()
                ?: return Result.failure(Exception("No hay sesion iniciada"))

            if (hasPasswordLogin()) {
                if (!currentUser.email.equals(credentialEmail, ignoreCase = true)) {
                    currentUser.updateEmail(credentialEmail).await()
                }
                currentUser.updatePassword(password).await()
            } else {
                val credential = EmailAuthProvider.getCredential(credentialEmail, password)
                currentUser.linkWithCredential(credential).await()
            }

            val updatedUser = currentProfile.copy(email = credentialEmail)
            saveUserProfile(
                user = updatedUser,
                previousUsername = currentProfile.usuario
            )

            firestore.collection("usuarios")
                .document(currentUser.uid)
                .set(
                    mapOf(
                        "passwordLoginEmail" to credentialEmail,
                        "passwordLoginUsername" to currentProfile.usuario
                    ),
                    SetOptions.merge()
                )
                .await()

            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlinkLoginProvider(providerId: String): Result<Usuario> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("No hay sesion iniciada"))
            val providerStates = getLoginProviders()
            val targetProvider = providerStates.firstOrNull { it.providerId == providerId }
                ?: return Result.failure(Exception("Proveedor no disponible"))

            if (!targetProvider.isLinked) {
                return Result.failure(Exception("Ese inicio de sesión no está asociado"))
            }

            if (targetProvider.isPrimary) {
                return Result.failure(Exception("No se puede quitar el inicio de sesión original"))
            }

            currentUser.unlink(providerId).await()

            if (providerId == EmailAuthProvider.PROVIDER_ID) {
                firestore.collection("usuarios")
                    .document(currentUser.uid)
                    .set(
                        mapOf(
                            "passwordLoginEmail" to FieldValue.delete(),
                            "passwordLoginUsername" to FieldValue.delete()
                        ),
                        SetOptions.merge()
                    )
                    .await()
            }

            val updatedUser = getCurrentUserProfile().getOrNull()
                ?: return Result.failure(Exception("No hay sesion iniciada"))
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun continueAsGuest(username: String): Result<Usuario> {
        return try {
            val normalizedUsername = normalizeUsername(username)
            if (normalizedUsername.isBlank()) {
                return Result.failure(Exception("El nombre de usuario no puede estar vacio"))
            }

            Result.success(GuestLocalStore.activateGuestProfile(username.trim()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolvePendingGuestMerge(strategy: GuestMergeStrategy): Result<Usuario> {
        return try {
            val pendingState = pendingGuestMergeState
                ?: return Result.failure(Exception("No hay ninguna fusion pendiente"))

            when (strategy) {
                GuestMergeStrategy.MERGE -> mergeGuestLibraryIntoCloud(
                    uid = pendingState.user.uid,
                    localState = pendingState.localState
                )

                GuestMergeStrategy.KEEP_CLOUD -> Unit

                GuestMergeStrategy.REPLACE_CLOUD -> {
                    clearCloudLibrary(pendingState.user.uid)
                    replaceCloudLibraryWithGuestData(
                        uid = pendingState.user.uid,
                        state = pendingState.localState
                    )
                }
            }

            GuestLocalStore.clearAll()
            pendingGuestMergeState = null
            Result.success(pendingState.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cancelPendingGuestMerge() {
        pendingGuestMergeState = null
        auth.signOut()
    }

    suspend fun syncPendingAccountNotifications() {
        val currentUid = auth.currentUser?.uid ?: return
        val context = FirebaseApp.getInstance().applicationContext
        val pendingNotifications = firestore.collection("usuarios")
            .document(currentUid)
            .collection("notifications")
            .whereEqualTo("localNotified", false)
            .get()
            .await()
            .documents

        pendingNotifications.forEach { document ->
            val title = document.getString("title").orEmpty()
            val body = document.getString("body").orEmpty()
            if (title.isBlank() || body.isBlank()) {
                return@forEach
            }

            val posted = DeviceNotificationManager.showAccountNotification(
                context = context,
                notificationId = document.id.hashCode(),
                title = title,
                body = body
            )

            if (posted) {
                document.reference
                    .set(mapOf("localNotified" to true), SetOptions.merge())
                    .await()
            }
        }
    }

    fun logout() {
        auth.signOut()
        GuestLocalStore.deactivateSession()
    }

    private suspend fun resolveEmailForLogin(identifier: String): Result<String> {
        val normalizedIdentifier = identifier.trim()
        if (normalizedIdentifier.isBlank()) {
            return Result.failure(Exception("Completa correo o usuario"))
        }

        val usernameMatches = firestore.collection("usuarios")
            .whereEqualTo("usuario", normalizedIdentifier)
            .limit(2)
            .get()
            .await()
            .documents

        if (usernameMatches.size > 1) {
            return Result.failure(Exception("Hay varios usuarios con ese nombre. Inicia sesion con tu correo"))
        }

        val matchedEmail = usernameMatches
            .firstOrNull()
            ?.let { document ->
                document.getString("passwordLoginEmail")
                    ?.takeIf { it.isNotBlank() }
                    ?: document.getString("email")
            }
            ?.trim()
            .orEmpty()

        if (matchedEmail.isNotBlank()) {
            return Result.success(matchedEmail)
        }

        if ("@" in normalizedIdentifier) {
            return Result.success(normalizedIdentifier)
        }

        val emailMatch = firestore.collection("usuarios")
            .whereEqualTo("email", normalizedIdentifier)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.getString("email")
            ?.trim()
            .orEmpty()

        return if (emailMatch.isNotBlank()) {
            Result.success(emailMatch)
        } else {
            Result.failure(Exception("No encontramos un usuario con ese correo o nombre"))
        }
    }

    private suspend fun uploadProfilePhoto(uid: String, uri: Uri): String {
        return ProfileImageCodec.encodeImageAsDataUri(
            context = FirebaseApp.getInstance().applicationContext,
            uri = uri
        )
    }

    private suspend fun resolvePostAuthResult(user: Usuario): AuthOperationResult {
        if (!hasGuestLibraryToMigrate()) {
            pendingGuestMergeState = null
            return AuthOperationResult.Success(user)
        }

        if (isTargetLibraryEmpty(user.uid)) {
            replaceCloudLibraryWithGuestData(
                uid = user.uid,
                state = GuestLocalStore.readState()
            )
            GuestLocalStore.clearAll()
            pendingGuestMergeState = null
            return AuthOperationResult.Success(user)
        }

        val localState = GuestLocalStore.readState()
        pendingGuestMergeState = PendingGuestMergeState(
            user = user,
            localState = localState
        )

        return AuthOperationResult.PendingGuestMerge(
            decision = GuestMergeDecision(
                user = user,
                localSummary = buildGuestLibrarySummary(localState),
                cloudSummary = buildCloudLibrarySummary(fetchCloudLibrarySnapshot(user.uid))
            )
        )
    }

    private fun userListsCollection(uid: String) = firestore.collection("usuarios")
        .document(uid)
        .collection("listas")

    private fun userReadCollection(uid: String) = firestore.collection("usuarios")
        .document(uid)
        .collection("leidos")

    private fun userTagsCollection(uid: String) = firestore.collection("usuarios")
        .document(uid)
        .collection("etiquetas")

    private fun userBookMetadataCollection(uid: String) = firestore.collection("usuarios")
        .document(uid)
        .collection("libros_metadata")

    private fun safeBookDocId(rawId: String): String {
        return rawId
            .trim()
            .ifBlank { "unknown_book" }
            .replace("/", "_")
    }

    private fun buildBookPayload(libro: Libro, safeBookId: String): Map<String, Any> {
        return mapOf(
            "id" to safeBookId,
            "isbn" to libro.isbn,
            "titulo" to libro.titulo,
            "autor" to libro.autor,
            "editorial" to libro.editorial,
            "genero" to libro.genero,
            "fechaPublicacion" to libro.fechaPublicacion,
            "paginas" to libro.paginas,
            "imagen" to libro.imagen,
            "pdf" to libro.pdf
        )
    }

    private fun buildReadBookPayload(readBook: LibroLeido): Map<String, Any> {
        return mapOf(
            "id" to readBook.id,
            "isbn" to readBook.isbn,
            "titulo" to readBook.titulo,
            "autor" to readBook.autor,
            "editorial" to readBook.editorial,
            "genero" to readBook.genero,
            "fechaPublicacion" to readBook.fechaPublicacion,
            "paginas" to readBook.paginas,
            "imagen" to readBook.imagen,
            "pdf" to readBook.pdf,
            "fechaLeido" to readBook.fechaLeido,
            "puntuacion" to readBook.puntuacion,
            "resena" to readBook.resena,
            "contieneSpoilers" to readBook.contieneSpoilers,
            "siNo" to readBook.siNo
        )
    }

    private fun hasGuestLibraryToMigrate(): Boolean {
        return GuestLocalStore.isSessionActive() && GuestLocalStore.hasLibraryData()
    }

    private fun buildGuestLibrarySummary(state: com.example.kaishelvesapp.data.local.GuestLibraryState): LibraryDataSummary {
        return LibraryDataSummary(
            customLists = state.lists.count { !it.isSystem },
            organizedBooks = state.listBooks.values.sumOf { it.size },
            readBooks = state.readBooks.size,
            tags = state.tags.size
        )
    }

    private fun buildCloudLibrarySummary(snapshot: CloudLibrarySnapshot): LibraryDataSummary {
        return LibraryDataSummary(
            customLists = snapshot.lists.count { !it.isSystem },
            organizedBooks = snapshot.listBooks.values.sumOf { it.size },
            readBooks = snapshot.readBooks.size,
            tags = snapshot.tags.size
        )
    }

    private suspend fun isTargetLibraryEmpty(uid: String): Boolean {
        val hasLists = userListsCollection(uid)
            .limit(1)
            .get()
            .await()
            .documents
            .isNotEmpty()
        val hasReadBooks = userReadCollection(uid)
            .limit(1)
            .get()
            .await()
            .documents
            .isNotEmpty()
        val hasTags = userTagsCollection(uid)
            .limit(1)
            .get()
            .await()
            .documents
            .isNotEmpty()
        val hasMetadata = userBookMetadataCollection(uid)
            .limit(1)
            .get()
            .await()
            .documents
            .isNotEmpty()

        return !hasLists && !hasReadBooks && !hasTags && !hasMetadata
    }

    private suspend fun replaceCloudLibraryWithGuestData(
        uid: String,
        state: com.example.kaishelvesapp.data.local.GuestLibraryState
    ) {
        var batch = firestore.batch()
        var writesInBatch = 0

        suspend fun flushBatch() {
            if (writesInBatch == 0) return
            batch.commit().await()
            batch = firestore.batch()
            writesInBatch = 0
        }

        fun trackWrite() {
            writesInBatch++
        }

        state.lists.forEach { list ->
            val listRef = userListsCollection(uid).document(list.id)
            batch.set(
                listRef,
                list.copy(
                    bookCount = state.listBooks[list.id].orEmpty().size,
                    previewImageUrls = emptyList()
                ),
                SetOptions.merge()
            )
            trackWrite()
            if (writesInBatch >= 350) {
                flushBatch()
            }

            state.listBooks[list.id].orEmpty().forEach { book ->
                val safeBookId = safeBookDocId(book.id.ifBlank { book.isbn })
                if (safeBookId == "unknown_book") return@forEach

                batch.set(
                    listRef.collection("libros").document(safeBookId),
                    buildBookPayload(book.copy(id = safeBookId), safeBookId) + mapOf(
                        "activityAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                trackWrite()
                if (writesInBatch >= 350) {
                    flushBatch()
                }
            }
        }

        state.readBooks.forEach { readBook ->
            val safeBookId = safeBookDocId(readBook.id.ifBlank { readBook.isbn })
            if (safeBookId == "unknown_book") return@forEach

            batch.set(
                userReadCollection(uid).document(safeBookId),
                buildReadBookPayload(readBook.copy(id = safeBookId)) + mapOf(
                    "activityAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            trackWrite()
            if (writesInBatch >= 350) {
                flushBatch()
            }
        }

        state.tags.forEach { tag ->
            batch.set(
                userTagsCollection(uid).document(tag.id),
                UserBookTag(
                    id = tag.id,
                    name = tag.name,
                    position = tag.position
                ),
                SetOptions.merge()
            )
            trackWrite()
            if (writesInBatch >= 350) {
                flushBatch()
            }
        }

        state.bookTagIds.forEach { (bookId, tagIds) ->
            val safeBookId = safeBookDocId(bookId)
            if (safeBookId == "unknown_book") return@forEach

            batch.set(
                userBookMetadataCollection(uid).document(safeBookId),
                mapOf("tagIds" to tagIds),
                SetOptions.merge()
            )
            trackWrite()
            if (writesInBatch >= 350) {
                flushBatch()
            }
        }

        flushBatch()
    }

    private suspend fun fetchCloudLibrarySnapshot(uid: String): CloudLibrarySnapshot {
        val listsSnapshot = userListsCollection(uid).get().await()
        val lists = listsSnapshot.documents.mapNotNull { document ->
            document.toObject(UserBookList::class.java)?.copy(id = document.id)
        }

        val listBooks = buildMap {
            listsSnapshot.documents.forEach { listDocument ->
                val books = listDocument.reference
                    .collection("libros")
                    .get()
                    .await()
                    .documents
                    .mapNotNull { document ->
                        document.toObject(Libro::class.java)?.copy(
                            id = document.getString("id").orEmpty().ifBlank { document.id }
                        )
                    }
                put(listDocument.id, books)
            }
        }

        val readBooks = userReadCollection(uid)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject(LibroLeido::class.java)?.copy(id = document.id)
            }

        val tags = userTagsCollection(uid)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject(UserBookTag::class.java)?.copy(id = document.id)
            }

        val bookTagIds = userBookMetadataCollection(uid)
            .get()
            .await()
            .documents
            .associate { document ->
                val tagIds = (document.get("tagIds") as? List<*>)
                    .orEmpty()
                    .filterIsInstance<String>()
                document.id to tagIds
            }

        return CloudLibrarySnapshot(
            lists = lists,
            listBooks = listBooks,
            readBooks = readBooks,
            tags = tags,
            bookTagIds = bookTagIds
        )
    }

    private suspend fun clearCloudLibrary(uid: String) {
        userListsCollection(uid)
            .get()
            .await()
            .documents
            .forEach { listDocument ->
                val booksSnapshot = listDocument.reference.collection("libros").get().await()
                booksSnapshot.documents.forEach { it.reference.delete().await() }
                listDocument.reference.delete().await()
            }

        userReadCollection(uid)
            .get()
            .await()
            .documents
            .forEach { it.reference.delete().await() }

        userTagsCollection(uid)
            .get()
            .await()
            .documents
            .forEach { it.reference.delete().await() }

        userBookMetadataCollection(uid)
            .get()
            .await()
            .documents
            .forEach { it.reference.delete().await() }
    }

    private suspend fun mergeGuestLibraryIntoCloud(
        uid: String,
        localState: com.example.kaishelvesapp.data.local.GuestLibraryState
    ) {
        val cloudSnapshot = fetchCloudLibrarySnapshot(uid)
        val defaultLists = localState.lists.filter { it.isSystem }
        val cloudListsById = cloudSnapshot.lists.associateBy { it.id }
        val existingCustomLists = cloudSnapshot.lists.filterNot { it.isSystem }
        val normalizedCloudCustomLists = existingCustomLists.associateBy {
            normalizeUsername(it.name)
        }.toMutableMap()
        val currentMaxCustomPosition = existingCustomLists.maxOfOrNull { it.position } ?: defaultLists.size

        val mergedLists = cloudListsById.toMutableMap()
        val mergedListBooks = cloudSnapshot.listBooks
            .mapValues { (_, books) -> books.associateBy { safeBookDocId(it.id.ifBlank { it.isbn }) }.toMutableMap() }
            .toMutableMap()
        val mergedReadBooks = cloudSnapshot.readBooks
            .associateBy { safeBookDocId(it.id.ifBlank { it.isbn }) }
            .toMutableMap()
        val mergedTagsById = cloudSnapshot.tags.associateBy { it.id }.toMutableMap()
        val normalizedCloudTags = cloudSnapshot.tags.associateBy { normalizeUsername(it.name) }.toMutableMap()
        val mergedBookTagIds = cloudSnapshot.bookTagIds
            .mapValues { (_, ids) -> ids.toMutableSet() }
            .toMutableMap()
        var nextCustomPosition = currentMaxCustomPosition + 1

        val localTagIdToRemoteTagId = mutableMapOf<String, String>()
        localState.tags.forEach { localTag ->
            val normalizedName = normalizeUsername(localTag.name)
            val existingTag = normalizedCloudTags[normalizedName]
            if (existingTag != null) {
                localTagIdToRemoteTagId[localTag.id] = existingTag.id
            } else {
                val newTagId = userTagsCollection(uid).document().id
                val newTag = UserBookTag(
                    id = newTagId,
                    name = localTag.name,
                    position = (mergedTagsById.values.maxOfOrNull { it.position } ?: -1) + 1
                )
                mergedTagsById[newTagId] = newTag
                normalizedCloudTags[normalizedName] = newTag
                localTagIdToRemoteTagId[localTag.id] = newTagId
            }
        }

        localState.lists.forEach { localList ->
            val targetList = when {
                localList.isSystem -> {
                    val fallback = defaultLists.firstOrNull { it.id == localList.id } ?: localList
                    mergedLists[localList.id] ?: fallback
                }

                else -> {
                    val normalizedName = normalizeUsername(localList.name)
                    normalizedCloudCustomLists[normalizedName] ?: UserBookList(
                        id = userListsCollection(uid).document().id,
                        name = localList.name,
                        description = localList.description,
                        bookCount = 0,
                        position = nextCustomPosition++
                    ).also { created ->
                        normalizedCloudCustomLists[normalizedName] = created
                    }
                }
            }

            mergedLists[targetList.id] = targetList.copy(
                description = targetList.description.ifBlank { localList.description }
            )

            val targetBooks = mergedListBooks.getOrPut(targetList.id) { mutableMapOf() }
            localState.listBooks[localList.id].orEmpty().forEach { localBook ->
                val safeBookId = safeBookDocId(localBook.id.ifBlank { localBook.isbn })
                if (safeBookId == "unknown_book") return@forEach
                val currentBook = targetBooks[safeBookId]
                targetBooks[safeBookId] = mergeBooks(
                    primary = currentBook,
                    secondary = localBook.copy(id = safeBookId)
                )
            }
        }

        localState.readBooks.forEach { localReadBook ->
            val safeBookId = safeBookDocId(localReadBook.id.ifBlank { localReadBook.isbn })
            if (safeBookId == "unknown_book") return@forEach
            val current = mergedReadBooks[safeBookId]
            mergedReadBooks[safeBookId] = mergeReadBooks(
                primary = current,
                secondary = localReadBook.copy(id = safeBookId)
            )
        }

        localState.bookTagIds.forEach { (bookId, localTagIds) ->
            val safeBookId = safeBookDocId(bookId)
            if (safeBookId == "unknown_book") return@forEach
            val targetTagIds = mergedBookTagIds.getOrPut(safeBookId) { mutableSetOf() }
            localTagIds.forEach { localTagId ->
                localTagIdToRemoteTagId[localTagId]?.let(targetTagIds::add)
            }
        }

        clearCloudLibrary(uid)
        replaceCloudLibraryWithGuestData(
            uid = uid,
            state = com.example.kaishelvesapp.data.local.GuestLibraryState(
                profile = null,
                isSessionActive = false,
                lists = mergedLists.values.toList(),
                listBooks = mergedListBooks.mapValues { (_, booksById) -> booksById.values.toList() },
                readBooks = mergedReadBooks.values.toList(),
                tags = mergedTagsById.values.toList(),
                bookTagIds = mergedBookTagIds.mapValues { (_, tagIds) -> tagIds.toList() }
            )
        )
    }

    private fun mergeBooks(primary: Libro?, secondary: Libro): Libro {
        if (primary == null) return secondary
        return primary.copy(
            id = primary.id.ifBlank { secondary.id },
            isbn = primary.isbn.ifBlank { secondary.isbn },
            titulo = primary.titulo.ifBlank { secondary.titulo },
            autor = primary.autor.ifBlank { secondary.autor },
            editorial = primary.editorial.ifBlank { secondary.editorial },
            genero = primary.genero.ifBlank { secondary.genero },
            fechaPublicacion = if (primary.fechaPublicacion != 0) primary.fechaPublicacion else secondary.fechaPublicacion,
            paginas = if (primary.paginas != 0) primary.paginas else secondary.paginas,
            imagen = primary.imagen.ifBlank { secondary.imagen },
            pdf = primary.pdf.ifBlank { secondary.pdf }
        )
    }

    private fun mergeReadBooks(primary: LibroLeido?, secondary: LibroLeido): LibroLeido {
        if (primary == null) return secondary

        val reviewToKeep = when {
            primary.resena.isBlank() -> secondary.resena
            secondary.resena.length > primary.resena.length -> secondary.resena
            else -> primary.resena
        }

        return primary.copy(
            id = primary.id.ifBlank { secondary.id },
            isbn = primary.isbn.ifBlank { secondary.isbn },
            titulo = primary.titulo.ifBlank { secondary.titulo },
            autor = primary.autor.ifBlank { secondary.autor },
            editorial = primary.editorial.ifBlank { secondary.editorial },
            genero = primary.genero.ifBlank { secondary.genero },
            fechaPublicacion = if (primary.fechaPublicacion != 0) primary.fechaPublicacion else secondary.fechaPublicacion,
            paginas = if (primary.paginas != 0) primary.paginas else secondary.paginas,
            imagen = primary.imagen.ifBlank { secondary.imagen },
            pdf = primary.pdf.ifBlank { secondary.pdf },
            fechaLeido = primary.fechaLeido.ifBlank { secondary.fechaLeido },
            puntuacion = maxOf(primary.puntuacion, secondary.puntuacion),
            resena = reviewToKeep,
            contieneSpoilers = primary.contieneSpoilers || secondary.contieneSpoilers,
            siNo = primary.siNo.ifBlank { secondary.siNo }
        )
    }

    private suspend fun notifyUsernameChanged(
        targetUser: Usuario,
        previousUsername: String,
        changedByAdmin: Boolean
    ) {
        val oldUsername = previousUsername.trim()
        val newUsername = targetUser.usuario.trim()
        if (oldUsername.isBlank() || newUsername.isBlank() || oldUsername == newUsername) {
            return
        }

        val title = "Tu nombre de usuario ha cambiado"
        val body = if (changedByAdmin) {
            "Tu usuario ha sido actualizado de \"$oldUsername\" a \"$newUsername\"."
        } else {
            "Tu usuario se ha actualizado de \"$oldUsername\" a \"$newUsername\"."
        }

        val notificationRef = firestore.collection("usuarios")
            .document(targetUser.uid)
            .collection("notifications")
            .document()

        notificationRef.set(
            mapOf(
                "type" to "username_changed",
                "title" to title,
                "body" to body,
                "previousUsername" to oldUsername,
                "newUsername" to newUsername,
                "read" to false,
                "localNotified" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()

        queueUsernameChangeEmail(
            user = targetUser,
            title = title,
            body = body,
            previousUsername = oldUsername,
            newUsername = newUsername
        )
    }

    // This queue can be processed later by Firebase Functions or another backend worker.
    private suspend fun queueUsernameChangeEmail(
        user: Usuario,
        title: String,
        body: String,
        previousUsername: String,
        newUsername: String
    ) {
        if (user.email.isBlank()) return

        firestore.collection("emailQueue")
            .document()
            .set(
                mapOf(
                    "toEmail" to user.email,
                    "subject" to title,
                    "body" to body,
                    "template" to "username_changed",
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "metadata" to mapOf(
                        "uid" to user.uid,
                        "previousUsername" to previousUsername,
                        "newUsername" to newUsername
                    )
                )
            )
            .await()
    }

    private suspend fun requireAdminAccess() {
        val firebaseUser = auth.currentUser
        val currentUid = firebaseUser?.uid
            ?: throw Exception("No hay sesion iniciada")
        val currentUserSnapshot = firestore.collection("usuarios")
            .document(currentUid)
            .get()
            .await()

        val currentUserEmail = currentUserSnapshot.getString("email")
            ?.takeIf { it.isNotBlank() }
            ?: firebaseUser?.email.orEmpty()
        val isBootstrapAdmin = bootstrapAdminAccounts.any { admin ->
            admin.uid == currentUid || admin.email.equals(currentUserEmail, ignoreCase = true)
        }
        val hasFirestoreAdminFlag = currentUserSnapshot.getBoolean("isAdmin") == true

        if (isBootstrapAdmin && !hasFirestoreAdminFlag) {
            currentUserSnapshot.reference
                .set(mapOf("isAdmin" to true), SetOptions.merge())
                .await()
            return
        }

        if (!hasFirestoreAdminFlag) {
            throw Exception("No tienes permisos para acceder al panel de administracion")
        }
    }

    private suspend fun saveUserProfile(
        user: Usuario,
        previousUsername: String = user.usuario
    ) {
        val normalizedUsername = normalizeUsername(user.usuario)
        if (normalizedUsername.isBlank()) {
            throw Exception("El nombre de usuario no puede estar vacio")
        }

        val previousNormalizedUsername = normalizeUsername(previousUsername)
        val userRef = firestore.collection("usuarios").document(user.uid)
        val usernameRef = firestore.collection("usernames").document(normalizedUsername)

        firestore.runTransaction { transaction ->
            val usernameSnapshot = transaction.get(usernameRef)
            val reservedUid = usernameSnapshot.getString("uid").orEmpty()

            if (usernameSnapshot.exists() && reservedUid.isNotBlank() && reservedUid != user.uid) {
                throw IllegalStateException("El nombre de usuario ya esta en uso")
            }

            transaction.set(userRef, user, SetOptions.merge())
            transaction.set(
                usernameRef,
                mapOf(
                    "uid" to user.uid,
                    "usuario" to user.usuario
                ),
                SetOptions.merge()
            )

            if (previousNormalizedUsername.isNotBlank() && previousNormalizedUsername != normalizedUsername) {
                transaction.delete(firestore.collection("usernames").document(previousNormalizedUsername))
            }
        }.await()
    }

    private suspend fun ensureUsernameIsUnique(
        username: String,
        currentUid: String? = null
    ) {
        val normalizedUsername = normalizeUsername(username)
        if (normalizedUsername.isBlank()) {
            throw Exception("El nombre de usuario no puede estar vacio")
        }

        val reservedSnapshot = firestore.collection("usernames")
            .document(normalizedUsername)
            .get()
            .await()

        val reservedUid = reservedSnapshot.getString("uid").orEmpty()
        if (reservedSnapshot.exists() && reservedUid.isNotBlank() && reservedUid != currentUid) {
            throw Exception("El nombre de usuario ya esta en uso")
        }

        val legacyConflict = firestore.collection("usuarios")
            .get()
            .await()
            .documents
            .firstOrNull { document ->
                document.id != currentUid &&
                    normalizeUsername(document.getString("usuario").orEmpty()) == normalizedUsername
            }

        if (legacyConflict != null) {
            throw Exception("El nombre de usuario ya esta en uso")
        }
    }

    private fun normalizeUsername(username: String): String {
        return username.trim().lowercase()
    }

    private suspend fun getOrCreateUserProfile(firebaseUser: FirebaseUser): Usuario {
        val uid = firebaseUser.uid
        val snapshot = firestore.collection("usuarios")
            .document(uid)
            .get()
            .await()

        val existingUser = snapshot.toObject(Usuario::class.java)
        if (existingUser != null) {
            return syncBootstrapAdminAccess(
                Usuario(
                uid = uid,
                usuario = existingUser.usuario.takeIf { it.isNotBlank() }
                    ?: firebaseUser.displayName.orEmpty(),
                email = existingUser.email.takeIf { it.isNotBlank() }
                    ?: firebaseUser.email.orEmpty(),
                photoUrl = existingUser.photoUrl.takeIf { it.isNotBlank() }
                    ?: firebaseUser.photoUrl?.toString().orEmpty(),
                isAdmin = existingUser.isAdmin,
                privacySettings = existingUser.privacySettings
            )
            )
        }

        val fallbackUsername = firebaseUser.displayName
            ?.takeIf { it.isNotBlank() }
            ?: firebaseUser.email?.substringBefore("@")
            ?: "Lector"

        val uniqueUsername = generateUniqueUsername(fallbackUsername)

        val newUser = Usuario(
            uid = uid,
            usuario = uniqueUsername,
            email = firebaseUser.email.orEmpty(),
            photoUrl = firebaseUser.photoUrl?.toString().orEmpty(),
            isAdmin = false
        )

        saveUserProfile(newUser)

        return syncBootstrapAdminAccess(newUser)
    }

    private suspend fun generateUniqueUsername(baseUsername: String): String {
        val trimmedBaseUsername = baseUsername.trim().ifBlank { "Lector" }
        if (isUsernameAvailable(trimmedBaseUsername)) {
            return trimmedBaseUsername
        }

        var suffix = 1
        while (suffix <= 1000) {
            val candidate = "$trimmedBaseUsername$suffix"
            if (isUsernameAvailable(candidate)) {
                return candidate
            }
            suffix++
        }

        throw Exception("No se pudo generar un nombre de usuario disponible")
    }

    private suspend fun isUsernameAvailable(username: String, currentUid: String? = null): Boolean {
        return try {
            ensureUsernameIsUnique(username, currentUid)
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun syncBootstrapAdminAccess(user: Usuario): Usuario {
        val shouldBeAdmin = bootstrapAdminAccounts.any { admin ->
            admin.uid == user.uid || admin.email.equals(user.email, ignoreCase = true)
        }

        if (!shouldBeAdmin || user.isAdmin) {
            return user
        }

        val updatedUser = user.copy(isAdmin = true)
        saveUserProfile(
            user = updatedUser,
            previousUsername = user.usuario
        )
        return updatedUser
    }

    private companion object {
        val supportedLoginProviderIds = listOf(
            GoogleAuthProvider.PROVIDER_ID,
            EmailAuthProvider.PROVIDER_ID,
            "facebook.com",
            "apple.com",
            "github.com"
        )
    }
}
