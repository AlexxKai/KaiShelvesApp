package com.example.kaishelvesapp.data.repository

import com.example.kaishelvesapp.data.localization.BookMetadataLocalizer
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.LibroLeido
import com.example.kaishelvesapp.data.remote.googlebooks.GoogleBooksClient
import com.example.kaishelvesapp.data.remote.googlebooks.LibraryGenres
import com.example.kaishelvesapp.data.remote.googlebooks.toLibro
import com.example.kaishelvesapp.ui.language.LanguageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userListsRepository: UserListsRepository = UserListsRepository(firestore, auth)
) {

    private val api = GoogleBooksClient.api
    private val publicApi = GoogleBooksClient.publicApi

    private fun safeBookDocId(rawId: String): String {
        return rawId
            .trim()
            .ifBlank { "unknown_book" }
            .replace("/", "_")
    }

    private fun normalizeIsbnQuery(rawValue: String): String {
        return rawValue
            .trim()
            .removePrefix("ISBN")
            .removePrefix("isbn")
            .replace(":", "")
            .replace("-", "")
            .replace(" ", "")
            .uppercase()
    }

    private fun looksLikeIsbn(rawValue: String): Boolean {
        val normalized = normalizeIsbnQuery(rawValue)
        return normalized.length == 10 || normalized.length == 13
    }

    private fun currentGoogleBooksLanguage(): String? {
        return when (LanguageManager.getCurrentLanguage()) {
            "es" -> "es"
            "en" -> "en"
            else -> null
        }
    }

    private fun Throwable.shouldRetryGoogleBooksWithoutApiKey(): Boolean {
        val httpException = this as? HttpException ?: return false
        return httpException.code() == 403 && GoogleBooksClient.hasApiKey
    }

    private suspend fun searchGoogleBooks(
        query: String,
        maxResults: Int
    ) = try {
        api.searchBooks(
            query = query,
            maxResults = maxResults,
            langRestrict = currentGoogleBooksLanguage()
        )
    } catch (error: Exception) {
        if (!error.shouldRetryGoogleBooksWithoutApiKey()) {
            throw error
        }

        publicApi.searchBooks(
            query = query,
            maxResults = maxResults,
            langRestrict = currentGoogleBooksLanguage()
        )
    }

    suspend fun obtenerLibros(): Result<List<Libro>> {
        return try {
            val response = searchGoogleBooks(
                query = "subject:fiction",
                maxResults = 40
            )

            val libros = response.items
                .map { it.toLibro(fallbackGenero = "Ficcion") }
                .localizeForCurrentLanguage()

            Result.success(libros)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBooksByGenre(genero: String): Result<List<Libro>> {
        return try {
            val subjectQuery = LibraryGenres.all
                .firstOrNull { it.label == genero }
                ?.subjectQuery
                ?: genero.lowercase()

            val response = searchGoogleBooks(
                query = "subject:$subjectQuery",
                maxResults = 40
            )

            val libros = response.items
                .map { it.toLibro(fallbackGenero = genero) }
                .localizeForCurrentLanguage()

            Result.success(libros)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchBooks(genero: String?, query: String): Result<List<Libro>> {
        return try {
            val cleanQuery = query.trim()
            val isbnQuery = normalizeIsbnQuery(cleanQuery)
            val subjectPart = if (!genero.isNullOrBlank() && genero != "Todos" && !looksLikeIsbn(cleanQuery)) {
                val subjectQuery = LibraryGenres.all
                    .firstOrNull { it.label == genero }
                    ?.subjectQuery
                    ?: genero.lowercase()
                " subject:$subjectQuery"
            } else {
                ""
            }

            val finalQuery = when {
                looksLikeIsbn(cleanQuery) -> "isbn:$isbnQuery"
                cleanQuery.isBlank() && subjectPart.isNotBlank() -> subjectPart.trim()
                cleanQuery.isNotBlank() -> "$cleanQuery$subjectPart"
                else -> "subject:fiction"
            }

            val response = searchGoogleBooks(
                query = finalQuery,
                maxResults = 40
            )

            val libros = response.items
                .map { it.toLibro(fallbackGenero = genero ?: "") }
                .localizeForCurrentLanguage()

            Result.success(libros)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchBooksByIsbn(isbn: String): Result<List<Libro>> {
        return try {
            val normalizedIsbn = normalizeIsbnQuery(isbn)
            if (normalizedIsbn.isBlank()) {
                return Result.success(emptyList())
            }

            val response = searchGoogleBooks(
                query = "isbn:$normalizedIsbn",
                maxResults = 10
            )

            val libros = response.items
                .map { it.toLibro() }
                .localizeForCurrentLanguage()

            Result.success(libros)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun marcarLibroComoLeido(libro: Libro): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val docId = safeBookDocId(libro.id.ifBlank { libro.isbn })
            if (docId == "unknown_book") {
                return Result.failure(Exception("El libro no tiene identificador valido"))
            }

            val fechaActual = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date())

            val libroLeido = LibroLeido(
                id = docId,
                isbn = docId,
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
                resena = "",
                contieneSpoilers = false,
                siNo = "si"
            )

            firestore.collection("usuarios")
                .document(uid)
                .collection("leidos")
                .document(docId)
                .set(libroLeido)
                .await()

            userListsRepository.syncBookIntoSystemReadList(libro).getOrThrow()

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
                document.toObject(LibroLeido::class.java)?.copy(id = document.id)
            }

            Result.success(libros)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarPuntuacion(bookId: String, puntuacion: Int): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            firestore.collection("usuarios")
                .document(uid)
                .collection("leidos")
                .document(safeBookDocId(bookId))
                .update("puntuacion", puntuacion)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarResenaLectura(
        bookId: String,
        puntuacion: Int,
        resena: String,
        contieneSpoilers: Boolean
    ): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            firestore.collection("usuarios")
                .document(uid)
                .collection("leidos")
                .document(safeBookDocId(bookId))
                .update(
                    mapOf(
                        "puntuacion" to puntuacion,
                        "resena" to resena.trim(),
                        "contieneSpoilers" to contieneSpoilers
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarLibroLeido(bookId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            firestore.collection("usuarios")
                .document(uid)
                .collection("leidos")
                .document(safeBookDocId(bookId))
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerLibroLeido(bookId: String): Result<LibroLeido?> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val snapshot = firestore.collection("usuarios")
                .document(uid)
                .collection("leidos")
                .document(safeBookDocId(bookId))
                .get()
                .await()

            val libroLeido = snapshot.toObject(LibroLeido::class.java)?.copy(id = snapshot.id)
            Result.success(libroLeido)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun List<Libro>.localizeForCurrentLanguage(): List<Libro> {
        val targetLanguage = LanguageManager.getCurrentLanguage()

        return map { book ->
            BookMetadataLocalizer.localize(
                book = book,
                targetLanguageTag = targetLanguage
            )
        }
    }
}
