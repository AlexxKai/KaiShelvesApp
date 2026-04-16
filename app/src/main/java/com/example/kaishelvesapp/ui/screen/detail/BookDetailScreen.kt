package com.example.kaishelvesapp.ui.screen.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.data.model.LibroLeido
import com.example.kaishelvesapp.data.model.UserBookList
import com.example.kaishelvesapp.data.model.UserBookTag
import com.example.kaishelvesapp.data.repository.UserListsRepository
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.components.KaiUserAvatar
import com.example.kaishelvesapp.ui.components.RatingStars
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.BookDetailViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun BookDetailScreen(
    libro: Libro,
    viewModel: BookDetailViewModel,
    userName: String? = null,
    profileImageUrl: String? = null,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit,
    onMarkAsRead: (Libro) -> Unit,
    onGoToReadingList: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val safeBookId = libro.id.ifBlank { libro.isbn }
    var showOrganizerDialog by rememberSaveable { mutableStateOf(false) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showReadReviewDialog by rememberSaveable { mutableStateOf(false) }
    var showReviewContentDialog by rememberSaveable { mutableStateOf(false) }
    var showRemoveFromMyBooksDialog by rememberSaveable { mutableStateOf(false) }
    var pendingReadTagIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var readReviewTriggeredFromOrganizer by rememberSaveable { mutableStateOf(false) }
    val readBook = uiState.readBook

    LaunchedEffect(libro.id, libro.isbn) {
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

    if (showOrganizerDialog) {
        BookOrganizationDialog(
            libro = libro,
            availableLists = uiState.availableLists,
            availableTags = uiState.availableTags,
            initiallySelectedListId = uiState.selectedListIds.firstOrNull(),
            initiallySelectedTagIds = uiState.selectedTagIds,
            isSaving = uiState.isSavingLists,
            onDismiss = {
                if (!uiState.isSavingLists) {
                    showOrganizerDialog = false
                }
            },
            onSave = { selectedListId, selectedTagIds ->
                val currentSelectedListId = uiState.selectedListIds.firstOrNull()
                if (
                    selectedListId == UserListsRepository.SYSTEM_LIST_READ_ID &&
                    currentSelectedListId != UserListsRepository.SYSTEM_LIST_READ_ID
                ) {
                    pendingReadTagIds = selectedTagIds
                    readReviewTriggeredFromOrganizer = true
                    showReadReviewDialog = true
                } else {
                    viewModel.guardarOrganizacion(
                        libro = libro,
                        selectedListIds = selectedListId?.let(::setOf).orEmpty(),
                        selectedTagIds = selectedTagIds
                    )
                }
                showOrganizerDialog = false
            },
            onCreateNew = { showCreateDialog = true },
            onRemoveFromMyBooks = {
                showRemoveFromMyBooksDialog = true
            }
        )
    }

    if (showRemoveFromMyBooksDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isSavingLists) {
                    showRemoveFromMyBooksDialog = false
                }
            },
            title = {
                Text(
                    text = "¿Eliminar el libro y toda la actividad relacionada?",
                    color = TarnishedGold
                )
            },
            text = {
                Text(
                    text = "Al eliminar un libro de tus estanterías, también se borrarán las calificaciones, reseñas y actualizaciones que hayas realizado que estén asociadas con ese libro. Este paso es irreversible.",
                    color = OldIvory
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearBookOrganization(safeBookId)
                        showRemoveFromMyBooksDialog = false
                        showOrganizerDialog = false
                    },
                    enabled = !uiState.isSavingLists
                ) {
                    Text(
                        text = "ELIMINAR",
                        color = Color(0xFFE57A6D)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRemoveFromMyBooksDialog = false },
                    enabled = !uiState.isSavingLists
                ) {
                    Text(
                        text = "CANCELAR",
                        color = OldIvory
                    )
                }
            },
            containerColor = Obsidian
        )
    }

    if (showCreateDialog) {
        CreateShelfOrTagDialog(
            isSaving = uiState.isSavingLists,
            onDismiss = {
                if (!uiState.isSavingLists) {
                    showCreateDialog = false
                }
            },
            onCreateShelf = { name ->
                viewModel.createList(name = name, bookId = safeBookId)
                showCreateDialog = false
            },
            onCreateTag = { name ->
                viewModel.createTag(name = name, bookId = safeBookId)
                showCreateDialog = false
            }
        )
    }

    if (showReadReviewDialog) {
        ReadReviewDialog(
            libro = libro,
            isSaving = uiState.isSavingLists,
            initialRating = readBook?.puntuacion ?: 0,
            initialReview = readBook?.resena.orEmpty(),
            initialContainsSpoilers = readBook?.contieneSpoilers ?: false,
            onDismiss = {
                if (!uiState.isSavingLists) {
                    showReadReviewDialog = false
                }
            },
            onSave = { rating, review, containsSpoilers ->
                if (readReviewTriggeredFromOrganizer) {
                    viewModel.guardarLecturaConResena(
                        libro = libro,
                        selectedTagIds = pendingReadTagIds,
                        puntuacion = rating,
                        resena = review,
                        contieneSpoilers = containsSpoilers
                    )
                } else {
                    viewModel.actualizarLecturaResena(
                        bookId = safeBookId,
                        puntuacion = rating,
                        resena = review,
                        contieneSpoilers = containsSpoilers
                    )
                }
                readReviewTriggeredFromOrganizer = false
                showReadReviewDialog = false
            }
        )
    }

    if (showReviewContentDialog && readBook != null) {
        ReviewContentDialog(
            libro = libro,
            readBook = readBook,
            userName = userName,
            profileImageUrl = profileImageUrl,
            isSaving = uiState.isSavingLists,
            onDismiss = { showReviewContentDialog = false },
            onEdit = {
                showReviewContentDialog = false
                showReadReviewDialog = true
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
                .statusBarsPadding()
                .padding(paddingValues)
                .padding(innerPadding)
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp, bottom = 0.dp, start = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = TarnishedGold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                BookHeroSection(
                    libro = libro,
                    readRating = readBook?.puntuacion ?: 0,
                    isRead = uiState.isAlreadyRead,
                    isSaving = uiState.isSavingLists,
                    onRatingSelected = { rating ->
                        viewModel.actualizarLecturaResena(
                            bookId = safeBookId,
                            puntuacion = rating,
                            resena = readBook?.resena.orEmpty(),
                            contieneSpoilers = readBook?.contieneSpoilers ?: false
                        )
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                BookActionsSection(
                    uiState = uiState,
                    hasPdf = libro.pdf.isNotBlank(),
                    onPrimaryShelfAction = {
                        if (uiState.selectedListIds.isEmpty()) {
                            viewModel.guardarOrganizacion(
                                libro = libro,
                                selectedListIds = setOf(UserListsRepository.SYSTEM_LIST_WANT_TO_READ_ID),
                                selectedTagIds = uiState.selectedTagIds
                            )
                        } else {
                            showOrganizerDialog = true
                        }
                    },
                    onOpenOrganizer = { showOrganizerDialog = true },
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

                Spacer(modifier = Modifier.height(18.dp))

                if (!readBook?.resena.isNullOrBlank()) {
                    ReviewSummarySection(
                        libro = libro,
                        readBook = readBook,
                        userName = userName,
                        profileImageUrl = profileImageUrl,
                        onReadReview = { showReviewContentDialog = true },
                        onEditReview = { showReadReviewDialog = true }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }

                BookDescriptionCard(libro = libro)
            }
        }
    }
}

@Composable
private fun BookHeroSection(
    libro: Libro,
    readRating: Int,
    isRead: Boolean,
    isSaving: Boolean,
    onRatingSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            BookCover(
                imageUrl = libro.imagen,
                title = libro.titulo,
                containerColor = Color.Transparent,
                borderColor = Color.Transparent,
                modifier = Modifier
                    .width(190.dp)
                    .height(276.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

        if (isRead) {
            RatingStars(
                rating = readRating,
                onRatingSelected = { selectedRating ->
                    if (!isSaving) {
                        onRatingSelected(selectedRating)
                    }
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (readRating > 0) {
                    stringResource(R.string.rating_value, readRating)
                } else {
                    stringResource(R.string.not_rated_yet)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory
            )
        }
    }
}

@Composable
private fun BookDescriptionCard(libro: Libro) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
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

@Composable
private fun ReviewSummarySection(
    libro: Libro,
    readBook: LibroLeido,
    userName: String?,
    profileImageUrl: String?,
    onReadReview: () -> Unit,
    onEditReview: () -> Unit
) {
    val reviewText = readBook.resena.trim()
    val displayName = userName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.app_name)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.my_review_title),
            style = MaterialTheme.typography.headlineSmall,
            color = TarnishedGold,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        HorizontalDivider(
            modifier = Modifier.width(174.dp),
            color = TarnishedGold.copy(alpha = 0.22f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Obsidian.copy(alpha = 0.78f)),
            border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.16f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        KaiUserAvatar(
                            displayName = displayName,
                            imageUrl = profileImageUrl.orEmpty(),
                            modifier = Modifier.size(72.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = reviewDateLabel(readBook),
                            color = OldIvory,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                HorizontalDivider(color = TarnishedGold.copy(alpha = 0.16f))

                Spacer(modifier = Modifier.height(18.dp))

                if (readBook.contieneSpoilers) {
                    Text(
                        text = stringResource(R.string.review_contains_spoilers_warning),
                        color = OldIvory,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    TextButton(
                        onClick = onReadReview,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.read_review_action),
                            color = TarnishedGold,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = reviewText,
                        color = OldIvory,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                HorizontalDivider(color = TarnishedGold.copy(alpha = 0.16f))

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onEditReview,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.edit_review_action),
                        color = TarnishedGold,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewContentDialog(
    libro: Libro,
    readBook: LibroLeido,
    userName: String?,
    profileImageUrl: String?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val displayName = userName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.app_name)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = Obsidian.copy(alpha = 0.98f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = OldIvory
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = stringResource(R.string.read_review_action),
                        color = TarnishedGold,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KaiUserAvatar(
                        displayName = displayName,
                        imageUrl = profileImageUrl.orEmpty()
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.user_has_written_review, displayName),
                            color = TarnishedGold,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = reviewDateLabel(readBook),
                            color = OldIvory,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Obsidian.copy(alpha = 0.78f)),
                    border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.16f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            BookCover(
                                imageUrl = libro.imagen,
                                title = libro.titulo,
                                modifier = Modifier
                                    .width(88.dp)
                                    .height(126.dp)
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = libro.titulo.ifBlank { stringResource(R.string.unknown_title) },
                                    color = TarnishedGold,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (libro.autor.isNotBlank()) {
                                    Text(
                                        text = stringResource(R.string.book_by_author, libro.autor),
                                        color = OldIvory,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }

                                Surface(
                                    color = BloodWine.copy(alpha = 0.32f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.18f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF5BBF72),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.read_shelf_label),
                                            color = OldIvory,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        if (readBook.puntuacion > 0) {
                            RatingStars(
                                rating = readBook.puntuacion,
                                onRatingSelected = {}
                            )
                        }

                        Text(
                            text = readBook.resena.trim(),
                            color = OldIvory,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = onEdit,
                                enabled = !isSaving,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TarnishedGold.copy(alpha = 0.18f),
                                    contentColor = OldIvory
                                ),
                                border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.22f))
                            ) {
                                Text(
                                    text = stringResource(R.string.edit_review_button),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun reviewDateLabel(readBook: LibroLeido): String {
    return if (readBook.fechaLeido.isNotBlank()) {
        stringResource(R.string.read_date_value, readBook.fechaLeido)
    } else {
        stringResource(R.string.review_written_now)
    }
}

@Composable
private fun BookActionsSection(
    uiState: com.example.kaishelvesapp.ui.viewmodel.BookDetailUiState,
    hasPdf: Boolean,
    onPrimaryShelfAction: () -> Unit,
    onOpenOrganizer: () -> Unit,
    onOpenPdf: () -> Unit
) {
    val selectedList = remember(uiState.availableLists, uiState.selectedListIds) {
        uiState.availableLists.firstOrNull { it.id in uiState.selectedListIds }
    }
    val buttonLabel = selectedList?.name ?: stringResource(R.string.want_to_read)
    val isAddedToShelf = selectedList != null
    val buttonColor = if (isAddedToShelf) {
        BloodWine.copy(alpha = 0.72f)
    } else {
        Obsidian.copy(alpha = 0.42f)
    }
    val checkTint = when (selectedList?.id) {
        UserListsRepository.SYSTEM_LIST_READ_ID -> Color(0xFF5BBF72)
        UserListsRepository.SYSTEM_LIST_READING_ID -> Color(0xFFE2B84C)
        null -> OldIvory
        else -> Color(0xFFB7B4B0)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.isListsLoading || uiState.isSavingLists) {
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = KaiShelvesThemeDefaults.secondaryButtonColors()
            ) {
                CircularProgressIndicator(color = OldIvory)
            }
        } else if (uiState.availableLists.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = buttonColor,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onPrimaryShelfAction)
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isAddedToShelf) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = checkTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = buttonLabel,
                            color = OldIvory,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(TarnishedGold.copy(alpha = 0.16f))
                    )

                    Box(
                        modifier = Modifier
                            .width(58.dp)
                            .height(54.dp)
                            .clickable(onClick = onOpenOrganizer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = stringResource(R.string.open_book_organizer),
                            tint = OldIvory,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        } else {
            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                border = BorderStroke(1.dp, TarnishedGold)
            ) {
                Text(
                    text = stringResource(R.string.no_user_lists_available),
                    color = TarnishedGold
                )
            }
        }

        if (hasPdf) {
            OutlinedButton(
                onClick = onOpenPdf,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.24f))
            ) {
                Text(
                    text = stringResource(R.string.open_pdf),
                    color = OldIvory
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookOrganizationDialog(
    libro: Libro,
    availableLists: List<UserBookList>,
    availableTags: List<UserBookTag>,
    initiallySelectedListId: String?,
    initiallySelectedTagIds: Set<String>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String?, Set<String>) -> Unit,
    onCreateNew: () -> Unit,
    onRemoveFromMyBooks: () -> Unit
) {
    var selectedListId by remember(initiallySelectedListId) { mutableStateOf(initiallySelectedListId) }
    var selectedTagIds by remember(initiallySelectedTagIds) { mutableStateOf(initiallySelectedTagIds) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            color = Obsidian.copy(alpha = 0.98f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, enabled = !isSaving) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = OldIvory
                        )
                    }

                    Text(
                        text = stringResource(R.string.add_to_my_books),
                        color = TarnishedGold,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { onSave(selectedListId, selectedTagIds) },
                        enabled = !isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = stringResource(R.string.save),
                            tint = if (isSaving) OldIvory.copy(alpha = 0.5f) else TarnishedGold
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Obsidian),
                    border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BookCover(
                            imageUrl = libro.imagen,
                            title = libro.titulo,
                            modifier = Modifier
                                .width(72.dp)
                                .height(104.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = libro.titulo.ifBlank { stringResource(R.string.unknown_title) },
                                color = TarnishedGold,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (libro.autor.isNotBlank()) {
                                Text(
                                    text = stringResource(R.string.book_by_author, libro.autor),
                                    color = OldIvory,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }

                SectionTitle(title = stringResource(R.string.select_shelf))

                availableLists.forEach { list ->
                    ShelfOptionRow(
                        list = list,
                        isSelected = selectedListId == list.id,
                        onSelect = { selectedListId = list.id }
                    )
                }

                SectionTitle(title = stringResource(R.string.select_tags))

                if (availableTags.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_tags_available),
                        color = OldIvory,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                } else {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableTags.forEach { tag ->
                            val isSelected = tag.id in selectedTagIds
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (isSelected) BloodWine else Obsidian,
                                border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.35f)),
                                modifier = Modifier.clickable {
                                    selectedTagIds = if (isSelected) {
                                        selectedTagIds - tag.id
                                    } else {
                                        selectedTagIds + tag.id
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = OldIvory,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = tag.name,
                                        color = OldIvory,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onCreateNew,
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.create_new_tag_or_shelf),
                        color = TarnishedGold
                    )
                }

                HorizontalDivider(color = TarnishedGold.copy(alpha = 0.18f))

                TextButton(
                    onClick = onRemoveFromMyBooks,
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.remove_from_my_books),
                        color = Color(0xFFE57A6D)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = TarnishedGold,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(1.dp)
                .background(TarnishedGold.copy(alpha = 0.25f))
        )
    }
}

@Composable
private fun ShelfOptionRow(
    list: UserBookList,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = list.name,
                color = OldIvory,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.list_books_count, list.bookCount),
                color = OldIvory.copy(alpha = 0.76f),
                style = MaterialTheme.typography.bodySmall
            )
        }

        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
    }

    HorizontalDivider(
        color = TarnishedGold.copy(alpha = 0.14f),
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@Composable
private fun CreateShelfOrTagDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onCreateShelf: (String) -> Unit,
    onCreateTag: (String) -> Unit
) {
    var selectedType by rememberSaveable { mutableStateOf(CreateType.SHELF) }
    var name by rememberSaveable { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Obsidian,
            border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.35f))
        ) {
            Column {
                Surface(
                    color = BloodWine,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.create),
                        color = OldIvory,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                    )
                }

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedType = CreateType.TAG }
                        ) {
                            RadioButton(
                                selected = selectedType == CreateType.TAG,
                                onClick = { selectedType = CreateType.TAG }
                            )
                            Text(
                                text = stringResource(R.string.tag),
                                color = OldIvory
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedType = CreateType.SHELF }
                        ) {
                            RadioButton(
                                selected = selectedType == CreateType.SHELF,
                                onClick = { selectedType = CreateType.SHELF }
                            )
                            Text(
                                text = stringResource(R.string.shelf),
                                color = OldIvory
                            )
                        }
                    }

                    if (selectedType == CreateType.SHELF) {
                        Text(
                            text = stringResource(R.string.single_shelf_note),
                            color = OldIvory,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.tag_create_note),
                            color = OldIvory,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                text = if (selectedType == CreateType.SHELF) {
                                    stringResource(R.string.shelf_name_hint)
                                } else {
                                    stringResource(R.string.tag_name_hint)
                                }
                            )
                        },
                        enabled = !isSaving
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss, enabled = !isSaving) {
                            Text(
                                text = stringResource(R.string.cancel),
                                color = OldIvory
                            )
                        }
                        TextButton(
                            onClick = {
                                val trimmedName = name.trim()
                                if (trimmedName.isNotBlank()) {
                                    if (selectedType == CreateType.SHELF) {
                                        onCreateShelf(trimmedName)
                                    } else {
                                        onCreateTag(trimmedName)
                                    }
                                }
                            },
                            enabled = !isSaving && name.isNotBlank()
                        ) {
                            Text(
                                text = stringResource(R.string.save),
                                color = TarnishedGold
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class CreateType {
    TAG,
    SHELF
}

@Composable
private fun ReadReviewDialog(
    libro: Libro,
    isSaving: Boolean,
    initialRating: Int,
    initialReview: String,
    initialContainsSpoilers: Boolean,
    onDismiss: () -> Unit,
    onSave: (Int, String, Boolean) -> Unit
) {
    var rating by rememberSaveable(initialRating) { mutableStateOf(initialRating) }
    var review by rememberSaveable(initialReview) { mutableStateOf(initialReview) }
    var containsSpoilers by rememberSaveable(initialContainsSpoilers) { mutableStateOf(initialContainsSpoilers) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            color = Obsidian.copy(alpha = 0.98f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, enabled = !isSaving) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = OldIvory
                        )
                    }

                    Text(
                        text = stringResource(R.string.write_a_review),
                        color = TarnishedGold,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { onSave(rating, review, containsSpoilers) },
                        enabled = !isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = stringResource(R.string.save),
                            tint = if (isSaving) OldIvory.copy(alpha = 0.5f) else TarnishedGold
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Obsidian),
                    border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BookCover(
                            imageUrl = libro.imagen,
                            title = libro.titulo,
                            modifier = Modifier
                                .width(72.dp)
                                .height(104.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = libro.titulo.ifBlank { stringResource(R.string.unknown_title) },
                                color = TarnishedGold,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (libro.autor.isNotBlank()) {
                                Text(
                                    text = stringResource(R.string.book_by_author, libro.autor),
                                    color = OldIvory,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.rate_it),
                        color = OldIvory,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        (1..5).forEach { star ->
                            Icon(
                                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = if (star <= rating) Color(0xFFE2B84C) else OldIvory.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { rating = star }
                            )
                        }
                    }
                }

                HorizontalDivider(color = TarnishedGold.copy(alpha = 0.18f))

                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    label = {
                        Text(stringResource(R.string.write_review_optional))
                    },
                    minLines = 8
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Switch(
                        checked = containsSpoilers,
                        onCheckedChange = { containsSpoilers = it }
                    )
                    Text(
                        text = stringResource(R.string.contains_spoilers),
                        color = OldIvory,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = TarnishedGold
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = OldIvory,
            textAlign = TextAlign.Center
        )
    }
}
