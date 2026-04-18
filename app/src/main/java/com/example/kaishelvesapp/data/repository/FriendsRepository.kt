package com.example.kaishelvesapp.data.repository

import com.example.kaishelvesapp.data.model.Usuario
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.LibroLeido
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.kaishelvesapp.data.repository.UserListsRepository.Companion.SYSTEM_LIST_READ_ID
import com.example.kaishelvesapp.data.repository.UserListsRepository.Companion.SYSTEM_LIST_READING_ID
import com.example.kaishelvesapp.data.repository.UserListsRepository.Companion.SYSTEM_LIST_WANT_TO_READ_ID

enum class SuggestionSource {
    FRIEND_OF_FRIEND,
    RANDOM
}

data class FriendSuggestion(
    val user: Usuario,
    val source: SuggestionSource
)

data class FriendSuggestionsData(
    val suggestions: List<FriendSuggestion>,
    val sentRequestIds: Set<String>
)

data class FriendRequestsData(
    val receivedRequests: List<Usuario>
)

data class FriendListItem(
    val user: Usuario,
    val booksRead: Int,
    val friendsCount: Int
)

data class FriendsData(
    val friends: List<FriendListItem>
)

enum class FriendActivityType {
    FRIENDSHIP,
    WANT_TO_READ,
    READING,
    READ
}

data class FriendActivityItem(
    val type: FriendActivityType,
    val user: Usuario,
    val relatedUserName: String? = null,
    val book: Libro? = null,
    val readBook: LibroLeido? = null
)

data class FriendShelfBookItem(
    val book: Libro,
    val rating: Int? = null
)

data class FriendShelfPreview(
    val listId: String,
    val title: String,
    val bookCount: Int,
    val books: List<FriendShelfBookItem>,
    val isReadList: Boolean
)

data class FriendBookListSummary(
    val id: String,
    val name: String,
    val bookCount: Int,
    val previewImageUrls: List<String>
)

data class FriendProfileData(
    val user: Usuario,
    val isFriend: Boolean,
    val isRequestSent: Boolean,
    val booksReadCount: Int,
    val friendsCount: Int,
    val readingBooks: List<Libro>,
    val wantToReadBooks: List<Libro>,
    val readBooks: List<LibroLeido>,
    val predefinedShelves: List<FriendShelfPreview>,
    val friendPreviews: List<Usuario>,
    val groupsCount: Int,
    val updates: List<FriendActivityItem>
)

class FriendsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private fun currentUid(): String? = auth.currentUser?.uid

    private fun usersCollection() = firestore.collection("usuarios")

    private fun friendsCollection(uid: String) = usersCollection().document(uid).collection("friends")
    private fun readsCollection(uid: String) = usersCollection().document(uid).collection("leidos")
    private fun systemListBooksCollection(uid: String, listId: String) = usersCollection()
        .document(uid)
        .collection("listas")
        .document(listId)
        .collection("libros")

    private fun sentRequestsCollection(uid: String) = usersCollection().document(uid).collection("friend_requests_sent")

    private fun receivedRequestsCollection(uid: String) = usersCollection().document(uid).collection("friend_requests_received")

    private fun DocumentSnapshot.stringValue(vararg keys: String): String {
        return keys.firstNotNullOfOrNull { key ->
            getString(key)?.trim()?.takeIf { it.isNotBlank() }
        }.orEmpty()
    }

    private fun snapshotToUser(
        snapshot: DocumentSnapshot,
        fallbackUid: String = snapshot.id
    ): Usuario? {
        if (!snapshot.exists()) {
            return null
        }

        val uid = snapshot.stringValue("uid").ifBlank { fallbackUid }
        val username = snapshot.stringValue("usuario", "username", "displayName", "nombreUsuario")
        val email = snapshot.stringValue("email", "correo", "mail")
        val photoUrl = snapshot.stringValue(
            "photoUrl",
            "profileImageUrl",
            "profilePhotoUrl",
            "avatarUrl",
            "avatar",
            "fotoPerfil",
            "imagenPerfil",
            "photo_url",
            "profile_photo_url"
        )

        return if (uid.isBlank() && username.isBlank() && email.isBlank() && photoUrl.isBlank()) {
            null
        } else {
            Usuario(
                uid = uid,
                usuario = username,
                email = email,
                photoUrl = photoUrl
            )
        }
    }

    private suspend fun getUserProfile(uid: String): Usuario? {
        val userSnapshot = usersCollection()
            .document(uid)
            .get()
            .await()

        return snapshotToUser(userSnapshot, uid)
    }

    private fun mergeUserProfile(
        primary: Usuario?,
        fallback: Usuario?,
        uid: String
    ): Usuario? {
        if (primary == null && fallback == null) {
            return null
        }

        return Usuario(
            uid = uid,
            usuario = primary?.usuario?.takeIf { it.isNotBlank() }
                ?: fallback?.usuario.orEmpty(),
            email = primary?.email?.takeIf { it.isNotBlank() }
                ?: fallback?.email.orEmpty(),
            photoUrl = primary?.photoUrl?.takeIf { it.isNotBlank() }
                ?: fallback?.photoUrl.orEmpty()
        )
    }

    private fun defaultSystemListTitle(listId: String): String {
        return when (listId) {
            SYSTEM_LIST_WANT_TO_READ_ID -> "Quiero leer"
            SYSTEM_LIST_READING_ID -> "Leyendo"
            SYSTEM_LIST_READ_ID -> "Leído"
            else -> "Lista"
        }
    }

    suspend fun loadSuggestions(): Result<FriendSuggestionsData> {
        return try {
            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesion iniciada"))

            val friendsSnapshot = friendsCollection(uid).get().await()
            val sentRequestsSnapshot = sentRequestsCollection(uid).get().await()
            val currentFriends = friendsSnapshot.documents.map { it.id }.toSet()
            val sentRequestIds = sentRequestsSnapshot.documents.map { it.id }.toSet()

            val candidatesById = linkedMapOf<String, FriendSuggestion>()
            val secondDegreeIds = linkedSetOf<String>()

            currentFriends.forEach { friendUid ->
                val friendConnections = friendsCollection(friendUid).get().await()
                friendConnections.documents
                    .map { it.id }
                    .filterNot { candidateUid ->
                        candidateUid == uid ||
                            candidateUid in currentFriends ||
                            candidateUid in sentRequestIds
                    }
                    .forEach { candidateUid ->
                        secondDegreeIds += candidateUid
                    }
            }

            secondDegreeIds.forEach { candidateUid ->
                val user = usersCollection()
                    .document(candidateUid)
                    .get()
                    .await()
                    .let { snapshotToUser(it, candidateUid) }

                if (user != null) {
                    candidatesById[candidateUid] = FriendSuggestion(
                        user = user,
                        source = SuggestionSource.FRIEND_OF_FRIEND
                    )
                }
            }

            if (candidatesById.isEmpty()) {
                val allUsers = usersCollection().get().await()
                allUsers.documents.forEach { document ->
                    val user = snapshotToUser(document) ?: return@forEach
                    val candidateUid = document.id
                    if (
                        candidateUid != uid &&
                        candidateUid !in currentFriends &&
                        candidateUid !in sentRequestIds
                    ) {
                        candidatesById[candidateUid] = FriendSuggestion(
                            user = user.copy(uid = candidateUid),
                            source = SuggestionSource.RANDOM
                        )
                    }
                }
            }

            Result.success(
                FriendSuggestionsData(
                    suggestions = candidatesById.values.toList(),
                    sentRequestIds = sentRequestIds
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendFriendRequest(targetUser: Usuario): Result<Unit> {
        return try {
            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesion iniciada"))
            val currentUser = usersCollection()
                .document(uid)
                .get()
                .await()
                .let { snapshotToUser(it, uid) }
                ?: return Result.failure(Exception("No se pudo obtener el perfil actual"))

            if (targetUser.uid.isBlank() || targetUser.uid == uid) {
                return Result.failure(Exception("Usuario no valido"))
            }

            val batch = firestore.batch()
            val sentRef = sentRequestsCollection(uid).document(targetUser.uid)
            val receivedRef = receivedRequestsCollection(targetUser.uid).document(uid)

            batch.set(
                sentRef,
                mapOf(
                    "uid" to targetUser.uid,
                    "usuario" to targetUser.usuario,
                    "email" to targetUser.email,
                    "photoUrl" to targetUser.photoUrl,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                receivedRef,
                mapOf(
                    "uid" to currentUser.uid,
                    "usuario" to currentUser.usuario,
                    "email" to currentUser.email,
                    "photoUrl" to currentUser.photoUrl,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadFriends(): Result<FriendsData> {
        return try {
            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesión iniciada"))

            val friendDocuments = friendsCollection(uid)
                .get()
                .await()
                .documents

            val friends = friendDocuments.mapNotNull { document ->
                val friendUid = document.getString("uid").orEmpty().ifBlank { document.id }
                val cachedFriend = snapshotToUser(document, friendUid)
                val friend = mergeUserProfile(
                    primary = getUserProfile(friendUid),
                    fallback = cachedFriend,
                    uid = friendUid
                )
                    ?: return@mapNotNull null

                val booksReadCount = readsCollection(friendUid).get().await().size()
                val friendsCount = friendsCollection(friendUid).get().await().size()

                FriendListItem(
                    user = friend,
                    booksRead = booksReadCount,
                    friendsCount = friendsCount
                )
            }

            Result.success(FriendsData(friends = friends))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadFriendProfile(friendUid: String): Result<FriendProfileData> {
        return try {
            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesión iniciada"))

            val friend = usersCollection()
                .document(friendUid)
                .get()
                .await()
                .let { snapshotToUser(it, friendUid) }
            val cachedFriend = friendsCollection(uid)
                .document(friendUid)
                .get()
                .await()
                .let { snapshotToUser(it, friendUid) }
            val resolvedFriend = mergeUserProfile(
                primary = friend,
                fallback = cachedFriend,
                uid = friendUid
            )
                ?: return Result.failure(Exception("No se pudo cargar el perfil del amigo"))
            val isFriend = friendsCollection(uid)
                .document(friendUid)
                .get()
                .await()
                .exists()
            val isRequestSent = sentRequestsCollection(uid)
                .document(friendUid)
                .get()
                .await()
                .exists()

            val booksRead = readsCollection(friendUid)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(LibroLeido::class.java) }

            val readingBooks = systemListBooksCollection(friendUid, SYSTEM_LIST_READING_ID)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Libro::class.java) }

            val wantToReadBooks = systemListBooksCollection(friendUid, SYSTEM_LIST_WANT_TO_READ_ID)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Libro::class.java) }

            val friendDocuments = friendsCollection(friendUid)
                .get()
                .await()
                .documents

            val friendPreviews = friendDocuments
                .take(5)
                .mapNotNull { document ->
                    val previewUid = document.getString("uid").orEmpty().ifBlank { document.id }
                    mergeUserProfile(
                        primary = getUserProfile(previewUid),
                        fallback = snapshotToUser(document, previewUid),
                        uid = previewUid
                    )
                }

            val currentUserName = usersCollection()
                .document(uid)
                .get()
                .await()
                .let { snapshotToUser(it, uid) }
                ?.usuario
                .orEmpty()

            val updates = buildList {
                if (friendDocuments.any { it.id == uid }) {
                    add(
                        FriendActivityItem(
                            type = FriendActivityType.FRIENDSHIP,
                            user = resolvedFriend,
                            relatedUserName = currentUserName
                        )
                    )
                }

                wantToReadBooks.firstOrNull()?.let { book ->
                    add(
                        FriendActivityItem(
                            type = FriendActivityType.WANT_TO_READ,
                            user = resolvedFriend,
                            book = book
                        )
                    )
                }

                readingBooks.firstOrNull()?.let { book ->
                    add(
                        FriendActivityItem(
                            type = FriendActivityType.READING,
                            user = resolvedFriend,
                            book = book
                        )
                    )
                }

                booksRead.firstOrNull()?.let { book ->
                    add(
                        FriendActivityItem(
                            type = FriendActivityType.READ,
                            user = resolvedFriend,
                            readBook = book
                        )
                    )
                }
            }

            val predefinedShelves = buildList {
                add(
                    FriendShelfPreview(
                        listId = SYSTEM_LIST_READING_ID,
                        title = defaultSystemListTitle(SYSTEM_LIST_READING_ID),
                        bookCount = readingBooks.size,
                        books = readingBooks.take(4).map { book ->
                            FriendShelfBookItem(book = book)
                        },
                        isReadList = false
                    )
                )
                add(
                    FriendShelfPreview(
                        listId = SYSTEM_LIST_WANT_TO_READ_ID,
                        title = defaultSystemListTitle(SYSTEM_LIST_WANT_TO_READ_ID),
                        bookCount = wantToReadBooks.size,
                        books = wantToReadBooks.take(4).map { book ->
                            FriendShelfBookItem(book = book)
                        },
                        isReadList = false
                    )
                )
                add(
                    FriendShelfPreview(
                        listId = SYSTEM_LIST_READ_ID,
                        title = defaultSystemListTitle(SYSTEM_LIST_READ_ID),
                        bookCount = booksRead.size,
                        books = booksRead.take(4).map { readBook ->
                            FriendShelfBookItem(
                                book = Libro(
                                    id = readBook.id.ifBlank { readBook.isbn },
                                    isbn = readBook.isbn,
                                    titulo = readBook.titulo,
                                    autor = readBook.autor,
                                    editorial = readBook.editorial,
                                    genero = readBook.genero,
                                    fechaPublicacion = readBook.fechaPublicacion,
                                    paginas = readBook.paginas,
                                    imagen = readBook.imagen,
                                    pdf = readBook.pdf
                                ),
                                rating = readBook.puntuacion
                            )
                        },
                        isReadList = true
                    )
                )
            }

            Result.success(
                FriendProfileData(
                    user = resolvedFriend,
                    isFriend = isFriend,
                    isRequestSent = isRequestSent,
                    booksReadCount = booksRead.size,
                    friendsCount = friendDocuments.size,
                    readingBooks = readingBooks.take(6),
                    wantToReadBooks = wantToReadBooks.take(6),
                    readBooks = booksRead.take(6),
                    predefinedShelves = predefinedShelves,
                    friendPreviews = friendPreviews,
                    groupsCount = 0,
                    updates = updates
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFriend(friendUid: String): Result<Unit> {
        return try {
            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesión iniciada"))

            if (friendUid.isBlank()) {
                return Result.failure(Exception("No se pudo identificar al amigo"))
            }

            val batch = firestore.batch()
            batch.delete(friendsCollection(uid).document(friendUid))
            batch.delete(friendsCollection(friendUid).document(uid))
            batch.delete(sentRequestsCollection(uid).document(friendUid))
            batch.delete(receivedRequestsCollection(uid).document(friendUid))
            batch.delete(sentRequestsCollection(friendUid).document(uid))
            batch.delete(receivedRequestsCollection(friendUid).document(uid))
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadFriendLists(friendUid: String): Result<List<FriendBookListSummary>> {
        return try {
            if (friendUid.isBlank()) {
                return Result.failure(Exception("No se pudo identificar al usuario"))
            }

            val listsSnapshot = usersCollection()
                .document(friendUid)
                .collection("listas")
                .get()
                .await()

            val lists = listsSnapshot.documents
                .map { document ->
                    val name = document.getString("name")
                        ?.takeIf { it.isNotBlank() }
                        ?: defaultSystemListTitle(document.id)
                    val bookCount = document.getLong("bookCount")?.toInt() ?: 0
                    val previewImages = document.reference
                        .collection("libros")
                        .limit(3)
                        .get()
                        .await()
                        .documents
                        .mapNotNull { it.getString("imagen")?.takeIf { image -> image.isNotBlank() } }

                    FriendBookListSummary(
                        id = document.id,
                        name = name,
                        bookCount = bookCount,
                        previewImageUrls = previewImages
                    )
                }
                .sortedWith(
                    compareBy<FriendBookListSummary> {
                        when (it.id) {
                            SYSTEM_LIST_WANT_TO_READ_ID -> 0
                            SYSTEM_LIST_READING_ID -> 1
                            SYSTEM_LIST_READ_ID -> 2
                            else -> 3
                        }
                    }.thenBy { it.name.lowercase() }
                )

            Result.success(lists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadReceivedFriendRequests(): Result<FriendRequestsData> {
        return try {
            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesión iniciada"))

            val receivedRequests = receivedRequestsCollection(uid)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    snapshotToUser(
                        snapshot = document,
                        fallbackUid = document.getString("uid").orEmpty().ifBlank { document.id }
                    )
                }

            Result.success(
                FriendRequestsData(
                    receivedRequests = receivedRequests
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(requestUser: Usuario): Result<Unit> {
        return try {
            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesión iniciada"))
            val currentUser = usersCollection()
                .document(uid)
                .get()
                .await()
                .let { snapshotToUser(it, uid) }
                ?: return Result.failure(Exception("No se pudo obtener el perfil actual"))

            if (requestUser.uid.isBlank()) {
                return Result.failure(Exception("Solicitud no válida"))
            }

            val batch = firestore.batch()
            val currentUserFriendRef = friendsCollection(uid).document(requestUser.uid)
            val requestUserFriendRef = friendsCollection(requestUser.uid).document(uid)
            val currentReceivedRef = receivedRequestsCollection(uid).document(requestUser.uid)
            val requesterSentRef = sentRequestsCollection(requestUser.uid).document(uid)

            batch.set(
                currentUserFriendRef,
                mapOf(
                    "uid" to requestUser.uid,
                    "usuario" to requestUser.usuario,
                    "email" to requestUser.email,
                    "photoUrl" to requestUser.photoUrl
                )
            )
            batch.set(
                requestUserFriendRef,
                mapOf(
                    "uid" to currentUser.uid,
                    "usuario" to currentUser.usuario,
                    "email" to currentUser.email,
                    "photoUrl" to currentUser.photoUrl
                )
            )
            batch.delete(currentReceivedRef)
            batch.delete(requesterSentRef)
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectFriendRequest(requestUser: Usuario): Result<Unit> {
        return try {
            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesión iniciada"))

            if (requestUser.uid.isBlank()) {
                return Result.failure(Exception("Solicitud no válida"))
            }

            val batch = firestore.batch()
            batch.delete(receivedRequestsCollection(uid).document(requestUser.uid))
            batch.delete(sentRequestsCollection(requestUser.uid).document(uid))
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
