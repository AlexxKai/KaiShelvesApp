package com.example.kaishelvesapp.data.repository

import com.example.kaishelvesapp.data.local.GuestLocalStore
import com.example.kaishelvesapp.data.local.DiscoverCatalogLocalStore
import com.example.kaishelvesapp.data.localization.BookMetadataLocalizer
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.LibroLeido
import com.example.kaishelvesapp.data.remote.googlebooks.GoogleBooksClient
import com.example.kaishelvesapp.data.remote.googlebooks.GoogleBooksResponse
import com.example.kaishelvesapp.data.remote.googlebooks.LibraryGenres
import com.example.kaishelvesapp.data.remote.googlebooks.toLibro
import com.example.kaishelvesapp.data.remote.inventaire.InventaireClient
import com.example.kaishelvesapp.data.remote.inventaire.inventaireBookFromResponse
import com.example.kaishelvesapp.data.remote.openlibrary.OpenLibraryClient
import com.example.kaishelvesapp.data.remote.openlibrary.toLibro
import com.example.kaishelvesapp.ui.language.LanguageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    enum class DiscoverCatalogMode(val cacheKey: String) {
        SPECIAL("special"),
        CURRENT("current"),
        TOP_RATED("top_rated"),
        KNOWN_AUTHORS("known_authors")
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

    private fun bookKey(book: Libro): String {
        val title = book.titulo.trim().lowercase(Locale.ROOT)
        val author = book.autor.trim().lowercase(Locale.ROOT)
        return book.id.ifBlank { book.isbn }
            .ifBlank { "$title-$author" }
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

    fun getCachedDiscoverBooks(mode: DiscoverCatalogMode): List<Libro> {
        return DiscoverCatalogLocalStore.read(
            languageTag = LanguageManager.getCurrentLanguage(),
            modeKey = mode.cacheKey
        )
    }

    suspend fun preloadDiscoverBooks(): Result<Unit> {
        return try {
            // La precarga no debe consumir cuota de Google Books antes de que el usuario busque.
            DiscoverCatalogMode.entries.forEach { mode ->
                DiscoverCatalogLocalStore.read(
                    languageTag = LanguageManager.getCurrentLanguage(),
                    modeKey = mode.cacheKey
                )
            }

            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun persistDiscoverBooks(mode: DiscoverCatalogMode, books: List<Libro>) {
        DiscoverCatalogLocalStore.write(
            languageTag = LanguageManager.getCurrentLanguage(),
            modeKey = mode.cacheKey,
            books = books
        )
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

    private suspend fun searchOpenLibraryByText(
        query: String,
        startIndex: Int,
        maxResults: Int
    ): BookSearchResult {
        val response = openLibraryApi.searchBooks(
            query = query,
            limit = maxResults,
            offset = startIndex
        )
        val books = response.docs
            .map { it.toLibro() }
            .filter { book -> book.titulo.isNotBlank() || book.autor.isNotBlank() }
            .distinctBy(::searchResultKey)

        return BookSearchResult(
            totalItems = response.numFound,
            books = books,
            hasMore = startIndex + maxResults < response.numFound
        )
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

    suspend fun obtenerLibros(
        mode: DiscoverCatalogMode = DiscoverCatalogMode.SPECIAL,
        previousBooks: List<Libro> = emptyList()
    ): Result<List<Libro>> {
        return try {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val recentYear = currentYear - 2
            val previousKeys = previousBooks.map(::bookKey).toSet()
            val refreshIndex = DiscoverCatalogLocalStore.nextRefreshIndex(
                languageTag = LanguageManager.getCurrentLanguage(),
                modeKey = mode.cacheKey
            )
            val discoverQueries = buildDiscoverQueries(mode, currentYear, recentYear, refreshIndex)

            val rawCandidates = discoverQueries
                .flatMapIndexed { index, query ->
                    runCatching {
                        searchGoogleBooks(
                            query = query.text,
                            maxResults = 40,
                            startIndex = discoverStartIndex(refreshIndex, index),
                            orderBy = query.orderBy
                        ).items
                    }.getOrDefault(emptyList())
                }
                .map { it.toLibro() }
                .filter { libro ->
                    libro.titulo.isNotBlank() &&
                        libro.autor.isNotBlank()
                }
                .distinctBy { libro ->
                    bookKey(libro)
                }
                // La mezcla final evita que el refresco vuelva a enseñar siempre los mismos primeros libros.
                .shuffled()
            val candidates = rawCandidates
                .filter { libro -> matchesDiscoverMode(libro, mode, recentYear, currentYear) }
                .takeIf { filteredBooks -> filteredBooks.size >= 8 }
                ?: rawCandidates

            val refreshedBooks = candidates
                .filterNot { libro -> bookKey(libro) in previousKeys }
                .take(40)
                .ifEmpty { candidates.drop((refreshIndex * 7) % candidates.size.coerceAtLeast(1)).take(40) }
                .localizeForCurrentLanguage()

            persistDiscoverBooks(mode, refreshedBooks)
            Result.success(refreshedBooks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildDiscoverQueries(
        mode: DiscoverCatalogMode,
        currentYear: Int,
        recentYear: Int,
        refreshIndex: Int
    ): List<DiscoverQuery> {
        val currentQueries = listOf(
            DiscoverQuery("$currentYear books", "newest"),
            DiscoverQuery("$currentYear novel", "newest"),
            DiscoverQuery("$currentYear literatura", "newest"),
            DiscoverQuery("$recentYear books", "newest"),
            DiscoverQuery("$recentYear novel", "newest"),
            DiscoverQuery("published $currentYear", "newest"),
            DiscoverQuery("subject:fiction $currentYear", "newest"),
            DiscoverQuery("subject:fantasy $currentYear", "newest"),
            DiscoverQuery("subject:mystery $currentYear", "newest"),
            DiscoverQuery("subject:romance $currentYear", "newest"),
            DiscoverQuery("subject:thriller $recentYear", "newest"),
            DiscoverQuery("subject:young adult $recentYear", "newest")
        )

        val topRatedQueries = listOf(
            DiscoverQuery("subject:fiction award winning"),
            DiscoverQuery("subject:fantasy bestseller"),
            DiscoverQuery("subject:mystery bestseller"),
            DiscoverQuery("subject:science fiction award"),
            DiscoverQuery("subject:historical fiction bestseller"),
            DiscoverQuery("modern classics fiction"),
            DiscoverQuery("goodreads choice awards fiction"),
            DiscoverQuery("hugely popular fantasy novel"),
            DiscoverQuery("critically acclaimed literary fiction"),
            DiscoverQuery("best books of the decade")
        )

        val knownAuthorQueries = listOf(
            "Brandon Sanderson",
            "Stephen King",
            "Agatha Christie",
            "Haruki Murakami",
            "Isabel Allende",
            "Carlos Ruiz Zafón",
            "Ursula K. Le Guin",
            "Neil Gaiman",
            "Jane Austen",
            "Gabriel García Márquez"
        ).map { author -> DiscoverQuery("inauthor:$author") }

        val specialQueries = currentQueries.take(4) + topRatedQueries.take(4) + knownAuthorQueries.take(6)

        val queries = when (mode) {
            DiscoverCatalogMode.SPECIAL -> specialQueries
            DiscoverCatalogMode.CURRENT -> currentQueries
            DiscoverCatalogMode.TOP_RATED -> topRatedQueries
            DiscoverCatalogMode.KNOWN_AUTHORS -> knownAuthorQueries
        }

        val offset = refreshIndex % queries.size.coerceAtLeast(1)
        return (queries.drop(offset) + queries.take(offset)).take(8)
    }

    private fun discoverStartIndex(refreshIndex: Int, queryIndex: Int): Int {
        val pages = listOf(0, 40, 80, 120, 160)
        return pages[(refreshIndex + queryIndex) % pages.size]
    }

    private fun matchesDiscoverMode(
        book: Libro,
        mode: DiscoverCatalogMode,
        recentYear: Int,
        currentYear: Int
    ): Boolean {
        return when (mode) {
            DiscoverCatalogMode.CURRENT ->
                book.fechaPublicacion in recentYear..currentYear

            DiscoverCatalogMode.TOP_RATED ->
                book.averageRating >= 3.8 || book.ratingsCount >= 20

            DiscoverCatalogMode.KNOWN_AUTHORS ->
                book.autor.isNotBlank()

            DiscoverCatalogMode.SPECIAL ->
                book.fechaPublicacion in recentYear..currentYear ||
                    book.averageRating >= 3.8 ||
                    book.ratingsCount >= 20 ||
                    book.genero.isNotBlank()
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
                // La consulta general suele recuperar mejor títulos y autores que las búsquedas exactas.
                val escapedQuery = cleanQuery.replace("\"", "")
                listOf(
                    cleanQuery,
                    "inauthor:\"$escapedQuery\"",
                    "intitle:\"$escapedQuery\""
                )
            }

            val responses = mutableListOf<GoogleBooksResponse>()
            var lastSearchError: Exception? = null

            for (googleQuery in googleQueries) {
                try {
                    val response = searchGoogleBooks(
                        query = googleQuery,
                        maxResults = maxResults,
                        startIndex = startIndex,
                        orderBy = if (sort == BookSearchSort.NEWEST) "newest" else null
                    )
                    responses += response

                    // Evita peticiones extra si la primera búsqueda ya trae una página completa.
                    if (response.items.size >= maxResults && startIndex == 0) break
                } catch (error: Exception) {
                    lastSearchError = error
                }
            }

            val queryTokens = cleanQuery
                .lowercase(Locale.ROOT)
                .split(Regex("\\s+"))
                .filter { it.length > 1 }

            val googleBooks = responses
                .flatMap { it.items }
                .map { it.toLibro() }
                .filter { it.titulo.isNotBlank() || it.autor.isNotBlank() }
                .distinctBy(::searchResultKey)
                .map { libro -> libro to searchRelevanceScore(libro, queryTokens, cleanQuery) }
                .filter { (_, score) -> score > 0 || looksLikeIsbn(cleanQuery) }
                .sortedWith(searchResultComparator(sort))
                .map { it.first }
                .take(80)

            if (googleBooks.isEmpty() && !looksLikeIsbn(cleanQuery)) {
                val fallbackResult = searchOpenLibraryByText(
                    query = cleanQuery,
                    startIndex = startIndex,
                    maxResults = maxResults
                )

                if (fallbackResult.books.isNotEmpty() || lastSearchError != null) {
                    return Result.success(fallbackResult)
                }
            }

            if (responses.isEmpty() && lastSearchError != null) {
                throw lastSearchError
            }

            val hasMore = responses.any { response ->
                response.items.isNotEmpty() && startIndex + maxResults < response.totalItems
            }
            Result.success(
                BookSearchResult(
                    totalItems = googleBooks.size,
                    books = googleBooks,
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

    suspend fun completarGenerosFaltantes(libros: List<Libro>): Result<List<Libro>> {
        return try {
            val enrichedBooks = libros.map { libro ->
                if (libro.genero.isNotBlank()) {
                    libro
                } else {
                    // Reutiliza las fuentes del catálogo para que las estadísticas no dependan de datos antiguos incompletos.
                    resolveGenreForBook(libro)?.let { genre -> libro.copy(genero = genre) } ?: libro
                }
            }

            Result.success(enrichedBooks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun resolveGenreForBook(libro: Libro): String? {
        val fromIsbn = libro.isbn
            .takeIf { it.isNotBlank() }
            ?.let { isbn ->
                searchBooksByIsbn(isbn)
                    .getOrDefault(emptyList())
                    .firstNotBlankGenre()
            }
        if (!fromIsbn.isNullOrBlank()) return fromIsbn

        val query = listOf(libro.titulo, libro.autor)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        if (query.isBlank()) return null

        return searchBooksForResults(query = query, maxResults = 10)
            .getOrDefault(BookSearchResult(totalItems = 0, books = emptyList()))
            .books
            .filter { candidate -> candidate.matchesBook(libro) }
            .firstNotBlankGenre()
    }

    private fun List<Libro>.firstNotBlankGenre(): String? {
        return firstOrNull { it.genero.isNotBlank() }?.genero
    }

    private fun Libro.matchesBook(other: Libro): Boolean {
        val sameIsbn = isbn.isNotBlank() && other.isbn.isNotBlank() &&
            isbn.equals(other.isbn, ignoreCase = true)
        val sameTitle = titulo.isNotBlank() && other.titulo.isNotBlank() &&
            titulo.trim().equals(other.titulo.trim(), ignoreCase = true)
        val sameAuthor = autor.isBlank() || other.autor.isBlank() ||
            autor.trim().equals(other.autor.trim(), ignoreCase = true) ||
            autor.trim().contains(other.autor.trim(), ignoreCase = true) ||
            other.autor.trim().contains(autor.trim(), ignoreCase = true)

        return sameIsbn || (sameTitle && sameAuthor)
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

private data class DiscoverQuery(
    val text: String,
    val orderBy: String? = null
)
