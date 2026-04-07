package com.example.kaishelvesapp.ui.screen.readinglist

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.data.model.LibroLeido
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.ReadingListViewModel

@Composable
fun ReadingListScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: ReadingListViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.cargarLecturas()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBack,
                border = BorderStroke(1.dp, TarnishedGold)
            ) {
                Text("Volver", color = TarnishedGold)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Mis lecturas",
                style = MaterialTheme.typography.headlineMedium,
                color = TarnishedGold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Obsidian
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    text = "Registro del lector",
                    style = MaterialTheme.typography.titleLarge,
                    color = TarnishedGold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Aquí se conservan los volúmenes que has leído.",
                    color = OldIvory
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
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

                    Button(colors = KaiShelvesThemeDefaults.primaryButtonColors(),
                        onClick = { viewModel.cargarLecturas() }) {
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

@Composable
private fun ReadingItem(
    libro: LibroLeido,
    onUpdateRating: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedRating by remember { mutableIntStateOf(libro.puntuacion) }

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
                Box(
                    modifier = Modifier
                        .width(58.dp)
                        .height(90.dp)
                        .background(
                            color = BloodWine,
                            shape = RoundedCornerShape(10.dp)
                        )
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
                    Text("Puntuación actual: ${libro.puntuacion}", color = OldIvory)
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
                    onDismissRequest = { expanded = false }
                ) {
                    (0..5).forEach { puntuacion ->
                        DropdownMenuItem(
                            text = { Text("$puntuacion") },
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

            TextButton(colors = KaiShelvesThemeDefaults.primaryButtonColors(),
                onClick = onDelete) {
                Text("Eliminar de mis lecturas")
            }
        }
    }
}