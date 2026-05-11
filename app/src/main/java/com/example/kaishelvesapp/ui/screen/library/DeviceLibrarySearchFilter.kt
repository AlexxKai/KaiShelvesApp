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

@Composable
fun LibrarySelectorMenu(fileCount: Int) {
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
fun DeviceLibrarySearchOverlay(
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
fun DeviceLibrarySearchPanel(
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
                                contentDescription = "Eliminar búsqueda reciente",
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
fun DeviceLibraryFilterOverlay(
    layoutMode: DeviceLibraryLayoutMode,
    sortOption: DeviceLibrarySortOption,
    sortDescending: Boolean,
    selectedFileTypes: Set<DeviceLibraryFileTypeFilter>,
    onLayoutModeChange: (DeviceLibraryLayoutMode) -> Unit,
    onSortOptionChange: (DeviceLibrarySortOption) -> Unit,
    onToggleSortDirection: () -> Unit,
    onFileTypesChange: (Set<DeviceLibraryFileTypeFilter>) -> Unit,
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
            selectedFileTypes = selectedFileTypes,
            onLayoutModeChange = onLayoutModeChange,
            onSortOptionChange = onSortOptionChange,
            onToggleSortDirection = onToggleSortDirection,
            onFileTypesChange = onFileTypesChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .widthIn(max = 520.dp)
                .clickable(onClick = { })
        )
    }
}

@Composable
fun DeviceLibraryFilterPanel(
    layoutMode: DeviceLibraryLayoutMode,
    sortOption: DeviceLibrarySortOption,
    sortDescending: Boolean,
    selectedFileTypes: Set<DeviceLibraryFileTypeFilter>,
    onLayoutModeChange: (DeviceLibraryLayoutMode) -> Unit,
    onSortOptionChange: (DeviceLibrarySortOption) -> Unit,
    onToggleSortDirection: () -> Unit,
    onFileTypesChange: (Set<DeviceLibraryFileTypeFilter>) -> Unit,
    modifier: Modifier = Modifier
) {
    var fileTypesExpanded by remember { mutableStateOf(selectedFileTypes.isNotEmpty()) }
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
                    stringResource(R.string.device_library_file_type),
                    modifier = Modifier.widthIn(min = 118.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldIvory
                )
                Checkbox(
                    checked = selectedFileTypes.isEmpty() && !fileTypesExpanded,
                    onCheckedChange = { checked ->
                        if (checked) {
                            fileTypesExpanded = false
                            onFileTypesChange(emptySet())
                        } else {
                            fileTypesExpanded = true
                        }
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF9EAFDF),
                        checkmarkColor = OldIvory,
                        uncheckedColor = OldIvory.copy(alpha = 0.72f)
                    )
                )
                Text(
                    text = stringResource(R.string.device_library_file_type_all),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldIvory
                )
            }

            if (fileTypesExpanded) {
                FileTypeOptionsGrid(
                    selectedFileTypes = selectedFileTypes,
                    onFileTypesChange = onFileTypesChange
                )
            }
        }
    }
}

@Composable
fun FileTypeOptionsGrid(
    selectedFileTypes: Set<DeviceLibraryFileTypeFilter>,
    onFileTypesChange: (Set<DeviceLibraryFileTypeFilter>) -> Unit
) {
    val options = remember { DeviceLibraryFileTypeFilter.entries }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(2.dp, BloodWine), RoundedCornerShape(0.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.chunked(3).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowOptions.forEach { option ->
                    FileTypeOption(
                        label = stringResource(option.labelRes),
                        selected = option in selectedFileTypes,
                        onClick = {
                            val updatedTypes = if (option in selectedFileTypes) {
                                selectedFileTypes - option
                            } else {
                                selectedFileTypes + option
                            }
                            onFileTypesChange(updatedTypes)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowOptions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun FileTypeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(30.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onClick() },
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF9EAFDF),
                checkmarkColor = OldIvory,
                uncheckedColor = OldIvory.copy(alpha = 0.72f)
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OldIvory,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FilterOptionRow(
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
fun LayoutSegmentButton(
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



