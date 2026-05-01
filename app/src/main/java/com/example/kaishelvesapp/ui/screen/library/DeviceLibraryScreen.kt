package com.example.kaishelvesapp.ui.screen.library

import android.content.ActivityNotFoundException
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

private enum class DeviceLibraryLayoutMode {
    List,
    Grid,
    Carousel
}

private enum class DeviceLibrarySortOption {
    Title,
    Author,
    Recent,
    Folder,
    RecentList
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
    viewModel: DeviceLibraryViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerExpanded = drawerState.targetValue == DrawerValue.Open || drawerState.currentValue == DrawerValue.Open
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var layoutMode by remember { mutableStateOf(DeviceLibraryLayoutMode.List) }
    var sortOption by remember { mutableStateOf(DeviceLibrarySortOption.Title) }
    var sortDescending by remember { mutableStateOf(false) }
    var showSearchPanel by remember { mutableStateOf(false) }
    var showFilterPanel by remember { mutableStateOf(false) }
    var showImportBooksDialog by remember { mutableStateOf(false) }
    var showDefaultCoverScreen by remember { mutableStateOf(false) }
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
    val sortedFiles = remember(uiState.filteredFiles, fileMetadata, sortOption, sortDescending) {
        val sorted = when (sortOption) {
            DeviceLibrarySortOption.Title -> uiState.filteredFiles.sortedBy { file ->
                fileMetadata[file.uri.toString()]?.title?.takeIf { it.isNotBlank() }?.lowercase(Locale.ROOT)
                    ?: file.name.substringBeforeLast('.').lowercase(Locale.ROOT)
            }
            DeviceLibrarySortOption.Author -> uiState.filteredFiles.sortedBy { file ->
                fileMetadata[file.uri.toString()]?.author?.takeIf { it.isNotBlank() }?.lowercase(Locale.ROOT)
                    ?: file.name.substringBeforeLast('.').lowercase(Locale.ROOT)
            }
            DeviceLibrarySortOption.Recent -> uiState.filteredFiles.sortedBy { it.modifiedAtMillis ?: 0L }
            DeviceLibrarySortOption.Folder -> uiState.filteredFiles.sortedWith(
                compareBy<DeviceLibraryFile> { it.location.lowercase(Locale.ROOT) }
                    .thenBy { it.name.substringBeforeLast('.').lowercase(Locale.ROOT) }
            )
            DeviceLibrarySortOption.RecentList -> uiState.filteredFiles.sortedBy { it.uri.toString() }
        }
        if (sortDescending) sorted.asReversed() else sorted
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) viewModel.useFolder(uri)
    }

    fun openFile(file: DeviceLibraryFile) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(file.uri, file.mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_file)))
        }.onFailure { throwable ->
            val message = if (throwable is ActivityNotFoundException) {
                R.string.no_app_to_open_file
            } else {
                R.string.could_not_open_file
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
                    onOpenFile = ::openFile
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
                    onLayoutModeChange = { layoutMode = it },
                    onSortOptionChange = { sortOption = it },
                    onToggleSortDirection = { sortDescending = !sortDescending },
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
        modifier = Modifier.widthIn(min = 300.dp, max = 390.dp),
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

private data class DefaultCoverOption(
    val id: String,
    val resourceId: Int? = null,
    val file: File? = null,
    val uri: Uri? = null,
    val displayName: String? = null
) {
    val isUserAdded: Boolean
        get() = resourceId == null

    val isDownloaded: Boolean
        get() = displayName.orEmpty().startsWith("download_", ignoreCase = true)
}

private val BuiltInDefaultCoverOptions: List<DefaultCoverOption> by lazy {
    R.drawable::class.java.fields
        .mapNotNull { field ->
            val name = field.name
            if (!isDefaultCoverResourceName(name)) return@mapNotNull null
            DefaultCoverOption(id = name, resourceId = field.getInt(null))
        }
        .sortedBy { it.id.lowercase(Locale.ROOT) }
}

private var selectedDefaultCoverIdState by mutableStateOf<String?>(null)

@Composable
private fun DefaultCoverScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedCoverId by remember { mutableStateOf(readDefaultCoverId(context)) }
    var backgroundTreeUriText by remember { mutableStateOf(readDefaultCoverStorageTreeUri(context)) }
    val backgroundTreeUri = remember(backgroundTreeUriText) { backgroundTreeUriText?.let(Uri::parse) }
    var downloadedCovers by remember(backgroundTreeUriText) {
        mutableStateOf(loadDownloadedDefaultCovers(context, backgroundTreeUri))
    }
    var showDownloadWindow by remember { mutableStateOf(false) }
    var openDownloadAfterFolderSelection by remember { mutableStateOf(false) }
    var coverToRename by remember { mutableStateOf<DefaultCoverOption?>(null) }
    val displayedBackgroundPath = remember(backgroundTreeUriText) {
        backgroundTreeUri?.let(::readableImportRootPath) ?: "/sdcard/backgrounds"
    }
    val backgroundFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            backgroundTreeUriText = uri.toString()
            saveDefaultCoverStorageTreeUri(context, uri)
            downloadedCovers = loadDownloadedDefaultCovers(context, uri)
            if (openDownloadAfterFolderSelection) {
                openDownloadAfterFolderSelection = false
                showDownloadWindow = true
            }
        }
    }
    val albumLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val savedCovers = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        saveImageUriToDefaultCovers(context, uri, backgroundTreeUri)
                    }
                }
                if (savedCovers.isNotEmpty()) {
                    downloadedCovers = loadDownloadedDefaultCovers(context, backgroundTreeUri)
                    selectedCoverId = savedCovers.last().id
                    saveDefaultCoverId(context, selectedCoverId)
                }
            }
        }
    }
    val allCovers = remember(downloadedCovers) {
        BuiltInDefaultCoverOptions + downloadedCovers
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .zIndex(20f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(Color(0xFF171717))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = OldIvory
                    )
                }
                Text(
                    text = "Cubierta por defecto",
                    style = MaterialTheme.typography.titleLarge,
                    color = OldIvory,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 86.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(allCovers, key = { it.id }) { cover ->
                    DefaultCoverTile(
                        cover = cover,
                        selected = cover.id == selectedCoverId,
                        onClick = {
                            selectedCoverId = cover.id
                            saveDefaultCoverId(context, cover.id)
                        },
                        onRename = { coverToRename = cover },
                        onDelete = {
                            scope.launch {
                                val deleted = withContext(Dispatchers.IO) {
                                    deleteDefaultCover(context, cover)
                                }
                                if (deleted) {
                                    if (selectedCoverId == cover.id) {
                                        selectedCoverId = BuiltInDefaultCoverOptions.firstOrNull()?.id.orEmpty()
                                        saveDefaultCoverId(context, selectedCoverId)
                                    }
                                    downloadedCovers = loadDownloadedDefaultCovers(context, backgroundTreeUri)
                                }
                            }
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xFF1C1C1C))
                .navigationBarsPadding()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextButton(
                onClick = {
                    if (backgroundTreeUri == null) {
                        openDownloadAfterFolderSelection = true
                        backgroundFolderLauncher.launch(null)
                    } else {
                        showDownloadWindow = true
                    }
                },
                modifier = Modifier.border(1.dp, OldIvory.copy(alpha = 0.72f), RoundedCornerShape(2.dp))
            ) {
                Text(text = "Descargar", color = OldIvory)
            }
            TextButton(
                onClick = { albumLauncher.launch("image/*") },
                modifier = Modifier.border(1.dp, OldIvory.copy(alpha = 0.72f), RoundedCornerShape(2.dp))
            ) {
                Text(text = "Álbum", color = OldIvory)
            }
            Text(
                text = backgroundTreeUri?.let { displayedBackgroundPath } ?: "Seleccionar carpeta",
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(2.dp))
                    .clickable { backgroundFolderLauncher.launch(null) }
                    .padding(horizontal = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = "Elegir carpeta de fondos",
                tint = OldIvory,
                modifier = Modifier
                    .size(30.dp)
                    .clickable { backgroundFolderLauncher.launch(null) }
            )
        }
    }

    if (showDownloadWindow) {
        BackgroundImageSearchDialog(
            backgroundTreeUri = backgroundTreeUri,
            onDismiss = { showDownloadWindow = false },
            onImageSaved = { savedCover ->
                downloadedCovers = loadDownloadedDefaultCovers(context, backgroundTreeUri)
                selectedCoverId = savedCover.id
                saveDefaultCoverId(context, selectedCoverId)
                showDownloadWindow = false
            }
        )
    }

    coverToRename?.let { cover ->
        RenameDefaultCoverDialog(
            cover = cover,
            onDismiss = { coverToRename = null },
            onRename = { newName ->
                scope.launch {
                    val renamedCover = withContext(Dispatchers.IO) {
                        renameDefaultCover(context, cover, newName)
                    }
                    if (renamedCover != null) {
                        if (selectedCoverId == cover.id) {
                            selectedCoverId = renamedCover.id
                            saveDefaultCoverId(context, selectedCoverId)
                        }
                        downloadedCovers = loadDownloadedDefaultCovers(context, backgroundTreeUri)
                    }
                    coverToRename = null
                }
            }
        )
    }
}

@Composable
private fun DefaultCoverTile(
    cover: DefaultCoverOption,
    selected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) Color(0xFF5AA7E8) else Color.Black
            )
    ) {
        when {
            cover.resourceId != null -> Image(
                painter = painterResource(cover.resourceId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            cover.file != null -> {
                val bitmap by produceState<Bitmap?>(initialValue = null, cover.file) {
                    value = withContext(Dispatchers.IO) {
                        BitmapFactory.decodeFile(cover.file.absolutePath)
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            cover.uri != null -> {
                val context = LocalContext.current
                val bitmap by produceState<Bitmap?>(initialValue = null, cover.uri) {
                    value = withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver.openInputStream(cover.uri)?.use(BitmapFactory::decodeStream)
                        }.getOrNull()
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        if (cover.isDownloaded) {
            Text(
                text = cover.displayName.orEmpty().substringBeforeLast('.'),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.38f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = OldIvory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        if (cover.isUserAdded) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(3.dp)
            ) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.more_option),
                        tint = OldIvory,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = Color(0xFF262626)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Renombrar",
                                color = OldIvory,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Borrar",
                                color = OldIvory,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RenameDefaultCoverDialog(
    cover: DefaultCoverOption,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    val currentName = cover.displayName
        ?: cover.file?.name
        ?: "cubierta"
    var newName by remember(cover.id) { mutableStateOf(currentName.substringBeforeLast('.')) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1C))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Renombrar",
                    style = MaterialTheme.typography.titleLarge,
                    color = OldIvory
                )
                ImportDialogTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Cancelar", color = OldIvory)
                    }
                    TextButton(
                        onClick = { onRename(newName) },
                        enabled = newName.isNotBlank()
                    ) {
                        Text(text = "Aceptar", color = OldIvory)
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundImageSearchDialog(
    backgroundTreeUri: Uri?,
    onDismiss: () -> Unit,
    onImageSaved: (DefaultCoverOption) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val selectedImagePreview by produceState<Bitmap?>(initialValue = null, selectedImageUrl) {
        value = withContext(Dispatchers.IO) {
            selectedImageUrl?.let { url ->
                runCatching {
                    URL(url).openStream().use(BitmapFactory::decodeStream)
                }.getOrNull()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .background(Color(0xFF171717))
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = OldIvory
                        )
                    }
                    Text(
                        text = "Imagen de fondo",
                        style = MaterialTheme.typography.titleLarge,
                        color = OldIvory,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AndroidView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    factory = { viewContext ->
                        WebView(viewContext).apply {
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            setOnLongClickListener {
                                val hit = hitTestResult
                                val url = hit.extra
                                if (
                                    url != null &&
                                    (hit.type == WebView.HitTestResult.IMAGE_TYPE ||
                                        hit.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE)
                                ) {
                                    selectedImageUrl = url
                                    true
                                } else {
                                    false
                                }
                            }
                            loadUrl("https://www.google.com/search?tbm=isch&q=Imagen%20de%20fondo")
                        }
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFD9D9D9))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedImagePreview != null) {
                        Image(
                            bitmap = selectedImagePreview!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(width = 62.dp, height = 46.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(
                        text = selectedImageUrl?.let { "Imagen seleccionada" }
                            ?: "Consejo: Realice una pulsación larga para seleccionar una imagen",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5F5F5F),
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .navigationBarsPadding()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF242424), RoundedCornerShape(6.dp))
                    ) {
                        Text(text = "Cancelar", color = OldIvory)
                    }
                    TextButton(
                        enabled = selectedImageUrl != null && !isSaving,
                        onClick = {
                            val url = selectedImageUrl ?: return@TextButton
                            isSaving = true
                            scope.launch {
                                val savedCover = withContext(Dispatchers.IO) {
                                    downloadImageToDefaultCovers(context, url, backgroundTreeUri)
                                }
                                isSaving = false
                                if (savedCover != null) {
                                    onImageSaved(savedCover)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "No se pudo guardar la imagen",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF242424), RoundedCornerShape(6.dp))
                    ) {
                        Text(text = if (isSaving) "Guardando" else "Aceptar", color = OldIvory)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportBooksDialog(
    initialTreeUri: Uri?,
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    val context = LocalContext.current
    val fileTypeRows = remember {
        listOf(
            "EPUB" to "PDF/DJVU",
            "FB2" to "MOBI/AZW3/PRC",
            "CHM/UMD" to "DOCX/ODT/RTF",
            "TXT/MD" to "HTML/MHTML",
            "CBZ/CBR" to null
        )
    }
    val categories = remember {
        listOf(
            "Categoría (opcional)",
            "Sin categoría",
            "Leyendo",
            "Pendientes",
            "Leídos",
            "Favoritos"
        )
    }
    var importTreeUri by remember(initialTreeUri) { mutableStateOf(initialTreeUri) }
    var folderPath by remember(initialTreeUri) {
        mutableStateOf(initialTreeUri?.let(::readableImportRootPath) ?: "/sdcard/Ac ebooks")
    }
    var showAdvancedOptions by remember { mutableStateOf(false) }
    var showFolderBrowser by remember { mutableStateOf(false) }
    var selectedFileTypes by remember {
        mutableStateOf(fileTypeRows.flatMap { listOfNotNull(it.first, it.second) }.toSet())
    }
    var minimumSizeKb by remember { mutableStateOf("1") }
    var favorite by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    val importFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            importTreeUri = uri
            folderPath = readableImportRootPath(uri)
            showFolderBrowser = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 390.dp),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1C)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Importar libros",
                    style = MaterialTheme.typography.titleLarge,
                    color = OldIvory,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ImportDialogPathSelector(
                        value = folderPath,
                        onClick = {
                            if (importTreeUri == null) {
                                importFolderLauncher.launch(null)
                            } else {
                                showFolderBrowser = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ImportDialogArrowButton(
                        expanded = showAdvancedOptions,
                        onClick = { showAdvancedOptions = !showAdvancedOptions },
                        contentDescription = "Mostrar opciones de importación"
                    )
                }

                if (showAdvancedOptions) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        fileTypeRows.forEach { (leftType, rightType) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ImportOptionCheckbox(
                                    text = leftType,
                                    checked = leftType in selectedFileTypes,
                                    onCheckedChange = { checked ->
                                        selectedFileTypes = selectedFileTypes.toggleItem(leftType, checked)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                if (rightType != null) {
                                    ImportOptionCheckbox(
                                        text = rightType,
                                        checked = rightType in selectedFileTypes,
                                        onCheckedChange = { checked ->
                                            selectedFileTypes = selectedFileTypes.toggleItem(rightType, checked)
                                        },
                                        modifier = Modifier.weight(1.35f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1.35f))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Tamaño del archivo >",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OldIvory
                            )
                            ImportDialogTextField(
                                value = minimumSizeKb,
                                onValueChange = { newValue ->
                                    minimumSizeKb = newValue.filter(Char::isDigit).ifBlank { "0" }
                                },
                                modifier = Modifier.width(56.dp),
                                keyboardType = KeyboardType.Number
                            )
                            Text(
                                text = "KB",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OldIvory
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ImportOptionCheckbox(
                                text = "Favorito",
                                checked = favorite,
                                onCheckedChange = { favorite = it },
                                modifier = Modifier.weight(1f)
                            )

                            Box(modifier = Modifier.weight(1.7f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ImportDialogTextField(
                                        value = selectedCategory,
                                        onValueChange = { selectedCategory = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ImportDialogArrowButton(
                                        expanded = showCategoryMenu,
                                        onClick = { showCategoryMenu = !showCategoryMenu },
                                        contentDescription = "Seleccionar categoría"
                                    )
                                }
                                DropdownMenu(
                                    expanded = showCategoryMenu,
                                    onDismissRequest = { showCategoryMenu = false },
                                    modifier = Modifier.widthIn(min = 190.dp),
                                    containerColor = Color(0xFF262626)
                                ) {
                                    categories.forEach { category ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = category,
                                                    color = OldIvory,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            onClick = {
                                                selectedCategory = category
                                                showCategoryMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "CANCELAR", color = OldIvory)
                    }
                    TextButton(onClick = onAccept) {
                        Text(text = "ACEPTAR", color = OldIvory)
                    }
                }
            }
        }
    }

    if (showFolderBrowser) {
        val treeUri = importTreeUri
        if (treeUri != null) {
            ImportFolderBrowserDialog(
                treeUri = treeUri,
                initialPath = folderPath,
                onChooseDifferentRoot = { importFolderLauncher.launch(null) },
                onDismiss = { showFolderBrowser = false },
                onFolderSelected = { path ->
                    folderPath = path
                    showFolderBrowser = false
                }
            )
        }
    }
}

@Composable
private fun ImportDialogPathSelector(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = OldIvory,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        HorizontalDivider(color = Color(0xFF348CD6), thickness = 1.dp)
    }
}

@Composable
private fun ImportFolderBrowserDialog(
    treeUri: Uri,
    initialPath: String,
    onChooseDifferentRoot: () -> Unit,
    onDismiss: () -> Unit,
    onFolderSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val rootDocumentId = remember(treeUri) { DocumentsContract.getTreeDocumentId(treeUri) }
    var currentDocumentId by remember(treeUri) { mutableStateOf(rootDocumentId) }
    var currentPath by remember(treeUri, initialPath) { mutableStateOf(readableImportRootPath(treeUri)) }
    val entriesState by produceState<Result<List<ImportBrowserEntry>>>(
        initialValue = Result.success(emptyList()),
        treeUri,
        currentDocumentId
    ) {
        value = runCatching {
            withContext(Dispatchers.IO) {
                loadImportBrowserEntries(context, treeUri, currentDocumentId, currentPath)
            }
        }
    }
    val entries = entriesState.getOrDefault(emptyList())
    val parentPath = currentPath.substringBeforeLast('/', missingDelimiterValue = currentPath)
    val canGoBack = currentDocumentId != rootDocumentId

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.96f),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFF171717))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = {
                        if (canGoBack) {
                            currentDocumentId = currentDocumentId.substringBeforeLast('/')
                            currentPath = parentPath
                        } else {
                            onDismiss()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = OldIvory
                        )
                    }
                    Text(
                        text = "Importar libros",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = OldIvory,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onChooseDifferentRoot) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = "Elegir otra carpeta",
                            tint = OldIvory
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(Color(0xFF202020))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    currentPath.split('/')
                        .filter { it.isNotBlank() }
                        .forEachIndexed { index, segment ->
                            Text(
                                text = if (index == 0) "/$segment" else segment,
                                style = MaterialTheme.typography.labelLarge,
                                color = OldIvory,
                                maxLines = 1
                            )
                            if (index < currentPath.split('/').filter { it.isNotBlank() }.lastIndex) {
                                Text(text = ">", color = OldIvory.copy(alpha = 0.45f))
                            }
                        }
                }

                if (entriesState.isFailure) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DeviceLibraryMessage(
                            title = "No se pudo abrir la carpeta",
                            body = entriesState.exceptionOrNull()?.localizedMessage.orEmpty()
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        items(entries) { entry ->
                            ImportBrowserEntryRow(
                                entry = entry,
                                onClick = {
                                    if (entry.isDirectory) {
                                        currentDocumentId = entry.documentId
                                        currentPath = entry.path
                                    }
                                }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1D1D1D))
                        .navigationBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Ruta: $currentPath",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OldIvory,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF2B2B2B), RoundedCornerShape(6.dp))
                        ) {
                            Text(text = "Cancelar", color = OldIvory)
                        }
                        TextButton(
                            onClick = { onFolderSelected(currentPath) },
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF2B2B2B), RoundedCornerShape(6.dp))
                        ) {
                            Text(text = "Aceptar", color = OldIvory)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportBrowserEntryRow(
    entry: ImportBrowserEntry,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = entry.isDirectory, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(50))
                .background(if (entry.isDirectory) TarnishedGold else importFileAccent(entry.name)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (entry.isDirectory) Icons.Filled.Folder else Icons.AutoMirrored.Filled.Article,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                color = OldIvory,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!entry.isDirectory && entry.sizeBytes != null) {
                Text(
                    text = Formatter.formatShortFileSize(LocalContext.current, entry.sizeBytes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldIvory.copy(alpha = 0.58f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ImportDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = OldIvory),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done
        ),
        colors = TextFieldDefaults.colors(
            focusedTextColor = OldIvory,
            unfocusedTextColor = OldIvory,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            cursorColor = Color(0xFF5AA7E8),
            focusedIndicatorColor = Color(0xFF348CD6),
            unfocusedIndicatorColor = Color(0xFF348CD6)
        )
    )
}

@Composable
private fun ImportDialogArrowButton(
    expanded: Boolean,
    onClick: () -> Unit,
    contentDescription: String
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(34.dp)
            .border(
                width = 2.dp,
                color = OldIvory.copy(alpha = 0.78f),
                shape = RoundedCornerShape(50)
            )
    ) {
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = contentDescription,
            tint = OldIvory,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun ImportOptionCheckbox(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFFA8B8E8),
                uncheckedColor = OldIvory.copy(alpha = 0.78f),
                checkmarkColor = Color.White
            )
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = OldIvory,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class ImportBrowserEntry(
    val name: String,
    val path: String,
    val documentId: String,
    val isDirectory: Boolean,
    val sizeBytes: Long?
)

private fun loadImportBrowserEntries(
    context: Context,
    treeUri: Uri,
    documentId: String,
    currentPath: String
): List<ImportBrowserEntry> {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_SIZE
    )

    return buildList {
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

            while (cursor.moveToNext()) {
                val childDocumentId = cursor.getString(idIndex)
                val name = cursor.getString(nameIndex).orEmpty()
                val mimeType = cursor.getString(mimeIndex).orEmpty()
                val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
                if (name.isBlank() || (!isDirectory && !isImportVisibleFile(name))) continue

                add(
                    ImportBrowserEntry(
                        name = name,
                        path = "${currentPath.trimEnd('/')}/$name",
                        documentId = childDocumentId,
                        isDirectory = isDirectory,
                        sizeBytes = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
                    )
                )
            }
        }
    }.sortedWith(
        compareByDescending<ImportBrowserEntry> { it.isDirectory }
            .thenBy { it.name.lowercase(Locale.ROOT) }
    )
}

private fun readableImportRootPath(treeUri: Uri): String {
    val documentId = DocumentsContract.getTreeDocumentId(treeUri)
    val relativePath = documentId
        .substringAfter(':', missingDelimiterValue = documentId)
        .trim('/')
    return if (relativePath.isBlank() || relativePath == "primary") {
        "/sdcard"
    } else {
        "/sdcard/$relativePath"
    }
}

private fun isImportVisibleFile(name: String): Boolean {
    val lowerName = name.lowercase(Locale.ROOT)
    return listOf(
        ".epub",
        ".pdf",
        ".djvu",
        ".fb2",
        ".mobi",
        ".azw3",
        ".prc",
        ".chm",
        ".umd",
        ".docx",
        ".odt",
        ".rtf",
        ".txt",
        ".md",
        ".html",
        ".mhtml",
        ".cbz",
        ".cbr"
    ).any(lowerName::endsWith)
}

private fun importFileAccent(name: String): Color {
    val lowerName = name.lowercase(Locale.ROOT)
    return when {
        lowerName.endsWith(".pdf") -> Color(0xFFE83B16)
        lowerName.endsWith(".epub") -> Color(0xFF16AEEB)
        lowerName.endsWith(".fb2") -> Color(0xFF6D8DFF)
        lowerName.endsWith(".cbz") || lowerName.endsWith(".cbr") -> Color(0xFF8A5CF6)
        else -> Color(0xFF6E7781)
    }
}

private fun isDefaultCoverResourceName(name: String): Boolean {
    val excludedNames = setOf(
        "bg_bookshelf",
        "ic_launcher_background",
        "ic_launcher_foreground",
        "logo_kaishelves",
        "logo_kaishelves2"
    )
    return name !in excludedNames &&
        !name.startsWith("sesion_") &&
        !name.startsWith("ic_")
}

private fun readDefaultCoverId(context: Context): String {
    val fallbackCoverId = BuiltInDefaultCoverOptions.firstOrNull()?.id.orEmpty()
    return context.getSharedPreferences(DEFAULT_COVER_PREFS, Context.MODE_PRIVATE)
        .getString(DEFAULT_COVER_KEY, fallbackCoverId)
        ?: fallbackCoverId
}

private fun saveDefaultCoverId(context: Context, coverId: String) {
    selectedDefaultCoverIdState = coverId
    context.getSharedPreferences(DEFAULT_COVER_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(DEFAULT_COVER_KEY, coverId)
        .apply()
}

private fun findDefaultCoverOption(context: Context, coverId: String): DefaultCoverOption? {
    val fallbackCover = BuiltInDefaultCoverOptions.firstOrNull()
    return BuiltInDefaultCoverOptions.firstOrNull { it.id == coverId }
        ?: File(coverId).takeIf { it.exists() }?.let { file ->
            DefaultCoverOption(id = file.absolutePath, file = file, displayName = file.name)
        }
        ?: coverId.takeIf { it.startsWith("content://") }?.let { uriText ->
            val uri = Uri.parse(uriText)
            if (canOpenContentUri(context, uri)) {
                DefaultCoverOption(id = uriText, uri = uri)
            } else {
                fallbackCover?.also { saveDefaultCoverId(context, it.id) }
            }
        }
        ?: fallbackCover
}

private fun canOpenContentUri(context: Context, uri: Uri): Boolean {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { true } == true
    }.getOrDefault(false)
}

private fun loadDownloadedDefaultCovers(context: Context, treeUri: Uri?): List<DefaultCoverOption> {
    val internalCovers = defaultCoversDir(context)
        .listFiles { file ->
            file.isFile && file.extension.lowercase(Locale.ROOT) in imageExtensions
        }
        ?.sortedBy { it.name.lowercase(Locale.ROOT) }
        ?.map { file -> DefaultCoverOption(id = file.absolutePath, file = file, displayName = file.name) }
        .orEmpty()

    val treeCovers = treeUri?.let { loadTreeDefaultCovers(context, it) }.orEmpty()
    return (internalCovers + treeCovers).distinctBy { it.id }
}

private fun saveImageUriToDefaultCovers(context: Context, uri: Uri, treeUri: Uri?): DefaultCoverOption? {
    return runCatching {
        val extension = uri.lastPathSegment
            ?.substringAfterLast('.', missingDelimiterValue = "jpg")
            ?.lowercase(Locale.ROOT)
            ?.takeIf { it in imageExtensions }
            ?: "jpg"
        val fileName = "album_${System.currentTimeMillis()}.$extension"
        treeUri?.let { targetTree ->
            return createImageInTree(context, targetTree, fileName, uri)
        }
        val target = File(defaultCoversDir(context), fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        DefaultCoverOption(id = target.absolutePath, file = target, displayName = target.name)
    }.getOrNull()
}

private fun downloadImageToDefaultCovers(context: Context, imageUrl: String, treeUri: Uri?): DefaultCoverOption? {
    return runCatching {
        val extension = imageUrl.substringBefore('?')
            .substringAfterLast('.', missingDelimiterValue = "jpg")
            .lowercase(Locale.ROOT)
            .takeIf { it in imageExtensions }
            ?: "jpg"
        val fileName = "download_${System.currentTimeMillis()}.$extension"
        treeUri?.let { targetTree ->
            val bytes = URL(imageUrl).openStream().use { it.readBytes() }
            return createImageBytesInTree(context, targetTree, fileName, bytes)
        }
        val target = File(defaultCoversDir(context), fileName)
        URL(imageUrl).openStream().use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        DefaultCoverOption(id = target.absolutePath, file = target, displayName = target.name)
    }.getOrNull()
}

private fun loadTreeDefaultCovers(context: Context, treeUri: Uri): List<DefaultCoverOption> {
    return runCatching {
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocumentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        buildList {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idIndex)
                    val name = cursor.getString(nameIndex).orEmpty()
                    val mimeType = cursor.getString(mimeIndex).orEmpty()
                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) continue
                    if (name.substringAfterLast('.', "").lowercase(Locale.ROOT) !in imageExtensions) continue
                    val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                    add(DefaultCoverOption(id = uri.toString(), uri = uri, displayName = name))
                }
            }
        }.sortedBy { option ->
            option.uri?.lastPathSegment.orEmpty().lowercase(Locale.ROOT)
        }
    }.getOrDefault(emptyList())
}

private fun createImageInTree(
    context: Context,
    treeUri: Uri,
    fileName: String,
    sourceUri: Uri
): DefaultCoverOption? {
    val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
    val rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocumentId)
    val targetUri = DocumentsContract.createDocument(
        context.contentResolver,
        rootDocumentUri,
        mimeTypeForImageName(fileName),
        fileName
    ) ?: return null
    context.contentResolver.openInputStream(sourceUri)?.use { input ->
        context.contentResolver.openOutputStream(targetUri)?.use { output ->
            input.copyTo(output)
        }
    } ?: return null
    return DefaultCoverOption(id = targetUri.toString(), uri = targetUri, displayName = fileName)
}

private fun createImageBytesInTree(
    context: Context,
    treeUri: Uri,
    fileName: String,
    bytes: ByteArray
): DefaultCoverOption? {
    val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
    val rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocumentId)
    val targetUri = DocumentsContract.createDocument(
        context.contentResolver,
        rootDocumentUri,
        mimeTypeForImageName(fileName),
        fileName
    ) ?: return null
    context.contentResolver.openOutputStream(targetUri)?.use { output ->
        output.write(bytes)
    } ?: return null
    return DefaultCoverOption(id = targetUri.toString(), uri = targetUri, displayName = fileName)
}

private fun renameDefaultCover(
    context: Context,
    cover: DefaultCoverOption,
    rawName: String
): DefaultCoverOption? {
    val cleanBaseName = rawName.trim().ifBlank { return null }
        .replace(Regex("""[\\/:*?"<>|]"""), "_")
    val extension = cover.displayName
        ?.substringAfterLast('.', "")
        ?.takeIf { it.isNotBlank() }
        ?: cover.file?.extension?.takeIf { it.isNotBlank() }
        ?: "jpg"
    val newDisplayName = if (cleanBaseName.endsWith(".$extension", ignoreCase = true)) {
        cleanBaseName
    } else {
        "$cleanBaseName.$extension"
    }

    cover.file?.let { file ->
        val target = File(file.parentFile ?: defaultCoversDir(context), newDisplayName)
        return if (file.renameTo(target)) {
            DefaultCoverOption(id = target.absolutePath, file = target, displayName = target.name)
        } else {
            null
        }
    }

    cover.uri?.let { uri ->
        val renamedUri = DocumentsContract.renameDocument(
            context.contentResolver,
            uri,
            newDisplayName
        ) ?: return null
        return DefaultCoverOption(
            id = renamedUri.toString(),
            uri = renamedUri,
            displayName = newDisplayName
        )
    }

    return null
}

private fun deleteDefaultCover(context: Context, cover: DefaultCoverOption): Boolean {
    cover.file?.let { file ->
        return file.delete()
    }
    cover.uri?.let { uri ->
        return runCatching {
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        }.getOrDefault(false)
    }
    return false
}

private fun readDefaultCoverStorageTreeUri(context: Context): String? {
    return context.getSharedPreferences(DEFAULT_COVER_PREFS, Context.MODE_PRIVATE)
        .getString(DEFAULT_COVER_STORAGE_TREE_KEY, null)
}

private fun saveDefaultCoverStorageTreeUri(context: Context, treeUri: Uri) {
    context.getSharedPreferences(DEFAULT_COVER_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(DEFAULT_COVER_STORAGE_TREE_KEY, treeUri.toString())
        .apply()
}

private fun mimeTypeForImageName(fileName: String): String {
    return when (fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }
}

private fun defaultCoversDir(context: Context): File {
    return File(context.filesDir, "backgrounds").apply { mkdirs() }
}

private const val DEFAULT_COVER_PREFS = "device_library_default_cover"
private const val DEFAULT_COVER_KEY = "selected_cover"
private const val DEFAULT_COVER_STORAGE_TREE_KEY = "storage_tree_uri"
private val imageExtensions = setOf("jpg", "jpeg", "png", "webp")

private fun Set<String>.toggleItem(item: String, checked: Boolean): Set<String> {
    return if (checked) this + item else this - item
}

@Composable
private fun LibrarySelectorMenu(fileCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF0141414)),
        border = BorderStroke(1.dp, OldIvory.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxSize()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    "Todos los libros",
                    "Mis Favoritos",
                    "Serie",
                    "Autor",
                    "Etiqueta",
                    "Carpetas",
                    "Mi clasificación  ›"
                ).forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OldIvory,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxSize()
                    .background(OldIvory.copy(alpha = 0.16f))
            )

            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                (5 downTo 1).forEach { stars ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buildString {
                                repeat(stars) { append("★") }
                                repeat(5 - stars) { append("☆") }
                            },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            color = OldIvory
                        )
                        Text(
                            text = if (stars == 5) fileCount.toString() else "0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OldIvory.copy(alpha = 0.72f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceLibrarySearchOverlay(
    topPadding: androidx.compose.ui.unit.Dp,
    query: String,
    files: List<DeviceLibraryFile>,
    recentSearches: List<String>,
    onQueryChange: (String) -> Unit,
    onCommitSearch: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.TopCenter
    ) {
        DeviceLibrarySearchPanel(
            query = query,
            files = files,
            recentSearches = recentSearches,
            onQueryChange = onQueryChange,
            onCommitSearch = onCommitSearch,
            onRemoveRecentSearch = onRemoveRecentSearch,
            onClose = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clickable(onClick = { })
        )
    }
}

@Composable
private fun DeviceLibrarySearchPanel(
    query: String,
    files: List<DeviceLibraryFile>,
    recentSearches: List<String>,
    onQueryChange: (String) -> Unit,
    onCommitSearch: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = remember(query, files) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            emptyList()
        } else {
            files.asSequence()
                .filter { file ->
                    file.name.contains(cleanQuery, ignoreCase = true) ||
                        file.location.contains(cleanQuery, ignoreCase = true)
                }
                .map { file -> "Archivo: ${file.name.substringBeforeLast('.')}" }
                .distinct()
                .take(8)
                .toList()
        }
    }
    val showingRecentSearches = query.isBlank()
    val visibleItems = if (showingRecentSearches) recentSearches else suggestions

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF0242424))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = OldIvory)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.clear_search),
                                    tint = OldIvory
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { onCommitSearch(query) }
                    ),
                    colors = com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults.outlinedTextFieldColors()
                )

                IconButton(
                    onClick = {
                        onQueryChange("")
                        onClose()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = OldIvory
                    )
                }
            }

            visibleItems.take(8).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = OldIvory.copy(alpha = 0.56f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val cleanItem = item.substringAfter(": ", item)
                                onQueryChange(cleanItem)
                                onCommitSearch(cleanItem)
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color = OldIvory,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (showingRecentSearches) {
                        IconButton(
                            onClick = { onRemoveRecentSearch(item) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Eliminar busqueda reciente",
                                tint = OldIvory.copy(alpha = 0.78f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceLibraryFilterOverlay(
    layoutMode: DeviceLibraryLayoutMode,
    sortOption: DeviceLibrarySortOption,
    sortDescending: Boolean,
    onLayoutModeChange: (DeviceLibraryLayoutMode) -> Unit,
    onSortOptionChange: (DeviceLibrarySortOption) -> Unit,
    onToggleSortDirection: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        DeviceLibraryFilterPanel(
            layoutMode = layoutMode,
            sortOption = sortOption,
            sortDescending = sortDescending,
            onLayoutModeChange = onLayoutModeChange,
            onSortOptionChange = onSortOptionChange,
            onToggleSortDirection = onToggleSortDirection,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .widthIn(max = 520.dp)
                .clickable(onClick = { })
        )
    }
}

@Composable
private fun DeviceLibraryFilterPanel(
    layoutMode: DeviceLibraryLayoutMode,
    sortOption: DeviceLibrarySortOption,
    sortDescending: Boolean,
    onLayoutModeChange: (DeviceLibraryLayoutMode) -> Unit,
    onSortOptionChange: (DeviceLibrarySortOption) -> Unit,
    onToggleSortDirection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF01A1717)),
        border = BorderStroke(1.dp, OldIvory.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Ordenado por",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OldIvory
                        )
                        IconButton(
                            onClick = onToggleSortDirection,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapVert,
                                contentDescription = if (sortDescending) "Orden descendente" else "Orden ascendente",
                                tint = TarnishedGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    FilterOptionRow(
                        "Título del libro",
                        selected = sortOption == DeviceLibrarySortOption.Title,
                        radio = true,
                        onClick = { onSortOptionChange(DeviceLibrarySortOption.Title) }
                    )
                    FilterOptionRow(
                        "Autor",
                        selected = sortOption == DeviceLibrarySortOption.Author,
                        radio = true,
                        onClick = { onSortOptionChange(DeviceLibrarySortOption.Author) }
                    )
                    FilterOptionRow(
                        "Reciente",
                        selected = sortOption == DeviceLibrarySortOption.Recent,
                        radio = true,
                        onClick = { onSortOptionChange(DeviceLibrarySortOption.Recent) }
                    )
                    FilterOptionRow(
                        "Carpetas",
                        selected = sortOption == DeviceLibrarySortOption.Folder,
                        radio = true,
                        onClick = { onSortOptionChange(DeviceLibrarySortOption.Folder) }
                    )
                    FilterOptionRow(
                        "Lista reciente",
                        selected = sortOption == DeviceLibrarySortOption.RecentList,
                        radio = true,
                        onClick = { onSortOptionChange(DeviceLibrarySortOption.RecentList) }
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Filtro de lectura",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OldIvory
                        )
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = TarnishedGold, modifier = Modifier.size(18.dp))
                    }
                    FilterOptionRow("Sin leer", selected = false, radio = false)
                    FilterOptionRow("Leyendo", selected = true, radio = false)
                    FilterOptionRow("Finalizado", selected = false, radio = false)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Disposición",
                    modifier = Modifier.widthIn(min = 86.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldIvory
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .border(BorderStroke(1.dp, OldIvory.copy(alpha = 0.72f)), RoundedCornerShape(999.dp))
                ) {
                    LayoutSegmentButton(
                        icon = Icons.Filled.TableRows,
                        selected = layoutMode == DeviceLibraryLayoutMode.List,
                        onClick = { onLayoutModeChange(DeviceLibraryLayoutMode.List) }
                    )
                    LayoutSegmentButton(
                        icon = Icons.Filled.GridView,
                        selected = layoutMode == DeviceLibraryLayoutMode.Grid,
                        onClick = { onLayoutModeChange(DeviceLibraryLayoutMode.Grid) }
                    )
                    LayoutSegmentButton(
                        icon = Icons.Filled.ViewColumn,
                        selected = layoutMode == DeviceLibraryLayoutMode.Carousel,
                        onClick = { onLayoutModeChange(DeviceLibraryLayoutMode.Carousel) }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Tipo de archivo",
                    modifier = Modifier.widthIn(min = 118.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldIvory
                )
                Checkbox(
                    checked = true,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF9EAFDF),
                        checkmarkColor = OldIvory,
                        uncheckedColor = OldIvory.copy(alpha = 0.72f)
                    )
                )
                Text("Todo", style = MaterialTheme.typography.bodyMedium, color = OldIvory)
            }
        }
    }
}

@Composable
private fun FilterOptionRow(
    label: String,
    selected: Boolean,
    radio: Boolean,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (radio) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF9EAFDF),
                    unselectedColor = OldIvory.copy(alpha = 0.72f)
                )
            )
        } else {
            Checkbox(
                checked = selected,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF9EAFDF),
                    checkmarkColor = OldIvory,
                    uncheckedColor = OldIvory.copy(alpha = 0.72f)
                )
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = OldIvory,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LayoutSegmentButton(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(68.dp)
            .height(38.dp)
            .clickable(onClick = onClick)
            .background(if (selected) Color(0xFF7881A2) else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OldIvory,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun DeviceLibraryContent(
    modifier: Modifier,
    files: List<DeviceLibraryFile>,
    isLoading: Boolean,
    errorMessage: String?,
    layoutMode: DeviceLibraryLayoutMode,
    onOpenFile: (DeviceLibraryFile) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = TarnishedGold)
        }
        return
    }

    if (errorMessage != null || files.isEmpty()) {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            errorMessage?.let { error ->
                item {
                    DeviceLibraryMessage(
                        title = stringResource(R.string.device_library_error_title),
                        body = error
                    )
                }
            }

            if (files.isEmpty()) {
                item {
                    DeviceLibraryMessage(
                        title = stringResource(R.string.device_library_empty_title),
                        body = stringResource(R.string.device_library_empty_body)
                    )
                }
            }
        }
        return
    }

    when (layoutMode) {
        DeviceLibraryLayoutMode.List -> DeviceLibraryListView(
            modifier = modifier,
            files = files,
            onOpenFile = onOpenFile
        )
        DeviceLibraryLayoutMode.Grid -> DeviceLibraryGridView(
            modifier = modifier,
            files = files,
            onOpenFile = onOpenFile
        )
        DeviceLibraryLayoutMode.Carousel -> DeviceLibraryCarouselView(
            modifier = modifier,
            files = files,
            onOpenFile = onOpenFile
        )
    }
}

@Composable
private fun DeviceLibraryListView(
    modifier: Modifier,
    files: List<DeviceLibraryFile>,
    onOpenFile: (DeviceLibraryFile) -> Unit
) {
    val listState = rememberLazyListState()

    Box(modifier = modifier.background(ShelfBackgroundBrush)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 28.dp, top = 18.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(files, key = { it.uri.toString() }) { file ->
                ShelfListRow(
                    file = file,
                    progress = readingProgressFor(file),
                    onClick = { onOpenFile(file) }
                )
            }
        }

        DeviceLibraryLazyListScrollbar(
            state = listState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun DeviceLibraryGridView(
    modifier: Modifier,
    files: List<DeviceLibraryFile>,
    onOpenFile: (DeviceLibraryFile) -> Unit
) {
    val gridState = rememberLazyGridState()

    Box(modifier = modifier.background(ShelfBackgroundBrush)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            state = gridState,
            contentPadding = PaddingValues(start = 18.dp, end = 30.dp, top = 18.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            items(files, key = { it.uri.toString() }) { file ->
                ShelfGridBook(
                    file = file,
                    progress = readingProgressFor(file),
                    onClick = { onOpenFile(file) }
                )
            }
        }

        DeviceLibraryLazyGridScrollbar(
            state = gridState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun DeviceLibraryLazyListScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier
) {
    DeviceLibraryFastScrollbar(
        totalItems = state.layoutInfo.totalItemsCount,
        visibleItems = state.layoutInfo.visibleItemsInfo.size,
        firstVisibleItemIndex = state.firstVisibleItemIndex,
        modifier = modifier,
        onScrollToItem = { index -> state.scrollToItem(index) }
    )
}

@Composable
private fun DeviceLibraryLazyGridScrollbar(
    state: LazyGridState,
    modifier: Modifier = Modifier
) {
    DeviceLibraryFastScrollbar(
        totalItems = state.layoutInfo.totalItemsCount,
        visibleItems = state.layoutInfo.visibleItemsInfo.size,
        firstVisibleItemIndex = state.firstVisibleItemIndex,
        modifier = modifier,
        onScrollToItem = { index -> state.scrollToItem(index) }
    )
}

@Composable
private fun DeviceLibraryFastScrollbar(
    totalItems: Int,
    visibleItems: Int,
    firstVisibleItemIndex: Int,
    modifier: Modifier = Modifier,
    onScrollToItem: suspend (Int) -> Unit
) {
    if (totalItems <= visibleItems || visibleItems == 0) return

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val scrollbarWidth = 14.dp
    var trackHeightPx by remember { mutableStateOf(0) }
    val minThumbHeightPx = with(density) { 44.dp.toPx() }
    val thumbHeightPx = if (trackHeightPx > 0) {
        (trackHeightPx * visibleItems / totalItems.toFloat()).coerceAtLeast(minThumbHeightPx)
    } else {
        minThumbHeightPx
    }
    val scrollableItems = (totalItems - visibleItems).coerceAtLeast(1)
    val scrollableTrackPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)
    val thumbOffsetPx = scrollableTrackPx * (firstVisibleItemIndex / scrollableItems.toFloat())
    val currentTrackHeightPx by rememberUpdatedState(trackHeightPx)
    val currentScrollableTrackPx by rememberUpdatedState(scrollableTrackPx)
    val currentScrollableItems by rememberUpdatedState(scrollableItems)
    val currentFirstVisibleItemIndex by rememberUpdatedState(firstVisibleItemIndex)
    val draggableState = rememberDraggableState { delta ->
        if (currentTrackHeightPx == 0) return@rememberDraggableState
        val currentOffset = currentScrollableTrackPx *
            (currentFirstVisibleItemIndex / currentScrollableItems.toFloat())
        val nextOffset = (currentOffset + delta).coerceIn(0f, currentScrollableTrackPx)
        val targetIndex = (nextOffset / currentScrollableTrackPx * currentScrollableItems)
            .toInt()
            .coerceIn(0, currentScrollableItems)
        scope.launch { onScrollToItem(targetIndex) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 18.dp, bottom = 96.dp, end = 4.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(
            modifier = Modifier
                .width(scrollbarWidth)
                .fillMaxSize()
                .onSizeChanged { trackHeightPx = it.height }
                .clip(RoundedCornerShape(999.dp))
                .background(OldIvory.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .width(scrollbarWidth)
                    .height(with(density) { thumbHeightPx.toDp() })
                    .graphicsLayer { translationY = thumbOffsetPx }
                    .clip(RoundedCornerShape(999.dp))
                    .background(TarnishedGold.copy(alpha = 0.82f))
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = draggableState
                    )
            )
        }
    }
}

@Composable
private fun DeviceLibraryCarouselView(
    modifier: Modifier,
    files: List<DeviceLibraryFile>,
    onOpenFile: (DeviceLibraryFile) -> Unit
) {
    var focusedIndex by remember(files) { mutableStateOf(if (files.size > 1) 1 else 0) }
    var advancing by remember(files) { mutableStateOf(false) }
    LaunchedEffect(files.size) {
        if (files.size <= 1) return@LaunchedEffect
        while (true) {
            delay(2200)
            advancing = true
            delay(820)
            focusedIndex = (focusedIndex + 1) % files.size
            advancing = false
        }
    }

    val currentFile = files[focusedIndex.coerceIn(files.indices)]
    val previousFile = files[(focusedIndex - 1).floorMod(files.size)]
    val nextFile = files[(focusedIndex + 1).floorMod(files.size)]
    val currentRotation by animateFloatAsState(
        targetValue = if (advancing) -62f else 0f,
        animationSpec = tween(durationMillis = 760),
        label = "carouselCurrentRotation"
    )
    val currentScale by animateFloatAsState(
        targetValue = if (advancing) 0.82f else 1f,
        animationSpec = tween(durationMillis = 760),
        label = "carouselCurrentScale"
    )
    val currentAlpha by animateFloatAsState(
        targetValue = if (advancing) 0.62f else 1f,
        animationSpec = tween(durationMillis = 760),
        label = "carouselCurrentAlpha"
    )
    val nextRotation by animateFloatAsState(
        targetValue = if (advancing) 0f else -58f,
        animationSpec = tween(durationMillis = 760),
        label = "carouselNextRotation"
    )
    val nextScale by animateFloatAsState(
        targetValue = if (advancing) 1f else 0.84f,
        animationSpec = tween(durationMillis = 760),
        label = "carouselNextScale"
    )
    val nextAlpha by animateFloatAsState(
        targetValue = if (advancing) 1f else 0.86f,
        animationSpec = tween(durationMillis = 760),
        label = "carouselNextAlpha"
    )

    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF36343F),
                        Color(0xFF151720),
                        Color.Black
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            text = "${focusedIndex + 1}/${files.size}",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 36.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = OldIvory
        )

        CarouselCover(
            file = previousFile,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(top = 18.dp)
                .zIndex(0f),
            rotationY = 58f,
            scale = 0.84f,
            alpha = 0.86f,
            onClick = { onOpenFile(previousFile) }
        )

        CarouselCover(
            file = nextFile,
            modifier = Modifier
                .align(if (advancing) Alignment.Center else Alignment.CenterEnd)
                .padding(top = 18.dp)
                .zIndex(if (advancing) 2f else 0f),
            rotationY = nextRotation,
            scale = nextScale,
            alpha = nextAlpha,
            onClick = { onOpenFile(nextFile) }
        )

        CarouselCover(
            file = currentFile,
            modifier = Modifier
                .align(if (advancing) Alignment.CenterStart else Alignment.Center)
                .zIndex(1f),
            rotationY = currentRotation,
            scale = currentScale,
            alpha = currentAlpha,
            onClick = { onOpenFile(currentFile) }
        )

        Text(
            text = currentFile.name.substringBeforeLast('.') + " - " + currentFile.location.substringAfterLast('/'),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = OldIvory,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ShelfListRow(
    file: DeviceLibraryFile,
    progress: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val bookMetadata by produceState<DeviceBookDisplayMetadata?>(initialValue = null, file.uri) {
        value = withContext(Dispatchers.IO) {
            if (isEpub(file)) extractEpubDisplayMetadata(context, file.uri) else null
        }
    }
    val title = bookMetadata?.title?.takeIf { it.isNotBlank() }
        ?: file.name.substringBeforeLast('.')
    val author = bookMetadata?.author.orEmpty()
    val summary = bookMetadata?.description.orEmpty()
    var showBookOptions by remember(file.uri) { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF171717),
                        Color(0xE5232323),
                        Color(0xFF151515)
                    )
                )
            )
    ) {
        val compact = maxWidth < 380.dp
        val rowHeight = when {
            compact -> 158.dp
            maxWidth < 600.dp -> 170.dp
            else -> 186.dp
        }
        val verticalPadding = if (compact) 10.dp else 12.dp
        val coverWidth = when {
            compact -> 86.dp
            maxWidth < 600.dp -> 96.dp
            else -> 108.dp
        }
        val contentGap = if (compact) 12.dp else 16.dp
        val summaryLines = if (compact) 2 else 3

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .padding(start = 10.dp, top = verticalPadding, end = 6.dp, bottom = verticalPadding),
            verticalAlignment = Alignment.Top
        ) {
            FilePagePreview(
                file = file,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(coverWidth)
            )

            Spacer(modifier = Modifier.width(contentGap))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(top = 1.dp, bottom = 1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = if (compact) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleLarge
                        },
                        color = OldIvory,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = if (compact) 1 else 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Box {
                        IconButton(
                            onClick = { showBookOptions = true },
                            modifier = Modifier.size(if (compact) 28.dp else 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.more_option),
                                tint = OldIvory.copy(alpha = 0.74f),
                                modifier = Modifier.size(if (compact) 20.dp else 22.dp)
                            )
                        }

                        DeviceLibraryBookOptionsMenu(
                            expanded = showBookOptions,
                            onDismiss = { showBookOptions = false }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (compact) 4.dp else 6.dp))

                Text(
                    text = author,
                    style = if (compact) {
                        MaterialTheme.typography.bodySmall
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    color = OldIvory.copy(alpha = 0.74f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(if (compact) 8.dp else 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReadingProgressBar(
                        progress = progress,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(if (compact) 8.dp else 10.dp))

                    Text(
                        text = "${progress}%",
                        style = if (compact) {
                            MaterialTheme.typography.bodySmall
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = OldIvory.copy(alpha = 0.78f),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(if (compact) 8.dp else 10.dp))

                Text(
                    text = summary,
                    modifier = Modifier.fillMaxWidth(),
                    style = if (compact) {
                        MaterialTheme.typography.bodySmall
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    color = OldIvory.copy(alpha = 0.72f),
                    maxLines = summaryLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DeviceLibraryBookOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 300.dp, max = 390.dp),
        shape = RoundedCornerShape(2.dp),
        containerColor = Color(0xFF262626),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        DeviceLibraryBookOptionItem(
            text = "Pin en la parte superior",
            onClick = onDismiss
        )

        HorizontalDivider(color = OldIvory.copy(alpha = 0.16f))

        DeviceLibraryBookOptionItem(
            text = "Información del libro",
            onClick = onDismiss
        )
        DeviceLibraryBookOptionItem(
            text = "Descargar Portada de Libro",
            onClick = onDismiss
        )
        DeviceLibraryBookOptionItem(
            text = "Crear acceso directo en escritorio",
            onClick = onDismiss
        )
        DeviceLibraryBookOptionItem(
            text = "Enviar archivo",
            onClick = onDismiss
        )
        DeviceLibraryBookOptionItem(
            text = "Quitar de mi biblioteca",
            onClick = onDismiss
        )
    }
}

@Composable
private fun DeviceLibraryBookOptionItem(
    text: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = OldIvory,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        onClick = onClick,
        modifier = Modifier.height(58.dp)
    )
}

@Composable
private fun ShelfGridBook(
    file: DeviceLibraryFile,
    progress: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.66f)
            .clickable(onClick = onClick)
    ) {
        FilePagePreview(
            file = file,
            modifier = Modifier.fillMaxSize()
        )
        Text(
            text = "${progress}%",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.62f))
                .padding(horizontal = 5.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = OldIvory
        )
    }
}

@Composable
private fun CarouselCover(
    file: DeviceLibraryFile,
    modifier: Modifier,
    rotationY: Float,
    scale: Float,
    alpha: Float,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .graphicsLayer {
                this.rotationY = rotationY
                this.scaleX = scale
                this.scaleY = scale
                this.alpha = alpha
                cameraDistance = 12f * density
            }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilePagePreview(
            file = file,
            modifier = Modifier
                .width(112.dp)
                .aspectRatio(0.68f)
        )
        FilePagePreview(
            file = file,
            modifier = Modifier
                .width(112.dp)
                .aspectRatio(0.68f)
                .graphicsLayer {
                    this.scaleY = -0.62f
                    this.alpha = 0.18f
                }
        )
    }
}

@Composable
private fun ReadingProgressBar(
    progress: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(OldIvory.copy(alpha = 0.26f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0, 100) / 100f)
                .height(2.dp)
                .background(Color(0xFF4F9FE3))
        )
    }
}

private val ShelfBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1F1F1F),
        Color(0xFF121212),
        Color(0xFF242424),
        Color(0xFF101010)
    )
)

private fun readingProgressFor(file: DeviceLibraryFile): Int {
    val seed = file.uri.toString().fold(0) { acc, char -> acc + char.code }
    return seed % 101
}

private fun Int.floorMod(other: Int): Int {
    return ((this % other) + other) % other
}

@Composable
private fun DeviceFileRow(
    file: DeviceLibraryFile,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val dateFormatter = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    val sizeText = file.sizeBytes?.let { Formatter.formatShortFileSize(context, it) }
    val modifiedText = file.modifiedAtMillis?.takeIf { it > 0 }?.let { dateFormatter.format(Date(it)) }
    val metadata = listOfNotNull(sizeText, modifiedText, readableFileType(file)).joinToString(" - ")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DeepWalnut.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilePagePreview(file = file)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = OldIvory,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = file.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = OldIvory.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (metadata.isNotBlank()) {
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodySmall,
                        color = TarnishedGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun FilePagePreview(
    file: DeviceLibraryFile,
    modifier: Modifier = Modifier
        .width(48.dp)
        .aspectRatio(0.68f)
) {
    val context = LocalContext.current
    val preview by produceState<Bitmap?>(initialValue = null, file.uri) {
        value = withContext(Dispatchers.IO) {
            when {
                isPdf(file) -> renderPdfFirstPage(context, file.uri)
                isEpub(file) -> extractEpubCover(context, file.uri)
                else -> null
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(OldIvory.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        if (preview != null) {
            Image(
                bitmap = preview!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            FallbackBookPreview(file = file)
        }
    }
}

@Composable
private fun FallbackBookPreview(file: DeviceLibraryFile) {
    val context = LocalContext.current
    val defaultCoverId = selectedDefaultCoverIdState ?: remember { readDefaultCoverId(context) }
    val defaultCover = remember(defaultCoverId) { findDefaultCoverOption(context, defaultCoverId) }
    val fileCoverBitmap by produceState<Bitmap?>(initialValue = null, defaultCover?.file) {
        value = withContext(Dispatchers.IO) {
            defaultCover?.file?.let { BitmapFactory.decodeFile(it.absolutePath) }
        }
    }
    val uriCoverBitmap by produceState<Bitmap?>(initialValue = null, defaultCover?.uri) {
        value = withContext(Dispatchers.IO) {
            defaultCover?.uri?.let { uri ->
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                }.getOrNull()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OldIvory.copy(alpha = 0.92f)),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.Start
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                defaultCover?.resourceId != null -> Image(
                    painter = painterResource(defaultCover.resourceId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                fileCoverBitmap != null -> Image(
                    bitmap = fileCoverBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                uriCoverBitmap != null -> Image(
                    bitmap = uriCoverBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                else -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    OldIvory,
                                    Color(0xFFC8B89F)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = DeepWalnut,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = file.name.substringBeforeLast('.').take(18),
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepWalnut,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DeviceLibraryMessage(
    title: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepWalnut),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TarnishedGold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory
            )
        }
    }
}

private fun isPdf(file: DeviceLibraryFile): Boolean {
    return file.mimeType == "application/pdf" || file.name.endsWith(".pdf", ignoreCase = true)
}

private fun isEpub(file: DeviceLibraryFile): Boolean {
    return file.mimeType == "application/epub+zip" || file.name.endsWith(".epub", ignoreCase = true)
}

private fun readableFileType(file: DeviceLibraryFile): String {
    return file.name.substringAfterLast('.', missingDelimiterValue = file.mimeType.orEmpty())
        .uppercase(Locale.ROOT)
        .ifBlank { file.mimeType.orEmpty() }
}

private data class DeviceBookDisplayMetadata(
    val title: String = "",
    val author: String = "",
    val description: String = ""
)

private fun renderPdfFirstPage(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            renderPdfFirstPage(descriptor)
        }
    }.getOrNull()
}

private fun renderPdfFirstPage(descriptor: ParcelFileDescriptor): Bitmap? {
    PdfRenderer(descriptor).use { renderer ->
        if (renderer.pageCount == 0) return null
        renderer.openPage(0).use { page ->
            val targetWidth = 180
            val targetHeight = (targetWidth.toFloat() / page.width * page.height).toInt().coerceAtLeast(220)
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(AndroidColor.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        }
    }
}

private fun extractEpubCover(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        val rootFilePath = readZipEntry(context, uri, "META-INF/container.xml")
            ?.decodeToString()
            ?.let(::parseRootFilePath)
            ?: return@runCatching null
        val opf = readZipEntry(context, uri, rootFilePath)?.decodeToString()
            ?: return@runCatching null
        val coverHref = parseCoverHref(opf) ?: return@runCatching null
        val coverPath = resolveZipPath(rootFilePath, coverHref)
        readZipEntry(context, uri, coverPath)?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }.getOrNull()
}

private fun extractEpubDisplayMetadata(context: Context, uri: Uri): DeviceBookDisplayMetadata? {
    return runCatching {
        val rootFilePath = readZipEntry(context, uri, "META-INF/container.xml")
            ?.decodeToString()
            ?.let(::parseRootFilePath)
            ?: return@runCatching null
        val opf = readZipEntry(context, uri, rootFilePath)?.decodeToString()
            ?: return@runCatching null
        parseEpubDisplayMetadata(opf)
    }.getOrNull()
}

private fun readZipEntry(context: Context, uri: Uri, targetPath: String): ByteArray? {
    context.contentResolver.openInputStream(uri)?.use { input ->
        ZipInputStream(input).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name == targetPath) {
                    return zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
    return null
}

private fun parseRootFilePath(containerXml: String): String? {
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    parser.setInput(containerXml.reader())
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        if (event == XmlPullParser.START_TAG && parser.name == "rootfile") {
            return parser.getAttributeValue(null, "full-path")
        }
        event = parser.next()
    }
    return null
}

private fun parseCoverHref(opfXml: String): String? {
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    parser.setInput(opfXml.reader())
    val manifestItems = linkedMapOf<String, String>()
    var coverId: String? = null
    var fallbackCover: String? = null
    var event = parser.eventType

    while (event != XmlPullParser.END_DOCUMENT) {
        if (event == XmlPullParser.START_TAG) {
            when (parser.name) {
                "meta" -> {
                    if (parser.getAttributeValue(null, "name") == "cover") {
                        coverId = parser.getAttributeValue(null, "content")
                    }
                }
                "item" -> {
                    val id = parser.getAttributeValue(null, "id")
                    val href = parser.getAttributeValue(null, "href")
                    val mediaType = parser.getAttributeValue(null, "media-type").orEmpty()
                    val properties = parser.getAttributeValue(null, "properties").orEmpty()
                    if (!id.isNullOrBlank() && !href.isNullOrBlank()) {
                        manifestItems[id] = href
                        if (properties.split(' ').contains("cover-image")) {
                            return href
                        }
                        if (fallbackCover == null && mediaType.startsWith("image/") && id.contains("cover", ignoreCase = true)) {
                            fallbackCover = href
                        }
                    }
                }
            }
        }
        event = parser.next()
    }

    return coverId?.let(manifestItems::get) ?: fallbackCover
}

private fun parseEpubDisplayMetadata(opfXml: String): DeviceBookDisplayMetadata {
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    parser.setInput(opfXml.reader())
    var title = ""
    var author = ""
    var description = ""
    var readingTag: String? = null
    var event = parser.eventType

    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> {
                val name = parser.name.substringAfter(':')
                if (name == "title" || name == "creator" || name == "description") {
                    readingTag = name
                }
            }
            XmlPullParser.TEXT -> {
                val text = parser.text.orEmpty().trim()
                if (text.isNotBlank()) {
                    when (readingTag) {
                        "title" -> if (title.isBlank()) title = text
                        "creator" -> if (author.isBlank()) author = text
                        "description" -> if (description.isBlank()) description = text
                    }
                }
            }
            XmlPullParser.END_TAG -> readingTag = null
        }
        event = parser.next()
    }

    return DeviceBookDisplayMetadata(
        title = title,
        author = author,
        description = description
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    )
}

private fun resolveZipPath(rootFilePath: String, href: String): String {
    val basePath = rootFilePath.substringBeforeLast('/', missingDelimiterValue = "")
    val combined = if (basePath.isBlank()) href else "$basePath/$href"
    val parts = ArrayDeque<String>()
    combined.split('/').forEach { part ->
        when (part) {
            "", "." -> Unit
            ".." -> if (parts.isNotEmpty()) parts.removeLast()
            else -> parts.addLast(part)
        }
    }
    return parts.joinToString("/")
}
