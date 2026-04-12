package com.example.kaishelvesapp.ui.screen.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.ui.components.KaiBottomBar
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
    genres: List<String>,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onGenreClick: (String) -> Unit,
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
            ModalDrawerSheet(
                drawerContainerColor = DeepWalnut,
                drawerContentColor = OldIvory
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                ) {
                    DrawerHeader(
                        userName = userName,
                        welcomeText = welcomeText,
                        expanded = drawerExpanded
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    DrawerSectionItem(
                        label = stringResource(R.string.home),
                        selected = true,
                        onClick = { scope.launch { drawerState.close() } }
                    )

                    DrawerSectionItem(
                        label = stringResource(R.string.catalog),
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onSectionSelected(KaiSection.CATALOG)
                        }
                    )

                    DrawerSectionItem(
                        label = stringResource(R.string.lists),
                        selected = false,
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                                contentDescription = null,
                                tint = TarnishedGold
                            )
                        },
                        onClick = {
                            scope.launch { drawerState.close() }
                            onSectionSelected(KaiSection.LISTS)
                        }
                    )

                    DrawerSectionItem(
                        label = stringResource(R.string.my_readings),
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onSectionSelected(KaiSection.READING)
                        }
                    )

                    DrawerSectionItem(
                        label = stringResource(R.string.reading_statistics),
                        selected = false,
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.BarChart,
                                contentDescription = null,
                                tint = TarnishedGold
                            )
                        },
                        onClick = {
                            scope.launch { drawerState.close() }
                            onSectionSelected(KaiSection.STATS)
                        }
                    )
                }
            }
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
private fun DrawerSectionItem(
    label: String,
    selected: Boolean,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium
            )
        },
        selected = selected,
        icon = icon,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = BloodWine.copy(alpha = 0.6f),
            selectedTextColor = TarnishedGold,
            unselectedContainerColor = Color.Transparent,
            unselectedTextColor = OldIvory
        ),
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun DrawerHeader(
    userName: String?,
    welcomeText: String,
    expanded: Boolean
) {
    val headerAlpha = animateFloatAsState(
        targetValue = if (expanded) 1f else 0.72f,
        animationSpec = tween(durationMillis = 450),
        label = "drawerHeaderAlpha"
    )
    val headerScale = animateFloatAsState(
        targetValue = if (expanded) 1f else 0.94f,
        animationSpec = tween(durationMillis = 500),
        label = "drawerHeaderScale"
    )
    val headerOffset = animateFloatAsState(
        targetValue = if (expanded) 0f else -12f,
        animationSpec = tween(durationMillis = 520),
        label = "drawerHeaderOffset"
    )
    val displayName = if (!userName.isNullOrBlank()) userName else stringResource(R.string.app_name)
    val monogram = displayName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "KS" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = headerOffset.value.dp)
            .scale(headerScale.value),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BloodWine.copy(alpha = 0.62f * headerAlpha.value),
                            DeepWalnut,
                            Obsidian
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .blur(16.dp)
                            .background(
                                color = TarnishedGold.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(24.dp)
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        TarnishedGold.copy(alpha = 0.32f),
                                        BloodWine.copy(alpha = 0.52f),
                                        DeepWalnut
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = monogram,
                            style = MaterialTheme.typography.titleMedium,
                            color = TarnishedGold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TarnishedGold
                    )

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OldIvory
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = welcomeText,
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory.copy(alpha = 0.95f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            DecorativeShelfLine()
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
