package com.example.kaishelvesapp.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.UserBookList
import com.example.kaishelvesapp.data.repository.FriendBookListSummary
import com.example.kaishelvesapp.data.repository.FriendShelfPreview
import com.example.kaishelvesapp.data.repository.UserListsRepository

@Composable
fun UserBookList.localizedName(): String {
    val resource = systemListNameResource(id, systemKey)
    return resource?.let { stringResource(it) } ?: name
}

@Composable
fun UserBookList.localizedDescription(): String {
    val resource = systemListDescriptionResource(id, systemKey)
    return resource?.let { stringResource(it) } ?: description
}

@Composable
fun FriendBookListSummary.localizedName(): String {
    return systemListNameResource(id)?.let { stringResource(it) } ?: name
}

@Composable
fun FriendShelfPreview.localizedName(): String {
    return systemListNameResource(listId)?.let { stringResource(it) } ?: title
}

private fun systemListNameResource(id: String, systemKey: String = ""): Int? {
    return when {
        id == UserListsRepository.SYSTEM_LIST_WANT_TO_READ_ID ||
            systemKey == UserListsRepository.SYSTEM_LIST_WANT_TO_READ_KEY -> R.string.system_list_want_to_read
        id == UserListsRepository.SYSTEM_LIST_READING_ID ||
            systemKey == UserListsRepository.SYSTEM_LIST_READING_KEY -> R.string.system_list_reading
        id == UserListsRepository.SYSTEM_LIST_READ_ID ||
            systemKey == UserListsRepository.SYSTEM_LIST_READ_KEY -> R.string.system_list_read
        id == UserListsRepository.SYSTEM_LIST_PENDING_ID ||
            systemKey == UserListsRepository.SYSTEM_LIST_PENDING_KEY -> R.string.system_list_pending
        id == UserListsRepository.SYSTEM_LIST_UNFINISHED_ID ||
            systemKey == UserListsRepository.SYSTEM_LIST_UNFINISHED_KEY -> R.string.system_list_unfinished
        id == UserListsRepository.SYSTEM_LIST_OWNED_ID ||
            systemKey == UserListsRepository.SYSTEM_LIST_OWNED_KEY -> R.string.system_list_owned
        else -> null
    }
}

private fun systemListDescriptionResource(id: String, systemKey: String = ""): Int? {
    return when {
        id == UserListsRepository.SYSTEM_LIST_WANT_TO_READ_ID ||
            systemKey == UserListsRepository.SYSTEM_LIST_WANT_TO_READ_KEY -> R.string.system_list_want_to_read_description
        id == UserListsRepository.SYSTEM_LIST_READING_ID ||
            systemKey == UserListsRepository.SYSTEM_LIST_READING_KEY -> R.string.system_list_reading_description
        id == UserListsRepository.SYSTEM_LIST_READ_ID ||
            systemKey == UserListsRepository.SYSTEM_LIST_READ_KEY -> R.string.system_list_read_description
        id == UserListsRepository.SYSTEM_LIST_PENDING_ID ||
            systemKey == UserListsRepository.SYSTEM_LIST_PENDING_KEY -> R.string.system_list_pending_description
        id == UserListsRepository.SYSTEM_LIST_UNFINISHED_ID ||
            systemKey == UserListsRepository.SYSTEM_LIST_UNFINISHED_KEY -> R.string.system_list_unfinished_description
        id == UserListsRepository.SYSTEM_LIST_OWNED_ID ||
            systemKey == UserListsRepository.SYSTEM_LIST_OWNED_KEY -> R.string.system_list_owned_description
        else -> null
    }
}
