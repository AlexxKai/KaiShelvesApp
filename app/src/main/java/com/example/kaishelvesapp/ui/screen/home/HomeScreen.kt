package com.example.kaishelvesapp.ui.screen.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.LibroLeido
import com.example.kaishelvesapp.data.model.UserBookList
import com.example.kaishelvesapp.data.model.UserBookTag
import com.example.kaishelvesapp.data.repository.FriendActivityItem
import com.example.kaishelvesapp.data.repository.FriendActivityType
import com.example.kaishelvesapp.data.repository.UserListsRepository
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.components.ActivitySocialActions
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiNavigationDrawerContent
import com.example.kaishelvesapp.ui.components.KaiPrimaryTopBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.components.KaiUserAvatar
import com.example.kaishelvesapp.ui.components.ReadReviewDialog
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.BookDetailUiState
import com.example.kaishelvesapp.ui.viewmodel.BookDetailViewModel
import com.example.kaishelvesapp.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    title: String = "",
    subtitle: String = "",
    currentSection: KaiSection = KaiSection.HOME,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScanResult: (String) -> Unit,
    userName: String? = null,
    profileImageUrl: String? = null,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onGoToProfile: () -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit,
    pendingRequestCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onOpenFriendProfile: (String) -> Unit,
    onOpenBook: (Libro) -> Unit,
    onSectionSelected: (KaiSection) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerExpanded = drawerState.targetValue == DrawerValue.Open || drawerState.currentValue == DrawerValue.Open
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadFeed()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KaiNavigationDrawerContent(
                currentSection = currentSection,
                subtitle = subtitle,
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
                    current = currentSection,
                    onSelect = onSectionSelected
                )
            }
        ) { innerPadding ->
            PullToRefreshBox(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                DeepWalnut,
                                Obsidian
                            )
                        )
                    )
                    .padding(paddingValues)
                    .padding(innerPadding),
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refreshFeed
            ) {
                when {
                    uiState.isLoading && uiState.activities.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TarnishedGold)
                        }
                    }

                    uiState.errorMessage != null -> {
                        HomeMessageCard(
                            title = stringResource(R.string.home_recent_activity_title),
                            message = uiState.errorMessage!!,
                            actionLabel = stringResource(R.string.retry),
                            onAction = viewModel::loadFeed
                        )
                    }

                    uiState.activities.isEmpty() -> {
                        HomeMessageCard(
                            title = stringResource(R.string.home_empty_activity_title),
                            message = stringResource(R.string.home_empty_activity_body),
                            actionLabel = stringResource(R.string.retry),
                            onAction = viewModel::loadFeed
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Text(
                                    text = stringResource(R.string.home_recent_activity_title),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TarnishedGold
                                )
                            }

                            items(
                                items = uiState.activities,
                                key = { item ->
                                    item.id.ifBlank {
                                        buildString {
                                            append(item.user.uid)
                                            append('_')
                                            append(item.type.name)
                                            append('_')
                                            append(item.book?.id ?: item.readBook?.id ?: "activity")
                                            append('_')
                                            append(item.timestampMillis ?: -1L)
                                        }
                                    }
                                }
                            ) { item ->
                                FeedActivityCard(
                                    item = item,
                                    onOpenFriendProfile = onOpenFriendProfile,
                                    onOpenBook = onOpenBook,
                                    comments = uiState.commentsByActivityId[item.id].orEmpty(),
                                    isLoadingComments = item.id in uiState.loadingCommentIds,
                                    isSocialActionRunning = item.id in uiState.socialActionIds,
                                    onToggleLike = viewModel::toggleLike,
                                    onLoadComments = viewModel::loadComments,
                                    onAddComment = viewModel::addComment
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMessageCard(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Obsidian),
            border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TarnishedGold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OldIvory,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onAction) {
                    Text(text = actionLabel, color = Color(0xFF66D6D6))
                }
            }
        }
    }
}

@Composable
private fun FeedActivityCard(
    item: FriendActivityItem,
    onOpenFriendProfile: (String) -> Unit,
    onOpenBook: (Libro) -> Unit,
    comments: List<com.example.kaishelvesapp.data.repository.ActivityComment>,
    isLoadingComments: Boolean,
    isSocialActionRunning: Boolean,
    onToggleLike: (String) -> Unit,
    onLoadComments: (String) -> Unit,
    onAddComment: (String, String) -> Unit
) {
    val book = remember(item) { item.book ?: item.readBook?.toLibro() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BloodWine.copy(alpha = 0.12f),
                            Obsidian.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier.clickable(
                        enabled = item.user.uid.isNotBlank(),
                        onClick = { onOpenFriendProfile(item.user.uid) }
                    )
                ) {
                    KaiUserAvatar(
                        displayName = item.user.usuario.ifBlank { item.user.email },
                        imageUrl = item.user.photoUrl
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = activityTitle(item),
                        style = MaterialTheme.typography.titleLarge,
                        color = OldIvory,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(
                            enabled = item.user.uid.isNotBlank(),
                            onClick = { onOpenFriendProfile(item.user.uid) }
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formatActivityTimestamp(item.timestampMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = OldIvory.copy(alpha = 0.72f)
                    )
                }
            }

            if (book != null) {
                Spacer(modifier = Modifier.height(14.dp))
                FeedBookCard(
                    item = item,
                    book = book,
                    onOpenBook = onOpenBook
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            ActivitySocialActions(
                item = item,
                postTitle = activityTitle(item),
                postTimestamp = formatActivityTimestamp(item.timestampMillis),
                comments = comments,
                isLoadingComments = isLoadingComments,
                isSaving = isSocialActionRunning,
                onToggleLike = onToggleLike,
                onLoadComments = onLoadComments,
                onAddComment = onAddComment
            )
        }
    }
}

@Composable
private fun FeedBookCard(
    item: FriendActivityItem,
    book: Libro,
    onOpenBook: (Libro) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onOpenBook(book) }),
        verticalAlignment = Alignment.Top
    ) {
        BookCover(
            imageUrl = book.imagen,
            title = book.titulo,
            modifier = Modifier
                .width(92.dp)
                .height(136.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = book.titulo.ifBlank { stringResource(R.string.unknown_title) },
                style = MaterialTheme.typography.titleLarge,
                color = OldIvory,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (book.autor.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.book_by_author, book.autor),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF66D6D6),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            FeedBookActions(book = book)
        }
    }
}

@Composable
private fun FeedBookActions(
    book: Libro
) {
    val safeBookId = book.id.ifBlank { book.isbn }
    val detailViewModel: BookDetailViewModel = viewModel(key = "feed_book_$safeBookId")
    val uiState by detailViewModel.uiState.collectAsStateWithLifecycle()
    var showOrganizerDialog by rememberSaveable(safeBookId) { mutableStateOf(false) }
    var showCreateDialog by rememberSaveable(safeBookId) { mutableStateOf(false) }
    var showRemoveDialog by rememberSaveable(safeBookId) { mutableStateOf(false) }
    var showReadReviewDialog by rememberSaveable(safeBookId) { mutableStateOf(false) }
    var pendingReadTagIds by rememberSaveable(safeBookId) { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(safeBookId) {
        detailViewModel.cargarListasParaLibro(safeBookId)
        detailViewModel.cargarEstadoLectura(safeBookId)
    }

    if (showOrganizerDialog) {
        FeedBookOrganizationDialog(
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
                if (
                    selectedListId == UserListsRepository.SYSTEM_LIST_READ_ID &&
                    UserListsRepository.SYSTEM_LIST_READ_ID !in uiState.selectedListIds
                ) {
                    pendingReadTagIds = selectedTagIds
                    showReadReviewDialog = true
                } else {
                    detailViewModel.guardarOrganizacion(
                        libro = book,
                        selectedListIds = selectedListId?.let(::setOf).orEmpty(),
                        selectedTagIds = selectedTagIds
                    )
                }
                showOrganizerDialog = false
            },
            onCreateNew = { showCreateDialog = true },
            onRemoveFromMyBooks = { showRemoveDialog = true }
        )
    }

    if (showCreateDialog) {
        FeedCreateShelfOrTagDialog(
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

    if (showReadReviewDialog) {
        ReadReviewDialog(
            libro = book,
            isSaving = uiState.isSavingLists,
            initialRating = uiState.readBook?.puntuacion ?: 0,
            initialReview = uiState.readBook?.resena.orEmpty(),
            initialContainsSpoilers = uiState.readBook?.contieneSpoilers ?: false,
            onDismiss = {
                if (!uiState.isSavingLists) {
                    showReadReviewDialog = false
                }
            },
            onSave = { rating, review, containsSpoilers ->
                detailViewModel.guardarLecturaConResena(
                    libro = book,
                    selectedTagIds = pendingReadTagIds,
                    puntuacion = rating,
                    resena = review,
                    contieneSpoilers = containsSpoilers
                )
                showReadReviewDialog = false
            }
        )
    }

    FeedShelfActionRow(
        uiState = uiState,
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
private fun FeedShelfActionRow(
    uiState: BookDetailUiState,
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
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            colors = KaiShelvesThemeDefaults.secondaryButtonColors()
        ) {
            CircularProgressIndicator(color = OldIvory)
        }
    } else if (uiState.availableLists.isNotEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.fillMaxWidth(),
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
private fun FeedBookOrganizationDialog(
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
                .padding(top = 28.dp),
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

                FeedSectionTitle(title = stringResource(R.string.select_shelf))

                availableLists.forEach { list ->
                    FeedShelfOptionRow(
                        list = list,
                        isSelected = selectedListId == list.id,
                        onSelect = { selectedListId = list.id }
                    )
                }

                FeedSectionTitle(title = stringResource(R.string.select_tags))

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
private fun FeedSectionTitle(title: String) {
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
private fun FeedShelfOptionRow(
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
private fun FeedCreateShelfOrTagDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onCreateShelf: (String) -> Unit,
    onCreateTag: (String) -> Unit
) {
    var selectedType by rememberSaveable { mutableStateOf(FeedCreateType.SHELF) }
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
                            modifier = Modifier.clickable { selectedType = FeedCreateType.TAG }
                        ) {
                            RadioButton(
                                selected = selectedType == FeedCreateType.TAG,
                                onClick = { selectedType = FeedCreateType.TAG }
                            )
                            Text(text = stringResource(R.string.tag), color = OldIvory)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedType = FeedCreateType.SHELF }
                        ) {
                            RadioButton(
                                selected = selectedType == FeedCreateType.SHELF,
                                onClick = { selectedType = FeedCreateType.SHELF }
                            )
                            Text(text = stringResource(R.string.shelf), color = OldIvory)
                        }
                    }

                    Text(
                        text = if (selectedType == FeedCreateType.SHELF) {
                            stringResource(R.string.single_shelf_note)
                        } else {
                            stringResource(R.string.tag_create_note)
                        },
                        color = OldIvory,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    androidx.compose.material3.OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                text = if (selectedType == FeedCreateType.SHELF) {
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
                                    if (selectedType == FeedCreateType.SHELF) {
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

private enum class FeedCreateType {
    TAG,
    SHELF
}

@Composable
private fun activityTitle(item: FriendActivityItem): String {
    val userName = item.user.usuario.ifBlank { stringResource(R.string.unknown_username) }
    return when (item.type) {
        FriendActivityType.FRIENDSHIP -> stringResource(
            R.string.friendship_update_text,
            userName,
            item.relatedUserName.orEmpty()
        )
        FriendActivityType.WANT_TO_READ -> stringResource(R.string.friend_wants_to_read, userName)
        FriendActivityType.READING -> stringResource(R.string.friend_is_reading, userName)
        FriendActivityType.READ -> stringResource(R.string.friend_has_read, userName)
    }
}

@Composable
private fun statusLabel(type: FriendActivityType): String {
    return when (type) {
        FriendActivityType.FRIENDSHIP -> stringResource(R.string.friends)
        FriendActivityType.WANT_TO_READ -> stringResource(R.string.want_to_read)
        FriendActivityType.READING -> stringResource(R.string.currently_reading)
        FriendActivityType.READ -> stringResource(R.string.mark_as_read)
    }
}

@Composable
private fun formatActivityTimestamp(timestampMillis: Long?): String {
    if (timestampMillis == null) {
        return stringResource(R.string.recently_label)
    }

    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val pattern = if (locale.language == "es") {
        "d 'de' MMM 'a la(s)' HH:mm"
    } else {
        "MMM d 'at' HH:mm"
    }

    return remember(timestampMillis, locale) {
        SimpleDateFormat(pattern, locale)
            .format(Date(timestampMillis))
            .replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(locale) else char.toString()
            }
    }
}

private fun LibroLeido.toLibro(): Libro {
    return Libro(
        id = id,
        isbn = isbn,
        titulo = titulo,
        autor = autor,
        editorial = editorial,
        genero = genero,
        fechaPublicacion = fechaPublicacion,
        paginas = paginas,
        imagen = imagen,
        pdf = pdf
    )
}
