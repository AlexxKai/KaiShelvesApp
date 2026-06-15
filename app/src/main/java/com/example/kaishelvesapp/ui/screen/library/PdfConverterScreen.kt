package com.example.kaishelvesapp.ui.screen.library

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Html
import android.text.Layout
import android.text.Spanned
import android.text.SpannedString
import android.text.StaticLayout
import android.text.TextPaint
import android.provider.OpenableColumns
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.repository.DeviceLibraryFile
import com.example.kaishelvesapp.data.repository.DeviceLibraryRepository
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiNavigationDrawerContent
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import java.util.Locale
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

private const val IMAGE_PDF_MAX_PAGE_SIDE = 4096
private const val PDF_EPUB_RENDER_SCALE = 2

@Composable
fun PdfConverterScreen(
    userName: String?,
    profileImageUrl: String?,
    onGoToProfile: () -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit,
    pendingRequestCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onSectionSelected: (KaiSection) -> Unit
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerExpanded = drawerState.targetValue == DrawerValue.Open || drawerState.currentValue == DrawerValue.Open
    val scope = rememberCoroutineScope()
    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf("") }
    var sourceFormat by remember { mutableStateOf<ConverterInputFormat?>(null) }
    var isConverting by remember { mutableStateOf(false) }
    var activeOutputFormat by remember { mutableStateOf<ConverterOutputFormat?>(null) }
    var selectedOutputFormat by remember { mutableStateOf(ConverterOutputFormat.Pdf) }
    var generatedUri by remember { mutableStateOf<Uri?>(null) }
    var generatedMimeType by remember { mutableStateOf("") }
    var generatedName by remember { mutableStateOf("") }
    var previewFile by remember { mutableStateOf<DeviceLibraryFile?>(null) }
    var showAddGeneratedBookDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pendingOnlineOutputFormat by remember { mutableStateOf<ConverterOutputFormat?>(null) }

    val sourceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            sourceUri = uri
            sourceName = displayNameForUri(context, uri)
            sourceFormat = detectConverterInputFormat(context, uri, sourceName)
            statusMessage = null
            pendingOnlineOutputFormat = null
            generatedUri = null
            generatedMimeType = ""
            generatedName = ""
        }
    }
    val outputLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        val inputUri = sourceUri
        if (targetUri == null || inputUri == null) return@rememberLauncherForActivityResult

        scope.launch {
            isConverting = true
            activeOutputFormat = ConverterOutputFormat.Pdf
            statusMessage = null
            val inputFormat = sourceFormat ?: detectConverterInputFormat(context, inputUri, displayNameForUri(context, inputUri))
            if (inputFormat == null) {
                isConverting = false
                activeOutputFormat = null
                statusMessage = context.getString(R.string.converter_unsupported_conversion)
                Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                convertDocumentToPdf(context, inputUri, targetUri, inputFormat)
            }
            isConverting = false
            activeOutputFormat = null
            statusMessage = if (result.isSuccess) {
                generatedUri = targetUri
                generatedMimeType = ConverterOutputFormat.Pdf.mimeType
                generatedName = displayNameForUri(context, targetUri)
                context.getString(R.string.pdf_converter_success)
            } else {
                generatedUri = null
                generatedMimeType = ""
                generatedName = ""
                result.exceptionOrNull()?.localizedMessage ?: context.getString(R.string.pdf_converter_error)
            }
            Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
        }
    }
    val epubOutputLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/epub+zip")
    ) { targetUri ->
        val inputUri = sourceUri
        if (targetUri == null || inputUri == null) return@rememberLauncherForActivityResult

        scope.launch {
            isConverting = true
            activeOutputFormat = ConverterOutputFormat.Epub
            statusMessage = null
            val inputFormat = sourceFormat ?: detectConverterInputFormat(context, inputUri, displayNameForUri(context, inputUri))
            if (inputFormat == null) {
                isConverting = false
                activeOutputFormat = null
                statusMessage = context.getString(R.string.converter_unsupported_conversion)
                Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                convertDocumentToEpub(context, inputUri, targetUri, inputFormat)
            }
            isConverting = false
            activeOutputFormat = null
            statusMessage = if (result.isSuccess) {
                generatedUri = targetUri
                generatedMimeType = ConverterOutputFormat.Epub.mimeType
                generatedName = displayNameForUri(context, targetUri)
                context.getString(R.string.epub_converter_success)
            } else {
                generatedUri = null
                generatedMimeType = ""
                generatedName = ""
                result.exceptionOrNull()?.localizedMessage ?: context.getString(R.string.epub_converter_error)
            }
            Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KaiNavigationDrawerContent(
                currentSection = KaiSection.FILE_CONVERTER,
                subtitle = stringResource(R.string.pdf_converter_subtitle),
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
            containerColor = Obsidian,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(
                            Brush.verticalGradient(
                                listOf(DeepWalnut.copy(alpha = 0.98f), Obsidian.copy(alpha = 0.96f))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.open_navigation_menu),
                            tint = TarnishedGold
                        )
                    }
                    Text(
                        text = stringResource(R.string.file_converter),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        color = TarnishedGold,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            bottomBar = {
                KaiBottomBar(
                    current = KaiSection.FILE_CONVERTER,
                    onSelect = onSectionSelected
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1F1F1F), Color(0xFF101010))
                        )
                    )
                    .padding(18.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepWalnut.copy(alpha = 0.94f)),
                    border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = null,
                            tint = TarnishedGold
                        )
                        Text(
                            text = stringResource(R.string.pdf_converter_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = TarnishedGold
                        )
                        Text(
                            text = stringResource(R.string.pdf_converter_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OldIvory
                        )

                        if (sourceName.isNotBlank()) {
                            val detectedFormatName = sourceFormat
                                ?.let { stringResource(it.labelRes) }
                                ?: stringResource(R.string.converter_format_unknown)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Description,
                                        contentDescription = null,
                                        tint = OldIvory
                                    )
                                    Text(
                                        text = stringResource(R.string.converter_selected_file, sourceName),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 10.dp),
                                        color = OldIvory,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.converter_detected_type, detectedFormatName),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OldIvory.copy(alpha = 0.78f)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                sourceLauncher.launch(
                                    arrayOf(
                                        "text/plain",
                                        "text/markdown",
                                        "text/html",
                                        "application/pdf",
                                        "application/epub+zip",
                                        "application/xml",
                                        "application/xhtml+xml",
                                        "image/jpeg",
                                        "image/png",
                                        "image/webp",
                                        "*/*"
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TarnishedGold),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(stringResource(R.string.pdf_converter_choose_file), color = Obsidian)
                        }

                        Text(
                            text = stringResource(R.string.converter_output_format),
                            style = MaterialTheme.typography.labelLarge,
                            color = TarnishedGold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ConverterFormatOption(
                                format = ConverterOutputFormat.Pdf,
                                selected = selectedOutputFormat == ConverterOutputFormat.Pdf,
                                enabled = !isConverting,
                                onSelect = { selectedOutputFormat = ConverterOutputFormat.Pdf }
                            )
                            ConverterFormatOption(
                                format = ConverterOutputFormat.Epub,
                                selected = selectedOutputFormat == ConverterOutputFormat.Epub,
                                enabled = !isConverting,
                                onSelect = { selectedOutputFormat = ConverterOutputFormat.Epub }
                            )
                        }
                        Text(
                            text = stringResource(
                                R.string.converter_selected_output,
                                stringResource(selectedOutputFormat.labelRes)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = OldIvory.copy(alpha = 0.78f)
                        )

                        val conversionSupported = sourceFormat?.supports(selectedOutputFormat) ?: false
                        val canConvertOrOfferOnline = sourceUri != null && !isConverting
                        if (sourceUri != null && !conversionSupported) {
                            Text(
                                text = stringResource(R.string.converter_online_fallback_available),
                                style = MaterialTheme.typography.bodySmall,
                                color = OldIvory.copy(alpha = 0.78f)
                            )
                        }

                        Button(
                            enabled = canConvertOrOfferOnline,
                            onClick = {
                                val outputName = sourceName.substringBeforeLast('.', missingDelimiterValue = sourceName)
                                    .ifBlank { "kai-shelves" } + selectedOutputFormat.extension
                                if (conversionSupported) {
                                    when (selectedOutputFormat) {
                                        ConverterOutputFormat.Pdf -> outputLauncher.launch(outputName)
                                        ConverterOutputFormat.Epub -> epubOutputLauncher.launch(outputName)
                                    }
                                } else {
                                    // Unsupported formats stay local until the user explicitly confirms opening the external site.
                                    pendingOnlineOutputFormat = selectedOutputFormat
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedOutputFormat == ConverterOutputFormat.Pdf) {
                                    OldIvory.copy(alpha = 0.88f)
                                } else {
                                    Obsidian.copy(alpha = 0.88f)
                                }
                            ),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            if (activeOutputFormat != null) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = if (selectedOutputFormat == ConverterOutputFormat.Pdf) OldIvory else Obsidian
                                )
                            } else {
                                Text(
                                    text = stringResource(
                                        if (conversionSupported) {
                                            selectedOutputFormat.actionLabelRes
                                        } else {
                                            R.string.converter_open_online_converter
                                        }
                                    ),
                                    color = if (selectedOutputFormat == ConverterOutputFormat.Pdf) TarnishedGold else TarnishedGold
                                )
                            }
                        }

                        generatedUri?.let { uri ->
                            Button(
                                onClick = {
                                    previewFile = generatedDeviceLibraryFile(
                                        context = context,
                                        uri = uri,
                                        fallbackName = generatedName,
                                        fallbackMimeType = generatedMimeType
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepWalnut.copy(alpha = 0.86f)),
                                border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.65f)),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Text(stringResource(R.string.converter_open_generated_file), color = OldIvory)
                            }
                            if (generatedName.isNotBlank()) {
                                Text(
                                    text = generatedName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OldIvory.copy(alpha = 0.78f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        statusMessage?.let {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OldIvory
                            )
                        }
                    }
                }

                previewFile?.let { file ->
                    DeviceBookReaderDialog(
                        file = file,
                        onProgressChanged = {},
                        onDismiss = {
                            previewFile = null
                            showAddGeneratedBookDialog = true
                        }
                    )
                }

                if (showAddGeneratedBookDialog) {
                    AddGeneratedBookDialog(
                        fileName = generatedName.ifBlank {
                            stringResource(R.string.converter_generated_file_fallback_name)
                        },
                        location = generatedUri?.toString().orEmpty(),
                        onDismiss = { showAddGeneratedBookDialog = false },
                        onConfirm = {
                            val uri = generatedUri ?: return@AddGeneratedBookDialog
                            DeviceLibraryRepository(context).upsertBookRecord(
                                generatedDeviceLibraryFile(
                                    context = context,
                                    uri = uri,
                                    fallbackName = generatedName,
                                    fallbackMimeType = generatedMimeType
                                )
                            )
                            showAddGeneratedBookDialog = false
                            Toast.makeText(
                                context,
                                context.getString(R.string.converter_generated_file_added),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                pendingOnlineOutputFormat?.let { outputFormat ->
                    OnlineConverterNoticeDialog(
                        outputFormat = outputFormat,
                        onDismiss = { pendingOnlineOutputFormat = null },
                        onConfirm = {
                            val targetUrl = onlineConverterUrl(outputFormat)
                            pendingOnlineOutputFormat = null
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, targetUrl.toUri()))
                            }.onFailure {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.no_app_to_open_file),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }

            }
        }
    }
}

@Composable
private fun OnlineConverterNoticeDialog(
    outputFormat: ConverterOutputFormat,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    R.string.converter_online_notice_title,
                    stringResource(outputFormat.labelRes)
                )
            )
        },
        text = {
            Text(text = stringResource(R.string.converter_online_notice_body))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.converter_open_online_converter))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ConverterFormatOption(
    format: ConverterOutputFormat,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = TarnishedGold,
                unselectedColor = OldIvory.copy(alpha = 0.72f),
                disabledSelectedColor = TarnishedGold.copy(alpha = 0.45f),
                disabledUnselectedColor = OldIvory.copy(alpha = 0.35f)
            )
        )
        Text(
            text = stringResource(format.labelRes),
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) OldIvory else OldIvory.copy(alpha = 0.48f)
        )
    }
}

@Composable
private fun AddGeneratedBookDialog(
    fileName: String,
    location: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.converter_add_generated_file_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = stringResource(R.string.converter_add_generated_file_body))
                Text(
                    text = location.ifBlank { fileName },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

private fun onlineConverterUrl(outputFormat: ConverterOutputFormat): String {
    // The app only opens the selected converter page; files are never uploaded automatically.
    return when (outputFormat) {
        ConverterOutputFormat.Pdf -> "https://documento.online-convert.com/es/convertir-a-pdf"
        ConverterOutputFormat.Epub -> "https://ebook.online-convert.com/es/convertir-a-epub"
    }
}

private fun generatedDeviceLibraryFile(
    context: Context,
    uri: Uri,
    fallbackName: String,
    fallbackMimeType: String
): DeviceLibraryFile {
    var name = fallbackName.ifBlank { displayNameForUri(context, uri) }
    var size: Long? = null
    var mimeType = fallbackMimeType.ifBlank { context.contentResolver.getType(uri).orEmpty() }
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) name = cursor.getString(nameIndex).orEmpty().ifBlank { name }
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
    }
    if (mimeType.isBlank()) {
        mimeType = when {
            name.endsWith(".pdf", ignoreCase = true) -> ConverterOutputFormat.Pdf.mimeType
            name.endsWith(".epub", ignoreCase = true) -> ConverterOutputFormat.Epub.mimeType
            else -> ""
        }
    }
    return DeviceLibraryFile(
        name = name.ifBlank { "kai-shelves" },
        location = context.getString(R.string.file_converter),
        mimeType = mimeType.ifBlank { null },
        sizeBytes = size,
        modifiedAtMillis = System.currentTimeMillis(),
        uri = uri
    )
}

private fun detectConverterInputFormat(
    context: Context,
    uri: Uri,
    displayName: String
): ConverterInputFormat? {
    val normalizedMimeType = context.contentResolver.getType(uri)
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase(Locale.ROOT)
        .orEmpty()
    val extension = displayName
        .substringAfterLast('.', missingDelimiterValue = "")
        .lowercase(Locale.ROOT)

    // MIME type is preferred when it is specific; extension keeps SAF files with generic MIME usable.
    return ConverterInputFormat.entries.firstOrNull { format ->
        normalizedMimeType in format.mimeTypes
    } ?: ConverterInputFormat.entries.firstOrNull { format ->
        extension in format.extensions
    }
}

private fun convertDocumentToPdf(
    context: Context,
    sourceUri: Uri,
    targetUri: Uri,
    sourceFormat: ConverterInputFormat
): Result<Unit> = runCatching {
    if (sourceFormat == ConverterInputFormat.Pdf) {
        copyUriContent(context, sourceUri, targetUri, R.string.pdf_converter_error)
        return@runCatching
    }
    if (sourceFormat.isImage) {
        convertImageToPdf(context, sourceUri, targetUri)
        return@runCatching
    }

    val blocks = extractConvertibleBlocks(context, sourceUri, sourceFormat)
    require(blocks.any { it.text.isNotBlank() }) { context.getString(R.string.pdf_converter_empty_error) }

    context.contentResolver.openOutputStream(targetUri)?.use { output ->
        val document = PdfDocument()
        try {
            writeBlocksToPdf(document, blocks)
            document.writeTo(output)
        } finally {
            document.close()
        }
    } ?: error(context.getString(R.string.pdf_converter_error))
}

private fun convertImageToPdf(
    context: Context,
    sourceUri: Uri,
    targetUri: Uri
) {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(sourceUri)?.use { input ->
        BitmapFactory.decodeStream(input, null, bounds)
    }
    val bitmapOptions = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
        inSampleSize = imagePdfSampleSize(bounds.outWidth, bounds.outHeight)
    }
    val bitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
        BitmapFactory.decodeStream(input, null, bitmapOptions)
    } ?: error(context.getString(R.string.pdf_converter_error))
    context.contentResolver.openOutputStream(targetUri)?.use { output ->
        val document = PdfDocument()
        try {
            writeImageToPdf(document, bitmap)
            document.writeTo(output)
        } finally {
            bitmap.recycle()
            document.close()
        }
    } ?: error(context.getString(R.string.pdf_converter_error))
}

private fun imagePdfSampleSize(width: Int, height: Int): Int {
    var sampleSize = 1
    val largestSide = maxOf(width, height)
    while (largestSide / sampleSize > IMAGE_PDF_MAX_PAGE_SIDE * 2) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun writeImageToPdf(document: PdfDocument, bitmap: Bitmap) {
    val pageScale = minOf(
        IMAGE_PDF_MAX_PAGE_SIDE / bitmap.width.toFloat(),
        IMAGE_PDF_MAX_PAGE_SIDE / bitmap.height.toFloat(),
        1f
    )
    val pageWidth = (bitmap.width * pageScale).toInt().coerceAtLeast(1)
    val pageHeight = (bitmap.height * pageScale).toInt().coerceAtLeast(1)
    val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
        isDither = true
    }
    val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
    page.canvas.drawBitmap(bitmap, null, RectF(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat()), imagePaint)
    document.finishPage(page)
}

private fun writeBlocksToPdf(
    document: PdfDocument,
    blocks: List<PdfTextBlock>
) {
    val pageWidth = 612
    val pageHeight = 792
    val margin = 42
    val textWidth = pageWidth - margin * 2
    val pageBottom = pageHeight - margin
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        // 18sp-like PDF units with moderate margins produce readable pages on phone and desktop viewers.
        textSize = 18f
    }
    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var y = margin

    fun finishPage() {
        document.finishPage(page)
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        y = margin
    }

    blocks.filter { it.text.isNotBlank() }.forEach { block ->
        val text = block.text.trimTrailingWhitespace()
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(5f, 1.05f)
            .setIncludePad(true)
            .build()

        var firstLine = 0
        while (firstLine < layout.lineCount) {
            val availableHeight = pageBottom - y
            if (availableHeight <= textPaint.textSize * 1.4f && y > margin) {
                finishPage()
                continue
            }

            var lastLineExclusive = firstLine
            while (
                lastLineExclusive < layout.lineCount &&
                layout.getLineBottom(lastLineExclusive) - layout.getLineTop(firstLine) <= availableHeight
            ) {
                lastLineExclusive++
            }

            if (lastLineExclusive == firstLine) {
                finishPage()
                continue
            }

            val chunkStart = layout.getLineStart(firstLine)
            val chunkEnd = layout.getLineEnd(lastLineExclusive - 1)
            val chunkText = text.subSequence(chunkStart, chunkEnd).trimTrailingWhitespace()
            val chunkLayout = StaticLayout.Builder
                .obtain(chunkText, 0, chunkText.length, textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(5f, 1.05f)
                .setIncludePad(true)
                .build()
            page.canvas.save()
            page.canvas.translate(margin.toFloat(), y.toFloat())
            chunkLayout.draw(page.canvas)
            page.canvas.restore()

            y += chunkLayout.height
            firstLine = lastLineExclusive
            if (firstLine < layout.lineCount) finishPage()
        }
        y += block.spacingAfter
        if (y > pageHeight - margin) finishPage()
    }

    document.finishPage(page)
}

private fun convertDocumentToEpub(
    context: Context,
    sourceUri: Uri,
    targetUri: Uri,
    sourceFormat: ConverterInputFormat
): Result<Unit> = runCatching {
    val sourceName = displayNameForUri(context, sourceUri)
    if (sourceFormat == ConverterInputFormat.Epub) {
        copyUriContent(context, sourceUri, targetUri, R.string.epub_converter_error)
        return@runCatching
    }

    val book = buildEpubBook(context, sourceUri, sourceName, sourceFormat)
    require(book.chapters.any { it.xhtmlBody.isNotBlank() } || book.assets.isNotEmpty()) {
        context.getString(R.string.pdf_converter_empty_error)
    }

    context.contentResolver.openOutputStream(targetUri)?.use { output ->
        ZipOutputStream(output).use { zip ->
            writeEpubBook(zip, book)
        }
    } ?: error(context.getString(R.string.epub_converter_error))
}

private fun copyUriContent(context: Context, sourceUri: Uri, targetUri: Uri, errorStringRes: Int) {
    val input = context.contentResolver.openInputStream(sourceUri)
        ?: error(context.getString(errorStringRes))
    val output = context.contentResolver.openOutputStream(targetUri)
        ?: error(context.getString(errorStringRes))
    input.use { source ->
        output.use { target -> source.copyTo(target) }
    }
}

private fun buildEpubBook(
    context: Context,
    uri: Uri,
    sourceName: String,
    sourceFormat: ConverterInputFormat
): ConverterEpubBook {
    val title = sourceName.substringBeforeLast('.', missingDelimiterValue = sourceName)
        .ifBlank { "Kai Shelves" }
    return when (sourceFormat) {
        ConverterInputFormat.Pdf -> pdfToEpubBook(context, uri, title)
        ConverterInputFormat.Fb2 -> fb2ToEpubBook(readUriText(context, uri), title)
        ConverterInputFormat.Html -> htmlToEpubBook(readUriText(context, uri), title)
        else -> txtToEpubBook(readUriText(context, uri), title)
    }
}

private fun txtToEpubBook(raw: String, title: String): ConverterEpubBook {
    val body = raw
        .lineSequence()
        .joinToString("\n") { line ->
            if (line.isBlank()) "<p class=\"empty\">&#160;</p>" else "<p>${line.escapeXml()}</p>"
        }
    return ConverterEpubBook(
        title = title,
        chapters = listOf(ConverterEpubChapter("chapter-1", "Texto", body))
    )
}

private fun htmlToEpubBook(raw: String, fallbackTitle: String): ConverterEpubBook {
    val title = Regex("(?is)<title\\b[^>]*>(.*?)</title>")
        .find(raw)
        ?.groupValues
        ?.getOrNull(1)
        ?.htmlToPlainText()
        ?.ifBlank { null }
        ?: fallbackTitle
    val language = Regex("""(?is)<html\b[^>]*\blang\s*=\s*["']([^"']+)["']""")
        .find(raw)
        ?.groupValues
        ?.getOrNull(1)
        ?.takeIf { it.isNotBlank() }
        ?: "es"
    val bodyText = htmlToSpanned(raw).toString()
    return txtToEpubBook(bodyText, title).copy(language = language)
}

private fun pdfToEpubBook(context: Context, uri: Uri, title: String): ConverterEpubBook {
    val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
        ?: error(context.getString(R.string.epub_converter_error))
    val assets = mutableListOf<ConverterEpubAsset>()
    val chapters = mutableListOf<ConverterEpubChapter>()

    // PDF -> EPUB keeps each page as an image for visual fidelity and adds a tiny text layer for reader compatibility.
    descriptor.use { pfd: ParcelFileDescriptor ->
        PdfRenderer(pfd).use { renderer ->
            for (index in 0 until renderer.pageCount) {
                renderer.openPage(index).use { page ->
                    val bitmap = Bitmap.createBitmap(
                        (page.width * PDF_EPUB_RENDER_SCALE).coerceAtLeast(1),
                        (page.height * PDF_EPUB_RENDER_SCALE).coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val imageBytes = bitmap.toPngBytes()
                    bitmap.recycle()
                    val fileName = "page-${index + 1}.png"
                    val pageTitle = context.getString(R.string.converter_pdf_epub_page_title, index + 1)
                    val readableText = context.getString(R.string.converter_pdf_epub_page_accessible_text, index + 1)
                    assets += ConverterEpubAsset("images/$fileName", "image/png", imageBytes)
                    chapters += ConverterEpubChapter(
                        id = "page-${index + 1}",
                        title = pageTitle,
                        xhtmlBody = """
                            <section class="pdf-page">
                              <p class="pdf-readable-layer">${readableText.escapeXml()}</p>
                              <figure><img src="../images/$fileName" alt="${pageTitle.escapeXml()}" /></figure>
                            </section>
                        """.trimIndent()
                    )
                }
            }
        }
    }
    return ConverterEpubBook(title = title, chapters = chapters, assets = assets)
}

private fun fb2ToEpubBook(raw: String, fallbackTitle: String): ConverterEpubBook {
    val metadata = parseFb2Metadata(raw, fallbackTitle)
    val binaryAssetsById = extractFb2BinaryAssets(raw)
    val body = Regex("(?is)<body\\b[^>]*>(.*?)</body>")
        .find(raw)
        ?.groupValues
        ?.getOrNull(1)
        ?: raw
    val xhtmlBody = body
        .replaceFb2Images(binaryAssetsById)
        .replace(Regex("(?is)<binary.*?</binary>"), " ")
        .replace(Regex("(?is)<section\\b[^>]*>"), "<section>")
        .replace(Regex("(?is)</section>"), "</section>")
        .replace(Regex("(?is)<title\\b[^>]*>"), "<h2>")
        .replace(Regex("(?is)</title>"), "</h2>")
        .replace(Regex("(?is)<subtitle\\b[^>]*>"), "<h3>")
        .replace(Regex("(?is)</subtitle>"), "</h3>")
        .replace(Regex("(?is)<empty-line\\s*/?>"), "<p class=\"empty\">&#160;</p>")
        .replace(Regex("(?is)<emphasis\\b[^>]*>"), "<em>")
        .replace(Regex("(?is)</emphasis>"), "</em>")
        .replace(Regex("(?is)<strong\\b[^>]*>"), "<strong>")
        .replace(Regex("(?is)</strong>"), "</strong>")
        .replace(Regex("(?is)<strikethrough\\b[^>]*>"), "<s>")
        .replace(Regex("(?is)</strikethrough>"), "</s>")
        .replace(Regex("(?is)<v\\b[^>]*>"), "<p class=\"verse\">")
        .replace(Regex("(?is)</v>"), "</p>")
        .replace(Regex("(?is)<poem\\b[^>]*>|</poem>|<stanza\\b[^>]*>|</stanza>"), "")
        .replace(Regex("(?is)<p\\b[^>]*>"), "<p>")
        .stripUnsupportedFb2Tags()
    return ConverterEpubBook(
        title = metadata.title,
        author = metadata.author,
        identifier = metadata.identifier,
        language = metadata.language,
        chapters = listOf(ConverterEpubChapter("chapter-1", metadata.title, xhtmlBody)),
        assets = binaryAssetsById.values.toList()
    )
}

private fun parseFb2Metadata(raw: String, fallbackTitle: String): ConverterEpubMetadata {
    val title = Regex("(?is)<book-title\\b[^>]*>(.*?)</book-title>")
        .find(raw)
        ?.groupValues
        ?.getOrNull(1)
        ?.htmlToPlainText()
        ?.ifBlank { null }
        ?: fallbackTitle
    val authorBlock = Regex("(?is)<author\\b[^>]*>(.*?)</author>")
        .find(raw)
        ?.groupValues
        ?.getOrNull(1)
        .orEmpty()
    val author = listOf("first-name", "middle-name", "last-name", "nickname")
        .mapNotNull { tag ->
            Regex("(?is)<$tag\\b[^>]*>(.*?)</$tag>").find(authorBlock)?.groupValues?.getOrNull(1)
        }
        .joinToString(" ") { it.htmlToPlainText() }
        .trim()
    val language = Regex("(?is)<lang\\b[^>]*>(.*?)</lang>")
        .find(raw)
        ?.groupValues
        ?.getOrNull(1)
        ?.htmlToPlainText()
        ?.ifBlank { null }
        ?: "es"
    val identifier = Regex("(?is)<id\\b[^>]*>(.*?)</id>")
        .find(raw)
        ?.groupValues
        ?.getOrNull(1)
        ?.htmlToPlainText()
        ?.takeIf { it.isNotBlank() }
        ?.let { "fb2:$it" }
        ?: "urn:uuid:${UUID.randomUUID()}"
    return ConverterEpubMetadata(title, author, language, identifier)
}

private fun extractFb2BinaryAssets(raw: String): Map<String, ConverterEpubAsset> {
    val assets = linkedMapOf<String, ConverterEpubAsset>()
    Regex("(?is)<binary\\b([^>]*)>(.*?)</binary>").findAll(raw).forEachIndexed { index, match ->
        val attributes = match.groupValues[1]
        val id = attributes.extractAttribute("id").ifBlank { "image-${index + 1}" }
        val mediaType = attributes.extractAttribute("content-type").ifBlank { "image/jpeg" }
        val bytes = runCatching {
            Base64.decode(match.groupValues[2].filterNot(Char::isWhitespace), Base64.DEFAULT)
        }.getOrNull() ?: return@forEachIndexed
        val extension = when (mediaType.lowercase(Locale.ROOT)) {
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        assets[id] = ConverterEpubAsset("images/${id.sanitizeFilePart()}.$extension", mediaType, bytes)
    }
    return assets
}

private fun String.replaceFb2Images(assetsById: Map<String, ConverterEpubAsset>): String {
    return replace(Regex("(?is)<image\\b([^>]*)/?>")) { match ->
        val imageId = match.groupValues[1].extractAttribute("href")
            .ifBlank { match.groupValues[1].extractAttribute("l:href") }
            .removePrefix("#")
        val asset = assetsById[imageId]
        if (asset != null) {
            "<figure class=\"image\"><img src=\"../${asset.path}\" alt=\"\" /></figure>"
        } else {
            ""
        }
    }
}

private fun String.stripUnsupportedFb2Tags(): String {
    val allowed = setOf("section", "h2", "h3", "p", "em", "strong", "s", "figure", "img", "br")
    return replace(Regex("(?is)</?([a-zA-Z0-9:_-]+)\\b([^>]*)>")) { match ->
        val tag = match.groupValues[1].substringAfter(':').lowercase(Locale.ROOT)
        if (tag in allowed) match.value else ""
    }
}

private fun writeEpubBook(zip: ZipOutputStream, book: ConverterEpubBook) {
    zip.putStoredEntry("mimetype", "application/epub+zip".toByteArray())
    zip.putTextEntry("META-INF/container.xml", epubContainerXml())
    zip.putTextEntry("OEBPS/styles/kai.css", epubStylesheet())

    book.chapters.forEachIndexed { index, chapter ->
        zip.putTextEntry("OEBPS/text/chapter-${index + 1}.xhtml", chapter.toXhtml(book))
    }
    book.assets.forEach { asset ->
        zip.putNextEntry(ZipEntry("OEBPS/${asset.path}"))
        zip.write(asset.bytes)
        zip.closeEntry()
    }
    zip.putTextEntry("OEBPS/nav.xhtml", epubNav(book))
    zip.putTextEntry("OEBPS/content.opf", epubPackage(book))
}

private fun epubContainerXml(): String = """
    <?xml version="1.0" encoding="UTF-8"?>
    <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
      <rootfiles>
        <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
      </rootfiles>
    </container>
""".trimIndent()

private fun epubStylesheet(): String = """
    body { font-family: serif; line-height: 1.55; margin: 5%; color: #1f1b16; background: #fbf7ef; }
    h1, h2, h3 { color: #5b3a1f; page-break-after: avoid; }
    p { margin: 0 0 0.85em; text-align: justify; }
    .empty { min-height: 1em; }
    .verse { text-align: left; margin-left: 1.5em; }
    figure { margin: 1em 0; text-align: center; }
    img { max-width: 100%; height: auto; }
    .pdf-page { page-break-after: always; }
    .pdf-readable-layer { position: absolute; left: -9999px; width: 1px; height: 1px; overflow: hidden; color: transparent; font-size: 1px; line-height: 1px; }
""".trimIndent()

private fun ConverterEpubChapter.toXhtml(book: ConverterEpubBook): String = """
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE html>
    <html xmlns="http://www.w3.org/1999/xhtml" lang="${book.language.escapeXml()}">
      <head>
        <title>${title.escapeXml()}</title>
        <link rel="stylesheet" type="text/css" href="../styles/kai.css" />
      </head>
      <body>
        <h1>${book.title.escapeXml()}</h1>
        $xhtmlBody
      </body>
    </html>
""".trimIndent()

private fun epubNav(book: ConverterEpubBook): String {
    val items = book.chapters.mapIndexed { index, chapter ->
        "<li><a href=\"text/chapter-${index + 1}.xhtml\">${chapter.title.escapeXml()}</a></li>"
    }.joinToString("\n")
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE html>
        <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" lang="${book.language.escapeXml()}">
          <head><title>${book.title.escapeXml()}</title></head>
          <body>
            <nav epub:type="toc" id="toc">
              <h1>${book.title.escapeXml()}</h1>
              <ol>$items</ol>
            </nav>
          </body>
        </html>
    """.trimIndent()
}

private fun epubPackage(book: ConverterEpubBook): String {
    val chapterItems = book.chapters.mapIndexed { index, chapter ->
        """<item id="${chapter.id.escapeXml()}" href="text/chapter-${index + 1}.xhtml" media-type="application/xhtml+xml"/>"""
    }.joinToString("\n")
    val assetItems = book.assets.mapIndexed { index, asset ->
        """<item id="asset-${index + 1}" href="${asset.path.escapeXml()}" media-type="${asset.mediaType.escapeXml()}"/>"""
    }.joinToString("\n")
    val spine = book.chapters.joinToString("\n") { """<itemref idref="${it.id.escapeXml()}"/>""" }
    val creator = book.author.takeIf { it.isNotBlank() }
        ?.let { "<dc:creator>${it.escapeXml()}</dc:creator>" }
        .orEmpty()
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="book-id">
          <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
            <dc:identifier id="book-id">${book.identifier.escapeXml()}</dc:identifier>
            <dc:title>${book.title.escapeXml()}</dc:title>
            $creator
            <dc:language>${book.language.escapeXml()}</dc:language>
          </metadata>
          <manifest>
            <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
            <item id="css" href="styles/kai.css" media-type="text/css"/>
            $chapterItems
            $assetItems
          </manifest>
          <spine>
            $spine
          </spine>
        </package>
    """.trimIndent()
}

private fun ZipOutputStream.putTextEntry(path: String, content: String) {
    putNextEntry(ZipEntry(path))
    write(content.toByteArray(Charsets.UTF_8))
    closeEntry()
}

private fun ZipOutputStream.putStoredEntry(path: String, bytes: ByteArray) {
    val crc = CRC32().apply { update(bytes) }
    val entry = ZipEntry(path).apply {
        method = ZipEntry.STORED
        size = bytes.size.toLong()
        compressedSize = bytes.size.toLong()
        this.crc = crc.value
    }
    putNextEntry(entry)
    write(bytes)
    closeEntry()
}

private data class PdfTextBlock(
    val text: CharSequence,
    val spacingAfter: Int = 18
)

private enum class ConverterInputFormat(
    val labelRes: Int,
    val pdfSupported: Boolean,
    val epubSupported: Boolean,
    val mimeTypes: Set<String>,
    val extensions: Set<String>
) {
    Txt(
        labelRes = R.string.converter_input_format_txt,
        pdfSupported = true,
        epubSupported = true,
        mimeTypes = setOf("text/plain"),
        extensions = setOf("txt")
    ),
    Markdown(
        labelRes = R.string.converter_input_format_markdown,
        pdfSupported = true,
        epubSupported = true,
        mimeTypes = setOf("text/markdown", "text/x-markdown"),
        extensions = setOf("md", "markdown")
    ),
    Html(
        labelRes = R.string.converter_input_format_html,
        pdfSupported = true,
        epubSupported = true,
        mimeTypes = setOf("text/html", "application/xhtml+xml"),
        extensions = setOf("html", "htm", "xhtml")
    ),
    Fb2(
        labelRes = R.string.converter_input_format_fb2,
        pdfSupported = true,
        epubSupported = true,
        mimeTypes = setOf("application/x-fictionbook+xml"),
        extensions = setOf("fb2")
    ),
    Epub(
        labelRes = R.string.converter_input_format_epub,
        pdfSupported = true,
        epubSupported = true,
        mimeTypes = setOf("application/epub+zip"),
        extensions = setOf("epub")
    ),
    Pdf(
        labelRes = R.string.converter_input_format_pdf,
        pdfSupported = true,
        epubSupported = true,
        mimeTypes = setOf("application/pdf"),
        extensions = setOf("pdf")
    ),
    Jpeg(
        labelRes = R.string.converter_input_format_jpeg,
        pdfSupported = true,
        epubSupported = false,
        mimeTypes = setOf("image/jpeg"),
        extensions = setOf("jpg", "jpeg")
    ),
    Png(
        labelRes = R.string.converter_input_format_png,
        pdfSupported = true,
        epubSupported = false,
        mimeTypes = setOf("image/png"),
        extensions = setOf("png")
    ),
    Webp(
        labelRes = R.string.converter_input_format_webp,
        pdfSupported = true,
        epubSupported = false,
        mimeTypes = setOf("image/webp"),
        extensions = setOf("webp")
    ),
    Word(
        labelRes = R.string.converter_input_format_word,
        pdfSupported = false,
        epubSupported = false,
        mimeTypes = setOf(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        ),
        extensions = setOf("doc", "docx")
    ),
    OpenDocumentText(
        labelRes = R.string.converter_input_format_odt,
        pdfSupported = false,
        epubSupported = false,
        mimeTypes = setOf("application/vnd.oasis.opendocument.text"),
        extensions = setOf("odt")
    ),
    RichText(
        labelRes = R.string.converter_input_format_rtf,
        pdfSupported = false,
        epubSupported = false,
        mimeTypes = setOf("application/rtf", "text/rtf"),
        extensions = setOf("rtf")
    ),
    Mobi(
        labelRes = R.string.converter_input_format_mobi,
        pdfSupported = false,
        epubSupported = false,
        mimeTypes = setOf("application/x-mobipocket-ebook"),
        extensions = setOf("mobi", "azw", "azw3", "prc")
    ),
    ComicArchive(
        labelRes = R.string.converter_input_format_comic,
        pdfSupported = false,
        epubSupported = false,
        mimeTypes = setOf("application/vnd.comicbook+zip", "application/x-cbr"),
        extensions = setOf("cbz", "cbr")
    ),
    Djvu(
        labelRes = R.string.converter_input_format_djvu,
        pdfSupported = false,
        epubSupported = false,
        mimeTypes = setOf("image/vnd.djvu", "image/x.djvu"),
        extensions = setOf("djvu", "djv")
    );

    val isImage: Boolean
        get() = this == Jpeg || this == Png || this == Webp

    fun supports(outputFormat: ConverterOutputFormat): Boolean {
        return when (outputFormat) {
            ConverterOutputFormat.Pdf -> pdfSupported
            ConverterOutputFormat.Epub -> epubSupported
        }
    }
}

private enum class ConverterOutputFormat {
    Pdf,
    Epub;

    val extension: String
        get() = when (this) {
            Pdf -> ".pdf"
            Epub -> ".epub"
        }

    val mimeType: String
        get() = when (this) {
            Pdf -> "application/pdf"
            Epub -> "application/epub+zip"
        }

    val labelRes: Int
        get() = when (this) {
            Pdf -> R.string.converter_format_pdf
            Epub -> R.string.converter_format_epub
        }

    val actionLabelRes: Int
        get() = when (this) {
            Pdf -> R.string.pdf_converter_convert
            Epub -> R.string.epub_converter_convert
        }
}

private data class ConverterEpubBook(
    val title: String,
    val author: String = "",
    val identifier: String = "urn:uuid:${UUID.randomUUID()}",
    val language: String = "es",
    val chapters: List<ConverterEpubChapter>,
    val assets: List<ConverterEpubAsset> = emptyList()
)

private data class ConverterEpubChapter(
    val id: String,
    val title: String,
    val xhtmlBody: String
)

private data class ConverterEpubAsset(
    val path: String,
    val mediaType: String,
    val bytes: ByteArray
)

private data class ConverterEpubMetadata(
    val title: String,
    val author: String,
    val language: String = "es",
    val identifier: String = "urn:uuid:${UUID.randomUUID()}"
)

private fun extractConvertibleBlocks(
    context: Context,
    uri: Uri,
    sourceFormat: ConverterInputFormat
): List<PdfTextBlock> {
    return when (sourceFormat) {
        ConverterInputFormat.Epub -> extractEpubBlocks(context, uri)
        ConverterInputFormat.Fb2 -> listOf(PdfTextBlock(fb2ToSpanned(readUriText(context, uri))))
        ConverterInputFormat.Html -> listOf(PdfTextBlock(htmlToSpanned(readUriText(context, uri))))
        else -> listOf(PdfTextBlock(SpannedString(readUriText(context, uri).trim())))
    }
}

private fun readUriText(context: Context, uri: Uri): String {
    return context.contentResolver.openInputStream(uri)?.use { input ->
        input.readBytes().decodeToString()
    }.orEmpty()
}

private fun extractEpubBlocks(context: Context, uri: Uri): List<PdfTextBlock> {
    val rootFilePath = readConverterZipEntry(context, uri, "META-INF/container.xml")
        ?.decodeToString()
        ?.let(::parseConverterRootFilePath)
        ?: return emptyList()
    val opf = readConverterZipEntry(context, uri, rootFilePath)?.decodeToString() ?: return emptyList()
    return parseConverterEpubReadingPaths(opf)
        .map { href -> resolveConverterZipPath(rootFilePath, href) }
        .mapNotNull { path -> readConverterZipEntry(context, uri, path)?.decodeToString() }
        .map(::htmlToSpanned)
        .filter { it.isNotBlank() }
        .map { PdfTextBlock(it) }
}

private fun readConverterZipEntry(context: Context, uri: Uri, targetPath: String): ByteArray? {
    context.contentResolver.openInputStream(uri)?.use { input ->
        ZipInputStream(input).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name == targetPath) return zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
    return null
}

private fun parseConverterRootFilePath(containerXml: String): String? {
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

private fun parseConverterEpubReadingPaths(opfXml: String): List<String> {
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    parser.setInput(opfXml.reader())
    val manifestItems = linkedMapOf<String, String>()
    val readableItems = linkedSetOf<String>()
    val spineIds = mutableListOf<String>()
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        if (event == XmlPullParser.START_TAG) {
            when (parser.name.substringAfter(':')) {
                "item" -> {
                    val id = parser.getAttributeValue(null, "id").orEmpty()
                    val href = parser.getAttributeValue(null, "href").orEmpty()
                    val mediaType = parser.getAttributeValue(null, "media-type").orEmpty()
                    if (id.isNotBlank() && href.isNotBlank()) {
                        manifestItems[id] = href
                        if (mediaType == "application/xhtml+xml" || mediaType == "text/html") readableItems.add(id)
                    }
                }
                "itemref" -> parser.getAttributeValue(null, "idref")?.takeIf { it.isNotBlank() }?.let(spineIds::add)
            }
        }
        event = parser.next()
    }
    return spineIds.mapNotNull(manifestItems::get).ifEmpty { readableItems.mapNotNull(manifestItems::get) }
}

private fun resolveConverterZipPath(rootFilePath: String, href: String): String {
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

private fun htmlToSpanned(raw: String): Spanned {
    val body = raw
        .replace(Regex("(?is)<(script|style).*?</\\1>"), " ")
        .replace(Regex("(?is)<head.*?</head>"), " ")
        .replace(Regex("(?is)<img\\b[^>]*>"), " ")
        .replace(Regex("(?i)<br\\s*/?>"), "<br>")
    return Html.fromHtml(body, Html.FROM_HTML_MODE_COMPACT)
}

private fun fb2ToSpanned(raw: String): Spanned {
    val body = Regex("(?is)<body\\b[^>]*>(.*?)</body>")
        .find(raw)
        ?.groupValues
        ?.getOrNull(1)
        ?: raw
    val html = body
        .replace(Regex("(?is)<binary.*?</binary>"), " ")
        .replace(Regex("(?is)<section\\b[^>]*>"), "<div>")
        .replace(Regex("(?is)</section>"), "</div>")
        .replace(Regex("(?is)<title\\b[^>]*>"), "<h2>")
        .replace(Regex("(?is)</title>"), "</h2>")
        .replace(Regex("(?is)<subtitle\\b[^>]*>"), "<h3>")
        .replace(Regex("(?is)</subtitle>"), "</h3>")
        .replace(Regex("(?is)<p\\b[^>]*>"), "<p>")
        .replace(Regex("(?is)<empty-line\\s*/?>"), "<br>")
        .replace(Regex("(?is)<emphasis\\b[^>]*>"), "<i>")
        .replace(Regex("(?is)</emphasis>"), "</i>")
        .replace(Regex("(?is)<strong\\b[^>]*>"), "<b>")
        .replace(Regex("(?is)</strong>"), "</b>")
        .replace(Regex("(?is)<strikethrough\\b[^>]*>"), "<s>")
        .replace(Regex("(?is)</strikethrough>"), "</s>")
        .replace(Regex("(?is)<v\\b[^>]*>"), "<p>")
        .replace(Regex("(?is)</v>"), "</p>")
        .replace(Regex("(?is)<poem\\b[^>]*>|</poem>|<stanza\\b[^>]*>|</stanza>"), "<br>")
        .replace(Regex("(?is)<image\\b[^>]*/?>"), " ")
    return htmlToSpanned(html)
}

private fun CharSequence.trimTrailingWhitespace(): CharSequence {
    var end = length
    while (end > 0 && this[end - 1].isWhitespace()) end--
    return subSequence(0, end)
}

private fun CharSequence.isNotBlank(): Boolean {
    for (index in indices) {
        if (!this[index].isWhitespace()) return true
    }
    return false
}

private fun Bitmap.toPngBytes(): ByteArray {
    return java.io.ByteArrayOutputStream().use { output ->
        compress(Bitmap.CompressFormat.PNG, 100, output)
        output.toByteArray()
    }
}

private fun String.extractAttribute(name: String): String {
    return Regex("""(?i)(?:^|\s)${Regex.escape(name)}\s*=\s*["']([^"']*)["']""")
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        .orEmpty()
}

private fun String.escapeXml(): String {
    return buildString(length) {
        this@escapeXml.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(char)
            }
        }
    }
}

private fun String.htmlToPlainText(): String {
    return Html.fromHtml(this, Html.FROM_HTML_MODE_COMPACT).toString().trim()
}

private fun String.sanitizeFilePart(): String {
    return replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "asset" }
}

private fun displayNameForUri(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) return cursor.getString(index).orEmpty()
        }
    }
    return uri.lastPathSegment.orEmpty().substringAfterLast('/').ifBlank { "documento" }
}
