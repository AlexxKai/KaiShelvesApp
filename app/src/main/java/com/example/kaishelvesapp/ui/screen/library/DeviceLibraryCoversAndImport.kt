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

data class DefaultCoverOption(
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

val BuiltInDefaultCoverOptions: List<DefaultCoverOption> by lazy {
    R.drawable::class.java.fields
        .mapNotNull { field ->
            val name = field.name
            if (!isDefaultCoverResourceName(name)) return@mapNotNull null
            DefaultCoverOption(id = name, resourceId = field.getInt(null))
        }
        .sortedBy { it.id.lowercase(Locale.ROOT) }
}

var selectedDefaultCoverIdState by mutableStateOf<String?>(null)

@Composable
fun DefaultCoverScreen(
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
fun DefaultCoverTile(
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
fun RenameDefaultCoverDialog(
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
fun BackgroundImageSearchDialog(
    backgroundTreeUri: Uri?,
    title: String = "Imagen de fondo",
    searchQuery: String = "Imagen de fondo",
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
                        text = title,
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
                            val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
                            loadUrl("https://www.google.com/search?tbm=isch&q=$encodedQuery")
                        }
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFD9D9D9))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
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
                        modifier = if (selectedImageUrl == null) Modifier.fillMaxWidth() else Modifier,
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
fun ImportBooksDialog(
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
fun ImportDialogPathSelector(
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
fun ImportFolderBrowserDialog(
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
fun ImportBrowserEntryRow(
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
fun ImportDialogTextField(
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
fun ImportDialogArrowButton(
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
fun ImportOptionCheckbox(
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

data class ImportBrowserEntry(
    val name: String,
    val path: String,
    val documentId: String,
    val isDirectory: Boolean,
    val sizeBytes: Long?
)

fun loadImportBrowserEntries(
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

fun readableImportRootPath(treeUri: Uri): String {
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

fun isImportVisibleFile(name: String): Boolean {
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

fun importFileAccent(name: String): Color {
    val lowerName = name.lowercase(Locale.ROOT)
    return when {
        lowerName.endsWith(".pdf") -> Color(0xFFE83B16)
        lowerName.endsWith(".epub") -> Color(0xFF16AEEB)
        lowerName.endsWith(".fb2") -> Color(0xFF6D8DFF)
        lowerName.endsWith(".cbz") || lowerName.endsWith(".cbr") -> Color(0xFF8A5CF6)
        else -> Color(0xFF6E7781)
    }
}

fun isDefaultCoverResourceName(name: String): Boolean {
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

fun readDefaultCoverId(context: Context): String {
    val fallbackCoverId = BuiltInDefaultCoverOptions.firstOrNull()?.id.orEmpty()
    return context.getSharedPreferences(DEFAULT_COVER_PREFS, Context.MODE_PRIVATE)
        .getString(DEFAULT_COVER_KEY, fallbackCoverId)
        ?: fallbackCoverId
}

fun saveDefaultCoverId(context: Context, coverId: String) {
    selectedDefaultCoverIdState = coverId
    context.getSharedPreferences(DEFAULT_COVER_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(DEFAULT_COVER_KEY, coverId)
        .apply()
}

fun findDefaultCoverOption(context: Context, coverId: String): DefaultCoverOption? {
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

fun canOpenContentUri(context: Context, uri: Uri): Boolean {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { true } == true
    }.getOrDefault(false)
}

fun loadDownloadedDefaultCovers(context: Context, treeUri: Uri?): List<DefaultCoverOption> {
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

fun saveImageUriToDefaultCovers(context: Context, uri: Uri, treeUri: Uri?): DefaultCoverOption? {
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

fun downloadImageToDefaultCovers(context: Context, imageUrl: String, treeUri: Uri?): DefaultCoverOption? {
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

fun loadTreeDefaultCovers(context: Context, treeUri: Uri): List<DefaultCoverOption> {
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

fun createImageInTree(
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

fun createImageBytesInTree(
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

fun renameDefaultCover(
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

fun deleteDefaultCover(context: Context, cover: DefaultCoverOption): Boolean {
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

fun readDefaultCoverStorageTreeUri(context: Context): String? {
    return context.getSharedPreferences(DEFAULT_COVER_PREFS, Context.MODE_PRIVATE)
        .getString(DEFAULT_COVER_STORAGE_TREE_KEY, null)
}

fun saveDefaultCoverStorageTreeUri(context: Context, treeUri: Uri) {
    context.getSharedPreferences(DEFAULT_COVER_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(DEFAULT_COVER_STORAGE_TREE_KEY, treeUri.toString())
        .apply()
}

fun mimeTypeForImageName(fileName: String): String {
    return when (fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }
}

fun defaultCoversDir(context: Context): File {
    return File(context.filesDir, "backgrounds").apply { mkdirs() }
}

const val DEFAULT_COVER_PREFS = "device_library_default_cover"
const val DEFAULT_COVER_KEY = "selected_cover"
const val DEFAULT_COVER_STORAGE_TREE_KEY = "storage_tree_uri"
const val BOOK_METADATA_PREFS = "device_library_book_metadata"
const val PINNED_BOOKS_PREFS = "device_library_pinned_books"
const val PINNED_BOOKS_KEY = "ordered_book_ids"
const val LIBRARY_VIEW_PREFS = "device_library_view"
const val LIBRARY_LAYOUT_MODE_KEY = "layout_mode"
val imageExtensions = setOf("jpg", "jpeg", "png", "webp")

fun readDeviceBookUserMetadata(context: Context, file: DeviceLibraryFile): DeviceBookUserMetadata {
    val prefs = context.getSharedPreferences(BOOK_METADATA_PREFS, Context.MODE_PRIVATE)
    val key = deviceBookMetadataKey(file)
    return DeviceBookUserMetadata(
        title = prefs.getString("${key}_title", "").orEmpty(),
        author = prefs.getString("${key}_author", "").orEmpty(),
        description = prefs.getString("${key}_description", "").orEmpty(),
        coverText = prefs.getString("${key}_cover_text", "").orEmpty(),
        coverId = prefs.getString("${key}_cover_id", "").orEmpty(),
        favorite = prefs.getBoolean("${key}_favorite", false),
        category = prefs.getString("${key}_category", "").orEmpty(),
        series = prefs.getString("${key}_series", "").orEmpty(),
        tags = prefs.getString("${key}_tags", "").orEmpty()
    )
}

fun saveDeviceBookUserMetadata(
    context: Context,
    file: DeviceLibraryFile,
    metadata: DeviceBookUserMetadata
) {
    val key = deviceBookMetadataKey(file)
    context.getSharedPreferences(BOOK_METADATA_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString("${key}_title", metadata.title)
        .putString("${key}_author", metadata.author)
        .putString("${key}_description", metadata.description)
        .putString("${key}_cover_text", metadata.coverText)
        .putString("${key}_cover_id", metadata.coverId)
        .putBoolean("${key}_favorite", metadata.favorite)
        .putString("${key}_category", metadata.category)
        .putString("${key}_series", metadata.series)
        .putString("${key}_tags", metadata.tags)
        .apply()
}

fun deviceBookMetadataKey(file: DeviceLibraryFile): String {
    return file.uri.toString().hashCode().toUInt().toString(16)
}

fun readPinnedDeviceBookIds(context: Context): List<String> {
    return context.getSharedPreferences(PINNED_BOOKS_PREFS, Context.MODE_PRIVATE)
        .getString(PINNED_BOOKS_KEY, "")
        .orEmpty()
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .toList()
}

fun savePinnedDeviceBookIds(context: Context, pinnedBookIds: List<String>) {
    context.getSharedPreferences(PINNED_BOOKS_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(PINNED_BOOKS_KEY, pinnedBookIds.distinct().joinToString("\n"))
        .apply()
}

fun deviceBookPinId(file: DeviceLibraryFile): String {
    return file.uri.toString()
}

fun readDeviceLibraryLayoutMode(context: Context): DeviceLibraryLayoutMode {
    val savedName = context.getSharedPreferences(LIBRARY_VIEW_PREFS, Context.MODE_PRIVATE)
        .getString(LIBRARY_LAYOUT_MODE_KEY, DeviceLibraryLayoutMode.List.name)
    return DeviceLibraryLayoutMode.entries.firstOrNull { it.name == savedName }
        ?: DeviceLibraryLayoutMode.List
}

fun saveDeviceLibraryLayoutMode(context: Context, layoutMode: DeviceLibraryLayoutMode) {
    context.getSharedPreferences(LIBRARY_VIEW_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(LIBRARY_LAYOUT_MODE_KEY, layoutMode.name)
        .apply()
}

fun Set<String>.toggleItem(item: String, checked: Boolean): Set<String> {
    return if (checked) this + item else this - item
}


