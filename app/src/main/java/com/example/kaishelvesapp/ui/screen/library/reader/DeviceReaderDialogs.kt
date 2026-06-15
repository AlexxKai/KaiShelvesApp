package com.example.kaishelvesapp.ui.screen.library

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.DeviceReaderAnnotation
import com.example.kaishelvesapp.data.model.DeviceReaderAnnotationType
import com.example.kaishelvesapp.data.repository.DeviceLibraryFile
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import java.text.DateFormat
import java.util.Date

@Composable
fun ReflowAnnotationsDialog(
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
                text = stringResource(R.string.reader_annotations_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            if (annotations.isEmpty()) {
                Text(
                    text = stringResource(R.string.reader_no_annotations),
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
                                text = stringResource(
                                    R.string.reader_annotation_page_summary,
                                    annotation.type.localizedReaderName(),
                                    annotation.page + 1,
                                    annotation.pageCount
                                ),
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
                                    Text(text = stringResource(R.string.go).uppercase(), color = TarnishedGold)
                                }
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
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = stringResource(R.string.close).uppercase(), color = Color.White)
            }
        }
    }
}

@Composable
fun ReaderNoteDialog(
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
                text = stringResource(R.string.reader_new_note),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            TextField(
                value = noteText,
                onValueChange = { noteText = it.take(500) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = { Text(stringResource(R.string.reader_note_placeholder)) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.cancel).uppercase(), color = Color.White)
                }
                TextButton(
                    onClick = { onSave(noteText.trim()) },
                    enabled = noteText.isNotBlank()
                ) {
                    Text(text = stringResource(R.string.save).uppercase(), color = TarnishedGold)
                }
            }
        }
    }
}

@Composable
fun ReaderAnnotationEditDialog(
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
                text = stringResource(
                    R.string.reader_annotation_page_summary,
                    annotation.type.localizedReaderName(),
                    annotation.page + 1,
                    annotation.pageCount
                ),
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
                label = { Text(stringResource(R.string.reader_selected_text)) }
            )
            TextField(
                value = noteText,
                onValueChange = { noteText = it.take(800) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text(stringResource(R.string.reader_selection_note)) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDelete) {
                    Text(text = stringResource(R.string.delete).uppercase(), color = Color(0xFFFFB4A8))
                }
                Row {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.cancel).uppercase(), color = Color.White)
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
                        Text(text = stringResource(R.string.save).uppercase(), color = TarnishedGold)
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceReaderAnnotationType.localizedReaderName(): String {
    return when (this) {
        DeviceReaderAnnotationType.Bookmark -> stringResource(R.string.reader_annotation_bookmark)
        DeviceReaderAnnotationType.Highlight -> stringResource(R.string.reader_annotation_highlight)
        DeviceReaderAnnotationType.Note -> stringResource(R.string.reader_annotation_note)
    }
}


@Composable
fun PdfBookInfoDialog(
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
        ?: stringResource(R.string.reader_book_info_default_description)
    val notAvailable = stringResource(R.string.not_available)
    val fileNameLine = stringResource(R.string.reader_book_info_file_name, file.name)
    val locationLine = stringResource(R.string.reader_book_info_location, file.location)
    val fileSizeLine = stringResource(
        R.string.reader_book_info_file_size,
        file.sizeBytes?.let { Formatter.formatShortFileSize(context, it) } ?: notAvailable
    )
    val totalPagesLine = stringResource(R.string.reader_book_info_total_pages, pageCount)
    val currentPageLine = stringResource(R.string.reader_book_info_current_page, currentPage + 1)
    val progressLine = stringResource(R.string.reader_book_info_progress, progress)
    val lastModifiedLine = file.modifiedAtMillis?.let {
        stringResource(R.string.reader_book_info_last_modified, DateFormat.getDateTimeInstance().format(Date(it)))
    }
    val readingHoursLine = stringResource(R.string.reader_book_info_reading_hours_pending)
    val readingSpeedLine = stringResource(R.string.reader_book_info_reading_speed_pending)
    val readingHistoryLine = stringResource(R.string.reader_book_info_reading_history_days)
    val currentChapterLine = stringResource(R.string.reader_book_info_current_chapter_pending)
    val bookmarksLine = stringResource(R.string.reader_book_info_bookmarks_pending)
    val bookInfoDetails = buildString {
        appendLine(fileNameLine)
        appendLine(locationLine)
        appendLine(fileSizeLine)
        appendLine(totalPagesLine)
        appendLine(currentPageLine)
        appendLine(progressLine)
        lastModifiedLine?.let(::appendLine)
        appendLine()
        appendLine(readingHoursLine)
        appendLine(readingSpeedLine)
        appendLine(readingHistoryLine)
        appendLine()
        appendLine(currentChapterLine)
        appendLine(bookmarksLine)
    }

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
                        text = stringResource(R.string.reader_menu_book_info),
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
                        text = bookInfoDetails,
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
                        Text(text = stringResource(R.string.more_option).uppercase() + "...", color = Color.White)
                    }
                    TextButton(onClick = {}) {
                        Text(text = stringResource(R.string.favorite).uppercase(), color = Color.White)
                    }
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.ok).uppercase(), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PdfBookInfoMoreDialog(
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
                    R.string.reader_book_info_google,
                    R.string.reader_book_info_wikipedia,
                    R.string.reader_book_info_facebook,
                    R.string.reader_book_info_twitter,
                    R.string.reader_book_info_goodreads,
                    R.string.reader_menu_share,
                    R.string.reader_book_info_send_file,
                    R.string.reader_book_info_clear_statistics,
                    R.string.reader_book_info_calendar,
                    R.string.reader_book_info_create_shortcut
                ).forEach { label ->
                    Text(
                        text = stringResource(label),
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



