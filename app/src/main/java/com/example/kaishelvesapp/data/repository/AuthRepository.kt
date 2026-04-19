package com.example.kaishelvesapp.data.repository

import android.net.Uri
import com.example.kaishelvesapp.data.model.Usuario
import com.example.kaishelvesapp.data.security.ProfileImageCodec
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
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

private data class BootstrapAdminAccount(
    val uid: String,
    val email: String
)

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val bootstrapAdminAccounts = listOf(
        BootstrapAdminAccount(
            uid = "npVZkecTBzLHU9r9Tof4FRLdn0k2",
            email = "admin@admin.com"
        )
    )

    fun isAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    fun getCurrentUid(): String? {
        return auth.currentUser?.uid
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

            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserProfile(): Result<Usuario> {
        return try {
            val firebaseUser = auth.currentUser
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
                isAdmin = storedUser?.isAdmin ?: false
            )

            Result.success(syncBootstrapAdminAccess(usuario))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(identifier: String, password: String): Result<Usuario> {
        return try {
            val resolvedEmail = resolveEmailForLogin(identifier)
                .getOrElse { error -> return Result.failure(error) }

            val authResult = auth.signInWithEmailAndPassword(resolvedEmail, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("No se pudo obtener el usuario autenticado"))
            val usuario = getOrCreateUserProfile(firebaseUser)
            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(usuario: String, email: String, password: String): Result<Usuario> {
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

            Result.success(nuevoUsuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<Usuario> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("No se pudo obtener el usuario de Google"))

            val usuario = getOrCreateUserProfile(firebaseUser)
            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(newUsername: String, selectedPhotoUri: String): Result<Usuario> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("No hay sesion iniciada"))
            val uid = currentUser.uid
            val currentProfile = getCurrentUserProfile().getOrNull()
            ensureUsernameIsUnique(newUsername, currentUid = uid)

            val resolvedPhotoUrl = when {
                selectedPhotoUri.isBlank() -> currentProfile?.photoUrl.orEmpty()
                selectedPhotoUri.startsWith("content://") -> uploadProfilePhoto(uid, Uri.parse(selectedPhotoUri))
                else -> selectedPhotoUri
            }

            val updatedUser = Usuario(
                uid = uid,
                usuario = newUsername,
                email = currentUser.email ?: "",
                photoUrl = resolvedPhotoUrl,
                isAdmin = currentProfile?.isAdmin ?: false
            )

            saveUserProfile(
                user = updatedUser,
                previousUsername = currentProfile?.usuario.orEmpty()
            )

            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    private suspend fun resolveEmailForLogin(identifier: String): Result<String> {
        val normalizedIdentifier = identifier.trim()
        if (normalizedIdentifier.isBlank()) {
            return Result.failure(Exception("Completa correo o usuario"))
        }

        if ("@" in normalizedIdentifier) {
            return Result.success(normalizedIdentifier)
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
            ?.getString("email")
            ?.trim()
            .orEmpty()

        if (matchedEmail.isNotBlank()) {
            return Result.success(matchedEmail)
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

            transaction.set(userRef, user)
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
                isAdmin = existingUser.isAdmin
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
}
