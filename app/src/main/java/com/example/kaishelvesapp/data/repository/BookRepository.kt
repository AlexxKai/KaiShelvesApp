package com.example.kaishelvesapp.data.repository

import com.example.kaishelvesapp.data.model.Libro
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BookRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun obtenerLibros(): Result<List<Libro>> {
        return try {
            val snapshot = firestore.collection("libros")
                .get()
                .await()

            val libros = snapshot.documents.mapNotNull { document ->
                document.toObject(Libro::class.java)
            }

            Result.success(libros)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}