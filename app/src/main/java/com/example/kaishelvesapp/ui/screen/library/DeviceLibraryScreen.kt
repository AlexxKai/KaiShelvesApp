package com.example.kaishelvesapp.ui.screen.library

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.repository.BookRepository
import com.example.kaishelvesapp.data.repository.DeviceLibraryFile
import com.example.kaishelvesapp.data.repository.ImportedReadMetadata
import com.example.kaishelvesapp.data.repository.UserListsRepository
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiNavigationDrawerContent
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.components.ReadReviewDialog
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.DeviceLibraryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap

enum class DeviceLibraryLayoutMode {
    List,
    Grid,
    Carousel
}

enum class DeviceLibrarySortOption {
    Title,
    Author,
    Recent,
    Folder,
    RecentList
}

enum class DeviceLibraryFileTypeFilter(
    val extensions: Set<String>,
    val labelRes: Int
) {
    Epub(setOf("epub"), R.string.device_library_file_type_epub),
    PdfDjvu(setOf("pdf", "djvu", "djv"), R.string.device_library_file_type_pdf_djvu),
    MobiAzwPrc(setOf("mobi", "azw", "azw3", "prc"), R.string.device_library_file_type_mobi_azw_prc),
    Fb2(setOf("fb2"), R.string.device_library_file_type_fb2),
    ChmUmd(setOf("chm", "umd"), R.string.device_library_file_type_chm_umd),
    Document(setOf("doc", "docx", "odt", "rtf"), R.string.device_library_file_type_document),
    Text(setOf("txt", "md", "log"), R.string.device_library_file_type_text),
    Comic(setOf("cbz", "cbr"), R.string.device_library_file_type_comic),
    Html(setOf("html", "htm", "mhtml", "mht"), R.string.device_library_file_type_html)
}

enum class DeviceLibraryReadingStatus {
    Unread,
    Reading,
    Finished
}

data class DeviceLibraryAuthorGroup(
    val name: String,
    val count: Int
)

private data class DeviceLibraryResolvedMetadata(
    val title: String,
    val author: String
)

fun DeviceLibraryFile.matchesFileTypeFilters(selectedTypes: Set<DeviceLibraryFileTypeFilter>): Boolean {
    if (selectedTypes.isEmpty()) return true
    val extension = name.substringAfterLast('.', missingDelimiterValue = "").lowercase(Locale.ROOT)
    return selectedTypes.any { filter -> extension in filter.extensions }
}

@Composable
fun DeviceLibraryScreen(
    userName: String?,
    profileImageUrl: String?,
    //searchQuery: String,
    // onSearchQueryChange: (String) -> Unit,
    // onSearch: () -> Unit,
    // onScanResult: (String) -> Unit,
    onGoToProfile: () -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit,
    // pendingRequestCount: Int = 0,
    // onOpenNotifications: () -> Unit = {},
    onSectionSelected: (KaiSection) -> Unit,
    openBookUri: String? = null,
    onOpenBookUriConsumed: () -> Unit = {},
    onReaderClosed: () -> Unit = {},
    viewModel: DeviceLibraryViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerExpanded = drawerState.targetValue == DrawerValue.Open || drawerState.currentValue == DrawerValue.Open
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val snackbarHostState = remember { SnackbarHostState() }
    // Recupera todos los ajustes del panel de filtros para mantener la vista elegida entre sesiones.
    var layoutMode by remember { mutableStateOf(readDeviceLibraryLayoutMode(context)) }
    var sortOption by remember { mutableStateOf(readDeviceLibrarySortOption(context)) }
    var sortDescending by remember { mutableStateOf(readDeviceLibrarySortDescending(context)) }
    var selectedFileTypes by remember { mutableStateOf(readDeviceLibraryFileTypeFilters(context)) }
    var selectedReadingStatuses by remember {
        mutableStateOf(readDeviceLibraryReadingStatuses(context))
    }
    var showSearchPanel by remember { mutableStateOf(false) }
    var showFilterPanel by remember { mutableStateOf(false) }
    var showImportBooksDialog by remember { mutableStateOf(false) }
    var showDefaultCoverScreen by remember { mutableStateOf(false) }
    var readerFile by remember { mutableStateOf<DeviceLibraryFile?>(null) }
    var readerStartProgress by remember { mutableStateOf(0) }
    var readerListPromptsEnabled by remember { mutableStateOf(true) }
    var pendingFirstOpenFile by remember { mutableStateOf<DeviceLibraryFile?>(null) }
    var pendingFinishedFile by remember { mutableStateOf<DeviceLibraryFile?>(null) }
    var pendingReadReviewBook by remember { mutableStateOf<Libro?>(null) }
    var isAddingReaderListBook by remember { mutableStateOf(false) }
    var progressRevision by remember { mutableStateOf(0) }
    var metadataRevision by remember { mutableStateOf(0) }
    var rebuildingCovers by remember { mutableStateOf(false) }
    var topBarHeight by remember { mutableStateOf(0.dp) }
    var selectedAuthor by remember { mutableStateOf<String?>(null) }
    val fileMetadata by produceState<Map<String, DeviceLibraryResolvedMetadata>>(
        initialValue = emptyMap(),
        uiState.files
    ) {
        value = withContext(Dispatchers.IO) {
            uiState.files
                .associate { file ->
                    val userMetadata = readDeviceBookUserMetadata(context, file)
                    val epubMetadata = if (isEpub(file)) {
                        extractEpubDisplayMetadata(context, file.uri)
                    } else {
                        null
                    }
                    val title = userMetadata.title
                        .ifBlank { epubMetadata?.title.orEmpty() }
                        .ifBlank { file.name.substringBeforeLast('.') }
                    val author = userMetadata.author
                        .ifBlank { epubMetadata?.author.orEmpty() }
                        .ifBlank { "(${readableFileType(file)})" }
                    file.uri.toString() to DeviceLibraryResolvedMetadata(title = title, author = author)
                }
        }
    }
    val authorGroups = remember(uiState.files, fileMetadata) {
        uiState.files
            .mapNotNull { file -> fileMetadata[file.uri.toString()]?.author?.trim()?.takeIf { it.isNotBlank() } }
            .groupingBy { it }
            .eachCount()
            .map { (author, count) -> DeviceLibraryAuthorGroup(name = author, count = count) }
            .sortedWith(
                compareBy<DeviceLibraryAuthorGroup> { it.name.startsWith("(") }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )
    }
    LaunchedEffect(authorGroups) {
        val currentAuthor = selectedAuthor
        if (currentAuthor != null && authorGroups.none { it.name == currentAuthor }) {
            selectedAuthor = null
        }
    }
    val sortedFiles = remember(
        uiState.files,
        uiState.searchQuery,
        fileMetadata,
        selectedAuthor,
        sortOption,
        sortDescending,
        selectedFileTypes,
        selectedReadingStatuses,
        progressRevision
    ) {
        val cleanQuery = uiState.searchQuery.trim()
        val searchFilteredFiles = if (cleanQuery.isBlank()) {
            uiState.files
        } else {
            uiState.files.filter { file ->
                val metadata = fileMetadata[file.uri.toString()]
                file.name.contains(cleanQuery, ignoreCase = true) ||
                    file.location.contains(cleanQuery, ignoreCase = true) ||
                    file.mimeType.orEmpty().contains(cleanQuery, ignoreCase = true) ||
                    metadata?.title.orEmpty().contains(cleanQuery, ignoreCase = true) ||
                    metadata?.author.orEmpty().contains(cleanQuery, ignoreCase = true)
            }
        }
        val authorFilteredFiles = selectedAuthor?.let { author ->
            searchFilteredFiles.filter { file ->
                fileMetadata[file.uri.toString()]?.author == author
            }
        } ?: searchFilteredFiles
        val formatFilteredFiles = authorFilteredFiles
            .filter { it.matchesFileTypeFilters(selectedFileTypes) }
            .filter { file ->
                readingStatusForProgress(readDeviceBookProgressPercent(context, file)) in selectedReadingStatuses
            }
        val sorted = when (sortOption) {
            DeviceLibrarySortOption.Title -> formatFilteredFiles.sortedBy { file ->
                fileMetadata[file.uri.toString()]?.title?.takeIf { it.isNotBlank() }?.lowercase(Locale.ROOT)
                    ?: file.name.substringBeforeLast('.').lowercase(Locale.ROOT)
            }
            DeviceLibrarySortOption.Author -> formatFilteredFiles.sortedBy { file ->
                fileMetadata[file.uri.toString()]?.author?.takeIf { it.isNotBlank() }?.lowercase(Locale.ROOT)
                    ?: file.name.substringBeforeLast('.').lowercase(Locale.ROOT)
            }
            DeviceLibrarySortOption.Recent -> formatFilteredFiles.sortedBy { it.modifiedAtMillis ?: 0L }
            DeviceLibrarySortOption.Folder -> formatFilteredFiles.sortedWith(
                compareBy<DeviceLibraryFile> { it.location.lowercase(Locale.ROOT) }
                    .thenBy { it.name.substringBeforeLast('.').lowercase(Locale.ROOT) }
            )
            DeviceLibrarySortOption.RecentList -> formatFilteredFiles.sortedBy { it.uri.toString() }
        }
        if (sortDescending) sorted.asReversed() else sorted
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) viewModel.useFolder(uri)
    }
    val showFolderSelectionHint =
        !uiState.isLoading &&
            uiState.files.isEmpty() &&
            uiState.searchQuery.isBlank() &&
            selectedAuthor == null

    fun showReaderListResult(
        result: Result<Unit>,
        successMessage: String
    ) {
        scope.launch {
            snackbarHostState.showSnackbar(
                result.fold(
                    onSuccess = { successMessage },
                    onFailure = { context.getString(R.string.reader_list_add_error) }
                )
            )
        }
    }

    fun addBookToReaderList(
        file: DeviceLibraryFile,
        listId: String,
        readMetadata: ImportedReadMetadata? = null,
        onDone: (Result<Unit>) -> Unit = {}
    ) {
        if (isAddingReaderListBook) return
        isAddingReaderListBook = true
        scope.launch {
            val metadata = fileMetadata[file.uri.toString()]
            val result = withContext(Dispatchers.IO) {
                resolveDeviceLibraryBook(
                    context = context,
                    repository = BookRepository(),
                    file = file,
                    metadata = metadata
                ).fold(
                    onSuccess = { libro ->
                        val listsRepository = UserListsRepository()
                        if (listId == UserListsRepository.SYSTEM_LIST_READ_ID) {
                            listsRepository.updateBookAssignments(
                                libro = libro,
                                selectedListIds = setOf(UserListsRepository.SYSTEM_LIST_READ_ID),
                                readMetadata = readMetadata
                            )
                        } else {
                            listsRepository.updateBookAssignments(libro, setOf(listId))
                        }
                    },
                    onFailure = { Result.failure(it) }
                )
            }
            isAddingReaderListBook = false
            onDone(result)
        }
    }

    fun resolveBookForReadReview(
        file: DeviceLibraryFile,
        onDone: (Result<Libro>) -> Unit
    ) {
        if (isAddingReaderListBook) return
        isAddingReaderListBook = true
        scope.launch {
            val metadata = fileMetadata[file.uri.toString()]
            val result = withContext(Dispatchers.IO) {
                resolveDeviceLibraryBook(
                    context = context,
                    repository = BookRepository(),
                    file = file,
                    metadata = metadata
                )
            }
            isAddingReaderListBook = false
            onDone(result)
        }
    }

    fun addResolvedReadBook(
        libro: Libro,
        readMetadata: ImportedReadMetadata,
        onDone: (Result<Unit>) -> Unit
    ) {
        if (isAddingReaderListBook) return
        isAddingReaderListBook = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                UserListsRepository().updateBookAssignments(
                    libro = libro,
                    selectedListIds = setOf(UserListsRepository.SYSTEM_LIST_READ_ID),
                    readMetadata = readMetadata
                )
            }
            isAddingReaderListBook = false
            onDone(result)
        }
    }

    fun launchReader(
        file: DeviceLibraryFile,
        enableListPrompts: Boolean = true
    ) {
        readerStartProgress = readDeviceBookProgressPercent(context, file)
        readerListPromptsEnabled = enableListPrompts
        saveLastOpenedDeviceBookUri(context, file.uri.toString())
        readerFile = file
    }

    fun openReader(file: DeviceLibraryFile) {
        when (readReaderListAutomationMode(context)) {
            ReaderListAutomationMode.Disabled -> launchReader(file)
            ReaderListAutomationMode.Ask -> {
                if (readDeviceBookProgressPercent(context, file) == 0) {
                    pendingFirstOpenFile = file
                } else {
                    launchReader(file)
                }
            }
            ReaderListAutomationMode.Automatic -> {
                if (readDeviceBookProgressPercent(context, file) == 0) {
                    addBookToReaderList(
                        file = file,
                        listId = UserListsRepository.SYSTEM_LIST_READING_ID,
                        onDone = { result ->
                            showReaderListResult(
                                result = result,
                                successMessage = context.getString(R.string.reader_added_to_reading_snackbar)
                            )
                            launchReader(file)
                        }
                    )
                } else {
                    launchReader(file)
                }
            }
        }
    }

    LaunchedEffect(openBookUri, uiState.files, uiState.isLoading, uiState.hasLoadedFiles) {
        val requestedUri = openBookUri?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val targetFile = uiState.files.firstOrNull { it.uri.toString() == requestedUri }
        if (targetFile != null) {
            openReader(targetFile)
            onOpenBookUriConsumed()
        } else if (uiState.hasLoadedFiles && !uiState.isLoading) {
            val externalFile = externalDeviceLibraryFile(context, requestedUri)
            if (externalFile != null) {
                launchReader(externalFile, enableListPrompts = false)
            }
            onOpenBookUriConsumed()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KaiNavigationDrawerContent(
                currentSection = KaiSection.LIBRARY,
                subtitle = stringResource(R.string.device_library_subtitle),
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
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    DeviceLibraryTopBar(
                        onOpenMenu = { scope.launch { drawerState.open() } },
                        fileCount = sortedFiles.size,
                        totalFileCount = uiState.files.size,
                        selectedAuthor = selectedAuthor,
                        authorGroups = authorGroups,
                        showSearchPanel = showSearchPanel,
                        onShowSearchPanel = { showSearchPanel = true },
                        onDismissSearchPanel = { showSearchPanel = false },
                        onToggleFilterPanel = { showFilterPanel = !showFilterPanel },
                        onDismissFilterPanel = { showFilterPanel = false },
                        // onQueryChange = viewModel::onSearchQueryChange,
                        onAllBooksSelected = {
                            selectedAuthor = null
                        },
                        onAuthorSelected = { author ->
                            selectedAuthor = author
                        },
                        onChooseFolder = { folderLauncher.launch(null) },
                        onImportBooks = { showImportBooksDialog = true },
                        onDefaultCover = { showDefaultCoverScreen = true },
                        onRebuildBookCovers = {
                            if (!rebuildingCovers) {
                                rebuildingCovers = true
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.device_library_rebuilding_covers),
                                    Toast.LENGTH_SHORT
                                ).show()
                                scope.launch {
                                    val backgroundTreeUri = readDefaultCoverStorageTreeUri(context)?.let(Uri::parse)
                                    val rebuiltCount = withContext(Dispatchers.IO) {
                                        rebuildMissingDeviceBookCovers(
                                            context = context,
                                            files = uiState.files,
                                            fileMetadata = fileMetadata,
                                            backgroundTreeUri = backgroundTreeUri
                                        )
                                    }
                                    rebuildingCovers = false
                                    if (rebuiltCount > 0) {
                                        metadataRevision++
                                    }
                                    Toast.makeText(
                                        context,
                                        if (rebuiltCount > 0) {
                                            context.getString(R.string.device_library_rebuilt_covers, rebuiltCount)
                                        } else {
                                            context.getString(R.string.device_library_no_new_covers)
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onOpenRandomBook = {
                            val availableBooks = uiState.files.filter { file ->
                                readDeviceBookProgressPercent(context, file) < 100
                            }
                            val randomBook = availableBooks.randomOrNull()
                            if (randomBook != null) {
                                openReader(randomBook)
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.device_library_no_pending_random_book),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onHeightChanged = { heightPx ->
                            topBarHeight = with(density) { heightPx.toDp() }
                        }
                    )
                },
                bottomBar = {
                    KaiBottomBar(
                        current = KaiSection.LIBRARY,
                        onSelect = onSectionSelected
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            val lastBookUri = readLastOpenedDeviceBookUri(context)
                            val lastBook = uiState.files.firstOrNull { it.uri.toString() == lastBookUri }
                            if (lastBook != null) {
                                openReader(lastBook)
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.device_library_no_recent_book),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        containerColor = Color(0xFF3A3A3A),
                        contentColor = OldIvory
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Ir al libro actual"
                        )
                    }
                }
            ) { innerPadding ->
                DeviceLibraryContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    files = sortedFiles,
                    isLoading = uiState.isLoading,
                    errorMessage = uiState.errorMessage,
                    hasActiveSearch = uiState.searchQuery.trim().isNotBlank() && uiState.files.isNotEmpty(),
                    searchQuery = uiState.searchQuery,
                    layoutMode = layoutMode,
                    progressRevision = progressRevision,
                    metadataRevision = metadataRevision,
                    onRemoveFile = viewModel::removeFromLibrary,
                    onOpenFile = { file -> openReader(file) }
                )
            }

            if (showFolderSelectionHint) {
                DeviceLibraryFolderHintArrow(modifier = Modifier.fillMaxSize())
            }

            if (showSearchPanel && !showFilterPanel) {
                DeviceLibrarySearchOverlay(
                    topPadding = topBarHeight,
                    query = uiState.searchQuery,
                    files = uiState.files,
                    recentSearches = uiState.recentSearches,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onCommitSearch = viewModel::commitSearch,
                    onRemoveRecentSearch = viewModel::removeRecentSearch,
                    onDismiss = { showSearchPanel = false }
                )
            }

            if (showFilterPanel) {
                    DeviceLibraryFilterOverlay(
                    layoutMode = layoutMode,
                    sortOption = sortOption,
                    sortDescending = sortDescending,
                    selectedFileTypes = selectedFileTypes,
                    selectedReadingStatuses = selectedReadingStatuses,
                    onLayoutModeChange = {
                        layoutMode = it
                        saveDeviceLibraryLayoutMode(context, it)
                    },
                    onSortOptionChange = {
                        sortOption = it
                        saveDeviceLibrarySortOption(context, it)
                    },
                    onToggleSortDirection = {
                        val updatedSortDescending = !sortDescending
                        sortDescending = updatedSortDescending
                        saveDeviceLibrarySortDescending(context, updatedSortDescending)
                    },
                    onReadingStatusesChange = {
                        selectedReadingStatuses = it
                        saveDeviceLibraryReadingStatuses(context, it)
                    },
                    onFileTypesChange = {
                        selectedFileTypes = it
                        saveDeviceLibraryFileTypeFilters(context, it)
                    },
                    onDismiss = { showFilterPanel = false }
                )
            }

            if (showImportBooksDialog) {
                ImportBooksDialog(
                    initialTreeUri = uiState.selectedFolderUri?.let(Uri::parse),
                    onDismiss = { showImportBooksDialog = false },
                    onAccept = {
                        showImportBooksDialog = false
                        Toast.makeText(
                            context,
                            context.getString(R.string.device_library_import_configured),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            if (showDefaultCoverScreen) {
                DefaultCoverScreen(
                    onDismiss = { showDefaultCoverScreen = false }
                )
            }

            readerFile?.let { file ->
                DeviceBookReaderDialog(
                    file = file,
                    onProgressChanged = { progressRevision++ },
                    onDismiss = {
                        val finishedNow = readerStartProgress < 100 &&
                            readDeviceBookProgressPercent(context, file) >= 100
                        readerFile = null
                        onReaderClosed()
                        if (readerListPromptsEnabled && finishedNow) {
                            when (readReaderListAutomationMode(context)) {
                                ReaderListAutomationMode.Ask -> pendingFinishedFile = file
                                ReaderListAutomationMode.Automatic -> {
                                    addBookToReaderList(
                                        file = file,
                                        listId = UserListsRepository.SYSTEM_LIST_READ_ID,
                                        onDone = { result ->
                                            showReaderListResult(
                                                result = result,
                                                successMessage = context.getString(R.string.reader_added_to_read_snackbar)
                                            )
                                        }
                                    )
                                }
                                ReaderListAutomationMode.Disabled -> Unit
                            }
                        }
                    }
                )
            }

            pendingFirstOpenFile?.let { file ->
                ReaderListActionDialog(
                    title = stringResource(R.string.reader_add_to_reading_title),
                    body = stringResource(R.string.reader_add_to_reading_body),
                    isLoading = isAddingReaderListBook,
                    onDismiss = {
                        if (!isAddingReaderListBook) pendingFirstOpenFile = null
                    },
                    onSkip = {
                        pendingFirstOpenFile = null
                        launchReader(file)
                    },
                    onConfirm = {
                        addBookToReaderList(
                            file = file,
                            listId = UserListsRepository.SYSTEM_LIST_READING_ID,
                            onDone = { result ->
                                showReaderListResult(
                                    result = result,
                                    successMessage = context.getString(R.string.reader_added_to_reading_snackbar)
                                )
                                pendingFirstOpenFile = null
                                launchReader(file)
                            }
                        )
                    }
                )
            }

            pendingFinishedFile?.let { file ->
                ReaderListActionDialog(
                    title = stringResource(R.string.reader_add_to_read_title),
                    body = stringResource(R.string.reader_add_to_read_body),
                    isLoading = isAddingReaderListBook,
                    onDismiss = {
                        if (!isAddingReaderListBook) pendingFinishedFile = null
                    },
                    onSkip = { pendingFinishedFile = null },
                    onConfirm = {
                        resolveBookForReadReview(file) { result ->
                            result.fold(
                                onSuccess = { libro ->
                                    pendingFinishedFile = null
                                    pendingReadReviewBook = libro
                                },
                                onFailure = {
                                    showReaderListResult(
                                        result = Result.failure(it),
                                        successMessage = context.getString(R.string.reader_added_to_read_snackbar)
                                    )
                                }
                            )
                        }
                    }
                )
            }

            pendingReadReviewBook?.let { libro ->
                ReadReviewDialog(
                    libro = libro,
                    isSaving = isAddingReaderListBook,
                    initialRating = 0,
                    initialReview = "",
                    initialContainsSpoilers = false,
                    onDismiss = {
                        if (!isAddingReaderListBook) pendingReadReviewBook = null
                    },
                    onSave = { rating, review, containsSpoilers ->
                        addResolvedReadBook(
                            libro = libro,
                            readMetadata = ImportedReadMetadata(
                                rating = rating,
                                review = review,
                                containsSpoilers = containsSpoilers
                            ),
                            onDone = { result ->
                                showReaderListResult(
                                    result = result,
                                    successMessage = context.getString(R.string.reader_added_to_read_snackbar)
                                )
                                if (result.isSuccess) {
                                    pendingReadReviewBook = null
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ReaderListActionDialog(
    title: String,
    body: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSkip: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            Text(text = body)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading
            ) {
                Text(text = stringResource(R.string.reader_list_yes_add))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onSkip,
                enabled = !isLoading
            ) {
                Text(text = stringResource(R.string.reader_list_no_add))
            }
        }
    )
}

private suspend fun resolveDeviceLibraryBook(
    context: Context,
    repository: BookRepository,
    file: DeviceLibraryFile,
    metadata: DeviceLibraryResolvedMetadata?
): Result<Libro> {
    val userMetadata = readDeviceBookUserMetadata(context, file)
    val epubMetadata = if (isEpub(file)) extractEpubDisplayMetadata(context, file.uri) else null
    val title = userMetadata.title
        .ifBlank { epubMetadata?.title.orEmpty() }
        .ifBlank { metadata?.title.orEmpty() }
        .ifBlank { file.name.substringBeforeLast('.') }
        .trim()
    val author = userMetadata.author
        .ifBlank { epubMetadata?.author.orEmpty() }
        .ifBlank { metadata?.author.orEmpty().takeUnless { it.startsWith("(") }.orEmpty() }
        .trim()
    val query = listOf(title, author)
        .filter { it.isNotBlank() }
        .joinToString(" ")

    return repository.searchBooksForResults(query = query, maxResults = 10)
        .mapCatching { result ->
            result.books
                .map { candidate -> candidate to deviceBookMatchScore(title, author, candidate) }
                .filter { (_, score) -> score >= DEVICE_BOOK_MIN_MATCH_SCORE }
                .maxByOrNull { (_, score) -> score }
                ?.first
                ?: throw IllegalStateException("No se encontro una coincidencia fiable en la API")
        }
        .map { libro -> libro.copy(pdf = file.uri.toString()) }
}

private fun deviceBookMatchScore(
    expectedTitle: String,
    expectedAuthor: String,
    candidate: Libro
): Int {
    val normalizedExpectedTitle = normalizeDeviceBookMatchText(expectedTitle)
    val normalizedCandidateTitle = normalizeDeviceBookMatchText(candidate.titulo)
    if (normalizedExpectedTitle.isBlank() || normalizedCandidateTitle.isBlank()) return 0

    val expectedTitleTokens = deviceBookMatchTokens(normalizedExpectedTitle)
    val candidateTitleTokens = deviceBookMatchTokens(normalizedCandidateTitle)
    val titleCoverage = tokenCoverage(expectedTitleTokens, candidateTitleTokens)
    val reverseTitleCoverage = tokenCoverage(candidateTitleTokens, expectedTitleTokens)
    val exactTitle = normalizedExpectedTitle == normalizedCandidateTitle
    val containedTitle = normalizedExpectedTitle.length >= 5 &&
        (normalizedCandidateTitle.contains(normalizedExpectedTitle) ||
            normalizedExpectedTitle.contains(normalizedCandidateTitle))

    var score = when {
        exactTitle -> 100
        containedTitle -> 78
        titleCoverage >= 0.8f && reverseTitleCoverage >= 0.55f -> 68
        titleCoverage >= 0.65f && reverseTitleCoverage >= 0.45f -> 52
        else -> return 0
    }

    val normalizedExpectedAuthor = normalizeDeviceBookMatchText(expectedAuthor)
    if (normalizedExpectedAuthor.isNotBlank()) {
        val normalizedCandidateAuthor = normalizeDeviceBookMatchText(candidate.autor)
        val authorCoverage = tokenCoverage(
            deviceBookMatchTokens(normalizedExpectedAuthor),
            deviceBookMatchTokens(normalizedCandidateAuthor)
        )
        score += when {
            normalizedCandidateAuthor == normalizedExpectedAuthor -> 35
            normalizedCandidateAuthor.contains(normalizedExpectedAuthor) ||
                normalizedExpectedAuthor.contains(normalizedCandidateAuthor) -> 28
            authorCoverage >= 0.6f -> 22
            exactTitle -> 0
            else -> -45
        }
    }

    return score
}

private fun normalizeDeviceBookMatchText(value: String): String {
    return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}

private fun deviceBookMatchTokens(value: String): Set<String> {
    return value.split(' ')
        .filter { it.length > 1 }
        .toSet()
}

private fun tokenCoverage(expected: Set<String>, candidate: Set<String>): Float {
    if (expected.isEmpty()) return 0f
    return expected.count { it in candidate }.toFloat() / expected.size
}

private const val DEVICE_BOOK_MIN_MATCH_SCORE = 60

@Composable
private fun DeviceLibraryFolderHintArrow(
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        val strokeWidth = with(density) { 4.dp.toPx() }
        val shadowStrokeWidth = with(density) { 7.dp.toPx() }
        val arrowHeadLength = with(density) { 17.dp.toPx() }
        val start = Offset(
            x = size.width - with(density) { 170.dp.toPx() },
            y = with(density) { 132.dp.toPx() }
        )
        val end = Offset(
            x = size.width - with(density) { 54.dp.toPx() },
            y = with(density) { 67.dp.toPx() }
        )
        val angle = atan2(end.y - start.y, end.x - start.x)
        val headAngle = 0.58f
        val leftHead = Offset(
            x = end.x + arrowHeadLength * cos(angle + PI.toFloat() - headAngle),
            y = end.y + arrowHeadLength * sin(angle + PI.toFloat() - headAngle)
        )
        val rightHead = Offset(
            x = end.x + arrowHeadLength * cos(angle + PI.toFloat() + headAngle),
            y = end.y + arrowHeadLength * sin(angle + PI.toFloat() + headAngle)
        )
        val shadowOffset = Offset(1.5.dp.toPx(), 2.dp.toPx())
        val shadowColor = Obsidian.copy(alpha = 0.78f)
        val arrowColor = TarnishedGold.copy(alpha = 0.95f)

        drawLine(
            color = shadowColor,
            start = start + shadowOffset,
            end = end + shadowOffset,
            strokeWidth = shadowStrokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = shadowColor,
            start = end + shadowOffset,
            end = leftHead + shadowOffset,
            strokeWidth = shadowStrokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = shadowColor,
            start = end + shadowOffset,
            end = rightHead + shadowOffset,
            strokeWidth = shadowStrokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = arrowColor,
            start = start,
            end = end,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = arrowColor,
            start = end,
            end = leftHead,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = arrowColor,
            start = end,
            end = rightHead,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun DeviceLibraryTopBar(
    onOpenMenu: () -> Unit,
    fileCount: Int,
    totalFileCount: Int,
    selectedAuthor: String?,
    authorGroups: List<DeviceLibraryAuthorGroup>,
    showSearchPanel: Boolean,
    onShowSearchPanel: () -> Unit,
    onDismissSearchPanel: () -> Unit,
    onToggleFilterPanel: () -> Unit,
    onDismissFilterPanel: () -> Unit,
    // onQueryChange: (String) -> Unit,
    onAllBooksSelected: () -> Unit,
    onAuthorSelected: (String) -> Unit,
    onChooseFolder: () -> Unit,
    onImportBooks: () -> Unit,
    onDefaultCover: () -> Unit,
    onRebuildBookCovers: () -> Unit,
    onOpenRandomBook: () -> Unit,
    onHeightChanged: (Int) -> Unit
) {
    var showLibraryMenu by remember { mutableStateOf(false) }
    var showTopBarOptions by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { onHeightChanged(it.height) }
            .statusBarsPadding()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DeepWalnut.copy(alpha = 0.98f),
                        Obsidian.copy(alpha = 0.96f)
                    )
                )
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenMenu) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(R.string.open_navigation_menu),
                    tint = TarnishedGold
                )
            }

            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium,
                color = TarnishedGold
            )

            DeviceLibraryFolderButton(
                fileCount = fileCount,
                onClick = onChooseFolder
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        showLibraryMenu = !showLibraryMenu
                        showTopBarOptions = false
                        onDismissSearchPanel()
                        onDismissFilterPanel()
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                text = selectedAuthor ?: stringResource(R.string.device_library_all_books),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = OldIvory,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = OldIvory,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = {
                    onShowSearchPanel()
                    showLibraryMenu = false
                    showTopBarOptions = false
                    onDismissFilterPanel()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.search),
                    tint = OldIvory
                )
            }

            IconButton(
                onClick = {
                    onDismissSearchPanel()
                    showLibraryMenu = false
                    showTopBarOptions = false
                    onToggleFilterPanel()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FilterAlt,
                    contentDescription = stringResource(R.string.device_library_filter),
                    tint = OldIvory
                )
            }

            Box {
                IconButton(
                    onClick = {
                        showTopBarOptions = true
                        showLibraryMenu = false
                        onDismissSearchPanel()
                        onDismissFilterPanel()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.more_option),
                        tint = OldIvory
                    )
                }

                DeviceLibraryTopBarOptionsMenu(
                    expanded = showTopBarOptions,
                    onDismiss = { showTopBarOptions = false },
                    onImportBooks = {
                        showTopBarOptions = false
                        onImportBooks()
                    },
                    onDefaultCover = {
                        showTopBarOptions = false
                        onDefaultCover()
                    },
                    onRebuildBookCovers = {
                        showTopBarOptions = false
                        onRebuildBookCovers()
                    },
                    onOpenRandomBook = {
                        showTopBarOptions = false
                        onOpenRandomBook()
                    }
                )
            }
        }

        when {
            showLibraryMenu -> LibrarySelectorMenu(
                fileCount = totalFileCount,
                authorGroups = authorGroups,
                selectedAuthor = selectedAuthor,
                onAllBooksSelected = {
                    onAllBooksSelected()
                    showLibraryMenu = false
                },
                onAuthorSelected = { author ->
                    onAuthorSelected(author)
                    showLibraryMenu = false
                }
            )
        }
    }
}

@Composable
private fun DeviceLibraryFolderButton(
    fileCount: Int,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = stringResource(R.string.device_library_choose_folder),
                tint = TarnishedGold,
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text = fileCount.coerceAtMost(999).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Obsidian,
                maxLines = 1,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .widthIn(max = 26.dp),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun DeviceLibraryTopBarOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onImportBooks: () -> Unit,
    onDefaultCover: () -> Unit,
    onRebuildBookCovers: () -> Unit,
    onOpenRandomBook: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 236.dp, max = 292.dp),
        shape = RoundedCornerShape(2.dp),
        containerColor = Color(0xFF262626),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        DeviceLibraryBookOptionItem(
            text = stringResource(R.string.device_library_import_books),
            onClick = onImportBooks
        )
        DeviceLibraryBookOptionItem(
            text = stringResource(R.string.device_library_default_cover),
            onClick = onDefaultCover
        )
        DeviceLibraryBookOptionItem(
            text = stringResource(R.string.device_library_rebuild_covers),
            onClick = onRebuildBookCovers
        )
        DeviceLibraryBookOptionItem(
            text = stringResource(R.string.device_library_open_random_book),
            onClick = onOpenRandomBook
        )
        // DeviceLibraryBookOptionItem(
        //     text = "Seleccionar todo",
        //     onClick = onDismiss
        // )
    }
}

private suspend fun rebuildMissingDeviceBookCovers(
    context: Context,
    files: List<DeviceLibraryFile>,
    fileMetadata: Map<String, DeviceLibraryResolvedMetadata>,
    backgroundTreeUri: Uri?
): Int {
    var rebuiltCount = 0
    files.forEach { file ->
        val currentMetadata = readDeviceBookUserMetadata(context, file)
        if (currentMetadata.coverId.isNotBlank() && canOpenSavedBookCover(context, currentMetadata.coverId)) {
            return@forEach
        }

        val resolvedMetadata = fileMetadata[file.uri.toString()]
        val title = currentMetadata.title
            .ifBlank { resolvedMetadata?.title.orEmpty() }
            .ifBlank { file.name.substringBeforeLast('.') }
            .trim()
        val author = currentMetadata.author
            .ifBlank { resolvedMetadata?.author.orEmpty() }
            .trim()
        if (title.isBlank()) return@forEach

        val imageBytes = findBookCoverImageBytes(title, author) ?: return@forEach
        val savedCover = saveDownloadedBookCover(context, imageBytes, backgroundTreeUri) ?: return@forEach
        saveDeviceBookUserMetadata(
            context = context,
            file = file,
            metadata = currentMetadata.copy(coverId = savedCover.id)
        )
        rebuiltCount++
    }
    return rebuiltCount
}

private fun canOpenSavedBookCover(
    context: Context,
    coverId: String
): Boolean {
    val cover = findDefaultCoverOption(context, coverId) ?: return false
    return when {
        cover.resourceId != null -> true
        cover.file != null -> BitmapFactory.decodeFile(cover.file.absolutePath) != null
        cover.uri != null -> runCatching {
            context.contentResolver.openInputStream(cover.uri)?.use(BitmapFactory::decodeStream) != null
        }.getOrDefault(false)
        else -> false
    }
}

private fun findBookCoverImageBytes(
    title: String,
    author: String
): ByteArray? {
    val query = listOf(title, author, "book cover")
        .filter { it.isNotBlank() }
        .joinToString(" ")
    return findDuckDuckGoImageUrls(query)
        .asSequence()
        .mapNotNull(::downloadValidImageBytes)
        .firstOrNull()
}

private fun findDuckDuckGoImageUrls(query: String): List<String> {
    return runCatching {
        val searchPage = readUrlText("https://duckduckgo.com/?q=${query.urlEncoded()}&iax=images&ia=images")
        val vqd = Regex("""vqd=['"]([^'"]+)['"]""")
            .find(searchPage)
            ?.groupValues
            ?.getOrNull(1)
            ?: return@runCatching emptyList()
        val json = readUrlText("https://duckduckgo.com/i.js?l=wt-wt&o=json&q=${query.urlEncoded()}&vqd=$vqd")
        val results = JSONObject(json).optJSONArray("results") ?: return@runCatching emptyList()
        buildList {
            for (index in 0 until results.length()) {
                val imageUrl = results.optJSONObject(index)?.optString("image").orEmpty()
                if (imageUrl.startsWith("http")) add(imageUrl)
            }
        }
    }.getOrDefault(emptyList())
}

private fun downloadValidImageBytes(imageUrl: String): ByteArray? {
    return runCatching {
        val bytes = readUrlBytes(imageUrl)
        if (bytes.size > 1_500 && BitmapFactory.decodeByteArray(bytes, 0, bytes.size) != null) {
            bytes
        } else {
            null
        }
    }.getOrNull()
}

private fun saveDownloadedBookCover(
    context: Context,
    bytes: ByteArray,
    backgroundTreeUri: Uri?
): DefaultCoverOption? {
    val fileName = "download_${System.currentTimeMillis()}.jpg"
    backgroundTreeUri?.let { treeUri ->
        return createImageBytesInTree(context, treeUri, fileName, bytes)
    }
    val target = File(defaultCoversDir(context), fileName)
    return runCatching {
        target.outputStream().use { output -> output.write(bytes) }
        DefaultCoverOption(id = target.absolutePath, file = target, displayName = target.name)
    }.getOrNull()
}

private fun readUrlText(url: String): String {
    return readUrlBytes(url).toString(Charsets.UTF_8)
}

private fun readUrlBytes(url: String): ByteArray {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 12_000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "Mozilla/5.0")
        setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
    }
    return connection.inputStream.use { it.readBytes() }
}

private fun String.urlEncoded(): String {
    return java.net.URLEncoder.encode(this, "UTF-8")
}

private fun saveLastOpenedDeviceBookUri(
    context: Context,
    uri: String
) {
    context.getSharedPreferences(DEVICE_LIBRARY_LAST_READING_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(DEVICE_LIBRARY_LAST_READING_URI_KEY, uri)
        .apply()
}

private fun readLastOpenedDeviceBookUri(context: Context): String? {
    return context.getSharedPreferences(DEVICE_LIBRARY_LAST_READING_PREFS, Context.MODE_PRIVATE)
        .getString(DEVICE_LIBRARY_LAST_READING_URI_KEY, null)
        ?.takeIf { it.isNotBlank() }
}

private const val DEVICE_LIBRARY_LAST_READING_PREFS = "device_library_last_reading"
private const val DEVICE_LIBRARY_LAST_READING_URI_KEY = "last_opened_uri"


