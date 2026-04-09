package com.example.kaishelvesapp.ui.screen.catalog

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.components.KaiScreen
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.CatalogViewModel

@Composable
fun CatalogScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: CatalogViewModel,
    onLogout: () -> Unit,
    onBookClick: (Libro) -> Unit = {},
    onSectionSelected: (KaiSection) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    KaiScreen(
        title = stringResource(R.string.catalog_title),
        subtitle = stringResource(R.string.catalog_subtitle),
        currentSection = KaiSection.CATALOG,
        onSectionSelected = onSectionSelected
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_by_title_author_publisher)) },
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
                colors = KaiShelvesThemeDefaults.outlinedTextFieldColors()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { viewModel.ejecutarBusqueda() },
                modifier = Modifier.fillMaxWidth(),
                colors = KaiShelvesThemeDefaults.primaryButtonColors()
            ) {
                Text(stringResource(R.string.search))
            }

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
                        CircularProgressIndicator(color = TarnishedGold)
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

                        Button(
                            onClick = { viewModel.cargarLibros() },
                            colors = KaiShelvesThemeDefaults.primaryButtonColors()
                        ) {
                            Text(stringResource(R.string.retry))
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
                            text = stringResource(R.string.no_books_found),
                            color = OldIvory
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.libros,
                            key = { _, libro -> libro.id.ifBlank { libro.titulo } }
                        ) { _, libro ->
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
                        Text(stringResource(R.string.logout), color = OldIvory)
                    }
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
    val background = if (selected) BloodWine else DeepWalnut
    val textColor = if (selected) TarnishedGold else OldIvory

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = TarnishedGold,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .animateContentSize()
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
            .clickable { onClick() }
            .animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            BookCover(
                imageUrl = libro.imagen,
                title = libro.titulo,
                modifier = Modifier
                    .width(64.dp)
                    .height(96.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = libro.titulo.ifBlank { stringResource(R.string.unknown_title) },
                    style = MaterialTheme.typography.titleLarge,
                    color = TarnishedGold
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (libro.autor.isNotBlank()) {
                    Text("${stringResource(R.string.author)}: ${libro.autor}", color = OldIvory)
                }

                if (libro.fechaPublicacion != 0) {
                    Text("${stringResource(R.string.year)}: ${libro.fechaPublicacion}", color = OldIvory)
                }

                if (libro.genero.isNotBlank()) {
                    Text("${stringResource(R.string.genre)}: ${libro.genero}", color = OldIvory)
                }
            }
        }
    }
}