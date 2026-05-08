package com.example.kaishelvesapp.ui.screen.library

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.provider.OpenableColumns
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
            statusMessage = null
            val result = withContext(Dispatchers.IO) {
                convertDocumentToPdf(context, inputUri, targetUri)
            }
            isConverting = false
            statusMessage = if (result.isSuccess) {
                context.getString(R.string.pdf_converter_success)
            } else {
                result.exceptionOrNull()?.localizedMessage ?: context.getString(R.string.pdf_converter_error)
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
                            if (isConverting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(18.dp),
                                    strokeWidth = 2.dp,
                                    color = OldIvory
                                )
                            } else {
                                Text(stringResource(R.string.pdf_converter_convert), color = OldIvory)
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
    val text = extractConvertibleText(context, sourceUri)
    require(text.isNotBlank()) { context.getString(R.string.pdf_converter_empty_error) }

    context.contentResolver.openOutputStream(targetUri)?.use { output ->
        val document = PdfDocument()
        try {
            writeTextToPdf(document, text)
            document.writeTo(output)
        } finally {
            document.close()
        }
    } ?: error(context.getString(R.string.pdf_converter_error))
}

private fun writeTextToPdf(
    document: PdfDocument,
    text: String
) {
    val pageWidth = 595
    val pageHeight = 842
    val margin = 48
    val textWidth = pageWidth - margin * 2
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = 13f
    }
    val paragraphs = text.split(Regex("\\n{2,}")).map { it.trim() }.filter { it.isNotBlank() }
    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var y = margin

    fun finishPage() {
        document.finishPage(page)
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        y = margin
    }

    paragraphs.forEach { paragraph ->
        val layout = StaticLayout.Builder
            .obtain(paragraph, 0, paragraph.length, textPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(4f, 1f)
            .setIncludePad(false)
            .build()
        if (y + layout.height > pageHeight - margin && y > margin) finishPage()
        page.canvas.save()
        page.canvas.translate(margin.toFloat(), y.toFloat())
        layout.draw(page.canvas)
        page.canvas.restore()
        y += layout.height + 18
    }

    document.finishPage(page)
}

private fun extractConvertibleText(context: Context, uri: Uri): String {
    val name = displayNameForUri(context, uri).lowercase(Locale.ROOT)
    return when {
        name.endsWith(".epub") -> extractEpubText(context, uri)
        name.endsWith(".fb2") -> readUriText(context, uri).let(::stripDocumentTags)
        else -> readUriText(context, uri)
    }.trim()
}

private fun readUriText(context: Context, uri: Uri): String {
    return context.contentResolver.openInputStream(uri)?.use { input ->
        input.readBytes().decodeToString()
    }.orEmpty()
}

private fun extractEpubText(context: Context, uri: Uri): String {
    val rootFilePath = readConverterZipEntry(context, uri, "META-INF/container.xml")
        ?.decodeToString()
        ?.let(::parseConverterRootFilePath)
        ?: return ""
    val opf = readConverterZipEntry(context, uri, rootFilePath)?.decodeToString() ?: return ""
    return parseConverterEpubReadingPaths(opf)
        .map { href -> resolveConverterZipPath(rootFilePath, href) }
        .mapNotNull { path -> readConverterZipEntry(context, uri, path)?.decodeToString() }
        .joinToString("\n\n", transform = ::stripDocumentTags)
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

private fun stripDocumentTags(raw: String): String {
    return raw
        .replace(Regex("(?is)<(script|style).*?</\\1>"), " ")
        .replace(Regex("(?i)</(p|div|br|h[1-6]|li|section|chapter)>"), "\n")
        .replace(Regex("(?s)<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
        .replace(Regex("\\n\\s+"), "\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
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
