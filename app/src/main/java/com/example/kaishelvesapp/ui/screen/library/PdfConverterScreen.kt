package com.example.kaishelvesapp.ui.screen.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.kaishelvesapp.R
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
    var isConverting by remember { mutableStateOf(false) }
    var activeOutputFormat by remember { mutableStateOf<ConverterOutputFormat?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val sourceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            sourceUri = uri
            sourceName = displayNameForUri(context, uri)
            statusMessage = null
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
            val result = withContext(Dispatchers.IO) {
                convertDocumentToPdf(context, inputUri, targetUri)
            }
            isConverting = false
            activeOutputFormat = null
            statusMessage = if (result.isSuccess) {
                context.getString(R.string.pdf_converter_success)
            } else {
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
            val result = withContext(Dispatchers.IO) {
                convertDocumentToEpub(context, inputUri, targetUri)
            }
            isConverting = false
            activeOutputFormat = null
            statusMessage = if (result.isSuccess) {
                context.getString(R.string.epub_converter_success)
            } else {
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
                                    text = sourceName,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 10.dp),
                                    color = OldIvory,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Button(
                            onClick = {
                                sourceLauncher.launch(
                                    arrayOf(
                                        "text/plain",
                                        "application/pdf",
                                        "application/epub+zip",
                                        "application/xml",
                                        "*/*"
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TarnishedGold),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(stringResource(R.string.pdf_converter_choose_file), color = Obsidian)
                        }

                        Button(
                            enabled = sourceUri != null && !isConverting,
                            onClick = {
                                val outputName = sourceName.substringBeforeLast('.', missingDelimiterValue = sourceName)
                                    .ifBlank { "kai-shelves" } + ".pdf"
                                outputLauncher.launch(outputName)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F9FE3)),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            if (activeOutputFormat == ConverterOutputFormat.Pdf) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(18.dp),
                                    strokeWidth = 2.dp,
                                    color = OldIvory
                                )
                            } else {
                                Text(stringResource(R.string.pdf_converter_convert), color = OldIvory)
                            }
                        }

                        Button(
                            enabled = sourceUri != null && !isConverting,
                            onClick = {
                                val outputName = sourceName.substringBeforeLast('.', missingDelimiterValue = sourceName)
                                    .ifBlank { "kai-shelves" } + ".epub"
                                epubOutputLauncher.launch(outputName)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TarnishedGold.copy(alpha = 0.88f)),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            if (activeOutputFormat == ConverterOutputFormat.Epub) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Obsidian
                                )
                            } else {
                                Text(stringResource(R.string.epub_converter_convert), color = Obsidian)
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
            }
        }
    }
}

private fun convertDocumentToPdf(
    context: Context,
    sourceUri: Uri,
    targetUri: Uri
): Result<Unit> = runCatching {
    val blocks = extractConvertibleBlocks(context, sourceUri)
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

private fun writeBlocksToPdf(
    document: PdfDocument,
    blocks: List<PdfTextBlock>
) {
    val pageWidth = 595
    val pageHeight = 842
    val margin = 48
    val textWidth = pageWidth - margin * 2
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = 13f
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
            .setLineSpacing(4f, 1f)
            .setIncludePad(false)
            .build()

        var firstLine = 0
        while (firstLine < layout.lineCount) {
            val remainingHeight = pageHeight - margin - y
            if (remainingHeight <= textPaint.textSize && y > margin) {
                finishPage()
                continue
            }

            var lastLineExclusive = firstLine
            while (
                lastLineExclusive < layout.lineCount &&
                layout.getLineBottom(lastLineExclusive) - layout.getLineTop(firstLine) <= remainingHeight
            ) {
                lastLineExclusive++
            }

            if (lastLineExclusive == firstLine) {
                finishPage()
                continue
            }

            val clipTop = layout.getLineTop(firstLine)
            val clipBottom = layout.getLineBottom(lastLineExclusive - 1)
            page.canvas.save()
            page.canvas.translate(margin.toFloat(), y.toFloat() - clipTop)
            page.canvas.clipRect(0, clipTop, textWidth, clipBottom)
            layout.draw(page.canvas)
            page.canvas.restore()

            y += clipBottom - clipTop
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
    targetUri: Uri
): Result<Unit> = runCatching {
    val sourceName = displayNameForUri(context, sourceUri)
    if (sourceName.endsWith(".epub", ignoreCase = true)) {
        copyUriContent(context, sourceUri, targetUri)
        return@runCatching
    }

    val book = buildEpubBook(context, sourceUri, sourceName)
    require(book.chapters.any { it.xhtmlBody.isNotBlank() } || book.assets.isNotEmpty()) {
        context.getString(R.string.pdf_converter_empty_error)
    }

    context.contentResolver.openOutputStream(targetUri)?.use { output ->
        ZipOutputStream(output).use { zip ->
            writeEpubBook(zip, book)
        }
    } ?: error(context.getString(R.string.epub_converter_error))
}

private fun copyUriContent(context: Context, sourceUri: Uri, targetUri: Uri) {
    val input = context.contentResolver.openInputStream(sourceUri)
        ?: error(context.getString(R.string.epub_converter_error))
    val output = context.contentResolver.openOutputStream(targetUri)
        ?: error(context.getString(R.string.epub_converter_error))
    input.use { source ->
        output.use { target -> source.copyTo(target) }
    }
}

private fun buildEpubBook(context: Context, uri: Uri, sourceName: String): ConverterEpubBook {
    val lowerName = sourceName.lowercase(Locale.ROOT)
    val title = sourceName.substringBeforeLast('.', missingDelimiterValue = sourceName)
        .ifBlank { "Kai Shelves" }
    return when {
        lowerName.endsWith(".pdf") -> pdfToEpubBook(context, uri, title)
        lowerName.endsWith(".fb2") -> fb2ToEpubBook(readUriText(context, uri), title)
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

private fun pdfToEpubBook(context: Context, uri: Uri, title: String): ConverterEpubBook {
    val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
        ?: error(context.getString(R.string.epub_converter_error))
    val assets = mutableListOf<ConverterEpubAsset>()
    val chapters = mutableListOf<ConverterEpubChapter>()

    // Renderiza cada pagina como imagen para conservar maquetacion, imagenes y texto visible del PDF.
    descriptor.use { pfd: ParcelFileDescriptor ->
        PdfRenderer(pfd).use { renderer ->
            for (index in 0 until renderer.pageCount) {
                renderer.openPage(index).use { page ->
                    val scale = 2
                    val bitmap = Bitmap.createBitmap(
                        (page.width * scale).coerceAtLeast(1),
                        (page.height * scale).coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val imageBytes = bitmap.toPngBytes()
                    bitmap.recycle()
                    val fileName = "page-${index + 1}.png"
                    assets += ConverterEpubAsset("images/$fileName", "image/png", imageBytes)
                    chapters += ConverterEpubChapter(
                        id = "page-${index + 1}",
                        title = "Pagina ${index + 1}",
                        xhtmlBody = "<figure class=\"pdf-page\"><img src=\"../images/$fileName\" alt=\"Pagina ${index + 1}\" /></figure>"
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
    return ConverterEpubMetadata(title, author)
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
""".trimIndent()

private fun ConverterEpubChapter.toXhtml(book: ConverterEpubBook): String = """
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE html>
    <html xmlns="http://www.w3.org/1999/xhtml" lang="es">
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
        <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" lang="es">
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
            <dc:language>es</dc:language>
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

private enum class ConverterOutputFormat {
    Pdf,
    Epub
}

private data class ConverterEpubBook(
    val title: String,
    val author: String = "",
    val identifier: String = "urn:uuid:${UUID.randomUUID()}",
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
    val author: String
)

private fun extractConvertibleBlocks(context: Context, uri: Uri): List<PdfTextBlock> {
    val name = displayNameForUri(context, uri).lowercase(Locale.ROOT)
    return when {
        name.endsWith(".epub") -> extractEpubBlocks(context, uri)
        name.endsWith(".fb2") -> listOf(PdfTextBlock(fb2ToSpanned(readUriText(context, uri))))
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
