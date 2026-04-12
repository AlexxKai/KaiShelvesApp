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

    private fun requireUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Usuario no autenticado")
    }

    private fun userListsCollection(uid: String) = firestore.collection("usuarios")
        .document(uid)
        .collection("listas")

    private fun safeBookDocId(rawId: String): String {
        return rawId
            .trim()
            .ifBlank { "unknown_book" }
            .replace("/", "_")
    }

    suspend fun getUserLists(): Result<List<UserBookList>> {
        return try {
            val uid = requireUid()
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
                    compareBy<UserBookList> { it.position }
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
            val newDocument = userListsCollection(uid).document()
            val currentLists = userListsCollection(uid)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(UserBookList::class.java)?.position }
            val nextPosition = (currentLists.maxOrNull() ?: -1) + 1
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
            val safeBookId = safeBookDocId(libro.id.ifBlank { libro.isbn })
            if (safeBookId == "unknown_book") {
                return Result.failure(IllegalArgumentException("El libro no tiene identificador válido"))
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
                batch.set(bookRef, bookPayload)
                batch.update(listRef, "bookCount", FieldValue.increment(1))
            }

            idsToRemove.forEach { listId ->
                val listRef = userListsCollection(uid).document(listId)
                val bookRef = listRef.collection("libros").document(safeBookId)
                batch.delete(bookRef)
                batch.update(listRef, "bookCount", FieldValue.increment(-1))
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
            val batch = firestore.batch()

            orderedListIds.forEachIndexed { index, listId ->
                val listRef = userListsCollection(uid).document(listId)
                batch.update(listRef, "position", index)
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
            val safeBookId = safeBookDocId(bookId)
            if (safeBookId == "unknown_book") {
                return Result.failure(IllegalArgumentException("El libro no tiene identificador válido"))
            }

            val listRef = userListsCollection(uid).document(listId)
            val bookRef = listRef.collection("libros").document(safeBookId)
            val bookSnapshot = bookRef.get().await()

            if (!bookSnapshot.exists()) {
                return Result.success(Unit)
            }

            val batch = firestore.batch()
            batch.delete(bookRef)
            batch.update(listRef, "bookCount", FieldValue.increment(-1))
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
