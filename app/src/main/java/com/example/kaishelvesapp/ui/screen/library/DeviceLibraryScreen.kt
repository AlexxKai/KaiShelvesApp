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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.kaishelvesapp.data.repository.DeviceLibraryFile
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiNavigationDrawerContent
import com.example.kaishelvesapp.ui.components.KaiSection
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

    LaunchedEffect(openBookUri, uiState.files, uiState.isLoading, uiState.hasLoadedFiles) {
        val requestedUri = openBookUri?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val targetFile = uiState.files.firstOrNull { it.uri.toString() == requestedUri }
        if (targetFile != null) {
            saveLastOpenedDeviceBookUri(context, targetFile.uri.toString())
            readerFile = targetFile
            onOpenBookUriConsumed()
        } else if (uiState.hasLoadedFiles && !uiState.isLoading) {
            onOpenBookUriConsumed()
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) viewModel.useFolder(uri)
    }
    fun openReader(file: DeviceLibraryFile) {
        saveLastOpenedDeviceBookUri(context, file.uri.toString())
        readerFile = file
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
                                    "Reconstruyendo portadas...",
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
                                            "Portadas reconstruidas: $rebuiltCount"
                                        } else {
                                            "No se encontraron portadas nuevas"
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
                                    "No hay libros pendientes para abrir al azar",
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
                            "Importación configurada",
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
                        readerFile = null
                        onReaderClosed()
                    }
                )
            }
        }
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
                    text = selectedAuthor ?: "Todos los libros",
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
                    contentDescription = "Filtro",
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
            text = "Importar libros",
            onClick = onImportBooks
        )
        DeviceLibraryBookOptionItem(
            text = "Cubierta por defecto",
            onClick = onDefaultCover
        )
        DeviceLibraryBookOptionItem(
            text = "Reconstruir portadas de libros",
            onClick = onRebuildBookCovers
        )
        DeviceLibraryBookOptionItem(
            text = "Abre un libro al azar",
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


