package com.example.kaishelvesapp.ui.screen.catalog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.Libro
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiNavigationDrawerContent
import com.example.kaishelvesapp.ui.components.KaiPrimaryTopBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.CatalogViewModel
import kotlinx.coroutines.launch

private enum class CatalogViewMode {
    COMPACT,
    DETAILED
}

@Composable
fun CatalogScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: CatalogViewModel,
    onGoToProfile: () -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit,
    onBookClick: (Libro) -> Unit = {},
    onSectionSelected: (KaiSection) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var viewMode by rememberSaveable { androidx.compose.runtime.mutableStateOf(CatalogViewMode.DETAILED) }
    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KaiNavigationDrawerContent(
                currentSection = KaiSection.CATALOG,
                headerTitle = stringResource(R.string.catalog_title),
                subtitle = stringResource(R.string.catalog_subtitle),
                onSectionSelected = { section ->
                    scope.launch { drawerState.close() }
                    onSectionSelected(section)
                }
            )
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                KaiPrimaryTopBar(
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    onGoToProfile = onGoToProfile,
                    onGoToSettingsPrivacy = onGoToSettingsPrivacy,
                    onLogout = onLogout
                )
            },
            bottomBar = {
                KaiBottomBar(
                    current = KaiSection.CATALOG,
                    onSelect = onSectionSelected
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                CatalogIntroCard(
                    title = stringResource(R.string.catalog_title),
                    subtitle = stringResource(R.string.catalog_subtitle),
                )

                Spacer(modifier = Modifier.height(10.dp))

                CatalogStickyControls(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onSearch = { viewModel.ejecutarBusqueda() },
                    genres = uiState.generos,
                    selectedGenre = uiState.selectedGenero,
                    onGenreSelected = viewModel::onGeneroSelected,
                    viewMode = viewMode,
                    onToggleCompact = { enabled ->
                        viewMode = if (enabled) CatalogViewMode.COMPACT else CatalogViewMode.DETAILED
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                when {
                    uiState.isLoading -> {
                        CatalogMessageBox {
                            CircularProgressIndicator(color = TarnishedGold)
                        }
                    }

                    uiState.errorMessage != null -> {
                        CatalogMessageBox {
                            Text(
                                text = uiState.errorMessage ?: "Error desconocido",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
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
                        CatalogMessageBox {
                            Text(
                                text = stringResource(R.string.no_books_found),
                                color = OldIvory,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            item {
                                AnimatedContent(
                                    targetState = viewMode,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "catalog_view_mode"
                                ) { mode ->
                                    when (mode) {
                                        CatalogViewMode.COMPACT -> CompactCatalogSection(
                                            books = uiState.libros,
                                            onBookClick = onBookClick
                                        )

                                        CatalogViewMode.DETAILED -> DetailedCatalogSection(
                                            books = uiState.libros,
                                            onBookClick = onBookClick
                                        )
                                    }
                                }
                            }

                            item {
                                TextButton(
                                    onClick = onLogout,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.logout), color = OldIvory)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogIntroCard(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BloodWine.copy(alpha = 0.16f),
                            DeepWalnut,
                            Obsidian
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TarnishedGold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory
            )
        }
    }
}

@Composable
private fun CatalogStickyControls(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    genres: List<String>,
    selectedGenre: String,
    onGenreSelected: (String) -> Unit,
    viewMode: CatalogViewMode,
    onToggleCompact: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DeepWalnut.copy(alpha = 0.98f),
                            Obsidian
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.search)) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = KaiShelvesThemeDefaults.outlinedTextFieldColors()
                )

                CatalogViewSwitch(
                    compactSelected = viewMode == CatalogViewMode.COMPACT,
                    onToggleCompact = onToggleCompact
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSearch,
                    colors = KaiShelvesThemeDefaults.primaryButtonColors()
                ) {
                    Text(stringResource(R.string.search))
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                ) {
                    genres.forEach { genero ->
                        GenreChip(
                            text = genero,
                            selected = selectedGenre == genero,
                            onClick = { onGenreSelected(genero) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogViewSwitch(
    compactSelected: Boolean,
    onToggleCompact: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(DeepWalnut.copy(alpha = 0.92f))
            .border(1.dp, TarnishedGold.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (compactSelected) "Portadas" else "Detalle",
            style = MaterialTheme.typography.labelLarge,
            color = TarnishedGold
        )

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = compactSelected,
            onCheckedChange = onToggleCompact,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TarnishedGold,
                checkedTrackColor = BloodWine,
                uncheckedThumbColor = OldIvory,
                uncheckedTrackColor = DeepWalnut,
                uncheckedBorderColor = TarnishedGold
            )
        )
    }
}

@Composable
private fun CatalogMessageBox(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    }
}

@Composable
private fun CompactCatalogSection(
    books: List<Libro>,
    onBookClick: (Libro) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val columns = if (maxWidth < 560.dp) 2 else 3
        val rows = books.chunked(columns)

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { libro ->
                        CompactBookCard(
                            libro = libro,
                            onClick = { onBookClick(libro) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    repeat(columns - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailedCatalogSection(
    books: List<Libro>,
    onBookClick: (Libro) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        books.forEach { libro ->
            BookCard(
                libro = libro,
                onClick = { onBookClick(libro) }
            )
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
private fun CompactBookCard(
    libro: Libro,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BloodWine.copy(alpha = 0.24f),
                            DeepWalnut,
                            Obsidian
                        )
                    )
                )
                .padding(8.dp)
        ) {
            BookCover(
                imageUrl = libro.imagen,
                title = libro.titulo,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = libro.titulo.ifBlank { stringResource(R.string.unknown_title) },
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory,
                maxLines = 2,
                modifier = Modifier.heightIn(min = 36.dp)
            )
        }
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
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            BloodWine.copy(alpha = 0.16f),
                            DeepWalnut,
                            Obsidian
                        )
                    )
                )
                .padding(16.dp)
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
