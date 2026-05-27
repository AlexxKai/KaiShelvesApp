package com.example.kaishelvesapp.ui.screen.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.kaishelvesapp.data.repository.DeviceLibraryFile
import com.example.kaishelvesapp.data.repository.DeviceLibraryRepository
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import android.graphics.Color as AndroidColor

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

data class EpubChapter(
    val title: String,
    val text: String,
    val href: String,
    val level: Int = 0,
    val parentTitle: String? = null
)

data class EpubWebPage(
    val title: String,
    val href: String,
    val html: String
)

data class EpubReaderContent(
    val metadata: DeviceBookDisplayMetadata = DeviceBookDisplayMetadata(),
    val chapters: List<EpubChapter> = emptyList(),
    val webPages: List<EpubWebPage> = emptyList(),
    val resources: Map<String, ByteArray> = emptyMap(),
    val imagePaths: List<String> = emptyList(),
    val coverPath: String? = null
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

fun readDeviceBookStoredPageCount(
    context: Context,
    file: DeviceLibraryFile
): Int {
    return deviceLibraryProgressPreferences(context)
        .getInt("${deviceBookProgressKey(file)}_page_count", 1)
        .coerceAtLeast(1)
}

fun readDeviceBookProgressPercent(
    context: Context,
    file: DeviceLibraryFile
): Int {
    return deviceLibraryProgressPreferences(context)
        .getInt("${deviceBookProgressKey(file)}_percent", 0)
        .coerceIn(0, 100)
}

fun readingStatusForProgress(progress: Int): DeviceLibraryReadingStatus {
    return when (progress.coerceIn(0, 100)) {
        0 -> DeviceLibraryReadingStatus.Unread
        100 -> DeviceLibraryReadingStatus.Finished
        else -> DeviceLibraryReadingStatus.Reading
    }
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

fun extractEpubReaderContent(context: Context, uri: Uri): EpubReaderContent? {
    return runCatching {
        val entries = readZipEntries(context, uri)
        val rootFilePath = entries["META-INF/container.xml"]
            ?.decodeToString()
            ?.let(::parseRootFilePath)
            ?: return@runCatching null
        val opf = entries[rootFilePath]?.decodeToString()
            ?: return@runCatching null
        val packageData = parseEpubPackage(opf)
        val spinePaths = packageData.readingHrefs.map { href -> resolveZipPath(rootFilePath, decodeEpubHref(href)) }
        val navPath = packageData.navHref?.let { href -> resolveZipPath(rootFilePath, decodeEpubHref(href)) }
        val tocEntries = buildEpubTocEntries(
            navPath = navPath,
            navHtml = navPath?.let { entries[it]?.decodeToString() },
            ncxPath = packageData.ncxHref?.let { href -> resolveZipPath(rootFilePath, decodeEpubHref(href)) },
            ncxXml = packageData.ncxHref
                ?.let { href -> resolveZipPath(rootFilePath, decodeEpubHref(href)) }
                ?.let { path -> entries[path]?.decodeToString() }
        )
        val tocTitlesByPath = tocEntries.associate { it.href.substringBefore('#') to it.title }
        val tocParentsByPath = tocEntries.parentTitlesByPath()
        val chapters = spinePaths.mapIndexedNotNull { index, path ->
            val html = entries[path]?.decodeToString() ?: return@mapIndexedNotNull null
            val text = stripXmlToText(html)
            if (text.isBlank()) return@mapIndexedNotNull null
            val tocEntry = tocEntries.firstOrNull { it.href.substringBefore('#') == path }
            EpubChapter(
                title = tocTitlesByPath[path]
                    ?: extractHtmlTitle(html)
                    ?: "",
                text = text,
                href = path,
                level = tocEntry?.level ?: 0,
                parentTitle = tocParentsByPath[path]
            )
        }
        val webPages = spinePaths.mapIndexedNotNull { index, path ->
            val html = entries[path]?.decodeToString() ?: return@mapIndexedNotNull null
            EpubWebPage(
                title = tocTitlesByPath[path]
                    ?: extractHtmlTitle(html)
                    ?: packageData.metadata.title.takeIf { index == 0 && it.isNotBlank() }
                    ?: "",
                href = path,
                html = html.withKaiReaderViewport()
            )
        }
        val coverPath = packageData.coverHref?.let { href -> resolveZipPath(rootFilePath, decodeEpubHref(href)) }
        val imagePaths = packageData.imageHrefs
            .map { href -> resolveZipPath(rootFilePath, decodeEpubHref(href)) }
            .distinct()

        EpubReaderContent(
            metadata = packageData.metadata,
            chapters = chapters,
            webPages = webPages,
            resources = entries,
            imagePaths = imagePaths,
            coverPath = coverPath
        )
    }.getOrNull()
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

fun readZipEntries(context: Context, uri: Uri): Map<String, ByteArray> {
    val entries = linkedMapOf<String, ByteArray>()
    context.contentResolver.openInputStream(uri)?.use { input ->
        ZipInputStream(input).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
    return entries
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

data class EpubPackageData(
    val metadata: DeviceBookDisplayMetadata,
    val readingHrefs: List<String>,
    val imageHrefs: List<String>,
    val coverHref: String?,
    val navHref: String?,
    val ncxHref: String?
)

private data class EpubTocEntry(
    val title: String,
    val href: String,
    val level: Int = 0
)

fun parseEpubPackage(opfXml: String): EpubPackageData {
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    parser.setInput(opfXml.reader())
    val metadata = parseEpubDisplayMetadata(opfXml)
    val manifestItems = linkedMapOf<String, String>()
    val itemMediaTypes = linkedMapOf<String, String>()
    val readableManifestItems = linkedSetOf<String>()
    val imageHrefs = linkedSetOf<String>()
    val spineIds = mutableListOf<String>()
    var coverId: String? = null
    var coverHref: String? = null
    var fallbackCoverHref: String? = null
    var navHref: String? = null
    var ncxId: String? = null
    var event = parser.eventType

    while (event != XmlPullParser.END_DOCUMENT) {
        if (event == XmlPullParser.START_TAG) {
            when (parser.name.substringAfter(':')) {
                "meta" -> {
                    if (parser.getAttributeValue(null, "name") == "cover") {
                        coverId = parser.getAttributeValue(null, "content")
                    }
                }
                "spine" -> {
                    ncxId = parser.getAttributeValue(null, "toc")
                }
                "item" -> {
                    val id = parser.getAttributeValue(null, "id").orEmpty()
                    val href = parser.getAttributeValue(null, "href").orEmpty()
                    val mediaType = parser.getAttributeValue(null, "media-type").orEmpty()
                    val properties = parser.getAttributeValue(null, "properties").orEmpty()
                    if (id.isNotBlank() && href.isNotBlank()) {
                        manifestItems[id] = href
                        itemMediaTypes[id] = mediaType
                        if (mediaType == "application/xhtml+xml" || mediaType == "text/html") {
                            readableManifestItems.add(id)
                        }
                        if (mediaType.startsWith("image/")) {
                            imageHrefs.add(href)
                            if (fallbackCoverHref == null && id.contains("cover", ignoreCase = true)) {
                                fallbackCoverHref = href
                            }
                        }
                        if (properties.split(' ').contains("cover-image")) {
                            coverHref = href
                        }
                        if (properties.split(' ').contains("nav")) {
                            navHref = href
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

    val readingHrefs = spineIds.mapNotNull { id -> manifestItems[id] }
        .ifEmpty { readableManifestItems.mapNotNull { id -> manifestItems[id] } }
    val ncxHref = ncxId?.let(manifestItems::get)
        ?: manifestItems.entries.firstOrNull { (_, href) -> href.endsWith(".ncx", ignoreCase = true) }?.value
        ?: itemMediaTypes.entries.firstOrNull { (_, mediaType) -> mediaType == "application/x-dtbncx+xml" }
            ?.key
            ?.let(manifestItems::get)

    return EpubPackageData(
        metadata = metadata,
        readingHrefs = readingHrefs,
        imageHrefs = imageHrefs.toList(),
        coverHref = coverHref ?: coverId?.let(manifestItems::get) ?: fallbackCoverHref,
        navHref = navHref,
        ncxHref = ncxHref
    )
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
        description = stripXmlToText(description)
            .replace(Regex("\\s+"), " ")
            .trim()
    )
}

private fun buildEpubTocEntries(
    navPath: String?,
    navHtml: String?,
    ncxPath: String?,
    ncxXml: String?
): List<EpubTocEntry> {
    val navEntries = navPath
        ?.takeIf { !navHtml.isNullOrBlank() }
        ?.let { path -> parseEpubNavEntries(path, navHtml.orEmpty()) }
        .orEmpty()
    if (navEntries.isNotEmpty()) return navEntries

    val ncxEntries = ncxPath
        ?.takeIf { !ncxXml.isNullOrBlank() }
        ?.let { path -> parseEpubNcxEntries(path, ncxXml.orEmpty()) }
        .orEmpty()
    if (ncxEntries.isNotEmpty()) return ncxEntries

    return emptyList()
}

private fun parseEpubNavEntries(navPath: String, navHtml: String): List<EpubTocEntry> {
    val linkRegex = Regex("""(?is)<a\b[^>]*href\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>""")
    return linkRegex.findAll(navHtml)
        .mapNotNull { match ->
            val href = match.groupValues[1].substringBefore('#')
            val title = stripXmlToText(match.groupValues[2]).ifBlank { return@mapNotNull null }
            val resolvedHref = resolveZipPath(navPath, decodeEpubHref(href))
            EpubTocEntry(title = title, href = resolvedHref, level = navLevelBefore(navHtml, match.range.first))
        }
        .distinctBy { it.href to it.level }
        .toList()
}

private fun parseEpubNcxEntries(ncxPath: String, ncxXml: String): List<EpubTocEntry> {
    val navPointRegex = Regex("""(?is)<navPoint\b.*?</navPoint>""")
    val textRegex = Regex("""(?is)<text\b[^>]*>(.*?)</text>""")
    val srcRegex = Regex("""(?is)<content\b[^>]*src\s*=\s*["']([^"']+)["']""")
    return navPointRegex.findAll(ncxXml)
        .mapNotNull { match ->
            val block = match.value
            val title = textRegex.find(block)
                ?.groupValues
                ?.getOrNull(1)
                ?.let(::stripXmlToText)
                ?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val href = srcRegex.find(block)
                ?.groupValues
                ?.getOrNull(1)
                ?.substringBefore('#')
                ?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            EpubTocEntry(title = title, href = resolveZipPath(ncxPath, decodeEpubHref(href)))
        }
        .distinctBy { it.href }
        .toList()
}

private fun navLevelBefore(navHtml: String, index: Int): Int {
    val prefix = navHtml.take(index)
    val openedLists = Regex("""(?is)<(ol|ul)\b""").findAll(prefix).count()
    val closedLists = Regex("""(?is)</(ol|ul)>""").findAll(prefix).count()
    return (openedLists - closedLists - 1).coerceAtLeast(0)
}

private fun List<EpubTocEntry>.parentTitlesByPath(): Map<String, String> {
    val parentsByLevel = linkedMapOf<Int, String>()
    val parentsByPath = linkedMapOf<String, String>()
    forEach { entry ->
        parentsByLevel.keys.filter { it >= entry.level }.forEach(parentsByLevel::remove)
        if (entry.level == 0) {
            parentsByLevel[entry.level] = entry.title
        } else {
            val parentTitle = parentsByLevel.entries.lastOrNull { it.key < entry.level }?.value
            if (!parentTitle.isNullOrBlank()) {
                parentsByPath[entry.href.substringBefore('#')] = parentTitle
            }
            parentsByLevel[entry.level] = entry.title
        }
    }
    return parentsByPath
}

fun extractHtmlTitle(html: String): String? {
    val titleTags = listOf("h1", "h2", "h3", "title")
    return titleTags.firstNotNullOfOrNull { tag ->
        Regex("(?is)<$tag\\b[^>]*>(.*?)</$tag>")
            .find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::stripXmlToText)
            ?.takeIf { it.isNotBlank() }
    }
}

private fun decodeEpubHref(href: String): String {
    return runCatching { URLDecoder.decode(href, "UTF-8") }.getOrDefault(href)
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
        .replace(Regex("(?is)<img\\b[^>]*>")) { match -> imageTagText(match.value) }
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)<li\\b[^>]*>"), "\n• ")
        .replace(Regex("(?i)</(p|div|h[1-6]|li|section|chapter|blockquote|tr)>"), "\n")
        .replace(Regex("(?i)<(p|div|h[1-6]|li|section|chapter|blockquote|tr)\\b[^>]*>"), "\n")
        .replace(Regex("(?s)<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&ndash;", "–")
        .replace("&mdash;", "—")
        .replace("&hellip;", "…")
        .replace("&laquo;", "«")
        .replace("&raquo;", "»")
        .replace("&ldquo;", "“")
        .replace("&rdquo;", "”")
        .replace("&lsquo;", "‘")
        .replace("&rsquo;", "’")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace(Regex("""&#(\d+);""")) { match ->
            match.groupValues[1].toIntOrNull()?.let { code -> code.toChar().toString() } ?: match.value
        }
        .replace(Regex("""&#x([0-9a-fA-F]+);""")) { match ->
            match.groupValues[1].toIntOrNull(16)?.let { code -> code.toChar().toString() } ?: match.value
        }
        .replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
        .replace(Regex(" *\\n *"), "\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

private fun imageTagText(tag: String): String {
    val alt = tag.attributeValue("alt").takeIf { it.isNotBlank() }
    val title = tag.attributeValue("title").takeIf { it.isNotBlank() }
    val srcName = tag.attributeValue("src")
        .substringBefore('#')
        .substringAfterLast('/')
        .takeIf { it.isNotBlank() }
    return "\n${alt ?: title ?: srcName.orEmpty()}\n"
}

private fun String.withKaiReaderViewport(): String {
    val viewport = """<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes" />"""
    val readerCss = """
        <style type="text/css">
        html, body { background:#202006; color:#d4d0b8; }
        body { padding: 0.4em 0.75em 2.2em; }
        img { max-width:100%; height:auto; }
        .cubierta img { width:100%; height:auto; max-height:88vh; object-fit:contain; }
        </style>
    """.trimIndent()
    return if (contains("</head>", ignoreCase = true)) {
        replace(Regex("(?i)</head>"), "$viewport\n$readerCss\n</head>")
    } else {
        "$viewport\n$readerCss\n$this"
    }
}

private fun String.attributeValue(name: String): String {
    val regex = Regex("""(?is)\b$name\s*=\s*["']([^"']*)["']""")
    return regex.find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?.let(::stripXmlToText)
        .orEmpty()
}

