package com.example.kaishelvesapp.ui.screen.library

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.text.format.Formatter
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.repository.DeviceLibraryFile
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiNavigationDrawerContent
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.DeviceLibraryViewModel
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.net.URL
import java.net.URLEncoder

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

fun DeviceLibraryFile.matchesFileTypeFilters(selectedTypes: Set<DeviceLibraryFileTypeFilter>): Boolean {
    if (selectedTypes.isEmpty()) return true
    val extension = name.substringAfterLast('.', missingDelimiterValue = "").lowercase(Locale.ROOT)
    return selectedTypes.any { filter -> extension in filter.extensions }
}

@Composable
fun DeviceLibraryScreen(
    userName: String?,
    profileImageUrl: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScanResult: (String) -> Unit,
    onGoToProfile: () -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit,
    pendingRequestCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onSectionSelected: (KaiSection) -> Unit,
    openBookUri: String? = null,
    onOpenBookUriConsumed: () -> Unit = {},
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
    var topBarHeight by remember { mutableStateOf(0.dp) }
    val fileMetadata by produceState<Map<String, DeviceBookDisplayMetadata>>(
        initialValue = emptyMap(),
        uiState.filteredFiles
    ) {
        value = withContext(Dispatchers.IO) {
            uiState.filteredFiles
                .filter { isEpub(it) }
                .associate { file ->
                    file.uri.toString() to (extractEpubDisplayMetadata(context, file.uri) ?: DeviceBookDisplayMetadata())
                }
        }
    }
    val sortedFiles = remember(
        uiState.filteredFiles,
        fileMetadata,
        sortOption,
        sortDescending,
        selectedFileTypes,
        selectedReadingStatuses,
        progressRevision
    ) {
        val formatFilteredFiles = uiState.filteredFiles
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

    LaunchedEffect(openBookUri, uiState.files, uiState.isLoading) {
        val requestedUri = openBookUri?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val targetFile = uiState.files.firstOrNull { it.uri.toString() == requestedUri }
        if (targetFile != null) {
            readerFile = targetFile
            onOpenBookUriConsumed()
        } else if (!uiState.isLoading && uiState.selectedFolderUri != null) {
            onOpenBookUriConsumed()
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) viewModel.useFolder(uri)
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
                        showSearchPanel = showSearchPanel,
                        onShowSearchPanel = { showSearchPanel = true },
                        onDismissSearchPanel = { showSearchPanel = false },
                        onToggleFilterPanel = { showFilterPanel = !showFilterPanel },
                        onDismissFilterPanel = { showFilterPanel = false },
                        onQueryChange = viewModel::onSearchQueryChange,
                        onChooseFolder = { folderLauncher.launch(null) },
                        onImportBooks = { showImportBooksDialog = true },
                        onDefaultCover = { showDefaultCoverScreen = true },
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
                            Toast.makeText(
                                context,
                                "Lectura actual: proximamente",
                                Toast.LENGTH_SHORT
                            ).show()
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
                    layoutMode = layoutMode,
                    progressRevision = progressRevision,
                    onOpenFile = { file -> readerFile = file }
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
                    onDismiss = { readerFile = null }
                )
            }
        }
    }
}

@Composable
private fun DeviceLibraryTopBar(
    onOpenMenu: () -> Unit,
    fileCount: Int,
    showSearchPanel: Boolean,
    onShowSearchPanel: () -> Unit,
    onDismissSearchPanel: () -> Unit,
    onToggleFilterPanel: () -> Unit,
    onDismissFilterPanel: () -> Unit,
    onQueryChange: (String) -> Unit,
    onChooseFolder: () -> Unit,
    onImportBooks: () -> Unit,
    onDefaultCover: () -> Unit,
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
                    text = "Todos los libros",
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
                    }
                )
            }
        }

        when {
            showLibraryMenu -> LibrarySelectorMenu(fileCount = fileCount)
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
    onDefaultCover: () -> Unit
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
            onClick = onDismiss
        )
        DeviceLibraryBookOptionItem(
            text = "Abre un libro al azar",
            onClick = onDismiss
        )
        DeviceLibraryBookOptionItem(
            text = "Seleccionar todo",
            onClick = onDismiss
        )
    }
}



