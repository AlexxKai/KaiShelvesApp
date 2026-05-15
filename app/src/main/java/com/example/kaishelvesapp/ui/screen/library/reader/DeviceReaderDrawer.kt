package com.example.kaishelvesapp.ui.screen.library

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.DeviceReaderAnnotation
import com.example.kaishelvesapp.data.repository.DeviceLibraryFile
import com.example.kaishelvesapp.ui.theme.TarnishedGold

enum class PdfReaderDrawerTab {
    Chapters,
    Bookmarks,
    Images
}

@Composable
fun ReflowReaderNavigationDrawer(
    file: DeviceLibraryFile,
    document: ReaderEngineDocument,
    currentPage: Int,
    pageCount: Int,
    annotations: List<DeviceReaderAnnotation>,
    sourcePageStartPages: List<Int>,
    onPageSelected: (Int) -> Unit,
    onAnnotationEdit: (DeviceReaderAnnotation) -> Unit,
    onAnnotationDelete: (DeviceReaderAnnotation) -> Unit,
    onAddBookmark: () -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(PdfReaderDrawerTab.Chapters) }
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
                text = stringResource(R.string.reader_tab_chapters),
                selected = selectedTab == PdfReaderDrawerTab.Chapters,
                onClick = { selectedTab = PdfReaderDrawerTab.Chapters }
            )
            PdfDrawerTabButton(
                text = stringResource(R.string.reader_tab_bookmarks),
                selected = selectedTab == PdfReaderDrawerTab.Bookmarks,
                onClick = { selectedTab = PdfReaderDrawerTab.Bookmarks }
            )
            IconButton(onClick = { selectedTab = PdfReaderDrawerTab.Images }) {
                Icon(
                    imageVector = Icons.Filled.ImageIcon,
                    contentDescription = stringResource(R.string.reader_tab_images),
                    tint = if (selectedTab == PdfReaderDrawerTab.Images) Color.White else Color.White.copy(alpha = 0.72f)
                )
            }
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
            PdfReaderDrawerTab.Chapters -> ReflowReaderChaptersTab(
                document = document,
                sourcePageStartPages = sourcePageStartPages,
                onPageSelected = onPageSelected,
                modifier = Modifier.weight(1f)
            )
            PdfReaderDrawerTab.Bookmarks -> PdfReaderBookmarksTab(
                currentPage = currentPage,
                pageCount = pageCount,
                annotations = annotations,
                onPageSelected = onPageSelected,
                onAnnotationEdit = onAnnotationEdit,
                onAnnotationDelete = onAnnotationDelete,
                onAddBookmark = onAddBookmark,
                modifier = Modifier.weight(1f)
            )
            PdfReaderDrawerTab.Images -> ReflowReaderImagesTab(
                file = file,
                document = document,
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
                text = stringResource(R.string.reader_page_progress, currentPage + 1, pageCount),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.reader_settings),
                tint = Color(0xFFEBC7E8)
            )
        }
    }
}

@Composable
fun PdfReaderNavigationDrawer(
    file: DeviceLibraryFile,
    currentPage: Int,
    pageCount: Int,
    annotations: List<DeviceReaderAnnotation>,
    onPageSelected: (Int) -> Unit,
    onAnnotationEdit: (DeviceReaderAnnotation) -> Unit,
    onAnnotationDelete: (DeviceReaderAnnotation) -> Unit,
    onAddBookmark: () -> Unit,
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
                text = stringResource(R.string.reader_tab_chapters),
                selected = selectedTab == PdfReaderDrawerTab.Chapters,
                onClick = { selectedTab = PdfReaderDrawerTab.Chapters }
            )
            PdfDrawerTabButton(
                text = stringResource(R.string.reader_tab_bookmarks),
                selected = selectedTab == PdfReaderDrawerTab.Bookmarks,
                onClick = { selectedTab = PdfReaderDrawerTab.Bookmarks }
            )
            IconButton(onClick = { selectedTab = PdfReaderDrawerTab.Images }) {
                Icon(
                    imageVector = Icons.Filled.ImageIcon,
                    contentDescription = stringResource(R.string.reader_tab_images),
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
                currentPage = currentPage,
                pageCount = pageCount,
                annotations = annotations,
                onPageSelected = onPageSelected,
                onAnnotationEdit = onAnnotationEdit,
                onAnnotationDelete = onAnnotationDelete,
                onAddBookmark = onAddBookmark,
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
                text = stringResource(R.string.reader_page_progress, currentPage + 1, pageCount),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.reader_settings),
                tint = Color(0xFFEBC7E8)
            )
        }
    }
}

@Composable
fun PdfDrawerTabButton(
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
fun PdfReaderChaptersTab(
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
                text = stringResource(R.string.reader_cover),
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
                text = stringResource(R.string.reader_no_pdf_chapters),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.padding(start = 18.dp, top = 12.dp)
            )
        }
    }
}

@Composable
fun ReflowReaderChaptersTab(
    document: ReaderEngineDocument,
    sourcePageStartPages: List<Int>,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedGroups by remember(document.chapters) { mutableStateOf(emptySet<String>()) }
    val chapterGroups = remember(document.chapters) { document.chapters.toReaderChapterGroups() }
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.reader_cover),
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
                text = document.title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        if (document.chapters.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.reader_no_chapters),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.padding(start = 18.dp, top = 12.dp)
                )
            }
        } else if (chapterGroups.isEmpty()) {
            items(document.chapters) { chapter ->
                ReflowChapterRow(
                    chapter = chapter,
                    sourcePageStartPages = sourcePageStartPages,
                    onPageSelected = onPageSelected
                )
            }
        } else {
            chapterGroups.forEach { group ->
                if (group.title == null) {
                    items(group.chapters) { chapter ->
                        ReflowChapterRow(
                            chapter = chapter,
                            sourcePageStartPages = sourcePageStartPages,
                            onPageSelected = onPageSelected
                        )
                    }
                } else {
                    item(key = "group_${group.title}") {
                        val expanded = group.title in expandedGroups
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedGroups = if (expanded) {
                                        expandedGroups - group.title
                                    } else {
                                        expandedGroups + group.title
                                    }
                                }
                                .padding(top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (expanded) {
                                    Icons.Filled.KeyboardArrowDown
                                } else {
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = group.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (group.title in expandedGroups) {
                        items(group.chapters) { chapter ->
                            ReflowChapterRow(
                                chapter = chapter,
                                sourcePageStartPages = sourcePageStartPages,
                                onPageSelected = onPageSelected,
                                indent = 34.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ReaderChapterGroup(
    val title: String?,
    val chapters: List<ReaderChapter>
)

private fun List<ReaderChapter>.toReaderChapterGroups(): List<ReaderChapterGroup> {
    if (none { !it.groupTitle.isNullOrBlank() }) return emptyList()
    val groups = mutableListOf<ReaderChapterGroup>()
    val ungrouped = mutableListOf<ReaderChapter>()
    forEach { chapter ->
        val groupTitle = chapter.groupTitle?.takeIf { it.isNotBlank() }
        if (groupTitle == null) {
            ungrouped += chapter
        } else {
            if (ungrouped.isNotEmpty()) {
                groups += ReaderChapterGroup(title = null, chapters = ungrouped.toList())
                ungrouped.clear()
            }
            val lastGroup = groups.lastOrNull()
            if (lastGroup?.title == groupTitle) {
                groups[groups.lastIndex] = lastGroup.copy(chapters = lastGroup.chapters + chapter)
            } else {
                groups += ReaderChapterGroup(title = groupTitle, chapters = listOf(chapter))
            }
        }
    }
    if (ungrouped.isNotEmpty()) {
        groups += ReaderChapterGroup(title = null, chapters = ungrouped.toList())
    }
    return groups
}

@Composable
private fun ReflowChapterRow(
    chapter: ReaderChapter,
    sourcePageStartPages: List<Int>,
    onPageSelected: (Int) -> Unit,
    indent: androidx.compose.ui.unit.Dp = 18.dp
) {
    val targetPage = sourcePageStartPages
        .getOrNull(chapter.sourceIndex)
        ?.coerceAtLeast(0)
        ?: 0
    Text(
        text = chapter.title,
        style = MaterialTheme.typography.bodySmall,
        color = Color.White,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPageSelected(targetPage) }
            .padding(start = indent, top = 8.dp, bottom = 8.dp)
    )
}

@Composable
fun PdfReaderBookmarksTab(
    currentPage: Int,
    pageCount: Int,
    annotations: List<DeviceReaderAnnotation>,
    onPageSelected: (Int) -> Unit,
    onAnnotationEdit: (DeviceReaderAnnotation) -> Unit,
    onAnnotationDelete: (DeviceReaderAnnotation) -> Unit,
    onAddBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedAnnotations = remember(annotations) {
        annotations.sortedWith(compareBy<DeviceReaderAnnotation> { it.page }.thenBy { it.createdAtMillis })
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (sortedAnnotations.isEmpty()) {
            // El icono grande solo aparece en el estado vacío; con anotaciones la lista aprovecha todo el panel.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
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
                Text(
                    text = stringResource(R.string.reader_no_bookmark),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.reader_add_bookmark_extended_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(sortedAnnotations, key = { "${it.id}-${it.createdAtMillis}-${it.page}" }) { annotation ->
                    // El panel usa el estado del lector para que los cambios se reflejen al instante en la página abierta.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onPageSelected(annotation.page.coerceIn(0, pageCount - 1)) }
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.reader_annotation_page_summary,
                                annotation.type.localizedReaderName(),
                                annotation.page + 1,
                                annotation.pageCount
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = TarnishedGold,
                            fontWeight = FontWeight.SemiBold
                        )
                        annotation.note.takeIf { it.isNotBlank() }?.let { note ->
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        annotation.selectedText.takeIf { it.isNotBlank() }?.let { selectedText ->
                            Text(
                                text = selectedText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.76f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { onAnnotationEdit(annotation) }) {
                                Text(text = stringResource(R.string.edit_review_button).uppercase(), color = Color.White)
                            }
                            TextButton(onClick = { onAnnotationDelete(annotation) }) {
                                Text(text = stringResource(R.string.delete).uppercase(), color = Color(0xFFFFB4A8))
                            }
                        }
                    }
                }
            }
        }
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
                    .clickable(onClick = onAddBookmark),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.reader_add_new),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.reader_settings),
                tint = Color(0xFFEBC7E8)
            )
        }
    }
}

@Composable
fun ReflowReaderImagesTab(
    file: DeviceLibraryFile,
    document: ReaderEngineDocument,
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
                coverText = "",
                overrideCoverId = null,
                modifier = Modifier
                    .width(126.dp)
                    .aspectRatio(0.68f)
                    .clickable(onClick = onCoverSelected)
            )
        }
        item { PdfImagePlaceholderLabel(stringResource(R.string.reader_detected_cover)) }
        items(document.imagePaths) { path ->
            EpubDrawerImageItem(
                path = path,
                bytes = document.epubResources[path]
                    ?: document.epubResources.entries.firstOrNull { it.key.equals(path, ignoreCase = true) }?.value
            )
        }
    }
}

@Composable
private fun EpubDrawerImageItem(
    path: String,
    bytes: ByteArray?
) {
    val bitmap = remember(path, bytes) {
        bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.reader_detected_image, path.substringAfterLast('/')),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.08f))
        )
    } else {
        PdfImagePlaceholderLabel(stringResource(R.string.reader_detected_image, path.substringAfterLast('/')))
    }
}

@Composable
fun PdfReaderImagesTab(
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
        item { PdfImagePlaceholderLabel(stringResource(R.string.reader_detected_cover)) }
        items(listOf("ePUB", "T", "☕", "━━━━━━", "⋯")) { label ->
            PdfImagePlaceholderLabel(label)
        }
    }
}

@Composable
fun PdfImagePlaceholderLabel(text: String) {
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



