package com.example.kaishelvesapp.ui.screen.lists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.repository.UserListsRepository
import com.example.kaishelvesapp.ui.components.RatingStars
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.components.BookShelfActions
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
import kotlin.math.roundToInt

private enum class ListDetailSortOption {
    TITLE,
    AUTHOR,
    RATING,
    READ_DATE
}

@Composable
fun UserListDetailScreen(
    listId: String,
    viewModel: UserListDetailViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit,
    onBookClick: (Libro) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var sortOption by remember(listId) { mutableStateOf(ListDetailSortOption.TITLE) }
    val isReadList = listId == UserListsRepository.SYSTEM_LIST_READ_ID
    val isPendingList = listId == UserListsRepository.SYSTEM_LIST_PENDING_ID
    val displayedPendingBooks = remember { mutableStateListOf<UserListDetailBookItem>() }
    var isPendingEditMode by rememberSaveable(listId) { mutableStateOf(false) }
    var showCoverView by rememberSaveable(listId) { mutableStateOf(false) }
    var draggingBookId by remember { mutableStateOf<String?>(null) }
    var draggingStartIndex by remember { mutableStateOf<Int?>(null) }
    var draggingTranslationX by remember { mutableFloatStateOf(0f) }
    var draggingTranslationY by remember { mutableFloatStateOf(0f) }
    var autoScrollDelta by remember { mutableFloatStateOf(0f) }
    val sortedBooks = remember(uiState.books, sortOption, isReadList, isPendingList) {
        if (isPendingList) {
            uiState.books
        } else {
            when (sortOption) {
                ListDetailSortOption.TITLE -> uiState.books.sortedBy { it.book.titulo.lowercase() }
                ListDetailSortOption.AUTHOR -> uiState.books.sortedBy { it.book.autor.lowercase() }
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
        sortOption = if (isReadList) ListDetailSortOption.READ_DATE else ListDetailSortOption.TITLE
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
            draggingStartIndex = null
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ListDetailHeaderCard(
                    userList = uiState.userList,
                    books = sortedBooks,
                    sortOption = sortOption,
                    isReadList = isReadList,
                    isPendingList = isPendingList,
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
                            key = { index, row -> "cover_row_$index:${row.joinToString { it.book.id.ifBlank { it.book.isbn } }}" }
                        ) { rowIndex, rowItems ->
                            CoverRow(
                                items = rowItems,
                                isPendingList = isPendingList,
                                draggingBookId = draggingBookId,
                                draggingOffsetX = draggingTranslationX,
                                draggingOffsetY = draggingTranslationY,
                                onBookClick = { book -> onBookClick(book) },
                                dragModifierFor = { bookId ->
                                    if (isPendingList) {
                                        Modifier.pointerInput(bookId, visibleBooks.size) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggingBookId = bookId
                                                    draggingStartIndex = displayedPendingBooks.indexOfFirst { pendingItem ->
                                                        pendingItem.book.id.ifBlank { pendingItem.book.isbn } == bookId
                                                    }.takeIf { it >= 0 }
                                                    draggingTranslationX = 0f
                                                    draggingTranslationY = 0f
                                                },
                                                onDragCancel = {
                                                    draggingBookId = null
                                                    draggingStartIndex = null
                                                    draggingTranslationX = 0f
                                                    draggingTranslationY = 0f
                                                    autoScrollDelta = 0f
                                                    displayedPendingBooks.clear()
                                                    displayedPendingBooks.addAll(uiState.books)
                                                },
                                                onDragEnd = {
                                                    val fromIndex = draggingStartIndex
                                                    if (fromIndex != null) {
                                                        val columnDelta = (draggingTranslationX / 104f).roundToInt()
                                                        val rowDelta = (draggingTranslationY / 158f).roundToInt()
                                                        val targetIndex = (fromIndex + columnDelta + (rowDelta * 3))
                                                            .coerceIn(0, displayedPendingBooks.lastIndex)
                                                        if (targetIndex != fromIndex) {
                                                            displayedPendingBooks.move(fromIndex, targetIndex)
                                                        }
                                                    }
                                                    val orderedIds = displayedPendingBooks.map { pendingItem ->
                                                        pendingItem.book.id.ifBlank { pendingItem.book.isbn }
                                                    }
                                                    draggingBookId = null
                                                    draggingStartIndex = null
                                                    draggingTranslationX = 0f
                                                    draggingTranslationY = 0f
                                                    autoScrollDelta = 0f
                                                    viewModel.updateBooksOrder(listId, orderedIds)
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    draggingTranslationX += dragAmount.x
                                                    draggingTranslationY += dragAmount.y
                                                    val currentIndex = displayedPendingBooks.indexOfFirst { pendingItem ->
                                                        pendingItem.book.id.ifBlank { pendingItem.book.isbn } == bookId
                                                    }
                                                    if (currentIndex == -1) return@detectDragGesturesAfterLongPress

                                                    val rowItem = listState.layoutInfo.visibleItemsInfo
                                                        .firstOrNull { it.index == rowIndex + 1 }
                                                    if (rowItem != null) {
                                                        autoScrollDelta = when {
                                                            rowItem.offset + rowItem.size + draggingTranslationY >
                                                                listState.layoutInfo.viewportEndOffset - 96 -> 18f
                                                            rowItem.offset + draggingTranslationY <
                                                                listState.layoutInfo.viewportStartOffset + 96 -> -18f
                                                            else -> 0f
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
                            isPendingEditMode = isPendingEditMode,
                            isDragging = isDragging,
                            draggingOffset = if (isDragging) draggingTranslationY else 0f,
                            onOpen = { onBookClick(item.book) },
                            dragHandleModifier = if (isPendingList && isPendingEditMode) {
                                Modifier.pointerInput(bookId, visibleBooks.size) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingBookId = bookId
                                            draggingStartIndex = null
                                            draggingTranslationX = 0f
                                            draggingTranslationY = 0f
                                        },
                                        onDragCancel = {
                                            draggingBookId = null
                                            draggingStartIndex = null
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
                                            draggingStartIndex = null
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
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = TarnishedGold
                    )
                }

                Text(
                    text = userList?.name ?: stringResource(R.string.list_detail_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TarnishedGold,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                if (books.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        books.take(3).forEach { item ->
                            BookCover(
                                imageUrl = item.book.imagen,
                                title = item.book.titulo,
                                showFrame = false,
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(42.dp)
                            )
                        }
                    }
                }
            }

            userList?.description?.takeIf { it.isNotBlank() }?.let { description ->
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OldIvory
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(
                    R.string.list_books_count,
                    userList?.bookCount ?: books.size
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
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

            if (!isPendingList) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(
                            color = BloodWine.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSortChange() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.sort_by_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OldIvory
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = when {
                            !isReadList && sortOption == ListDetailSortOption.AUTHOR -> stringResource(R.string.sort_option_author)
                            !isReadList -> stringResource(R.string.sort_option_title)
                            sortOption == ListDetailSortOption.READ_DATE -> stringResource(R.string.sort_option_read_date)
                            sortOption == ListDetailSortOption.RATING -> stringResource(R.string.sort_option_rating)
                            else -> stringResource(R.string.sort_option_title)
                        },
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
    isPendingList: Boolean,
    draggingBookId: String?,
    draggingOffsetX: Float,
    draggingOffsetY: Float,
    onBookClick: (Libro) -> Unit,
    dragModifierFor: (String) -> Modifier
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            val bookId = item.book.id.ifBlank { item.book.isbn }
            val isDragging = draggingBookId == bookId
            Column(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        translationX = if (isDragging) draggingOffsetX else 0f
                        translationY = if (isDragging) draggingOffsetY else 0f
                        scaleX = if (isDragging) 1.04f else 1f
                        scaleY = if (isDragging) 1.04f else 1f
                    }
                    .then(dragModifierFor(bookId))
                    .clickable(enabled = draggingBookId == null) { onBookClick(item.book) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BookCover(
                    imageUrl = item.book.imagen,
                    title = item.book.titulo,
                    showFrame = false,
                    modifier = Modifier
                        .width(92.dp)
                        .height(138.dp)
                )
            }
        }

        repeat(3 - items.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ListBookCard(
    item: UserListDetailBookItem,
    isReadList: Boolean,
    isPendingList: Boolean,
    isPendingEditMode: Boolean,
    isDragging: Boolean,
    draggingOffset: Float,
    onOpen: () -> Unit,
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
            .clickable(enabled = !isPendingEditMode) { onOpen() },
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
                BookCover(
                    imageUrl = libro.imagen,
                    title = libro.titulo,
                    showFrame = false,
                    modifier = Modifier
                        .width(if (isPendingEditMode) 48.dp else 62.dp)
                        .height(if (isPendingEditMode) 72.dp else 92.dp)
                )

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
                }
            }

            if (!isPendingEditMode) {
                Spacer(modifier = Modifier.height(14.dp))

                BookShelfActions(
                    book = libro,
                    viewModelKeyPrefix = "list_detail_shelf"
                )
            }
        }
    }
}
