package com.example.kaishelvesapp.ui.screen.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.components.RatingStars
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.BookDetailViewModel
import kotlinx.coroutines.launch

@Composable
fun BookDetailScreen(
    libro: Libro,
    viewModel: BookDetailViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit,
    onMarkAsRead: (Libro) -> Unit,
    onGoToReadingList: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showListsDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(libro.id, libro.isbn) {
        val safeBookId = libro.id.ifBlank { libro.isbn }
        viewModel.cargarEstadoLectura(safeBookId)
        viewModel.cargarListasParaLibro(safeBookId)
    }

    LaunchedEffect(uiState.errorMessageRes, uiState.successMessageRes) {
        uiState.errorMessageRes?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            viewModel.clearMessages()
        }

        uiState.successMessageRes?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            viewModel.clearMessages()
        }
    }

    if (showListsDialog) {
        SelectListsDialog(
            lists = uiState.availableLists,
            initiallySelected = uiState.selectedListIds,
            isSaving = uiState.isSavingLists,
            onDismiss = {
                if (!uiState.isSavingLists) {
                    showListsDialog = false
                }
            },
            onConfirm = { selectedIds ->
                viewModel.guardarListas(libro, selectedIds)
                if (!uiState.isSavingLists) {
                    showListsDialog = false
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                border = BorderStroke(1.dp, TarnishedGold)
            ) {
                Text(
                    text = stringResource(R.string.back),
                    color = TarnishedGold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Obsidian
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    BookHeroSection(libro = libro)

                    Spacer(modifier = Modifier.height(20.dp))

                    BookDescriptionCard(libro = libro)

                    Spacer(modifier = Modifier.height(18.dp))

                    BookActionsSection(
                        uiState = uiState,
                        libro = libro,
                        onMarkAsRead = {
                            onMarkAsRead(libro)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.book_added_to_readings)
                                )
                            }
                        },
                        onGoToReadingList = onGoToReadingList,
                        onOpenLists = { showListsDialog = true },
                        onOpenPdf = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(libro.pdf))
                                context.startActivity(intent)
                            } catch (_: ActivityNotFoundException) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.could_not_open_pdf)
                                    )
                                }
                            } catch (_: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.invalid_pdf_link)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookHeroSection(libro: Libro) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BloodWine,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 24.dp, vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            BookCover(
                imageUrl = libro.imagen,
                title = libro.titulo,
                modifier = Modifier
                    .width(210.dp)
                    .height(305.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = libro.titulo.ifBlank { stringResource(R.string.unknown_title) },
            style = MaterialTheme.typography.displaySmall,
            color = TarnishedGold,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            lineHeight = MaterialTheme.typography.displaySmall.lineHeight
        )

        if (libro.autor.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.book_by_author, libro.autor),
                style = MaterialTheme.typography.titleMedium,
                color = OldIvory.copy(alpha = 0.92f),
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
private fun BookDescriptionCard(libro: Libro) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.book_description_section),
                style = MaterialTheme.typography.headlineSmall,
                color = TarnishedGold,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            DetailRow(
                label = stringResource(R.string.title),
                value = libro.titulo.ifBlank { stringResource(R.string.unknown_title) }
            )

            if (libro.autor.isNotBlank()) {
                DetailRow(
                    label = stringResource(R.string.author),
                    value = libro.autor
                )
            }

            if (libro.editorial.isNotBlank()) {
                DetailRow(
                    label = stringResource(R.string.publisher),
                    value = libro.editorial
                )
            }

            if (libro.genero.isNotBlank()) {
                DetailRow(
                    label = stringResource(R.string.genre),
                    value = libro.genero
                )
            }

            if (libro.fechaPublicacion != 0) {
                DetailRow(
                    label = stringResource(R.string.publication),
                    value = libro.fechaPublicacion.toString()
                )
            }

            if (libro.paginas != 0) {
                DetailRow(
                    label = stringResource(R.string.pages),
                    value = libro.paginas.toString()
                )
            }

            if (libro.isbn.isNotBlank()) {
                DetailRow(
                    label = stringResource(R.string.isbn),
                    value = libro.isbn
                )
            }
        }
    }
}

@Composable
private fun BookActionsSection(
    uiState: com.example.kaishelvesapp.ui.viewmodel.BookDetailUiState,
    libro: Libro,
    onMarkAsRead: () -> Unit,
    onGoToReadingList: () -> Unit,
    onOpenLists: () -> Unit,
    onOpenPdf: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Obsidian.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                uiState.isLoading -> {
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = KaiShelvesThemeDefaults.secondaryButtonColors()
                    ) {
                        CircularProgressIndicator(color = OldIvory)
                    }
                }

                uiState.isAlreadyRead -> {
                    Button(
                        onClick = onGoToReadingList,
                        modifier = Modifier.fillMaxWidth(),
                        colors = KaiShelvesThemeDefaults.secondaryButtonColors()
                    ) {
                        Text(stringResource(R.string.go_to_my_readings))
                    }
                }

                else -> {
                    Button(
                        onClick = onMarkAsRead,
                        modifier = Modifier.fillMaxWidth(),
                        colors = KaiShelvesThemeDefaults.primaryButtonColors()
                    ) {
                        Text(stringResource(R.string.mark_as_read))
                    }
                }
            }

            OutlinedButton(
                onClick = onOpenLists,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isListsLoading && !uiState.isSavingLists && uiState.availableLists.isNotEmpty(),
                border = BorderStroke(1.dp, TarnishedGold)
            ) {
                if (uiState.isSavingLists) {
                    CircularProgressIndicator(
                        color = TarnishedGold,
                        modifier = Modifier.width(18.dp).height(18.dp)
                    )
                } else {
                    Text(
                        text = if (uiState.selectedListIds.isEmpty()) {
                            stringResource(R.string.select_lists_for_book)
                        } else {
                            stringResource(R.string.manage_book_lists)
                        },
                        color = TarnishedGold
                    )
                }
            }

            if (uiState.availableLists.isEmpty() && !uiState.isListsLoading) {
                Text(
                    text = stringResource(R.string.no_user_lists_available),
                    color = OldIvory,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (libro.pdf.isNotBlank()) {
                Button(
                    onClick = onOpenPdf,
                    modifier = Modifier.fillMaxWidth(),
                    colors = KaiShelvesThemeDefaults.secondaryButtonColors()
                ) {
                    Text(stringResource(R.string.open_pdf))
                }
            }
        }
    }
}

@Composable
private fun SelectListsDialog(
    lists: List<com.example.kaishelvesapp.data.model.UserBookList>,
    initiallySelected: Set<String>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var selectedIds by remember(initiallySelected, lists) {
        mutableStateOf(initiallySelected.intersect(lists.map { it.id }.toSet()))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.select_lists_dialog_title),
                color = TarnishedGold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_lists_dialog_body),
                    color = OldIvory,
                    style = MaterialTheme.typography.bodyMedium
                )

                lists.forEach { list ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedIds.contains(list.id),
                            onCheckedChange = { checked ->
                                selectedIds = if (checked) {
                                    selectedIds + list.id
                                } else {
                                    selectedIds - list.id
                                }
                            }
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = list.name,
                                color = TarnishedGold,
                                style = MaterialTheme.typography.titleMedium
                            )

                            if (list.description.isNotBlank()) {
                                Text(
                                    text = list.description,
                                    color = OldIvory,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedIds) },
                enabled = !isSaving
            ) {
                Text(
                    text = stringResource(R.string.save),
                    color = TarnishedGold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = OldIvory
                )
            }
        },
        containerColor = Obsidian
    )
}

@Composable
private fun DetailLine(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TarnishedGold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = OldIvory
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = TarnishedGold,
            modifier = Modifier.width(112.dp)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = OldIvory,
            modifier = Modifier.weight(1f)
        )
    }
}
