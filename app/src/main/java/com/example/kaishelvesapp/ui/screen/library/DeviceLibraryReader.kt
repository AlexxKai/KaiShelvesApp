package com.example.kaishelvesapp.ui.screen.library

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.format.Formatter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.DeviceReaderAnnotation
import com.example.kaishelvesapp.data.model.DeviceReaderAnnotationType
import com.example.kaishelvesapp.data.repository.DeviceLibraryFile
import com.example.kaishelvesapp.data.repository.DeviceLibraryRepository
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun DeviceBookReaderDialog(
    file: DeviceLibraryFile,
    onProgressChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        when (readerEngineFor(file)) {
            ReaderEngineKind.Pdf -> PdfBookReader(
                file = file,
                onProgressChanged = onProgressChanged,
                onDismiss = onDismiss
            )
            ReaderEngineKind.Epub,
            ReaderEngineKind.Txt,
            ReaderEngineKind.Fb2 -> ReflowBookReader(
                file = file,
                engineKind = readerEngineFor(file),
                onProgressChanged = onProgressChanged,
                onDismiss = onDismiss
            )
            ReaderEngineKind.Unsupported -> UnsupportedBookReader(file = file)
        }
    }
}

private enum class ReaderEngineKind {
    Pdf,
    Epub,
    Txt,
    Fb2,
    Unsupported
}

private data class ReaderEngineDocument(
    val title: String,
    val subtitle: String,
    val pages: List<String>
)

private sealed interface ReaderEngineLoadState {
    data object Loading : ReaderEngineLoadState
    data class Ready(val document: ReaderEngineDocument) : ReaderEngineLoadState
    data object Empty : ReaderEngineLoadState
}

private fun readerEngineFor(file: DeviceLibraryFile): ReaderEngineKind {
    return when {
        isPdf(file) -> ReaderEngineKind.Pdf
        isEpub(file) -> ReaderEngineKind.Epub
        isTextBook(file) -> ReaderEngineKind.Txt
        isFb2(file) -> ReaderEngineKind.Fb2
        else -> ReaderEngineKind.Unsupported
    }
}

private suspend fun loadReaderEngineDocument(
    context: Context,
    file: DeviceLibraryFile,
    engineKind: ReaderEngineKind
): ReaderEngineDocument? = withContext(Dispatchers.IO) {
    when (engineKind) {
        ReaderEngineKind.Epub -> {
            val metadata = extractEpubDisplayMetadata(context, file.uri)
            val text = extractEpubReadingText(context, file.uri)
            buildTextEngineDocument(file, readableFileType(file), metadata, text)
        }
        ReaderEngineKind.Txt -> {
            val text = readPlainTextFile(context, file.uri).orEmpty()
            buildTextEngineDocument(file, readableFileType(file), null, text)
        }
        ReaderEngineKind.Fb2 -> {
            // FB2 se trata como adaptador textual ligero; si falla, queda listo para conversor EPUB futuro.
            val text = readPlainTextFile(context, file.uri)?.let(::stripXmlToText).orEmpty()
            buildTextEngineDocument(file, "FB2", null, text)
        }
        else -> null
    }
}

private fun buildTextEngineDocument(
    file: DeviceLibraryFile,
    subtitle: String,
    metadata: DeviceBookDisplayMetadata?,
    text: String
): ReaderEngineDocument? {
    val cleanText = text.replace(Regex("\\s+"), " ").trim()
    if (cleanText.isBlank()) return null
    return ReaderEngineDocument(
        title = metadata?.title?.takeIf { it.isNotBlank() } ?: file.name.substringBeforeLast('.'),
        subtitle = metadata?.author?.takeIf { it.isNotBlank() } ?: subtitle,
        pages = listOf(cleanText)
    )
}

@Composable
private fun ReflowBookReader(
    file: DeviceLibraryFile,
    engineKind: ReaderEngineKind,
    onProgressChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val documentState by produceState<ReaderEngineLoadState>(
        initialValue = ReaderEngineLoadState.Loading,
        file.uri,
        engineKind
    ) {
        val loadedDocument = loadReaderEngineDocument(context, file, engineKind)
        value = loadedDocument?.let(ReaderEngineLoadState::Ready) ?: ReaderEngineLoadState.Empty
    }

    when (val state = documentState) {
        ReaderEngineLoadState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = TarnishedGold)
        }
        ReaderEngineLoadState.Empty -> ReaderMessage(
            title = stringResource(R.string.device_reader_error_title),
            body = stringResource(R.string.device_reader_text_error)
        )
        is ReaderEngineLoadState.Ready -> {
            val loadedDocument = state.document
            val annotationRepository = remember(file.uri) { DeviceLibraryRepository(context) }
            var currentPage by remember(file.uri) { mutableStateOf(readDeviceBookCurrentPage(context, file, 1)) }
            var logicalPageCount by remember(file.uri) { mutableStateOf(1) }
            var controlsVisible by remember(file.uri) { mutableStateOf(false) }
            var showTextSizeSettings by remember(file.uri) { mutableStateOf(false) }
            var showAnnotationsDialog by remember(file.uri) { mutableStateOf(false) }
            var showNoteDialog by remember(file.uri) { mutableStateOf(false) }
            var editingAnnotation by remember(file.uri) { mutableStateOf<DeviceReaderAnnotation?>(null) }
            var textSizePercent by remember { mutableStateOf(readReaderPdfZoomPercent(context)) }
            var zoomPercentBeforeChange by remember(file.uri) { mutableStateOf<Int?>(null) }
            var visiblePageText by remember(file.uri) { mutableStateOf("") }
            var selectedText by remember(file.uri) { mutableStateOf("") }
            var annotations by remember(file.uri) { mutableStateOf(annotationRepository.getAnnotations(file)) }
            val progress = readingProgressForPage(currentPage, logicalPageCount)

            LaunchedEffect(file.uri, currentPage, logicalPageCount) {
                saveDeviceBookReadingProgress(context, file, currentPage, logicalPageCount)
                onProgressChanged()
            }
            LaunchedEffect(logicalPageCount) {
                currentPage = currentPage.coerceIn(0, logicalPageCount.coerceAtLeast(1) - 1)
            }
            LaunchedEffect(textSizePercent) {
                saveReaderTextSizePercent(context, textSizePercent)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                ReflowTextReaderPage(
                    sourcePages = loadedDocument.pages,
                    pageIndex = currentPage,
                    textSizePercent = textSizePercent,
                    onPageCountChanged = { count -> logicalPageCount = count.coerceAtLeast(1) },
                    onVisibleTextChanged = { visiblePageText = it },
                    onSelectedTextChanged = { selectedText = it },
                    modifier = Modifier.fillMaxSize()
                )

                if (controlsVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f)
                            .clickable {
                                controlsVisible = false
                                showTextSizeSettings = false
                            }
                    )
                    PdfReaderTopControls(
                        title = loadedDocument.title,
                        onDismiss = onDismiss,
                        onTitleClick = {},
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .zIndex(2f)
                    )
                    if (showTextSizeSettings) {
                        ReaderTextSizePanel(
                            zoomPercent = textSizePercent,
                            isReflowMode = true,
                            canUseReflowMode = true,
                            showReset = zoomPercentBeforeChange != null && zoomPercentBeforeChange != textSizePercent,
                            onZoomPercentChange = { newZoomPercent ->
                                val targetZoomPercent = newZoomPercent.coerceIn(READER_PDF_ZOOM_MIN, READER_PDF_ZOOM_MAX)
                                if (targetZoomPercent != textSizePercent && zoomPercentBeforeChange == null) {
                                    zoomPercentBeforeChange = textSizePercent
                                }
                                textSizePercent = targetZoomPercent
                                if (zoomPercentBeforeChange == textSizePercent) {
                                    zoomPercentBeforeChange = null
                                }
                            },
                            onResetZoomPercent = {
                                zoomPercentBeforeChange?.let { textSizePercent = it }
                                zoomPercentBeforeChange = null
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .zIndex(3f)
                        )
                    } else {
                        ReflowReaderBottomControls(
                            currentPage = currentPage,
                            pageCount = logicalPageCount,
                            progress = progress,
                            subtitle = loadedDocument.subtitle,
                            onPageSelected = { page -> currentPage = page.coerceIn(0, logicalPageCount - 1) },
                            annotationsCount = annotations.size,
                            onOpenTextSizeSettings = {
                                showTextSizeSettings = true
                                zoomPercentBeforeChange = null
                            },
                            onOpenAnnotations = { showAnnotationsDialog = true },
                            onAddBookmark = {
                                annotationRepository.addAnnotation(
                                    file = file,
                                    annotation = DeviceReaderAnnotation(
                                        type = DeviceReaderAnnotationType.Bookmark,
                                        page = currentPage,
                                        pageCount = logicalPageCount,
                                        selectedText = visiblePageText.take(READER_ANNOTATION_TEXT_LIMIT),
                                        note = "Marcador de lectura"
                                    )
                                )
                                annotations = annotationRepository.getAnnotations(file)
                            },
                            onAddHighlight = {
                                annotationRepository.addAnnotation(
                                    file = file,
                                    annotation = DeviceReaderAnnotation(
                                        type = DeviceReaderAnnotationType.Highlight,
                                        page = currentPage,
                                        pageCount = logicalPageCount,
                                        selectedText = selectedText.ifBlank { visiblePageText }.take(READER_ANNOTATION_TEXT_LIMIT),
                                        color = "#EBC7E8"
                                    )
                                )
                                annotations = annotationRepository.getAnnotations(file)
                            },
                            onAddNote = { showNoteDialog = true },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .zIndex(2f)
                        )
                    }
                } else {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(0.33f)
                                .fillMaxHeight()
                                .clickable { currentPage = (currentPage - 1).coerceAtLeast(0) }
                        )
                        Box(
                            modifier = Modifier
                                .weight(0.34f)
                                .fillMaxHeight()
                                .clickable { controlsVisible = true }
                        )
                        Box(
                            modifier = Modifier
                                .weight(0.33f)
                                .fillMaxHeight()
                                .clickable { currentPage = (currentPage + 1).coerceAtMost(logicalPageCount - 1) }
                        )
                    }
                }

                if (showAnnotationsDialog) {
                    ReflowAnnotationsDialog(
                        annotations = annotations,
                        onAnnotationSelected = { annotation ->
                            currentPage = annotation.page.coerceIn(0, logicalPageCount - 1)
                            showAnnotationsDialog = false
                            controlsVisible = false
                        },
                        onAnnotationEdit = { annotation -> editingAnnotation = annotation },
                        onAnnotationDelete = { annotation ->
                            annotationRepository.deleteAnnotation(file, annotation.id)
                            annotations = annotationRepository.getAnnotations(file)
                        },
                        onDismiss = { showAnnotationsDialog = false }
                    )
                }

                if (showNoteDialog) {
                    ReaderNoteDialog(
                        onSave = { note ->
                            annotationRepository.addAnnotation(
                                file = file,
                                annotation = DeviceReaderAnnotation(
                                    type = DeviceReaderAnnotationType.Note,
                                    page = currentPage,
                                    pageCount = logicalPageCount,
                                    selectedText = selectedText.ifBlank { visiblePageText }.take(READER_ANNOTATION_TEXT_LIMIT),
                                    note = note
                                )
                            )
                            annotations = annotationRepository.getAnnotations(file)
                            showNoteDialog = false
                        },
                        onDismiss = { showNoteDialog = false }
                    )
                }

                editingAnnotation?.let { annotation ->
                    ReaderAnnotationEditDialog(
                        annotation = annotation,
                        onSave = { updated ->
                            annotationRepository.updateAnnotation(file, updated)
                            annotations = annotationRepository.getAnnotations(file)
                            editingAnnotation = null
                        },
                        onDelete = {
                            annotationRepository.deleteAnnotation(file, annotation.id)
                            annotations = annotationRepository.getAnnotations(file)
                            editingAnnotation = null
                        },
                        onDismiss = { editingAnnotation = null }
                    )
                }
            }
        }
    }
}

@Composable
fun PdfBookReader(
    file: DeviceLibraryFile,
    onProgressChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pageCount by produceState<Int?>(initialValue = null, file.uri) {
        value = withContext(Dispatchers.IO) { getPdfPageCount(context, file.uri) }
    }

    when (val pages = pageCount) {
        null -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = TarnishedGold)
        }
        0 -> ReaderMessage(
            title = stringResource(R.string.device_reader_error_title),
            body = stringResource(R.string.device_reader_pdf_error)
        )
        else -> {
            var currentPage by remember(file.uri, pages) {
                mutableStateOf(readDeviceBookCurrentPage(context, file, pages))
            }
            var controlsVisible by remember(file.uri) { mutableStateOf(false) }
            var resetSliderPage by remember(file.uri) { mutableStateOf<Int?>(null) }
            var showBookInfo by remember(file.uri) { mutableStateOf(false) }
            var showBookInfoMore by remember(file.uri) { mutableStateOf(false) }
            var showDisplaySettings by remember(file.uri) { mutableStateOf(false) }
            var showTextSizeSettings by remember(file.uri) { mutableStateOf(false) }
            var showAnnotationsDialog by remember(file.uri) { mutableStateOf(false) }
            var showNoteDialog by remember(file.uri) { mutableStateOf(false) }
            var editingAnnotation by remember(file.uri) { mutableStateOf<DeviceReaderAnnotation?>(null) }
            var brightnessPercent by remember { mutableStateOf(readReaderBrightnessPercent(context)) }
            var autoBrightness by remember { mutableStateOf(readReaderAutoBrightness(context)) }
            var blueLightFilterEnabled by remember { mutableStateOf(readReaderBlueLightFilterEnabled(context)) }
            var blueLightOpacity by remember { mutableStateOf(readReaderBlueLightOpacity(context)) }
            var readerZoomPercent by remember { mutableStateOf(readReaderPdfZoomPercent(context)) }
            var zoomPercentBeforeChange by remember(file.uri) { mutableStateOf<Int?>(null) }
            var pdfPanX by remember(file.uri) { mutableStateOf(0f) }
            var pdfPanY by remember(file.uri) { mutableStateOf(0f) }
            var showBrightnessAdvancedSettings by remember(file.uri) { mutableStateOf(false) }
            var selectedBrightnessEdge by remember { mutableStateOf(readReaderBrightnessEdge(context)) }
            var resumeAutoBrightnessAfterInactivity by remember {
                mutableStateOf(readReaderResumeAutoBrightness(context))
            }
            var resumeAutoBrightnessMinutes by remember {
                mutableStateOf(readReaderResumeAutoBrightnessMinutes(context))
            }
            var showAutoBrightnessMinutesDialog by remember(file.uri) { mutableStateOf(false) }
            var brightnessEdgeChangeCount by remember(file.uri) { mutableStateOf(0) }
            var brightnessFeedbackPercent by remember(file.uri) { mutableStateOf<Int?>(null) }
            val userMetadata = remember(file.uri) { readDeviceBookUserMetadata(context, file) }
            val annotationRepository = remember(file.uri) { DeviceLibraryRepository(context) }
            var annotations by remember(file.uri) { mutableStateOf(annotationRepository.getAnnotations(file)) }
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val activePageCount = pages
            val progress = readingProgressForPage(currentPage, activePageCount)
            ApplyReaderBrightness(
                brightnessPercent = brightnessPercent,
                autoBrightness = autoBrightness
            )

            LaunchedEffect(file.uri, currentPage, activePageCount) {
                saveDeviceBookReadingProgress(context, file, currentPage, activePageCount)
                onProgressChanged()
            }
            LaunchedEffect(file.uri, activePageCount) {
                currentPage = currentPage.coerceIn(0, activePageCount.coerceAtLeast(1) - 1)
            }
            LaunchedEffect(file.uri, currentPage) {
                pdfPanX = 0f
                pdfPanY = 0f
            }
            LaunchedEffect(
                brightnessPercent,
                autoBrightness,
                blueLightFilterEnabled,
                blueLightOpacity,
                readerZoomPercent,
                selectedBrightnessEdge,
                resumeAutoBrightnessAfterInactivity,
                resumeAutoBrightnessMinutes
            ) {
                saveReaderDisplaySettings(
                    context = context,
                    brightnessPercent = brightnessPercent,
                    autoBrightness = autoBrightness,
                    blueLightFilterEnabled = blueLightFilterEnabled,
                    blueLightOpacity = blueLightOpacity,
                    pdfZoomPercent = readerZoomPercent,
                    brightnessEdge = selectedBrightnessEdge,
                    resumeAutoBrightness = resumeAutoBrightnessAfterInactivity,
                    resumeAutoBrightnessMinutes = resumeAutoBrightnessMinutes
                )
            }
            LaunchedEffect(brightnessFeedbackPercent) {
                if (brightnessFeedbackPercent != null) {
                    delay(900)
                    brightnessFeedbackPercent = null
                }
            }
            LaunchedEffect(
                brightnessEdgeChangeCount,
                resumeAutoBrightnessAfterInactivity,
                resumeAutoBrightnessMinutes
            ) {
                if (brightnessEdgeChangeCount > 0 && resumeAutoBrightnessAfterInactivity) {
                    val minutes = resumeAutoBrightnessMinutes.toLongOrNull()?.coerceAtLeast(1L) ?: 60L
                    delay(minutes * 60_000L)
                    autoBrightness = true
                }
            }
            val pdfTransformState = rememberTransformableState { zoomChange, panChange, _ ->
                if (!controlsVisible) {
                    val targetZoomPercent = (readerZoomPercent * zoomChange)
                        .roundToInt()
                        .coerceIn(READER_PDF_ZOOM_MIN, READER_PDF_ZOOM_MAX)
                    if (targetZoomPercent != readerZoomPercent) {
                        readerZoomPercent = targetZoomPercent
                    }
                    pdfPanX += panChange.x
                    pdfPanY += panChange.y
                }
            }

            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = drawerState.isOpen,
                drawerContent = {
                    PdfReaderNavigationDrawer(
                        file = file,
                        currentPage = currentPage,
                        pageCount = activePageCount,
                        onPageSelected = { page ->
                            currentPage = page.coerceIn(0, activePageCount - 1)
                            scope.launch { drawerState.close() }
                        },
                        onClose = { scope.launch { drawerState.close() } }
                    )
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .transformable(
                            state = pdfTransformState,
                            enabled = !controlsVisible
                        )
                ) {
                    PdfReaderPage(
                        file = file,
                        pageIndex = currentPage,
                        pdfZoomPercent = readerZoomPercent,
                        pdfPanX = pdfPanX,
                        pdfPanY = pdfPanY,
                        coverText = userMetadata.coverText,
                        overrideCoverId = userMetadata.coverId.takeIf { it.isNotBlank() },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (blueLightFilterEnabled && blueLightOpacity > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFFFB35A).copy(alpha = blueLightOpacity / 100f * 0.46f))
                        )
                    }

                    if (controlsVisible) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(1f)
                                .clickable {
                                    controlsVisible = false
                                    showDisplaySettings = false
                                    showTextSizeSettings = false
                                }
                        )
                    } else {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .weight(0.33f)
                                    .fillMaxHeight()
                                    .clickable {
                                        currentPage = (currentPage - 1).coerceAtLeast(0)
                                    }
                            )
                            Box(
                                modifier = Modifier
                                    .weight(0.34f)
                                    .fillMaxHeight()
                                    .clickable { controlsVisible = true }
                            )
                            Box(
                                modifier = Modifier
                                    .weight(0.33f)
                                    .fillMaxHeight()
                                    .clickable {
                                        currentPage = (currentPage + 1).coerceAtMost(activePageCount - 1)
                                    }
                            )
                        }
                    }

                    if (controlsVisible) {
                        PdfReaderTopControls(
                            title = file.name.substringBeforeLast('.'),
                            onDismiss = onDismiss,
                            onTitleClick = { showBookInfo = true },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .zIndex(2f)
                        )
                        if (!showDisplaySettings && !showTextSizeSettings) {
                            PdfReaderBottomControls(
                                currentPage = currentPage,
                                pageCount = activePageCount,
                                progress = progress,
                                showReset = resetSliderPage != null && resetSliderPage != currentPage,
                                onPageSelected = { page ->
                                    val targetPage = page.coerceIn(0, activePageCount - 1)
                                    if (targetPage != currentPage && resetSliderPage == null) {
                                        resetSliderPage = currentPage
                                    }
                                    currentPage = targetPage
                                    if (resetSliderPage == currentPage) {
                                        resetSliderPage = null
                                    }
                                },
                                onResetPage = {
                                    resetSliderPage?.let { page -> currentPage = page.coerceIn(0, activePageCount - 1) }
                                    resetSliderPage = null
                                },
                                onOpenNavigation = { scope.launch { drawerState.open() } },
                                onOpenDisplaySettings = {
                                    showDisplaySettings = true
                                    showTextSizeSettings = false
                                },
                                onOpenTextSizeSettings = {
                                    showTextSizeSettings = true
                                    showDisplaySettings = false
                                    zoomPercentBeforeChange = null
                                },
                                annotationsCount = annotations.size,
                                onOpenAnnotations = { showAnnotationsDialog = true },
                                onAddBookmark = {
                                    annotationRepository.addAnnotation(
                                        file = file,
                                        annotation = DeviceReaderAnnotation(
                                            type = DeviceReaderAnnotationType.Bookmark,
                                            page = currentPage,
                                            pageCount = activePageCount,
                                            note = "Marcador de lectura"
                                        )
                                    )
                                    annotations = annotationRepository.getAnnotations(file)
                                },
                                onAddHighlight = {
                                    annotationRepository.addAnnotation(
                                        file = file,
                                        annotation = DeviceReaderAnnotation(
                                            type = DeviceReaderAnnotationType.Highlight,
                                            page = currentPage,
                                            pageCount = activePageCount,
                                            color = "#EBC7E8"
                                        )
                                    )
                                    annotations = annotationRepository.getAnnotations(file)
                                },
                                onAddNote = { showNoteDialog = true },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .zIndex(2f)
                            )
                        }
                    } else {
                        ReaderBrightnessEdgeGesture(
                            selectedEdge = selectedBrightnessEdge,
                            onBrightnessDelta = { delta ->
                                brightnessPercent = (brightnessPercent + delta).coerceIn(0, 100)
                                brightnessFeedbackPercent = brightnessPercent
                                autoBrightness = false
                                brightnessEdgeChangeCount += 1
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(1f)
                        )
                    }

                    if (showDisplaySettings) {
                        PdfDisplaySettingsPanel(
                            brightnessPercent = brightnessPercent,
                            autoBrightness = autoBrightness,
                            blueLightFilterEnabled = blueLightFilterEnabled,
                            blueLightOpacity = blueLightOpacity,
                            onBrightnessChange = {
                                brightnessPercent = it.coerceIn(0, 100)
                                brightnessFeedbackPercent = brightnessPercent
                                autoBrightness = false
                            },
                            onAutoBrightnessChange = { autoBrightness = it },
                            onBlueLightFilterChange = { blueLightFilterEnabled = it },
                            onBlueLightOpacityChange = { blueLightOpacity = it.coerceIn(0, 100) },
                            onOpenBrightnessAdvancedSettings = { showBrightnessAdvancedSettings = true },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .zIndex(3f)
                        )
                    }

                    if (showTextSizeSettings) {
                        ReaderTextSizePanel(
                            zoomPercent = readerZoomPercent,
                            isReflowMode = false,
                            canUseReflowMode = false,
                            showReset = zoomPercentBeforeChange != null && zoomPercentBeforeChange != readerZoomPercent,
                            onZoomPercentChange = { newZoomPercent ->
                                val targetZoomPercent = newZoomPercent.coerceIn(READER_PDF_ZOOM_MIN, READER_PDF_ZOOM_MAX)
                                if (targetZoomPercent != readerZoomPercent && zoomPercentBeforeChange == null) {
                                    zoomPercentBeforeChange = readerZoomPercent
                                }
                                readerZoomPercent = targetZoomPercent
                                if (zoomPercentBeforeChange == readerZoomPercent) {
                                    zoomPercentBeforeChange = null
                                }
                            },
                            onResetZoomPercent = {
                                zoomPercentBeforeChange?.let { readerZoomPercent = it }
                                zoomPercentBeforeChange = null
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .zIndex(3f)
                        )
                    }

                    brightnessFeedbackPercent?.let { percent ->
                        ReaderBrightnessFeedback(
                            percent = percent,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .zIndex(4f)
                        )
                    }

                    if (showBrightnessAdvancedSettings) {
                        BrightnessAdvancedSettingsDialog(
                            selectedEdge = selectedBrightnessEdge,
                            resumeAutoBrightnessAfterInactivity = resumeAutoBrightnessAfterInactivity,
                            resumeAutoBrightnessMinutes = resumeAutoBrightnessMinutes,
                            onSelectedEdgeChange = { selectedBrightnessEdge = it },
                            onResumeAutoBrightnessAfterInactivityChange = {
                                resumeAutoBrightnessAfterInactivity = it
                            },
                            onOpenResumeMinutesSettings = { showAutoBrightnessMinutesDialog = true },
                            onDismiss = { showBrightnessAdvancedSettings = false }
                        )
                    }

                    if (showAutoBrightnessMinutesDialog) {
                        AutoBrightnessMinutesDialog(
                            minutes = resumeAutoBrightnessMinutes,
                            onMinutesChange = { resumeAutoBrightnessMinutes = it.filter(Char::isDigit).take(4) },
                            onDismiss = { showAutoBrightnessMinutesDialog = false }
                        )
                    }

                    if (showBookInfo) {
                        PdfBookInfoDialog(
                            file = file,
                            currentPage = currentPage,
                            pageCount = activePageCount,
                            progress = progress,
                            onMore = {
                                showBookInfo = false
                                showBookInfoMore = true
                            },
                            onDismiss = { showBookInfo = false }
                        )
                    }

                    if (showBookInfoMore) {
                        PdfBookInfoMoreDialog(onDismiss = { showBookInfoMore = false })
                    }

                    if (showAnnotationsDialog) {
                        ReflowAnnotationsDialog(
                            annotations = annotations,
                            onAnnotationSelected = { annotation ->
                                currentPage = annotation.page.coerceIn(0, activePageCount - 1)
                                showAnnotationsDialog = false
                                controlsVisible = false
                            },
                            onAnnotationEdit = { annotation -> editingAnnotation = annotation },
                            onAnnotationDelete = { annotation ->
                                annotationRepository.deleteAnnotation(file, annotation.id)
                                annotations = annotationRepository.getAnnotations(file)
                            },
                            onDismiss = { showAnnotationsDialog = false }
                        )
                    }

                    if (showNoteDialog) {
                        ReaderNoteDialog(
                            onSave = { note ->
                                annotationRepository.addAnnotation(
                                    file = file,
                                    annotation = DeviceReaderAnnotation(
                                        type = DeviceReaderAnnotationType.Note,
                                        page = currentPage,
                                        pageCount = activePageCount,
                                        note = note
                                    )
                                )
                                annotations = annotationRepository.getAnnotations(file)
                                showNoteDialog = false
                            },
                            onDismiss = { showNoteDialog = false }
                        )
                    }

                    editingAnnotation?.let { annotation ->
                        ReaderAnnotationEditDialog(
                            annotation = annotation,
                            onSave = { updated ->
                                annotationRepository.updateAnnotation(file, updated)
                                annotations = annotationRepository.getAnnotations(file)
                                editingAnnotation = null
                            },
                            onDelete = {
                                annotationRepository.deleteAnnotation(file, annotation.id)
                                annotations = annotationRepository.getAnnotations(file)
                                editingAnnotation = null
                            },
                            onDismiss = { editingAnnotation = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfReaderTopControls(
    title: String,
    onDismiss: () -> Unit,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF151515).copy(alpha = 0.96f))
                .statusBarsPadding()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onTitleClick)
                    .padding(vertical = 10.dp)
            )
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Más opciones",
                        tint = Color.White
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color(0xFF252525))
                ) {
                    listOf(
                        "Opciones de Visualización",
                        "Opciones de Control",
                        "Otros ajustes",
                        "Temas",
                        "Reemplazo de nombres",
                        "Más operaciones...",
                        "Compartir...",
                        "Información del libro"
                    ).forEach { label ->
                        DropdownMenuItem(
                            text = { Text(text = label, color = Color.White) },
                            onClick = { menuExpanded = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfReaderBottomControls(
    currentPage: Int,
    pageCount: Int,
    progress: Int,
    showReset: Boolean,
    onPageSelected: (Int) -> Unit,
    onResetPage: () -> Unit,
    onOpenNavigation: () -> Unit,
    onOpenDisplaySettings: () -> Unit,
    onOpenTextSizeSettings: () -> Unit,
    annotationsCount: Int,
    onOpenAnnotations: () -> Unit,
    onAddBookmark: () -> Unit,
    onAddHighlight: () -> Unit,
    onAddNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF151515).copy(alpha = 0.96f))
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.size(18.dp)
            )
            Slider(
                value = (currentPage + 1).toFloat(),
                onValueChange = { value ->
                    onPageSelected(value.roundToInt() - 1)
                },
                valueRange = 1f..pageCount.toFloat().coerceAtLeast(2f),
                enabled = pageCount > 1,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$progress%",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            if (showReset) {
                IconButton(
                    onClick = onResetPage,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay,
                        contentDescription = "Volver a la página anterior",
                        tint = Color(0xFFEBC7E8)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.size(18.dp)
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReaderToolButton(Icons.AutoMirrored.Filled.VolumeUp, "Lectura en voz alta")
            ReaderToolButton(Icons.AutoMirrored.Filled.FormatListBulleted, "Capítulos y marcadores", onOpenNavigation)
            ReaderToolButton(Icons.Filled.Brightness6, "Brillo", onOpenDisplaySettings)
            ReaderToolButton(Icons.Filled.FormatSize, "Tamaño del texto", onOpenTextSizeSettings)
            ReaderToolButton(Icons.Filled.BookmarkBorder, "Añadir marcador", onAddBookmark)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReaderToolButton(Icons.AutoMirrored.Filled.FormatAlignLeft, "Alineación")
            ReaderToolButton(Icons.Filled.Star, "Subrayar selección", onAddHighlight)
            ReaderToolButton(Icons.Filled.MoreHoriz, "Añadir nota", onAddNote)
            Box {
                ReaderToolButton(Icons.Filled.Search, "Ver anotaciones", onOpenAnnotations)
                if (annotationsCount > 0) {
                    Text(
                        text = annotationsCount.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = TarnishedGold,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }
            ReaderToolButton(Icons.Filled.TableRows, "Vista de página")
        }
    }
}

@Composable
private fun ReaderToolButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit = {},
    enabled: Boolean = true
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color(0xFFEBC7E8) else Color.White.copy(alpha = 0.34f)
        )
    }
}

@Composable
private fun ReflowReaderBottomControls(
    currentPage: Int,
    pageCount: Int,
    progress: Int,
    subtitle: String,
    onPageSelected: (Int) -> Unit,
    annotationsCount: Int,
    onOpenTextSizeSettings: () -> Unit,
    onOpenAnnotations: () -> Unit,
    onAddBookmark: () -> Unit,
    onAddHighlight: () -> Unit,
    onAddNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF151515).copy(alpha = 0.96f))
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TarnishedGold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.size(18.dp)
            )
            Slider(
                value = (currentPage + 1).toFloat(),
                onValueChange = { value -> onPageSelected(value.roundToInt() - 1) },
                valueRange = 1f..pageCount.toFloat().coerceAtLeast(2f),
                enabled = pageCount > 1,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$progress%",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.size(18.dp)
            )
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReaderToolButton(Icons.Filled.FormatSize, "Tamaño del texto", onOpenTextSizeSettings)
            ReaderToolButton(Icons.Filled.BookmarkBorder, "Añadir marcador", onAddBookmark)
            ReaderToolButton(Icons.Filled.Star, "Subrayar página", onAddHighlight)
            ReaderToolButton(Icons.Filled.MoreHoriz, "Añadir nota", onAddNote)
            Box {
                ReaderToolButton(Icons.AutoMirrored.Filled.FormatListBulleted, "Ver anotaciones", onOpenAnnotations)
                if (annotationsCount > 0) {
                    Text(
                        text = annotationsCount.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = TarnishedGold,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReflowAnnotationsDialog(
    annotations: List<DeviceReaderAnnotation>,
    onAnnotationSelected: (DeviceReaderAnnotation) -> Unit,
    onAnnotationEdit: (DeviceReaderAnnotation) -> Unit,
    onAnnotationDelete: (DeviceReaderAnnotation) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.72f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF202020))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Anotaciones",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            if (annotations.isEmpty()) {
                Text(
                    text = "Todavía no hay marcadores, subrayados ni notas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(annotations.sortedWith(compareBy<DeviceReaderAnnotation> { it.page }.thenBy { it.createdAtMillis })) { annotation ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.26f))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${annotation.type.readableName()} · Página ${annotation.page + 1}/${annotation.pageCount}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TarnishedGold
                            )
                            annotation.note.takeIf { it.isNotBlank() }?.let { note ->
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                            annotation.selectedText.takeIf { it.isNotBlank() }?.let { selectedText ->
                                Text(
                                    text = selectedText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.76f),
                                    overflow = TextOverflow.Clip
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { onAnnotationSelected(annotation) }) {
                                    Text(text = "IR", color = TarnishedGold)
                                }
                                TextButton(onClick = { onAnnotationEdit(annotation) }) {
                                    Text(text = "EDITAR", color = Color.White)
                                }
                                TextButton(onClick = { onAnnotationDelete(annotation) }) {
                                    Text(text = "ELIMINAR", color = Color(0xFFFFB4A8))
                                }
                            }
                        }
                    }
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = "CERRAR", color = Color.White)
            }
        }
    }
}

@Composable
private fun ReaderNoteDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var noteText by remember { mutableStateOf("") }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF202020))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Nueva nota",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            TextField(
                value = noteText,
                onValueChange = { noteText = it.take(500) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = { Text("Escribe una nota de lectura") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = "CANCELAR", color = Color.White)
                }
                TextButton(
                    onClick = { onSave(noteText.trim()) },
                    enabled = noteText.isNotBlank()
                ) {
                    Text(text = "GUARDAR", color = TarnishedGold)
                }
            }
        }
    }
}

@Composable
private fun ReaderAnnotationEditDialog(
    annotation: DeviceReaderAnnotation,
    onSave: (DeviceReaderAnnotation) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var noteText by remember(annotation.id) { mutableStateOf(annotation.note) }
    var selectedText by remember(annotation.id) { mutableStateOf(annotation.selectedText) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.78f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF202020))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${annotation.type.readableName()} · Página ${annotation.page + 1}/${annotation.pageCount}",
                style = MaterialTheme.typography.titleMedium,
                color = TarnishedGold
            )
            TextField(
                value = selectedText,
                onValueChange = { selectedText = it.take(READER_ANNOTATION_TEXT_LIMIT) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                minLines = 5,
                label = { Text("Texto seleccionado") }
            )
            TextField(
                value = noteText,
                onValueChange = { noteText = it.take(800) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text("Nota") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDelete) {
                    Text(text = "ELIMINAR", color = Color(0xFFFFB4A8))
                }
                Row {
                    TextButton(onClick = onDismiss) {
                        Text(text = "CANCELAR", color = Color.White)
                    }
                    TextButton(
                        onClick = {
                            onSave(
                                annotation.copy(
                                    selectedText = selectedText.trim(),
                                    note = noteText.trim()
                                )
                            )
                        }
                    ) {
                        Text(text = "GUARDAR", color = TarnishedGold)
                    }
                }
            }
        }
    }
}

private fun DeviceReaderAnnotationType.readableName(): String {
    return when (this) {
        DeviceReaderAnnotationType.Bookmark -> "Marcador"
        DeviceReaderAnnotationType.Highlight -> "Subrayado"
        DeviceReaderAnnotationType.Note -> "Nota"
    }
}

private fun normalizeSelectionRange(
    start: Int?,
    end: Int?,
    textLength: Int
): IntRange? {
    if (start == null || end == null || textLength <= 0) return null
    val safeStart = start.coerceIn(0, textLength)
    val safeEnd = end.coerceIn(0, textLength)
    val from = minOf(safeStart, safeEnd)
    val to = maxOf(safeStart, safeEnd)
    return if (from == to) null else from until to
}

private fun wordRangeAt(text: String, offset: Int): IntRange {
    if (text.isBlank()) return 0..0
    val safeOffset = offset.coerceIn(0, text.length - 1)
    if (text[safeOffset].isWhitespace()) {
        return safeOffset until (safeOffset + 1).coerceAtMost(text.length)
    }
    var start = safeOffset
    while (start > 0 && !text[start - 1].isWhitespace()) start -= 1
    var end = safeOffset + 1
    while (end < text.length && !text[end].isWhitespace()) end += 1
    return start until end
}

private fun buildSelectableReaderText(
    text: String,
    selectedRange: IntRange?
): AnnotatedString {
    if (selectedRange == null) return AnnotatedString(text)
    val start = selectedRange.first.coerceIn(0, text.length)
    val end = (selectedRange.last + 1).coerceIn(start, text.length)
    return buildAnnotatedString {
        append(text.substring(0, start))
        withStyle(SpanStyle(background = Color(0x66EBC7E8))) {
            append(text.substring(start, end))
        }
        append(text.substring(end))
    }
}

@Composable
private fun ReaderTextSizePanel(
    zoomPercent: Int,
    isReflowMode: Boolean,
    canUseReflowMode: Boolean,
    showReset: Boolean,
    onZoomPercentChange: (Int) -> Unit,
    onResetZoomPercent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.96f))
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showReset) {
                IconButton(
                    onClick = onResetZoomPercent,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay,
                        contentDescription = "Restaurar tamaño de texto",
                        tint = Color.White
                    )
                }
            }
            Text(
                text = "$zoomPercent%",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = when {
                isReflowMode -> "Tamaño de lectura remaquetada"
                canUseReflowMode -> "Zoom visual; cambia a vista reflow desde Vista"
                else -> "Zoom visual: este PDF no tiene texto extraíble para reflow"
            },
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Slider(
                value = zoomPercent.toFloat(),
                onValueChange = { onZoomPercentChange(it.roundToInt()) },
                valueRange = READER_PDF_ZOOM_MIN.toFloat()..READER_PDF_ZOOM_MAX.toFloat(),
                modifier = Modifier.weight(1f)
            )
            ReaderStepButton(
                text = "-",
                onClick = { onZoomPercentChange(zoomPercent - 1) }
            )
            ReaderStepButton(
                text = "+",
                onClick = { onZoomPercentChange(zoomPercent + 1) }
            )
        }
    }
}

@Composable
private fun PdfDisplaySettingsPanel(
    brightnessPercent: Int,
    autoBrightness: Boolean,
    blueLightFilterEnabled: Boolean,
    blueLightOpacity: Int,
    onBrightnessChange: (Int) -> Unit,
    onAutoBrightnessChange: (Boolean) -> Unit,
    onBlueLightFilterChange: (Boolean) -> Unit,
    onBlueLightOpacityChange: (Int) -> Unit,
    onOpenBrightnessAdvancedSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.96f))
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "Brillo:",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        ReaderPercentControl(
            value = brightnessPercent,
            onValueChange = onBrightnessChange,
            showSettings = true,
            onSettingsClick = onOpenBrightnessAdvancedSettings,
            leadingContent = {
                Checkbox(
                    checked = autoBrightness,
                    onCheckedChange = onAutoBrightnessChange,
                    modifier = Modifier.size(32.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFB9C7FF),
                        uncheckedColor = Color.White
                    )
                )
                Text(
                    text = "Auto",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(30.dp)
        ) {
            Checkbox(
                checked = blueLightFilterEnabled,
                onCheckedChange = onBlueLightFilterChange,
                modifier = Modifier.size(32.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFFB9C7FF),
                    uncheckedColor = Color.White
                )
            )
            Text(
                text = "Filtro de luz azul para el cuidado de la vista",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
        ReaderPercentControl(
            value = blueLightOpacity,
            onValueChange = onBlueLightOpacityChange,
            showSettings = false,
            onSettingsClick = {},
            leadingContent = {
                Text(
                    text = "Opacidad",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.width(58.dp)
                )
            }
        )
    }
}

@Composable
private fun ReaderPercentControl(
    value: Int,
    onValueChange: (Int) -> Unit,
    showSettings: Boolean,
    onSettingsClick: () -> Unit,
    leadingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        leadingContent?.invoke()
        ReaderPercentSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f)
        )
        ReaderStepButton(text = "-", onClick = { onValueChange(value - 1) })
        ReaderStepButton(text = "+", onClick = { onValueChange(value + 1) })
        if (showSettings) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Ajustes de brillo",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ReaderPercentSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.height(50.dp)
    ) {
        val safeValue = value.coerceIn(0, 100)
        val labelWidth = 36.dp
        val fraction = safeValue / 100f
        val labelOffset = ((maxWidth - labelWidth) * fraction).coerceIn(0.dp, maxWidth - labelWidth)
        Text(
            text = "$safeValue%",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(labelWidth)
                .offset(x = labelOffset)
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.78f))
        )
        Slider(
            value = safeValue.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ReaderStepButton(
    text: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.size(width = 28.dp, height = 30.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReaderBrightnessFeedback(
    percent: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF5C7FB9).copy(alpha = 0.92f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ReaderBrightnessEdgeGesture(
    selectedEdge: String,
    onBrightnessDelta: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        val edgeModifier = Modifier
            .then(
                when (selectedEdge) {
                    "Borde derecho" -> Modifier
                        .align(Alignment.CenterEnd)
                        .width(28.dp)
                        .fillMaxHeight()
                    "Borde superior" -> Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(28.dp)
                    "Borde inferior" -> Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(28.dp)
                    else -> Modifier
                        .align(Alignment.CenterStart)
                        .width(28.dp)
                        .fillMaxHeight()
                }
            )
            .pointerInput(selectedEdge) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val rawDelta = when (selectedEdge) {
                        "Borde superior", "Borde inferior" -> dragAmount.x / 8f
                        else -> -dragAmount.y / 8f
                    }
                    val delta = rawDelta.roundToInt().let {
                        when {
                            it != 0 -> it
                            rawDelta > 0f -> 1
                            rawDelta < 0f -> -1
                            else -> 0
                        }
                    }
                    if (delta != 0) onBrightnessDelta(delta)
                }
            }
        Box(modifier = edgeModifier)
    }
}

@Composable
private fun BrightnessAdvancedSettingsDialog(
    selectedEdge: String,
    resumeAutoBrightnessAfterInactivity: Boolean,
    resumeAutoBrightnessMinutes: String,
    onSelectedEdgeChange: (String) -> Unit,
    onResumeAutoBrightnessAfterInactivityChange: (Boolean) -> Unit,
    onOpenResumeMinutesSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    var edgeMenuExpanded by remember { mutableStateOf(false) }
    val edgeOptions = listOf(
        "Borde izquierdo",
        "Borde derecho",
        "Borde superior",
        "Borde inferior"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.58f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF202020))
                    .clickable(onClick = {})
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Brillo:",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "Ajustar brillo a",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { edgeMenuExpanded = true }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedEdge,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.62f)
                            )
                        }
                        DropdownMenu(
                            expanded = edgeMenuExpanded,
                            onDismissRequest = { edgeMenuExpanded = false },
                            modifier = Modifier.background(Color(0xFF252525))
                        ) {
                            edgeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option,
                                            color = Color.White
                                        )
                                    },
                                    onClick = {
                                        onSelectedEdgeChange(option)
                                        edgeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = resumeAutoBrightnessAfterInactivity,
                        onCheckedChange = onResumeAutoBrightnessAfterInactivityChange,
                        modifier = Modifier.size(32.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFFB9C7FF),
                            uncheckedColor = Color.White
                        )
                    )
                    Text(
                        text = "Reanudar el brillo\nautomático después de ${
                            formatReaderDuration(resumeAutoBrightnessMinutes)
                        }\nde inactividad",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onOpenResumeMinutesSettings,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Ajustar minutos",
                            tint = Color(0xFFEBC7E8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "ACEPTAR", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoBrightnessMinutesDialog(
    minutes: String,
    onMinutesChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var draftTotalMinutes by remember(minutes) {
        mutableStateOf(minutes.toIntOrNull()?.coerceIn(0, 23 * 60 + 59) ?: 60)
    }
    val selectedHour = draftTotalMinutes / 60
    val selectedMinute = draftTotalMinutes % 60

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.36f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF202020))
                    .clickable(onClick = {})
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimePickerColumn(
                        label = "Horas",
                        values = 0..23,
                        selectedValue = selectedHour,
                        onValueSelected = { hour ->
                            draftTotalMinutes = ((hour * 60) + selectedMinute).coerceIn(0, 23 * 60 + 59)
                        }
                    )
                    TimePickerSeparator()
                    TimePickerColumn(
                        label = "Minutos",
                        values = 0..59,
                        selectedValue = selectedMinute,
                        onValueSelected = { minute ->
                            draftTotalMinutes = ((selectedHour * 60) + minute).coerceIn(0, 23 * 60 + 59)
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "CANCELAR", color = Color.White)
                    }
                    TextButton(
                        onClick = {
                            onMinutesChange(draftTotalMinutes.toString())
                            onDismiss()
                        }
                    ) {
                        Text(text = "ACEPTAR", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimePickerSeparator() {
    Column(
        modifier = Modifier.width(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier.height(144.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ":",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TimePickerColumn(
    label: String,
    values: IntRange,
    selectedValue: Int,
    onValueSelected: (Int) -> Unit
) {
    val valuesList = remember(values) { values.toList() }
    val selectedIndex = valuesList.indexOf(selectedValue).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val scope = rememberCoroutineScope()

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val layoutInfo = listState.layoutInfo
        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
        val centeredItem = layoutInfo.visibleItemsInfo.minByOrNull { item ->
            kotlin.math.abs((item.offset + item.size / 2) - viewportCenter)
        }
        centeredItem?.let { item ->
            valuesList.getOrNull(item.index)?.let { centeredValue ->
                if (centeredValue != selectedValue) {
                    onValueSelected(centeredValue)
                }
            }
        }
    }
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            val centeredItem = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs((item.offset + item.size / 2) - viewportCenter)
            } ?: return@LaunchedEffect
            valuesList.getOrNull(centeredItem.index)?.let { centeredValue ->
                if (centeredValue != selectedValue) {
                    onValueSelected(centeredValue)
                }
                listState.animateScrollToItem(centeredItem.index)
            }
        }
    }

    Column(
        modifier = Modifier.width(92.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier
                .height(144.dp)
                .width(92.dp),
            contentPadding = PaddingValues(vertical = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(valuesList) { value ->
                val selected = value == selectedValue
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1.32f else 0.9f,
                    animationSpec = tween(durationMillis = 180),
                    label = "timePickerScale"
                )
                val alpha by animateFloatAsState(
                    targetValue = if (selected) 1f else 0.32f,
                    animationSpec = tween(durationMillis = 180),
                    label = "timePickerAlpha"
                )
                Text(
                    text = value.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White.copy(alpha = alpha),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clickable {
                            onValueSelected(value)
                            scope.launch {
                                listState.animateScrollToItem(valuesList.indexOf(value).coerceAtLeast(0))
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun ApplyReaderBrightness(
    brightnessPercent: Int,
    autoBrightness: Boolean
) {
    val view = LocalView.current
    val window = remember(view) { view.context.findActivity()?.window }
    val originalBrightness = remember(window) { window?.attributes?.screenBrightness }
    DisposableEffect(view, brightnessPercent, autoBrightness) {
        if (window != null) {
            val attributes = window.attributes
            attributes.screenBrightness = if (autoBrightness) {
                android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            } else {
                (brightnessPercent.coerceIn(1, 100) / 100f)
            }
            window.attributes = attributes
        }
        onDispose {
            if (window != null && originalBrightness != null) {
                val attributes = window.attributes
                attributes.screenBrightness = originalBrightness
                window.attributes = attributes
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun formatReaderDuration(minutesText: String): String {
    val totalMinutes = minutesText.toIntOrNull()?.coerceAtLeast(0) ?: 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours <= 0 -> "$minutes minutos"
        minutes == 0 -> "$hours ${if (hours == 1) "hora" else "horas"}"
        else -> "$hours ${if (hours == 1) "hora" else "horas"} y $minutes minutos"
    }
}

private fun readerDisplaySettingsPreferences(context: Context) =
    context.getSharedPreferences("device_reader_display_settings", Context.MODE_PRIVATE)

private const val READER_PDF_ZOOM_MIN = 50
private const val READER_PDF_ZOOM_MAX = 150
private const val READER_PDF_ZOOM_DEFAULT = 100
private const val READER_ANNOTATION_TEXT_LIMIT = 480

private fun readReaderBrightnessPercent(context: Context): Int {
    return readerDisplaySettingsPreferences(context)
        .getInt("brightness_percent", 50)
        .coerceIn(0, 100)
}

private fun readReaderAutoBrightness(context: Context): Boolean {
    return readerDisplaySettingsPreferences(context)
        .getBoolean("auto_brightness", true)
}

private fun readReaderBlueLightFilterEnabled(context: Context): Boolean {
    return readerDisplaySettingsPreferences(context)
        .getBoolean("blue_light_filter_enabled", false)
}

private fun readReaderBlueLightOpacity(context: Context): Int {
    return readerDisplaySettingsPreferences(context)
        .getInt("blue_light_opacity", 35)
        .coerceIn(0, 100)
}

private fun readReaderPdfZoomPercent(context: Context): Int {
    return readerDisplaySettingsPreferences(context)
        .getInt("pdf_zoom_percent", READER_PDF_ZOOM_DEFAULT)
        .coerceIn(READER_PDF_ZOOM_MIN, READER_PDF_ZOOM_MAX)
}

private fun saveReaderTextSizePercent(context: Context, textSizePercent: Int) {
    readerDisplaySettingsPreferences(context)
        .edit()
        .putInt("pdf_zoom_percent", textSizePercent.coerceIn(READER_PDF_ZOOM_MIN, READER_PDF_ZOOM_MAX))
        .apply()
}

private fun readerPdfZoomPercentToScale(zoomPercent: Int): Float {
    return zoomPercent.coerceIn(READER_PDF_ZOOM_MIN, READER_PDF_ZOOM_MAX) / 100f
}

private fun paginatePdfReflowText(
    sourcePages: List<String>,
    fontSizePx: Float,
    lineHeightPx: Float,
    maxWidthPx: Int,
    maxHeightPx: Int
): List<String> {
    val normalizedText = sourcePages
        .map { it.replace(Regex("\\s+"), " ").trim() }
        .filter { it.isNotBlank() }
        .joinToString("\n\n")
    if (normalizedText.isBlank() || maxWidthPx <= 0 || maxHeightPx <= 0) return listOf("")

    val paint = TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = fontSizePx
        color = android.graphics.Color.rgb(26, 18, 11)
    }
    val layout = StaticLayout.Builder
        .obtain(normalizedText, 0, normalizedText.length, paint, maxWidthPx)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setIncludePad(false)
        .setLineSpacing(lineHeightPx - fontSizePx, 1f)
        .build()
    val blocks = mutableListOf<String>()
    var startLine = 0

    // Se corta por lineas ya maquetadas fuera de Compose para que el cambio de pagina sea estable.
    while (startLine < layout.lineCount) {
        val startTop = layout.getLineTop(startLine)
        var endLine = startLine
        while (endLine < layout.lineCount) {
            val blockHeight = layout.getLineBottom(endLine) - startTop
            if (blockHeight > maxHeightPx && endLine > startLine) break
            if (blockHeight > maxHeightPx) break
            endLine += 1
        }
        val safeEndLine = endLine.coerceAtLeast(startLine + 1).coerceAtMost(layout.lineCount)
        val startOffset = layout.getLineStart(startLine)
        val endOffset = layout.getLineEnd(safeEndLine - 1)
        blocks += normalizedText.substring(startOffset, endOffset).trim()
        startLine = safeEndLine
    }
    return blocks.filter { it.isNotBlank() }.ifEmpty { listOf("") }
}

private fun readReaderBrightnessEdge(context: Context): String {
    return readerDisplaySettingsPreferences(context)
        .getString("brightness_edge", "Borde izquierdo")
        .orEmpty()
        .takeIf {
            it in setOf("Borde izquierdo", "Borde derecho", "Borde superior", "Borde inferior")
        }
        ?: "Borde izquierdo"
}

private fun readReaderResumeAutoBrightness(context: Context): Boolean {
    return readerDisplaySettingsPreferences(context)
        .getBoolean("resume_auto_brightness", false)
}

private fun readReaderResumeAutoBrightnessMinutes(context: Context): String {
    return readerDisplaySettingsPreferences(context)
        .getString("resume_auto_brightness_minutes", "60")
        .orEmpty()
        .filter(Char::isDigit)
        .takeIf { it.isNotBlank() }
        ?: "60"
}

private fun saveReaderDisplaySettings(
    context: Context,
    brightnessPercent: Int,
    autoBrightness: Boolean,
    blueLightFilterEnabled: Boolean,
    blueLightOpacity: Int,
    pdfZoomPercent: Int,
    brightnessEdge: String,
    resumeAutoBrightness: Boolean,
    resumeAutoBrightnessMinutes: String
) {
    readerDisplaySettingsPreferences(context)
        .edit()
        .putInt("brightness_percent", brightnessPercent.coerceIn(0, 100))
        .putBoolean("auto_brightness", autoBrightness)
        .putBoolean("blue_light_filter_enabled", blueLightFilterEnabled)
        .putInt("blue_light_opacity", blueLightOpacity.coerceIn(0, 100))
        .putInt("pdf_zoom_percent", pdfZoomPercent.coerceIn(READER_PDF_ZOOM_MIN, READER_PDF_ZOOM_MAX))
        .putString("brightness_edge", brightnessEdge)
        .putBoolean("resume_auto_brightness", resumeAutoBrightness)
        .putString(
            "resume_auto_brightness_minutes",
            resumeAutoBrightnessMinutes.filter(Char::isDigit).takeIf { it.isNotBlank() } ?: "60"
        )
        .apply()
}

private enum class PdfReaderDrawerTab {
    Chapters,
    Bookmarks,
    Images
}

@Composable
private fun PdfReaderNavigationDrawer(
    file: DeviceLibraryFile,
    currentPage: Int,
    pageCount: Int,
    onPageSelected: (Int) -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(PdfReaderDrawerTab.Chapters) }
    val context = LocalContext.current
    val userMetadata = remember(file.uri) { readDeviceBookUserMetadata(context, file) }
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(292.dp)
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(Color(0xFF171717))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White
                )
            }
            PdfDrawerTabButton(
                text = "Capítulos",
                selected = selectedTab == PdfReaderDrawerTab.Chapters,
                onClick = { selectedTab = PdfReaderDrawerTab.Chapters }
            )
            PdfDrawerTabButton(
                text = "Marcadores",
                selected = selectedTab == PdfReaderDrawerTab.Bookmarks,
                onClick = { selectedTab = PdfReaderDrawerTab.Bookmarks }
            )
            IconButton(onClick = { selectedTab = PdfReaderDrawerTab.Images }) {
                Icon(
                    imageVector = Icons.Filled.ImageIcon,
                    contentDescription = "Imágenes",
                    tint = if (selectedTab == PdfReaderDrawerTab.Images) Color.White else Color.White.copy(alpha = 0.72f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Box(
            modifier = Modifier
                .padding(
                    start = when (selectedTab) {
                        PdfReaderDrawerTab.Chapters -> 54.dp
                        PdfReaderDrawerTab.Bookmarks -> 124.dp
                        PdfReaderDrawerTab.Images -> 206.dp
                    }
                )
                .height(3.dp)
                .width(44.dp)
                .background(TarnishedGold)
        )
        when (selectedTab) {
            PdfReaderDrawerTab.Chapters -> PdfReaderChaptersTab(
                file = file,
                onPageSelected = onPageSelected,
                modifier = Modifier.weight(1f)
            )
            PdfReaderDrawerTab.Bookmarks -> PdfReaderBookmarksTab(
                file = file,
                currentPage = currentPage,
                pageCount = pageCount,
                onPageSelected = onPageSelected,
                modifier = Modifier.weight(1f)
            )
            PdfReaderDrawerTab.Images -> PdfReaderImagesTab(
                file = file,
                coverText = userMetadata.coverText,
                overrideCoverId = userMetadata.coverId.takeIf { it.isNotBlank() },
                onCoverSelected = { onPageSelected(0) },
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Página ${currentPage + 1}/$pageCount",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Buscar",
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(18.dp))
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Ajustes",
                tint = Color(0xFFEBC7E8)
            )
        }
    }
}

@Composable
private fun PdfDrawerTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (selected) Color.White else Color.White.copy(alpha = 0.76f),
        maxLines = 1,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 16.dp)
    )
}

@Composable
private fun PdfReaderChaptersTab(
    file: DeviceLibraryFile,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Cubierta",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPageSelected(0) }
                    .padding(vertical = 8.dp)
            )
        }
        item {
            Text(
                text = file.name.substringBeforeLast('.'),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        item {
            Text(
                text = "No hay capítulos detectados en este PDF.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.padding(start = 18.dp, top = 12.dp)
            )
        }
    }
}

@Composable
private fun PdfReaderBookmarksTab(
    file: DeviceLibraryFile,
    currentPage: Int,
    pageCount: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember(file.uri) { DeviceLibraryRepository(context) }
    var bookmarks by remember(file.uri) {
        mutableStateOf(
            repository.getAnnotations(file)
                .filter { it.type == DeviceReaderAnnotationType.Bookmark }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.28f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.BookmarkBorder,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.18f),
                modifier = Modifier.size(54.dp)
            )
        }
        Spacer(modifier = Modifier.height(42.dp))
        if (bookmarks.isEmpty()) {
            Text(
                text = "No hay marcador de página",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Toque agregar nuevo para guardar la página actual\n(Subrayados y notas usan el mismo modelo de anotaciones)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(bookmarks) { bookmark ->
                    Text(
                        text = "Página ${bookmark.page + 1}/${bookmark.pageCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPageSelected(bookmark.page.coerceIn(0, pageCount - 1)) }
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.BookmarkBorder,
                contentDescription = null,
                tint = Color(0xFFEBC7E8)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .background(Color(0xFF202020))
                    .clickable {
                        repository.addAnnotation(
                            file = file,
                            annotation = DeviceReaderAnnotation(
                                type = DeviceReaderAnnotationType.Bookmark,
                                page = currentPage,
                                pageCount = pageCount,
                                note = "Marcador de página"
                            )
                        )
                        bookmarks = repository.getAnnotations(file)
                            .filter { it.type == DeviceReaderAnnotationType.Bookmark }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AGREGAR NUEVO",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Buscar",
                tint = Color.White
            )
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Ajustes",
                tint = Color(0xFFEBC7E8)
            )
        }
    }
}

@Composable
private fun PdfReaderImagesTab(
    file: DeviceLibraryFile,
    coverText: String,
    overrideCoverId: String?,
    onCoverSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item {
            FilePagePreview(
                file = file,
                coverText = coverText,
                overrideCoverId = overrideCoverId,
                modifier = Modifier
                    .width(126.dp)
                    .aspectRatio(0.68f)
                    .clickable(onClick = onCoverSelected)
            )
        }
        item { PdfImagePlaceholderLabel("imagen detectada: cubierta") }
        items(listOf("ePUB", "T", "☕", "━━━━━━", "⋯")) { label ->
            PdfImagePlaceholderLabel(label)
        }
    }
}

@Composable
private fun PdfImagePlaceholderLabel(text: String) {
    Box(
        modifier = Modifier
            .widthIn(min = 48.dp)
            .height(34.dp)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PdfBookInfoDialog(
    file: DeviceLibraryFile,
    currentPage: Int,
    pageCount: Int,
    progress: Int,
    onMore: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val userMetadata = remember(file.uri) { readDeviceBookUserMetadata(context, file) }
    val scrollState = rememberScrollState()
    val title = userMetadata.title.takeIf { it.isNotBlank() } ?: file.name.substringBeforeLast('.')
    val author = userMetadata.author.takeIf { it.isNotBlank() }
    val description = userMetadata.description.takeIf { it.isNotBlank() }
        ?: "Información del libro pendiente de completar. Podrás ampliar estos datos desde las opciones del libro."

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.88f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF202020))
                    .clickable(onClick = {})
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Información del libro",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    repeat(5) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.16f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FilePagePreview(
                            file = file,
                            coverText = userMetadata.coverText,
                            overrideCoverId = userMetadata.coverId.takeIf { it.isNotBlank() },
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .aspectRatio(0.68f)
                                .border(8.dp, Color(0xFFA8C7B1))
                        )
                    }

                    Text(
                        text = listOfNotNull(title, author).joinToString(" - "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                    Text(
                        text = buildString {
                            appendLine("Nombre del archivo: ${file.name}")
                            appendLine("Ubicación: ${file.location}")
                            appendLine("Tamaño del archivo: ${file.sizeBytes?.let { Formatter.formatShortFileSize(context, it) } ?: "No disponible"}")
                            appendLine("Páginas totales: $pageCount")
                            appendLine("Página actual: ${currentPage + 1}")
                            appendLine("Progreso: $progress%")
                            file.modifiedAtMillis?.let {
                                appendLine("Última modificación: ${DateFormat.getDateTimeInstance().format(Date(it))}")
                            }
                            appendLine()
                            appendLine("Horas de lectura: --")
                            appendLine("Velocidad de lectura (p/m): --")
                            appendLine("Historial de lectura en días:")
                            appendLine()
                            appendLine("Capítulo actual: pendiente de detectar")
                            appendLine("Marcadores: pendiente de implementar")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onMore) {
                        Text(text = "MÁS...", color = Color.White)
                    }
                    TextButton(onClick = {}) {
                        Text(text = "FAVORITO", color = Color.White)
                    }
                    TextButton(onClick = onDismiss) {
                        Text(text = "ACEPTAR", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfBookInfoMoreDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF252525))
                    .clickable(onClick = {})
                    .padding(vertical = 10.dp)
            ) {
                listOf(
                    "Google Información del libro",
                    "Wikipedia Información del libro",
                    "Facebook Información del libro",
                    "Twitter Información del libro",
                    "GoodReads Información del libro",
                    "Compartir",
                    "Enviar archivo",
                    "Limpiar “Estadísticas”",
                    "Calendario",
                    "Crear acceso directo en escritorio"
                ).forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReflowTextReaderPage(
    sourcePages: List<String>,
    pageIndex: Int,
    textSizePercent: Int,
    onPageCountChanged: (Int) -> Unit,
    onVisibleTextChanged: (String) -> Unit = {},
    onSelectedTextChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var selectionStart by remember { mutableStateOf<Int?>(null) }
    var selectionEnd by remember { mutableStateOf<Int?>(null) }
    BoxWithConstraints(
        modifier = modifier.background(Color(0xFFF8F1DE))
    ) {
        val horizontalPadding = 26.dp
        val verticalPadding = 30.dp
        val contentWidthPx = with(density) {
            (maxWidth - horizontalPadding * 2).roundToPx().coerceAtLeast(1)
        }
        val contentHeightPx = with(density) {
            (maxHeight - verticalPadding * 2).roundToPx().coerceAtLeast(1)
        }
        val fontSize = (18f * readerPdfZoomPercentToScale(textSizePercent)).sp
        val fontSizePx = with(density) { fontSize.toPx() }
        val lineHeightPx = fontSizePx * 1.42f
        val textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = Color(0xFF1A120B),
            fontSize = fontSize,
            lineHeight = (fontSize.value * 1.42f).sp
        )
        val logicalPages by produceState<List<String>>(
            initialValue = listOf(""),
            sourcePages,
            textSizePercent,
            contentWidthPx,
            contentHeightPx
        ) {
            value = withContext(Dispatchers.Default) {
                paginatePdfReflowText(
                    sourcePages = sourcePages,
                    fontSizePx = fontSizePx,
                    lineHeightPx = lineHeightPx,
                    maxWidthPx = contentWidthPx,
                    maxHeightPx = (contentHeightPx - with(density) { 14.dp.roundToPx() }).coerceAtLeast(1)
                )
            }
        }
        val safePageIndex = pageIndex.coerceIn(0, logicalPages.lastIndex.coerceAtLeast(0))

        LaunchedEffect(logicalPages.size) {
            onPageCountChanged(logicalPages.size.coerceAtLeast(1))
        }
        LaunchedEffect(safePageIndex, logicalPages) {
            onVisibleTextChanged(logicalPages.getOrElse(safePageIndex) { "" })
            selectionStart = null
            selectionEnd = null
            onSelectedTextChanged("")
        }
        val visibleText = logicalPages.getOrElse(safePageIndex) { "" }
        val selectedRange = remember(visibleText, selectionStart, selectionEnd) {
            normalizeSelectionRange(selectionStart, selectionEnd, visibleText.length)
        }
        val displayedText = remember(visibleText, selectedRange) {
            buildSelectableReaderText(visibleText, selectedRange)
        }

        // Cada pagina logica ya esta medida para caber completa en el viewport, sin scroll interno.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                .pointerInput(visibleText, textLayoutResult) {
                    detectTapGestures { offset ->
                        textLayoutResult?.let { layout ->
                            val selectedWord = wordRangeAt(visibleText, layout.getOffsetForPosition(offset))
                            selectionStart = selectedWord.first
                            selectionEnd = selectedWord.last + 1
                            onSelectedTextChanged(visibleText.substring(selectedWord.first, selectedWord.last + 1))
                        }
                    }
                }
                .pointerInput(visibleText, textLayoutResult) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            textLayoutResult?.let { layout ->
                                val selectedWord = wordRangeAt(visibleText, layout.getOffsetForPosition(offset))
                                selectionStart = selectedWord.first
                                selectionEnd = selectedWord.last + 1
                                onSelectedTextChanged(visibleText.substring(selectedWord.first, selectedWord.last + 1))
                            }
                        },
                        onDrag = { change, _ ->
                            textLayoutResult?.let { layout ->
                                val start = selectionStart ?: return@let
                                val end = layout.getOffsetForPosition(change.position)
                                val range = normalizeSelectionRange(start, end, visibleText.length) ?: return@let
                                selectionEnd = range.last + 1
                                onSelectedTextChanged(visibleText.substring(range.first, range.last + 1))
                            }
                        }
                    )
                }
        ) {
            Text(
                text = displayedText,
                style = textStyle,
                onTextLayout = { textLayoutResult = it },
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
fun PdfReaderPage(
    file: DeviceLibraryFile,
    pageIndex: Int,
    pdfZoomPercent: Int = READER_PDF_ZOOM_DEFAULT,
    pdfPanX: Float = 0f,
    pdfPanY: Float = 0f,
    coverText: String = "",
    overrideCoverId: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BoxWithConstraints(modifier = modifier) {
        val pdfZoom = readerPdfZoomPercentToScale(pdfZoomPercent)
        val targetWidth = with(LocalDensity.current) {
            (maxWidth.roundToPx() * pdfZoom).roundToInt()
        }.coerceAtLeast(360)
        val bitmap by produceState<Bitmap?>(initialValue = null, file.uri, pageIndex, targetWidth) {
            value = withContext(Dispatchers.IO) {
                renderPdfPage(context, file.uri, pageIndex, targetWidth)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (pageIndex == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1D1A06))
                        .padding(horizontal = 42.dp, vertical = 34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FilePagePreview(
                        file = file,
                        coverText = coverText,
                        overrideCoverId = overrideCoverId,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.68f)
                    )
                }
            } else if (bitmap == null) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(36.dp),
                    color = TarnishedGold
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(bitmap!!.width / bitmap!!.height.toFloat())
                        .graphicsLayer {
                            scaleX = pdfZoom
                            scaleY = pdfZoom
                            translationX = pdfPanX
                            translationY = pdfPanY
                            transformOrigin = TransformOrigin(0.5f, 0f)
                        }
                ) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                }
            }
        }
    }
}

@Composable
fun UnsupportedBookReader(file: DeviceLibraryFile) {
    ReaderMessage(
        title = stringResource(R.string.device_reader_unsupported_title),
        body = stringResource(R.string.device_reader_pdf_only_body, readableFileType(file))
    )
}

@Composable
fun ReaderMessage(
    title: String,
    body: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ShelfBackgroundBrush)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DeepWalnut.copy(alpha = 0.96f)),
            border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
}
