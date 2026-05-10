package com.example.kaishelvesapp.ui.screen.library

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kaishelvesapp.R
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
fun ReflowTextReaderPage(
    sourcePages: List<String>,
    pageIndex: Int,
    textSizePercent: Int,
    onPageCountChanged: (Int) -> Unit,
    onSourcePageStartPagesChanged: (List<Int>) -> Unit = {},
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
                    fontSizePx = fontSizePx,
                    lineHeightPx = lineHeightPx,
                    maxWidthPx = contentWidthPx,
                    maxHeightPx = (contentHeightPx - with(density) { 14.dp.roundToPx() }).coerceAtLeast(1)
                )
            }
        }
        val logicalPages = pagination.pages
        val safePageIndex = pageIndex.coerceIn(0, logicalPages.lastIndex.coerceAtLeast(0))

        LaunchedEffect(logicalPages.size) {
            onPageCountChanged(logicalPages.size.coerceAtLeast(1))
        }
        LaunchedEffect(pagination.sourceStartPages) {
            onSourcePageStartPagesChanged(pagination.sourceStartPages)
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

        // Cada página lógica ya está medida para caber completa en el viewport, sin scroll interno.
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



