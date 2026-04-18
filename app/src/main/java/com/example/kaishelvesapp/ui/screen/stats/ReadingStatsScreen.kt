package com.example.kaishelvesapp.ui.screen.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.model.LibroLeido
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiNavigationDrawerContent
import com.example.kaishelvesapp.ui.components.KaiPrimaryTopBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.ReadingListViewModel
import kotlinx.coroutines.launch

@Composable
fun ReadingStatsScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: ReadingListViewModel,
    userName: String? = null,
    profileImageUrl: String? = null,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScanResult: (String) -> Unit,
    onGoToProfile: () -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit,
    pendingRequestCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onSectionSelected: (KaiSection) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerExpanded = drawerState.targetValue == DrawerValue.Open || drawerState.currentValue == DrawerValue.Open
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (uiState.libros.isEmpty()) {
            viewModel.cargarLecturas()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KaiNavigationDrawerContent(
                currentSection = KaiSection.STATS,
                subtitle = stringResource(R.string.reading_statistics_subtitle),
                userName = userName.orEmpty(),
                profileImageUrl = profileImageUrl.orEmpty(),
                expanded = drawerExpanded,
                onGoToProfile = {
                    scope.launch { drawerState.close() }
                    onGoToProfile()
                },
                onGoToSettingsPrivacy = {
                    scope.launch { drawerState.close() }
                    onGoToSettingsPrivacy()
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    onLogout()
                },
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
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onSearch = onSearch,
                    onScanResult = onScanResult,
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    notificationCount = pendingRequestCount,
                    onOpenNotifications = onOpenNotifications
                )
            },
            bottomBar = {
                KaiBottomBar(
                    current = KaiSection.STATS,
                    onSelect = onSectionSelected
                )
            }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                when {
                    uiState.isLoading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = TarnishedGold)
                        }
                    }

                    else -> {
                        val stats = rememberReadingStats(uiState.libros)
                        val stacked = maxWidth < 700.dp

                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StatsHero(totalBooks = stats.totalBooks)

                            if (stacked) {
                                StatsCard(stringResource(R.string.total_books_read), stats.totalBooks.toString())
                                StatsCard(stringResource(R.string.average_rating), stats.averageRating)
                                StatsCard(stringResource(R.string.favorite_genre), stats.favoriteGenre)
                                StatsCard(stringResource(R.string.total_pages_read), stats.totalPages.toString())
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    StatsCard(stringResource(R.string.total_books_read), stats.totalBooks.toString(), Modifier.weight(1f))
                                    StatsCard(stringResource(R.string.average_rating), stats.averageRating, Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    StatsCard(stringResource(R.string.favorite_genre), stats.favoriteGenre, Modifier.weight(1f))
                                    StatsCard(stringResource(R.string.total_pages_read), stats.totalPages.toString(), Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ReadingStats(
    val totalBooks: Int,
    val averageRating: String,
    val favoriteGenre: String,
    val totalPages: Int
)

@Composable
private fun rememberReadingStats(books: List<LibroLeido>): ReadingStats {
    val ratedBooks = books.filter { it.puntuacion > 0 }
    val averageRating = if (ratedBooks.isNotEmpty()) {
        String.format("%.1f/5", ratedBooks.map { it.puntuacion }.average())
    } else {
        stringResource(R.string.not_rated_yet)
    }
    val favoriteGenre = books
        .map { it.genero.ifBlank { stringResource(R.string.unknown_genre) } }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?: stringResource(R.string.no_books_found)

    return ReadingStats(
        totalBooks = books.size,
        averageRating = averageRating,
        favoriteGenre = favoriteGenre,
        totalPages = books.sumOf { it.paginas }
    )
}

@Composable
private fun StatsHero(totalBooks: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BloodWine.copy(alpha = 0.24f),
                            DeepWalnut,
                            Obsidian
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.reading_statistics),
                style = MaterialTheme.typography.headlineMedium,
                color = TarnishedGold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.reading_statistics_summary, totalBooks),
                style = MaterialTheme.typography.bodyLarge,
                color = OldIvory
            )
        }
    }
}

@Composable
private fun StatsCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TarnishedGold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = OldIvory
            )
        }
    }
}
