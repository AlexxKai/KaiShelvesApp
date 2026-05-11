package com.example.kaishelvesapp.ui.screen.library

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.DeviceReaderAnnotation
import com.example.kaishelvesapp.data.model.DeviceReaderAnnotationType
import com.example.kaishelvesapp.data.repository.DeviceLibraryFile
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

fun normalizeSelectionRange(
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

fun wordRangeAt(text: String, offset: Int): IntRange {
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

fun buildSelectableReaderText(
    text: String,
    selectedRange: IntRange?,
    highlights: List<DeviceReaderAnnotation> = emptyList(),
    pageTextRange: ReaderPageTextRange? = null
): AnnotatedString {
    val ranges = mutableListOf<Pair<IntRange, ReaderHighlightStyle>>()
    highlights.forEach { highlight ->
        val hasStoredRange = highlight.sourcePage >= 0 && highlight.selectionStart >= 0 && highlight.selectionEnd > highlight.selectionStart
        if (hasStoredRange && pageTextRange != null && highlight.sourcePage == pageTextRange.sourcePage) {
            val start = maxOf(highlight.selectionStart, pageTextRange.start) - pageTextRange.start
            val end = minOf(highlight.selectionEnd, pageTextRange.end) - pageTextRange.start
            if (end > start) {
                ranges += (start until end) to highlight.color.readerHighlightStyle()
            }
        } else if (!hasStoredRange) {
            val highlightedText = highlight.selectedText.takeIf { it.isNotBlank() } ?: return@forEach
            var searchStart = 0
            while (searchStart < text.length) {
                val matchStart = text.indexOf(highlightedText, searchStart, ignoreCase = false)
                if (matchStart < 0) break
                val matchEnd = (matchStart + highlightedText.length).coerceAtMost(text.length)
                ranges += (matchStart until matchEnd) to highlight.color.readerHighlightStyle()
                searchStart = matchEnd
            }
        }
    }
    selectedRange?.let { range ->
        ranges += range to ReaderHighlightStyle(background = Color(0x66EBC7E8))
    }
    if (ranges.isEmpty()) return AnnotatedString(text)

    val normalizedRanges = ranges
        .mapNotNull { (range, style) ->
            val start = range.first.coerceIn(0, text.length)
            val end = (range.last + 1).coerceIn(start, text.length)
            if (start == end) null else (start until end) to style
        }
        .sortedBy { it.first.first }

    return buildAnnotatedString {
        var cursor = 0
        normalizedRanges.forEach { (range, highlightStyle) ->
            val start = range.first.coerceIn(cursor, text.length)
            val end = (range.last + 1).coerceIn(start, text.length)
            if (cursor < start) append(text.substring(cursor, start))
            withStyle(
                SpanStyle(
                    background = highlightStyle.background,
                    textDecoration = highlightStyle.textDecoration
                )
            ) {
                append(text.substring(start, end))
            }
            cursor = end
        }
        if (cursor < text.length) append(text.substring(cursor))
    }
}

data class ReaderHighlightStyle(
    val background: Color,
    val textDecoration: TextDecoration? = null
)

data class ReaderTextSelection(
    val text: String,
    val sourcePage: Int,
    val start: Int,
    val end: Int
)

private data class ReaderVisibleHighlight(
    val annotation: DeviceReaderAnnotation,
    val range: IntRange
)

fun String.readerHighlightStyle(): ReaderHighlightStyle {
    val rawStyle = substringBefore(":", "")
    val color = readerHighlightColor()
    return ReaderHighlightStyle(
        background = color,
        textDecoration = when (rawStyle) {
            "underline", "diagonal" -> TextDecoration.Underline
            "strike" -> TextDecoration.LineThrough
            else -> null
        }
    )
}

fun String.readerHighlightColor(): Color {
    val colorPart = substringAfter(":", this)
    return runCatching {
        Color(android.graphics.Color.parseColor(colorPart))
    }.getOrElse {
        Color(0x66EBC7E8)
    }
}


@Composable
fun ReflowTextReaderPage(
    file: DeviceLibraryFile? = null,
    sourcePages: List<String>,
    sourcePageTitles: List<String?> = emptyList(),
    sourcePageKinds: List<ReaderSourcePageKind> = emptyList(),
    pageIndex: Int,
    textSizePercent: Int,
    onPageCountChanged: (Int) -> Unit,
    onSourcePageStartPagesChanged: (List<Int>) -> Unit = {},
    onVisibleTextChanged: (String) -> Unit = {},
    onSelectedTextChanged: (String) -> Unit = {},
    onPageChanged: (Int) -> Unit = {},
    onPreviousPage: () -> Unit = {},
    onNextPage: () -> Unit = {},
    onCenterTap: () -> Unit = {},
    highlights: List<DeviceReaderAnnotation> = emptyList(),
    onHighlightSelection: (ReaderTextSelection, String) -> Unit = { _, _ -> },
    onHighlightUpdate: (DeviceReaderAnnotation, String) -> Unit = { _, _ -> },
    onHighlightDelete: (DeviceReaderAnnotation) -> Unit = {},
    onNoteSelection: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var activeSelectionText by remember { mutableStateOf("") }
    var activeSelection by remember { mutableStateOf<ReaderTextSelection?>(null) }
    var activeSelectionPageIndex by remember { mutableStateOf<Int?>(null) }
    var activeHighlight by remember { mutableStateOf<DeviceReaderAnnotation?>(null) }
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
        val pagination by produceState<ReaderPaginationResult>(
            initialValue = ReaderPaginationResult(pages = listOf(""), sourceStartPages = listOf(0)),
            sourcePages,
            textSizePercent,
            contentWidthPx,
            contentHeightPx
        ) {
            value = withContext(Dispatchers.Default) {
                paginateReflowTextWithStarts(
                    sourcePages = sourcePages,
                    sourcePageTitles = sourcePageTitles,
                    sourcePageKinds = sourcePageKinds,
                    fontSizePx = fontSizePx,
                    lineHeightPx = lineHeightPx,
                    maxWidthPx = contentWidthPx,
                    maxHeightPx = (contentHeightPx - with(density) { 14.dp.roundToPx() }).coerceAtLeast(1)
                )
            }
        }
        val logicalPages = pagination.pages
        val safePageIndex = pageIndex.coerceIn(0, logicalPages.lastIndex.coerceAtLeast(0))
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = safePageIndex)

        LaunchedEffect(logicalPages.size) {
            onPageCountChanged(logicalPages.size.coerceAtLeast(1))
        }
        LaunchedEffect(pagination.sourceStartPages) {
            onSourcePageStartPagesChanged(pagination.sourceStartPages)
        }
        LaunchedEffect(safePageIndex, logicalPages.size) {
            if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != safePageIndex) {
                listState.scrollToItem(safePageIndex)
            }
            onVisibleTextChanged(logicalPages.getOrElse(safePageIndex) { "" })
        }
        LaunchedEffect(listState, logicalPages) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .collect { visibleIndex ->
                    val safeVisibleIndex = visibleIndex.coerceIn(0, logicalPages.lastIndex.coerceAtLeast(0))
                    onPageChanged(safeVisibleIndex)
                    onVisibleTextChanged(logicalPages.getOrElse(safeVisibleIndex) { "" })
                    onSelectedTextChanged("")
                    activeSelectionText = ""
                    activeSelection = null
                    activeSelectionPageIndex = null
                    activeHighlight = null
                }
        }
        // Las páginas se mantienen paginadas, pero se presentan en una lista continua para seguir leyendo con scroll.
        LazyColumn(
            state = listState,
            userScrollEnabled = activeSelectionPageIndex == null,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            itemsIndexed(logicalPages) { index, visibleText ->
                val pageTextRange = pagination.pageTextRanges.getOrNull(index)
                val pageHeader = pagination.pageHeaders.getOrNull(index)
                val pageKind = pagination.pageKinds.getOrNull(index) ?: ReaderSourcePageKind.Body
                val pageHighlights = remember(visibleText, highlights, pageTextRange) {
                    highlights.filter { annotation ->
                        if (annotation.type != DeviceReaderAnnotationType.Highlight) {
                            false
                        } else if (annotation.sourcePage >= 0 && pageTextRange != null) {
                            annotation.sourcePage == pageTextRange.sourcePage &&
                                annotation.selectionStart < pageTextRange.end &&
                                annotation.selectionEnd > pageTextRange.start
                        } else {
                            annotation.selectedText.isNotBlank() && visibleText.contains(annotation.selectedText)
                        }
                    }
                }
                val visibleHighlightRanges = remember(visibleText, pageHighlights, pageTextRange) {
                    pageHighlights.flatMap { annotation ->
                        annotation.visibleRangesInText(visibleText, pageTextRange).map { range ->
                            ReaderVisibleHighlight(annotation, range)
                        }
                    }
                }
                var textLayoutResult by remember(index, visibleText) { mutableStateOf<TextLayoutResult?>(null) }
                var selectionStart by remember(index, visibleText) { mutableStateOf<Int?>(null) }
                var selectionEnd by remember(index, visibleText) { mutableStateOf<Int?>(null) }
                var selectionToolbarOffsetY by remember(index, visibleText) { mutableStateOf(0.dp) }
                val selectedRange = remember(visibleText, selectionStart, selectionEnd) {
                    normalizeSelectionRange(selectionStart, selectionEnd, visibleText.length)
                }
                val displayedText = remember(visibleText, selectedRange, pageHighlights, pageTextRange) {
                    buildSelectableReaderText(visibleText, selectedRange, pageHighlights, pageTextRange)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .pointerInput(visibleText, textLayoutResult, selectedRange, visibleHighlightRanges, pageTextRange) {
                            detectTapGestures(
                                onLongPress = { offset ->
                                    textLayoutResult?.let { layout ->
                                        val selectedWord = wordRangeAt(visibleText, layout.getOffsetForPosition(offset))
                                        selectionStart = selectedWord.first
                                        selectionEnd = selectedWord.last + 1
                                        activeSelectionPageIndex = index
                                        activeHighlight = null
                                        selectionToolbarOffsetY = with(density) {
                                            (layout.getLineTop(layout.getLineForOffset(selectedWord.first)) - 108.dp.toPx())
                                                .coerceAtLeast(0f)
                                                .toDp()
                                        }
                                        val selected = visibleText.substring(selectedWord.first, selectedWord.last + 1)
                                        activeSelectionText = selected
                                        activeSelection = selected.toReaderTextSelection(
                                            pageTextRange = pageTextRange,
                                            localStart = selectedWord.first,
                                            localEnd = selectedWord.last + 1
                                        )
                                        onSelectedTextChanged(selected)
                                    }
                                },
                                onTap = { offset ->
                                    if (selectedRange != null) {
                                        selectionStart = null
                                        selectionEnd = null
                                        activeSelectionText = ""
                                        activeSelection = null
                                        activeSelectionPageIndex = null
                                        activeHighlight = null
                                        onSelectedTextChanged("")
                                        return@detectTapGestures
                                    }
                                    val tappedOffset = textLayoutResult?.getOffsetForPosition(offset)
                                    val tappedHighlight = tappedOffset?.let { textOffset ->
                                        visibleHighlightRanges.firstOrNull { textOffset in it.range }
                                    }
                                    if (tappedHighlight != null) {
                                        val range = tappedHighlight.range
                                        selectionStart = range.first
                                        selectionEnd = range.last + 1
                                        activeSelectionPageIndex = index
                                        activeHighlight = tappedHighlight.annotation
                                        selectionToolbarOffsetY = with(density) {
                                            textLayoutResult?.let { layout ->
                                                (layout.getLineTop(layout.getLineForOffset(range.first)) - 108.dp.toPx())
                                                    .coerceAtLeast(0f)
                                                    .toDp()
                                            } ?: 0.dp
                                        }
                                        val selected = visibleText.substring(range.first, range.last + 1)
                                        activeSelectionText = selected
                                        activeSelection = selected.toReaderTextSelection(pageTextRange, range.first, range.last + 1)
                                        onSelectedTextChanged(selected)
                                        return@detectTapGestures
                                    }
                                    val pageWidth = size.width.toFloat().coerceAtLeast(1f)
                                    when {
                                        offset.x < pageWidth * 0.33f -> onPageChanged((index - 1).coerceAtLeast(0))
                                        offset.x > pageWidth * 0.67f -> onPageChanged((index + 1).coerceAtMost(logicalPages.lastIndex))
                                        else -> onCenterTap()
                                    }
                                }
                            )
                        }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (pageKind == ReaderSourcePageKind.Cover && file != null && pageHeader != null) {
                            FilePagePreview(
                                file = file,
                                coverText = "",
                                overrideCoverId = null,
                                modifier = Modifier
                                    .width(156.dp)
                                    .aspectRatio(0.68f)
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                        pageHeader?.let { header ->
                            Text(
                                text = header,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = Color(0xFF1A120B),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = (24f * readerPdfZoomPercentToScale(textSizePercent)).sp,
                                    lineHeight = (30f * readerPdfZoomPercentToScale(textSizePercent)).sp
                                ),
                                textAlign = if (pageKind == ReaderSourcePageKind.Cover) TextAlign.Center else TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = if (pageKind == ReaderSourcePageKind.Chapter) 18.dp else 10.dp,
                                        bottom = if (pageKind == ReaderSourcePageKind.Chapter) 18.dp else 12.dp
                                    )
                            )
                        }
                        Text(
                            text = displayedText,
                            style = textStyle,
                            onTextLayout = { textLayoutResult = it },
                            overflow = TextOverflow.Clip
                        )
                    }
                    if (selectedRange != null && activeSelectionText.isNotBlank() && activeSelectionPageIndex == index) {
                        ReaderSelectionToolbar(
                            selectedText = activeSelectionText,
                            onHighlightSelection = { _, color ->
                                activeHighlight?.let { highlight ->
                                    onHighlightUpdate(highlight, color)
                                } ?: activeSelection?.let { selection ->
                                    onHighlightSelection(selection, color)
                                }
                                selectionStart = null
                                selectionEnd = null
                                activeSelectionText = ""
                                activeSelection = null
                                activeSelectionPageIndex = null
                                activeHighlight = null
                                onSelectedTextChanged("")
                            },
                            onNoteSelection = onNoteSelection,
                            showDelete = activeHighlight != null,
                            onDeleteSelection = {
                                activeHighlight?.let(onHighlightDelete)
                                selectionStart = null
                                selectionEnd = null
                                activeSelectionText = ""
                                activeSelection = null
                                activeSelectionPageIndex = null
                                activeHighlight = null
                                onSelectedTextChanged("")
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = selectionToolbarOffsetY)
                        )
                    }
                }
            }
        }
    }
}

private fun DeviceReaderAnnotation.visibleRangesInText(
    text: String,
    pageTextRange: ReaderPageTextRange?
): List<IntRange> {
    val hasStoredRange = sourcePage >= 0 && selectionStart >= 0 && selectionEnd > selectionStart
    if (hasStoredRange && pageTextRange != null && sourcePage == pageTextRange.sourcePage) {
        val start = maxOf(selectionStart, pageTextRange.start) - pageTextRange.start
        val end = minOf(selectionEnd, pageTextRange.end) - pageTextRange.start
        return if (end > start) listOf(start until end) else emptyList()
    }
    val highlightedText = selectedText.takeIf { it.isNotBlank() } ?: return emptyList()
    val ranges = mutableListOf<IntRange>()
    var searchStart = 0
    while (searchStart < text.length) {
        val matchStart = text.indexOf(highlightedText, searchStart, ignoreCase = false)
        if (matchStart < 0) break
        val matchEnd = (matchStart + highlightedText.length).coerceAtMost(text.length)
        ranges += matchStart until matchEnd
        searchStart = matchEnd
    }
    return ranges
}

private fun String.toReaderTextSelection(
    pageTextRange: ReaderPageTextRange?,
    localStart: Int,
    localEnd: Int
): ReaderTextSelection {
    return ReaderTextSelection(
        text = this,
        sourcePage = pageTextRange?.sourcePage ?: -1,
        start = pageTextRange?.start?.plus(localStart) ?: -1,
        end = pageTextRange?.start?.plus(localEnd) ?: -1
    )
}

@Composable
fun ReaderSelectionToolbar(
    selectedText: String,
    onHighlightSelection: (String, String) -> Unit,
    onNoteSelection: (String) -> Unit,
    modifier: Modifier = Modifier,
    showDelete: Boolean = false,
    onDeleteSelection: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .width(342.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xEE202020))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.32f)), RoundedCornerShape(2.dp))
            .clickable(onClick = {})
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionToolIcon(
                icon = Icons.Filled.FormatColorFill,
                contentDescriptionRes = R.string.reader_selection_highlight,
                onClick = { onHighlightSelection(selectedText, "#EBC7E8") }
            )
            SelectionToolIcon(
                icon = Icons.Filled.FormatUnderlined,
                contentDescriptionRes = R.string.reader_selection_underline,
                onClick = { onHighlightSelection(selectedText, "underline:#EBC7E8") }
            )
            SelectionToolIcon(
                icon = Icons.Filled.FormatStrikethrough,
                contentDescriptionRes = R.string.reader_selection_strikethrough,
                onClick = { onHighlightSelection(selectedText, "strike:#EBC7E8") }
            )
            SelectionToolIcon(
                icon = Icons.Filled.Edit,
                contentDescriptionRes = R.string.reader_selection_diagonal,
                onClick = { onHighlightSelection(selectedText, "diagonal:#EBC7E8") }
            )
            SelectionToolIcon(Icons.Filled.MoreHoriz, R.string.reader_selection_more)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("#17E879", "#FF8A10", "#D6A1C9", "#B8B8B8", "#8C00FF").forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.readerHighlightColor())
                        .clickable { onHighlightSelection(selectedText, color) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showDelete) {
                IconButton(onClick = onDeleteSelection, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(R.string.reader_selection_delete),
                        tint = Color.White
                    )
                }
            }
            Text(
                text = stringResource(R.string.reader_selection_copy),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable { }
            )
            Text(
                text = stringResource(R.string.reader_selection_highlight),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable { onHighlightSelection(selectedText, "#EBC7E8") }
            )
            Text(
                text = stringResource(R.string.reader_selection_note),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable { onNoteSelection(selectedText) }
            )
            Text(
                text = stringResource(R.string.reader_selection_dict),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.reader_selection_more),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SelectionToolIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescriptionRes: Int,
    onClick: () -> Unit = {}
) {
    Icon(
        imageVector = icon,
        contentDescription = stringResource(contentDescriptionRes),
        tint = Color.White,
        modifier = Modifier
            .size(22.dp)
            .clickable(onClick = onClick)
    )
}

@Composable
fun PdfReaderVerticalPages(
    file: DeviceLibraryFile,
    pageCount: Int,
    currentPage: Int,
    pdfZoomPercent: Int = READER_PDF_ZOOM_DEFAULT,
    coverText: String = "",
    overrideCoverId: String? = null,
    onPageChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPage)

    LaunchedEffect(currentPage, pageCount) {
        val safePage = currentPage.coerceIn(0, pageCount.coerceAtLeast(1) - 1)
        if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != safePage) {
            listState.scrollToItem(safePage)
        }
    }
    LaunchedEffect(listState, pageCount) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { visiblePage ->
                onPageChanged(visiblePage.coerceIn(0, pageCount.coerceAtLeast(1) - 1))
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.background(Color.White)
    ) {
        items(pageCount) { pageIndex ->
            PdfReaderPage(
                file = file,
                pageIndex = pageIndex,
                pdfZoomPercent = pdfZoomPercent,
                coverText = coverText,
                overrideCoverId = overrideCoverId,
                compactForScroll = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
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
    compactForScroll: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BoxWithConstraints(modifier = modifier) {
        val pdfZoom = readerPdfZoomPercentToScale(pdfZoomPercent)
        val scaledPageWidth = maxWidth * pdfZoom
        val targetWidth = with(LocalDensity.current) {
            (maxWidth.roundToPx() * pdfZoom).roundToInt()
        }.coerceAtLeast(360)
        val bitmap by produceState<Bitmap?>(initialValue = null, file.uri, pageIndex, targetWidth) {
            value = withContext(Dispatchers.IO) {
                renderPdfPage(context, file.uri, pageIndex, targetWidth)
            }
        }

        Box(
            modifier = if (compactForScroll) {
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 4.dp)
            } else {
                Modifier
                    .fillMaxSize()
                    .background(Color.White)
            },
            contentAlignment = Alignment.Center
        ) {
            if (pageIndex == 0) {
                Box(
                    modifier = if (compactForScroll) {
                        Modifier
                            .requiredWidth(scaledPageWidth)
                            .aspectRatio(0.68f)
                            .background(Color(0xFF1D1A06))
                            .padding(horizontal = 42.dp, vertical = 34.dp)
                    } else {
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1D1A06))
                            .padding(horizontal = 42.dp, vertical = 34.dp)
                    },
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.68f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(36.dp),
                        color = TarnishedGold
                    )
                }
            } else {
                Box(
                    modifier = if (compactForScroll) {
                        Modifier
                            .requiredWidth(scaledPageWidth)
                            .aspectRatio(bitmap!!.width / bitmap!!.height.toFloat())
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(bitmap!!.width / bitmap!!.height.toFloat())
                            .graphicsLayer {
                                scaleX = pdfZoom
                                scaleY = pdfZoom
                                translationX = pdfPanX
                                translationY = pdfPanY
                                transformOrigin = TransformOrigin(0.5f, 0f)
                            }
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



