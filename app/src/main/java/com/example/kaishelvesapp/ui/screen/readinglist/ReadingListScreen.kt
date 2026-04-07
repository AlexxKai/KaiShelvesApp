package com.example.kaishelvesapp.ui.screen.readinglist

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.data.model.LibroLeido
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.components.KaiTopBar
import com.example.kaishelvesapp.ui.components.RatingStars
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.ReadingListViewModel

@Composable
fun ReadingListScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: ReadingListViewModel,
    onBack: () -> Unit,
    onSectionSelected: (KaiSection) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.cargarLecturas()
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensajes()
        }

        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensajes()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            KaiBottomBar(
                current = KaiSection.READING,
                onSelect = onSectionSelected
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            KaiTopBar(
                title = "Mis lecturas",
                subtitle = "Aquí se conservan los volúmenes que has leído."
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = TarnishedGold)
                    }
                }

                uiState.errorMessage != null && uiState.libros.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "Error",
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.cargarLecturas() },
                            colors = KaiShelvesThemeDefaults.primaryButtonColors()
                        ) {
                            Text("Reintentar")
                        }
                    }
                }

                uiState.libros.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Aún no has marcado ningún libro como leído",
                            color = OldIvory
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.libros) { libro ->
                            ReadingItem(
                                libro = libro,
                                onUpdateRating = { rating ->
                                    viewModel.actualizarPuntuacion(libro.isbn, rating)
                                },
                                onDelete = {
                                    viewModel.eliminarLibro(libro.isbn)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingItem(
    libro: LibroLeido,
    onUpdateRating: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedRating by remember { mutableIntStateOf(libro.puntuacion) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar lectura", color = TarnishedGold) },
            text = { Text("¿Quieres eliminar \"${libro.titulo}\" de tus lecturas?", color = OldIvory) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Eliminar", color = TarnishedGold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar", color = OldIvory)
                }
            },
            containerColor = Obsidian
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row {
                BookCover(
                    imageUrl = libro.imagen,
                    title = libro.titulo,
                    modifier = Modifier
                        .width(58.dp)
                        .height(90.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = libro.titulo,
                        style = MaterialTheme.typography.titleLarge,
                        color = TarnishedGold
                    )
                    Text("Autor: ${libro.autor}", color = OldIvory)
                    Text("Leído el: ${libro.fechaLeido}", color = OldIvory)

                    Spacer(modifier = Modifier.height(8.dp))

                    RatingStars(
                        rating = libro.puntuacion
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (libro.puntuacion == 0) {
                            "Sin puntuar todavía"
                        } else {
                            "Puntuación: ${libro.puntuacion}/5"
                        },
                        color = OldIvory
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    border = BorderStroke(1.dp, TarnishedGold)
                ) {
                    Text("Cambiar puntuación", color = TarnishedGold)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = Obsidian
                ) {
                    (0..5).forEach { puntuacion ->
                        DropdownMenuItem(
                            text = { Text("$puntuacion", color = OldIvory) },
                            onClick = {
                                selectedRating = puntuacion
                                expanded = false
                                onUpdateRating(selectedRating)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(onClick = { showDeleteDialog = true }) {
                Text("Eliminar de mis lecturas", color = TarnishedGold)
            }
        }
    }
}