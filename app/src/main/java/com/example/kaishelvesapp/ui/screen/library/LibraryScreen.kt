package com.example.kaishelvesapp.ui.screen.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiNavigationDrawerContent
import com.example.kaishelvesapp.ui.components.KaiPrimaryTopBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.NightBlack
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    userName: String?,
    profileImageUrl: String?,
    genres: List<String>,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onGenreClick: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScanResult: (String) -> Unit,
    onGoToProfile: () -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit,
    onSectionSelected: (KaiSection) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val welcomeText = if (!userName.isNullOrBlank()) {
        "$userName. ${stringResource(R.string.library_welcome_subtitle)}"
    } else {
        stringResource(R.string.library_guest_subtitle)
    }
    val genreRows = remember(genres) { genres.chunked(2) }
    val drawerExpanded = drawerState.targetValue == DrawerValue.Open || drawerState.currentValue == DrawerValue.Open
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KaiNavigationDrawerContent(
                currentSection = KaiSection.HOME,
                subtitle = welcomeText,
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
                    onOpenMenu = { scope.launch { drawerState.open() } }
                )
            },
            bottomBar = {
                KaiBottomBar(
                    current = KaiSection.HOME,
                    onSelect = onSectionSelected
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    LibraryHero(
                        userName = userName,
                        welcomeText = welcomeText
                    )
                }

                item {
                    LibraryShelvesIntro()
                }

                items(genreRows) { rowGenres ->
                    ShelfRow(
                        genres = rowGenres,
                        onGenreClick = onGenreClick
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryHero(
    userName: String?,
    welcomeText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, TarnishedGold)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DeepWalnut,
                            BloodWine.copy(alpha = 0.55f),
                            NightBlack
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.main_hall),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TarnishedGold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (!userName.isNullOrBlank()) userName else stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = OldIvory
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = welcomeText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OldIvory
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.main_hall_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldIvory.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(18.dp))

                DecorativeShelfLine()
            }
        }
    }
}

@Composable
private fun LibraryShelvesIntro() {
    Column(
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Text(
            text = stringResource(R.string.shelves_by_genre),
            style = MaterialTheme.typography.titleLarge,
            color = TarnishedGold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.each_shelf_leads_to_a_catalog_section),
            style = MaterialTheme.typography.bodyMedium,
            color = OldIvory
        )
    }
}

@Composable
private fun ShelfRow(
    genres: List<String>,
    onGenreClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BloodWine.copy(alpha = 0.16f),
                        DeepWalnut.copy(alpha = 0.85f),
                        Obsidian.copy(alpha = 0.98f)
                    )
                )
            )
            .padding(horizontal = 14.dp, vertical = 18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                genres.forEach { genre ->
                    GenreVolume(
                        title = genre,
                        onClick = { onGenreClick(genre) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (genres.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            DecorativeShelfLine()
        }
    }
}

@Composable
private fun GenreVolume(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(142.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DeepWalnut),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BloodWine.copy(alpha = 0.45f),
                            DeepWalnut,
                            Obsidian
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = title,
                tint = TarnishedGold,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = OldIvory
            )

            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(4.dp)
                    .background(
                        color = TarnishedGold.copy(alpha = 0.38f),
                        shape = RoundedCornerShape(6.dp)
                    )
            )
        }
    }
}

@Composable
private fun DecorativeShelfLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        TarnishedGold.copy(alpha = 0.22f),
                        BloodWine.copy(alpha = 0.35f),
                        DeepWalnut,
                        TarnishedGold.copy(alpha = 0.18f)
                    )
                )
            )
    )
}
