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
import android.text.format.Formatter
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

private enum class DeviceLibraryLayoutMode {
    List,
    Grid,
    Carousel
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
    var layoutMode by remember { mutableStateOf(DeviceLibraryLayoutMode.List) }
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
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                DeviceLibraryTopBar(
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    notificationCount = pendingRequestCount,
                    onOpenNotifications = onOpenNotifications,
                    hasFolder = uiState.selectedFolderUri != null,
                    fileCount = uiState.filteredFiles.size,
                    isLoading = uiState.isLoading,
                    query = uiState.searchQuery,
                    files = uiState.files,
                    layoutMode = layoutMode,
                    onLayoutModeChange = { layoutMode = it },
                    onQueryChange = viewModel::onSearchQueryChange,
                    onChooseFolder = { folderLauncher.launch(null) },
                    onRefresh = viewModel::refresh
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
                files = uiState.filteredFiles,
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                layoutMode = layoutMode,
                onOpenFile = ::openFile
            )
        }
    }
}

@Composable
private fun DeviceLibraryTopBar(
    onOpenMenu: () -> Unit,
    notificationCount: Int,
    onOpenNotifications: () -> Unit,
    hasFolder: Boolean,
    fileCount: Int,
    isLoading: Boolean,
    query: String,
    files: List<DeviceLibraryFile>,
    layoutMode: DeviceLibraryLayoutMode,
    onLayoutModeChange: (DeviceLibraryLayoutMode) -> Unit,
    onQueryChange: (String) -> Unit,
    onChooseFolder: () -> Unit,
    onRefresh: () -> Unit
) {
    var showLibraryMenu by remember { mutableStateOf(false) }
    var showSearchPanel by remember { mutableStateOf(false) }
    var showFilterPanel by remember { mutableStateOf(false) }
    val recentSearches = remember { mutableStateListOf("alma", "cuerpo", "la chica", "inv", "mil no", "pav", "ese ins") }

    fun commitSearch(value: String) {
        val cleanValue = value.trim()
        if (cleanValue.isNotBlank()) {
            recentSearches.remove(cleanValue)
            recentSearches.add(0, cleanValue)
            while (recentSearches.size > 7) recentSearches.removeAt(recentSearches.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
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

            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(onClick = onOpenNotifications) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsNone,
                        contentDescription = stringResource(R.string.open_notifications_center),
                        tint = TarnishedGold
                    )
                }

                if (notificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp, top = 6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(BloodWine)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = notificationCount.coerceAtMost(99).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = OldIvory
                        )
                    }
                }
            }
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
                        showSearchPanel = false
                        showFilterPanel = false
                    }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Todos los libros",
                    style = MaterialTheme.typography.titleMedium,
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
                    showSearchPanel = true
                    showLibraryMenu = false
                    showFilterPanel = false
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.search),
                    tint = OldIvory
                )
            }

            IconButton(
                onClick = {
                    showFilterPanel = !showFilterPanel
                    showSearchPanel = false
                    showLibraryMenu = false
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.FilterAlt,
                    contentDescription = "Filtro",
                    tint = OldIvory
                )
            }

            IconButton(onClick = onChooseFolder) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = stringResource(R.string.device_library_choose_folder),
                    tint = TarnishedGold
                )
            }

            IconButton(onClick = onRefresh, enabled = !isLoading && hasFolder) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.refresh),
                    tint = if (!isLoading && hasFolder) TarnishedGold else TarnishedGold.copy(alpha = 0.42f)
                )
            }

            Text(
                text = fileCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = OldIvory.copy(alpha = 0.78f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.more_option),
                    tint = OldIvory
                )
            }
        }

        when {
            showLibraryMenu -> LibrarySelectorMenu(fileCount = fileCount)
            showSearchPanel -> DeviceLibrarySearchPanel(
                query = query,
                files = files,
                recentSearches = recentSearches,
                onQueryChange = onQueryChange,
                onCommitSearch = { commitSearch(it) },
                onRemoveRecentSearch = { recentSearches.remove(it) },
                onClose = { showSearchPanel = false }
            )
            showFilterPanel -> DeviceLibraryFilterPanel(
                layoutMode = layoutMode,
                onLayoutModeChange = onLayoutModeChange
            )
        }
    }
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
private fun DeviceLibrarySearchPanel(
    query: String,
    files: List<DeviceLibraryFile>,
    recentSearches: List<String>,
    onQueryChange: (String) -> Unit,
    onCommitSearch: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onClose: () -> Unit
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
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
                        .padding(horizontal = 12.dp, vertical = 12.dp),
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
private fun DeviceLibraryFilterPanel(
    layoutMode: DeviceLibraryLayoutMode,
    onLayoutModeChange: (DeviceLibraryLayoutMode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF0141414)),
        border = BorderStroke(1.dp, OldIvory.copy(alpha = 0.12f))
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
                    Text("Ordenado por", style = MaterialTheme.typography.bodyMedium, color = OldIvory)
                    FilterOptionRow("Título del libro", selected = true, radio = true)
                    FilterOptionRow("Autor", selected = false, radio = true)
                    FilterOptionRow("Hora de\nimportación", selected = false, radio = true)
                    FilterOptionRow("Carpetas", selected = false, radio = true)
                    FilterOptionRow("Lista reciente", selected = false, radio = true)
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
    radio: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp),
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
    LazyColumn(
        modifier = modifier.background(ShelfBackgroundBrush),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 96.dp),
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
}

@Composable
private fun DeviceLibraryGridView(
    modifier: Modifier,
    files: List<DeviceLibraryFile>,
    onOpenFile: (DeviceLibraryFile) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.background(ShelfBackgroundBrush),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 96.dp),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(178.dp)
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
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilePagePreview(
            file = file,
            modifier = Modifier
                .width(112.dp)
                .aspectRatio(0.68f)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = file.name.substringBeforeLast('.'),
                style = MaterialTheme.typography.titleMedium,
                color = OldIvory,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = file.location.substringAfterLast('/'),
                style = MaterialTheme.typography.bodySmall,
                color = OldIvory.copy(alpha = 0.68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            ReadingProgressBar(progress = progress)
            Text(
                text = "${progress}%",
                modifier = Modifier.align(Alignment.End),
                style = MaterialTheme.typography.bodySmall,
                color = OldIvory.copy(alpha = 0.78f)
            )
            Text(
                text = file.location,
                style = MaterialTheme.typography.bodySmall,
                color = OldIvory.copy(alpha = 0.56f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = null,
            tint = OldIvory.copy(alpha = 0.74f)
        )
    }
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
private fun ReadingProgressBar(progress: Int) {
    Box(
        modifier = Modifier
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
    Column(
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
