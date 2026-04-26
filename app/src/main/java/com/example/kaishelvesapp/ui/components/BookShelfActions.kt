package com.example.kaishelvesapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.UserBookList
import com.example.kaishelvesapp.data.model.UserBookTag
import com.example.kaishelvesapp.data.repository.UserListsRepository
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.BookDetailUiState
import com.example.kaishelvesapp.ui.viewmodel.BookDetailViewModel

@Composable
fun BookShelfActions(
    book: Libro,
    modifier: Modifier = Modifier,
    viewModelKeyPrefix: String = "book_shelf"
) {
    val safeBookId = book.id.ifBlank { book.isbn }
    val detailViewModel: BookDetailViewModel = viewModel(key = "${viewModelKeyPrefix}_$safeBookId")
    val uiState by detailViewModel.uiState.collectAsStateWithLifecycle()
    var showOrganizerDialog by rememberSaveable(safeBookId) { mutableStateOf(false) }
    var showCreateDialog by rememberSaveable(safeBookId) { mutableStateOf(false) }
    var showRemoveDialog by rememberSaveable(safeBookId) { mutableStateOf(false) }

    LaunchedEffect(safeBookId) {
        detailViewModel.cargarListasParaLibro(safeBookId)
    }

    if (showOrganizerDialog) {
        BookShelfOrganizationDialog(
            libro = book,
            availableLists = uiState.availableLists,
            availableTags = uiState.availableTags,
            initiallySelectedListId = uiState.selectedListIds.firstOrNull(),
            initiallySelectedTagIds = uiState.selectedTagIds,
            isSaving = uiState.isSavingLists,
            onDismiss = {
                if (!uiState.isSavingLists) {
                    showOrganizerDialog = false
                }
            },
            onSave = { selectedListId, selectedTagIds ->
                detailViewModel.guardarOrganizacion(
                    libro = book,
                    selectedListIds = selectedListId?.let(::setOf).orEmpty(),
                    selectedTagIds = selectedTagIds
                )
                showOrganizerDialog = false
            },
            onCreateNew = { showCreateDialog = true },
            onRemoveFromMyBooks = { showRemoveDialog = true }
        )
    }

    if (showCreateDialog) {
        BookShelfCreateDialog(
            isSaving = uiState.isSavingLists,
            onDismiss = {
                if (!uiState.isSavingLists) {
                    showCreateDialog = false
                }
            },
            onCreateShelf = { name ->
                detailViewModel.createList(name = name, bookId = safeBookId)
                showCreateDialog = false
            },
            onCreateTag = { name ->
                detailViewModel.createTag(name = name, bookId = safeBookId)
                showCreateDialog = false
            }
        )
    }

    if (showRemoveDialog) {
        Dialog(onDismissRequest = { if (!uiState.isSavingLists) showRemoveDialog = false }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Obsidian,
                border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.remove_from_my_books),
                        color = TarnishedGold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(R.string.book_lists_updated),
                        color = OldIvory,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showRemoveDialog = false },
                            enabled = !uiState.isSavingLists
                        ) {
                            Text(text = stringResource(R.string.cancel), color = OldIvory)
                        }
                        TextButton(
                            onClick = {
                                detailViewModel.clearBookOrganization(safeBookId)
                                showRemoveDialog = false
                            },
                            enabled = !uiState.isSavingLists
                        ) {
                            Text(text = stringResource(R.string.delete), color = Color(0xFFE57A6D))
                        }
                    }
                }
            }
        }
    }

    BookShelfActionRow(
        uiState = uiState,
        modifier = modifier,
        onPrimaryShelfAction = {
            if (uiState.selectedListIds.isEmpty()) {
                detailViewModel.guardarOrganizacion(
                    libro = book,
                    selectedListIds = setOf(UserListsRepository.SYSTEM_LIST_WANT_TO_READ_ID),
                    selectedTagIds = uiState.selectedTagIds
                )
            } else {
                showOrganizerDialog = true
            }
        },
        onOpenOrganizer = { showOrganizerDialog = true }
    )
}

@Composable
private fun BookShelfActionRow(
    uiState: BookDetailUiState,
    modifier: Modifier,
    onPrimaryShelfAction: () -> Unit,
    onOpenOrganizer: () -> Unit
) {
    val selectedList = remember(uiState.availableLists, uiState.selectedListIds) {
        uiState.availableLists.firstOrNull { it.id in uiState.selectedListIds }
    }
    val buttonLabel = selectedList?.name ?: stringResource(R.string.want_to_read)
    val isAddedToShelf = selectedList != null
    val buttonColor = if (isAddedToShelf) {
        BloodWine.copy(alpha = 0.72f)
    } else {
        Obsidian.copy(alpha = 0.42f)
    }
    val checkTint = when (selectedList?.id) {
        UserListsRepository.SYSTEM_LIST_READ_ID -> Color(0xFF5BBF72)
        UserListsRepository.SYSTEM_LIST_READING_ID -> Color(0xFFE2B84C)
        null -> OldIvory
        else -> Color(0xFFB7B4B0)
    }

    if (uiState.isListsLoading || uiState.isSavingLists) {
        androidx.compose.material3.Button(
            onClick = {},
            modifier = modifier.fillMaxWidth(),
            enabled = false,
            colors = KaiShelvesThemeDefaults.secondaryButtonColors()
        ) {
            CircularProgressIndicator(color = OldIvory)
        }
    } else if (uiState.availableLists.isNotEmpty()) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = buttonColor,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onPrimaryShelfAction)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isAddedToShelf) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = checkTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = buttonLabel,
                        color = OldIvory,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(TarnishedGold.copy(alpha = 0.16f))
                )

                Box(
                    modifier = Modifier
                        .width(58.dp)
                        .height(54.dp)
                        .clickable(onClick = onOpenOrganizer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = stringResource(R.string.open_book_organizer),
                        tint = OldIvory,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    } else {
        OutlinedButton(
            onClick = {},
            modifier = modifier.fillMaxWidth(),
            enabled = false,
            border = BorderStroke(1.dp, TarnishedGold)
        ) {
            Text(
                text = stringResource(R.string.no_user_lists_available),
                color = TarnishedGold
            )
        }
    }
}

@Composable
private fun BookShelfOrganizationDialog(
    libro: Libro,
    availableLists: List<UserBookList>,
    availableTags: List<UserBookTag>,
    initiallySelectedListId: String?,
    initiallySelectedTagIds: Set<String>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String?, Set<String>) -> Unit,
    onCreateNew: () -> Unit,
    onRemoveFromMyBooks: () -> Unit
) {
    var selectedListId by remember(initiallySelectedListId) { mutableStateOf(initiallySelectedListId) }
    var selectedTagIds by remember(initiallySelectedTagIds) { mutableStateOf(initiallySelectedTagIds) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = Obsidian.copy(alpha = 0.98f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, enabled = !isSaving) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = OldIvory
                        )
                    }

                    Text(
                        text = stringResource(R.string.add_to_my_books),
                        color = TarnishedGold,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { onSave(selectedListId, selectedTagIds) },
                        enabled = !isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = stringResource(R.string.save),
                            tint = if (isSaving) OldIvory.copy(alpha = 0.5f) else TarnishedGold
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Obsidian),
                    border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BookCover(
                            imageUrl = libro.imagen,
                            title = libro.titulo,
                            modifier = Modifier
                                .width(72.dp)
                                .height(104.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = libro.titulo.ifBlank { stringResource(R.string.unknown_title) },
                                color = TarnishedGold,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (libro.autor.isNotBlank()) {
                                Text(
                                    text = stringResource(R.string.book_by_author, libro.autor),
                                    color = OldIvory,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }

                BookShelfSectionTitle(title = stringResource(R.string.select_shelf))

                availableLists.forEach { list ->
                    BookShelfOptionRow(
                        list = list,
                        isSelected = selectedListId == list.id,
                        onSelect = { selectedListId = list.id }
                    )
                }

                BookShelfSectionTitle(title = stringResource(R.string.select_tags))

                if (availableTags.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_tags_available),
                        color = OldIvory,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableTags.forEach { tag ->
                            val isSelected = tag.id in selectedTagIds
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (isSelected) BloodWine else Obsidian,
                                border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.35f)),
                                modifier = Modifier.clickable {
                                    selectedTagIds = if (isSelected) selectedTagIds - tag.id else selectedTagIds + tag.id
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = OldIvory,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = tag.name,
                                        color = OldIvory,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onCreateNew,
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.create_new_tag_or_shelf),
                        color = TarnishedGold
                    )
                }

                TextButton(
                    onClick = onRemoveFromMyBooks,
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.remove_from_my_books),
                        color = Color(0xFFE57A6D)
                    )
                }
            }
        }
    }
}

@Composable
private fun BookShelfSectionTitle(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = TarnishedGold,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(1.dp)
                .background(TarnishedGold.copy(alpha = 0.25f))
        )
    }
}

@Composable
private fun BookShelfOptionRow(
    list: UserBookList,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = list.name,
                color = OldIvory,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.list_books_count, list.bookCount),
                color = OldIvory.copy(alpha = 0.76f),
                style = MaterialTheme.typography.bodySmall
            )
        }

        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
    }

    HorizontalDivider(
        color = TarnishedGold.copy(alpha = 0.14f),
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@Composable
private fun BookShelfCreateDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onCreateShelf: (String) -> Unit,
    onCreateTag: (String) -> Unit
) {
    var selectedType by rememberSaveable { mutableStateOf(BookShelfCreateType.SHELF) }
    var name by rememberSaveable { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Obsidian,
            border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.35f))
        ) {
            Column {
                Surface(
                    color = BloodWine,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.create),
                        color = OldIvory,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                    )
                }

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedType = BookShelfCreateType.TAG }
                        ) {
                            RadioButton(
                                selected = selectedType == BookShelfCreateType.TAG,
                                onClick = { selectedType = BookShelfCreateType.TAG }
                            )
                            Text(text = stringResource(R.string.tag), color = OldIvory)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedType = BookShelfCreateType.SHELF }
                        ) {
                            RadioButton(
                                selected = selectedType == BookShelfCreateType.SHELF,
                                onClick = { selectedType = BookShelfCreateType.SHELF }
                            )
                            Text(text = stringResource(R.string.shelf), color = OldIvory)
                        }
                    }

                    Text(
                        text = if (selectedType == BookShelfCreateType.SHELF) {
                            stringResource(R.string.single_shelf_note)
                        } else {
                            stringResource(R.string.tag_create_note)
                        },
                        color = OldIvory,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                text = if (selectedType == BookShelfCreateType.SHELF) {
                                    stringResource(R.string.shelf_name_hint)
                                } else {
                                    stringResource(R.string.tag_name_hint)
                                }
                            )
                        },
                        enabled = !isSaving
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss, enabled = !isSaving) {
                            Text(text = stringResource(R.string.cancel), color = OldIvory)
                        }
                        TextButton(
                            onClick = {
                                val trimmedName = name.trim()
                                if (trimmedName.isNotBlank()) {
                                    if (selectedType == BookShelfCreateType.SHELF) {
                                        onCreateShelf(trimmedName)
                                    } else {
                                        onCreateTag(trimmedName)
                                    }
                                }
                            },
                            enabled = !isSaving && name.isNotBlank()
                        ) {
                            Text(text = stringResource(R.string.save), color = TarnishedGold)
                        }
                    }
                }
            }
        }
    }
}

private enum class BookShelfCreateType {
    TAG,
    SHELF
}
