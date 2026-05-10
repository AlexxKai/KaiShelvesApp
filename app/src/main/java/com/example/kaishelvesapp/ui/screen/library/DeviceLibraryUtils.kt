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
import com.example.kaishelvesapp.data.repository.DeviceLibraryRepository
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

fun isPdf(file: DeviceLibraryFile): Boolean {
    return file.mimeType == "application/pdf" || file.name.endsWith(".pdf", ignoreCase = true)
}

fun isEpub(file: DeviceLibraryFile): Boolean {
    return file.mimeType == "application/epub+zip" || file.name.endsWith(".epub", ignoreCase = true)
}

fun isTextBook(file: DeviceLibraryFile): Boolean {
    return file.mimeType.orEmpty().startsWith("text/") ||
        file.name.endsWith(".txt", ignoreCase = true)
}

fun isFb2(file: DeviceLibraryFile): Boolean {
    return file.name.endsWith(".fb2", ignoreCase = true)
}

fun readableFileType(file: DeviceLibraryFile): String {
    return file.name.substringAfterLast('.', missingDelimiterValue = file.mimeType.orEmpty())
        .uppercase(Locale.ROOT)
        .ifBlank { file.mimeType.orEmpty() }
}

data class DeviceBookDisplayMetadata(
    val title: String = "",
    val author: String = "",
    val description: String = ""
)

data class DeviceBookUserMetadata(
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val coverText: String = "",
    val coverId: String = "",
    val favorite: Boolean = false,
    val category: String = "",
    val series: String = "",
    val tags: String = ""
)

fun renderPdfFirstPage(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            renderPdfFirstPage(descriptor)
        }
    }.getOrNull()
}

fun renderPdfFirstPage(descriptor: ParcelFileDescriptor): Bitmap? {
    PdfRenderer(descriptor).use { renderer ->
        if (renderer.pageCount == 0) return null
        renderer.openPage(0).use { page ->
            val targetWidth = 180
            val targetHeight = (targetWidth.toFloat() / page.width * page.height).toInt().coerceAtLeast(220)
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(AndroidColor.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        }
    }
}

fun getPdfPageCount(context: Context, uri: Uri): Int {
    return runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            PdfRenderer(descriptor).use { renderer -> renderer.pageCount }
        } ?: 0
    }.getOrDefault(0)
}

fun renderPdfPage(
    context: Context,
    uri: Uri,
    pageIndex: Int,
    targetWidth: Int
): Bitmap? {
    return runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                if (pageIndex !in 0 until renderer.pageCount) {
                    null
                } else {
                    renderer.openPage(pageIndex).use { page ->
                        val scale = targetWidth / page.width.toFloat()
                        val targetHeight = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(AndroidColor.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        }
    }.getOrNull()
}

fun readDeviceBookCurrentPage(
    context: Context,
    file: DeviceLibraryFile,
    pageCount: Int
): Int {
    if (pageCount <= 0) return 0
    return deviceLibraryProgressPreferences(context)
        .getInt("${deviceBookProgressKey(file)}_page", 0)
        .coerceIn(0, pageCount - 1)
}

fun readDeviceBookProgressPercent(
    context: Context,
    file: DeviceLibraryFile
): Int {
    return deviceLibraryProgressPreferences(context)
        .getInt("${deviceBookProgressKey(file)}_percent", 0)
        .coerceIn(0, 100)
}

fun saveDeviceBookReadingProgress(
    context: Context,
    file: DeviceLibraryFile,
    currentPage: Int,
    pageCount: Int
) {
    val safePageCount = pageCount.coerceAtLeast(1)
    val safePage = currentPage.coerceIn(0, safePageCount - 1)
    deviceLibraryProgressPreferences(context)
        .edit()
        .putInt("${deviceBookProgressKey(file)}_page", safePage)
        .putInt("${deviceBookProgressKey(file)}_page_count", safePageCount)
        .putInt("${deviceBookProgressKey(file)}_percent", readingProgressForPage(safePage, safePageCount))
        .apply()
    DeviceLibraryRepository(context).saveProgress(file, safePage, safePageCount)
}

fun readingProgressForPage(
    currentPage: Int,
    pageCount: Int
): Int {
    if (pageCount <= 0) return 0
    return (((currentPage.coerceIn(0, pageCount - 1) + 1) * 100f) / pageCount)
        .toInt()
        .coerceIn(0, 100)
}

fun deviceLibraryProgressPreferences(context: Context) =
    context.getSharedPreferences("device_library_reading_progress", Context.MODE_PRIVATE)

fun deviceBookProgressKey(file: DeviceLibraryFile): String {
    return URLEncoder.encode(file.uri.toString(), "UTF-8")
}

fun extractEpubCover(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        val rootFilePath = readZipEntry(context, uri, "META-INF/container.xml")
            ?.decodeToString()
            ?.let(::parseRootFilePath)
            ?: return@runCatching null
        val opf = readZipEntry(context, uri, rootFilePath)?.decodeToString()
            ?: return@runCatching null
        val coverHref = parseCoverHref(opf) ?: return@runCatching null
        val coverPath = resolveZipPath(rootFilePath, coverHref)
        readZipEntry(context, uri, coverPath)?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }.getOrNull()
}

fun extractEpubReadingText(context: Context, uri: Uri): String {
    return runCatching {
        val rootFilePath = readZipEntry(context, uri, "META-INF/container.xml")
            ?.decodeToString()
            ?.let(::parseRootFilePath)
            ?: return@runCatching ""
        val opf = readZipEntry(context, uri, rootFilePath)?.decodeToString()
            ?: return@runCatching ""
        parseEpubReadingPaths(opf)
            .map { href -> resolveZipPath(rootFilePath, href) }
            .mapNotNull { path -> readZipEntry(context, uri, path)?.decodeToString() }
            .joinToString("\n\n") { html -> stripXmlToText(html) }
            .trim()
    }.getOrDefault("")
}

fun extractEpubDisplayMetadata(context: Context, uri: Uri): DeviceBookDisplayMetadata? {
    return runCatching {
        val rootFilePath = readZipEntry(context, uri, "META-INF/container.xml")
            ?.decodeToString()
            ?.let(::parseRootFilePath)
            ?: return@runCatching null
        val opf = readZipEntry(context, uri, rootFilePath)?.decodeToString()
            ?: return@runCatching null
        parseEpubDisplayMetadata(opf)
    }.getOrNull()
}

fun readZipEntry(context: Context, uri: Uri, targetPath: String): ByteArray? {
    context.contentResolver.openInputStream(uri)?.use { input ->
        ZipInputStream(input).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name == targetPath) {
                    return zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
    return null
}

fun parseRootFilePath(containerXml: String): String? {
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

fun parseCoverHref(opfXml: String): String? {
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    parser.setInput(opfXml.reader())
    val manifestItems = linkedMapOf<String, String>()
    var coverId: String? = null
    var fallbackCover: String? = null
    var event = parser.eventType

    while (event != XmlPullParser.END_DOCUMENT) {
        if (event == XmlPullParser.START_TAG) {
            when (parser.name) {
                "meta" -> {
                    if (parser.getAttributeValue(null, "name") == "cover") {
                        coverId = parser.getAttributeValue(null, "content")
                    }
                }
                "item" -> {
                    val id = parser.getAttributeValue(null, "id")
                    val href = parser.getAttributeValue(null, "href")
                    val mediaType = parser.getAttributeValue(null, "media-type").orEmpty()
                    val properties = parser.getAttributeValue(null, "properties").orEmpty()
                    if (!id.isNullOrBlank() && !href.isNullOrBlank()) {
                        manifestItems[id] = href
                        if (properties.split(' ').contains("cover-image")) {
                            return href
                        }
                        if (fallbackCover == null && mediaType.startsWith("image/") && id.contains("cover", ignoreCase = true)) {
                            fallbackCover = href
                        }
                    }
                }
            }
        }
        event = parser.next()
    }

    return coverId?.let(manifestItems::get) ?: fallbackCover
}

fun parseEpubReadingPaths(opfXml: String): List<String> {
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    parser.setInput(opfXml.reader())
    val manifestItems = linkedMapOf<String, String>()
    val readableManifestItems = linkedSetOf<String>()
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
                        if (mediaType == "application/xhtml+xml" || mediaType == "text/html") {
                            readableManifestItems.add(id)
                        }
                    }
                }
                "itemref" -> {
                    val idRef = parser.getAttributeValue(null, "idref").orEmpty()
                    if (idRef.isNotBlank()) spineIds.add(idRef)
                }
            }
        }
        event = parser.next()
    }

    val spinePaths = spineIds.mapNotNull { id -> manifestItems[id] }
    return spinePaths.ifEmpty { readableManifestItems.mapNotNull { id -> manifestItems[id] } }
}

fun parseEpubDisplayMetadata(opfXml: String): DeviceBookDisplayMetadata {
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    parser.setInput(opfXml.reader())
    var title = ""
    var author = ""
    var description = ""
    var readingTag: String? = null
    var event = parser.eventType

    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> {
                val name = parser.name.substringAfter(':')
                if (name == "title" || name == "creator" || name == "description") {
                    readingTag = name
                }
            }
            XmlPullParser.TEXT -> {
                val text = parser.text.orEmpty().trim()
                if (text.isNotBlank()) {
                    when (readingTag) {
                        "title" -> if (title.isBlank()) title = text
                        "creator" -> if (author.isBlank()) author = text
                        "description" -> if (description.isBlank()) description = text
                    }
                }
            }
            XmlPullParser.END_TAG -> readingTag = null
        }
        event = parser.next()
    }

    return DeviceBookDisplayMetadata(
        title = title,
        author = author,
        description = description
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    )
}

fun resolveZipPath(rootFilePath: String, href: String): String {
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

fun readPlainTextFile(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().decodeToString()
        }?.trim()
    }.getOrNull()
}

fun stripXmlToText(raw: String): String {
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

