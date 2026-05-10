package com.example.kaishelvesapp.data.repository

import com.example.kaishelvesapp.data.local.GuestLocalStore
import com.example.kaishelvesapp.data.model.Usuario
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.LibroLeido
import com.example.kaishelvesapp.data.model.UserBookList
import com.example.kaishelvesapp.data.model.UserBookTag
import com.example.kaishelvesapp.data.model.UserPrivacySettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.tasks.await
import com.example.kaishelvesapp.data.repository.UserListsRepository.Companion.SYSTEM_LIST_PENDING_ID
import com.example.kaishelvesapp.data.repository.UserListsRepository.Companion.SYSTEM_LIST_READ_ID
import com.example.kaishelvesapp.data.repository.UserListsRepository.Companion.SYSTEM_LIST_READING_ID
import com.example.kaishelvesapp.data.repository.UserListsRepository.Companion.SYSTEM_LIST_UNFINISHED_ID
import com.example.kaishelvesapp.data.repository.UserListsRepository.Companion.SYSTEM_LIST_WANT_TO_READ_ID

private const val LAST_QUARTER_MILLIS = 90L * 24L * 60L * 60L * 1000L
const val FRIEND_TAG_DETAIL_PREFIX = "friend_tag__"

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
    val sentRequestIds: Set<String>,
    val receivedRequestIds: Set<String> = emptySet()
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
    READ,
    LIST_ADDED
}

data class FriendActivityItem(
    val id: String = "",
    val type: FriendActivityType,
    val user: Usuario,
    val timestampMillis: Long? = null,
    val relatedUserName: String? = null,
    val listName: String? = null,
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

enum class ActivityNotificationType {
    LIKE,
    COMMENT
}

data class ActivityNotificationItem(
    val id: String,
    val type: ActivityNotificationType,
    val activityId: String,
    val user: Usuario,
    val activity: FriendActivityItem,
    val text: String = "",
    val timestampMillis: Long? = null,
    val isRead: Boolean = false
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

data class FriendBookTagSummary(
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
    val isRequestReceived: Boolean = false,
    val isPrivateProfile: Boolean = false,
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

data class BlockedMember(
    val user: Usuario,
    val blockedAtMillis: Long? = null
)

enum class AccountReportStatus {
    PENDING,
    NEEDS_INFO,
    RESOLVED
}

data class AccountReport(
    val id: String = "",
    val reportedUser: Usuario = Usuario(),
    val subject: String = "",
    val message: String = "",
    val photoUris: List<String> = emptyList(),
    val status: AccountReportStatus = AccountReportStatus.PENDING,
    val adminMessage: String = "",
    val createdAtMillis: Long? = null,
    val updatedAtMillis: Long? = null
)

private class CompositeListenerRegistration(
    private val registrations: List<ListenerRegistration>
) : ListenerRegistration {
    override fun remove() {
        registrations.forEach { it.remove() }
    }
}

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
    private fun listsCollection(uid: String) = usersCollection().document(uid).collection("listas")
    private fun systemListBooksCollection(uid: String, listId: String) = usersCollection()
        .document(uid)
        .collection("listas")
        .document(listId)
        .collection("libros")

    private fun tagsCollection(uid: String) = usersCollection().document(uid).collection("etiquetas")

    private fun bookMetadataCollection(uid: String) = usersCollection().document(uid).collection("libros_metadata")

    private fun sentRequestsCollection(uid: String) = usersCollection().document(uid).collection("friend_requests_sent")

    private fun receivedRequestsCollection(uid: String) = usersCollection().document(uid).collection("friend_requests_received")

    private fun activitySocialCollection() = firestore.collection("activitySocial")

    private fun activitySocialDocument(activityId: String) = activitySocialCollection()
        .document(activityId)

    private fun activityLikesCollection(activityId: String) = activitySocialDocument(activityId)
        .collection("likes")

    private fun activityCommentsCollection(activityId: String) = activitySocialDocument(activityId)
        .collection("comments")

    private fun activityNotificationReadsCollection(uid: String) = usersCollection()
        .document(uid)
        .collection("activity_notification_reads")

    private fun hiddenActivityUpdatesCollection(uid: String) = usersCollection()
        .document(uid)
        .collection("hidden_activity_updates")

    private fun blockedMembersCollection(uid: String) = usersCollection()
        .document(uid)
        .collection("blocked_members")

    private fun reportReviewsCollection(uid: String) = usersCollection()
        .document(uid)
        .collection("report_reviews")

    private fun accountReportsCollection() = firestore.collection("accountReports")

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

    private suspend fun blockedUserIds(uid: String): Set<String> {
        if (uid.isBlank()) return emptySet()
        return blockedMembersCollection(uid)
            .get()
            .await()
            .documents
            .map { it.id }
            .toSet()
    }

    private suspend fun isBlockedBetween(firstUid: String, secondUid: String): Boolean {
        if (firstUid.isBlank() || secondUid.isBlank() || firstUid == secondUid) return false
        val firstBlockedSecond = blockedMembersCollection(firstUid)
            .document(secondUid)
            .get()
            .await()
            .exists()
        if (firstBlockedSecond) return true

        return blockedMembersCollection(secondUid)
            .document(firstUid)
            .get()
            .await()
            .exists()
    }

    private suspend fun canOpenProfile(targetUid: String, viewerUid: String, targetUser: Usuario): Boolean {
        if (isBlockedBetween(targetUid, viewerUid)) return false
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
            SYSTEM_LIST_UNFINISHED_ID -> "No terminado"
            SYSTEM_LIST_PENDING_ID -> "Pendientes"
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

    private fun lastQuarterCutoffMillis(): Long {
        return System.currentTimeMillis() - LAST_QUARTER_MILLIS
    }

    private fun isWithinLastQuarter(timestampMillis: Long?): Boolean {
        return timestampMillis != null && timestampMillis >= lastQuarterCutoffMillis()
    }

    private fun bookUniqueKey(book: Libro): String {
        return when {
            book.isbn.isNotBlank() -> "isbn:${book.isbn.trim().lowercase()}"
            book.id.isNotBlank() -> "id:${book.id.trim().lowercase()}"
            else -> "title:${book.titulo.trim().lowercase()}|author:${book.autor.trim().lowercase()}"
        }
    }

    private fun bookUniqueKey(readBook: LibroLeido): String {
        return when {
            readBook.isbn.isNotBlank() -> "isbn:${readBook.isbn.trim().lowercase()}"
            readBook.id.isNotBlank() -> "id:${readBook.id.trim().lowercase()}"
            else -> "title:${readBook.titulo.trim().lowercase()}|author:${readBook.autor.trim().lowercase()}"
        }
    }

    private suspend fun uniqueBooksInAllListsCount(uid: String): Int {
        val uniqueKeys = linkedSetOf<String>()
        val listDocuments = listsCollection(uid).get().await().documents
        listDocuments.forEach { listDocument ->
            listDocument.reference
                .collection("libros")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Libro::class.java) }
                .forEach { book ->
                    uniqueKeys += bookUniqueKey(book)
                }
        }

        readsCollection(uid)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(LibroLeido::class.java) }
            .forEach { readBook ->
                uniqueKeys += bookUniqueKey(readBook)
            }

        return uniqueKeys.size
    }

    private fun safeBookDocId(rawId: String): String {
        return rawId
            .trim()
            .ifBlank { "unknown_book" }
            .replace("/", "_")
    }

    private suspend fun allBooksByIdInLists(uid: String): Map<String, Libro> {
        return listsCollection(uid)
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
    }

    private suspend fun loadFriendTagSummaries(friendUid: String): List<FriendBookTagSummary> {
        val tags = tagsCollection(friendUid)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject(UserBookTag::class.java)?.copy(id = document.id)
            }
            .sortedWith(compareBy<UserBookTag> { it.position }.thenBy { it.name.lowercase() })

        if (tags.isEmpty()) return emptyList()

        val metadataDocuments = bookMetadataCollection(friendUid)
            .get()
            .await()
            .documents
        val metadataBooksById = metadataDocuments
            .mapNotNull { metadataDocument ->
                metadataDocument.toObject(Libro::class.java)?.copy(
                    id = metadataDocument.getString("id").orEmpty().ifBlank { metadataDocument.id }
                )
            }
            .associateBy { book -> safeBookDocId(book.id.ifBlank { book.isbn }) }
        val allBooksById = allBooksByIdInLists(friendUid) + metadataBooksById

        return tags.map { tag ->
            val bookIds = metadataDocuments
                .filter { document ->
                    val tagIds = document.get("tagIds") as? List<*>
                    tag.id in tagIds.orEmpty().filterIsInstance<String>()
                }
                .map { it.id }

            FriendBookTagSummary(
                id = tag.id,
                name = tag.name,
                bookCount = bookIds.size,
                previewImageUrls = bookIds.mapNotNull { bookId ->
                    allBooksById[bookId]?.imagen?.takeIf(String::isNotBlank)
                }.take(3)
            )
        }
    }

    private suspend fun loadFriendTagBooks(friendUid: String, tagId: String): List<FriendBookListDetailBookItem> {
        val metadataDocuments = bookMetadataCollection(friendUid)
            .get()
            .await()
            .documents
        val taggedBookIds = metadataDocuments
            .filter { document ->
                val tagIds = document.get("tagIds") as? List<*>
                tagId in tagIds.orEmpty().filterIsInstance<String>()
            }
            .map { it.id }
            .toSet()
        val metadataBooks = metadataDocuments
            .filter { it.id in taggedBookIds }
            .mapNotNull { metadataDocument ->
                metadataDocument.toObject(Libro::class.java)?.copy(
                    id = metadataDocument.getString("id").orEmpty().ifBlank { metadataDocument.id }
                )
            }
        val listBooks = allBooksByIdInLists(friendUid)
            .filterKeys { it in taggedBookIds }
            .values

        return (listBooks + metadataBooks)
            .distinctBy { book -> safeBookDocId(book.id.ifBlank { book.isbn }) }
            .sortedBy { it.titulo.lowercase() }
            .map { book -> FriendBookListDetailBookItem(book = book) }
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

    private fun notificationId(activityId: String, type: String, sourceId: String): String {
        return "${activityId}_${type}_${sourceId}"
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .take(240)
    }

    private suspend fun activitySocialInteractionError(activityId: String): String? {
        val ownerUid = activityOwnerUid(activityId)
        if (ownerUid.isBlank()) {
            return null
        }

        val owner = getUserProfile(ownerUid) ?: return null
        if (!owner.privacySettings.readingActivityVisible) {
            return "Este usuario no muestra su actividad de lectura ahora mismo"
        }
        if (!owner.privacySettings.socialInteractionPermissions) {
            return "Este usuario no permite interacciones en su actividad"
        }

        return null
    }

    private suspend fun socialSummary(activityId: String, currentUid: String): ActivitySocialSummary {
        if (activityId.isBlank()) return ActivitySocialSummary()
        if (activitySocialInteractionError(activityId) != null) return ActivitySocialSummary()

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

    private suspend fun visibleActivityUpdates(
        ownerUid: String,
        activities: List<FriendActivityItem>
    ): List<FriendActivityItem> {
        if (ownerUid.isBlank() || activities.isEmpty()) return activities

        val hiddenIds = hiddenActivityUpdatesCollection(ownerUid)
            .get()
            .await()
            .documents
            .map { it.id }
            .toSet()

        return activities.filterNot { it.id in hiddenIds }
    }

    private suspend fun bookListActivities(
        ownerUid: String,
        owner: Usuario,
        viewerUid: String
    ): List<FriendActivityItem> {
        val listDocuments = listsCollection(ownerUid).get().await().documents
        val hasReadListBooks = listDocuments.any { listDocument ->
            listDocument.id == SYSTEM_LIST_READ_ID &&
                listDocument.reference.collection("libros").limit(1).get().await().documents.isNotEmpty()
        }

        val listActivities = listDocuments.flatMap { listDocument ->
            val listId = listDocument.id
            val listName = listDocument.getString("name")
                ?.takeIf { it.isNotBlank() }
                ?: defaultSystemListTitle(listId)
            val type = when (listId) {
                SYSTEM_LIST_WANT_TO_READ_ID -> FriendActivityType.WANT_TO_READ
                SYSTEM_LIST_READING_ID -> FriendActivityType.READING
                SYSTEM_LIST_READ_ID -> FriendActivityType.READ
                else -> FriendActivityType.LIST_ADDED
            }

            listDocument.reference
                .collection("libros")
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    val timestamp = document.timestampMillis("activityAt")
                    if (!isWithinLastQuarter(timestamp)) return@mapNotNull null
                    val book = document.toObject(Libro::class.java) ?: return@mapNotNull null
                    FriendActivityItem(
                        id = activityId(
                            ownerUid = ownerUid,
                            type = type,
                            sourceId = "${listId}_${document.id}",
                            timestampMillis = timestamp
                        ),
                        type = type,
                        user = owner.visibleTo(viewerUid),
                        timestampMillis = timestamp,
                        listName = listName,
                        book = book
                    )
                }
        }

        if (hasReadListBooks) {
            return listActivities
        }

        val readActivities = readsCollection(ownerUid)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                val readBook = document.toObject(LibroLeido::class.java) ?: return@mapNotNull null
                val timestamp = document.timestampMillis("activityAt")
                    ?: parseReadDateToMillis(document.getString("fechaLeido").orEmpty())
                    ?: parseReadDateToMillis(readBook.fechaLeido)
                if (!isWithinLastQuarter(timestamp)) return@mapNotNull null
                FriendActivityItem(
                    id = activityId(
                        ownerUid = ownerUid,
                        type = FriendActivityType.READ,
                        sourceId = document.id,
                        timestampMillis = timestamp
                    ),
                    type = FriendActivityType.READ,
                    user = owner.visibleTo(viewerUid),
                    timestampMillis = timestamp,
                    readBook = readBook
                )
            }

        return listActivities + readActivities
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
                ?: return Result.failure(Exception("No hay sesión iniciada"))

            val friendsSnapshot = friendsCollection(uid).get().await()
            val sentRequestsSnapshot = sentRequestsCollection(uid).get().await()
            val receivedRequestsSnapshot = receivedRequestsCollection(uid).get().await()
            val currentFriends = friendsSnapshot.documents.map { it.id }.toSet()
            val sentRequestIds = sentRequestsSnapshot.documents.map { it.id }.toSet()
            val receivedRequestIds = receivedRequestsSnapshot.documents.map { it.id }.toSet()
            val blockedByMe = blockedUserIds(uid)

            val candidatesById = linkedMapOf<String, FriendSuggestion>()
            val secondDegreeIds = linkedSetOf<String>()

            (sentRequestIds + receivedRequestIds)
                .filterNot { requestUid ->
                    requestUid == uid ||
                        requestUid in currentFriends ||
                        requestUid in blockedByMe ||
                        isBlockedBetween(uid, requestUid)
                }
                .forEach { requestUid ->
                    val user = getUserProfile(requestUid)
                    if (user != null) {
                        candidatesById[requestUid] = FriendSuggestion(
                            user = user.visibleTo(uid),
                            source = SuggestionSource.RANDOM
                        )
                    }
                }

            currentFriends.forEach { friendUid ->
                val friendConnections = friendsCollection(friendUid).get().await()
                friendConnections.documents
                    .map { it.id }
                    .filterNot { candidateUid ->
                        candidateUid == uid ||
                            candidateUid in currentFriends ||
                            candidateUid in blockedByMe
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

                if (
                    user != null &&
                    user.privacySettings.friendRequestPermissions &&
                    !isBlockedBetween(uid, candidateUid)
                ) {
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
                        candidateUid !in blockedByMe &&
                        !isBlockedBetween(uid, candidateUid) &&
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
                    sentRequestIds = sentRequestIds,
                    receivedRequestIds = receivedRequestIds
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
                ?: return Result.failure(Exception("No hay sesión iniciada"))
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
            if (!targetProfile.privacySettings.friendRequestPermissions) {
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

    suspend fun cancelSentFriendRequest(targetUser: Usuario): Result<Unit> {
        return try {
            if (isGuestSessionActive()) {
                return Result.failure(Exception("Las funciones sociales para invitado llegaran en una siguiente iteracion"))
            }

            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesión iniciada"))

            if (targetUser.uid.isBlank() || targetUser.uid == uid) {
                return Result.failure(Exception("Usuario no valido"))
            }

            val batch = firestore.batch()
            batch.delete(sentRequestsCollection(uid).document(targetUser.uid))
            batch.delete(receivedRequestsCollection(targetUser.uid).document(uid))
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
            val blockedByMe = blockedUserIds(uid)

            val friends = friendDocuments.mapNotNull { document ->
                val friendUid = document.getString("uid").orEmpty().ifBlank { document.id }
                if (friendUid in blockedByMe || isBlockedBetween(uid, friendUid)) {
                    return@mapNotNull null
                }
                val cachedFriend = snapshotToUser(document, friendUid)
                val friend = mergeUserProfile(
                    primary = getUserProfile(friendUid),
                    fallback = cachedFriend,
                    uid = friendUid
                )
                    ?: return@mapNotNull null

                val booksReadCount = if (friend.privacySettings.readingActivityVisible) {
                    uniqueBooksInAllListsCount(friendUid)
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
                ?: return Result.failure(Exception("No hay sesión iniciada"))

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
            val blockedByMe = blockedUserIds(uid)

            val activities = buildList {
                friendDocuments.forEach { document ->
                    val friendUid = document.getString("uid").orEmpty().ifBlank { document.id }
                    if (friendUid in blockedByMe || isBlockedBetween(uid, friendUid)) {
                        return@forEach
                    }
                    val cachedFriend = snapshotToUser(document, friendUid)
                    val friend = mergeUserProfile(
                        primary = getUserProfile(friendUid),
                        fallback = cachedFriend,
                        uid = friendUid
                    ) ?: return@forEach
                    if (!friend.privacySettings.profileVisible || !friend.privacySettings.readingActivityVisible) {
                        return@forEach
                    }

                    val friendshipTimestamp = document.timestampMillis("createdAt")
                    if (friendshipTimestamp == null || isWithinLastQuarter(friendshipTimestamp)) {
                        add(
                            FriendActivityItem(
                                id = activityId(
                                    ownerUid = friendUid,
                                    type = FriendActivityType.FRIENDSHIP,
                                    sourceId = uid,
                                    timestampMillis = friendshipTimestamp
                                ),
                                type = FriendActivityType.FRIENDSHIP,
                                user = friend.visibleTo(uid),
                                timestampMillis = friendshipTimestamp,
                                relatedUserName = currentUserName
                            )
                        )
                    }

                    addAll(bookListActivities(friendUid, friend, uid))
                }
            }

            val visibleActivities = activities
                .groupBy { activityOwnerUid(it.id) }
                .values
                .flatMap { ownerActivities ->
                    visibleActivityUpdates(
                        ownerUid = activityOwnerUid(ownerActivities.firstOrNull()?.id.orEmpty()),
                        activities = ownerActivities
                    )
                }

            Result.success(enrichWithSocial(sortActivitiesByRecency(visibleActivities), uid))
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

            if (friendUid != uid && isBlockedBetween(uid, friendUid)) {
                return Result.failure(Exception("No puedes ver este perfil"))
            }

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
            val isRequestReceived = receivedRequestsCollection(uid)
                .document(friendUid)
                .get()
                .await()
                .exists()
            val isPrivateProfile = friendUid != uid && !isFriend && !resolvedFriend.privacySettings.profileVisible

            if (isPrivateProfile) {
                return Result.success(
                    FriendProfileData(
                        user = resolvedFriend.visibleTo(uid),
                        isFriend = isFriend,
                        isRequestSent = isRequestSent,
                        isRequestReceived = isRequestReceived,
                        isPrivateProfile = true,
                        booksReadCount = 0,
                        friendsCount = 0,
                        readingBooks = emptyList(),
                        wantToReadBooks = emptyList(),
                        readBooks = emptyList(),
                        predefinedShelves = emptyList(),
                        friendPreviews = emptyList(),
                        groupsCount = 0,
                        updates = emptyList()
                    )
                )
            }

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
            val uniqueBooksInListsCount = if (canShowReadingActivity) {
                uniqueBooksInAllListsCount(friendUid)
            } else {
                0
            }

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
                    if (previewUid != uid && isBlockedBetween(uid, previewUid)) {
                        return@mapNotNull null
                    }
                    mergeUserProfile(
                        primary = getUserProfile(previewUid),
                        fallback = snapshotToUser(document, previewUid),
                        uid = previewUid
                    )
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
                    val friendshipTimestamp = friendDocuments
                        .firstOrNull { it.id == uid }
                        ?.timestampMillis("createdAt")
                    if (friendshipTimestamp == null || isWithinLastQuarter(friendshipTimestamp)) {
                    add(
                        FriendActivityItem(
                            id = activityId(
                                ownerUid = friendUid,
                                type = FriendActivityType.FRIENDSHIP,
                                sourceId = uid,
                                timestampMillis = friendshipTimestamp
                            ),
                            type = FriendActivityType.FRIENDSHIP,
                            user = resolvedFriend.visibleTo(uid),
                            timestampMillis = friendshipTimestamp,
                            relatedUserName = currentUserName
                        )
                    )
                    }
                }

                addAll(bookListActivities(friendUid, resolvedFriend, uid))
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
                    isRequestReceived = isRequestReceived,
                    isPrivateProfile = false,
                    booksReadCount = uniqueBooksInListsCount,
                    friendsCount = visibleFriendProfiles.size,
                    readingBooks = readingBooks.take(6),
                    wantToReadBooks = wantToReadBooks.take(6),
                    readBooks = booksRead.take(6),
                    predefinedShelves = predefinedShelves,
                    friendPreviews = friendPreviews.map { it.visibleTo(uid) },
                    groupsCount = 0,
                    updates = enrichWithSocial(
                        sortActivitiesByRecency(
                            visibleActivityUpdates(
                                ownerUid = friendUid,
                                activities = updates
                            )
                        ),
                        uid
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hideActivityUpdate(activityId: String): Result<Unit> {
        return try {
            if (isGuestSessionActive()) {
                return Result.failure(Exception("Las funciones sociales para invitado llegaran en una siguiente iteracion"))
            }

            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesión iniciada"))
            val ownerUid = activityOwnerUid(activityId)
            if (activityId.isBlank() || ownerUid != uid) {
                return Result.failure(Exception("No se pudo eliminar esta actualización"))
            }

            hiddenActivityUpdatesCollection(uid)
                .document(activityId)
                .set(
                    mapOf(
                        "activityId" to activityId,
                        "hiddenAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun blockMember(targetUid: String): Result<Unit> {
        return try {
            if (isGuestSessionActive()) {
                return Result.failure(Exception("Inicia sesión para bloquear perfiles"))
            }

            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesión iniciada"))
            if (targetUid.isBlank() || targetUid == uid) {
                return Result.failure(Exception("No se pudo identificar el perfil"))
            }

            val targetUser = getUserProfile(targetUid) ?: Usuario(uid = targetUid)
            val currentUser = getUserProfile(uid) ?: Usuario(uid = uid)
            val batch = firestore.batch()
            batch.set(
                blockedMembersCollection(uid).document(targetUid),
                mapOf(
                    "uid" to targetUser.uid,
                    "usuario" to targetUser.usuario,
                    "email" to targetUser.email,
                    "photoUrl" to targetUser.photoUrl,
                    "blockedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.delete(friendsCollection(uid).document(targetUid))
            batch.delete(friendsCollection(targetUid).document(uid))
            batch.delete(sentRequestsCollection(uid).document(targetUid))
            batch.delete(receivedRequestsCollection(uid).document(targetUid))
            batch.delete(sentRequestsCollection(targetUid).document(uid))
            batch.delete(receivedRequestsCollection(targetUid).document(uid))
            batch.set(
                usersCollection().document(uid),
                mapOf(
                    "uid" to currentUser.uid,
                    "usuario" to currentUser.usuario,
                    "email" to currentUser.email,
                    "photoUrl" to currentUser.photoUrl
                ),
                SetOptions.merge()
            )
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unblockMember(targetUid: String): Result<Unit> {
        return try {
            if (isGuestSessionActive()) {
                return Result.failure(Exception("Inicia sesión para desbloquear perfiles"))
            }

            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesión iniciada"))
            if (targetUid.isBlank()) {
                return Result.failure(Exception("No se pudo identificar el perfil"))
            }

            blockedMembersCollection(uid).document(targetUid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadBlockedMembers(): Result<List<BlockedMember>> {
        return try {
            if (isGuestSessionActive()) {
                return Result.success(emptyList())
            }

            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesión iniciada"))

            val members = blockedMembersCollection(uid)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    val blockedUid = document.getString("uid").orEmpty().ifBlank { document.id }
                    mergeUserProfile(
                        primary = getUserProfile(blockedUid),
                        fallback = snapshotToUser(document, blockedUid),
                        uid = blockedUid
                    )?.let { user ->
                        BlockedMember(
                            user = user,
                            blockedAtMillis = document.timestampMillis("blockedAt")
                        )
                    }
                }

            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportMember(
        targetUid: String,
        subject: String,
        message: String,
        photoUris: List<String>
    ): Result<Unit> {
        return try {
            if (isGuestSessionActive()) {
                return Result.failure(Exception("Inicia sesión para enviar denuncias"))
            }

            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesión iniciada"))
            val trimmedSubject = subject.trim()
            val trimmedMessage = message.trim()
            if (targetUid.isBlank() || targetUid == uid) {
                return Result.failure(Exception("No se pudo identificar el perfil"))
            }
            if (trimmedSubject.isBlank() || trimmedMessage.isBlank()) {
                return Result.failure(Exception("Completa el asunto y el mensaje"))
            }

            val reporter = getUserProfile(uid) ?: Usuario(uid = uid)
            val reported = getUserProfile(targetUid) ?: Usuario(uid = targetUid)
            val reportRef = accountReportsCollection().document()
            val reportData = mapOf(
                "id" to reportRef.id,
                "reporterUid" to uid,
                "reporterUsuario" to reporter.usuario,
                "reporterEmail" to reporter.email,
                "reporterPhotoUrl" to reporter.photoUrl,
                "reportedUid" to reported.uid,
                "reportedUsuario" to reported.usuario,
                "reportedEmail" to reported.email,
                "reportedPhotoUrl" to reported.photoUrl,
                "subject" to trimmedSubject,
                "message" to trimmedMessage,
                "photoUris" to photoUris.filter { it.isNotBlank() },
                "status" to AccountReportStatus.PENDING.name,
                "adminMessage" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = firestore.batch()
            batch.set(reportRef, reportData)
            batch.set(reportReviewsCollection(uid).document(reportRef.id), reportData)
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadMyReports(): Result<List<AccountReport>> {
        return try {
            if (isGuestSessionActive()) {
                return Result.success(emptyList())
            }

            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesión iniciada"))

            val reports = reportReviewsCollection(uid)
                .get()
                .await()
                .documents
                .map { document ->
                    AccountReport(
                        id = document.getString("id").orEmpty().ifBlank { document.id },
                        reportedUser = Usuario(
                            uid = document.getString("reportedUid").orEmpty(),
                            usuario = document.getString("reportedUsuario").orEmpty(),
                            email = document.getString("reportedEmail").orEmpty(),
                            photoUrl = document.getString("reportedPhotoUrl").orEmpty()
                        ),
                        subject = document.getString("subject").orEmpty(),
                        message = document.getString("message").orEmpty(),
                        photoUris = (document.get("photoUris") as? List<*>)
                            ?.mapNotNull { it as? String }
                            .orEmpty(),
                        status = runCatching {
                            AccountReportStatus.valueOf(document.getString("status").orEmpty())
                        }.getOrDefault(AccountReportStatus.PENDING),
                        adminMessage = document.getString("adminMessage").orEmpty(),
                        createdAtMillis = document.timestampMillis("createdAt"),
                        updatedAtMillis = document.timestampMillis("updatedAt")
                    )
                }
                .sortedByDescending { it.createdAtMillis ?: Long.MIN_VALUE }

            Result.success(reports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleActivityLike(activityId: String): Result<ActivitySocialSummary> {
        return try {
            if (isGuestSessionActive()) {
                return Result.failure(Exception("Inicia sesión para indicar que te gusta una publicación"))
            }

            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesión iniciada"))
            if (activityId.isBlank()) {
                return Result.failure(Exception("No se pudo identificar la publicación"))
            }
            activitySocialInteractionError(activityId)?.let { error ->
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
                return Result.failure(Exception("No se pudo identificar la publicación"))
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

    suspend fun loadActivityNotifications(): Result<List<ActivityNotificationItem>> {
        return try {
            if (isGuestSessionActive()) {
                return Result.success(emptyList())
            }

            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesión iniciada"))
            val readNotificationIds = activityNotificationReadsCollection(uid)
                .get()
                .await()
                .documents
                .map { it.id }
                .toSet()
            val currentUser = getUserProfile(uid) ?: Usuario(uid = uid)
            val friendshipActivities = friendsCollection(uid)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    val friendUid = document.getString("uid").orEmpty().ifBlank { document.id }
                    if (friendUid.isBlank()) return@mapNotNull null
                    val timestamp = document.timestampMillis("createdAt")
                    FriendActivityItem(
                        id = activityId(
                            ownerUid = uid,
                            type = FriendActivityType.FRIENDSHIP,
                            sourceId = friendUid,
                            timestampMillis = timestamp
                        ),
                        type = FriendActivityType.FRIENDSHIP,
                        user = currentUser.visibleTo(uid),
                        timestampMillis = timestamp,
                        relatedUserName = document.getString("usuario").orEmpty()
                    )
                }
            val ownActivities = enrichWithSocial(
                visibleActivityUpdates(
                    ownerUid = uid,
                    activities = friendshipActivities + bookListActivities(uid, currentUser, uid)
                ),
                uid
            ).associateBy { it.id }

            val notifications = activitySocialCollection()
                .get()
                .await()
                .documents
                .filter { document -> activityOwnerUid(document.id) == uid }
                .flatMap { document ->
                    val activityId = document.id
                    val activity = ownActivities[activityId]
                        ?: return@flatMap emptyList<ActivityNotificationItem>()
                    val likes = activityLikesCollection(activityId)
                        .get()
                        .await()
                        .documents
                        .mapNotNull { likeDocument ->
                            val actorUid = likeDocument.getString("uid").orEmpty().ifBlank { likeDocument.id }
                            if (actorUid == uid) return@mapNotNull null

                            val actor = getUserProfile(actorUid) ?: Usuario(
                                uid = actorUid,
                                usuario = likeDocument.getString("usuario").orEmpty(),
                                email = likeDocument.getString("email").orEmpty(),
                                photoUrl = likeDocument.getString("photoUrl").orEmpty()
                            )

                            ActivityNotificationItem(
                                id = notificationId(activityId, "like", likeDocument.id),
                                type = ActivityNotificationType.LIKE,
                                activityId = activityId,
                                user = actor.visibleTo(uid),
                                activity = activity,
                                timestampMillis = likeDocument.timestampMillis("createdAt"),
                                isRead = notificationId(activityId, "like", likeDocument.id) in readNotificationIds
                            )
                        }

                    val comments = activityCommentsCollection(activityId)
                        .get()
                        .await()
                        .documents
                        .mapNotNull { commentDocument ->
                            val actorUid = commentDocument.getString("uid").orEmpty()
                            if (actorUid.isBlank() || actorUid == uid) return@mapNotNull null

                            val actor = getUserProfile(actorUid) ?: Usuario(
                                uid = actorUid,
                                usuario = commentDocument.getString("usuario").orEmpty(),
                                email = commentDocument.getString("email").orEmpty(),
                                photoUrl = commentDocument.getString("photoUrl").orEmpty()
                            )

                            ActivityNotificationItem(
                                id = notificationId(activityId, "comment", commentDocument.id),
                                type = ActivityNotificationType.COMMENT,
                                activityId = activityId,
                                user = actor.visibleTo(uid),
                                activity = activity,
                                text = commentDocument.getString("text").orEmpty(),
                                timestampMillis = commentDocument.timestampMillis("createdAt"),
                                isRead = notificationId(activityId, "comment", commentDocument.id) in readNotificationIds
                            )
                        }

                    likes + comments
                }
                .sortedByDescending { it.timestampMillis ?: Long.MIN_VALUE }

            Result.success(notifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeActivityNotificationChanges(onChange: () -> Unit): ListenerRegistration? {
        if (isGuestSessionActive()) {
            return null
        }

        val uid = currentUid() ?: return null
        val socialListener = activitySocialCollection().addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val hasOwnActivityChange = snapshot
                ?.documentChanges
                ?.any { change -> activityOwnerUid(change.document.id) == uid } == true
            if (hasOwnActivityChange) {
                onChange()
            }
        }
        val likesListener = firestore.collectionGroup("likes").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val hasOwnLikeChange = snapshot
                ?.documentChanges
                ?.any { change ->
                    val activityId = change.document.reference.parent.parent?.id.orEmpty()
                    activityOwnerUid(activityId) == uid
                } == true
            if (hasOwnLikeChange) {
                onChange()
            }
        }
        val commentsListener = firestore.collectionGroup("comments").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val hasOwnCommentChange = snapshot
                ?.documentChanges
                ?.any { change ->
                    val activityId = change.document.reference.parent.parent?.id.orEmpty()
                    activityOwnerUid(activityId) == uid
                } == true
            if (hasOwnCommentChange) {
                onChange()
            }
        }
        val readsListener = activityNotificationReadsCollection(uid).addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot?.documentChanges?.isNotEmpty() == true) {
                onChange()
            }
        }

        return CompositeListenerRegistration(
            listOf(socialListener, likesListener, commentsListener, readsListener)
        )
    }

    suspend fun markActivityNotificationRead(notificationId: String): Result<Unit> {
        return try {
            if (isGuestSessionActive()) {
                return Result.success(Unit)
            }

            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesión iniciada"))
            if (notificationId.isBlank()) {
                return Result.failure(Exception("No se pudo identificar la notificación"))
            }

            activityNotificationReadsCollection(uid)
                .document(notificationId)
                .set(mapOf("readAt" to FieldValue.serverTimestamp()), SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addActivityComment(activityId: String, text: String): Result<Pair<ActivitySocialSummary, List<ActivityComment>>> {
        return try {
            if (isGuestSessionActive()) {
                return Result.failure(Exception("Inicia sesión para comentar una publicación"))
            }

            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesión iniciada"))
            val trimmedText = text.trim()
            if (activityId.isBlank()) {
                return Result.failure(Exception("No se pudo identificar la publicación"))
            }
            if (trimmedText.isBlank()) {
                return Result.failure(Exception("Escribe un comentario"))
            }
            activitySocialInteractionError(activityId)?.let { error ->
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
                ?: return Result.failure(Exception("No hay sesión iniciada"))
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
                            SYSTEM_LIST_PENDING_ID -> 3
                            SYSTEM_LIST_UNFINISHED_ID -> 4
                            else -> 5
                        }
                    }.thenBy { it.name.lowercase() }
                )

            Result.success(lists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadFriendTags(friendUid: String): Result<List<FriendBookTagSummary>> {
        return try {
            if (isGuestSessionActive()) {
                return Result.failure(Exception("Las etiquetas sociales para invitado llegaran en una siguiente iteracion"))
            }

            if (friendUid.isBlank()) {
                return Result.failure(Exception("No se pudo identificar al usuario"))
            }

            val uid = currentUid()
                ?: return Result.failure(Exception("No hay sesión iniciada"))
            val friend = getUserProfile(friendUid)
                ?: return Result.failure(Exception("No se pudo cargar el perfil del usuario"))
            if (!canOpenReadingActivity(friendUid, uid, friend)) {
                return Result.failure(Exception("Este usuario no muestra su actividad de lectura ahora mismo"))
            }

            Result.success(loadFriendTagSummaries(friendUid))
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
                ?: return Result.failure(Exception("No hay sesión iniciada"))
            val friend = getUserProfile(friendUid)
                ?: return Result.failure(Exception("No se pudo cargar el perfil del usuario"))
            if (!canOpenReadingActivity(friendUid, uid, friend)) {
                return Result.failure(Exception("Este usuario no muestra su actividad de lectura ahora mismo"))
            }

            if (listId.startsWith(FRIEND_TAG_DETAIL_PREFIX)) {
                val tagId = listId.removePrefix(FRIEND_TAG_DETAIL_PREFIX)
                val tagDocument = tagsCollection(friendUid)
                    .document(tagId)
                    .get()
                    .await()
                val tagName = tagDocument.getString("name").orEmpty()
                val items = loadFriendTagBooks(friendUid, tagId)

                return Result.success(
                    FriendBookListDetail(
                        list = UserBookList(
                            id = listId,
                            name = tagName,
                            bookCount = items.size
                        ),
                        books = items
                    )
                )
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
                        listId == SYSTEM_LIST_READ_ID ||
                        listId == SYSTEM_LIST_UNFINISHED_ID ||
                        listId == SYSTEM_LIST_PENDING_ID
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
                    "photoUrl" to requestUser.photoUrl,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                requestUserFriendRef,
                mapOf(
                    "uid" to currentUser.uid,
                    "usuario" to currentUser.usuario,
                    "email" to currentUser.email,
                    "photoUrl" to currentUser.photoUrl,
                    "createdAt" to FieldValue.serverTimestamp()
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


