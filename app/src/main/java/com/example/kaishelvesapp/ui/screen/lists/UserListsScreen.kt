package com.example.kaishelvesapp.ui.screen.lists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.UserBookList
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiNavigationDrawerContent
import com.example.kaishelvesapp.ui.components.KaiPrimaryTopBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.UserListsViewModel
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UserListsScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: UserListsViewModel,
    onOpenList: (String) -> Unit,
    onGoToProfile: () -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit,
    onSectionSelected: (KaiSection) -> Unit
) {
    val listItemsStartIndex = 1
    val showDragDebug = false
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var editingList by remember { mutableStateOf<UserBookList?>(null) }
    var deletingList by remember { mutableStateOf<UserBookList?>(null) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    val displayedLists = remember { mutableStateListOf<UserBookList>() }
    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var draggingTranslationY by remember { mutableFloatStateOf(0f) }
    var autoScrollDelta by remember { mutableFloatStateOf(0f) }
    var dragDebugMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadLists()
    }

    LaunchedEffect(uiState.lists) {
        if (draggingItemId == null) {
            displayedLists.clear()
            displayedLists.addAll(uiState.lists)
        }
    }

    LaunchedEffect(draggingItemId, autoScrollDelta) {
        while (draggingItemId != null && autoScrollDelta != 0f) {
            listState.scrollBy(autoScrollDelta)
            draggingTranslationY += autoScrollDelta
            delay(16)
        }
    }

    LaunchedEffect(uiState.errorMessageRes, uiState.successMessageRes) {
        uiState.errorMessageRes?.let { messageRes ->
            snackbarHostState.showSnackbar(context.getString(messageRes))
            viewModel.clearMessages()
        }

        uiState.successMessageRes?.let { messageRes ->
            snackbarHostState.showSnackbar(context.getString(messageRes))
            viewModel.clearMessages()
        }
    }

    if (showCreateDialog) {
        ListEditorDialog(
            title = stringResource(R.string.create_new_list),
            confirmLabel = stringResource(R.string.save),
            initialName = "",
            initialDescription = "",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description ->
                viewModel.createList(name, description)
                if (name.trim().isNotBlank()) {
                    showCreateDialog = false
                }
            }
        )
    }

    editingList?.let { list ->
        ListEditorDialog(
            title = stringResource(R.string.edit_list),
            confirmLabel = stringResource(R.string.save),
            initialName = list.name,
            initialDescription = list.description,
            onDismiss = { editingList = null },
            onConfirm = { name, description ->
                viewModel.updateList(list.id, name, description)
                if (name.trim().isNotBlank()) {
                    editingList = null
                }
            }
        )
    }

    deletingList?.let { list ->
        AlertDialog(
            onDismissRequest = { deletingList = null },
            title = {
                Text(
                    text = stringResource(R.string.delete_list),
                    color = TarnishedGold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_list_confirmation, list.name),
                    color = OldIvory
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteList(list.id)
                        deletingList = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = TarnishedGold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingList = null }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = OldIvory
                    )
                }
            },
            containerColor = Obsidian
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KaiNavigationDrawerContent(
                currentSection = KaiSection.LISTS,
                headerTitle = stringResource(R.string.lists),
                subtitle = stringResource(R.string.lists_subtitle),
                onSectionSelected = { section ->
                    scope.launch { drawerState.close() }
                    onSectionSelected(section)
                }
            )
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                KaiPrimaryTopBar(
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    onGoToProfile = onGoToProfile,
                    onGoToSettingsPrivacy = onGoToSettingsPrivacy,
                    onLogout = onLogout
                )
            },
            bottomBar = {
                KaiBottomBar(
                    current = KaiSection.LISTS,
                    onSelect = onSectionSelected
                )
            }
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ListsHeroCard(
                        onCreateList = { showCreateDialog = true }
                    )
                }

                if (uiState.isLoading && uiState.lists.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TarnishedGold)
                        }
                    }
                } else if (uiState.lists.isEmpty()) {
                    item {
                        EmptyListsCard(
                            onCreateList = { showCreateDialog = true }
                        )
                    }
                } else {
                    itemsIndexed(
                        items = displayedLists,
                        key = { _, list -> list.id }
                    ) { index, list ->
                        val isDragging = draggingItemId == list.id
                        UserListCard(
                            userList = list,
                            onOpen = { onOpenList(list.id) },
                            onEdit = { editingList = list },
                            onDelete = { deletingList = list },
                            isDragging = isDragging,
                            dragHandleModifier = Modifier.pointerInput(list.id, displayedLists.size) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingItemId = list.id
                                        draggingTranslationY = 0f
                                        autoScrollDelta = 0f
                                        dragDebugMessage = "start id=${list.id} index=$index"
                                    },
                                    onDragCancel = {
                                        val orderedIds = displayedLists.map { it.id }
                                        val orderChanged = orderedIds != uiState.lists.map { it.id }
                                        draggingItemId = null
                                        draggingTranslationY = 0f
                                        autoScrollDelta = 0f
                                        dragDebugMessage = "cancel changed=$orderChanged order=$orderedIds"
                                        if (orderChanged) {
                                            viewModel.updateListOrder(orderedIds)
                                        } else {
                                            displayedLists.clear()
                                            displayedLists.addAll(uiState.lists)
                                        }
                                    },
                                    onDragEnd = {
                                        val orderedIds = displayedLists.map { it.id }
                                        val orderChanged = orderedIds != uiState.lists.map { it.id }
                                        draggingItemId = null
                                        draggingTranslationY = 0f
                                        autoScrollDelta = 0f
                                        dragDebugMessage = "end changed=$orderChanged order=$orderedIds"
                                        if (orderChanged) {
                                            viewModel.updateListOrder(orderedIds)
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val currentDraggingId = draggingItemId ?: return@detectDragGesturesAfterLongPress
                                        val currentIndex = displayedLists.indexOfFirst { it.id == currentDraggingId }
                                        if (currentIndex == -1) return@detectDragGesturesAfterLongPress

                                        draggingTranslationY += dragAmount.y

                                        val visibleItems = listState.layoutInfo.visibleItemsInfo
                                        val currentLayoutIndex = currentIndex + listItemsStartIndex
                                        val currentItem = visibleItems.firstOrNull { it.index == currentLayoutIndex }
                                            ?: return@detectDragGesturesAfterLongPress
                                        val currentMidPoint = currentItem.offset + currentItem.size / 2 + draggingTranslationY
                                        val viewportStart = listState.layoutInfo.viewportStartOffset
                                        val viewportEnd = listState.layoutInfo.viewportEndOffset
                                        val currentTop = currentItem.offset + draggingTranslationY
                                        val currentBottom = currentTop + currentItem.size

                                        autoScrollDelta = when {
                                            currentBottom > viewportEnd - 120 -> {
                                                val intensity = ((currentBottom - (viewportEnd - 120)) / 120f)
                                                    .coerceIn(0.2f, 1f)
                                                10f + (24f * intensity)
                                            }
                                            currentTop < viewportStart + 120 -> {
                                                val intensity = (((viewportStart + 120) - currentTop) / 120f)
                                                    .coerceIn(0.2f, 1f)
                                                -(10f + (24f * intensity))
                                            }
                                            else -> 0f
                                        }

                                        val previousItem = visibleItems.firstOrNull {
                                            it.index == currentLayoutIndex - 1 && it.index >= listItemsStartIndex
                                        }
                                        val nextItem = visibleItems.firstOrNull {
                                            it.index == currentLayoutIndex + 1
                                        }

                                        when {
                                            nextItem != null && currentMidPoint > nextItem.offset + nextItem.size / 2 -> {
                                                val targetListIndex = (nextItem.index - listItemsStartIndex)
                                                    .coerceAtMost(displayedLists.lastIndex)
                                                val deltaToTarget = (nextItem.offset - currentItem.offset).toFloat()
                                                displayedLists.move(currentIndex, targetListIndex)
                                                draggingTranslationY -= deltaToTarget
                                                dragDebugMessage = "down from=$currentIndex to=$targetListIndex mid=$currentMidPoint"
                                            }

                                            previousItem != null && currentMidPoint < previousItem.offset + previousItem.size / 2 -> {
                                                val targetListIndex = (previousItem.index - listItemsStartIndex)
                                                    .coerceAtLeast(0)
                                                val deltaToTarget = (currentItem.offset - previousItem.offset).toFloat()
                                                displayedLists.move(currentIndex, targetListIndex)
                                                draggingTranslationY += deltaToTarget
                                                dragDebugMessage = "up from=$currentIndex to=$targetListIndex mid=$currentMidPoint"
                                            }

                                            showDragDebug -> {
                                                dragDebugMessage = "drag idx=$currentIndex y=${draggingTranslationY.toInt()} mid=${currentMidPoint.toInt()}"
                                            }
                                        }
                                    }
                                )
                            },
                            modifier = Modifier
                                .zIndex(if (isDragging) 1f else 0f)
                                .shadow(
                                    elevation = if (isDragging) 18.dp else 0.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    clip = false
                                )
                                .graphicsLayer {
                                    translationY = if (isDragging) draggingTranslationY else 0f
                                    scaleX = if (isDragging) 1.02f else 1f
                                    scaleY = if (isDragging) 1.02f else 1f
                                }
                        )
                    }
                }

                if (showDragDebug) {
                    item {
                        Text(
                            text = dragDebugMessage.ifBlank { "drag-debug" },
                            color = TarnishedGold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListsHeroCard(
    onCreateList: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, TarnishedGold)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DeepWalnut,
                            BloodWine.copy(alpha = 0.55f),
                            Obsidian
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                contentDescription = null,
                tint = TarnishedGold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.lists_intro_title),
                style = MaterialTheme.typography.headlineMedium,
                color = TarnishedGold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.lists_intro_body),
                style = MaterialTheme.typography.bodyLarge,
                color = OldIvory
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onCreateList,
                colors = KaiShelvesThemeDefaults.primaryButtonColors()
            ) {
                Icon(
                    imageVector = Icons.Filled.PostAdd,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.create_new_list))
            }
        }
    }
}

@Composable
private fun EmptyListsCard(
    onCreateList: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.75f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.empty_lists_title),
                style = MaterialTheme.typography.titleLarge,
                color = TarnishedGold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.empty_lists_body),
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCreateList,
                border = BorderStroke(1.dp, TarnishedGold)
            ) {
                Text(
                    text = stringResource(R.string.create_new_list),
                    color = TarnishedGold
                )
            }
        }
    }
}

@Composable
private fun UserListCard(
    userList: UserBookList,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isDragging: Boolean,
    dragHandleModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isDragging) { onOpen() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = kotlin.collections.listOf(
                            BloodWine.copy(alpha = 0.18f),
                            DeepWalnut.copy(alpha = 0.92f),
                            Obsidian
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = userList.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TarnishedGold,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = userList.description.ifBlank { stringResource(R.string.lists_subtitle) },
                        style = MaterialTheme.typography.bodySmall,
                        color = OldIvory,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = dragHandleModifier.padding(horizontal = 8.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DragIndicator,
                            contentDescription = stringResource(R.string.drag_to_reorder),
                            tint = OldIvory.copy(alpha = 0.75f)
                        )
                    }

                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.edit_list),
                            tint = TarnishedGold
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete_list),
                            tint = TarnishedGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                    contentDescription = null,
                    tint = TarnishedGold
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.list_books_count, userList.bookCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldIvory,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = stringResource(R.string.open_list),
                    style = MaterialTheme.typography.labelLarge,
                    color = TarnishedGold
                )
            }

            if (userList.previewImageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    userList.previewImageUrls.take(3).forEach { imageUrl ->
                        BookCover(
                            imageUrl = imageUrl,
                            title = userList.name,
                            modifier = Modifier
                                .width(34.dp)
                                .height(52.dp)
                        )
                    }

                    if (userList.bookCount > userList.previewImageUrls.size) {
                        Box(
                            modifier = Modifier
                                .width(34.dp)
                                .height(52.dp)
                                .background(
                                    color = BloodWine.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${userList.bookCount - userList.previewImageUrls.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TarnishedGold
                            )
                        }
                    }
                }
            }

            if (userList.previewImageUrls.isEmpty() && userList.bookCount == 0) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.list_preview_empty_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = OldIvory.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private fun <T> MutableList<T>.move(fromIndex: Int, toIndex: Int) {
    if (fromIndex == toIndex) return
    val item = removeAt(fromIndex)
    add(toIndex, item)
}

@Composable
private fun ListEditorDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var description by rememberSaveable(initialDescription) { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = TarnishedGold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(stringResource(R.string.list_name))
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(stringResource(R.string.list_description))
                    },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description) }
            ) {
                Text(
                    text = confirmLabel,
                    color = TarnishedGold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = OldIvory
                )
            }
        },
        containerColor = Obsidian
    )
}
