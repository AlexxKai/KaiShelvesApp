package com.example.kaishelvesapp.data.repository

import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.LibroLeido
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
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

    suspend fun getBooksByGenre(genero: String): Result<List<Libro>> {
        return try {
            val snapshot = firestore.collection("libros")
                .whereEqualTo("genero", genero)
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

    suspend fun searchBooks(genero: String?, query: String): Result<List<Libro>> {
        return try {
            val baseQuery = if (genero.isNullOrBlank() || genero == "Todos") {
                firestore.collection("libros")
            } else {
                firestore.collection("libros")
                    .whereEqualTo("genero", genero)
            }

            val snapshot = baseQuery.get().await()
            val texto = query.trim().lowercase()

            val libros = snapshot.documents
                .mapNotNull { it.toObject(Libro::class.java) }
                .filter { libro ->
                    libro.titulo.lowercase().contains(texto) ||
                            libro.autor.lowercase().contains(texto) ||
                            libro.editorial.lowercase().contains(texto)
                }

            Result.success(libros)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun marcarLibroComoLeido(libro: Libro): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val fechaActual = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date())

            val libroLeido = LibroLeido(
                isbn = libro.isbn,
                titulo = libro.titulo,
                autor = libro.autor,
                editorial = libro.editorial,
                genero = libro.genero,
                fechaPublicacion = libro.fechaPublicacion,
                paginas = libro.paginas,
                imagen = libro.imagen,
                pdf = libro.pdf,
                fechaLeido = fechaActual,
                puntuacion = 0,
                siNo = "si"
            )

            firestore.collection("usuarios")
                .document(uid)
                .collection("leidos")
                .document(libro.isbn)
                .set(libroLeido)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerListaLecturas(): Result<List<LibroLeido>> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val snapshot = firestore.collection("usuarios")
                .document(uid)
                .collection("leidos")
                .get()
                .await()

            val libros = snapshot.documents.mapNotNull { document ->
                document.toObject(LibroLeido::class.java)
            }

            Result.success(libros)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarPuntuacion(isbn: String, puntuacion: Int): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            firestore.collection("usuarios")
                .document(uid)
                .collection("leidos")
                .document(isbn)
                .update("puntuacion", puntuacion)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarLibroLeido(isbn: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            firestore.collection("usuarios")
                .document(uid)
                .collection("leidos")
                .document(isbn)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}