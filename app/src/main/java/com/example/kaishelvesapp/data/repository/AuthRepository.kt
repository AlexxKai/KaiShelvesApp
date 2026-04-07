package com.example.kaishelvesapp.data.repository

import com.example.kaishelvesapp.data.model.Usuario
import com.google.firebase.auth.FirebaseAuth
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
                ?: return Result.failure(Exception("No hay sesión iniciada"))

            val snapshot = firestore.collection("usuarios")
                .document(uid)
                .get()
                .await()

            val usuario = snapshot.toObject(Usuario::class.java)
                ?: Usuario(
                    uid = uid,
                    usuario = auth.currentUser?.displayName ?: "",
                    email = auth.currentUser?.email ?: ""
                )

            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<Usuario> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("No se pudo obtener el UID del usuario"))

            val snapshot = firestore.collection("usuarios")
                .document(uid)
                .get()
                .await()

            val usuario = snapshot.toObject(Usuario::class.java)
                ?: Usuario(
                    uid = uid,
                    usuario = "",
                    email = auth.currentUser?.email ?: email
                )

            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(usuario: String, email: String, password: String): Result<Usuario> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("No se pudo obtener el UID del usuario"))

            val nuevoUsuario = Usuario(
                uid = uid,
                usuario = usuario,
                email = email
            )

            firestore.collection("usuarios")
                .document(uid)
                .set(nuevoUsuario)
                .await()

            Result.success(nuevoUsuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUsername(newUsername: String): Result<Usuario> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("No hay sesión iniciada"))

            val email = auth.currentUser?.email ?: ""

            val updatedUser = Usuario(
                uid = uid,
                usuario = newUsername,
                email = email
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
}