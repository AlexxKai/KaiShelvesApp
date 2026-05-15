package com.example.kaishelvesapp.data.repository

import com.example.kaishelvesapp.data.local.GuestLibraryState
import com.example.kaishelvesapp.data.local.AppContextProvider
import com.example.kaishelvesapp.data.local.GuestLocalStore
import com.example.kaishelvesapp.data.model.DeviceBookFormat
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.OwnedDeviceBook
import com.example.kaishelvesapp.data.model.UserBookList
import com.example.kaishelvesapp.data.model.UserBookTag
import com.example.kaishelvesapp.data.model.UserBookTagSummary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ImportedReadMetadata(
    val readDate: String = "",
    val rating: Int = 0,
    val review: String = "",
    val containsSpoilers: Boolean = false
)

class UserListsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    companion object {
        const val SYSTEM_LIST_WANT_TO_READ_ID = "system_want_to_read"
        const val SYSTEM_LIST_READING_ID = "system_reading"
        const val SYSTEM_LIST_READ_ID = "system_read"
        const val SYSTEM_LIST_UNFINISHED_ID = "system_unfinished"
        const val SYSTEM_LIST_PENDING_ID = "system_pending"
        const val SYSTEM_LIST_OWNED_ID = "system_owned"

        const val SYSTEM_LIST_WANT_TO_READ_KEY = "want_to_read"
        const val SYSTEM_LIST_READING_KEY = "reading"
        const val SYSTEM_LIST_READ_KEY = "read"
        const val SYSTEM_LIST_UNFINISHED_KEY = "unfinished"
        const val SYSTEM_LIST_PENDING_KEY = "pending"
        const val SYSTEM_LIST_OWNED_KEY = "owned"

        private var cachedListsOwnerId: String? = null
        private var cachedUserLists: List<UserBookList>? = null
        private var cachedTagsOwnerId: String? = null
        private var cachedUserTags: List<UserBookTag>? = null
        private val cachedUserListsState = MutableStateFlow<List<UserBookList>>(emptyList())
    }

    private val systemListIds = setOf(
        SYSTEM_LIST_WANT_TO_READ_ID,
        SYSTEM_LIST_READING_ID,
        SYSTEM_LIST_READ_ID,
        SYSTEM_LIST_UNFINISHED_ID,
        SYSTEM_LIST_PENDING_ID,
        SYSTEM_LIST_OWNED_ID
    )

    private val systemListPriority = listOf(
        SYSTEM_LIST_READ_ID,
        SYSTEM_LIST_READING_ID,
        SYSTEM_LIST_PENDING_ID,
        SYSTEM_LIST_UNFINISHED_ID,
        SYSTEM_LIST_WANT_TO_READ_ID
    )

    private val assignableListIds = systemListIds - SYSTEM_LIST_OWNED_ID

    private fun requireUid(): String {
        return auth.currentUser?.uid
            ?: GuestLocalStore.getActiveProfile()?.uid
            ?: throw IllegalStateException("Usuario no autenticado")
    }

    private fun currentOwnerIdOrNull(): String? {
        return auth.currentUser?.uid ?: GuestLocalStore.getActiveProfile()?.uid
    }

    fun currentLibraryOwnerId(): String {
        return currentOwnerIdOrNull() ?: "anonymous"
    }

    private fun isGuestSessionActive(): Boolean {
        return auth.currentUser == null && GuestLocalStore.isSessionActive()
    }

    private fun updateCachedListCounts(
        ownerId: String?,
        addedListIds: Set<String>,
        removedListIds: Set<String>
    ) {
        if (cachedListsOwnerId != ownerId || cachedUserLists == null) return

        val updatedLists = cachedUserLists?.map { list ->
            val delta = (if (list.id in addedListIds) 1 else 0) - (if (list.id in removedListIds) 1 else 0)
            if (delta == 0) {
                list
            } else {
                list.copy(bookCount = (list.bookCount + delta).coerceAtLeast(0))
            }
        }
        cachedUserLists = updatedLists
        cachedUserListsState.value = updatedLists.orEmpty()
    }

    fun observeCachedUserLists(): StateFlow<List<UserBookList>> = cachedUserListsState.asStateFlow()

    private fun localBookId(book: Libro): String {
        return safeBookDocId(book.id.ifBlank { book.isbn })
    }

    private fun localStoredBook(libro: Libro, safeBookId: String): Libro {
        return libro.copy(id = safeBookId)
    }

    private fun localReadDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun deviceLibraryRepository(): DeviceLibraryRepository {
        return DeviceLibraryRepository(AppContextProvider.requireContext())
    }

    private fun ownedDeviceBooks(): List<OwnedDeviceBook> {
        val records = deviceLibraryRepository().getBookRecords()
        return records
            // Agrupa el mismo libro local cuando existe en varios formatos dentro de la biblioteca.
            .groupBy { record ->
                "${record.title.ifBlank { record.name.substringBeforeLast('.') }.trim().lowercase()}|" +
                    record.author.trim().lowercase()
            }
            .values
            .map { group ->
                val first = group.minBy { it.importedAtMillis }
                val title = first.title.ifBlank { first.name.substringBeforeLast('.') }
                val formats = group.map { it.format }.distinct().sortedBy { it.name }
                OwnedDeviceBook(
                    book = Libro(
                        id = first.id,
                        titulo = title,
                        autor = first.author,
                        pdf = first.uri
                    ),
                    formats = formats,
                    uri = first.uri,
                    name = first.name,
                    location = first.location,
                    mimeType = first.mimeType,
                    sizeBytes = first.sizeBytes,
                    modifiedAtMillis = first.modifiedAtMillis
                )
            }
            .sortedBy { it.book.titulo.lowercase() }
    }

    private fun ownedSystemList(): UserBookList {
        val books = ownedDeviceBooks()
        return UserBookList(
            id = SYSTEM_LIST_OWNED_ID,
            name = "Tengo",
            description = "Libros detectados automaticamente en la biblioteca del dispositivo.",
            bookCount = books.size,
            position = 5,
            isSystem = true,
            systemKey = SYSTEM_LIST_OWNED_KEY
        )
    }

    private fun localGuestLists(state: GuestLibraryState): List<UserBookList> {
        return state.lists
            .map { list ->
                if (list.id == SYSTEM_LIST_OWNED_ID) {
                    return@map ownedSystemList()
                }
                val books = state.listBooks[list.id].orEmpty()
                list.copy(
                    bookCount = books.size,
                    previewImageUrls = books.take(3).mapNotNull { it.imagen.takeIf(String::isNotBlank) }
                )
            }
            .sortedWith(
                compareBy<UserBookList> { !it.isSystem }
                    .thenBy { it.position }
                    .thenBy { it.name.lowercase() }
            )
    }

    private fun localGuestTagSummaries(state: GuestLibraryState): List<UserBookTagSummary> {
        val booksById = state.listBooks.values
            .flatten()
            .distinctBy(::localBookId)
            .associateBy(::localBookId)

        return state.tags
            .sortedWith(compareBy<UserBookTag> { it.position }.thenBy { it.name.lowercase() })
            .map { tag ->
                val bookIds = state.bookTagIds
                    .filterValues { tag.id in it }
                    .keys
                    .toList()
                UserBookTagSummary(
                    tag = tag,
                    bookCount = bookIds.size,
                    previewImageUrls = bookIds.mapNotNull { bookId ->
                        booksById[bookId]?.imagen?.takeIf(String::isNotBlank)
                    }.take(3)
                )
            }
    }

    private fun localNormalizeSelectedListIds(
        state: GuestLibraryState,
        selectedListIds: Set<String>
    ): Set<String> {
        val candidateIds = selectedListIds.filter { it in assignableListIds }
        if (candidateIds.isEmpty()) return emptySet()

        val selectedSystemId = systemListPriority.firstOrNull { candidateIds.contains(it) }
        if (selectedSystemId != null) {
            return setOf(selectedSystemId)
        }

        val customLists = localGuestLists(state)
            .filterNot { it.isSystem }
        val selectedCustom = customLists.firstOrNull { it.id in candidateIds }
        return selectedCustom?.let { setOf(it.id) } ?: candidateIds.firstOrNull()?.let(::setOf).orEmpty()
    }

    private fun localSelectedListIds(state: GuestLibraryState, safeBookId: String): Set<String> {
        return state.listBooks.mapNotNull { (listId, books) ->
            listId.takeIf { books.any { localBookId(it) == safeBookId } }
        }.toSet()
    }

    private fun localReadBookPayload(
        libro: Libro,
        safeBookId: String,
        readMetadata: ImportedReadMetadata? = null
    ) = mapOf(
        "id" to safeBookId,
        "isbn" to libro.isbn,
        "titulo" to libro.titulo,
        "autor" to libro.autor,
        "editorial" to libro.editorial,
        "genero" to libro.genero,
        "fechaPublicacion" to libro.fechaPublicacion,
        "paginas" to libro.paginas,
        "imagen" to libro.imagen,
        "pdf" to libro.pdf,
        "fechaLeido" to readMetadata?.readDate.orEmpty().ifBlank { localReadDate() },
        "puntuacion" to (readMetadata?.rating ?: 0),
        "resena" to readMetadata?.review.orEmpty(),
        "contieneSpoilers" to (readMetadata?.containsSpoilers ?: false),
        "siNo" to "si"
    )

    private fun ensureLocalReadBook(
        state: GuestLibraryState,
        libro: Libro,
        safeBookId: String,
        readMetadata: ImportedReadMetadata? = null
    ): GuestLibraryState {
        if (state.readBooks.any { it.id == safeBookId }) {
            if (readMetadata == null) return state
            return state.copy(
                readBooks = state.readBooks.map { readBook ->
                    if (readBook.id == safeBookId) {
                        readBook.copy(
                            fechaLeido = readMetadata.readDate.ifBlank { readBook.fechaLeido },
                            puntuacion = readMetadata.rating.takeIf { it > 0 } ?: readBook.puntuacion,
                            resena = readMetadata.review.ifBlank { readBook.resena },
                            contieneSpoilers = readMetadata.containsSpoilers
                        )
                    } else {
                        readBook
                    }
                }
            )
        }

        val readBook = com.example.kaishelvesapp.data.model.LibroLeido(
            id = safeBookId,
            isbn = libro.isbn,
            titulo = libro.titulo,
            autor = libro.autor,
            editorial = libro.editorial,
            genero = libro.genero,
            fechaPublicacion = libro.fechaPublicacion,
            paginas = libro.paginas,
            imagen = libro.imagen,
            pdf = libro.pdf,
            fechaLeido = readMetadata?.readDate.orEmpty().ifBlank { localReadDate() },
            puntuacion = readMetadata?.rating ?: 0,
            resena = readMetadata?.review.orEmpty(),
            contieneSpoilers = readMetadata?.containsSpoilers ?: false,
            siNo = "si"
        )

        return state.copy(readBooks = state.readBooks + readBook)
    }

    private fun userListsCollection(uid: String) = firestore.collection("usuarios")
        .document(uid)
        .collection("listas")

    private fun userReadCollection(uid: String) = firestore.collection("usuarios")
        .document(uid)
        .collection("leidos")

    private fun userTagsCollection(uid: String) = firestore.collection("usuarios")
        .document(uid)
        .collection("etiquetas")

    private fun userBookMetadataCollection(uid: String) = firestore.collection("usuarios")
        .document(uid)
        .collection("libros_metadata")

    private fun safeBookDocId(rawId: String): String {
        return rawId
            .trim()
            .ifBlank { "unknown_book" }
            .replace("/", "_")
    }

    private suspend fun normalizeSelectedListIds(
        uid: String,
        selectedListIds: Set<String>
    ): Set<String> {
        val candidateIds = selectedListIds.filter { it in assignableListIds || !systemListIds.contains(it) }
        if (candidateIds.isEmpty()) return emptySet()

        val selectedSystemId = systemListPriority.firstOrNull { candidateIds.contains(it) }
        if (selectedSystemId != null) {
            return setOf(selectedSystemId)
        }

        val customLists = userListsCollection(uid)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject(UserBookList::class.java)?.copy(id = document.id)
            }
            .filterNot { it.isSystem }
            .sortedWith(compareBy<UserBookList> { it.position }.thenBy { it.name.lowercase() })

        val selectedCustom = customLists.firstOrNull { it.id in candidateIds }
        return selectedCustom?.let { setOf(it.id) } ?: candidateIds.firstOrNull()?.let(::setOf).orEmpty()
    }

    private suspend fun pruneBookAssignmentsToSingleList(
        uid: String,
        safeBookId: String,
        keepListIds: Set<String>
    ) {
        val allListsSnapshot = userListsCollection(uid).get().await()
        val batch = firestore.batch()
        var changed = false

        allListsSnapshot.documents.forEach { document ->
            val listId = document.id
            if (listId == SYSTEM_LIST_OWNED_ID || keepListIds.contains(listId)) return@forEach

            val bookRef = document.reference.collection("libros").document(safeBookId)
            if (bookRef.get().await().exists()) {
                batch.delete(bookRef)
                batch.update(document.reference, "bookCount", FieldValue.increment(-1))
                changed = true
            }
        }

        if (changed) {
            batch.commit().await()
        }
    }

    private fun buildBookPayload(libro: Libro, safeBookId: String): Map<String, Any> {
        return mapOf(
            "id" to safeBookId,
            "isbn" to libro.isbn,
            "titulo" to libro.titulo,
            "autor" to libro.autor,
            "editorial" to libro.editorial,
            "genero" to libro.genero,
            "fechaPublicacion" to libro.fechaPublicacion,
            "paginas" to libro.paginas,
            "imagen" to libro.imagen,
            "pdf" to libro.pdf
        )
    }

    private fun buildReadBookPayload(
        libro: Libro,
        safeBookId: String,
        readMetadata: ImportedReadMetadata? = null
    ): Map<String, Any> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return buildBookPayload(libro, safeBookId) + mapOf(
            "fechaLeido" to readMetadata?.readDate.orEmpty().ifBlank { today },
            "puntuacion" to (readMetadata?.rating ?: 0),
            "resena" to readMetadata?.review.orEmpty(),
            "contieneSpoilers" to (readMetadata?.containsSpoilers ?: false),
            "siNo" to "si"
        )
    }

    private fun defaultSystemLists() = listOf(
        UserBookList(
            id = SYSTEM_LIST_WANT_TO_READ_ID,
            name = "Quiero leer",
            description = "Libros que quieres empezar pronto.",
            position = 0,
            isSystem = true,
            systemKey = SYSTEM_LIST_WANT_TO_READ_KEY
        ),
        UserBookList(
            id = SYSTEM_LIST_READING_ID,
            name = "Leyendo",
            description = "Libros que tienes ahora mismo entre manos.",
            position = 1,
            isSystem = true,
            systemKey = SYSTEM_LIST_READING_KEY
        ),
        UserBookList(
            id = SYSTEM_LIST_READ_ID,
            name = "Leido",
            description = "Libros que ya forman parte de tu historial de lectura.",
            position = 2,
            isSystem = true,
            systemKey = SYSTEM_LIST_READ_KEY
        ),
        UserBookList(
            id = SYSTEM_LIST_PENDING_ID,
            name = "Pendientes",
            description = "Libros que quieres ordenar a tu manera para retomarlos después.",
            position = 3,
            isSystem = true,
            systemKey = SYSTEM_LIST_PENDING_KEY
        ),
        UserBookList(
            id = SYSTEM_LIST_UNFINISHED_ID,
            name = "No terminado",
            description = "Libros que dejaste a medias o prefieres pausar.",
            position = 4,
            isSystem = true,
            systemKey = SYSTEM_LIST_UNFINISHED_KEY
        ),
        UserBookList(
            id = SYSTEM_LIST_OWNED_ID,
            name = "Tengo",
            description = "Libros detectados automaticamente en la biblioteca del dispositivo.",
            position = 5,
            isSystem = true,
            systemKey = SYSTEM_LIST_OWNED_KEY
        )
    )

    fun getCachedUserListsOrDefault(): List<UserBookList> {
        val ownerId = currentOwnerIdOrNull()
        return cachedUserLists
            ?.takeIf { cachedListsOwnerId == ownerId }
            ?: defaultSystemLists()
    }

    fun getCachedUserTags(): List<UserBookTag> {
        val ownerId = currentOwnerIdOrNull()
        return cachedUserTags
            ?.takeIf { cachedTagsOwnerId == ownerId }
            .orEmpty()
    }

    private suspend fun ensureDefaultLists(uid: String) {
        val batch = firestore.batch()
        var changed = false

        defaultSystemLists().forEach { defaultList ->
            val ref = userListsCollection(uid).document(defaultList.id)
            val snapshot = ref.get().await()
            val currentCount = snapshot.getLong("bookCount")?.toInt() ?: 0

            if (!snapshot.exists()) {
                batch.set(ref, defaultList.copy(bookCount = currentCount))
                changed = true
            } else {
                val needsRefresh =
                    snapshot.getString("name") != defaultList.name ||
                        snapshot.getString("description") != defaultList.description ||
                        snapshot.getLong("position")?.toInt() != defaultList.position ||
                        snapshot.getBoolean("isSystem") != true ||
                        snapshot.getString("systemKey") != defaultList.systemKey

                if (needsRefresh) {
                    batch.set(ref, defaultList.copy(bookCount = currentCount))
                    changed = true
                }
            }
        }

        if (changed) {
            batch.commit().await()
        }
    }

    suspend fun getUserLists(): Result<List<UserBookList>> {
        return try {
            if (isGuestSessionActive()) {
                val lists = localGuestLists(GuestLocalStore.readState())
                cachedListsOwnerId = currentOwnerIdOrNull()
                cachedUserLists = lists
                cachedUserListsState.value = lists
                return Result.success(lists)
            }

            val uid = requireUid()
            ensureDefaultLists(uid)

            val snapshot = userListsCollection(uid)
                .get()
                .await()

            val lists = snapshot.documents
                .mapNotNull { document ->
                    val storedList = document.toObject(UserBookList::class.java) ?: return@mapNotNull null
                    if (document.id == SYSTEM_LIST_OWNED_ID) {
                        return@mapNotNull ownedSystemList()
                    }
                    val previewSnapshot = document.reference
                        .collection("libros")
                        .limit(3)
                        .get()
                        .await()

                    storedList.copy(
                        id = document.id,
                        previewImageUrls = previewSnapshot.documents.mapNotNull { previewDocument ->
                            previewDocument.getString("imagen")?.takeIf { it.isNotBlank() }
                        }
                    )
                }
                .sortedWith(
                    compareBy<UserBookList> { !it.isSystem }
                        .thenBy { it.position }
                        .thenBy { it.name.lowercase() }
                )

            cachedListsOwnerId = uid
            cachedUserLists = lists
            cachedUserListsState.value = lists
            Result.success(lists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getListById(listId: String): Result<UserBookList?> {
        return try {
            if (listId == SYSTEM_LIST_OWNED_ID) {
                return Result.success(ownedSystemList())
            }

            if (isGuestSessionActive()) {
                val state = GuestLocalStore.readState()
                return Result.success(localGuestLists(state).firstOrNull { it.id == listId })
            }

            val uid = requireUid()
            ensureDefaultLists(uid)

            val snapshot = userListsCollection(uid)
                .document(listId)
                .get()
                .await()

            Result.success(
                snapshot.toObject(UserBookList::class.java)?.copy(id = snapshot.id)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBooksInList(listId: String): Result<List<Libro>> {
        return try {
            if (listId == SYSTEM_LIST_OWNED_ID) {
                return Result.success(ownedDeviceBooks().map { it.book })
            }

            if (isGuestSessionActive()) {
                val storedBooks = GuestLocalStore.readState()
                    .listBooks[listId]
                    .orEmpty()
                val books = if (listId == SYSTEM_LIST_PENDING_ID) {
                    storedBooks
                } else {
                    storedBooks.sortedBy { it.titulo.lowercase() }
                }
                return Result.success(books)
            }

            val uid = requireUid()
            ensureDefaultLists(uid)

            val snapshot = userListsCollection(uid)
                .document(listId)
                .collection("libros")
                .get()
                .await()

            val sortedDocuments = if (listId == SYSTEM_LIST_PENDING_ID) {
                snapshot.documents.sortedWith(
                    compareBy<com.google.firebase.firestore.DocumentSnapshot> {
                        it.getLong("listPosition") ?: Long.MAX_VALUE
                    }.thenBy { it.getString("titulo").orEmpty().lowercase() }
                )
            } else {
                snapshot.documents.sortedBy { it.getString("titulo").orEmpty().lowercase() }
            }

            val books = sortedDocuments.mapNotNull { document ->
                document.toObject(Libro::class.java)?.copy(
                    id = document.getString("id").orEmpty().ifBlank { document.id }
                )
            }

            Result.success(books)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSelectedListIdsForBook(bookId: String): Result<Set<String>> {
        return try {
            if (isGuestSessionActive()) {
                val safeBookId = safeBookDocId(bookId)
                if (safeBookId == "unknown_book") {
                    return Result.success(emptySet())
                }

                val state = GuestLocalStore.readState()
                val selectedIds = localSelectedListIds(state, safeBookId)
                val normalizedIds = localNormalizeSelectedListIds(state, selectedIds)

                if (selectedIds != normalizedIds) {
                    GuestLocalStore.updateState { currentState ->
                        val mutableListBooks = currentState.listBooks.toMutableMap()
                        mutableListBooks.keys.forEach { listId ->
                            val books = mutableListBooks[listId].orEmpty()
                            mutableListBooks[listId] = if (listId in normalizedIds) {
                                books
                            } else {
                                books.filterNot { localBookId(it) == safeBookId }
                            }
                        }
                        currentState.copy(listBooks = mutableListBooks)
                    }
                }

                return Result.success(normalizedIds)
            }

            val uid = requireUid()
            ensureDefaultLists(uid)

            val safeBookId = safeBookDocId(bookId)
            if (safeBookId == "unknown_book") {
                return Result.success(emptySet())
            }

            val snapshot = userListsCollection(uid)
                .get()
                .await()

            val selectedIds = snapshot.documents.mapNotNull { document ->
                if (document.id == SYSTEM_LIST_OWNED_ID) return@mapNotNull null
                val exists = document.reference
                    .collection("libros")
                    .document(safeBookId)
                    .get()
                    .await()
                    .exists()

                document.id.takeIf { exists }
            }.toSet()

            val normalizedIds = normalizeSelectedListIds(uid, selectedIds)
            if (selectedIds.size > 1 || selectedIds != normalizedIds) {
                pruneBookAssignmentsToSingleList(uid, safeBookId, normalizedIds)
            }

            Result.success(normalizedIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserTags(): Result<List<UserBookTag>> {
        return try {
            if (isGuestSessionActive()) {
                val tags = GuestLocalStore.readState()
                    .tags
                    .sortedWith(compareBy<UserBookTag> { it.position }.thenBy { it.name.lowercase() })
                cachedTagsOwnerId = currentOwnerIdOrNull()
                cachedUserTags = tags
                return Result.success(tags)
            }

            val uid = requireUid()
            val tags = userTagsCollection(uid)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(UserBookTag::class.java)?.copy(id = document.id)
                }
                .sortedWith(compareBy<UserBookTag> { it.position }.thenBy { it.name.lowercase() })

            cachedTagsOwnerId = uid
            cachedUserTags = tags
            Result.success(tags)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getOwnedDeviceBooks(): Result<List<OwnedDeviceBook>> {
        return try {
            // "Tengo" se calcula al vuelo para no duplicar archivos locales en Firestore ni en listas manuales.
            Result.success(ownedDeviceBooks())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserTagSummaries(): Result<List<UserBookTagSummary>> {
        return try {
            if (isGuestSessionActive()) {
                return Result.success(localGuestTagSummaries(GuestLocalStore.readState()))
            }

            val uid = requireUid()
            val tags = getUserTags().getOrThrow()
            val metadataDocuments = userBookMetadataCollection(uid)
                .get()
                .await()
                .documents

            val allBooksById = userListsCollection(uid)
                .get()
                .await()
                .documents
                .flatMap { listDocument ->
                    listDocument.reference
                        .collection("libros")
                        .get()
                        .await()
                        .documents
                }
                .mapNotNull { bookDocument ->
                    bookDocument.toObject(Libro::class.java)?.copy(
                        id = bookDocument.getString("id").orEmpty().ifBlank { bookDocument.id }
                    )
                }
                .distinctBy { book -> safeBookDocId(book.id.ifBlank { book.isbn }) }
                .associateBy { book -> safeBookDocId(book.id.ifBlank { book.isbn }) }

            val summaries = tags.map { tag ->
                val bookIds = metadataDocuments
                    .filter { document ->
                        val tagIds = document.get("tagIds") as? List<*>
                        tag.id in tagIds.orEmpty().filterIsInstance<String>()
                    }
                    .map { it.id }

                UserBookTagSummary(
                    tag = tag,
                    bookCount = bookIds.size,
                    previewImageUrls = bookIds.mapNotNull { bookId ->
                        allBooksById[bookId]?.imagen?.takeIf(String::isNotBlank)
                    }.take(3)
                )
            }

            Result.success(summaries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTagById(tagId: String): Result<UserBookTag?> {
        return try {
            if (isGuestSessionActive()) {
                return Result.success(GuestLocalStore.readState().tags.firstOrNull { it.id == tagId })
            }

            val uid = requireUid()
            val snapshot = userTagsCollection(uid)
                .document(tagId)
                .get()
                .await()

            Result.success(snapshot.toObject(UserBookTag::class.java)?.copy(id = snapshot.id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBooksInTag(tagId: String): Result<List<Libro>> {
        return try {
            if (isGuestSessionActive()) {
                val state = GuestLocalStore.readState()
                val taggedBookIds = state.bookTagIds
                    .filterValues { tagId in it }
                    .keys
                    .toSet()
                val books = state.listBooks.values
                    .flatten()
                    .distinctBy(::localBookId)
                    .filter { localBookId(it) in taggedBookIds }
                    .sortedBy { it.titulo.lowercase() }
                return Result.success(books)
            }

            val uid = requireUid()
            val taggedBookIds = userBookMetadataCollection(uid)
                .get()
                .await()
                .documents
                .filter { document ->
                    val tagIds = document.get("tagIds") as? List<*>
                    tagId in tagIds.orEmpty().filterIsInstance<String>()
                }
                .map { it.id }
                .toSet()

            val listBooks = userListsCollection(uid)
                .get()
                .await()
                .documents
                .flatMap { listDocument ->
                    listDocument.reference
                        .collection("libros")
                        .get()
                        .await()
                        .documents
                }
                .mapNotNull { bookDocument ->
                    bookDocument.toObject(Libro::class.java)?.copy(
                        id = bookDocument.getString("id").orEmpty().ifBlank { bookDocument.id }
                    )
                }
                .distinctBy { book -> safeBookDocId(book.id.ifBlank { book.isbn }) }
                .filter { book -> safeBookDocId(book.id.ifBlank { book.isbn }) in taggedBookIds }
            val metadataBooks = userBookMetadataCollection(uid)
                .get()
                .await()
                .documents
                .filter { it.id in taggedBookIds }
                .mapNotNull { metadataDocument ->
                    metadataDocument.toObject(Libro::class.java)?.copy(
                        id = metadataDocument.getString("id").orEmpty().ifBlank { metadataDocument.id }
                    )
                }

            val books = (listBooks + metadataBooks)
                .distinctBy { book -> safeBookDocId(book.id.ifBlank { book.isbn }) }
                .sortedBy { it.titulo.lowercase() }

            Result.success(books)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSelectedTagIdsForBook(bookId: String): Result<Set<String>> {
        return try {
            if (isGuestSessionActive()) {
                val safeBookId = safeBookDocId(bookId)
                if (safeBookId == "unknown_book") {
                    return Result.success(emptySet())
                }

                val selected = GuestLocalStore.readState()
                    .bookTagIds[safeBookId]
                    .orEmpty()
                    .toSet()
                return Result.success(selected)
            }

            val uid = requireUid()
            val safeBookId = safeBookDocId(bookId)
            if (safeBookId == "unknown_book") {
                return Result.success(emptySet())
            }

            val snapshot = userBookMetadataCollection(uid)
                .document(safeBookId)
                .get()
                .await()

            val tagIds = snapshot.get("tagIds") as? List<*>
            Result.success(tagIds.orEmpty().filterIsInstance<String>().toSet())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createList(name: String, description: String): Result<Unit> {
        return try {
            if (isGuestSessionActive()) {
                GuestLocalStore.updateState { currentState ->
                    val currentPositions = currentState.lists
                        .filterNot { it.isSystem }
                        .map { it.position }
                    val nextPosition = (currentPositions.maxOrNull() ?: 2) + 1
                    currentState.copy(
                        lists = currentState.lists + UserBookList(
                            id = UUID.randomUUID().toString(),
                            name = name.trim(),
                            description = description.trim(),
                            bookCount = 0,
                            position = nextPosition
                        )
                    )
                }
                return Result.success(Unit)
            }

            val uid = requireUid()
            ensureDefaultLists(uid)

            val newDocument = userListsCollection(uid).document()
            val currentPositions = userListsCollection(uid)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(UserBookList::class.java) }
                .filterNot { it.isSystem }
                .map { it.position }
            val nextPosition = (currentPositions.maxOrNull() ?: 2) + 1

            val newList = UserBookList(
                id = newDocument.id,
                name = name.trim(),
                description = description.trim(),
                bookCount = 0,
                position = nextPosition
            )

            newDocument.set(newList).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrCreateCustomList(name: String, description: String): Result<String> {
        val trimmedName = name.trim()
        val trimmedDescription = description.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("El nombre de la lista no puede estar vacio"))
        }

        return try {
            if (isGuestSessionActive()) {
                var targetId = ""
                GuestLocalStore.updateState { currentState ->
                    val existingList = currentState.lists.firstOrNull { list ->
                        !list.isSystem && list.name.equals(trimmedName, ignoreCase = true)
                    }
                    if (existingList != null) {
                        targetId = existingList.id
                        return@updateState currentState
                    }

                    val currentPositions = currentState.lists
                        .filterNot { it.isSystem }
                        .map { it.position }
                    val nextPosition = (currentPositions.maxOrNull() ?: 2) + 1
                    val newList = UserBookList(
                        id = UUID.randomUUID().toString(),
                        name = trimmedName,
                        description = trimmedDescription,
                        bookCount = 0,
                        position = nextPosition
                    )
                    targetId = newList.id
                    currentState.copy(lists = currentState.lists + newList)
                }
                cachedListsOwnerId = null
                cachedUserLists = null
                return Result.success(targetId)
            }

            val uid = requireUid()
            ensureDefaultLists(uid)

            val existingList = userListsCollection(uid)
                .get()
                .await()
                .documents
                .firstNotNullOfOrNull { document ->
                    val list = document.toObject(UserBookList::class.java)
                    document.id.takeIf {
                        list?.isSystem != true && list?.name.equals(trimmedName, ignoreCase = true)
                    }
                }
            if (existingList != null) {
                return Result.success(existingList)
            }

            val newDocument = userListsCollection(uid).document()
            val currentPositions = userListsCollection(uid)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(UserBookList::class.java) }
                .filterNot { it.isSystem }
                .map { it.position }
            val nextPosition = (currentPositions.maxOrNull() ?: 2) + 1
            val newList = UserBookList(
                id = newDocument.id,
                name = trimmedName,
                description = trimmedDescription,
                bookCount = 0,
                position = nextPosition
            )

            newDocument.set(newList).await()
            cachedListsOwnerId = null
            cachedUserLists = null
            Result.success(newDocument.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun nextListPosition(uid: String, listId: String): Long {
        return userListsCollection(uid)
            .document(listId)
            .collection("libros")
            .get()
            .await()
            .documents
            .mapNotNull { it.getLong("listPosition") }
            .maxOrNull()
            ?.plus(1L)
            ?: 0L
    }

    suspend fun updateBookAssignments(
        libro: Libro,
        selectedListIds: Set<String>
    ): Result<Unit> {
        return updateBookAssignments(
            libro = libro,
            selectedListIds = selectedListIds,
            readMetadata = null
        )
    }

    suspend fun updateBookAssignments(
        libro: Libro,
        selectedListIds: Set<String>,
        readMetadata: ImportedReadMetadata?
    ): Result<Unit> {
        return try {
            if (isGuestSessionActive()) {
                val safeBookId = safeBookDocId(libro.id.ifBlank { libro.isbn })
                if (safeBookId == "unknown_book") {
                    return Result.failure(IllegalArgumentException("El libro no tiene identificador valido"))
                }

                var localIdsToAdd = emptySet<String>()
                var localIdsToRemove = emptySet<String>()
                GuestLocalStore.updateState { currentState ->
                    val normalizedSelectedIds = localNormalizeSelectedListIds(currentState, selectedListIds)
                    val currentSelectedIds = localSelectedListIds(currentState, safeBookId)
                    localIdsToAdd = normalizedSelectedIds - currentSelectedIds
                    localIdsToRemove = currentSelectedIds - normalizedSelectedIds
                    val mutableListBooks = currentState.listBooks.toMutableMap()
                    val storedBook = localStoredBook(libro, safeBookId)

                    (currentSelectedIds - normalizedSelectedIds).forEach { listId ->
                        mutableListBooks[listId] = mutableListBooks[listId]
                            .orEmpty()
                            .filterNot { localBookId(it) == safeBookId }
                    }

                    (normalizedSelectedIds - currentSelectedIds).forEach { listId ->
                        val books = mutableListBooks[listId].orEmpty()
                            .filterNot { localBookId(it) == safeBookId } + storedBook
                        mutableListBooks[listId] = books
                    }

                    val updatedState = currentState.copy(listBooks = mutableListBooks)
                    if (normalizedSelectedIds.contains(SYSTEM_LIST_READ_ID)) {
                        ensureLocalReadBook(updatedState, libro, safeBookId, readMetadata)
                    } else {
                        updatedState
                    }
                }
                updateCachedListCounts(
                    ownerId = currentOwnerIdOrNull(),
                    addedListIds = localIdsToAdd,
                    removedListIds = localIdsToRemove
                )
                return Result.success(Unit)
            }

            val uid = requireUid()
            ensureDefaultLists(uid)

            val safeBookId = safeBookDocId(libro.id.ifBlank { libro.isbn })
            if (safeBookId == "unknown_book") {
                return Result.failure(IllegalArgumentException("El libro no tiene identificador valido"))
            }

            val normalizedSelectedIds = normalizeSelectedListIds(uid, selectedListIds)

            val allListsSnapshot = userListsCollection(uid)
                .get()
                .await()

            val currentSelectedIds = allListsSnapshot.documents.mapNotNull { document ->
                if (document.id == SYSTEM_LIST_OWNED_ID) return@mapNotNull null
                val exists = document.reference
                    .collection("libros")
                    .document(safeBookId)
                    .get()
                    .await()
                    .exists()

                document.id.takeIf { exists }
            }.toSet()

            val idsToAdd = normalizedSelectedIds - currentSelectedIds
            val idsToRemove = currentSelectedIds - normalizedSelectedIds

            if (idsToAdd.isEmpty() && idsToRemove.isEmpty()) {
                return Result.success(Unit)
            }

            val batch = firestore.batch()
            val bookPayload = buildBookPayload(libro, safeBookId)

            idsToAdd.forEach { listId ->
                val listRef = userListsCollection(uid).document(listId)
                val bookRef = listRef.collection("libros").document(safeBookId)
                val alreadyExists = bookRef.get().await().exists()
                val payload = if (listId == SYSTEM_LIST_PENDING_ID && !alreadyExists) {
                    bookPayload + mapOf(
                        "activityAt" to FieldValue.serverTimestamp(),
                        "listPosition" to nextListPosition(uid, listId)
                    )
                } else {
                    bookPayload + mapOf("activityAt" to FieldValue.serverTimestamp())
                }
                batch.set(bookRef, payload, SetOptions.merge())
                if (!alreadyExists) {
                    batch.update(listRef, "bookCount", FieldValue.increment(1))
                }

                if (listId == SYSTEM_LIST_READ_ID) {
                    val readDocRef = userReadCollection(uid).document(safeBookId)
                    val readDocExists = readDocRef.get().await().exists()
                    if (readDocExists) {
                        batch.set(
                            readDocRef,
                            if (readMetadata == null) {
                                buildBookPayload(libro, safeBookId) + mapOf("activityAt" to FieldValue.serverTimestamp())
                            } else {
                                buildReadBookPayload(libro, safeBookId, readMetadata) +
                                    mapOf("activityAt" to FieldValue.serverTimestamp())
                            },
                            SetOptions.merge()
                        )
                    } else {
                        batch.set(
                            readDocRef,
                            buildReadBookPayload(libro, safeBookId, readMetadata) +
                                mapOf("activityAt" to FieldValue.serverTimestamp())
                        )
                    }
                }
            }

            idsToRemove.forEach { listId ->
                val listRef = userListsCollection(uid).document(listId)
                val bookRef = listRef.collection("libros").document(safeBookId)
                val alreadyExists = bookRef.get().await().exists()
                if (alreadyExists) {
                    batch.delete(bookRef)
                    batch.update(listRef, "bookCount", FieldValue.increment(-1))
                }

            }

            batch.commit().await()
            updateCachedListCounts(
                ownerId = uid,
                addedListIds = idsToAdd,
                removedListIds = idsToRemove
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateList(listId: String, name: String, description: String): Result<Unit> {
        return try {
            if (isGuestSessionActive()) {
                GuestLocalStore.updateState { currentState ->
                    val updatedLists = currentState.lists.map { list ->
                        if (list.id != listId) {
                            list
                        } else if (list.isSystem) {
                            throw IllegalArgumentException("No se pueden editar las listas del sistema")
                        } else {
                            list.copy(
                                name = name.trim(),
                                description = description.trim()
                            )
                        }
                    }
                    currentState.copy(lists = updatedLists)
                }
                return Result.success(Unit)
            }

            val uid = requireUid()
            ensureDefaultLists(uid)

            val existingList = userListsCollection(uid)
                .document(listId)
                .get()
                .await()
                .toObject(UserBookList::class.java)

            if (existingList?.isSystem == true) {
                return Result.failure(IllegalArgumentException("No se pueden editar las listas del sistema"))
            }

            userListsCollection(uid)
                .document(listId)
                .update(
                    mapOf(
                        "name" to name.trim(),
                        "description" to description.trim()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateListOrder(orderedListIds: List<String>): Result<Unit> {
        return try {
            if (isGuestSessionActive()) {
                GuestLocalStore.updateState { currentState ->
                    val systemLists = currentState.lists.filter { it.isSystem }
                    val customListsById = currentState.lists
                        .filterNot { it.isSystem }
                        .associateBy { it.id }
                    val orderedCustomIds = orderedListIds.filter { customListsById.containsKey(it) }
                    val missingCustomIds = customListsById.keys.filterNot { orderedCustomIds.contains(it) }
                    val finalCustomIds = orderedCustomIds + missingCustomIds
                    val reorderedCustomLists = finalCustomIds.mapIndexedNotNull { index, listId ->
                        customListsById[listId]?.copy(position = index + systemLists.size)
                    }
                    currentState.copy(lists = systemLists + reorderedCustomLists)
                }
                return Result.success(Unit)
            }

            val uid = requireUid()
            ensureDefaultLists(uid)

            val customListsById = userListsCollection(uid)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(UserBookList::class.java)?.copy(id = document.id)
                }
                .filterNot { it.isSystem }
                .associateBy { it.id }

            val orderedCustomIds = orderedListIds.filter { customListsById.containsKey(it) }
            val missingCustomIds = customListsById.keys.filterNot { orderedCustomIds.contains(it) }
            val finalCustomIds = orderedCustomIds + missingCustomIds
            val batch = firestore.batch()

            finalCustomIds.forEachIndexed { index, listId ->
                batch.update(
                    userListsCollection(uid).document(listId),
                    "position",
                    index + defaultSystemLists().size
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteList(listId: String): Result<Unit> {
        return try {
            if (isGuestSessionActive()) {
                GuestLocalStore.updateState { currentState ->
                    val targetList = currentState.lists.firstOrNull { it.id == listId }
                    if (targetList?.isSystem == true) {
                        throw IllegalArgumentException("No se pueden eliminar las listas del sistema")
                    }

                    currentState.copy(
                        lists = currentState.lists.filterNot { it.id == listId },
                        listBooks = currentState.listBooks - listId
                    )
                }
                return Result.success(Unit)
            }

            val uid = requireUid()
            val listRef = userListsCollection(uid).document(listId)
            val existingList = listRef.get().await().toObject(UserBookList::class.java)

            if (existingList?.isSystem == true) {
                return Result.failure(IllegalArgumentException("No se pueden eliminar las listas del sistema"))
            }

            val booksSnapshot = listRef.collection("libros").get().await()
            val batch = firestore.batch()

            booksSnapshot.documents.forEach { document ->
                batch.delete(document.reference)
            }
            batch.delete(listRef)
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeBookFromList(listId: String, bookId: String): Result<Unit> {
        return try {
            if (isGuestSessionActive()) {
                val safeBookId = safeBookDocId(bookId)
                if (safeBookId == "unknown_book") {
                    return Result.failure(IllegalArgumentException("El libro no tiene identificador valido"))
                }

                GuestLocalStore.updateState { currentState ->
                    val targetList = currentState.lists.firstOrNull { it.id == listId }
                    val updatedBooks = currentState.listBooks.toMutableMap()
                    updatedBooks[listId] = updatedBooks[listId]
                        .orEmpty()
                        .filterNot { localBookId(it) == safeBookId }

                    val updatedReads = if (targetList?.systemKey == SYSTEM_LIST_READ_KEY) {
                        currentState.readBooks.filterNot { it.id == safeBookId }
                    } else {
                        currentState.readBooks
                    }

                    currentState.copy(
                        listBooks = updatedBooks,
                        readBooks = updatedReads
                    )
                }
                return Result.success(Unit)
            }

            val uid = requireUid()
            ensureDefaultLists(uid)

            val safeBookId = safeBookDocId(bookId)
            if (safeBookId == "unknown_book") {
                return Result.failure(IllegalArgumentException("El libro no tiene identificador valido"))
            }

            val listRef = userListsCollection(uid).document(listId)
            val existingList = listRef.get().await().toObject(UserBookList::class.java)
            val bookRef = listRef.collection("libros").document(safeBookId)
            val bookSnapshot = bookRef.get().await()

            if (!bookSnapshot.exists()) {
                return Result.success(Unit)
            }

            val batch = firestore.batch()
            batch.delete(bookRef)
            batch.update(listRef, "bookCount", FieldValue.increment(-1))
            if (existingList?.systemKey == SYSTEM_LIST_READ_KEY) {
                batch.delete(userReadCollection(uid).document(safeBookId))
            }
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBooksInListOrder(listId: String, orderedBookIds: List<String>): Result<Unit> {
        return try {
            if (listId != SYSTEM_LIST_PENDING_ID) {
                return Result.success(Unit)
            }

            if (isGuestSessionActive()) {
                GuestLocalStore.updateState { currentState ->
                    val currentBooks = currentState.listBooks[listId].orEmpty()
                    val booksById = currentBooks.associateBy { localBookId(it) }
                    val normalizedOrderedIds = orderedBookIds
                        .map(::safeBookDocId)
                        .filter { booksById.containsKey(it) }
                    val missingBooks = currentBooks.filterNot { localBookId(it) in normalizedOrderedIds }
                    val reorderedBooks = normalizedOrderedIds.mapNotNull { booksById[it] } + missingBooks
                    currentState.copy(
                        listBooks = currentState.listBooks + (listId to reorderedBooks)
                    )
                }
                return Result.success(Unit)
            }

            val uid = requireUid()
            ensureDefaultLists(uid)

            val listRef = userListsCollection(uid).document(listId)
            val currentBooks = listRef.collection("libros").get().await().documents
            val currentIds = currentBooks.map { it.id }.toSet()
            val normalizedOrderedIds = orderedBookIds
                .map(::safeBookDocId)
                .filter { it in currentIds }
            val missingIds = currentBooks.map { it.id }.filterNot { it in normalizedOrderedIds }
            val finalIds = normalizedOrderedIds + missingIds
            val batch = firestore.batch()

            finalIds.forEachIndexed { index, bookId ->
                batch.update(
                    listRef.collection("libros").document(bookId),
                    "listPosition",
                    index
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncBookIntoSystemReadList(libro: Libro): Result<Unit> {
        return try {
            if (isGuestSessionActive()) {
                val safeBookId = safeBookDocId(libro.id.ifBlank { libro.isbn })
                if (safeBookId == "unknown_book") {
                    return Result.failure(IllegalArgumentException("El libro no tiene identificador valido"))
                }

                GuestLocalStore.updateState { currentState ->
                    val storedBook = localStoredBook(libro, safeBookId)
                    val updatedListBooks = currentState.listBooks.toMutableMap().apply {
                        keys.forEach { listId ->
                            this[listId] = this[listId]
                                .orEmpty()
                                .filterNot { localBookId(it) == safeBookId }
                        }
                        this[SYSTEM_LIST_READ_ID] = this[SYSTEM_LIST_READ_ID]
                            .orEmpty()
                            .filterNot { localBookId(it) == safeBookId } + storedBook
                    }

                    ensureLocalReadBook(
                        currentState.copy(listBooks = updatedListBooks),
                        libro,
                        safeBookId
                    )
                }
                return Result.success(Unit)
            }

            val uid = requireUid()
            ensureDefaultLists(uid)

            val safeBookId = safeBookDocId(libro.id.ifBlank { libro.isbn })
            if (safeBookId == "unknown_book") {
                return Result.failure(IllegalArgumentException("El libro no tiene identificador valido"))
            }

            val readListRef = userListsCollection(uid).document(SYSTEM_LIST_READ_ID)
            val bookRef = readListRef.collection("libros").document(safeBookId)
            val alreadyExists = bookRef.get().await().exists()
            val batch = firestore.batch()

            userListsCollection(uid)
                .get()
                .await()
                .documents
                .filter { it.id != SYSTEM_LIST_READ_ID }
                .forEach { document ->
                    val otherBookRef = document.reference.collection("libros").document(safeBookId)
                    if (otherBookRef.get().await().exists()) {
                        batch.delete(otherBookRef)
                        batch.update(document.reference, "bookCount", FieldValue.increment(-1))
                    }
                }

            batch.set(bookRef, buildBookPayload(libro, safeBookId))
            if (!alreadyExists) {
                batch.update(readListRef, "bookCount", FieldValue.increment(1))
            }
            val readDocRef = userReadCollection(uid).document(safeBookId)
            val readDocExists = readDocRef.get().await().exists()
            if (readDocExists) {
                batch.set(readDocRef, buildBookPayload(libro, safeBookId), SetOptions.merge())
            } else {
                batch.set(readDocRef, buildReadBookPayload(libro, safeBookId))
            }
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBookTags(bookId: String, tagIds: Set<String>): Result<Unit> {
        return updateBookTags(bookId = bookId, libro = null, tagIds = tagIds)
    }

    suspend fun updateBookTags(libro: Libro, tagIds: Set<String>): Result<Unit> {
        return updateBookTags(
            bookId = libro.id.ifBlank { libro.isbn },
            libro = libro,
            tagIds = tagIds
        )
    }

    private suspend fun updateBookTags(
        bookId: String,
        libro: Libro?,
        tagIds: Set<String>
    ): Result<Unit> {
        return try {
            if (isGuestSessionActive()) {
                val safeBookId = safeBookDocId(bookId)
                if (safeBookId == "unknown_book") {
                    return Result.failure(IllegalArgumentException("El libro no tiene identificador valido"))
                }

                GuestLocalStore.updateState { currentState ->
                    val updatedBookTagIds = currentState.bookTagIds.toMutableMap()
                    val currentSelectedListIds = localSelectedListIds(currentState, safeBookId)
                    if (tagIds.isEmpty() && currentSelectedListIds.isEmpty()) {
                        updatedBookTagIds.remove(safeBookId)
                    } else {
                        updatedBookTagIds[safeBookId] = tagIds.toList()
                    }
                    currentState.copy(bookTagIds = updatedBookTagIds)
                }
                return Result.success(Unit)
            }

            val uid = requireUid()
            val safeBookId = safeBookDocId(bookId)
            if (safeBookId == "unknown_book") {
                return Result.failure(IllegalArgumentException("El libro no tiene identificador valido"))
            }

            val metadataRef = userBookMetadataCollection(uid).document(safeBookId)
            if (tagIds.isEmpty()) {
                val currentSelectedListIds = getSelectedListIdsForBook(safeBookId).getOrDefault(emptySet())
                if (currentSelectedListIds.isEmpty()) {
                    metadataRef.delete().await()
                } else {
                    metadataRef.set(mapOf("tagIds" to emptyList<String>()), com.google.firebase.firestore.SetOptions.merge()).await()
                }
            } else {
                val payload = libro
                    ?.let { buildBookPayload(it, safeBookId) }
                    .orEmpty() + mapOf("tagIds" to tagIds.toList())
                metadataRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearBookOrganization(bookId: String): Result<Unit> {
        return try {
            if (isGuestSessionActive()) {
                val safeBookId = safeBookDocId(bookId)
                if (safeBookId == "unknown_book") {
                    return Result.failure(IllegalArgumentException("El libro no tiene identificador valido"))
                }

                var removedListIds = emptySet<String>()
                GuestLocalStore.updateState { currentState ->
                    removedListIds = localSelectedListIds(currentState, safeBookId)
                    val updatedListBooks = currentState.listBooks.mapValues { (_, books) ->
                        books.filterNot { localBookId(it) == safeBookId }
                    }

                    currentState.copy(
                        listBooks = updatedListBooks,
                        bookTagIds = currentState.bookTagIds - safeBookId,
                        readBooks = currentState.readBooks.filterNot { it.id == safeBookId }
                    )
                }
                updateCachedListCounts(
                    ownerId = currentOwnerIdOrNull(),
                    addedListIds = emptySet(),
                    removedListIds = removedListIds
                )
                return Result.success(Unit)
            }

            val uid = requireUid()
            val safeBookId = safeBookDocId(bookId)
            if (safeBookId == "unknown_book") {
                return Result.failure(IllegalArgumentException("El libro no tiene identificador valido"))
            }

            val allListsSnapshot = userListsCollection(uid).get().await()
            val batch = firestore.batch()
            val removedListIds = mutableSetOf<String>()

            allListsSnapshot.documents.forEach { document ->
                val bookRef = document.reference.collection("libros").document(safeBookId)
                if (bookRef.get().await().exists()) {
                    removedListIds += document.id
                    batch.delete(bookRef)
                    batch.update(document.reference, "bookCount", FieldValue.increment(-1))
                }
            }

            batch.delete(userBookMetadataCollection(uid).document(safeBookId))
            batch.delete(userReadCollection(uid).document(safeBookId))
            batch.commit().await()
            updateCachedListCounts(
                ownerId = uid,
                addedListIds = emptySet(),
                removedListIds = removedListIds
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTag(name: String): Result<Unit> {
        return try {
            if (isGuestSessionActive()) {
                GuestLocalStore.updateState { currentState ->
                    val nextPosition = (currentState.tags.maxOfOrNull { it.position } ?: -1) + 1
                    currentState.copy(
                        tags = currentState.tags + UserBookTag(
                            id = UUID.randomUUID().toString(),
                            name = name.trim(),
                            position = nextPosition
                        )
                    )
                }
                return Result.success(Unit)
            }

            val uid = requireUid()
            val newDocument = userTagsCollection(uid).document()
            val nextPosition = (userTagsCollection(uid)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(UserBookTag::class.java)?.position }
                .maxOrNull() ?: -1) + 1

            newDocument.set(
                UserBookTag(
                    id = newDocument.id,
                    name = name.trim(),
                    position = nextPosition
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTag(tagId: String): Result<Unit> {
        return try {
            if (isGuestSessionActive()) {
                GuestLocalStore.updateState { currentState ->
                    currentState.copy(
                        tags = currentState.tags.filterNot { it.id == tagId },
                        bookTagIds = currentState.bookTagIds.mapValues { (_, tagIds) ->
                            tagIds.filterNot { it == tagId }
                        }.filterValues { it.isNotEmpty() }
                    )
                }
                return Result.success(Unit)
            }

            val uid = requireUid()
            val metadataSnapshot = userBookMetadataCollection(uid).get().await()
            val batch = firestore.batch()

            // Limpia referencias para que la etiqueta borrada no quede asociada a ningun libro.
            metadataSnapshot.documents.forEach { document ->
                val tagIds = (document.get("tagIds") as? List<*>)
                    .orEmpty()
                    .filterIsInstance<String>()
                if (tagId in tagIds) {
                    batch.set(
                        document.reference,
                        mapOf("tagIds" to tagIds.filterNot { it == tagId }),
                        SetOptions.merge()
                    )
                }
            }

            batch.delete(userTagsCollection(uid).document(tagId))
            batch.commit().await()
            cachedUserTags = cachedUserTags?.filterNot { it.id == tagId }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
