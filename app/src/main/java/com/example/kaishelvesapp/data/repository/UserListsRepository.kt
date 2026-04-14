package com.example.kaishelvesapp.data.repository

import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.UserBookList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserListsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    companion object {
        const val SYSTEM_LIST_WANT_TO_READ_ID = "system_want_to_read"
        const val SYSTEM_LIST_READING_ID = "system_reading"
        const val SYSTEM_LIST_READ_ID = "system_read"

        const val SYSTEM_LIST_WANT_TO_READ_KEY = "want_to_read"
        const val SYSTEM_LIST_READING_KEY = "reading"
        const val SYSTEM_LIST_READ_KEY = "read"
    }

    private fun requireUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Usuario no autenticado")
    }

    private fun userListsCollection(uid: String) = firestore.collection("usuarios")
        .document(uid)
        .collection("listas")

    private fun userReadCollection(uid: String) = firestore.collection("usuarios")
        .document(uid)
        .collection("leidos")

    private fun safeBookDocId(rawId: String): String {
        return rawId
            .trim()
            .ifBlank { "unknown_book" }
            .replace("/", "_")
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
        )
    )

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

    private suspend fun syncReadListWithLegacyCollection(uid: String) {
        val readListRef = userListsCollection(uid).document(SYSTEM_LIST_READ_ID)
        val legacyBooks = userReadCollection(uid).get().await().documents.associateBy { it.id }
        val listBooks = readListRef.collection("libros").get().await().documents.associateBy { it.id }
        val batch = firestore.batch()
        var changed = false

        legacyBooks.forEach { (bookId, document) ->
            val payload = mapOf(
                "id" to bookId,
                "isbn" to document.getString("isbn").orEmpty().ifBlank { bookId },
                "titulo" to document.getString("titulo").orEmpty(),
                "autor" to document.getString("autor").orEmpty(),
                "editorial" to document.getString("editorial").orEmpty(),
                "genero" to document.getString("genero").orEmpty(),
                "fechaPublicacion" to (document.getLong("fechaPublicacion")?.toInt() ?: 0),
                "paginas" to (document.getLong("paginas")?.toInt() ?: 0),
                "imagen" to document.getString("imagen").orEmpty(),
                "pdf" to document.getString("pdf").orEmpty()
            )
            if (listBooks[bookId]?.data != payload) {
                batch.set(readListRef.collection("libros").document(bookId), payload)
                changed = true
            }
        }

        listBooks.keys
            .filterNot { legacyBooks.containsKey(it) }
            .forEach { bookId ->
                batch.delete(readListRef.collection("libros").document(bookId))
                changed = true
            }

        val currentCount = readListRef.get().await().getLong("bookCount")?.toInt() ?: -1
        if (currentCount != legacyBooks.size) {
            batch.update(readListRef, "bookCount", legacyBooks.size)
            changed = true
        }

        if (changed) {
            batch.commit().await()
        }
    }

    suspend fun getUserLists(): Result<List<UserBookList>> {
        return try {
            val uid = requireUid()
            ensureDefaultLists(uid)
            syncReadListWithLegacyCollection(uid)

            val snapshot = userListsCollection(uid)
                .get()
                .await()

            val lists = snapshot.documents
                .mapNotNull { document ->
                    val storedList = document.toObject(UserBookList::class.java) ?: return@mapNotNull null
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

            Result.success(lists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getListById(listId: String): Result<UserBookList?> {
        return try {
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
            val uid = requireUid()
            ensureDefaultLists(uid)
            if (listId == SYSTEM_LIST_READ_ID) {
                syncReadListWithLegacyCollection(uid)
            }

            val snapshot = userListsCollection(uid)
                .document(listId)
                .collection("libros")
                .get()
                .await()

            val books = snapshot.documents.mapNotNull { document ->
                document.toObject(Libro::class.java)?.copy(
                    id = document.getString("id").orEmpty().ifBlank { document.id }
                )
            }.sortedBy { it.titulo.lowercase() }

            Result.success(books)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSelectedListIdsForBook(bookId: String): Result<Set<String>> {
        return try {
            val uid = requireUid()
            ensureDefaultLists(uid)
            syncReadListWithLegacyCollection(uid)

            val safeBookId = safeBookDocId(bookId)
            if (safeBookId == "unknown_book") {
                return Result.success(emptySet())
            }

            val snapshot = userListsCollection(uid)
                .get()
                .await()

            val selectedIds = snapshot.documents.mapNotNull { document ->
                val exists = document.reference
                    .collection("libros")
                    .document(safeBookId)
                    .get()
                    .await()
                    .exists()

                document.id.takeIf { exists }
            }.toSet()

            Result.success(selectedIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createList(name: String, description: String): Result<Unit> {
        return try {
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

    suspend fun updateBookAssignments(
        libro: Libro,
        selectedListIds: Set<String>
    ): Result<Unit> {
        return try {
            val uid = requireUid()
            ensureDefaultLists(uid)
            syncReadListWithLegacyCollection(uid)

            val safeBookId = safeBookDocId(libro.id.ifBlank { libro.isbn })
            if (safeBookId == "unknown_book") {
                return Result.failure(IllegalArgumentException("El libro no tiene identificador valido"))
            }

            val allListsSnapshot = userListsCollection(uid)
                .get()
                .await()

            val currentSelectedIds = allListsSnapshot.documents.mapNotNull { document ->
                val exists = document.reference
                    .collection("libros")
                    .document(safeBookId)
                    .get()
                    .await()
                    .exists()

                document.id.takeIf { exists }
            }.toSet()

            val idsToAdd = selectedListIds - currentSelectedIds
            val idsToRemove = currentSelectedIds - selectedListIds

            if (idsToAdd.isEmpty() && idsToRemove.isEmpty()) {
                return Result.success(Unit)
            }

            val batch = firestore.batch()
            val bookPayload = mapOf(
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

            idsToAdd.forEach { listId ->
                val listRef = userListsCollection(uid).document(listId)
                val bookRef = listRef.collection("libros").document(safeBookId)
                val alreadyExists = bookRef.get().await().exists()
                batch.set(bookRef, bookPayload)
                if (!alreadyExists) {
                    batch.update(listRef, "bookCount", FieldValue.increment(1))
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateList(listId: String, name: String, description: String): Result<Unit> {
        return try {
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

    suspend fun syncBookIntoSystemReadList(libro: Libro): Result<Unit> {
        return try {
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

            batch.set(
                bookRef,
                mapOf(
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
            )
            if (!alreadyExists) {
                batch.update(readListRef, "bookCount", FieldValue.increment(1))
            }
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
