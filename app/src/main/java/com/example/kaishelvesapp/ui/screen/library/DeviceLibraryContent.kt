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
fun DeviceLibraryContent(
    modifier: Modifier,
    files: List<DeviceLibraryFile>,
    isLoading: Boolean,
    errorMessage: String?,
    layoutMode: DeviceLibraryLayoutMode,
    progressRevision: Int,
    onOpenFile: (DeviceLibraryFile) -> Unit
) {
    val context = LocalContext.current
    var pinnedBookIds by remember { mutableStateOf(readPinnedDeviceBookIds(context)) }
    val progressByFile = remember(files, progressRevision) {
        files.associate { file -> file.uri.toString() to readDeviceBookProgressPercent(context, file) }
    }
    val displayedFiles = remember(files, pinnedBookIds) {
        val filesById = files.associateBy(::deviceBookPinId)
        val pinnedFiles = pinnedBookIds.mapNotNull(filesById::get)
        pinnedFiles + files.filterNot { deviceBookPinId(it) in pinnedBookIds }
    }

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
            displayedFiles = displayedFiles,
            pinnedBookIds = pinnedBookIds,
            onPinnedBookIdsChange = { pinnedBookIds = it },
            progressByFile = progressByFile,
            onOpenFile = onOpenFile
        )
        DeviceLibraryLayoutMode.Grid -> DeviceLibraryGridView(
            modifier = modifier,
            files = displayedFiles,
            pinnedBookIds = pinnedBookIds,
            onPinnedBookIdsChange = { pinnedBookIds = it },
            progressByFile = progressByFile,
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
fun DeviceLibraryListView(
    modifier: Modifier,
    files: List<DeviceLibraryFile>,
    displayedFiles: List<DeviceLibraryFile>,
    pinnedBookIds: List<String>,
    onPinnedBookIdsChange: (List<String>) -> Unit,
    progressByFile: Map<String, Int>,
    onOpenFile: (DeviceLibraryFile) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    Box(modifier = modifier.background(ShelfBackgroundBrush)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 28.dp, top = 18.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(displayedFiles, key = { it.uri.toString() }) { file ->
                val fileId = deviceBookPinId(file)
                ShelfListRow(
                    file = file,
                    progress = progressByFile[file.uri.toString()] ?: 0,
                    isPinned = fileId in pinnedBookIds,
                    onTogglePin = {
                        if (fileId in pinnedBookIds) {
                            val updatedPinnedBookIds = pinnedBookIds - fileId
                            onPinnedBookIdsChange(updatedPinnedBookIds)
                            savePinnedDeviceBookIds(context, updatedPinnedBookIds)
                            val updatedDisplayedFiles = updatedPinnedBookIds
                                .mapNotNull { pinnedId -> files.firstOrNull { deviceBookPinId(it) == pinnedId } } +
                                files.filterNot { deviceBookPinId(it) in updatedPinnedBookIds }
                            val targetIndex = updatedDisplayedFiles.indexOfFirst { deviceBookPinId(it) == fileId }
                                .coerceAtLeast(0)
                            scope.launch {
                                listState.animateScrollToItem(targetIndex)
                            }
                        } else {
                            val updatedPinnedBookIds = listOf(fileId) + pinnedBookIds.filterNot { it == fileId }
                            onPinnedBookIdsChange(updatedPinnedBookIds)
                            savePinnedDeviceBookIds(context, updatedPinnedBookIds)
                            scope.launch {
                                listState.animateScrollToItem(0)
                            }
                        }
                    },
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
fun DeviceLibraryGridView(
    modifier: Modifier,
    files: List<DeviceLibraryFile>,
    pinnedBookIds: List<String>,
    onPinnedBookIdsChange: (List<String>) -> Unit,
    progressByFile: Map<String, Int>,
    onOpenFile: (DeviceLibraryFile) -> Unit
) {
    val context = LocalContext.current
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
                val fileId = deviceBookPinId(file)
                ShelfGridBook(
                    file = file,
                    progress = progressByFile[file.uri.toString()] ?: 0,
                    isPinned = fileId in pinnedBookIds,
                    onTogglePin = {
                        val updatedPinnedBookIds = if (fileId in pinnedBookIds) {
                            pinnedBookIds - fileId
                        } else {
                            listOf(fileId) + pinnedBookIds.filterNot { it == fileId }
                        }
                        onPinnedBookIdsChange(updatedPinnedBookIds)
                        savePinnedDeviceBookIds(context, updatedPinnedBookIds)
                    },
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
fun DeviceLibraryLazyListScrollbar(
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
fun DeviceLibraryLazyGridScrollbar(
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
fun DeviceLibraryFastScrollbar(
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
fun DeviceLibraryCarouselView(
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
fun ShelfListRow(
    file: DeviceLibraryFile,
    progress: Int,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val bookMetadata by produceState<DeviceBookDisplayMetadata?>(initialValue = null, file.uri) {
        value = withContext(Dispatchers.IO) {
            if (isEpub(file)) extractEpubDisplayMetadata(context, file.uri) else null
        }
    }
    var userMetadata by remember(file.uri) { mutableStateOf(readDeviceBookUserMetadata(context, file)) }
    val title = userMetadata.title.takeIf { it.isNotBlank() }
        ?: bookMetadata?.title?.takeIf { it.isNotBlank() }
        ?: file.name.substringBeforeLast('.')
    val author = userMetadata.author.takeIf { it.isNotBlank() } ?: bookMetadata?.author.orEmpty()
    val summary = userMetadata.description.takeIf { it.isNotBlank() } ?: bookMetadata?.description.orEmpty()
    var showBookOptions by remember(file.uri) { mutableStateOf(false) }
    var showBookInfoDialog by remember(file.uri) { mutableStateOf(false) }
    var showCoverDownload by remember(file.uri) { mutableStateOf(false) }
    val backgroundTreeUri = remember { readDefaultCoverStorageTreeUri(context)?.let(Uri::parse) }

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
                coverText = userMetadata.coverText,
                overrideCoverId = userMetadata.coverId.takeIf { it.isNotBlank() },
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
                            isPinned = isPinned,
                            onDismiss = { showBookOptions = false },
                            onTogglePin = {
                                showBookOptions = false
                                onTogglePin()
                            },
                            onShowBookInfo = {
                                showBookOptions = false
                                showBookInfoDialog = true
                            },
                            onDownloadCover = {
                                showBookOptions = false
                                showCoverDownload = true
                            }
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

        if (showBookInfoDialog) {
            DeviceLibraryBookInfoDialog(
                file = file,
                metadata = bookMetadata,
                initialUserMetadata = userMetadata,
                onSave = { savedMetadata -> userMetadata = savedMetadata },
                onDismiss = { showBookInfoDialog = false }
            )
        }

        if (showCoverDownload) {
            val search = listOf(title, author, "portada libro")
                .filter { it.isNotBlank() }
                .joinToString(" ")
            BackgroundImageSearchDialog(
                backgroundTreeUri = backgroundTreeUri,
                title = "Descargar portada",
                searchQuery = search,
                onDismiss = { showCoverDownload = false },
                onImageSaved = { savedCover ->
                    val savedMetadata = userMetadata.copy(coverId = savedCover.id)
                    saveDeviceBookUserMetadata(context, file, savedMetadata)
                    userMetadata = savedMetadata
                    showCoverDownload = false
                }
            )
        }
    }
}

@Composable
fun DeviceLibraryBookInfoDialog(
    file: DeviceLibraryFile,
    metadata: DeviceBookDisplayMetadata?,
    initialUserMetadata: DeviceBookUserMetadata,
    onSave: (DeviceBookUserMetadata) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormatter = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    val title = initialUserMetadata.title.takeIf { it.isNotBlank() }
        ?: metadata?.title?.takeIf { it.isNotBlank() }
        ?: file.name.substringBeforeLast('.')
    val author = initialUserMetadata.author.takeIf { it.isNotBlank() }
        ?: metadata?.author?.takeIf { it.isNotBlank() }
        ?: "Autor desconocido"
    val description = initialUserMetadata.description.takeIf { it.isNotBlank() } ?: metadata?.description.orEmpty()
    val importedAt = file.modifiedAtMillis?.takeIf { it > 0 }?.let { dateFormatter.format(Date(it)) }
        ?: "Sin fecha disponible"
    val sizeText = file.sizeBytes?.let { Formatter.formatShortFileSize(context, it) }
    val pathText = readableDeviceBookPath(file, sizeText)
    val scrollState = rememberScrollState()
    val backgroundTreeUri = remember { readDefaultCoverStorageTreeUri(context)?.let(Uri::parse) }
    var showCoverDownload by remember(file.uri) { mutableStateOf(false) }
    var favorite by remember(file.uri) { mutableStateOf(initialUserMetadata.favorite) }
    var editableTitle by remember(file.uri) { mutableStateOf(title) }
    var editableAuthor by remember(file.uri) { mutableStateOf(author) }
    var editableDescription by remember(file.uri) { mutableStateOf(description) }
    var coverText by remember(file.uri) { mutableStateOf(initialUserMetadata.coverText) }
    var coverId by remember(file.uri) { mutableStateOf(initialUserMetadata.coverId) }
    var category by remember(file.uri) { mutableStateOf(initialUserMetadata.category) }
    var series by remember(file.uri) { mutableStateOf(initialUserMetadata.series) }
    var tags by remember(file.uri) { mutableStateOf(initialUserMetadata.tags) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF171717))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = OldIvory
                    )
                }
                Text(
                    text = "InformaciÃ³n del libro",
                    style = MaterialTheme.typography.titleLarge,
                    color = OldIvory,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    FilePagePreview(
                        file = file,
                        coverText = coverText,
                        overrideCoverId = coverId.takeIf { it.isNotBlank() },
                        modifier = Modifier
                            .width(122.dp)
                            .aspectRatio(0.68f)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        DeviceBookInfoField(
                            label = "TÃ­tulo del libro",
                            value = editableTitle,
                            onValueChange = { editableTitle = it },
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                color = OldIvory,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        DeviceBookInfoField(
                            label = "Autor del libro",
                            value = editableAuthor,
                            onValueChange = { editableAuthor = it },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Buscar autor",
                                    tint = OldIvory.copy(alpha = 0.72f)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    DeviceBookInfoField(
                        label = "Texto de la portada (opcional)",
                        value = coverText,
                        onValueChange = { coverText = it },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    DeviceBookInfoSmallButton(text = "Seleccionar portada")
                    Spacer(modifier = Modifier.width(8.dp))
                    DeviceBookInfoSmallButton(
                        text = "Descargar...",
                        onClick = { showCoverDownload = true }
                    )
                }

                Spacer(modifier = Modifier.height(34.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(5) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = OldIvory.copy(alpha = 0.18f),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                DeviceBookInfoField(
                    label = "DescripciÃ³n",
                    value = editableDescription,
                    onValueChange = { editableDescription = it },
                    minLines = 6,
                    maxLines = Int.MAX_VALUE
                )

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = favorite,
                        onCheckedChange = { favorite = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = TarnishedGold,
                            uncheckedColor = OldIvory.copy(alpha = 0.72f),
                            checkmarkColor = Color.Black
                        )
                    )
                    Text(
                        text = "Favorito",
                        modifier = Modifier.width(92.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OldIvory
                    )
                    DeviceBookInfoField(
                        label = "CategorÃ­a (opcional)",
                        value = category,
                        onValueChange = { category = it },
                        modifier = Modifier.weight(1f),
                        trailingIcon = { DeviceBookInfoDropDownIcon() }
                    )
                }

                DeviceBookInfoField(
                    label = "Serie",
                    value = series,
                    onValueChange = { series = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp)
                )

                DeviceBookInfoField(
                    label = "Etiquetas",
                    value = tags,
                    onValueChange = { tags = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp),
                    trailingIcon = { DeviceBookInfoDropDownIcon() }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtro de lectura",
                        modifier = Modifier.width(122.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OldIvory.copy(alpha = 0.76f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Auto",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = OldIvory,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = OldIvory.copy(alpha = 0.28f)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = pathText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldIvory.copy(alpha = 0.86f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Hora de importaciÃ³n: $importedAt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldIvory.copy(alpha = 0.86f),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DeviceBookInfoBottomButton(
                    text = "Cancelar",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                DeviceBookInfoBottomButton(
                    text = "Guardar",
                    onClick = {
                        val savedMetadata = DeviceBookUserMetadata(
                            title = editableTitle.trim(),
                            author = editableAuthor.trim(),
                            description = editableDescription.trim(),
                            coverText = coverText.trim(),
                            coverId = coverId,
                            favorite = favorite,
                            category = category.trim(),
                            series = series.trim(),
                            tags = tags.trim()
                        )
                        saveDeviceBookUserMetadata(context, file, savedMetadata)
                        onSave(savedMetadata)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showCoverDownload) {
        val search = listOf(editableTitle, editableAuthor, "portada libro")
            .filter { it.isNotBlank() }
            .joinToString(" ")
        BackgroundImageSearchDialog(
            backgroundTreeUri = backgroundTreeUri,
            title = "Descargar portada",
            searchQuery = search,
            onDismiss = { showCoverDownload = false },
            onImageSaved = { savedCover ->
                coverId = savedCover.id
                showCoverDownload = false
            }
        )
    }
}

@Composable
fun DeviceBookInfoField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium.copy(
        color = OldIvory,
        fontWeight = FontWeight.SemiBold
    ),
    minLines: Int = 1,
    maxLines: Int = if (minLines > 1) 5 else 2,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = OldIvory.copy(alpha = 0.66f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        textStyle = textStyle,
        minLines = minLines,
        maxLines = maxLines,
        trailingIcon = trailingIcon,
        colors = TextFieldDefaults.colors(
            focusedTextColor = OldIvory,
            unfocusedTextColor = OldIvory,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            cursorColor = TarnishedGold,
            focusedIndicatorColor = OldIvory.copy(alpha = 0.58f),
            unfocusedIndicatorColor = OldIvory.copy(alpha = 0.34f),
            focusedLabelColor = OldIvory.copy(alpha = 0.66f),
            unfocusedLabelColor = OldIvory.copy(alpha = 0.66f)
        )
    )
}

@Composable
fun DeviceBookInfoSmallButton(
    text: String,
    onClick: () -> Unit = {}
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .height(38.dp)
            .widthIn(min = 104.dp),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, OldIvory.copy(alpha = 0.7f)),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = OldIvory,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DeviceBookInfoBottomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(6.dp),
        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
            containerColor = Color(0xFF242424),
            contentColor = OldIvory
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun DeviceBookInfoDropDownIcon() {
    Box(
        modifier = Modifier
            .size(30.dp)
            .border(2.dp, OldIvory.copy(alpha = 0.7f), RoundedCornerShape(50)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = OldIvory.copy(alpha = 0.72f)
        )
    }
}

fun readableDeviceBookPath(file: DeviceLibraryFile, sizeText: String?): String {
    val location = file.location.trim('/').takeIf { it.isNotBlank() }
    val path = listOfNotNull("/sdcard", location, file.name).joinToString("/")
    return if (sizeText.isNullOrBlank()) path else "$path ($sizeText)"
}

@Composable
fun DeviceLibraryBookOptionsMenu(
    expanded: Boolean,
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onShowBookInfo: () -> Unit,
    onDownloadCover: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 244.dp, max = 304.dp),
        shape = RoundedCornerShape(2.dp),
        containerColor = Color(0xFF262626),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        DeviceLibraryBookOptionItem(
            text = if (isPinned) "Eliminar de la parte superior" else "Pin en la parte superior",
            onClick = onTogglePin
        )

        HorizontalDivider(color = OldIvory.copy(alpha = 0.16f))

        DeviceLibraryBookOptionItem(
            text = "InformaciÃ³n del libro",
            onClick = onShowBookInfo
        )
        DeviceLibraryBookOptionItem(
            text = "Descargar Portada de Libro",
            onClick = onDownloadCover
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
fun DeviceLibraryBookOptionItem(
    text: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = OldIvory,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        onClick = onClick,
        modifier = Modifier.height(46.dp)
    )
}

@Composable
fun ShelfGridBook(
    file: DeviceLibraryFile,
    progress: Int,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val bookMetadata by produceState<DeviceBookDisplayMetadata?>(initialValue = null, file.uri) {
        value = withContext(Dispatchers.IO) {
            if (isEpub(file)) extractEpubDisplayMetadata(context, file.uri) else null
        }
    }
    var userMetadata by remember(file.uri) { mutableStateOf(readDeviceBookUserMetadata(context, file)) }
    val title = userMetadata.title.takeIf { it.isNotBlank() }
        ?: bookMetadata?.title?.takeIf { it.isNotBlank() }
        ?: file.name.substringBeforeLast('.')
    val author = userMetadata.author.takeIf { it.isNotBlank() } ?: bookMetadata?.author.orEmpty()
    var showBookOptions by remember(file.uri) { mutableStateOf(false) }
    var showBookInfoDialog by remember(file.uri) { mutableStateOf(false) }
    var showCoverDownload by remember(file.uri) { mutableStateOf(false) }
    val backgroundTreeUri = remember { readDefaultCoverStorageTreeUri(context)?.let(Uri::parse) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.66f)
            .clickable(onClick = onClick)
    ) {
        FilePagePreview(
            file = file,
            coverText = userMetadata.coverText,
            overrideCoverId = userMetadata.coverId.takeIf { it.isNotBlank() },
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 2.dp, bottom = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .clickable { showBookOptions = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.more_option),
                    tint = OldIvory,
                    modifier = Modifier.size(18.dp)
                )
            }

            DeviceLibraryBookOptionsMenu(
                expanded = showBookOptions,
                isPinned = isPinned,
                onDismiss = { showBookOptions = false },
                onTogglePin = {
                    showBookOptions = false
                    onTogglePin()
                },
                onShowBookInfo = {
                    showBookOptions = false
                    showBookInfoDialog = true
                },
                onDownloadCover = {
                    showBookOptions = false
                    showCoverDownload = true
                }
            )
        }
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

        if (showBookInfoDialog) {
            DeviceLibraryBookInfoDialog(
                file = file,
                metadata = bookMetadata,
                initialUserMetadata = userMetadata,
                onSave = { savedMetadata -> userMetadata = savedMetadata },
                onDismiss = { showBookInfoDialog = false }
            )
        }

        if (showCoverDownload) {
            val search = listOf(title, author, "portada libro")
                .filter { it.isNotBlank() }
                .joinToString(" ")
            BackgroundImageSearchDialog(
                backgroundTreeUri = backgroundTreeUri,
                title = "Descargar portada",
                searchQuery = search,
                onDismiss = { showCoverDownload = false },
                onImageSaved = { savedCover ->
                    val savedMetadata = userMetadata.copy(coverId = savedCover.id)
                    saveDeviceBookUserMetadata(context, file, savedMetadata)
                    userMetadata = savedMetadata
                    showCoverDownload = false
                }
            )
        }
    }
}

@Composable
fun CarouselCover(
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
fun ReadingProgressBar(
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

val ShelfBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1F1F1F),
        Color(0xFF121212),
        Color(0xFF242424),
        Color(0xFF101010)
    )
)

fun Int.floorMod(other: Int): Int {
    return ((this % other) + other) % other
}

@Composable
fun DeviceFileRow(
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
fun FilePagePreview(
    file: DeviceLibraryFile,
    coverText: String = "",
    overrideCoverId: String? = null,
    modifier: Modifier = Modifier
        .width(48.dp)
        .aspectRatio(0.68f)
) {
    val context = LocalContext.current
    val overrideCover = remember(overrideCoverId) {
        overrideCoverId?.let { findDefaultCoverOption(context, it) }
    }
    val preview by produceState<Bitmap?>(initialValue = null, file.uri, overrideCoverId) {
        value = withContext(Dispatchers.IO) {
            if (overrideCoverId != null) return@withContext null
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
        when {
            overrideCover != null -> DefaultCoverOptionPreview(cover = overrideCover)
            preview != null -> {
                Image(
                    bitmap = preview!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> FallbackBookPreview(file = file)
        }

        if (coverText.isNotBlank()) {
            Text(
                text = coverText,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.54f))
                    .padding(horizontal = 5.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = OldIvory,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun DefaultCoverOptionPreview(cover: DefaultCoverOption) {
    val context = LocalContext.current
    val fileCoverBitmap by produceState<Bitmap?>(initialValue = null, cover.file) {
        value = withContext(Dispatchers.IO) {
            cover.file?.let { BitmapFactory.decodeFile(it.absolutePath) }
        }
    }
    val uriCoverBitmap by produceState<Bitmap?>(initialValue = null, cover.uri) {
        value = withContext(Dispatchers.IO) {
            cover.uri?.let { uri ->
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
                }.getOrNull()
            }
        }
    }

    when {
        cover.resourceId != null -> Image(
            painter = painterResource(cover.resourceId),
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
                .background(OldIvory.copy(alpha = 0.92f))
        )
    }
}

@Composable
fun FallbackBookPreview(file: DeviceLibraryFile) {
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
fun DeviceLibraryMessage(
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


