package com.example.kaishelvesapp.ui.screen.lists

import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.kaishelvesapp.data.model.DeviceBookFormat
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.repository.DeviceLibraryFile
import com.example.kaishelvesapp.data.repository.UserListsRepository
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.components.BookShelfActions
import com.example.kaishelvesapp.ui.components.RatingStars
import com.example.kaishelvesapp.ui.screen.library.FilePagePreview
import com.example.kaishelvesapp.ui.screen.library.readDeviceBookProgressPercent
import com.example.kaishelvesapp.ui.screen.library.readDeviceBookUserMetadata
import com.example.kaishelvesapp.ui.screen.library.readingStatusForProgress
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.util.formatReadDateForDisplay
import com.example.kaishelvesapp.ui.viewmodel.UserListDetailBookItem
import com.example.kaishelvesapp.ui.viewmodel.UserListDetailViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ListDetailSortOption {
    TITLE,
    AUTHOR,
    FORMAT,
    RATING,
    READ_DATE
}

@Composable
fun UserListDetailScreen(
    listId: String,
    viewModel: UserListDetailViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit,
    onBookClick: (Libro) -> Unit,
    onReadOwnedBook: (UserListDetailBookItem) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var sortOption by remember(listId) { mutableStateOf(ListDetailSortOption.TITLE) }
    val isReadList = listId == UserListsRepository.SYSTEM_LIST_READ_ID
    val isPendingList = listId == UserListsRepository.SYSTEM_LIST_PENDING_ID
    val isOwnedList = listId == UserListsRepository.SYSTEM_LIST_OWNED_ID
    val displayedPendingBooks = remember { mutableStateListOf<UserListDetailBookItem>() }
    var isPendingEditMode by rememberSaveable(listId) { mutableStateOf(false) }
    var showCoverView by rememberSaveable(listId) { mutableStateOf(false) }
    var draggingBookId by remember { mutableStateOf<String?>(null) }
    var draggingSourceIndex by remember { mutableStateOf<Int?>(null) }
    var draggingTargetIndex by remember { mutableStateOf<Int?>(null) }
    var draggingTranslationX by remember { mutableFloatStateOf(0f) }
    var draggingTranslationY by remember { mutableFloatStateOf(0f) }
    var autoScrollDelta by remember { mutableFloatStateOf(0f) }
    var selectedOwnedItem by remember { mutableStateOf<UserListDetailBookItem?>(null) }
    val sortedBooks = remember(uiState.books, sortOption, isReadList, isPendingList, isOwnedList) {
        if (isPendingList) {
            uiState.books
        } else {
            when (sortOption) {
                ListDetailSortOption.TITLE -> uiState.books.sortedBy { it.book.titulo.lowercase() }
                ListDetailSortOption.AUTHOR -> uiState.books.sortedBy { it.book.autor.lowercase() }
                ListDetailSortOption.FORMAT -> uiState.books.sortedWith(
                    compareBy<UserListDetailBookItem> { formatSortKey(it.ownedFormats) }
                        .thenBy { it.book.titulo.lowercase() }
                )
                ListDetailSortOption.RATING -> uiState.books.sortedWith(
                    compareByDescending<UserListDetailBookItem> { it.rating ?: -1 }
                        .thenBy { it.book.titulo.lowercase() }
                )
                ListDetailSortOption.READ_DATE -> uiState.books.sortedWith(
                    compareByDescending<UserListDetailBookItem> { it.readDate.orEmpty() }
                        .thenBy { it.book.titulo.lowercase() }
                )
            }
        }
    }
    val visibleBooks = if (isPendingList) displayedPendingBooks else sortedBooks

    LaunchedEffect(listId) {
        viewModel.loadListDetail(listId)
        sortOption = when {
            isOwnedList -> ListDetailSortOption.FORMAT
            isReadList -> ListDetailSortOption.READ_DATE
            else -> ListDetailSortOption.TITLE
        }
    }

    LaunchedEffect(uiState.books, isPendingList) {
        if (isPendingList && draggingBookId == null) {
            displayedPendingBooks.clear()
            displayedPendingBooks.addAll(uiState.books)
        }
    }

    LaunchedEffect(isPendingList, isPendingEditMode) {
        if (!isPendingList || !isPendingEditMode) {
            draggingBookId = null
            draggingSourceIndex = null
            draggingTargetIndex = null
            draggingTranslationX = 0f
            draggingTranslationY = 0f
            autoScrollDelta = 0f
        }
    }

    LaunchedEffect(draggingBookId, autoScrollDelta) {
        while (draggingBookId != null && autoScrollDelta != 0f) {
            listState.scrollBy(autoScrollDelta)
            delay(16)
        }
    }

    LaunchedEffect(uiState.errorMessageRes, uiState.successMessageRes) {
        uiState.errorMessageRes?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            viewModel.clearMessages()
        }

        uiState.successMessageRes?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            viewModel.clearMessages()
        }
    }

    selectedOwnedItem?.let { item ->
        OwnedBookDetailDialog(
            item = item,
            onDismiss = { selectedOwnedItem = null },
            onRead = {
                selectedOwnedItem = null
                onReadOwnedBook(item)
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 240) {
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
                .statusBarsPadding()
                .padding(paddingValues)
                .padding(innerPadding)
                .padding(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ListDetailHeaderCard(
                    userList = uiState.userList,
                    books = sortedBooks,
                    sortOption = sortOption,
                    isReadList = isReadList,
                    isPendingList = isPendingList,
                    isOwnedList = isOwnedList,
                    isPendingEditMode = isPendingEditMode,
                    showCoverView = showCoverView,
                    onBack = onBack,
                    onToggleViewMode = {
                        if (!isPendingEditMode) {
                            showCoverView = !showCoverView
                        }
                    },
                    onTogglePendingEditMode = {
                        if (isPendingEditMode) {
                            val orderedIds = displayedPendingBooks.map { pendingItem ->
                                pendingItem.book.id.ifBlank { pendingItem.book.isbn }
                            }
                            viewModel.updateBooksOrder(listId, orderedIds)
                        }
                        isPendingEditMode = !isPendingEditMode
                    },
                    onSortChange = {
                        sortOption = when {
                            isOwnedList && sortOption == ListDetailSortOption.FORMAT -> ListDetailSortOption.TITLE
                            isOwnedList && sortOption == ListDetailSortOption.TITLE -> ListDetailSortOption.AUTHOR
                            isOwnedList -> ListDetailSortOption.FORMAT
                            !isReadList && sortOption == ListDetailSortOption.TITLE -> ListDetailSortOption.AUTHOR
                            !isReadList -> ListDetailSortOption.TITLE
                            sortOption == ListDetailSortOption.READ_DATE -> ListDetailSortOption.RATING
                            sortOption == ListDetailSortOption.RATING -> ListDetailSortOption.TITLE
                            else -> ListDetailSortOption.READ_DATE
                        }
                    }
                )
            }

            when {
                uiState.isLoading -> {
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
                }

                uiState.books.isEmpty() -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Obsidian),
                            border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.75f))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.empty_list_books_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TarnishedGold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = stringResource(R.string.empty_list_books_body),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OldIvory
                                )
                            }
                        }
                    }
                }

                else -> {
                    if (showCoverView && !isPendingEditMode) {
                        itemsIndexed(
                            visibleBooks.chunked(3),
                            key = { index, _ -> "cover_row_$index" }
                        ) { rowIndex, rowItems ->
                            val visibleRows = listState.layoutInfo.visibleItemsInfo
                                .filter { it.index > 0 }
                            val cellWidth = listState.layoutInfo.viewportSize.width / 3f
                            CoverRow(
                                items = rowItems,
                                rowStartIndex = rowIndex * 3,
                                draggingBookId = draggingBookId,
                                draggingSourceIndex = draggingSourceIndex,
                                draggingOffsetX = draggingTranslationX,
                                draggingOffsetY = draggingTranslationY,
                                visualOffsetFor = { itemIndex ->
                                    val sourceIndex = draggingSourceIndex
                                    val targetIndex = draggingTargetIndex
                                    if (
                                        sourceIndex == null ||
                                        targetIndex == null ||
                                        sourceIndex == targetIndex ||
                                        cellWidth <= 0f
                                    ) {
                                        0f to 0f
                                    } else {
                                        val visualIndex = when {
                                            sourceIndex < targetIndex && itemIndex in (sourceIndex + 1)..targetIndex ->
                                                itemIndex - 1
                                            targetIndex < sourceIndex && itemIndex in targetIndex until sourceIndex ->
                                                itemIndex + 1
                                            else -> itemIndex
                                        }
                                        if (visualIndex == itemIndex) {
                                            0f to 0f
                                        } else {
                                            val currentRowIndex = itemIndex / 3 + 1
                                            val visualRowIndex = visualIndex / 3 + 1
                                            val currentRowOffset = visibleRows
                                                .firstOrNull { it.index == currentRowIndex }
                                                ?.offset
                                            val visualRowOffset = visibleRows
                                                .firstOrNull { it.index == visualRowIndex }
                                                ?.offset
                                            if (currentRowOffset == null || visualRowOffset == null) {
                                                0f to 0f
                                            } else {
                                                val offsetX = ((visualIndex % 3) - (itemIndex % 3)) * cellWidth
                                                val offsetY = (visualRowOffset - currentRowOffset).toFloat()
                                                offsetX to offsetY
                                            }
                                        }
                                    }
                                },
                                canOpenBooks = true,
                                onBookClick = { book ->
                                    if (isOwnedList) {
                                        rowItems.firstOrNull { it.book.id == book.id }?.let { selectedOwnedItem = it }
                                    } else {
                                        onBookClick(book)
                                    }
                                },
                                dragModifierFor = { bookId ->
                                    if (isPendingList) {
                                        Modifier.pointerInput(bookId, visibleBooks.size) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    val sourceIndex = displayedPendingBooks.indexOfFirst { pendingItem ->
                                                        pendingItem.book.id.ifBlank { pendingItem.book.isbn } == bookId
                                                    }.takeIf { it >= 0 }
                                                    draggingBookId = bookId
                                                    draggingSourceIndex = sourceIndex
                                                    draggingTargetIndex = sourceIndex
                                                    draggingTranslationX = 0f
                                                    draggingTranslationY = 0f
                                                },
                                                onDragCancel = {
                                                    draggingBookId = null
                                                    draggingSourceIndex = null
                                                    draggingTargetIndex = null
                                                    draggingTranslationX = 0f
                                                    draggingTranslationY = 0f
                                                    autoScrollDelta = 0f
                                                    displayedPendingBooks.clear()
                                                    displayedPendingBooks.addAll(uiState.books)
                                                },
                                                onDragEnd = {
                                                    val sourceIndex = draggingSourceIndex
                                                    val targetIndex = draggingTargetIndex
                                                    if (
                                                        sourceIndex != null &&
                                                        targetIndex != null &&
                                                        sourceIndex != targetIndex &&
                                                        sourceIndex in displayedPendingBooks.indices
                                                    ) {
                                                        displayedPendingBooks.move(
                                                            sourceIndex,
                                                            targetIndex.coerceIn(0, displayedPendingBooks.lastIndex)
                                                        )
                                                    }
                                                    val orderedIds = displayedPendingBooks.map { pendingItem ->
                                                        pendingItem.book.id.ifBlank { pendingItem.book.isbn }
                                                    }
                                                    draggingBookId = null
                                                    draggingSourceIndex = null
                                                    draggingTargetIndex = null
                                                    draggingTranslationX = 0f
                                                    draggingTranslationY = 0f
                                                    autoScrollDelta = 0f
                                                    viewModel.updateBooksOrder(listId, orderedIds)
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    draggingTranslationX += dragAmount.x
                                                    draggingTranslationY += dragAmount.y
                                                    val sourceIndex = draggingSourceIndex
                                                        ?: return@detectDragGesturesAfterLongPress

                                                    val visibleRows = listState.layoutInfo.visibleItemsInfo
                                                        .filter { it.index > 0 }
                                                    val sourceLayoutIndex = sourceIndex / 3 + 1
                                                    val sourceRowItem = visibleRows.firstOrNull { it.index == sourceLayoutIndex }
                                                    if (sourceRowItem != null) {
                                                        val sourceColumn = sourceIndex % 3
                                                        val cellWidth = listState.layoutInfo.viewportSize.width / 3f
                                                        val draggedCenterX = (cellWidth * (sourceColumn + 0.5f)) + draggingTranslationX
                                                        val draggedCenterY = sourceRowItem.offset +
                                                            (sourceRowItem.size / 2f) +
                                                            draggingTranslationY

                                                        autoScrollDelta = when {
                                                            sourceRowItem.offset + sourceRowItem.size + draggingTranslationY >
                                                                listState.layoutInfo.viewportEndOffset - 96 -> 18f
                                                            sourceRowItem.offset + draggingTranslationY <
                                                                listState.layoutInfo.viewportStartOffset + 96 -> -18f
                                                            else -> 0f
                                                        }

                                                        val targetRowItem = visibleRows.firstOrNull { rowInfo ->
                                                            draggedCenterY >= rowInfo.offset &&
                                                                draggedCenterY <= rowInfo.offset + rowInfo.size
                                                        }

                                                        if (targetRowItem != null && cellWidth > 0f) {
                                                            val targetRow = targetRowItem.index - 1
                                                            val targetColumn = (draggedCenterX / cellWidth)
                                                                .toInt()
                                                                .coerceIn(0, 2)
                                                            val targetIndex = (targetRow * 3 + targetColumn)
                                                                .coerceIn(0, displayedPendingBooks.lastIndex)

                                                            draggingTargetIndex = targetIndex
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    } else {
                                        Modifier
                                    }
                                }
                            )
                        }
                    } else {
                        itemsIndexed(visibleBooks, key = { _, item -> item.book.id.ifBlank { item.book.isbn } }) { _, item ->
                        val bookId = item.book.id.ifBlank { item.book.isbn }
                        val isDragging = draggingBookId == bookId
                        ListBookCard(
                            item = item,
                            isReadList = isReadList,
                            isPendingList = isPendingList,
                            isOwnedList = isOwnedList,
                            isPendingEditMode = isPendingEditMode,
                            isDragging = isDragging,
                            draggingOffset = if (isDragging) draggingTranslationY else 0f,
                            onOpen = { onBookClick(item.book) },
                            onReadOwnedBook = { onReadOwnedBook(item) },
                            onOrganizationChanged = { viewModel.loadListDetail(listId) },
                            dragHandleModifier = if (isPendingList && isPendingEditMode) {
                                Modifier.pointerInput(bookId, visibleBooks.size) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingBookId = bookId
                                            draggingSourceIndex = null
                                            draggingTargetIndex = null
                                            draggingTranslationX = 0f
                                            draggingTranslationY = 0f
                                        },
                                        onDragCancel = {
                                            draggingBookId = null
                                            draggingSourceIndex = null
                                            draggingTargetIndex = null
                                            draggingTranslationX = 0f
                                            draggingTranslationY = 0f
                                            autoScrollDelta = 0f
                                            displayedPendingBooks.clear()
                                            displayedPendingBooks.addAll(uiState.books)
                                        },
                                        onDragEnd = {
                                            val orderedIds = displayedPendingBooks.map { pendingItem ->
                                                pendingItem.book.id.ifBlank { pendingItem.book.isbn }
                                            }
                                            draggingBookId = null
                                            draggingSourceIndex = null
                                            draggingTargetIndex = null
                                            draggingTranslationX = 0f
                                            draggingTranslationY = 0f
                                            autoScrollDelta = 0f
                                            viewModel.updateBooksOrder(listId, orderedIds)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            draggingTranslationY += dragAmount.y
                                            val currentIndex = displayedPendingBooks.indexOfFirst { pendingItem ->
                                                pendingItem.book.id.ifBlank { pendingItem.book.isbn } == bookId
                                            }
                                            if (currentIndex == -1) return@detectDragGesturesAfterLongPress

                                            val visibleItems = listState.layoutInfo.visibleItemsInfo
                                            val currentLayoutIndex = currentIndex + 1
                                            val currentItem = visibleItems.firstOrNull { it.index == currentLayoutIndex }
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

                                            val targetItem = visibleItems
                                                .filter { it.index > 0 }
                                                .firstOrNull { itemInfo ->
                                                    itemInfo.index != currentLayoutIndex &&
                                                        currentMidPoint >= itemInfo.offset &&
                                                        currentMidPoint <= itemInfo.offset + itemInfo.size
                                                }

                                            if (targetItem != null) {
                                                val targetIndex = (targetItem.index - 1)
                                                    .coerceIn(0, displayedPendingBooks.lastIndex)
                                                val fromIndex = displayedPendingBooks.indexOfFirst { pendingItem ->
                                                    pendingItem.book.id.ifBlank { pendingItem.book.isbn } == bookId
                                                }
                                                if (fromIndex != -1 && fromIndex != targetIndex) {
                                                    displayedPendingBooks.move(fromIndex, targetIndex)
                                                    draggingTranslationY = 0f
                                                }
                                            }
                                        }
                                    )
                                }
                            } else {
                                Modifier
                            }
                        )
                        }
                    }
                }
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
private fun ListDetailHeaderCard(
    userList: com.example.kaishelvesapp.data.model.UserBookList?,
    books: List<UserListDetailBookItem>,
    sortOption: ListDetailSortOption,
    isReadList: Boolean,
    isPendingList: Boolean,
    isOwnedList: Boolean,
    isPendingEditMode: Boolean,
    showCoverView: Boolean,
    onBack: () -> Unit,
    onToggleViewMode: () -> Unit,
    onTogglePendingEditMode: () -> Unit,
    onSortChange: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DeepWalnut.copy(alpha = 0.96f),
                            Obsidian
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = TarnishedGold,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = userList?.name ?: stringResource(R.string.list_detail_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TarnishedGold,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    modifier = Modifier
                        .background(
                            color = BloodWine.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = TarnishedGold,
                        modifier = Modifier.size(17.dp)
                    )

                    Text(
                        text = (userList?.bookCount ?: books.size).toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OldIvory,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleViewMode,
                    enabled = !isPendingEditMode,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = if (showCoverView) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                        contentDescription = null,
                        tint = TarnishedGold,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (showCoverView) stringResource(R.string.list_view) else stringResource(R.string.cover_view),
                        color = TarnishedGold,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (isOwnedList) {
                    OutlinedButton(
                        onClick = onSortChange,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(R.string.sort_action),
                            tint = TarnishedGold,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = sortOptionLabel(sortOption, isReadList = false),
                            color = TarnishedGold,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isPendingList && !showCoverView) {
                    OutlinedButton(
                        onClick = onTogglePendingEditMode,
                        enabled = !showCoverView,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector = if (isPendingEditMode) Icons.Filled.Done else Icons.Filled.Edit,
                            contentDescription = null,
                            tint = TarnishedGold,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (isPendingEditMode) stringResource(R.string.save) else stringResource(R.string.edit_review_button),
                            color = TarnishedGold,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (!isPendingList && !isOwnedList) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(
                            color = BloodWine.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSortChange() }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sort_by_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OldIvory
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = sortOptionLabel(sortOption, isReadList),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TarnishedGold,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(R.string.sort_action),
                        tint = TarnishedGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverRow(
    items: List<UserListDetailBookItem>,
    rowStartIndex: Int,
    draggingBookId: String?,
    draggingSourceIndex: Int?,
    draggingOffsetX: Float,
    draggingOffsetY: Float,
    visualOffsetFor: (Int) -> Pair<Float, Float>,
    canOpenBooks: Boolean,
    onBookClick: (Libro) -> Unit,
    dragModifierFor: (String) -> Modifier
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) { columnIndex ->
            val layoutIndex = rowStartIndex + columnIndex
            val item = items.getOrNull(columnIndex)
            val itemBookId = item?.book?.id?.ifBlank { item.book.isbn }
            val isDragging = draggingBookId != null &&
                layoutIndex == draggingSourceIndex &&
                itemBookId == draggingBookId
            val visualOffset = if (isDragging) {
                draggingOffsetX to draggingOffsetY
            } else {
                visualOffsetFor(layoutIndex)
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (item != null) {
                    CoverCell(
                        item = item,
                        isDragging = isDragging,
                        offsetX = visualOffset.first,
                        offsetY = visualOffset.second,
                        dragModifier = dragModifierFor(itemBookId.orEmpty()),
                        onBookClick = onBookClick,
                        enabled = draggingBookId == null && canOpenBooks
                    )
                } else {
                    Spacer(
                        modifier = Modifier
                            .width(92.dp)
                            .height(138.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverCell(
    item: UserListDetailBookItem,
    isDragging: Boolean,
    offsetX: Float,
    offsetY: Float,
    dragModifier: Modifier,
    onBookClick: (Libro) -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier
            .graphicsLayer {
                translationX = offsetX
                translationY = offsetY
                scaleX = if (isDragging) 1.04f else 1f
                scaleY = if (isDragging) 1.04f else 1f
            }
            .zIndex(if (isDragging) 1f else 0f)
            .then(dragModifier)
            .clickable(enabled = enabled) { onBookClick(item.book) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (item.ownedUri.isNotBlank()) {
            OwnedBookCover(
                item = item,
                modifier = Modifier
                    .width(92.dp)
                    .height(138.dp)
            )
        } else {
            BookCover(
                imageUrl = item.book.imagen,
                title = item.book.titulo,
                showFrame = false,
                modifier = Modifier
                    .width(92.dp)
                    .height(138.dp)
            )
        }

        if (item.ownedFormats.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            FormatBadge(text = formatSummary(item.ownedFormats))
        }
    }
}

@Composable
private fun OwnedBookCover(
    item: UserListDetailBookItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = remember(item.ownedUri) { ownedDeviceLibraryFile(item) }
    val metadata = remember(item.ownedUri) { readDeviceBookUserMetadata(context, file) }

    FilePagePreview(
        file = file,
        coverText = metadata.coverText,
        overrideCoverId = metadata.coverId.takeIf { it.isNotBlank() },
        modifier = modifier
    )
}

@Composable
private fun OwnedBookDetailDialog(
    item: UserListDetailBookItem,
    onDismiss: () -> Unit,
    onRead: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(item.ownedUri) { ownedDeviceLibraryFile(item) }
    val progress = remember(item.ownedUri) { readDeviceBookProgressPercent(context, file) }
    val statusText = when (readingStatusForProgress(progress)) {
        com.example.kaishelvesapp.ui.screen.library.DeviceLibraryReadingStatus.Unread -> stringResource(R.string.reading_status_unread)
        com.example.kaishelvesapp.ui.screen.library.DeviceLibraryReadingStatus.Reading -> stringResource(R.string.reading_status_reading)
        com.example.kaishelvesapp.ui.screen.library.DeviceLibraryReadingStatus.Finished -> stringResource(R.string.reading_status_finished)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.owned_book_details),
                color = TarnishedGold
            )
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                OwnedBookCover(
                    item = item,
                    modifier = Modifier
                        .width(78.dp)
                        .height(116.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.book.titulo.ifBlank { item.ownedFileName.substringBeforeLast('.') },
                        style = MaterialTheme.typography.titleMedium,
                        color = TarnishedGold,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.book.autor.ifBlank { stringResource(R.string.owned_book_author_unknown) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = OldIvory
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    FormatBadge(text = formatSummary(item.ownedFormats))

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = OldIvory.copy(alpha = 0.88f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRead) {
                Text(
                    text = stringResource(R.string.read_owned_book),
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

private fun ownedDeviceLibraryFile(item: UserListDetailBookItem): DeviceLibraryFile {
    return DeviceLibraryFile(
        name = item.ownedFileName.ifBlank { item.book.titulo },
        location = item.ownedLocation,
        mimeType = item.ownedMimeType,
        sizeBytes = item.ownedSizeBytes,
        modifiedAtMillis = item.ownedModifiedAtMillis,
        uri = Uri.parse(item.ownedUri)
    )
}

@Composable
private fun OwnedReadingStatusText(
    item: UserListDetailBookItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = remember(item.ownedUri) { ownedDeviceLibraryFile(item) }
    val progress = remember(item.ownedUri) { readDeviceBookProgressPercent(context, file) }
    val statusText = when (readingStatusForProgress(progress)) {
        com.example.kaishelvesapp.ui.screen.library.DeviceLibraryReadingStatus.Unread -> stringResource(R.string.reading_status_unread)
        com.example.kaishelvesapp.ui.screen.library.DeviceLibraryReadingStatus.Reading -> stringResource(R.string.reading_status_reading)
        com.example.kaishelvesapp.ui.screen.library.DeviceLibraryReadingStatus.Finished -> stringResource(R.string.reading_status_finished)
    }

    Text(
        text = statusText,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = OldIvory.copy(alpha = 0.88f)
    )
}

@Composable
private fun FormatBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = BloodWine.copy(alpha = 0.72f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TarnishedGold,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatSummary(formats: List<DeviceBookFormat>): String {
    return formats
        .ifEmpty { listOf(DeviceBookFormat.Unsupported) }
        .joinToString(" + ") { format ->
            when (format) {
                DeviceBookFormat.Pdf -> "PDF"
                DeviceBookFormat.Epub -> "EPUB"
                DeviceBookFormat.Txt -> "TXT"
                DeviceBookFormat.Fb2 -> "FB2"
                DeviceBookFormat.Mobi -> "MOBI"
                DeviceBookFormat.Azw -> "AZW"
                DeviceBookFormat.Azw3 -> "AZW3"
                DeviceBookFormat.Cbz -> "CBZ"
                DeviceBookFormat.Unsupported -> "Archivo"
            }
        }
}

private fun formatSortKey(formats: List<DeviceBookFormat>): String {
    return formats
        .ifEmpty { listOf(DeviceBookFormat.Unsupported) }
        .minBy { formatSortOrder(it) }
        .let { "${formatSortOrder(it).toString().padStart(2, '0')}_${formatSummary(listOf(it))}" }
}

private fun formatSortOrder(format: DeviceBookFormat): Int {
    return when (format) {
        DeviceBookFormat.Epub -> 0
        DeviceBookFormat.Pdf -> 1
        DeviceBookFormat.Txt -> 2
        DeviceBookFormat.Fb2 -> 3
        DeviceBookFormat.Mobi -> 4
        DeviceBookFormat.Azw -> 5
        DeviceBookFormat.Azw3 -> 6
        DeviceBookFormat.Cbz -> 7
        DeviceBookFormat.Unsupported -> 99
    }
}

@Composable
private fun sortOptionLabel(
    sortOption: ListDetailSortOption,
    isReadList: Boolean
): String {
    return when {
        sortOption == ListDetailSortOption.FORMAT -> stringResource(R.string.sort_option_format)
        !isReadList && sortOption == ListDetailSortOption.AUTHOR -> stringResource(R.string.sort_option_author)
        !isReadList -> stringResource(R.string.sort_option_title)
        sortOption == ListDetailSortOption.READ_DATE -> stringResource(R.string.sort_option_read_date)
        sortOption == ListDetailSortOption.RATING -> stringResource(R.string.sort_option_rating)
        else -> stringResource(R.string.sort_option_title)
    }
}

@Composable
private fun ListBookCard(
    item: UserListDetailBookItem,
    isReadList: Boolean,
    isPendingList: Boolean,
    isOwnedList: Boolean,
    isPendingEditMode: Boolean,
    isDragging: Boolean,
    draggingOffset: Float,
    onOpen: () -> Unit,
    onReadOwnedBook: () -> Unit = {},
    onOrganizationChanged: () -> Unit,
    dragHandleModifier: Modifier = Modifier
) {
    val libro = item.book
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = draggingOffset
                scaleX = if (isDragging) 1.01f else 1f
                scaleY = if (isDragging) 1.01f else 1f
            }
            .clickable(enabled = !isPendingEditMode && !isOwnedList) { onOpen() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isOwnedList && item.ownedUri.isNotBlank()) {
                    OwnedBookCover(
                        item = item,
                        modifier = Modifier
                            .width(if (isPendingEditMode) 48.dp else 62.dp)
                            .height(if (isPendingEditMode) 72.dp else 92.dp)
                    )
                } else {
                    BookCover(
                        imageUrl = libro.imagen,
                        title = libro.titulo,
                        showFrame = false,
                        modifier = Modifier
                            .width(if (isPendingEditMode) 48.dp else 62.dp)
                            .height(if (isPendingEditMode) 72.dp else 92.dp)
                    )
                }

                Spacer(modifier = Modifier.width(if (isPendingEditMode) 10.dp else 14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = libro.titulo.ifBlank { stringResource(R.string.unknown_title) },
                        style = if (isPendingEditMode) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        color = TarnishedGold
                    )

                    Spacer(modifier = Modifier.height(if (isPendingEditMode) 4.dp else 6.dp))

                    if (libro.autor.isNotBlank()) {
                        Text(
                            text = libro.autor,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OldIvory
                        )
                    }

                    if (isOwnedList) {
                        Spacer(modifier = Modifier.height(6.dp))
                        // En "Tengo" el formato sustituye a las acciones de estanteria.
                        FormatBadge(text = formatSummary(item.ownedFormats))

                        Spacer(modifier = Modifier.height(8.dp))
                        OwnedReadingStatusText(item = item)
                    }

                    if (!isPendingEditMode && libro.genero.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = libro.genero,
                            style = MaterialTheme.typography.bodySmall,
                            color = OldIvory
                        )
                    }

                    if (isReadList) {
                        Spacer(modifier = Modifier.height(8.dp))

                        if ((item.rating ?: 0) > 0) {
                            RatingStars(
                                rating = item.rating ?: 0,
                                iconSize = 14.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.not_rated_yet),
                                style = MaterialTheme.typography.bodySmall,
                                color = OldIvory
                            )
                        }

                        item.readDate?.takeIf { it.isNotBlank() }?.let { readDate ->
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = stringResource(
                                    R.string.read_date_value,
                                    formatReadDateForDisplay(readDate)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = OldIvory.copy(alpha = 0.88f)
                            )
                        }
                    }
                }

                if (isPendingList && isPendingEditMode) {
                    Box(
                        modifier = dragHandleModifier.padding(start = 10.dp, top = 12.dp, bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DragIndicator,
                            contentDescription = stringResource(R.string.drag_to_reorder),
                            tint = OldIvory.copy(alpha = 0.75f)
                        )
                    }
                } else if (isOwnedList) {
                    OutlinedButton(
                        onClick = onReadOwnedBook,
                        modifier = Modifier.padding(start = 10.dp),
                        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = TarnishedGold,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = stringResource(R.string.read_owned_book),
                            color = TarnishedGold,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (!isPendingEditMode && !isOwnedList) {
                Spacer(modifier = Modifier.height(14.dp))

                BookShelfActions(
                    book = libro,
                    viewModelKeyPrefix = "list_detail_shelf",
                    onOrganizationChanged = onOrganizationChanged
                )
            }
        }
    }
}
