package com.example.kaishelvesapp.ui.screen.library

import android.content.Context
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.DeviceReaderAnnotation
import com.example.kaishelvesapp.data.model.DeviceReaderAnnotationType
import com.example.kaishelvesapp.data.repository.DeviceLibraryFile
import com.example.kaishelvesapp.data.repository.DeviceLibraryRepository
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import kotlin.math.abs
import kotlin.math.roundToInt
import org.json.JSONTokener
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

data class ReaderEngineDocument(
    val title: String,
    val subtitle: String,
    val pages: List<String>,
    val pageTitles: List<String?> = emptyList(),
    val pageKinds: List<ReaderSourcePageKind> = emptyList(),
    val epubPages: List<ReaderEpubPage> = emptyList(),
    val epubResources: Map<String, ByteArray> = emptyMap(),
    val chapters: List<ReaderChapter> = emptyList(),
    val imageLabels: List<String> = emptyList(),
    val coverPath: String? = null
)

data class ReaderEpubPage(
    val title: String,
    val href: String,
    val html: String
)

enum class ReaderSourcePageKind {
    Body,
    Cover,
    Synopsis,
    Chapter
}

data class ReaderChapter(
    val title: String,
    val sourceIndex: Int,
    val level: Int = 0,
    val groupTitle: String? = null
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
            val epubContent = extractEpubReaderContent(context, file.uri) ?: return@withContext null
            buildEpubEngineDocument(
                file = file,
                epubContent = epubContent,
                fallbackChapterTitle = { index -> context.getString(R.string.reader_fallback_chapter, index + 1) },
                synopsisTitle = context.getString(R.string.reader_synopsis)
            )
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

enum class PdfReaderScrollOrientation {
    Horizontal,
    Vertical
}

private fun buildEpubEngineDocument(
    file: DeviceLibraryFile,
    epubContent: EpubReaderContent,
    fallbackChapterTitle: (Int) -> String,
    synopsisTitle: String
): ReaderEngineDocument? {
    val sourcePages = mutableListOf<String>()
    val pageTitles = mutableListOf<String?>()
    val pageKinds = mutableListOf<ReaderSourcePageKind>()
    val readerChapters = mutableListOf<ReaderChapter>()
    val bookTitle = epubContent.metadata.title.takeIf { it.isNotBlank() } ?: file.name.substringBeforeLast('.')
    val bookAuthor = epubContent.metadata.author.takeIf { it.isNotBlank() }.orEmpty()
    val needsFallbackCover = epubContent.coverPath == null
    val epubWebPages = if (needsFallbackCover) {
        listOf(ReaderEpubPage(title = bookTitle, href = EPUB_FALLBACK_COVER_HREF, html = "")) +
            epubContent.webPages.map { page -> ReaderEpubPage(page.title, page.href, page.html) }
    } else {
        epubContent.webPages.map { page -> ReaderEpubPage(page.title, page.href, page.html) }
    }
    val webPageIndexOffset = if (needsFallbackCover) 1 else 0
    val webPageIndicesByHref = epubContent.webPages.mapIndexed { index, page -> page.href to (index + webPageIndexOffset) }.toMap()

    if (epubContent.coverPath != null) {
        sourcePages += listOf(bookTitle, bookAuthor).filter { it.isNotBlank() }.joinToString("\n").ifBlank { bookTitle }
        pageTitles += bookTitle
        pageKinds += ReaderSourcePageKind.Cover
    }
    if (epubContent.metadata.description.isNotBlank()) {
        sourcePages += epubContent.metadata.description
        pageTitles += synopsisTitle
        pageKinds += ReaderSourcePageKind.Synopsis
    }
    epubContent.chapters.forEachIndexed { index, chapter ->
        val title = chapter.title.takeIf { it.isNotBlank() } ?: fallbackChapterTitle(index)
        val body = chapter.text.trim().withoutLeadingTitle(title)
        if (body.isNotBlank()) {
            val sourceIndex = sourcePages.size
            sourcePages += body
            pageTitles += title
            pageKinds += ReaderSourcePageKind.Chapter
            readerChapters += ReaderChapter(
                title = title,
                sourceIndex = webPageIndicesByHref[chapter.href] ?: sourceIndex,
                level = chapter.level,
                groupTitle = chapter.parentTitle
            )
        }
    }
    val pages = sourcePages.filter { it.isNotBlank() }
    if (pages.isEmpty()) return null
    return ReaderEngineDocument(
        title = bookTitle,
        subtitle = bookAuthor.ifBlank { readableFileType(file) },
        pages = pages,
        pageTitles = pageTitles,
        pageKinds = pageKinds,
        epubPages = epubWebPages,
        epubResources = epubContent.resources,
        chapters = readerChapters,
        imageLabels = epubContent.imagePaths.map { path -> path.substringAfterLast('/') },
        coverPath = epubContent.coverPath
    )
}

private fun buildTextEngineDocument(
    file: DeviceLibraryFile,
    subtitle: String,
    metadata: DeviceBookDisplayMetadata?,
    text: String
): ReaderEngineDocument? {
    val cleanText = text
        .replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
        .replace(Regex(" *\\n *"), "\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
    if (cleanText.isBlank()) return null
    return ReaderEngineDocument(
        title = metadata?.title?.takeIf { it.isNotBlank() } ?: file.name.substringBeforeLast('.'),
        subtitle = metadata?.author?.takeIf { it.isNotBlank() } ?: subtitle,
        pages = listOf(cleanText),
        pageKinds = listOf(ReaderSourcePageKind.Body)
    )
}

private fun String.withoutLeadingTitle(title: String): String {
    val trimmedText = trimStart()
    val trimmedTitle = title.trim()
    if (trimmedTitle.isBlank()) return trimmedText
    return if (trimmedText.startsWith(trimmedTitle, ignoreCase = true)) {
        trimmedText.drop(trimmedTitle.length).trimStart('\n', ' ', '\t')
    } else {
        trimmedText
    }
}

@Composable
private fun EpubWebReaderPage(
    file: DeviceLibraryFile,
    page: ReaderEpubPage,
    resources: Map<String, ByteArray>,
    textSizePercent: Int,
    colorTheme: ReaderColorTheme,
    onVisibleTextChanged: (String) -> Unit,
    onSelectedTextChanged: (String) -> Unit,
    onHighlightSelection: (String, String) -> Unit,
    onNoteSelection: (String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onCenterTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (page.href == EPUB_FALLBACK_COVER_HREF) {
        Box(
            modifier = modifier
                .background(colorTheme.background.toReaderColor())
                .clickable(onClick = onCenterTap),
            contentAlignment = Alignment.Center
        ) {
            FilePagePreview(
                file = file,
                coverText = "",
                overrideCoverId = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(34.dp)
            )
        }
        return
    }

    var webSelectedText by remember(page.href) { mutableStateOf("") }
    var activeWebView by remember(page.href) { mutableStateOf<WebView?>(null) }
    Box(modifier = modifier.background(Color(0xFF202006))) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                object : WebView(viewContext) {
                    override fun startActionMode(callback: ActionMode.Callback?): ActionMode? {
                        return super.startActionMode(EpubSelectionActionModeCallback(callback))
                    }

                    override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
                        return super.startActionMode(EpubSelectionActionModeCallback(callback), type)
                    }
                }.apply {
                    activeWebView = this
                    setBackgroundColor(android.graphics.Color.rgb(32, 32, 6))
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    webViewClient = EpubResourceWebViewClient(resources)
                    var longPressTriggered = false
                    setOnLongClickListener {
                        longPressTriggered = true
                        listOf(260L, 620L, 980L).forEach { delayMillis ->
                            postDelayed({
                                captureEpubSelection(
                                    onSelected = { selected ->
                                        webSelectedText = selected
                                        onSelectedTextChanged(selected)
                                    }
                                )
                            }, delayMillis)
                        }
                        false
                    }
                    var downX = 0f
                    var downY = 0f
                    setOnTouchListener { view, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                downX = event.x
                                downY = event.y
                                longPressTriggered = false
                                false
                            }
                            MotionEvent.ACTION_UP -> {
                                if (longPressTriggered) {
                                    longPressTriggered = false
                                    return@setOnTouchListener true
                                }
                                val movedX = kotlin.math.abs(event.x - downX)
                                val movedY = kotlin.math.abs(event.y - downY)
                                if (movedX < 18f && movedY < 18f) {
                                    if (!longPressTriggered) {
                                        webSelectedText = ""
                                        evaluateJavascript("window.getSelection().removeAllRanges();", null)
                                    }
                                    val width = view.width.toFloat().coerceAtLeast(1f)
                                    when {
                                        event.x < width * 0.24f -> {
                                            onPreviousPage()
                                            true
                                        }
                                        event.x > width * 0.76f -> {
                                            onNextPage()
                                            true
                                        }
                                        else -> {
                                            onCenterTap()
                                            true
                                        }
                                    }
                                } else {
                                    false
                                }
                            }
                            else -> false
                        }
                    }
                }
            },
            update = { webView ->
                val fontScale = readerPdfZoomPercentToScale(textSizePercent)
                webView.settings.textZoom = (fontScale * 100).roundToInt().coerceIn(70, 220)
                val renderTag = "${page.href}|${colorTheme.background}|${colorTheme.text}"
                if (webView.tag != renderTag) {
                    webView.tag = renderTag
                    webSelectedText = ""
                    onVisibleTextChanged(page.title)
                    webView.loadDataWithBaseURL(
                        page.epubBaseUrl(),
                        page.html.withReaderColors(colorTheme).withoutExecutableScripts(),
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        )
        if (webSelectedText.isNotBlank()) {
            ReaderSelectionToolbar(
                selectedText = webSelectedText,
                onHighlightSelection = { _, color ->
                    activeWebView?.applyEpubSelectionStyle(color)
                    onHighlightSelection(webSelectedText, color)
                    webSelectedText = ""
                },
                onNoteSelection = {
                    onNoteSelection(webSelectedText)
                    webSelectedText = ""
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 92.dp),
                showDelete = false
            )
        }
    }
}

private class EpubResourceWebViewClient(
    private val resources: Map<String, ByteArray>
) : WebViewClient() {
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val uri = request?.url ?: return null
        if (uri.host != EPUB_WEB_HOST) return null
        val path = uri.path.orEmpty().trimStart('/')
        val bytes = resources[path] ?: return null
        return WebResourceResponse(
            path.epubMimeType(),
            if (path.epubMimeType().startsWith("text/") || path.endsWith(".xhtml")) "UTF-8" else null,
            ByteArrayInputStream(bytes)
        )
    }
}

private class EpubSelectionActionModeCallback(
    private val delegate: ActionMode.Callback?
) : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        delegate?.onCreateActionMode(mode, menu)
        menu.clear()
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        delegate?.onPrepareActionMode(mode, menu)
        menu.clear()
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return delegate?.onActionItemClicked(mode, item) ?: false
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        delegate?.onDestroyActionMode(mode)
    }
}

private const val EPUB_WEB_HOST = "kai-epub.local"
private const val EPUB_FALLBACK_COVER_HREF = "__kai_fallback_cover__"

private fun ReaderEpubPage.epubBaseUrl(): String {
    val directory = href.substringBeforeLast('/', missingDelimiterValue = "")
    return if (directory.isBlank()) {
        "https://$EPUB_WEB_HOST/"
    } else {
        "https://$EPUB_WEB_HOST/$directory/"
    }
}

private fun String.withReaderColors(theme: ReaderColorTheme): String {
    val colorCss = """
        <style type="text/css">
        html, body { background:${theme.background}; color:${theme.text}; }
        body, p, li, blockquote, div, span, h1, h2, h3, h4, h5, h6 { color:${theme.text}; }
        a { color:${theme.text}; }
        </style>
    """.trimIndent()
    return if (contains("</head>", ignoreCase = true)) {
        replace(Regex("(?i)</head>"), "$colorCss\n</head>")
    } else {
        "$colorCss\n$this"
    }
}

private fun WebView.applyEpubSelectionStyle(color: String) {
    val safeColor = color.substringAfter(":").takeIf { it.startsWith("#") } ?: "#EBC7E8"
    val command = when {
        color.startsWith("underline") || color.startsWith("diagonal") -> "document.execCommand('underline', false, null);"
        color.startsWith("strike") -> "document.execCommand('strikeThrough', false, null);"
        else -> "document.execCommand('hiliteColor', false, '$safeColor');"
    }
    evaluateJavascript(
        """
        (function(){
            try {
                document.designMode = 'on';
                $command
                document.designMode = 'off';
            } catch(e) {
                document.designMode = 'off';
            }
        })();
        """.trimIndent(),
        null
    )
}

private fun WebView.captureEpubSelection(onSelected: (String) -> Unit) {
    evaluateJavascript("(function(){return window.getSelection().toString();})()") { encoded ->
        val selected = encoded.decodeJavascriptString().trim()
        if (selected.isNotBlank()) {
            onSelected(selected)
        }
    }
}

private fun String.withoutExecutableScripts(): String {
    return replace(Regex("(?is)<script\\b.*?</script>"), "")
}

private fun String.decodeJavascriptString(): String {
    return runCatching { JSONTokener(this).nextValue() as? String }
        .getOrNull()
        .orEmpty()
}

private fun String.toReaderColor(): Color {
    return runCatching { Color(android.graphics.Color.parseColor(this)) }
        .getOrDefault(Color(0xFF202006))
}

private fun String.epubMimeType(): String {
    return when (substringAfterLast('.', "").lowercase()) {
        "css" -> "text/css"
        "xhtml", "html", "htm" -> "application/xhtml+xml"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "svg" -> "image/svg+xml"
        "webp" -> "image/webp"
        else -> "application/octet-stream"
    }
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
            var showDisplaySettings by remember(file.uri) { mutableStateOf(false) }
            var showTextSizeSettings by remember(file.uri) { mutableStateOf(false) }
            var showThemeSettings by remember(file.uri) { mutableStateOf(false) }
            var showAnnotationsDialog by remember(file.uri) { mutableStateOf(false) }
            var showNoteDialog by remember(file.uri) { mutableStateOf(false) }
            var editingAnnotation by remember(file.uri) { mutableStateOf<DeviceReaderAnnotation?>(null) }
            var brightnessPercent by remember { mutableStateOf(readReaderBrightnessPercent(context)) }
            var readerColorTheme by remember { mutableStateOf(readReaderColorTheme(context)) }
            var autoBrightness by remember { mutableStateOf(readReaderAutoBrightness(context)) }
            var blueLightFilterEnabled by remember { mutableStateOf(readReaderBlueLightFilterEnabled(context)) }
            var blueLightOpacity by remember { mutableStateOf(readReaderBlueLightOpacity(context)) }
            var textSizePercent by remember { mutableStateOf(readReaderPdfZoomPercent(context)) }
            var zoomPercentBeforeChange by remember(file.uri) { mutableStateOf<Int?>(null) }
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
            var visiblePageText by remember(file.uri) { mutableStateOf("") }
            var selectedText by remember(file.uri) { mutableStateOf("") }
            var annotations by remember(file.uri) { mutableStateOf(annotationRepository.getAnnotations(file)) }
            var sourcePageStartPages by remember(file.uri) { mutableStateOf(listOf(0)) }
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val progress = readingProgressForPage(currentPage, logicalPageCount)
            ApplyReaderBrightness(
                brightnessPercent = brightnessPercent,
                autoBrightness = autoBrightness
            )

            LaunchedEffect(file.uri, currentPage, logicalPageCount) {
                saveDeviceBookReadingProgress(context, file, currentPage, logicalPageCount)
                onProgressChanged()
            }
            LaunchedEffect(logicalPageCount) {
                currentPage = currentPage.coerceIn(0, logicalPageCount.coerceAtLeast(1) - 1)
            }
            LaunchedEffect(engineKind, loadedDocument.epubPages.size) {
                if (engineKind == ReaderEngineKind.Epub && loadedDocument.epubPages.isNotEmpty()) {
                    logicalPageCount = loadedDocument.epubPages.size
                    sourcePageStartPages = loadedDocument.epubPages.indices.toList().ifEmpty { listOf(0) }
                }
            }
            LaunchedEffect(textSizePercent) {
                saveReaderTextSizePercent(context, textSizePercent)
            }
            LaunchedEffect(
                brightnessPercent,
                autoBrightness,
                blueLightFilterEnabled,
                blueLightOpacity,
                textSizePercent,
                readerColorTheme,
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
                    pdfZoomPercent = textSizePercent,
                    brightnessEdge = selectedBrightnessEdge,
                    resumeAutoBrightness = resumeAutoBrightnessAfterInactivity,
                    resumeAutoBrightnessMinutes = resumeAutoBrightnessMinutes
                )
                saveReaderColorTheme(context, readerColorTheme)
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

            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = drawerState.isOpen,
                drawerContent = {
                    ReflowReaderNavigationDrawer(
                        file = file,
                        document = loadedDocument,
                        currentPage = currentPage,
                        pageCount = logicalPageCount,
                        sourcePageStartPages = sourcePageStartPages,
                        onPageSelected = { page ->
                            currentPage = page.coerceIn(0, logicalPageCount - 1)
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
                ) {
                    if (engineKind == ReaderEngineKind.Epub && loadedDocument.epubPages.isNotEmpty()) {
                        EpubWebReaderPage(
                            file = file,
                            page = loadedDocument.epubPages[currentPage.coerceIn(0, loadedDocument.epubPages.lastIndex)],
                            resources = loadedDocument.epubResources,
                            textSizePercent = textSizePercent,
                            colorTheme = readerColorTheme,
                            onVisibleTextChanged = { visiblePageText = it },
                            onSelectedTextChanged = { selectedText = it },
                            onHighlightSelection = { selected, highlightColor ->
                                annotationRepository.addAnnotation(
                                    file = file,
                                    annotation = DeviceReaderAnnotation(
                                        type = DeviceReaderAnnotationType.Highlight,
                                        page = currentPage,
                                        pageCount = logicalPageCount,
                                        selectedText = selected.take(READER_ANNOTATION_TEXT_LIMIT),
                                        color = highlightColor
                                    )
                                )
                                annotationRepository.addAnnotation(
                                    file = file,
                                    annotation = DeviceReaderAnnotation(
                                        type = DeviceReaderAnnotationType.Bookmark,
                                        page = currentPage,
                                        pageCount = logicalPageCount,
                                        selectedText = selected.take(READER_ANNOTATION_TEXT_LIMIT),
                                        note = selected.take(READER_ANNOTATION_TEXT_LIMIT)
                                    )
                                )
                                annotations = annotationRepository.getAnnotations(file)
                            },
                            onNoteSelection = { selected ->
                                selectedText = selected.take(READER_ANNOTATION_TEXT_LIMIT)
                                showNoteDialog = true
                            },
                            onPreviousPage = { currentPage = (currentPage - 1).coerceAtLeast(0) },
                            onNextPage = { currentPage = (currentPage + 1).coerceAtMost(logicalPageCount - 1) },
                            onCenterTap = { controlsVisible = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        ReflowTextReaderPage(
                            file = file,
                            sourcePages = loadedDocument.pages,
                            sourcePageTitles = loadedDocument.pageTitles,
                            sourcePageKinds = loadedDocument.pageKinds,
                            pageIndex = currentPage,
                            textSizePercent = textSizePercent,
                            onPageCountChanged = { count -> logicalPageCount = count.coerceAtLeast(1) },
                            onSourcePageStartPagesChanged = { starts -> sourcePageStartPages = starts.ifEmpty { listOf(0) } },
                            onVisibleTextChanged = { visiblePageText = it },
                            onSelectedTextChanged = { selectedText = it },
                            onPageChanged = { page -> currentPage = page.coerceIn(0, logicalPageCount - 1) },
                            onPreviousPage = { currentPage = (currentPage - 1).coerceAtLeast(0) },
                            onNextPage = { currentPage = (currentPage + 1).coerceAtMost(logicalPageCount - 1) },
                            onCenterTap = { controlsVisible = true },
                            highlights = annotations.filter { it.type == DeviceReaderAnnotationType.Highlight },
                            onHighlightSelection = { selection, highlightColor ->
                                annotationRepository.addAnnotation(
                                    file = file,
                                    annotation = DeviceReaderAnnotation(
                                        type = DeviceReaderAnnotationType.Highlight,
                                        page = currentPage,
                                        pageCount = logicalPageCount,
                                        sourcePage = selection.sourcePage,
                                        selectionStart = selection.start,
                                        selectionEnd = selection.end,
                                        selectedText = selection.text.take(READER_ANNOTATION_TEXT_LIMIT),
                                        color = highlightColor
                                    )
                                )
                                annotations = annotationRepository.getAnnotations(file)
                            },
                            onHighlightUpdate = { annotation, highlightColor ->
                                annotationRepository.updateAnnotation(
                                    file = file,
                                    annotation = annotation.copy(color = highlightColor)
                                )
                                annotations = annotationRepository.getAnnotations(file)
                            },
                            onHighlightDelete = { annotation ->
                                annotationRepository.deleteAnnotation(file, annotation.id)
                                annotations = annotationRepository.getAnnotations(file)
                            },
                            onNoteSelection = { noteText ->
                                selectedText = noteText.take(READER_ANNOTATION_TEXT_LIMIT)
                                showNoteDialog = true
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

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
                                    showThemeSettings = false
                                }
                        )
                        PdfReaderTopControls(
                            title = loadedDocument.title,
                            onDismiss = onDismiss,
                            onTitleClick = {},
                            onOpenThemeSettings = {
                                showThemeSettings = true
                                showDisplaySettings = false
                                showTextSizeSettings = false
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .zIndex(2f)
                        )
                        if (!showDisplaySettings && !showTextSizeSettings && !showThemeSettings) {
                            PdfReaderBottomControls(
                                currentPage = currentPage,
                                pageCount = logicalPageCount,
                                progress = progress,
                                showReset = false,
                                onPageSelected = { page -> currentPage = page.coerceIn(0, logicalPageCount - 1) },
                                onResetPage = {},
                                onOpenNavigation = { scope.launch { drawerState.open() } },
                                onOpenDisplaySettings = {
                                    showDisplaySettings = true
                                    showTextSizeSettings = false
                                    showThemeSettings = false
                                },
                                annotationsCount = annotations.size,
                                onOpenTextSizeSettings = {
                                    showTextSizeSettings = true
                                    showDisplaySettings = false
                                    showThemeSettings = false
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

                    if (showThemeSettings) {
                        ReaderThemePanel(
                            selectedTheme = readerColorTheme,
                            onThemeSelected = { readerColorTheme = it },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .zIndex(3f)
                        )
                    }

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
                                if (engineKind == ReaderEngineKind.Epub) {
                                    annotationRepository.addAnnotation(
                                        file = file,
                                        annotation = DeviceReaderAnnotation(
                                            type = DeviceReaderAnnotationType.Bookmark,
                                            page = currentPage,
                                            pageCount = logicalPageCount,
                                            selectedText = selectedText.ifBlank { visiblePageText }.take(READER_ANNOTATION_TEXT_LIMIT),
                                            note = note
                                        )
                                    )
                                }
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
            var pdfScrollOrientation by remember(file.uri) { mutableStateOf(PdfReaderScrollOrientation.Horizontal) }
            var pdfMovementLocked by remember(file.uri) { mutableStateOf(false) }
            var pdfFloatingControlsVisible by remember(file.uri) { mutableStateOf(false) }
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
            LaunchedEffect(pdfFloatingControlsVisible, controlsVisible) {
                if (pdfFloatingControlsVisible && !controlsVisible) {
                    delay(1800)
                    pdfFloatingControlsVisible = false
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
                    if (!pdfMovementLocked) {
                        pdfPanX += panChange.x
                        pdfPanY += panChange.y
                        if (panChange.x != 0f || panChange.y != 0f) {
                            pdfFloatingControlsVisible = true
                        }
                    }
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
                            enabled = !controlsVisible &&
                                !(pdfScrollOrientation == PdfReaderScrollOrientation.Vertical && pdfMovementLocked)
                        )
                ) {
                    if (pdfScrollOrientation == PdfReaderScrollOrientation.Vertical && pdfMovementLocked) {
                        PdfReaderVerticalPages(
                            file = file,
                            pageCount = activePageCount,
                            currentPage = currentPage,
                            pdfZoomPercent = readerZoomPercent,
                            coverText = userMetadata.coverText,
                            overrideCoverId = userMetadata.coverId.takeIf { it.isNotBlank() },
                            onPageChanged = { page -> currentPage = page.coerceIn(0, activePageCount - 1) },
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(pdfScrollOrientation, pdfMovementLocked) {
                                    detectTapGestures { offset ->
                                        val width = size.width.toFloat().coerceAtLeast(1f)
                                        if (offset.x in (width * 0.33f)..(width * 0.67f)) {
                                            controlsVisible = true
                                        }
                                    }
                                }
                        )
                    } else {
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
                    }

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
                    } else if (pdfScrollOrientation == PdfReaderScrollOrientation.Horizontal) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(pdfScrollOrientation, activePageCount) {
                                    var draggedX = 0f
                                    var draggedY = 0f
                                    detectDragGestures(
                                        onDragStart = {
                                            draggedX = 0f
                                            draggedY = 0f
                                        },
                                        onDrag = { _, dragAmount ->
                                            draggedX += dragAmount.x
                                            draggedY += dragAmount.y
                                        },
                                        onDragEnd = {
                                            val horizontalGesture = abs(draggedX) > abs(draggedY) * 1.1f
                                            val verticalGesture = abs(draggedY) > abs(draggedX) * 1.1f
                                            val threshold = 72f
                                            when {
                                                pdfScrollOrientation == PdfReaderScrollOrientation.Horizontal &&
                                                    horizontalGesture &&
                                                    abs(draggedX) > threshold -> {
                                                    currentPage = if (draggedX < 0) {
                                                        (currentPage + 1).coerceAtMost(activePageCount - 1)
                                                    } else {
                                                        (currentPage - 1).coerceAtLeast(0)
                                                    }
                                                }
                                                pdfScrollOrientation == PdfReaderScrollOrientation.Vertical &&
                                                    verticalGesture &&
                                                    abs(draggedY) > threshold -> {
                                                    currentPage = if (draggedY < 0) {
                                                        (currentPage + 1).coerceAtMost(activePageCount - 1)
                                                    } else {
                                                        (currentPage - 1).coerceAtLeast(0)
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                        ) {
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

                    if ((controlsVisible || pdfFloatingControlsVisible) && !showDisplaySettings && !showTextSizeSettings) {
                        PdfReaderFloatingControls(
                            scrollOrientation = pdfScrollOrientation,
                            movementLocked = pdfMovementLocked,
                            onToggleScrollOrientation = {
                                pdfScrollOrientation = when (pdfScrollOrientation) {
                                    PdfReaderScrollOrientation.Horizontal -> PdfReaderScrollOrientation.Vertical
                                    PdfReaderScrollOrientation.Vertical -> PdfReaderScrollOrientation.Horizontal
                                }
                            },
                            onToggleMovementLock = { pdfMovementLocked = !pdfMovementLocked },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(
                                    end = 14.dp,
                                    bottom = if (controlsVisible) 204.dp else 48.dp
                                )
                                .zIndex(2f)
                        )
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



