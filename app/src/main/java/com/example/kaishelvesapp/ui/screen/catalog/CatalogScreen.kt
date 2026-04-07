package com.example.kaishelvesapp.ui.screen.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.ui.theme.AntiqueGold
import com.example.kaishelvesapp.ui.theme.AshBlack
import com.example.kaishelvesapp.ui.theme.BurgundyDark
import com.example.kaishelvesapp.ui.theme.CharcoalBrown
import com.example.kaishelvesapp.ui.theme.DarkWalnut
import com.example.kaishelvesapp.ui.theme.OldPaper
import com.example.kaishelvesapp.ui.viewmodel.CatalogViewModel

@Composable
fun CatalogScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: CatalogViewModel,
    onLogout: () -> Unit,
    onBookClick: (Libro) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = CharcoalBrown,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Archivo de la biblioteca",
                    style = MaterialTheme.typography.headlineLarge,
                    color = AntiqueGold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Busca entre volúmenes, autores y géneros.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldPaper
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar por título, autor o editorial") },
            shape = RoundedCornerShape(18.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            uiState.generos.forEach { genero ->
                GenreChip(
                    text = genero,
                    selected = uiState.selectedGenero == genero,
                    onClick = { viewModel.onGeneroSelected(genero) }
                )
                Spacer(modifier = Modifier.width(8.dp))
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
                        text = uiState.errorMessage ?: "Error desconocido",
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(onClick = { viewModel.cargarLibros() }) {
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
                        text = "No se han encontrado volúmenes",
                        color = OldPaper
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.libros) { libro ->
                        BookCard(
                            libro = libro,
                            onClick = { onBookClick(libro) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = onLogout,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cerrar sesión")
                }
            }
        }
    }
}

@Composable
private fun GenreChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) BurgundyDark else DarkWalnut
    val textColor = if (selected) AntiqueGold else OldPaper

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = AntiqueGold,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun BookCard(
    libro: Libro,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalBrown),
        border = androidx.compose.foundation.BorderStroke(1.dp, AntiqueGold),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(96.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BurgundyDark)
                    .border(
                        width = 1.dp,
                        color = AntiqueGold,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 6.dp, height = 70.dp)
                        .align(Alignment.CenterStart)
                        .background(AshBlack.copy(alpha = 0.35f))
                )

                Text(
                    text = "Tomo",
                    color = OldPaper,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = libro.titulo,
                    style = MaterialTheme.typography.titleLarge,
                    color = AntiqueGold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Autor: ${libro.autor}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldPaper
                )

                Text(
                    text = "Género: ${libro.genero}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldPaper
                )

                Text(
                    text = "Editorial: ${libro.editorial}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldPaper
                )

                if (libro.fechaPublicacion != 0) {
                    Text(
                        text = "Publicación: ${libro.fechaPublicacion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OldPaper
                    )
                }
            }
        }
    }
}