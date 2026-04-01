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

    fun logout() {
        auth.signOut()
    }
}