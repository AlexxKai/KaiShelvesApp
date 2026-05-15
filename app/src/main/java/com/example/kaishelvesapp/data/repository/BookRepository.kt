package com.example.kaishelvesapp.data.repository

import com.example.kaishelvesapp.data.local.GuestLocalStore
import com.example.kaishelvesapp.data.localization.BookMetadataLocalizer
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.LibroLeido
import com.example.kaishelvesapp.data.remote.googlebooks.GoogleBooksClient
import com.example.kaishelvesapp.data.remote.googlebooks.LibraryGenres
import com.example.kaishelvesapp.data.remote.googlebooks.toLibro
import com.example.kaishelvesapp.data.remote.inventaire.InventaireClient
import com.example.kaishelvesapp.data.remote.inventaire.inventaireBookFromResponse
import com.example.kaishelvesapp.data.remote.openlibrary.OpenLibraryClient
import com.example.kaishelvesapp.data.remote.openlibrary.toLibro
import com.example.kaishelvesapp.ui.language.LanguageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class BookRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userListsRepository: UserListsRepository = UserListsRepository(firestore, auth)
) {

    private val api = GoogleBooksClient.api
    private val publicApi = GoogleBooksClient.publicApi
    private val inventaireApi = InventaireClient.api
    private val openLibraryApi = OpenLibraryClient.api

    data class BookSearchResult(
        val totalItems: Int,
        val books: List<Libro>,
        val hasMore: Boolean = false
    )

    enum class BookSearchSort {
        NEWEST,
        RATING
    }

    private fun safeBookDocId(rawId: String): String {
        return rawId
            .trim()
            .ifBlank { "unknown_book" }
            .replace("/", "_")
    }

    private fun isGuestSessionActive(): Boolean {
        return auth.currentUser == null && GuestLocalStore.isSessionActive()
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

    private fun searchRelevanceScore(book: Libro, queryTokens: List<String>, rawQuery: String): Int {
        if (queryTokens.isEmpty()) return 1

        val normalizedQuery = rawQuery.lowercase(Locale.ROOT)
        val title = book.titulo.lowercase(Locale.ROOT)
        val author = book.autor.lowercase(Locale.ROOT)
        val publisher = book.editorial.lowercase(Locale.ROOT)
        val isbn = book.isbn.lowercase(Locale.ROOT)
        val searchableText = "$title $author $publisher $isbn"
        val matchedTokens = queryTokens.count { token -> token in searchableText }

        if (matchedTokens == 0) return 0

        var score = matchedTokens * 12
        if (normalizedQuery in author) score += 60
        if (normalizedQuery in title) score += 45
        if (queryTokens.all { token -> token in author }) score += 35
        if (queryTokens.all { token -> token in title }) score += 25
        if (book.imagen.isNotBlank()) score += 4
        if (book.fechaPublicacion != 0) score += 2

        return score
    }

    private fun searchResultKey(book: Libro): String {
        val title = book.titulo.trim().lowercase(Locale.ROOT)
        val author = book.autor.trim().lowercase(Locale.ROOT)
        return if (title.isNotBlank() && author.isNotBlank()) {
            "$title-$author"
        } else {
            book.id.ifBlank { book.isbn.ifBlank { "$title-$author" } }
        }
    }

    private fun searchResultComparator(sort: BookSearchSort): Comparator<Pair<Libro, Int>> {
        return when (sort) {
            BookSearchSort.NEWEST -> compareByDescending<Pair<Libro, Int>> { it.first.fechaPublicacion }
                .thenByDescending { it.second }
                .thenByDescending { it.first.averageRating }
                .thenBy { it.first.titulo.lowercase(Locale.ROOT) }

            BookSearchSort.RATING -> compareByDescending<Pair<Libro, Int>> { it.first.averageRating }
                .thenByDescending { it.first.ratingsCount }
                .thenByDescending { it.second }
                .thenByDescending { it.first.fechaPublicacion }
                .thenBy { it.first.titulo.lowercase(Locale.ROOT) }
        }
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
        return GoogleBooksClient.hasApiKey && httpException.code() in setOf(403, 429, 500, 502, 503, 504)
    }

    private fun Throwable.shouldRetryGoogleBooksRequest(): Boolean {
        val httpException = this as? HttpException ?: return false
        return httpException.code() in setOf(429, 500, 502, 503, 504)
    }

    private suspend fun <T> retryGoogleBooksRequest(block: suspend () -> T): T {
        var lastError: Exception? = null

        repeat(3) { attempt ->
            try {
                return block()
            } catch (error: Exception) {
                lastError = error
                if (!error.shouldRetryGoogleBooksRequest() || attempt == 2) {
                    throw error
                }
                delay(350L * (attempt + 1))
            }
        }

        throw lastError ?: IllegalStateException("No se pudo completar la peticion a Google Books")
    }

    private suspend fun searchGoogleBooks(
        query: String,
        maxResults: Int,
        startIndex: Int = 0,
        orderBy: String? = null
    ) = try {
        retryGoogleBooksRequest {
            api.searchBooks(
                query = query,
                maxResults = maxResults,
                startIndex = startIndex,
                orderBy = orderBy,
                langRestrict = currentGoogleBooksLanguage()
            )
        }
    } catch (error: Exception) {
        if (!error.shouldRetryGoogleBooksWithoutApiKey()) {
            throw error
        }

        retryGoogleBooksRequest {
            publicApi.searchBooks(
                query = query,
                maxResults = maxResults,
                startIndex = startIndex,
                orderBy = orderBy,
                langRestrict = currentGoogleBooksLanguage()
            )
        }
    }

    private suspend fun searchGoogleBooksPublicOnce(
        query: String,
        maxResults: Int,
        startIndex: Int = 0,
        orderBy: String? = null,
        langRestrict: String? = currentGoogleBooksLanguage()
    ) = publicApi.searchBooks(
        query = query,
        maxResults = maxResults,
        startIndex = startIndex,
        orderBy = orderBy,
        langRestrict = langRestrict
    )

    private suspend fun searchOpenLibraryByIsbn(isbn: String): Libro? {
        val normalizedIsbn = normalizeIsbnQuery(isbn)
        if (normalizedIsbn.isBlank()) return null

        val response = openLibraryApi.getBooksByBibkeys(
            bibkeys = "ISBN:$normalizedIsbn"
        )

        return response["ISBN:$normalizedIsbn"]
            ?.toLibro(normalizedIsbn)
            ?.takeIf { book ->
                book.titulo.isNotBlank() || book.autor.isNotBlank() || book.imagen.isNotBlank()
            }
    }

    private suspend fun searchInventaireByIsbn(isbn: String): Libro? {
        val normalizedIsbn = normalizeIsbnQuery(isbn)
        if (normalizedIsbn.isBlank()) return null

        val response = inventaireApi.getEntitiesByUris(
            uris = "isbn:$normalizedIsbn",
            refresh = false
        )

        return inventaireBookFromResponse(response, normalizedIsbn)
            ?.takeIf { book ->
                book.titulo.isNotBlank() || book.autor.isNotBlank()
            }
    }

    suspend fun obtenerLibros(): Result<List<Libro>> {
        return try {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val recentYear = currentYear - 1
            val authorSeeds = listOf(
                "inauthor:a",
                "inauthor:e",
                "inauthor:i",
                "inauthor:o",
                "inauthor:u",
                "inauthor:an",
                "inauthor:ma",
                "inauthor:jo",
                "inauthor:la",
                "inauthor:ca"
            ).shuffled().take(4)
            val newestQueries = listOf(
                currentYear.toString(),
                "published $currentYear",
                "$currentYear books",
                "$currentYear novel",
                "$currentYear literatura",
                "$currentYear libro",
                "$recentYear books",
                "$recentYear novel",
                "$recentYear literatura"
            )
                .shuffled()
                .take(5) + authorSeeds

            val libros = newestQueries
                .flatMap { query ->
                    searchGoogleBooks(
                        query = query,
                        maxResults = 40,
                        startIndex = listOf(0, 40, 80).random(),
                        orderBy = "newest"
                    ).items
                }
                .map { it.toLibro() }
                .filter { libro ->
                    libro.titulo.isNotBlank() &&
                        libro.autor.isNotBlank() &&
                        libro.fechaPublicacion in recentYear..currentYear
                }
                .distinctBy { libro ->
                    libro.id.ifBlank {
                        libro.isbn.ifBlank {
                            "${libro.titulo.lowercase()}-${libro.autor.lowercase()}"
                        }
                    }
                }
                .sortedWith(
                    compareByDescending<Libro> { it.fechaPublicacion }
                        .thenBy { it.titulo.lowercase() }
                )
                .let { sortedBooks ->
                    val randomOffset = if (sortedBooks.size > 40) {
                        Random.nextInt(0, (sortedBooks.size - 40).coerceAtMost(12) + 1)
                    } else {
                        0
                    }

                    sortedBooks.drop(randomOffset).take(40)
                }
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

    suspend fun searchBooksForResults(
        query: String,
        sort: BookSearchSort = BookSearchSort.NEWEST,
        startIndex: Int = 0,
        maxResults: Int = 40
    ): Result<BookSearchResult> {
        return try {
            val cleanQuery = query.trim()
            if (cleanQuery.isBlank()) {
                return Result.success(BookSearchResult(totalItems = 0, books = emptyList()))
            }

            val isbnQuery = normalizeIsbnQuery(cleanQuery)
            val googleQueries = if (looksLikeIsbn(cleanQuery)) {
                listOf("isbn:$isbnQuery")
            } else {
                val escapedQuery = cleanQuery.replace("\"", "")
                listOf(
                    "\"$escapedQuery\"",
                    "inauthor:\"$escapedQuery\"",
                    "intitle:\"$escapedQuery\"",
                    cleanQuery
                )
            }

            val responses = coroutineScope {
                googleQueries.map { googleQuery ->
                    async {
                        runCatching {
                            searchGoogleBooks(
                                query = googleQuery,
                                maxResults = maxResults,
                                startIndex = startIndex,
                                orderBy = if (sort == BookSearchSort.NEWEST) "newest" else null
                            )
                        }.getOrNull()
                    }
                }.mapNotNull { it.await() }
            }

            val queryTokens = cleanQuery
                .lowercase(Locale.ROOT)
                .split(Regex("\\s+"))
                .filter { it.length > 1 }

            val libros = responses
                .flatMap { it.items }
                .map { it.toLibro() }
                .filter { it.titulo.isNotBlank() || it.autor.isNotBlank() }
                .distinctBy(::searchResultKey)
                .map { libro -> libro to searchRelevanceScore(libro, queryTokens, cleanQuery) }
                .filter { (_, score) -> score > 0 || looksLikeIsbn(cleanQuery) }
                .sortedWith(searchResultComparator(sort))
                .map { it.first }
                .take(80)

            val hasMore = responses.any { response ->
                response.items.isNotEmpty() && startIndex + maxResults < response.totalItems
            }
            Result.success(
                BookSearchResult(
                    totalItems = libros.size,
                    books = libros,
                    hasMore = hasMore
                )
            )
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

            searchInventaireByIsbn(normalizedIsbn)?.let { inventaireBook ->
                return Result.success(listOf(inventaireBook))
            }

            searchOpenLibraryByIsbn(normalizedIsbn)?.let { openLibraryBook ->
                return Result.success(listOf(openLibraryBook))
            }

            val response = if (GoogleBooksClient.hasApiKey) {
                api.searchBooks(
                    query = "isbn:$normalizedIsbn",
                    maxResults = 10,
                    startIndex = 0,
                    langRestrict = currentGoogleBooksLanguage()
                )
            } else {
                searchGoogleBooksPublicOnce(
                    query = "isbn:$normalizedIsbn",
                    maxResults = 10
                )
            }

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
            if (isGuestSessionActive()) {
                userListsRepository.syncBookIntoSystemReadList(libro).getOrThrow()
                return Result.success(Unit)
            }

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
            if (isGuestSessionActive()) {
                return Result.success(
                    GuestLocalStore.readState().readBooks
                )
            }

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
            if (isGuestSessionActive()) {
                val safeBookId = safeBookDocId(bookId)
                GuestLocalStore.updateState { currentState ->
                    currentState.copy(
                        readBooks = currentState.readBooks.map { readBook ->
                            if (readBook.id == safeBookId) {
                                readBook.copy(puntuacion = puntuacion)
                            } else {
                                readBook
                            }
                        }
                    )
                }
                return Result.success(Unit)
            }

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
            if (isGuestSessionActive()) {
                val safeBookId = safeBookDocId(bookId)
                GuestLocalStore.updateState { currentState ->
                    currentState.copy(
                        readBooks = currentState.readBooks.map { readBook ->
                            if (readBook.id == safeBookId) {
                                readBook.copy(
                                    puntuacion = puntuacion,
                                    resena = resena.trim(),
                                    contieneSpoilers = contieneSpoilers
                                )
                            } else {
                                readBook
                            }
                        }
                    )
                }
                return Result.success(Unit)
            }

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
            if (isGuestSessionActive()) {
                val safeBookId = safeBookDocId(bookId)
                GuestLocalStore.updateState { currentState ->
                    currentState.copy(
                        readBooks = currentState.readBooks.filterNot { it.id == safeBookId }
                    )
                }
                return Result.success(Unit)
            }

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
            if (isGuestSessionActive()) {
                val safeBookId = safeBookDocId(bookId)
                return Result.success(
                    GuestLocalStore.readState().readBooks.firstOrNull { it.id == safeBookId }
                )
            }

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
