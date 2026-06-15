package com.example.kaishelvesapp.ui.screen.library

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.ActionMode
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.webkit.JavascriptInterface
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.zIndex
import androidx.core.graphics.toColorInt
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
import org.json.JSONObject
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
    val imagePaths: List<String> = emptyList(),
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
            buildTextEngineDocument(file, readableFileType(file), text)
        }
        ReaderEngineKind.Fb2 -> {
            // FB2 se trata como adaptador textual ligero; si falla, queda listo para conversor EPUB futuro.
            val text = readPlainTextFile(context, file.uri)?.let(::stripXmlToText).orEmpty()
            buildTextEngineDocument(file, "FB2", text)
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
    val introPages = buildEpubIntroPages(
        title = bookTitle,
        author = bookAuthor,
        description = epubContent.metadata.description,
        coverPath = epubContent.coverPath,
        synopsisTitle = synopsisTitle
    )
    val chapterTextByHref = epubContent.chapters.associate { it.href to it.text }
    val epubWebPages = introPages + epubContent.webPages.map { page ->
        val fallbackText = chapterTextByHref[page.href].orEmpty()
        ReaderEpubPage(page.title, page.href, page.html.withReadableFallback(fallbackText))
    }
    val webPageIndexOffset = introPages.size
    val webPageIndicesByHref = epubContent.webPages
        .mapIndexed { index, page -> page.href to (index + webPageIndexOffset) }
        .toMap()

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
        imagePaths = epubContent.imagePaths,
        coverPath = epubContent.coverPath
    )
}

private fun buildEpubIntroPages(
    title: String,
    author: String,
    description: String,
    coverPath: String?,
    synopsisTitle: String
): List<ReaderEpubPage> {
    val pages = mutableListOf<ReaderEpubPage>()
    val coverHtml = buildString {
        append("<section class=\"kai-reader-cover\">")
        coverPath?.let { append("<img class=\"kai-cover-image\" src=\"${it.escapeHtmlAttribute()}\" alt=\"${title.escapeHtmlAttribute()}\" />") }
        append("<h1>${title.escapeHtml()}</h1>")
        if (author.isNotBlank()) append("<p class=\"kai-author\">${author.escapeHtml()}</p>")
        append("</section>")
    }
    pages += ReaderEpubPage(title = title, href = EPUB_SYNTHETIC_COVER_HREF, html = coverHtml)

    if (description.isNotBlank()) {
        pages += ReaderEpubPage(
            title = synopsisTitle,
            href = EPUB_SYNTHETIC_SYNOPSIS_HREF,
            html = "<section class=\"kai-reader-synopsis\"><h2>${synopsisTitle.escapeHtml()}</h2><p>${description.escapeHtml()}</p></section>"
        )
    }

    return pages
}

private fun buildTextEngineDocument(
    file: DeviceLibraryFile,
    subtitle: String,
    // Sin uso actual: antes llegaba siempre null y generaba aviso.
    // metadata: DeviceBookDisplayMetadata?,
    text: String
): ReaderEngineDocument? {
    val cleanText = text
        .replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
        .replace(Regex(" *\\n *"), "\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
    if (cleanText.isBlank()) return null
    return ReaderEngineDocument(
        title = file.name.substringBeforeLast('.'),
        subtitle = subtitle,
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
@Suppress("UNUSED_VALUE")
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
private fun EpubWebReaderPage(
    file: DeviceLibraryFile,
    pages: List<ReaderEpubPage>,
    resources: Map<String, ByteArray>,
    annotations: List<DeviceReaderAnnotation>,
    pageIndex: Int,
    textSizePercent: Int,
    colorTheme: ReaderColorTheme,
    onPageCountChanged: (Int) -> Unit,
    onChapterPageStartsChanged: (List<Int>) -> Unit,
    onPageChanged: (Int) -> Unit,
    onVisibleTextChanged: (String) -> Unit,
    onSelectedTextChanged: (String) -> Unit,
    onHighlightSelection: (String, String, String?) -> String,
    onHighlightUpdate: (DeviceReaderAnnotation, String) -> Unit,
    onHighlightDelete: (DeviceReaderAnnotation) -> Unit,
    onNoteSelection: (String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onCenterTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webSelectedText by remember(file.uri) { mutableStateOf("") }
    var selectedRect by remember(file.uri) { mutableStateOf<Rect?>(null) }
    var activeHighlight by remember(file.uri) { mutableStateOf<DeviceReaderAnnotation?>(null) }
    var activeWebView by remember(file.uri) { mutableStateOf<WebView?>(null) }
    var allowNativeSelection by remember(file.uri) { mutableStateOf(false) }
    var selectionLongPressConfirmed by remember(file.uri) { mutableStateOf(false) }
    var highlightTouchInProgress by remember(file.uri) { mutableStateOf(false) }
    val density = LocalDensity.current
    val latestAnnotations by rememberUpdatedState(annotations)
    val epubHighlightsJson = remember(annotations) {
        annotations.filter { it.type == DeviceReaderAnnotationType.Highlight }.toEpubHighlightsJson()
    }
    val bridge = remember(file.uri) {
        EpubJsBridge(
            onTextSelected = { text, _, rect ->
                val safeText = text.take(READER_ANNOTATION_TEXT_LIMIT)
                webSelectedText = safeText
                selectedRect = rect
                activeHighlight = null
                onSelectedTextChanged(safeText)
            },
            onHighlightSelected = { annotationId, text, rect ->
                val safeText = text.take(READER_ANNOTATION_TEXT_LIMIT)
                val normalizedText = safeText.normalizeReaderSelectionText()

                activeHighlight = latestAnnotations.firstOrNull { it.id == annotationId }
                    ?: latestAnnotations.firstOrNull { annotation ->
                        annotation.type == DeviceReaderAnnotationType.Highlight &&
                                annotation.selectedText.normalizeReaderSelectionText() == normalizedText
                    }
                            ?: latestAnnotations.firstOrNull { annotation ->
                        annotation.type == DeviceReaderAnnotationType.Highlight &&
                                normalizedText.contains(annotation.selectedText.normalizeReaderSelectionText())
                    }
                            ?: latestAnnotations.firstOrNull { annotation ->
                        annotation.type == DeviceReaderAnnotationType.Highlight &&
                                annotation.selectedText.normalizeReaderSelectionText().contains(normalizedText)
                    }

                webSelectedText = safeText
                selectedRect = rect
                onSelectedTextChanged(webSelectedText)
            },
            onPageCountReady = { pageCount, chapterStarts ->
                onPageCountChanged(pageCount.coerceAtLeast(1))
                onChapterPageStartsChanged(chapterStarts.ifEmpty { listOf(0) })
            },
            onPageChanged = onPageChanged
        )
    }

    BoxWithConstraints(modifier = modifier.background(colorTheme.background.toReaderColor())) {
        // Sincroniza cambios externos del drawer sin recargar el WebView ni mover la página actual.
        LaunchedEffect(activeWebView, epubHighlightsJson) {
            activeWebView?.syncEpubHighlights(epubHighlightsJson)
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                object : WebView(viewContext) {
                    override fun performClick(): Boolean {
                        super.performClick()
                        return true
                    }
                    private fun shouldAllowSelectionActionMode(): Boolean {
                        return allowNativeSelection && selectionLongPressConfirmed
                    }

                    private fun onNativeSelectionCreated() {
                        postDelayed({
                            readCurrentWebSelection(
                                onSelection = { text, rect ->
                                    val safeText = text.take(READER_ANNOTATION_TEXT_LIMIT)

                                    if (safeText.isBlank()) return@readCurrentWebSelection

                                    webSelectedText = safeText
                                    selectedRect = rect
                                    activeHighlight = null
                                    onSelectedTextChanged(safeText)

                                    evaluateJavascript(
                                        "window.kaiFinishSelection && window.kaiFinishSelection();",
                                        null
                                    )
                                },
                                fallback = {
                                    evaluateJavascript(
                                        "window.kaiCancelSelection && window.kaiCancelSelection();",
                                        null
                                    )
                                }
                            )
                        }, 80L)
                    }

                    override fun startActionMode(callback: ActionMode.Callback?): ActionMode? {
                        if (!shouldAllowSelectionActionMode()) {
                            post {
                                evaluateJavascript(
                                    "window.kaiCancelSelection && window.kaiCancelSelection();",
                                    null
                                )
                            }
                            return null
                        }

                        onNativeSelectionCreated()

                        return super.startActionMode(
                            EpubSelectionActionModeCallback(callback) {
                                onNativeSelectionCreated()
                            }
                        )
                    }

                    override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
                        if (!shouldAllowSelectionActionMode()) {
                            post {
                                evaluateJavascript(
                                    "window.kaiCancelSelection && window.kaiCancelSelection();",
                                    null
                                )
                            }
                            return null
                        }

                        onNativeSelectionCreated()

                        return super.startActionMode(
                            EpubSelectionActionModeCallback(callback) {
                                onNativeSelectionCreated()
                            },
                            type
                        )
                    }
                }.apply {
                    activeWebView = this
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    isHorizontalScrollBarEnabled = false
                    isVerticalScrollBarEnabled = false
                    overScrollMode = WebView.OVER_SCROLL_NEVER
                    webViewClient = EpubResourceWebViewClient(resources)
                    addJavascriptInterface(bridge, EPUB_JS_BRIDGE_NAME)
                    var selectionGestureActive = false
                    var downX = 0f
                    var downY = 0f
                    var downAt = 0L
                    var movedTooMuchForSelection = false

                    val gestureDetector = GestureDetector(
                        viewContext,
                        object : GestureDetector.SimpleOnGestureListener() {
                            override fun onLongPress(event: MotionEvent) {
                                if (movedTooMuchForSelection || highlightTouchInProgress) return

                                selectionGestureActive = true
                                selectionLongPressConfirmed = true
                                allowNativeSelection = true

                                evaluateJavascript("window.kaiEnableSelection && window.kaiEnableSelection();", null)
                            }

                            override fun onSingleTapUp(event: MotionEvent): Boolean {
                                return true
                            }

                            override fun onFling(
                                e1: MotionEvent?,
                                e2: MotionEvent,
                                velocityX: Float,
                                velocityY: Float
                            ): Boolean {
                                if (selectionGestureActive || selectionLongPressConfirmed) return true

                                val isHorizontalPageGesture = abs(velocityX) > abs(velocityY) && abs(velocityX) >= 420f
                                val isVerticalPageGesture = abs(velocityY) > abs(velocityX) && abs(velocityY) >= 420f
                                when {
                                    isHorizontalPageGesture -> {
                                        if (velocityX < 0f) onNextPage() else onPreviousPage()
                                        return true
                                    }
                                    isVerticalPageGesture -> {
                                        if (velocityY < 0f) onNextPage() else onPreviousPage()
                                        return true
                                    }
                                    else -> return false
                                }
                            }
                        }
                    )
                    setOnTouchListener { _, event ->
                        val handledByGestureDetector = gestureDetector.onTouchEvent(event)

                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                downX = event.x
                                downY = event.y
                                downAt = event.eventTime

                                movedTooMuchForSelection = false
                                selectionGestureActive = false
                                selectionLongPressConfirmed = false
                                highlightTouchInProgress = false

                                isPointInsideEpubHighlight(event.x, event.y) { isHighlight ->
                                    highlightTouchInProgress = isHighlight
                                }
                            }

                            MotionEvent.ACTION_MOVE -> {
                                val dragX = event.x - downX
                                val dragY = event.y - downY

                                if (!selectionLongPressConfirmed && (abs(dragX) > 42f || abs(dragY) > 42f)) {
                                    movedTooMuchForSelection = true
                                }
                            }

                            MotionEvent.ACTION_UP -> {
                                val dragX = event.x - downX
                                val dragY = event.y - downY
                                val duration = event.eventTime - downAt

                                if (highlightTouchInProgress) {
                                    evaluateJavascript("window.kaiTapAt && window.kaiTapAt(${event.x}, ${event.y});", null)
                                    highlightTouchInProgress = false
                                    selectionGestureActive = false
                                    selectionLongPressConfirmed = false
                                    return@setOnTouchListener true
                                }

                                if (selectionGestureActive && selectionLongPressConfirmed) {
                                    postDelayed({
                                        readCurrentWebSelection(
                                            onSelection = { text, rect ->
                                                val safeText = text.take(READER_ANNOTATION_TEXT_LIMIT)

                                                if (safeText.isBlank()) {
                                                    evaluateJavascript(
                                                        "window.kaiCancelSelection && window.kaiCancelSelection();",
                                                        null
                                                    )
                                                    return@readCurrentWebSelection
                                                }

                                                webSelectedText = safeText
                                                selectedRect = rect
                                                activeHighlight = null
                                                onSelectedTextChanged(safeText)

                                                evaluateJavascript(
                                                    "window.kaiFinishSelection && window.kaiFinishSelection();",
                                                    null
                                                )
                                            },
                                            fallback = {
                                                evaluateJavascript(
                                                    "window.kaiFinishSelection && window.kaiFinishSelection();",
                                                    null
                                                )
                                            }
                                        )
                                    }, 120L)

                                    selectionGestureActive = false

                                    return@setOnTouchListener true
                                }

                                val tapGesture = duration < 320L && abs(dragX) < 18f && abs(dragY) < 18f

                                if (tapGesture) {
                                    performClick()
                                    allowNativeSelection = false
                                    selectionLongPressConfirmed = false
                                    selectionGestureActive = false

                                    var tapResolved = false
                                    fun handleReaderTapFallback() {
                                        if (tapResolved) return

                                        tapResolved = true
                                        webSelectedText = ""
                                        selectedRect = null
                                        activeHighlight = null

                                        val width = this.width.toFloat().coerceAtLeast(1f)
                                        when {
                                            event.x < width * 0.26f -> onPreviousPage()
                                            event.x > width * 0.74f -> onNextPage()
                                            else -> onCenterTap()
                                        }
                                    }

                                    evaluateJavascript(
                                        """
                                        (function() {
                                            if (window.kaiTapAt && window.kaiTapAt(${event.x}, ${event.y})) return true;
                                            if (window.kaiLastHighlightTapAt && Date.now() - window.kaiLastHighlightTapAt < 450) return true;
                                            window.kaiCancelSelection && window.kaiCancelSelection();
                                            return false;
                                        })();
                                        """.trimIndent()
                                    ) { handledTap ->
                                        if (tapResolved) {
                                            return@evaluateJavascript
                                        }

                                        tapResolved = true

                                        if (handledTap == "true") {
                                            return@evaluateJavascript
                                        }

                                        webSelectedText = ""
                                        selectedRect = null
                                        activeHighlight = null

                                        val width = this.width.toFloat().coerceAtLeast(1f)
                                        when {
                                            event.x < width * 0.26f -> onPreviousPage()
                                            event.x > width * 0.74f -> onNextPage()
                                            else -> onCenterTap()
                                        }
                                    }
                                    postDelayed({ handleReaderTapFallback() }, 180L)
                                    return@setOnTouchListener true
                                }

                                allowNativeSelection = false
                                selectionLongPressConfirmed = false
                                selectionGestureActive = false

                                if (webSelectedText.isBlank()) {
                                    evaluateJavascript("window.kaiCancelSelection && window.kaiCancelSelection();", null)
                                }

                                val quickPageDrag = duration < 650L && maxOf(abs(dragX), abs(dragY)) > 104f
                                if (!handledByGestureDetector && webSelectedText.isBlank() && quickPageDrag) {
                                    if (abs(dragY) >= abs(dragX)) {
                                        if (dragY < 0f) onNextPage() else onPreviousPage()
                                    } else {
                                        if (dragX < 0f) onNextPage() else onPreviousPage()
                                    }
                                    return@setOnTouchListener true
                                }
                                postDelayed({ snapEpubWebViewToNearestPage(this, onPageChanged) }, 120L)
                            }
                            MotionEvent.ACTION_CANCEL -> {
                                if (webSelectedText.isBlank()) {
                                    evaluateJavascript("window.kaiCancelSelection && window.kaiCancelSelection();", null)
                                }

                                allowNativeSelection = false
                                selectionGestureActive = false
                                selectionLongPressConfirmed = false
                                highlightTouchInProgress = false
                                movedTooMuchForSelection = false
                                postDelayed({ snapEpubWebViewToNearestPage(this, onPageChanged) }, 120L)
                            }
                        }

                        handledByGestureDetector
                    }
                }
            },
            update = { webView ->
                val fontScale = readerPdfZoomPercentToScale(textSizePercent)
                val renderTag = "${file.uri}|${pages.size}|${colorTheme.background}|${colorTheme.text}|$textSizePercent"
                if (webView.tag != renderTag) {
                    webView.tag = renderTag
                    webSelectedText = ""
                    selectedRect = null
                    activeHighlight = null
                    onVisibleTextChanged(pages.firstOrNull()?.title.orEmpty())
                    webView.loadDataWithBaseURL(
                        "https://$EPUB_WEB_HOST/",
                        buildEpubReaderHtml(
                            pages = pages,
                            annotations = annotations.filter { it.type == DeviceReaderAnnotationType.Highlight },
                            resources = resources,
                            theme = colorTheme,
                            fontScale = fontScale
                        ),
                        "text/html",
                        "UTF-8",
                        null
                    )
                    webView.postDelayed({ webView.goToEpubPage(pageIndex) }, 360L)
                } else if (webSelectedText.isBlank() && !allowNativeSelection && !selectionLongPressConfirmed) {
                    webView.goToEpubPage(pageIndex)
                }
            }
        )

        selectedRect?.takeIf { webSelectedText.isNotBlank() }?.let { rect ->
            val toolbarWidthPx = with(density) { 338.dp.roundToPx() }
            val toolbarHeightPx = with(density) { 122.dp.roundToPx() }
            val gapPx = with(density) { 4.dp.roundToPx() }
            val sideMarginPx = with(density) { 6.dp.roundToPx() }
            val normalizedSelectedText = webSelectedText.normalizeReaderSelectionText()

            val possibleHighlightToDelete = activeHighlight
                ?: latestAnnotations.firstOrNull { annotation ->
                    annotation.type == DeviceReaderAnnotationType.Highlight &&
                            annotation.selectedText.normalizeReaderSelectionText() == normalizedSelectedText
                }
                ?: latestAnnotations.firstOrNull { annotation ->
                    annotation.type == DeviceReaderAnnotationType.Highlight &&
                            normalizedSelectedText.contains(annotation.selectedText.normalizeReaderSelectionText())
                }
                ?: latestAnnotations.firstOrNull { annotation ->
                    annotation.type == DeviceReaderAnnotationType.Highlight &&
                            annotation.selectedText.normalizeReaderSelectionText().contains(normalizedSelectedText)
                }

            val canDeleteHighlight = possibleHighlightToDelete != null

            val maxX = (constraints.maxWidth - toolbarWidthPx - sideMarginPx)
                .coerceAtLeast(sideMarginPx)

            val maxY = (constraints.maxHeight - toolbarHeightPx - sideMarginPx)
                .coerceAtLeast(sideMarginPx)

            val popupX = (rect.centerX() - toolbarWidthPx / 2)
                .coerceIn(sideMarginPx, maxX)

            val aboveY = rect.top - toolbarHeightPx - gapPx
            val belowY = rect.bottom + gapPx

            val popupY = when {
                aboveY >= sideMarginPx -> aboveY
                belowY + toolbarHeightPx <= constraints.maxHeight - sideMarginPx -> belowY
                else -> rect.top - toolbarHeightPx / 2
            }.coerceIn(sideMarginPx, maxY)

            Popup(
                offset = IntOffset(
                    x = popupX,
                    y = popupY
                ),
                onDismissRequest = {
                    activeWebView?.cancelEpubSelection()
                    webSelectedText = ""
                    selectedRect = null
                    activeHighlight = null
                }
            ) {
                ReaderSelectionToolbar(
                    selectedText = webSelectedText,
                    showDelete = canDeleteHighlight,
                    onHighlightSelection = { _, color ->
                        activeHighlight?.let { highlight ->
                            onHighlightUpdate(highlight, color)
                            activeWebView?.updateEpubHighlightStyle(highlight.id, color)

                            webSelectedText = ""
                            selectedRect = null
                            activeHighlight = null
                            activeWebView?.cancelEpubSelection()
                        } ?: run {
                            val text = webSelectedText

                            if (text.isNotBlank()) {
                                activeWebView?.createEpubHighlight(color) { tempId ->
                                    if (tempId.isNotBlank()) {
                                        val storedId = onHighlightSelection(text, color, tempId)
                                        if (tempId != storedId) {
                                            activeWebView?.replaceEpubHighlightId(tempId, storedId)
                                        }
                                    }

                                    webSelectedText = ""
                                    selectedRect = null
                                    activeHighlight = null
                                    activeWebView?.cancelEpubSelection()
                                }
                            } else {
                                webSelectedText = ""
                                selectedRect = null
                                activeHighlight = null
                                activeWebView?.cancelEpubSelection()
                            }
                        }
                    },
                    onNoteSelection = {
                        onNoteSelection(webSelectedText)

                        webSelectedText = ""
                        selectedRect = null
                        activeHighlight = null
                        activeWebView?.cancelEpubSelection()
                    },
                    onDeleteSelection = {
                        val normalizedText = webSelectedText.normalizeReaderSelectionText()

                        val highlightToDelete = activeHighlight
                            ?: latestAnnotations.firstOrNull { annotation ->
                                annotation.type == DeviceReaderAnnotationType.Highlight &&
                                        annotation.selectedText.normalizeReaderSelectionText() == normalizedText
                            }
                            ?: latestAnnotations.firstOrNull { annotation ->
                                annotation.type == DeviceReaderAnnotationType.Highlight &&
                                        normalizedText.contains(annotation.selectedText.normalizeReaderSelectionText())
                            }
                            ?: latestAnnotations.firstOrNull { annotation ->
                                annotation.type == DeviceReaderAnnotationType.Highlight &&
                                        annotation.selectedText.normalizeReaderSelectionText().contains(normalizedText)
                            }

                        if (highlightToDelete != null) {
                            activeWebView?.removeEpubHighlight(highlightToDelete.id)
                            onHighlightDelete(highlightToDelete)
                        }

                        webSelectedText = ""
                        selectedRect = null
                        activeHighlight = null
                        activeWebView?.cancelEpubSelection()
                    }
                )
            }
        }
    }
}

// El puente JS mantiene la selección dentro de WebView y deja que Compose pinte el menú flotante.
@Suppress("unused")
private class EpubJsBridge(
    private val onTextSelected: (String, String, Rect) -> Unit,
    private val onHighlightSelected: (String, String, Rect) -> Unit,
    private val onPageCountReady: (Int, List<Int>) -> Unit,
    private val onPageChanged: (Int) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onSelectionChanged(text: String, tempId: String, x: Float, y: Float, width: Float, height: Float) {
        val rect = Rect(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt())
        mainHandler.post { onTextSelected(text, tempId, rect) }
    }

    @JavascriptInterface
    fun onHighlightTapped(id: String, text: String, x: Float, y: Float, width: Float, height: Float) {
        val rect = Rect(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt())
        mainHandler.post { onHighlightSelected(id, text, rect) }
    }

    @JavascriptInterface
    fun onPageCountCalculated(count: Int, chapterStarts: String) {
        val starts = chapterStarts.split(',')
            .mapNotNull { it.toIntOrNull() }
            .ifEmpty { listOf(0) }
        mainHandler.post { onPageCountReady(count, starts) }
    }

    @JavascriptInterface
    fun onPageChanged(page: Int) {
        mainHandler.post { onPageChanged(page.coerceAtLeast(0)) }
    }
}

private fun String.normalizeReaderSelectionText(): String {
    return trim()
        .replace(Regex("\\s+"), " ")
        .lowercase()
}

private class EpubSelectionActionModeCallback(
    private val delegate: ActionMode.Callback?,
    private val onSelectionChanged: () -> Unit
) : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menu?.clear()
        onSelectionChanged()
        return delegate?.onCreateActionMode(mode, menu) ?: true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menu?.clear()
        onSelectionChanged()
        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        delegate?.onDestroyActionMode(mode)
    }
}

// private fun WebView.captureEpubSelectionPreview(onPreview: (String, Rect) -> Unit) {
//     postDelayed({
//         evaluateJavascript(
//             """
//             (function() {
//                 const sel = window.getSelection();
//                 if (!sel || sel.isCollapsed || sel.rangeCount === 0) return '';
//                 const range = sel.getRangeAt(0);
//                 const rects = Array.from(range.getClientRects()).filter(r => r.width > 0 && r.height > 0);
//                 const rect = rects[0] || range.getBoundingClientRect();
//                 if (!rect || rect.width === 0 || rect.height === 0) return '';
//                 return JSON.stringify({
//                     text: sel.toString(),
//                     left: rect.left,
//                     top: rect.top,
//                     width: rect.width,
//                     height: rect.height
//                 });
//             })();
//             """.trimIndent()
//         ) { rawJson ->
//             val json = rawJson.decodeJavascriptString()
//             if (json.isBlank()) return@evaluateJavascript
//             runCatching {
//                 val obj = JSONObject(json)
//                 val text = obj.optString("text").take(READER_ANNOTATION_TEXT_LIMIT)
//                 if (text.isBlank()) return@runCatching
//                 val left = obj.optDouble("left").toInt()
//                 val top = obj.optDouble("top").toInt()
//                 val width = obj.optDouble("width").toInt()
//                 val height = obj.optDouble("height").toInt()
//                 onPreview(text, Rect(left, top, left + width, top + height))
//             }
//         }
//     }, 80L)
// }


private fun WebView.readCurrentWebSelection(
    onSelection: (String, Rect) -> Unit,
    fallback: () -> Unit = {}
) {
    evaluateJavascript(
        """
        (function() {
            const selection = window.getSelection();

            if (!selection || selection.isCollapsed || selection.rangeCount === 0) {
                return '';
            }

            const range = selection.getRangeAt(0);

            const rects = Array.from(range.getClientRects()).filter(function(rect) {
                return rect.width > 0 &&
                    rect.height > 0 &&
                    rect.right >= 0 &&
                    rect.left <= window.innerWidth &&
                    rect.bottom >= 0 &&
                    rect.top <= window.innerHeight;
            });

            const rect = rects.length ? rects[0] : range.getBoundingClientRect();

            if (!rect || rect.width === 0 || rect.height === 0) {
                return '';
            }

            return JSON.stringify({
                text: selection.toString(),
                left: rect.left,
                top: rect.top,
                width: rect.width,
                height: rect.height
            });
        })();
        """.trimIndent()
    ) { encoded ->
        val raw = encoded.decodeJavascriptString()

        if (raw.isBlank()) {
            fallback()
            return@evaluateJavascript
        }

        runCatching {
            val obj = JSONObject(raw)

            val text = obj.optString("text").orEmpty()
            val left = obj.optDouble("left").toInt()
            val top = obj.optDouble("top").toInt()
            val width = obj.optDouble("width").toInt()
            val height = obj.optDouble("height").toInt()

            if (text.isBlank() || width <= 0 || height <= 0) {
                fallback()
            } else {
                onSelection(
                    text,
                    Rect(
                        left,
                        top,
                        left + width,
                        top + height
                    )
                )
            }
        }.onFailure {
            fallback()
        }
    }
}

private fun WebView.isPointInsideEpubHighlight(
    x: Float,
    y: Float,
    onResult: (Boolean) -> Unit
) {
    evaluateJavascript(
        """
        (function() {
            if (!window.kaiIsPointInsideHighlight) return false;
            return window.kaiIsPointInsideHighlight(${x}, ${y});
        })();
        """.trimIndent()
    ) { encoded ->
        onResult(encoded == "true")
    }
}

// Construye un único documento HTML para que las columnas CSS paginen to-do el EPUB como páginas reales.
private fun buildEpubReaderHtml(
    pages: List<ReaderEpubPage>,
    annotations: List<DeviceReaderAnnotation>,
    resources: Map<String, ByteArray>,
    theme: ReaderColorTheme,
    fontScale: Float
): String {
    val epubCss = resources.epubStylesheetCss()
    val sections = pages.mapIndexed { index, page ->
        val html = page.html.toEpubSectionBody(page.href, resources)
        val bodyClasses = page.html.extractEpubBodyClasses()
        """
        <section class="kai-source $bodyClasses" data-source-index="$index" data-title="${page.title.escapeHtmlAttribute()}">
            $html
        </section>
        """.trimIndent()
    }.joinToString("\n")
    val highlightsJson = annotations.toEpubHighlightsJson()

    return """
        <!doctype html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
                $epubCss
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100vw;
                    height: 100vh;
                    overflow: hidden;
                    background: ${theme.background};
                    color: ${theme.text};
                }
                #kai-reader-content {
                    position: fixed;
                    inset: 0;
                    box-sizing: border-box;
                    column-width: calc(100vw - 52px);
                    column-gap: 52px;
                    font-size: ${(18f * fontScale).coerceIn(12f, 32f)}px;
                    line-height: 1.52;
                    padding: 30px 26px 34px;
                    font-family: serif;
                    overflow-x: hidden;
                    overflow-y: hidden;
                    scrollbar-width: none;
                    -webkit-user-select: none;
                    user-select: none;
                    -webkit-touch-callout: none;
                }
                #kai-reader-content.kai-selection-active {
                    -webkit-user-select: text;
                    user-select: text;
                    -webkit-touch-callout: default;
                }
                #kai-reader-content::-webkit-scrollbar {
                    display: none;
                }
                .kai-source {
                    min-height: calc(100vh - 64px);
                    display: block;
                    -webkit-column-break-inside: auto;
                    break-inside: auto;
                }
                .kai-source + .kai-source {
                    -webkit-column-break-before: always;
                    break-before: column;
                    page-break-before: always;
                }
                .kai-reader-cover {
                    min-height: calc(100vh - 72px);
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    text-align: center;
                }
                .kai-cover-image {
                    max-width: 62vw;
                    max-height: 58vh;
                    object-fit: contain;
                    margin-bottom: 24px;
                }
                .kai-reader-cover h1, h1, h2, h3 {
                    color: ${theme.text};
                    line-height: 1.18;
                }
                .kai-author {
                    opacity: 0.78;
                    font-style: italic;
                }
                .kai-reader-images figure {
                    margin: 0 0 24px;
                    break-inside: avoid;
                    text-align: center;
                }
                img, svg {
                    max-width: calc(100vw - 64px);
                    max-height: calc(100vh - 104px);
                    object-fit: contain;
                }
                p, li, blockquote, div, span, a {
                    color: ${theme.text};
                }
                .kai-highlight {
                    border-radius: 2px;
                    pointer-events: auto;
                    position: relative;
                    z-index: 2;
                    box-decoration-break: clone;
                    -webkit-box-decoration-break: clone;
                    text-decoration-color: var(--kai-highlight-color, #EBC7E8) !important;
                }

                .kai-highlight[data-style="fill"] {
                    background-color: var(--kai-highlight-color, #EBC7E8) !important;
                    background-image: none !important;
                }

                .kai-highlight[data-style="underline"] {
                    background-color: transparent !important;
                    background-image: linear-gradient(
                        to top,
                        var(--kai-highlight-color, #EBC7E8) 0.14em,
                        transparent 0.14em
                    ) !important;
                    background-repeat: repeat-x !important;
                    background-position: 0 100% !important;
                    text-decoration: none !important;
                }

                .kai-highlight[data-style="strike"] {
                    background: transparent !important;
                    text-decoration-line: line-through !important;
                    text-decoration-thickness: 0.12em !important;
                    text-decoration-color: var(--kai-highlight-color, #EBC7E8) !important;
                }

                .kai-highlight[data-style="zigzag"] {
                    background-color: transparent !important;
                    text-decoration-line: underline !important;
                    text-decoration-style: wavy !important;
                    text-decoration-thickness: 0.11em !important;
                    text-underline-offset: 0.18em !important;
                    text-decoration-color: var(--kai-highlight-color, #EBC7E8) !important;
                }

                .kai-highlight[data-style="diagonal"] {
                    background-color: transparent !important;
                    text-decoration-line: underline !important;
                    text-decoration-style: wavy !important;
                    text-decoration-thickness: 0.11em !important;
                    text-underline-offset: 0.18em !important;
                    text-decoration-color: var(--kai-highlight-color, #EBC7E8) !important;
                }
                #kai-magnifier {
                    display: none !important;
                    visibility: hidden !important;
                    opacity: 0 !important;
                    pointer-events: none !important;
                }
            </style>
        </head>
        <body>
            <div id="kai-magnifier"></div>
            <main id="kai-reader-content">
                $sections
            </main>
            <script>
                (function() {
                    const bridge = window.$EPUB_JS_BRIDGE_NAME;
                    const highlights = $highlightsJson;
                    const content = document.getElementById('kai-reader-content');
                    const pageWidth = () => Math.max(1, window.innerWidth);
                    const pageIndex = () => Math.round((content ? content.scrollLeft : 0) / pageWidth());
                    const pageCount = () => Math.max(1, Math.ceil((content ? content.scrollWidth : document.body.scrollWidth) / pageWidth()));
                    const isCssColor = (value) => {
                        if (!value) return false;
                        const trimmed = String(value).trim();
                        return trimmed.startsWith('#') ||
                            trimmed.startsWith('rgb(') ||
                            trimmed.startsWith('rgba(') ||
                            trimmed.startsWith('hsl(') ||
                            trimmed.startsWith('hsla(');
                    };

                    const highlightColor = (raw) => {
                        if (!raw) return '#EBC7E8';
                        const value = String(raw).trim();
                        return value.indexOf(':') >= 0 ? value.split(':').pop() : value;
                    };

                    const highlightStyle = (raw) => {
                        if (!raw) return 'fill';

                        const value = String(raw).trim();

                        if (value.indexOf(':') >= 0) {
                            const style = value.split(':')[0];
                            return style === 'diagonal' ? 'zigzag' : style;
                        }

                        if (isCssColor(value)) {
                            return 'fill';
                        }

                        return value === 'diagonal' ? 'zigzag' : value;
                    };

                    function applyHighlightVisualStyle(span, color) {
                        if (!span) return;

                        const style = highlightStyle(color || '');
                        const resolvedColor = highlightColor(color || '#EBC7E8');

                        span.dataset.style = style;
                        span.style.setProperty('--kai-highlight-color', resolvedColor);
                        span.style.backgroundColor = style === 'fill' ? resolvedColor : 'transparent';
                        span.style.backgroundImage = '';
                        span.style.textDecorationLine = '';
                        span.style.textDecorationStyle = '';
                        span.style.textDecorationColor = resolvedColor;
                    }
                    let selectionArmed = false;
                    let selectionFinished = false;
                    let savedSelectionRange = null;
                    let savedSelectionText = '';

                    function notifyPages() {
                        const starts = Array.from(document.querySelectorAll('.kai-source')).map((node) => {
                            return Math.max(0, Math.round(node.offsetLeft / pageWidth()));
                        });
                        bridge.onPageCountCalculated(pageCount(), starts.join(','));
                        bridge.onPageChanged(pageIndex());
                    }

                    function notifyPageChanged() {
                        bridge.onPageChanged(Math.max(0, Math.min(pageIndex(), pageCount() - 1)));
                    }

                    function goToPage(page) {
                        const safePage = Math.max(0, Math.min(page, pageCount() - 1));
                        if (content) content.scrollTo({ left: safePage * pageWidth(), top: 0, behavior: 'auto' });
                        bridge.onPageChanged(safePage);
                    }

                    function textNodesUnder(root) {
                        const nodes = [];
                        const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
                            acceptNode: (node) => {
                                if (!node.nodeValue || !node.nodeValue.trim()) return NodeFilter.FILTER_REJECT;
                                if (node.parentElement && node.parentElement.closest('.kai-highlight, script, style')) return NodeFilter.FILTER_REJECT;
                                return NodeFilter.FILTER_ACCEPT;
                            }
                        });
                        while (walker.nextNode()) nodes.push(walker.currentNode);
                        return nodes;
                    }

                    function createHighlightSpan(id, color) {
                        const span = document.createElement('span');
                        span.className = 'kai-highlight';
                        span.dataset.id = id;
                        applyHighlightVisualStyle(span, color || '#EBC7E8');
                        return span;
                    }

                    function wrapTextSegment(node, start, end, id, color) {
                        if (!node || start >= end) return null;

                        const range = document.createRange();
                        range.setStart(node, start);
                        range.setEnd(node, end);

                        const span = createHighlightSpan(id, color);
                        try {
                            range.surroundContents(span);
                        } catch (error) {
                            const fragment = range.extractContents();
                            span.appendChild(fragment);
                            range.insertNode(span);
                        }
                        bindHighlight(span);
                        return span;
                    }

                    function selectionSegments(range) {
                        return textNodesUnder(content || document.body)
                            .map((node) => {
                                try {
                                    if (range.intersectsNode && !range.intersectsNode(node)) {
                                        return null;
                                    }
                                } catch (error) {
                                    return null;
                                }

                                const start = node === range.startContainer
                                    ? Math.max(0, Math.min(range.startOffset, node.nodeValue.length))
                                    : 0;
                                const end = node === range.endContainer
                                    ? Math.max(0, Math.min(range.endOffset, node.nodeValue.length))
                                    : node.nodeValue.length;

                                return end > start ? { node, start, end } : null;
                            })
                            .filter(Boolean);
                    }

                    function wrapRangeWithHighlights(range, id, color) {
                        const segments = selectionSegments(range);
                        const spans = [];

                        segments.forEach((segment) => {
                            const span = wrapTextSegment(segment.node, segment.start, segment.end, id, color);
                            if (span) spans.push(span);
                        });

                        return spans;
                    }

                    function applyHighlight(annotation) {
                        if (!annotation.text) return;
                        removeHighlightById(annotation.id);

                        const exactSegments = findTextSegments(annotation.text);
                        if (exactSegments.length > 0) {
                            exactSegments.forEach((segment) => {
                                wrapTextSegment(segment.node, segment.start, segment.end, annotation.id, annotation.color || '#EBC7E8');
                            });
                            return;
                        }

                        const normalizedSegments = findNormalizedTextSegments(annotation.text);
                        normalizedSegments.forEach((segment) => {
                            wrapTextSegment(segment.node, segment.start, segment.end, annotation.id, annotation.color || '#EBC7E8');
                        });
                    }

                    function findTextSegments(text) {
                        const needle = String(text || '');
                        if (!needle) return [];

                        const nodes = textNodesUnder(content || document.body);
                        let fullText = '';
                        const entries = nodes.map((node) => {
                            const start = fullText.length;
                            fullText += node.nodeValue || '';
                            return {
                                node,
                                start,
                                end: fullText.length
                            };
                        });

                        const index = fullText.indexOf(needle);
                        if (index < 0) return [];

                        return segmentsFromAbsoluteRange(entries, index, index + needle.length);
                    }

                    function findNormalizedTextSegments(text) {
                        const needle = normalizeReaderText(text);
                        if (!needle) return [];

                        const nodes = textNodesUnder(content || document.body);
                        const entries = [];
                        let normalizedText = '';

                        nodes.forEach((node) => {
                            const raw = node.nodeValue || '';
                            for (let offset = 0; offset < raw.length; offset += 1) {
                                const normalizedChar = /\s/.test(raw[offset]) ? ' ' : raw[offset];
                                const previous = normalizedText[normalizedText.length - 1];

                                if (normalizedChar === ' ' && previous === ' ') continue;

                                entries.push({
                                    node,
                                    offset,
                                    normalizedIndex: normalizedText.length
                                });
                                normalizedText += normalizedChar;
                            }
                        });

                        const index = normalizedText.indexOf(needle);
                        if (index < 0) return [];

                        const endIndex = index + needle.length - 1;
                        const startEntry = entries.find((entry) => entry.normalizedIndex === index);
                        const endEntry = entries.find((entry) => entry.normalizedIndex === endIndex);

                        if (!startEntry || !endEntry) return [];

                        return textSegmentsBetween(nodes, startEntry.node, startEntry.offset, endEntry.node, endEntry.offset + 1);
                    }

                    function normalizeReaderText(text) {
                        return String(text || '').replace(/\s+/g, ' ').trim();
                    }

                    function segmentsFromAbsoluteRange(entries, start, end) {
                        return entries
                            .map((entry) => {
                                const segmentStart = Math.max(start, entry.start);
                                const segmentEnd = Math.min(end, entry.end);

                                if (segmentEnd <= segmentStart) return null;

                                return {
                                    node: entry.node,
                                    start: segmentStart - entry.start,
                                    end: segmentEnd - entry.start
                                };
                            })
                            .filter(Boolean);
                    }

                    function textSegmentsBetween(nodes, startNode, startOffset, endNode, endOffset) {
                        let inside = false;
                        const segments = [];

                        nodes.forEach((node) => {
                            if (node === startNode) inside = true;
                            if (!inside) return;

                            const start = node === startNode ? startOffset : 0;
                            const end = node === endNode ? endOffset : (node.nodeValue || '').length;

                            if (end > start) {
                                segments.push({ node, start, end });
                            }

                            if (node === endNode) inside = false;
                        });

                        return segments;
                    }

                    function bindHighlight(span) {
                        const handler = (event) => {
                            event.preventDefault();
                            event.stopPropagation();
                            event.stopImmediatePropagation();
                            window.kaiLastHighlightTapAt = Date.now();
                            openHighlightMenu(span);
                            return false;
                        };

                        span.addEventListener('click', handler, true);
                        span.addEventListener('touchstart', handler, { capture: true, passive: false });
                        span.addEventListener('touchend', handler, { capture: true, passive: false });
                        span.addEventListener('pointerdown', handler, true);
                        span.addEventListener('pointerup', handler, true);
                    }

                    function removeHighlightById(id) {
                        if (!id) return;

                        highlightSpansById(id).forEach((span) => {
                            const textNode = document.createTextNode(span.textContent || '');
                            span.replaceWith(textNode);

                            if (textNode.parentNode) {
                                textNode.parentNode.normalize();
                            }
                        });
                    }

                    function highlightSpansById(id) {
                        if (!id) return [];
                        return Array.from(document.querySelectorAll('.kai-highlight')).filter((span) => span.dataset.id === id);
                    }

                    function highlightTextById(id) {
                        return highlightSpansById(id).map((span) => span.innerText || span.textContent || '').join('');
                    }

                    function bestVisibleRect(rects) {
                        const visible = Array.from(rects || []).filter((rect) => {
                            return rect.width > 0 &&
                                rect.height > 0 &&
                                rect.right >= 0 &&
                                rect.left <= window.innerWidth &&
                                rect.bottom >= 0 &&
                                rect.top <= window.innerHeight;
                        });

                        if (visible.length === 0) return null;

                        visible.sort((a, b) => {
                            const aDistance = Math.abs(a.top - window.innerHeight * 0.45) + Math.abs(a.left);
                            const bDistance = Math.abs(b.top - window.innerHeight * 0.45) + Math.abs(b.left);
                            return aDistance - bDistance;
                        });

                        return visible[0];
                    }

                    function openHighlightMenu(span) {
                        if (!span) return false;

                        const rect = bestVisibleRect(span.getClientRects()) || span.getBoundingClientRect();

                        if (!rect || rect.width === 0 || rect.height === 0) {
                            return false;
                        }

                        window.getSelection().removeAllRanges();
                        selectionArmed = false;
                        selectionFinished = false;
                        savedSelectionRange = null;
                        savedSelectionText = '';
                        hideMagnifier();

                        if (content) {
                            content.classList.remove('kai-selection-active');
                        }

                        bridge.onHighlightTapped(
                            span.dataset.id || '',
                            span.innerText || span.textContent || '',
                            rect.left,
                            rect.top,
                            rect.width,
                            rect.height
                        );

                        return true;
                    }

                    function wrapCurrentSelection(color) {
                        const liveSelected = selectionRect();
                        const range = liveSelected && liveSelected.selection && liveSelected.selection.rangeCount
                            ? liveSelected.selection.getRangeAt(0).cloneRange()
                            : savedSelectionRange;
                        const text = liveSelected && liveSelected.text ? liveSelected.text : (savedSelectionText || '');
                        if (!range || !text.trim()) return null;
                        const id = 'tmp-' + Date.now() + '-' + Math.floor(Math.random() * 100000);
                        const spans = wrapRangeWithHighlights(range, id, color || '#EBC7E8');
                        if (spans.length === 0) return null;
                        savedSelectionRange = null;
                        savedSelectionText = '';

                        const rects = [];
                        spans.forEach((span) => {
                            Array.from(span.getClientRects()).forEach((rect) => rects.push(rect));
                        });
                        const rect = bestVisibleRect(rects) || spans[0].getBoundingClientRect();

                        return { id: id, text: text, rect: rect };
                    }

                    function showMagnifier(text, rect) {
                        hideMagnifier();
                    }

                    function hideMagnifier() {
                        const magnifier = document.getElementById('kai-magnifier');
                        if (!magnifier) return;

                        magnifier.textContent = '';
                        magnifier.style.display = 'none';
                        magnifier.style.visibility = 'hidden';
                        magnifier.style.opacity = '0';
                        magnifier.style.pointerEvents = 'none';
                    }

                    function selectionRect() {
                        const selection = window.getSelection();

                        if (!selection || selection.isCollapsed || selection.rangeCount === 0) {
                            return null;
                        }

                        const range = selection.getRangeAt(0);
                        const rect = bestVisibleRect(range.getClientRects()) || range.getBoundingClientRect();

                        if (!rect || rect.width === 0 || rect.height === 0) {
                            return null;
                        }

                        return {
                            selection: selection,
                            rect: rect,
                            text: selection.toString()
                        };
                    }

                    function rememberSelection(selected) {
                        if (!selected || !selected.text.trim()) return;
                        savedSelectionRange = selected.selection.getRangeAt(0).cloneRange();
                        savedSelectionText = selected.text;
                    }

                    highlights.forEach((annotation) => {
                        try {
                            applyHighlight(annotation);
                        } catch (error) {
                        }
                    });
                    document.querySelectorAll('.kai-highlight').forEach(bindHighlight);
                    window.kaiGoToPage = goToPage;
                    window.kaiWheelPageTimer = 0;
                    window.kaiLastHighlightTapAt = 0;

                    function findHighlightAtPoint(x, y) {
                        const elements = document.elementsFromPoint
                            ? document.elementsFromPoint(x, y)
                            : [document.elementFromPoint(x, y)].filter(Boolean);

                        const direct = elements
                            .map((element) => element && element.closest ? element.closest('.kai-highlight') : null)
                            .find(Boolean);

                        if (direct) return direct;

                        const tolerance = 8;
                        const highlights = Array.from(document.querySelectorAll('.kai-highlight'));

                        return highlights.find((span) => {
                            const rects = Array.from(span.getClientRects());
                            return rects.some((rect) => {
                                return x >= rect.left - tolerance &&
                                    x <= rect.right + tolerance &&
                                    y >= rect.top - tolerance &&
                                    y <= rect.bottom + tolerance;
                            });
                        }) || null;
                    }

                    window.kaiTapAt = function(x, y) {
                        const highlight = findHighlightAtPoint(x, y);

                        if (highlight) {
                            window.kaiLastHighlightTapAt = Date.now();
                            return openHighlightMenu(highlight);
                        }

                        return false;
                    };
                    window.kaiIsPointInsideHighlight = function(x, y) {
                        return !!findHighlightAtPoint(x, y);
                    };
                    window.kaiEnableSelection = function() {
                        selectionArmed = true;
                        selectionFinished = false;
                        hideMagnifier();

                        if (content) {
                            content.classList.add('kai-selection-active');
                        }
                    };
                    window.kaiDisableSelection = function() {
                        selectionArmed = false;
                        selectionFinished = false;
                        hideMagnifier();

                        if (content) {
                            content.classList.remove('kai-selection-active');
                        }
                    };
                    window.kaiFinishSelection = function() {
                        if (!selectionArmed || selectionFinished) {
                            hideMagnifier();
                            return;
                        }

                        const selected = selectionRect();
                        hideMagnifier();

                        if (!selected || !selected.text.trim()) return;

                        selectionFinished = true;

                        if (content) {
                            content.classList.add('kai-selection-active');
                        }

                        rememberSelection(selected);
                        bridge.onSelectionChanged(selected.text, '', selected.rect.left, selected.rect.top, selected.rect.width, selected.rect.height);
                    };
                    window.kaiCancelSelection = function() {
                        selectionArmed = false;
                        selectionFinished = false;
                        savedSelectionRange = null;
                        savedSelectionText = '';
                        hideMagnifier();

                        if (content) {
                            content.classList.remove('kai-selection-active');
                        }

                        window.getSelection().removeAllRanges();
                    };
                    window.kaiUpdateHighlight = function(id, color) {
                        highlightSpansById(id).forEach((span) => {
                            applyHighlightVisualStyle(span, color || '#EBC7E8');
                        });
                    };
                    window.kaiSyncHighlights = function(nextHighlights) {
                        const incoming = Array.isArray(nextHighlights) ? nextHighlights : [];
                        const incomingById = new Map(incoming.filter((item) => item && item.id).map((item) => [item.id, item]));
                        const existingIds = new Set(Array.from(document.querySelectorAll('.kai-highlight'))
                            .map((span) => span.dataset.id || '')
                            .filter(Boolean));

                        existingIds.forEach((id) => {
                            if (!incomingById.has(id)) {
                                removeHighlightById(id);
                            }
                        });

                        incoming.forEach((annotation) => {
                            try {
                                if (!annotation || !annotation.id) return;

                                const existing = highlightSpansById(annotation.id);
                                if (existing.length > 0) {
                                    const currentText = highlightTextById(annotation.id);
                                    if (currentText === annotation.text || normalizeReaderText(currentText) === normalizeReaderText(annotation.text)) {
                                        existing.forEach((span) => applyHighlightVisualStyle(span, annotation.color || '#EBC7E8'));
                                    } else {
                                        removeHighlightById(annotation.id);
                                        applyHighlight(annotation);
                                    }
                                } else {
                                    applyHighlight(annotation);
                                }
                            } catch (error) {
                            }
                        });
                    };
                    window.kaiApplySelectionHighlight = function(color) {
                        const selected = wrapCurrentSelection(color || '#EBC7E8');

                        if (!selected || !selected.text.trim()) {
                            hideMagnifier();
                            return '';
                        }

                        hideMagnifier();

                        selectionArmed = false;
                        selectionFinished = false;
                        savedSelectionRange = null;
                        savedSelectionText = '';

                        if (content) {
                            content.classList.remove('kai-selection-active');
                        }

                        window.getSelection().removeAllRanges();

                        return selected.id || '';
                    };
                    window.kaiReplaceHighlightId = function(oldId, newId) {
                        if (!oldId || !newId) return;
                        highlightSpansById(oldId).forEach((span) => {
                            span.dataset.id = newId;
                        });
                    };
                    window.kaiRemoveHighlight = function(id) {
                        removeHighlightById(id);
                        window.getSelection().removeAllRanges();
                    };

                    document.addEventListener('selectionchange', () => {
                        hideMagnifier();

                        if (!selectionArmed) {
                            return;
                        }
                        const selected = selectionRect();
                        if (!selected) {
                            return;
                        }
                        rememberSelection(selected);
                        bridge.onSelectionChanged(selected.text, '', selected.rect.left, selected.rect.top, selected.rect.width, selected.rect.height);
                    });
                    document.addEventListener('touchend', () => {
                        if (!selectionArmed || selectionFinished) {
                            hideMagnifier();
                            return;
                        }

                        window.setTimeout(() => {
                            if (window.kaiFinishSelection) window.kaiFinishSelection();
                        }, 120);
                    }, { passive: true });
                    document.addEventListener('mouseup', () => {
                        if (!selectionArmed || selectionFinished) {
                            hideMagnifier();
                            return;
                        }

                        window.setTimeout(() => {
                            if (window.kaiFinishSelection) window.kaiFinishSelection();
                        }, 80);
                    });
                    if (content) {
                        content.addEventListener('scroll', () => window.clearTimeout(window.kaiScrollTimer) || (window.kaiScrollTimer = window.setTimeout(notifyPageChanged, 80)));
                        content.addEventListener('wheel', (event) => {
                            event.preventDefault();
                            const now = Date.now();
                            if (now - window.kaiWheelPageTimer < 360 || Math.abs(event.deltaY) < 10) return;
                            window.kaiWheelPageTimer = now;
                            goToPage(pageIndex() + (event.deltaY > 0 ? 1 : -1));
                        }, { passive: false });
                    }
                    window.addEventListener('resize', () => window.setTimeout(notifyPages, 120));
                    window.addEventListener('load', () => window.setTimeout(notifyPages, 180));
                    window.setTimeout(notifyPages, 120);
                    window.setTimeout(notifyPages, 360);
                    window.setTimeout(notifyPages, 900);
                    window.setTimeout(notifyPages, 1600);
                })();
            </script>
        </body>
        </html>
    """.trimIndent()
}

// La navegación siempre salta a una columna concreta; tap, gesto, scroll y slider usan el mismo índice.
private fun WebView.goToEpubPage(page: Int) {
    evaluateJavascript("window.kaiGoToPage && window.kaiGoToPage(${page.coerceAtLeast(0)});", null)
}

private fun WebView.updateEpubHighlightStyle(annotationId: String, color: String) {
    evaluateJavascript(
        "window.kaiUpdateHighlight && window.kaiUpdateHighlight('${annotationId.escapeJsString()}', '${color.escapeJsString()}');",
        null
    )
}

private fun WebView.removeEpubHighlight(annotationId: String) {
    evaluateJavascript(
        "window.kaiRemoveHighlight && window.kaiRemoveHighlight('${annotationId.escapeJsString()}');",
        null
    )
}

private fun WebView.syncEpubHighlights(highlightsJson: String) {
    evaluateJavascript(
        """
        (function() {
            if (!window.kaiSyncHighlights) return;
            window.kaiSyncHighlights($highlightsJson);
        })();
        """.trimIndent(),
        null
    )
}

private fun WebView.createEpubHighlight(
    color: String,
    onCreated: (String) -> Unit
) {
    evaluateJavascript(
        """
        (function() {
            if (!window.kaiApplySelectionHighlight) return '';
            return window.kaiApplySelectionHighlight('${color.escapeJsString()}');
        })();
        """.trimIndent()
    ) { encoded ->
        onCreated(encoded.decodeJavascriptString())
    }
}

private fun WebView.cancelEpubSelection() {
    evaluateJavascript("window.kaiCancelSelection && window.kaiCancelSelection();", null)
}

private fun WebView.replaceEpubHighlightId(oldId: String?, newId: String) {
    if (oldId.isNullOrBlank() || newId.isBlank()) return
    evaluateJavascript(
        "window.kaiReplaceHighlightId && window.kaiReplaceHighlightId('${oldId.escapeJsString()}', '${newId.escapeJsString()}');",
        null
    )
}

private fun List<DeviceReaderAnnotation>.toEpubHighlightsJson(): String {
    return joinToString(
        prefix = "[",
        postfix = "]"
    ) { annotation ->
        """{"id":"${annotation.id.escapeJsString()}","text":"${annotation.selectedText.escapeJsString()}","color":"${annotation.color.escapeJsString()}"}"""
    }
}

private fun snapEpubWebViewToNearestPage(webView: WebView, onPageChanged: (Int) -> Unit) {
    webView.evaluateJavascript(
        """
        (function(){
            var width = Math.max(1, window.innerWidth);
            var content = document.getElementById('kai-reader-content');
            var page = Math.round(((content && content.scrollLeft) || 0) / width);
            if (window.kaiGoToPage) window.kaiGoToPage(page);
            return page;
        })();
        """.trimIndent()
    ) { encoded ->
        encoded.toIntOrNull()?.let { onPageChanged(it.coerceAtLeast(0)) }
    }
}

private fun String.toEpubSectionBody(pageHref: String, resources: Map<String, ByteArray>): String {
    return withoutExecutableScripts()
        .rewriteEpubRelativeResources(pageHref)
        .inlineEpubImages(resources)
        .extractEpubBodyContent()
}

private fun String.extractEpubBodyClasses(): String {
    val match = Regex("(?is)<body\\b[^>]*\\bclass\\s*=\\s*(['\"])(.*?)\\1").find(this)
    return match?.groupValues?.getOrNull(2)
        ?.split(Regex("\\s+"))
        ?.joinToString(" ") { it.escapeHtmlAttribute() }
        .orEmpty()
}

private fun String.extractEpubBodyContent(): String {
    val bodyMatch = Regex("(?is)<body\\b[^>]*>(.*?)</body>").find(this)
    return (bodyMatch?.groupValues?.getOrNull(1) ?: this)
        .replace(Regex("(?is)<head\\b.*?</head>"), "")
}

private fun Map<String, ByteArray>.epubStylesheetCss(): String {
    return entries
        .filter { it.key.endsWith(".css", ignoreCase = true) }
        .joinToString("\n") { (path, bytes) ->
            bytes.decodeToString()
                .rewriteEpubCssUrls(path)
                .inlineEpubCssImages(this)
        }
}

private fun String.rewriteEpubCssUrls(cssPath: String): String {
    val directory = cssPath.substringBeforeLast('/', missingDelimiterValue = "")
    return replace(Regex("""(?i)url\(\s*(['"]?)(?!https?:|data:|#)([^'")]+)\1\s*\)""")) { match ->
        val rawValue = match.groupValues[2].trim()
        val valueWithoutFragment = rawValue.substringBefore('#')
        val fragment = rawValue.substringAfter('#', missingDelimiterValue = "")
        val resolvedPath = when {
            rawValue.startsWith("/") -> rawValue.trimStart('/')
            directory.isBlank() -> valueWithoutFragment
            else -> "$directory/$valueWithoutFragment"
        }.normalizeEpubPath()
        val resolved = buildString {
            append("https://")
            append(EPUB_WEB_HOST)
            append("/")
            append(resolvedPath)
            if (fragment.isNotBlank()) {
                append("#")
                append(fragment)
            }
        }
        "url('$resolved')"
    }
}

private fun String.rewriteEpubRelativeResources(pageHref: String): String {
    val directory = pageHref.substringBeforeLast('/', missingDelimiterValue = "")
    return replace(Regex("""(?i)\b(src|href)\s*=\s*(['"])(?!https?:|data:|mailto:|#)([^'"]+)\2""")) { match ->
        val attr = match.groupValues[1]
        val quote = match.groupValues[2]
        val rawValue = match.groupValues[3]
        val valueWithoutFragment = rawValue.substringBefore('#')
        val fragment = rawValue.substringAfter('#', missingDelimiterValue = "")
        val resolvedPath = when {
            rawValue.startsWith("/") -> rawValue.trimStart('/')
            directory.isBlank() -> valueWithoutFragment
            else -> "$directory/$valueWithoutFragment"
        }.normalizeEpubPath()
        val resolved = buildString {
            append("https://")
            append(EPUB_WEB_HOST)
            append("/")
            append(resolvedPath)
            if (fragment.isNotBlank()) {
                append("#")
                append(fragment)
            }
        }
        "$attr=$quote$resolved$quote"
    }
}

private fun String.inlineEpubImages(resources: Map<String, ByteArray>): String {
    return replace(Regex("""(?i)\bsrc\s*=\s*(['"])https://$EPUB_WEB_HOST/([^'"]+)\1""")) { match ->
        val quote = match.groupValues[1]
        val rawPath = match.groupValues[2].substringBefore('#')
        val path = rawPath.normalizeEpubPath()
        val bytes = resources[path] ?: resources.entries.firstOrNull { it.key.equals(path, ignoreCase = true) }?.value
        if (bytes == null) {
            match.value
        } else {
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "src=$quote" + "data:${path.epubMimeType()};base64,$encoded" + quote
        }
    }
}

private fun String.inlineEpubCssImages(resources: Map<String, ByteArray>): String {
    return replace(Regex("""(?i)url\(\s*(['"])https://$EPUB_WEB_HOST/([^'"]+)\1\s*\)""")) { match ->
        val quote = match.groupValues[1]
        val rawPath = match.groupValues[2].substringBefore('#')
        val path = rawPath.normalizeEpubPath()
        val bytes = resources[path] ?: resources.entries.firstOrNull { it.key.equals(path, ignoreCase = true) }?.value
        if (bytes == null) {
            match.value
        } else {
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "url($quote" + "data:${path.epubMimeType()};base64,$encoded" + quote + ")"
        }
    }
}

private fun String.normalizeEpubPath(): String {
    val parts = split('/').filter { it.isNotBlank() }
    val stack = mutableListOf<String>()
    parts.forEach { part ->
        when (part) {
            "." -> Unit
            ".." -> if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
            else -> stack += part
        }
    }
    return stack.joinToString("/")
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

private const val EPUB_WEB_HOST = "kai-epub.local"
private const val EPUB_JS_BRIDGE_NAME = "KaiEpubBridge"
private const val EPUB_SYNTHETIC_COVER_HREF = "__kai_cover__.xhtml"
private const val EPUB_SYNTHETIC_SYNOPSIS_HREF = "__kai_synopsis__.xhtml"

// Sin uso actual. Para cargar las páginas EPUB por URL base individual
//private fun ReaderEpubPage.epubBaseUrl(): String {
//    val directory = href.substringBeforeLast('/', missingDelimiterValue = "")
//    return if (directory.isBlank()) {
//        "https://$EPUB_WEB_HOST/"
//    } else {
//        "https://$EPUB_WEB_HOST/$directory/"
//    }
//}

// Sin uso actual. Los colores se inyectan desde buildEpubReaderHtml.
//private fun String.withReaderColors(theme: ReaderColorTheme): String {
//    val colorCss = """
//        <style type="text/css">
//        html, body { background:${theme.background}; color:${theme.text}; }
//        body, p, li, blockquote, div, span, h1, h2, h3, h4, h5, h6 { color:${theme.text}; }
//        a { color:${theme.text}; }
//        </style>
//    """.trimIndent()
//    return if (contains("</head>", ignoreCase = true)) {
//        replace(Regex("(?i)</head>"), "$colorCss\n</head>")
//    } else {
//        "$colorCss\n$this"
//    }
//}

private fun String.withReadableFallback(fallbackText: String): String {
    if (fallbackText.isBlank()) return this
    if (contains(Regex("(?is)<img\\b|<svg\\b"))) return this
    val hasVisibleBodyText = extractEpubBodyContent()
        .replace(Regex("(?is)<(script|style)\\b.*?</\\1>"), "")
        .replace(Regex("(?is)<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .trim()
        .isNotBlank()
    if (hasVisibleBodyText) return this
    return "<section><p>${fallbackText.escapeHtml().replace("\n\n", "</p><p>").replace("\n", "<br />")}</p></section>"
}

private fun String.escapeHtml(): String {
    return replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}

private fun String.escapeHtmlAttribute(): String = escapeHtml()

private fun String.escapeJsString(): String {
    return buildString {
        this@escapeJsString.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '\'' -> append("\\'")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
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
    return runCatching { Color(toColorInt()) }
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
@Suppress("UNUSED_VALUE")
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
            val storedPageCount = remember(file.uri) { readDeviceBookStoredPageCount(context, file) }
            var currentPage by remember(file.uri) {
                mutableStateOf(readDeviceBookCurrentPage(context, file, storedPageCount))
            }
            var logicalPageCount by remember(file.uri) { mutableStateOf(storedPageCount.coerceAtLeast(1)) }
            var controlsVisible by remember(file.uri) { mutableStateOf(false) }
            var showDisplaySettings by remember(file.uri) { mutableStateOf(false) }
            var showTextSizeSettings by remember(file.uri) { mutableStateOf(false) }
            var showThemeSettings by remember(file.uri) { mutableStateOf(false) }
            var showBookInfo by remember(file.uri) { mutableStateOf(false) }
            var showBookInfoMore by remember(file.uri) { mutableStateOf(false) }
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
                    logicalPageCount = logicalPageCount.coerceAtLeast(1)
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
                        annotations = annotations,
                        sourcePageStartPages = sourcePageStartPages,
                        onPageSelected = { page ->
                            currentPage = page.coerceIn(0, logicalPageCount - 1)
                            scope.launch { drawerState.close() }
                        },
                        onAnnotationEdit = { annotation -> editingAnnotation = annotation },
                        onAnnotationDelete = { annotation ->
                            annotationRepository.deleteAnnotation(file, annotation.id)
                            annotations = annotationRepository.getAnnotations(file)
                        },
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
                            pages = loadedDocument.epubPages,
                            resources = loadedDocument.epubResources,
                            annotations = annotations,
                            pageIndex = currentPage,
                            textSizePercent = textSizePercent,
                            colorTheme = readerColorTheme,
                            onPageCountChanged = { count -> logicalPageCount = count.coerceAtLeast(1) },
                            onChapterPageStartsChanged = { starts -> sourcePageStartPages = starts.ifEmpty { listOf(0) } },
                            onPageChanged = { page -> currentPage = page.coerceIn(0, logicalPageCount - 1) },
                            onVisibleTextChanged = { visiblePageText = it },
                            onSelectedTextChanged = { selectedText = it },
                            onHighlightSelection = { selected, highlightColor, requestedId ->
                                val storedHighlight = annotationRepository.addAnnotation(
                                    file = file,
                                    annotation = DeviceReaderAnnotation(
                                        id = requestedId.orEmpty(),
                                        type = DeviceReaderAnnotationType.Highlight,
                                        page = currentPage,
                                        pageCount = logicalPageCount,
                                        selectedText = selected.take(READER_ANNOTATION_TEXT_LIMIT),
                                        color = highlightColor
                                    )
                                )
                                annotations = annotationRepository.getAnnotations(file)
                                storedHighlight.id
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
                            onTitleClick = { showBookInfo = true },
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

                    if (showBookInfo) {
                        PdfBookInfoDialog(
                            file = file,
                            currentPage = currentPage,
                            pageCount = logicalPageCount,
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
}
@Composable
fun PdfBookReader(
    @Suppress("UNUSED_VALUE")
    file: DeviceLibraryFile,
    onProgressChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val rememberedPageCount = remember(file.uri) {
        readDeviceBookStoredPageCount(context, file).coerceAtLeast(1)
    }
    val pageCount by produceState(initialValue = rememberedPageCount, file.uri) {
        val detectedPageCount = withContext(Dispatchers.IO) { getPdfPageCount(context, file.uri) }
        value = detectedPageCount
    }

    when (val pages = pageCount) {
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
                        annotations = annotations,
                        onPageSelected = { page ->
                            currentPage = page.coerceIn(0, activePageCount - 1)
                            scope.launch { drawerState.close() }
                        },
                        onAnnotationEdit = { annotation -> editingAnnotation = annotation },
                        onAnnotationDelete = { annotation ->
                            annotationRepository.deleteAnnotation(file, annotation.id)
                            annotations = annotationRepository.getAnnotations(file)
                        },
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



