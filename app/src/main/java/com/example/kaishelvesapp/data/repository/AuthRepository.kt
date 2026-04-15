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
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("No hay sesion iniciada"))

            val snapshot = firestore.collection("usuarios")
                .document(uid)
                .get()
                .await()

            val usuario = snapshot.toObject(Usuario::class.java)
                ?: Usuario(
                    uid = uid,
                    usuario = auth.currentUser?.displayName ?: "",
                    email = auth.currentUser?.email ?: "",
                    photoUrl = auth.currentUser?.photoUrl?.toString().orEmpty()
                )

            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<Usuario> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
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

    private suspend fun uploadProfilePhoto(uid: String, uri: Uri): String {
        return ProfileImageCodec.encodeEncryptedImage(
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
            return existingUser
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
