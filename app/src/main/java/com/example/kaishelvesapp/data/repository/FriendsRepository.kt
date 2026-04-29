package com.example.kaishelvesapp.data.repository

import com.example.kaishelvesapp.data.local.GuestLocalStore
import com.example.kaishelvesapp.data.model.Usuario
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.LibroLeido
import com.example.kaishelvesapp.data.model.UserBookList
import com.example.kaishelvesapp.data.model.UserPrivacySettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Locale
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
    val id: String = "",
    val type: FriendActivityType,
    val user: Usuario,
    val timestampMillis: Long? = null,
    val relatedUserName: String? = null,
    val book: Libro? = null,
    val readBook: LibroLeido? = null,
    val social: ActivitySocialSummary = ActivitySocialSummary()
)

data class ActivitySocialSummary(
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val likedByCurrentUser: Boolean = false
)

data class ActivityComment(
    val id: String = "",
    val user: Usuario = Usuario(),
    val text: String = "",
    val timestampMillis: Long? = null
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

data class FriendBookListDetailBookItem(
    val book: Libro,
    val rating: Int? = null,
    val readDate: String? = null
)

data class FriendBookListDetail(
    val list: UserBookList?,
    val books: List<FriendBookListDetailBookItem>
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

    private fun isGuestSessionActive(): Boolean {
        return auth.currentUser == null && GuestLocalStore.isSessionActive()
    }

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

    private fun activitySocialDocument(activityId: String) = firestore.collection("activitySocial")
        .document(activityId)

    private fun activityLikesCollection(activityId: String) = activitySocialDocument(activityId)
        .collection("likes")

    private fun activityCommentsCollection(activityId: String) = activitySocialDocument(activityId)
        .collection("comments")

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
        val storedUser = snapshot.toObject(Usuario::class.java)
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
                photoUrl = photoUrl,
                isAdmin = storedUser?.isAdmin ?: false,
                isGuest = storedUser?.isGuest ?: false,
                privacySettings = storedUser?.privacySettings ?: UserPrivacySettings()
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

    private fun Usuario.visibleTo(viewerUid: String?): Usuario {
        return if (uid == viewerUid || privacySettings.emailVisible) {
            this
        } else {
            copy(email = "")
        }
    }

    private suspend fun canOpenProfile(targetUid: String, viewerUid: String, targetUser: Usuario): Boolean {
        return targetUid == viewerUid ||
            targetUser.privacySettings.profileVisible ||
            friendsCollection(viewerUid).document(targetUid).get().await().exists()
    }

    private suspend fun canOpenReadingActivity(targetUid: String, viewerUid: String, targetUser: Usuario): Boolean {
        return canOpenProfile(targetUid, viewerUid, targetUser) &&
            (targetUid == viewerUid || targetUser.privacySettings.readingActivityVisible)
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
                ?: fallback?.photoUrl.orEmpty(),
            isAdmin = primary?.isAdmin ?: fallback?.isAdmin ?: false,
            isGuest = primary?.isGuest ?: fallback?.isGuest ?: false,
            privacySettings = primary?.privacySettings
                ?: fallback?.privacySettings
                ?: UserPrivacySettings()
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

    private fun DocumentSnapshot.timestampMillis(vararg keys: String): Long? {
        return keys.firstNotNullOfOrNull { key ->
            getTimestamp(key)?.toDate()?.time
        }
    }

    private fun parseReadDateToMillis(date: String): Long? {
        if (date.isBlank()) return null
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)?.time
        }.getOrNull()
    }

    private fun sortActivitiesByRecency(items: List<FriendActivityItem>): List<FriendActivityItem> {
        return items.sortedWith(
            compareByDescending<FriendActivityItem> { it.timestampMillis ?: Long.MIN_VALUE }
                .thenBy { it.user.usuario.ifBlank { it.user.email }.lowercase() }
        )
    }

    private fun activityId(
        ownerUid: String,
        type: FriendActivityType,
        sourceId: String,
        timestampMillis: Long?
    ): String {
        return listOf(
            ownerUid,
            type.name.lowercase(),
            sourceId.ifBlank { "activity" },
            timestampMillis?.toString().orEmpty()
        )
            .joinToString("_")
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .take(220)
    }

    private fun activityOwnerUid(activityId: String): String {
        return activityId.substringBefore("_").trim()
    }

    private suspend fun activitySocialInteractionError(activityId: String, currentUid: String): String? {
        val ownerUid = activityOwnerUid(activityId)
        if (ownerUid.isBlank() || ownerUid == currentUid) {
            return null
        }

        val owner = getUserProfile(ownerUid) ?: return null
        if (!owner.privacySettings.socialInteractionPermissions) {
            return "Este usuario no permite interacciones en su actividad"
        }

        return null
    }

    private suspend fun socialSummary(activityId: String, currentUid: String): ActivitySocialSummary {
        if (activityId.isBlank()) return ActivitySocialSummary()

        val likes = activityLikesCollection(activityId).get().await()
        val comments = activityCommentsCollection(activityId).get().await()
        return ActivitySocialSummary(
            likeCount = likes.size(),
            commentCount = comments.size(),
            likedByCurrentUser = likes.documents.any { it.id == currentUid }
        )
    }

    private suspend fun enrichWithSocial(
        activities: List<FriendActivityItem>,
        currentUid: String
    ): List<FriendActivityItem> {
        return activities.map { item ->
            item.copy(social = socialSummary(item.id, currentUid))
        }
    }

    suspend fun loadSuggestions(): Result<FriendSuggestionsData> {
        return try {
            if (isGuestSessionActive()) {
                return Result.success(
                    FriendSuggestionsData(
                        suggestions = emptyList(),
                        sentRequestIds = emptySet()
                    )
                )
            }

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

                if (user != null && user.privacySettings.profileVisible && user.privacySettings.friendRequestPermissions) {
                    candidatesById[candidateUid] = FriendSuggestion(
                        user = user.visibleTo(uid),
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
                        candidateUid !in sentRequestIds &&
                        user.privacySettings.profileVisible &&
                        user.privacySettings.friendRequestPermissions
                    ) {
                        candidatesById[candidateUid] = FriendSuggestion(
                            user = user.copy(uid = candidateUid).visibleTo(uid),
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
            if (isGuestSessionActive()) {
                return Result.failure(Exception("Las funciones sociales para invitado llegaran en una siguiente iteracion"))
            }

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

            val targetProfile = getUserProfile(targetUser.uid) ?: targetUser
            if (!targetProfile.privacySettings.profileVisible || !targetProfile.privacySettings.friendRequestPermissions) {
                return Result.failure(Exception("Este usuario no acepta solicitudes de amistad ahora mismo"))
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
            if (isGuestSessionActive()) {
                return Result.success(FriendsData(friends = emptyList()))
            }

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

                val booksReadCount = if (friend.privacySettings.readingActivityVisible) {
                    readsCollection(friendUid).get().await().size()
                } else {
                    0
                }
                val friendsCount = if (friend.privacySettings.friendsVisible) {
                    friendsCollection(friendUid).get().await().size()
                } else {
                    0
                }

                FriendListItem(
                    user = friend.visibleTo(uid),
                    booksRead = booksReadCount,
                    friendsCount = friendsCount
                )
            }

            Result.success(FriendsData(friends = friends))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadHomeFeed(): Result<List<FriendActivityItem>> {
        return try {
            if (isGuestSessionActive()) {
                return Result.success(emptyList())
            }

            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesiÃ³n iniciada"))

            val currentUserName = usersCollection()
                .document(uid)
                .get()
                .await()
                .let { snapshotToUser(it, uid) }
                ?.usuario
                .orEmpty()

            val friendDocuments = friendsCollection(uid)
                .get()
                .await()
                .documents

            val activities = buildList {
                friendDocuments.forEach { document ->
                    val friendUid = document.getString("uid").orEmpty().ifBlank { document.id }
                    val cachedFriend = snapshotToUser(document, friendUid)
                    val friend = mergeUserProfile(
                        primary = getUserProfile(friendUid),
                        fallback = cachedFriend,
                        uid = friendUid
                    ) ?: return@forEach
                    if (!friend.privacySettings.profileVisible || !friend.privacySettings.readingActivityVisible) {
                        return@forEach
                    }

                    add(
                        FriendActivityItem(
                            id = activityId(
                                ownerUid = friendUid,
                                type = FriendActivityType.FRIENDSHIP,
                                sourceId = uid,
                                timestampMillis = document.timestampMillis("createdAt")
                            ),
                            type = FriendActivityType.FRIENDSHIP,
                            user = friend.visibleTo(uid),
                            timestampMillis = document.timestampMillis("createdAt"),
                            relatedUserName = currentUserName
                        )
                    )

                    val wantToReadDoc = systemListBooksCollection(friendUid, SYSTEM_LIST_WANT_TO_READ_ID)
                        .get()
                        .await()
                        .documents
                        .maxByOrNull { it.timestampMillis("activityAt") ?: Long.MIN_VALUE }

                    wantToReadDoc?.toObject(Libro::class.java)?.let { book ->
                        add(
                            FriendActivityItem(
                                id = activityId(
                                    ownerUid = friendUid,
                                    type = FriendActivityType.WANT_TO_READ,
                                    sourceId = wantToReadDoc.id,
                                    timestampMillis = wantToReadDoc.timestampMillis("activityAt")
                                ),
                                type = FriendActivityType.WANT_TO_READ,
                                user = friend.visibleTo(uid),
                                timestampMillis = wantToReadDoc.timestampMillis("activityAt"),
                                book = book
                            )
                        )
                    }

                    val readingDoc = systemListBooksCollection(friendUid, SYSTEM_LIST_READING_ID)
                        .get()
                        .await()
                        .documents
                        .maxByOrNull { it.timestampMillis("activityAt") ?: Long.MIN_VALUE }

                    readingDoc?.toObject(Libro::class.java)?.let { book ->
                        add(
                            FriendActivityItem(
                                id = activityId(
                                    ownerUid = friendUid,
                                    type = FriendActivityType.READING,
                                    sourceId = readingDoc.id,
                                    timestampMillis = readingDoc.timestampMillis("activityAt")
                                ),
                                type = FriendActivityType.READING,
                                user = friend.visibleTo(uid),
                                timestampMillis = readingDoc.timestampMillis("activityAt"),
                                book = book
                            )
                        )
                    }

                    val readDoc = readsCollection(friendUid)
                        .get()
                        .await()
                        .documents
                        .maxByOrNull { readDocument ->
                            readDocument.timestampMillis("activityAt")
                                ?: parseReadDateToMillis(readDocument.getString("fechaLeido").orEmpty())
                                ?: Long.MIN_VALUE
                        }

                    readDoc?.toObject(LibroLeido::class.java)?.let { readBook ->
                        add(
                            FriendActivityItem(
                                id = activityId(
                                    ownerUid = friendUid,
                                    type = FriendActivityType.READ,
                                    sourceId = readDoc.id,
                                    timestampMillis = readDoc.timestampMillis("activityAt")
                                        ?: parseReadDateToMillis(readBook.fechaLeido)
                                ),
                                type = FriendActivityType.READ,
                                user = friend.visibleTo(uid),
                                timestampMillis = readDoc.timestampMillis("activityAt")
                                    ?: parseReadDateToMillis(readBook.fechaLeido),
                                readBook = readBook
                            )
                        )
                    }
                }
            }

            Result.success(enrichWithSocial(sortActivitiesByRecency(activities), uid))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadFriendProfile(friendUid: String): Result<FriendProfileData> {
        return try {
            if (isGuestSessionActive()) {
                return Result.failure(Exception("Los perfiles sociales para invitado llegaran en una siguiente iteracion"))
            }

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
            if (friendUid != uid && !isFriend && !resolvedFriend.privacySettings.profileVisible) {
                return Result.failure(Exception("Este perfil no esta visible ahora mismo"))
            }
            val isRequestSent = sentRequestsCollection(uid)
                .document(friendUid)
                .get()
                .await()
                .exists()

            val canShowReadingActivity = resolvedFriend.privacySettings.readingActivityVisible || friendUid == uid
            val canShowFriends = resolvedFriend.privacySettings.friendsVisible || friendUid == uid

            val booksReadDocuments = if (canShowReadingActivity) {
                readsCollection(friendUid)
                    .get()
                    .await()
                    .documents
            } else {
                emptyList()
            }

            val booksRead = booksReadDocuments
                .mapNotNull { it.toObject(LibroLeido::class.java) }

            val readingBookDocuments = if (canShowReadingActivity) {
                systemListBooksCollection(friendUid, SYSTEM_LIST_READING_ID)
                    .get()
                    .await()
                    .documents
            } else {
                emptyList()
            }

            val readingBooks = readingBookDocuments
                .mapNotNull { it.toObject(Libro::class.java) }

            val wantToReadBookDocuments = if (canShowReadingActivity) {
                systemListBooksCollection(friendUid, SYSTEM_LIST_WANT_TO_READ_ID)
                    .get()
                    .await()
                    .documents
            } else {
                emptyList()
            }

            val wantToReadBooks = wantToReadBookDocuments
                .mapNotNull { it.toObject(Libro::class.java) }

            val allFriendDocuments = friendsCollection(friendUid)
                .get()
                .await()
                .documents
            val friendDocuments = if (canShowFriends) allFriendDocuments else emptyList()

            val visibleFriendProfiles = friendDocuments
                .mapNotNull { document ->
                    val previewUid = document.getString("uid").orEmpty().ifBlank { document.id }
                    mergeUserProfile(
                        primary = getUserProfile(previewUid),
                        fallback = snapshotToUser(document, previewUid),
                        uid = previewUid
                    )
                }
                .filter { preview ->
                    preview.uid == uid || preview.privacySettings.profileVisible
                }

            val friendPreviews = visibleFriendProfiles.take(5)

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
                            id = activityId(
                                ownerUid = friendUid,
                                type = FriendActivityType.FRIENDSHIP,
                                sourceId = uid,
                                timestampMillis = friendDocuments
                                    .firstOrNull { it.id == uid }
                                    ?.timestampMillis("createdAt")
                            ),
                            type = FriendActivityType.FRIENDSHIP,
                            user = resolvedFriend.visibleTo(uid),
                            timestampMillis = friendDocuments
                                .firstOrNull { it.id == uid }
                                ?.timestampMillis("createdAt"),
                            relatedUserName = currentUserName
                        )
                    )
                }

                wantToReadBookDocuments
                    .maxByOrNull { it.timestampMillis("activityAt") ?: Long.MIN_VALUE }
                    ?.let { document ->
                        val book = document.toObject(Libro::class.java) ?: return@let
                    add(
                        FriendActivityItem(
                            id = activityId(
                                ownerUid = friendUid,
                                type = FriendActivityType.WANT_TO_READ,
                                sourceId = document.id,
                                timestampMillis = document.timestampMillis("activityAt")
                            ),
                            type = FriendActivityType.WANT_TO_READ,
                            user = resolvedFriend.visibleTo(uid),
                            timestampMillis = document.timestampMillis("activityAt"),
                            book = book
                        )
                    )
                }

                readingBookDocuments
                    .maxByOrNull { it.timestampMillis("activityAt") ?: Long.MIN_VALUE }
                    ?.let { document ->
                        val book = document.toObject(Libro::class.java) ?: return@let
                    add(
                        FriendActivityItem(
                            id = activityId(
                                ownerUid = friendUid,
                                type = FriendActivityType.READING,
                                sourceId = document.id,
                                timestampMillis = document.timestampMillis("activityAt")
                            ),
                            type = FriendActivityType.READING,
                            user = resolvedFriend.visibleTo(uid),
                            timestampMillis = document.timestampMillis("activityAt"),
                            book = book
                        )
                    )
                }

                booksReadDocuments
                    .maxByOrNull { document ->
                        document.timestampMillis("activityAt")
                            ?: parseReadDateToMillis(document.getString("fechaLeido").orEmpty())
                            ?: Long.MIN_VALUE
                    }
                    ?.let { document ->
                        val book = document.toObject(LibroLeido::class.java) ?: return@let
                    add(
                        FriendActivityItem(
                            id = activityId(
                                ownerUid = friendUid,
                                type = FriendActivityType.READ,
                                sourceId = document.id,
                                timestampMillis = document.timestampMillis("activityAt")
                                    ?: parseReadDateToMillis(book.fechaLeido)
                            ),
                            type = FriendActivityType.READ,
                            user = resolvedFriend.visibleTo(uid),
                            timestampMillis = document.timestampMillis("activityAt")
                                ?: parseReadDateToMillis(book.fechaLeido),
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
                    user = resolvedFriend.visibleTo(uid),
                    isFriend = isFriend,
                    isRequestSent = isRequestSent,
                    booksReadCount = booksRead.size,
                    friendsCount = visibleFriendProfiles.size,
                    readingBooks = readingBooks.take(6),
                    wantToReadBooks = wantToReadBooks.take(6),
                    readBooks = booksRead.take(6),
                    predefinedShelves = predefinedShelves,
                    friendPreviews = friendPreviews.map { it.visibleTo(uid) },
                    groupsCount = 0,
                    updates = enrichWithSocial(sortActivitiesByRecency(updates), uid)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleActivityLike(activityId: String): Result<ActivitySocialSummary> {
        return try {
            if (isGuestSessionActive()) {
                return Result.failure(Exception("Inicia sesion para indicar que te gusta una publicacion"))
            }

            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesion iniciada"))
            if (activityId.isBlank()) {
                return Result.failure(Exception("No se pudo identificar la publicacion"))
            }
            activitySocialInteractionError(activityId, uid)?.let { error ->
                return Result.failure(Exception(error))
            }

            val socialRef = activitySocialDocument(activityId)
            val likeRef = activityLikesCollection(activityId).document(uid)
            val likeSnapshot = likeRef.get().await()
            if (likeSnapshot.exists()) {
                likeRef.delete().await()
            } else {
                val user = getUserProfile(uid) ?: Usuario(uid = uid)
                socialRef.set(mapOf("updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge()).await()
                likeRef.set(
                    mapOf(
                        "uid" to user.uid,
                        "usuario" to user.usuario,
                        "email" to user.email,
                        "photoUrl" to user.photoUrl,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(socialSummary(activityId, uid))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadActivityComments(activityId: String): Result<List<ActivityComment>> {
        return try {
            if (isGuestSessionActive()) {
                return Result.success(emptyList())
            }

            val currentUid = currentUid()

            if (activityId.isBlank()) {
                return Result.failure(Exception("No se pudo identificar la publicacion"))
            }

            val comments = activityCommentsCollection(activityId)
                .get()
                .await()
                .documents
                .map { document ->
                    val commentUid = document.getString("uid").orEmpty()
                    val commenter = getUserProfile(commentUid)
                    ActivityComment(
                        id = document.id,
                        user = (commenter ?: Usuario(
                            uid = commentUid,
                            usuario = document.getString("usuario").orEmpty(),
                            email = document.getString("email").orEmpty(),
                            photoUrl = document.getString("photoUrl").orEmpty()
                        )).visibleTo(currentUid),
                        text = document.getString("text").orEmpty(),
                        timestampMillis = document.timestampMillis("createdAt")
                    )
                }
                .sortedBy { it.timestampMillis ?: Long.MAX_VALUE }

            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addActivityComment(activityId: String, text: String): Result<Pair<ActivitySocialSummary, List<ActivityComment>>> {
        return try {
            if (isGuestSessionActive()) {
                return Result.failure(Exception("Inicia sesion para comentar una publicacion"))
            }

            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesion iniciada"))
            val trimmedText = text.trim()
            if (activityId.isBlank()) {
                return Result.failure(Exception("No se pudo identificar la publicacion"))
            }
            if (trimmedText.isBlank()) {
                return Result.failure(Exception("Escribe un comentario"))
            }
            activitySocialInteractionError(activityId, uid)?.let { error ->
                return Result.failure(Exception(error))
            }

            val user = getUserProfile(uid) ?: Usuario(uid = uid)
            activitySocialDocument(activityId)
                .set(mapOf("updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
                .await()
            activityCommentsCollection(activityId)
                .document()
                .set(
                    mapOf(
                        "uid" to user.uid,
                        "usuario" to user.usuario,
                        "email" to user.email,
                        "photoUrl" to user.photoUrl,
                        "text" to trimmedText,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()

            Result.success(
                socialSummary(activityId, uid) to loadActivityComments(activityId).getOrElse { emptyList() }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFriend(friendUid: String): Result<Unit> {
        return try {
            if (isGuestSessionActive()) {
                return Result.failure(Exception("Las funciones sociales para invitado llegaran en una siguiente iteracion"))
            }

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
            if (isGuestSessionActive()) {
                return Result.failure(Exception("Las listas sociales para invitado llegaran en una siguiente iteracion"))
            }

            if (friendUid.isBlank()) {
                return Result.failure(Exception("No se pudo identificar al usuario"))
            }

            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesiÃ³n iniciada"))
            val friend = getUserProfile(friendUid)
                ?: return Result.failure(Exception("No se pudo cargar el perfil del usuario"))
            if (!canOpenReadingActivity(friendUid, uid, friend)) {
                return Result.failure(Exception("Este usuario no muestra su actividad de lectura ahora mismo"))
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

    suspend fun loadFriendListDetail(friendUid: String, listId: String): Result<FriendBookListDetail> {
        return try {
            if (isGuestSessionActive()) {
                return Result.failure(Exception("Las listas sociales para invitado llegaran en una siguiente iteracion"))
            }

            if (friendUid.isBlank() || listId.isBlank()) {
                return Result.failure(Exception("No se pudo identificar la lista"))
            }

            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesiÃ³n iniciada"))
            val friend = getUserProfile(friendUid)
                ?: return Result.failure(Exception("No se pudo cargar el perfil del usuario"))
            if (!canOpenReadingActivity(friendUid, uid, friend)) {
                return Result.failure(Exception("Este usuario no muestra su actividad de lectura ahora mismo"))
            }

            val listDocument = usersCollection()
                .document(friendUid)
                .collection("listas")
                .document(listId)
                .get()
                .await()

            val list = if (listDocument.exists()) {
                UserBookList(
                    id = listDocument.id,
                    name = listDocument.getString("name")
                        ?.takeIf { it.isNotBlank() }
                        ?: defaultSystemListTitle(listDocument.id),
                    description = listDocument.getString("description").orEmpty(),
                    bookCount = listDocument.getLong("bookCount")?.toInt() ?: 0,
                    position = listDocument.getLong("position")?.toInt() ?: 0,
                    isSystem = listDocument.getBoolean("isSystem") == true,
                    systemKey = listDocument.getString("systemKey").orEmpty()
                )
            } else {
                UserBookList(
                    id = listId,
                    name = defaultSystemListTitle(listId),
                    isSystem = listId == SYSTEM_LIST_WANT_TO_READ_ID ||
                        listId == SYSTEM_LIST_READING_ID ||
                        listId == SYSTEM_LIST_READ_ID
                )
            }

            val items = if (listId == SYSTEM_LIST_READ_ID) {
                val readDocuments = readsCollection(friendUid).get().await().documents
                if (readDocuments.isNotEmpty()) {
                    readDocuments.mapNotNull { document ->
                        val readBook = document.toObject(LibroLeido::class.java) ?: return@mapNotNull null
                        FriendBookListDetailBookItem(
                            book = Libro(
                                id = readBook.id.ifBlank { readBook.isbn.ifBlank { document.id } },
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
                            rating = readBook.puntuacion,
                            readDate = readBook.fechaLeido
                        )
                    }
                } else {
                    systemListBooksCollection(friendUid, listId)
                        .get()
                        .await()
                        .documents
                        .mapNotNull { document ->
                            document.toObject(Libro::class.java)?.copy(
                                id = document.getString("id").orEmpty().ifBlank { document.id }
                            )?.let { book ->
                                FriendBookListDetailBookItem(book = book)
                            }
                        }
                }
            } else {
                systemListBooksCollection(friendUid, listId)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { document ->
                        document.toObject(Libro::class.java)?.copy(
                            id = document.getString("id").orEmpty().ifBlank { document.id }
                        )?.let { book ->
                            FriendBookListDetailBookItem(book = book)
                        }
                    }
            }

            Result.success(
                FriendBookListDetail(
                    list = list.copy(bookCount = list.bookCount.takeIf { it > 0 } ?: items.size),
                    books = items
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadReceivedFriendRequests(): Result<FriendRequestsData> {
        return try {
            if (isGuestSessionActive()) {
                return Result.success(
                    FriendRequestsData(
                        receivedRequests = emptyList()
                    )
                )
            }

            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesión iniciada"))

            val receivedRequests = receivedRequestsCollection(uid)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    val requestUid = document.getString("uid").orEmpty().ifBlank { document.id }
                    mergeUserProfile(
                        primary = getUserProfile(requestUid),
                        fallback = snapshotToUser(
                            snapshot = document,
                            fallbackUid = requestUid
                        ),
                        uid = requestUid
                    )?.visibleTo(uid)
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
            if (isGuestSessionActive()) {
                return Result.failure(Exception("Las funciones sociales para invitado llegaran en una siguiente iteracion"))
            }

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
            if (isGuestSessionActive()) {
                return Result.failure(Exception("Las funciones sociales para invitado llegaran en una siguiente iteracion"))
            }

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
