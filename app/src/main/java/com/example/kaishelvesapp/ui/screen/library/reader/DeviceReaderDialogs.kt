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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
                text = "Anotaciones",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            if (annotations.isEmpty()) {
                Text(
                    text = "Todavía no hay marcadores, subrayados ni notas.",
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
                                text = "${annotation.type.readableName()} · Página ${annotation.page + 1}/${annotation.pageCount}",
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
                                    Text(text = "IR", color = TarnishedGold)
                                }
                                TextButton(onClick = { onAnnotationEdit(annotation) }) {
                                    Text(text = "EDITAR", color = Color.White)
                                }
                                TextButton(onClick = { onAnnotationDelete(annotation) }) {
                                    Text(text = "ELIMINAR", color = Color(0xFFFFB4A8))
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
                Text(text = "CERRAR", color = Color.White)
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
                text = "Nueva nota",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            TextField(
                value = noteText,
                onValueChange = { noteText = it.take(500) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = { Text("Escribe una nota de lectura") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = "CANCELAR", color = Color.White)
                }
                TextButton(
                    onClick = { onSave(noteText.trim()) },
                    enabled = noteText.isNotBlank()
                ) {
                    Text(text = "GUARDAR", color = TarnishedGold)
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
                text = "${annotation.type.readableName()} · Página ${annotation.page + 1}/${annotation.pageCount}",
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
                label = { Text("Texto seleccionado") }
            )
            TextField(
                value = noteText,
                onValueChange = { noteText = it.take(800) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text("Nota") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDelete) {
                    Text(text = "ELIMINAR", color = Color(0xFFFFB4A8))
                }
                Row {
                    TextButton(onClick = onDismiss) {
                        Text(text = "CANCELAR", color = Color.White)
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
                        Text(text = "GUARDAR", color = TarnishedGold)
                    }
                }
            }
        }
    }
}

fun DeviceReaderAnnotationType.readableName(): String {
    return when (this) {
        DeviceReaderAnnotationType.Bookmark -> "Marcador"
        DeviceReaderAnnotationType.Highlight -> "Subrayado"
        DeviceReaderAnnotationType.Note -> "Nota"
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
        ?: "Información del libro pendiente de completar. Podrás ampliar estos datos desde las opciones del libro."

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
                        text = "Información del libro",
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
                        text = buildString {
                            appendLine("Nombre del archivo: ${file.name}")
                            appendLine("Ubicación: ${file.location}")
                            appendLine("Tamaño del archivo: ${file.sizeBytes?.let { Formatter.formatShortFileSize(context, it) } ?: "No disponible"}")
                            appendLine("Páginas totales: $pageCount")
                            appendLine("Página actual: ${currentPage + 1}")
                            appendLine("Progreso: $progress%")
                            file.modifiedAtMillis?.let {
                                appendLine("Última modificación: ${DateFormat.getDateTimeInstance().format(Date(it))}")
                            }
                            appendLine()
                            appendLine("Horas de lectura: --")
                            appendLine("Velocidad de lectura (p/m): --")
                            appendLine("Historial de lectura en días:")
                            appendLine()
                            appendLine("Capítulo actual: pendiente de detectar")
                            appendLine("Marcadores: pendiente de implementar")
                        },
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
                        Text(text = "MÁS...", color = Color.White)
                    }
                    TextButton(onClick = {}) {
                        Text(text = "FAVORITO", color = Color.White)
                    }
                    TextButton(onClick = onDismiss) {
                        Text(text = "ACEPTAR", color = Color.White)
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
                    "Google Información del libro",
                    "Wikipedia Información del libro",
                    "Facebook Información del libro",
                    "Twitter Información del libro",
                    "GoodReads Información del libro",
                    "Compartir",
                    "Enviar archivo",
                    "Limpiar “Estadísticas”",
                    "Calendario",
                    "Crear acceso directo en escritorio"
                ).forEach { label ->
                    Text(
                        text = label,
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



