package com.example.kaishelvesapp.data.repository

import android.net.Uri
import com.example.kaishelvesapp.data.model.Usuario
import com.example.kaishelvesapp.data.security.ProfileImageCodec
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun isAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    fun getCurrentUid(): String? {
        return auth.currentUser?.uid
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
                    ?: firebaseUser?.photoUrl?.toString().orEmpty()
            )

            Result.success(usuario)
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
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("No se pudo obtener el UID del usuario"))

            val nuevoUsuario = Usuario(
                uid = firebaseUser.uid,
                usuario = usuario,
                email = email,
                photoUrl = firebaseUser.photoUrl?.toString().orEmpty()
            )

            firestore.collection("usuarios")
                .document(firebaseUser.uid)
                .set(nuevoUsuario)
                .await()

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

            val resolvedPhotoUrl = when {
                selectedPhotoUri.isBlank() -> currentProfile?.photoUrl.orEmpty()
                selectedPhotoUri.startsWith("content://") -> uploadProfilePhoto(uid, Uri.parse(selectedPhotoUri))
                else -> selectedPhotoUri
            }

            val updatedUser = Usuario(
                uid = uid,
                usuario = newUsername,
                email = currentUser.email ?: "",
                photoUrl = resolvedPhotoUrl
            )

            firestore.collection("usuarios")
                .document(uid)
                .set(updatedUser)
                .await()

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

    private suspend fun getOrCreateUserProfile(firebaseUser: FirebaseUser): Usuario {
        val uid = firebaseUser.uid
        val snapshot = firestore.collection("usuarios")
            .document(uid)
            .get()
            .await()

        val existingUser = snapshot.toObject(Usuario::class.java)
        if (existingUser != null) {
            return Usuario(
                uid = uid,
                usuario = existingUser.usuario.takeIf { it.isNotBlank() }
                    ?: firebaseUser.displayName.orEmpty(),
                email = existingUser.email.takeIf { it.isNotBlank() }
                    ?: firebaseUser.email.orEmpty(),
                photoUrl = existingUser.photoUrl.takeIf { it.isNotBlank() }
                    ?: firebaseUser.photoUrl?.toString().orEmpty()
            )
        }

        val fallbackUsername = firebaseUser.displayName
            ?.takeIf { it.isNotBlank() }
            ?: firebaseUser.email?.substringBefore("@")
            ?: "Lector"

        val newUser = Usuario(
            uid = uid,
            usuario = fallbackUsername,
            email = firebaseUser.email.orEmpty(),
            photoUrl = firebaseUser.photoUrl?.toString().orEmpty()
        )

        firestore.collection("usuarios")
            .document(uid)
            .set(newUser)
            .await()

        return newUser
    }
}
