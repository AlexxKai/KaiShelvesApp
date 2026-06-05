package com.example.kaishelvesapp.ui.screen.lists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.UserBookList
import com.example.kaishelvesapp.data.model.UserBookTagSummary
import com.example.kaishelvesapp.data.model.UserListPreviewDeviceBook
import com.example.kaishelvesapp.data.repository.DeviceLibraryFile
import com.example.kaishelvesapp.data.repository.UserListsRepository
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiNavigationDrawerContent
import com.example.kaishelvesapp.ui.components.KaiPrimaryTopBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.screen.library.FilePagePreview
import com.example.kaishelvesapp.ui.screen.library.readDeviceBookUserMetadata
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.util.localizedName
import com.example.kaishelvesapp.ui.viewmodel.USER_TAG_DETAIL_PREFIX
import com.example.kaishelvesapp.ui.viewmodel.UserListsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ListsCreateType {
    TAG,
    SHELF
}

@Composable
fun UserListsScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: UserListsViewModel,
    userName: String? = null,
    profileImageUrl: String? = null,
    onOpenList: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScanResult: (String) -> Unit,
    onGoToProfile: () -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit,
    pendingRequestCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onSectionSelected: (KaiSection) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerExpanded = drawerState.targetValue == DrawerValue.Open || drawerState.currentValue == DrawerValue.Open
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var editingList by remember { mutableStateOf<UserBookList?>(null) }
    var deletingList by remember { mutableStateOf<UserBookList?>(null) }
    var deletingTag by remember { mutableStateOf<UserBookTagSummary?>(null) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showAllCollections by rememberSaveable { mutableStateOf(false) }
    val displayedCustomLists = remember { mutableStateListOf<UserBookList>() }
    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var draggingTranslationY by remember { mutableFloatStateOf(0f) }
    var autoScrollDelta by remember { mutableFloatStateOf(0f) }
    val protectedSystemListIds = setOf(
        UserListsRepository.SYSTEM_LIST_WANT_TO_READ_ID,
        UserListsRepository.SYSTEM_LIST_READING_ID,
        UserListsRepository.SYSTEM_LIST_READ_ID,
        UserListsRepository.SYSTEM_LIST_PENDING_ID,
        UserListsRepository.SYSTEM_LIST_UNFINISHED_ID,
        UserListsRepository.SYSTEM_LIST_OWNED_ID
    )
    val protectedSystemListKeys = setOf(
        UserListsRepository.SYSTEM_LIST_WANT_TO_READ_KEY,
        UserListsRepository.SYSTEM_LIST_READING_KEY,
        UserListsRepository.SYSTEM_LIST_READ_KEY,
        UserListsRepository.SYSTEM_LIST_PENDING_KEY,
        UserListsRepository.SYSTEM_LIST_UNFINISHED_KEY,
        UserListsRepository.SYSTEM_LIST_OWNED_KEY
    )
    fun isProtectedSystemList(list: UserBookList) =
        list.id in protectedSystemListIds || list.systemKey in protectedSystemListKeys
    val systemListOrder = listOf(
        UserListsRepository.SYSTEM_LIST_WANT_TO_READ_ID,
        UserListsRepository.SYSTEM_LIST_READING_ID,
        UserListsRepository.SYSTEM_LIST_READ_ID,
        UserListsRepository.SYSTEM_LIST_PENDING_ID,
        UserListsRepository.SYSTEM_LIST_UNFINISHED_ID,
        UserListsRepository.SYSTEM_LIST_OWNED_ID
    )
    val systemLists = uiState.lists
        .filter(::isProtectedSystemList)
        .sortedBy { list -> systemListOrder.indexOf(list.id).takeIf { it >= 0 } ?: Int.MAX_VALUE }
    fun currentCustomOrder(lists: List<UserBookList>) = lists.map { it.id }

    LaunchedEffect(Unit) {
        viewModel.loadLists()
    }

    LaunchedEffect(uiState.lists) {
        if (draggingItemId == null) {
            displayedCustomLists.clear()
            displayedCustomLists.addAll(uiState.lists.filterNot(::isProtectedSystemList))
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
        CollectionCreateDialog(
            onDismiss = { showCreateDialog = false },
            onCreateShelf = { name ->
                viewModel.createList(name, "")
                if (name.trim().isNotBlank()) {
                    showCreateDialog = false
                }
            },
            onCreateTag = { name ->
                viewModel.createTag(name)
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

    deletingTag?.let { tagSummary ->
        AlertDialog(
            onDismissRequest = { deletingTag = null },
            title = {
                Text(
                    text = stringResource(R.string.delete_tag),
                    color = TarnishedGold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_tag_confirmation, tagSummary.tag.name),
                    color = OldIvory
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTag(tagSummary.tag.id)
                        deletingTag = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = TarnishedGold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTag = null }) {
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
                currentSection = KaiSection.MY_BOOKS,
                subtitle = stringResource(R.string.lists_subtitle),
                userName = userName.orEmpty(),
                profileImageUrl = profileImageUrl.orEmpty(),
                expanded = drawerExpanded,
                onGoToProfile = {
                    scope.launch { drawerState.close() }
                    onGoToProfile()
                },
                onGoToSettingsPrivacy = {
                    scope.launch { drawerState.close() }
                    onGoToSettingsPrivacy()
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    onLogout()
                },
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
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onSearch = onSearch,
                    onScanResult = onScanResult,
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    notificationCount = pendingRequestCount,
                    onOpenNotifications = onOpenNotifications
                )
            },
            bottomBar = {
                KaiBottomBar(
                    current = KaiSection.MY_BOOKS,
                    onSelect = onSectionSelected
                )
            },
            floatingActionButton = {
                if (
                    showAllCollections &&
                    (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 240)
                ) {
                    FloatingActionButton(
                        onClick = { scope.launch { listState.animateScrollToItem(0) } },
                        containerColor = BloodWine,
                        contentColor = TarnishedGold
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = stringResource(R.string.scroll_to_top)
                        )
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                        EmptyListsCard()
                    }
                } else if (showAllCollections) {
                    item {
                        AllCollectionsHeader(
                            onBack = { showAllCollections = false }
                        )
                    }

                    item {
                        CollectionsSectionHeader(title = stringResource(R.string.system_lists_title))
                    }

                    items(
                        items = systemLists,
                        key = { list -> list.id }
                    ) { list ->
                        UserListCard(
                            userList = list,
                            onOpen = { onOpenList(list.id) },
                            onEdit = {},
                            isDragging = false,
                            isEditable = false
                        )
                    }

                    item {
                        CollectionsSectionHeader(title = stringResource(R.string.custom_lists_title))
                    }

                    itemsIndexed(
                        items = displayedCustomLists,
                        key = { _, list -> list.id }
                    ) { index, list ->
                        CustomUserListCard(
                            userList = list,
                            isDragging = draggingItemId == list.id,
                            draggingOffset = if (draggingItemId == list.id) draggingTranslationY else 0f,
                            onOpen = { onOpenList(list.id) },
                            onEdit = { editingList = list },
                            onDelete = { deletingList = list },
                            dragHandleModifier = Modifier.pointerInput(list.id, displayedCustomLists.size) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingItemId = list.id
                                        draggingTranslationY = 0f
                                        autoScrollDelta = 0f
                                    },
                                    onDragCancel = {
                                        draggingItemId = null
                                        draggingTranslationY = 0f
                                        autoScrollDelta = 0f
                                        displayedCustomLists.clear()
                                        displayedCustomLists.addAll(uiState.lists.filterNot(::isProtectedSystemList))
                                    },
                                    onDragEnd = {
                                        draggingItemId = null
                                        draggingTranslationY = 0f
                                        autoScrollDelta = 0f
                                        viewModel.updateListOrder(currentCustomOrder(displayedCustomLists))
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        draggingTranslationY += dragAmount.y
                                        val currentIndex = displayedCustomLists.indexOfFirst { it.id == list.id }
                                        if (currentIndex == -1) return@detectDragGesturesAfterLongPress

                                        val lazyIndex = 1 + systemLists.size + currentIndex
                                        val currentItem = listState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { it.index == lazyIndex }
                                            ?: return@detectDragGesturesAfterLongPress
                                        val currentMidPoint = currentItem.offset + currentItem.size / 2 + draggingTranslationY
                                        val viewportStart = listState.layoutInfo.viewportStartOffset
                                        val viewportEnd = listState.layoutInfo.viewportEndOffset
                                        val currentTop = currentItem.offset + draggingTranslationY
                                        val currentBottom = currentItem.offset + currentItem.size + draggingTranslationY

                                        autoScrollDelta = when {
                                            currentBottom > viewportEnd - 96 -> 18f
                                            currentTop < viewportStart + 96 -> -18f
                                            else -> 0f
                                        }

                                        val firstCustomLazyIndex = 1 + systemLists.size
                                        val targetItem = listState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { itemInfo ->
                                                itemInfo.index >= firstCustomLazyIndex &&
                                                    itemInfo.index < firstCustomLazyIndex + displayedCustomLists.size &&
                                                    itemInfo.index != lazyIndex &&
                                                    currentMidPoint >= itemInfo.offset &&
                                                    currentMidPoint <= itemInfo.offset + itemInfo.size
                                            }

                                        if (targetItem != null) {
                                            val targetIndex = (targetItem.index - firstCustomLazyIndex)
                                                .coerceIn(0, displayedCustomLists.lastIndex)
                                            val fromIndex = displayedCustomLists.indexOfFirst { it.id == list.id }
                                            if (fromIndex != -1 && fromIndex != targetIndex) {
                                                displayedCustomLists.move(fromIndex, targetIndex)
                                                draggingTranslationY = 0f
                                            }
                                        }
                                    }
                                )
                            }
                        )
                    }

                    item {
                        CollectionsSectionHeader(title = stringResource(R.string.tags_title))
                    }

                    items(
                        items = uiState.tags,
                        key = { tag -> tag.tag.id }
                    ) { tag ->
                        TagListCard(
                            tagSummary = tag,
                            onOpen = { onOpenList(USER_TAG_DETAIL_PREFIX + tag.tag.id) },
                            onDelete = { deletingTag = tag }
                        )
                    }

                    item {
                        CreateCollectionButton(onClick = { showCreateDialog = true })
                    }
                } else {
                    items(
                        items = systemLists,
                        key = { list -> list.id }
                    ) { list ->
                        UserListCard(
                            userList = list,
                            onOpen = { onOpenList(list.id) },
                            onEdit = {},
                            isDragging = false,
                            isEditable = false
                        )
                    }

                    item {
                        TagsPreviewSection(
                            tags = uiState.tags,
                            onOpenTag = { tag -> onOpenList(USER_TAG_DETAIL_PREFIX + tag.tag.id) },
                            onViewAll = { showAllCollections = true }
                        )
                    }

                    item {
                        CreateCollectionButton(onClick = { showCreateDialog = true })
                    }
                }

            }
        }
    }
}

@Composable
private fun EmptyListsCard(
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
        }
    }
}

@Composable
private fun AllCollectionsHeader(
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) {
            Text(
                text = stringResource(R.string.lists_intro_title),
                color = Color(0xFF66D6D6)
            )
        }
        Text(
            text = stringResource(R.string.all_lists_title),
            style = MaterialTheme.typography.headlineSmall,
            color = OldIvory,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ViewAllButton(
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.view_all).uppercase(),
            color = Color(0xFF66D6D6),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CollectionsSectionHeader(
    title: String
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.titleSmall,
        color = OldIvory.copy(alpha = 0.82f),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun TagsPreviewSection(
    tags: List<UserBookTagSummary>,
    onOpenTag: (UserBookTagSummary) -> Unit,
    onViewAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.tags_title).uppercase(),
            style = MaterialTheme.typography.titleLarge,
            color = OldIvory,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .width(172.dp)
                .height(1.dp)
                .background(TarnishedGold.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
        )

        Spacer(modifier = Modifier.height(22.dp))

        if (tags.isEmpty()) {
            Text(
                text = stringResource(R.string.no_tags_available),
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory.copy(alpha = 0.78f)
            )
        } else {
            tags.take(6).chunked(3).forEach { rowTags ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowTags.forEach { tag ->
                        TagChip(
                            tagSummary = tag,
                            onClick = { onOpenTag(tag) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        ViewAllButton(onClick = onViewAll)
    }
}

@Composable
private fun TagChip(
    tagSummary: UserBookTagSummary,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = Color(0xFF244845).copy(alpha = 0.72f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color(0xFF66D6D6).copy(alpha = 0.18f))
    ) {
        Text(
            text = "${tagSummary.tag.name} (${tagSummary.bookCount})",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF9DE6DD),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CreateCollectionButton(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(54.dp)
            .clickable(onClick = onClick),
        color = BloodWine.copy(alpha = 0.34f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.42f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = TarnishedGold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.create_new_tag_or_shelf).removePrefix("+").trim(),
                color = OldIvory,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TagListCard(
    tagSummary: UserBookTagSummary,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BloodWine.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete_tag),
                    tint = TarnishedGold
                )
            }
        },
        content = {
            UserListCard(
                userList = UserBookList(
                    id = USER_TAG_DETAIL_PREFIX + tagSummary.tag.id,
                    name = tagSummary.tag.name,
                    bookCount = tagSummary.bookCount,
                    previewImageUrls = tagSummary.previewImageUrls
                ),
                onOpen = onOpen,
                onEdit = {},
                isDragging = false,
                isEditable = false
            )
        }
    )
}

@Composable
private fun CustomUserListCard(
    userList: UserBookList,
    isDragging: Boolean,
    draggingOffset: Float,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dragHandleModifier: Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BloodWine.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete_list),
                    tint = TarnishedGold
                )
            }
        },
        content = {
            UserListCard(
                userList = userList,
                onOpen = onOpen,
                onEdit = onEdit,
                isDragging = isDragging,
                isEditable = true,
                dragHandleModifier = dragHandleModifier,
                modifier = Modifier.graphicsLayer {
                    translationY = draggingOffset
                    scaleX = if (isDragging) 1.01f else 1f
                    scaleY = if (isDragging) 1.01f else 1f
                }
            )
        }
    )
}

@Composable
private fun UserListCard(
    userList: UserBookList,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    isDragging: Boolean,
    isEditable: Boolean,
    dragHandleModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isDragging) { onOpen() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.45f))
    ) {
        Row(
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
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ListPreviewStack(
                userList = userList,
                modifier = Modifier
                    .width(112.dp)
                    .height(92.dp)
            )

            Spacer(modifier = Modifier.width(18.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = userList.localizedName(),
                            style = MaterialTheme.typography.titleMedium,
                            color = TarnishedGold,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = stringResource(R.string.list_books_count, userList.bookCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OldIvory,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isEditable) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = dragHandleModifier.padding(start = 2.dp, top = 4.dp, bottom = 4.dp),
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
                        }
                    }
                }

                if (userList.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = userList.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = OldIvory.copy(alpha = 0.86f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ListPreviewStack(
    userList: UserBookList,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomStart
    ) {
        val previewDeviceBooks = userList.previewDeviceBooks.take(3)
        val previewImageUrls = userList.previewImageUrls.take(3)
        val previewCount = previewDeviceBooks.size.takeIf { it > 0 } ?: previewImageUrls.size

        if (previewCount == 0) {
            Box(
                modifier = Modifier
                    .offset(x = 34.dp)
                    .width(44.dp)
                    .height(70.dp)
                    .background(
                        color = OldIvory.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Box(
                modifier = Modifier
                    .offset(x = 52.dp)
                    .width(38.dp)
                    .height(58.dp)
                    .background(
                        color = OldIvory.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(86.dp)
                    .background(
                        color = DeepWalnut,
                        shape = RoundedCornerShape(2.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = OldIvory,
                    modifier = Modifier.size(34.dp)
                )
            }
        } else {
            val previewSlots = previewDeviceBooks.ifEmpty {
                previewImageUrls.map { imageUrl ->
                    UserListPreviewDeviceBook(uri = imageUrl)
                }
            }

            previewSlots.forEachIndexed { index, preview ->
                    val coverWidth = when (index) {
                        0 -> 70.dp
                        1 -> 52.dp
                        else -> 42.dp
                    }
                    val coverHeight = when (index) {
                        0 -> 86.dp
                        1 -> 76.dp
                        else -> 64.dp
                    }
                    val xOffset = when (index) {
                        0 -> 0.dp
                        1 -> 34.dp
                        else -> 62.dp
                    }

                    val coverModifier = Modifier
                        .offset(x = xOffset)
                        .zIndex((3 - index).toFloat())
                        .width(coverWidth)
                        .height(coverHeight)

                    if (previewDeviceBooks.isNotEmpty()) {
                        OwnedListPreviewCover(
                            preview = preview,
                            modifier = coverModifier
                        )
                    } else {
                        BookCover(
                            imageUrl = preview.uri,
                            title = userList.localizedName(),
                            showFrame = false,
                            modifier = coverModifier
                        )
                    }
                }

            if (userList.bookCount > previewCount) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .zIndex(4f)
                        .background(
                            color = BloodWine.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+${userList.bookCount - previewCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TarnishedGold
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnedListPreviewCover(
    preview: UserListPreviewDeviceBook,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = remember(preview.uri) { previewDeviceLibraryFile(preview) }
    val metadata = remember(preview.uri) { readDeviceBookUserMetadata(context, file) }

    FilePagePreview(
        file = file,
        coverText = metadata.coverText,
        overrideCoverId = metadata.coverId.takeIf { it.isNotBlank() },
        modifier = modifier
    )
}

private fun previewDeviceLibraryFile(preview: UserListPreviewDeviceBook): DeviceLibraryFile {
    return DeviceLibraryFile(
        name = preview.name,
        location = preview.location,
        mimeType = preview.mimeType,
        sizeBytes = preview.sizeBytes,
        modifiedAtMillis = preview.modifiedAtMillis,
        uri = preview.uri.toUri()
    )
}

private fun <T> MutableList<T>.move(fromIndex: Int, toIndex: Int) {
    if (fromIndex == toIndex) return
    val item = removeAt(fromIndex)
    add(toIndex, item)
}

@Composable
private fun CollectionCreateDialog(
    onDismiss: () -> Unit,
    onCreateShelf: (String) -> Unit,
    onCreateTag: (String) -> Unit
) {
    var selectedType by rememberSaveable { mutableStateOf(ListsCreateType.SHELF) }
    var name by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.create_new_tag_or_shelf).removePrefix("+").trim(),
                color = TarnishedGold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedType = ListsCreateType.SHELF },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == ListsCreateType.SHELF,
                            onClick = { selectedType = ListsCreateType.SHELF }
                        )
                        Text(text = stringResource(R.string.shelf), color = OldIvory)
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedType = ListsCreateType.TAG },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == ListsCreateType.TAG,
                            onClick = { selectedType = ListsCreateType.TAG }
                        )
                        Text(text = stringResource(R.string.tag), color = OldIvory)
                    }
                }

                Text(
                    text = if (selectedType == ListsCreateType.SHELF) {
                        stringResource(R.string.single_shelf_note)
                    } else {
                        stringResource(R.string.tag_create_note)
                    },
                    color = OldIvory.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            if (selectedType == ListsCreateType.SHELF) {
                                stringResource(R.string.shelf_name_hint)
                            } else {
                                stringResource(R.string.tag_name_hint)
                            }
                        )
                    },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedType == ListsCreateType.SHELF) {
                        onCreateShelf(name)
                    } else {
                        onCreateTag(name)
                    }
                }
            ) {
                Text(
                    text = stringResource(R.string.create),
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
