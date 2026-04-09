package com.example.kaishelvesapp.ui.screen.library

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.components.KaiTopBar
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.NightBlack
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold

@Composable
fun LibraryScreen(
    userName: String?,
    genres: List<String>,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onGenreClick: (String) -> Unit,
    onLogout: () -> Unit,
    onSectionSelected: (KaiSection) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            KaiBottomBar(
                current = KaiSection.HOME,
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
                title = stringResource(R.string.great_library),
                subtitle = if (!userName.isNullOrBlank()) {
                    "$userName. ${stringResource(R.string.library_welcome_subtitle)}"
                } else {
                    stringResource(R.string.library_guest_subtitle)
                },
                actionText = stringResource(R.string.logout),
                onActionClick = onLogout
            )

            Spacer(modifier = Modifier.height(16.dp))

            ChamberCard()

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Obsidian.copy(alpha = 0.96f)),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, TarnishedGold)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = stringResource(R.string.shelves_by_genre),
                        style = MaterialTheme.typography.titleLarge,
                        color = TarnishedGold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.each_shelf_leads_to_a_catalog_section),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OldIvory
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.height(340.dp)
                    ) {
                        items(genres) { genre ->
                            BookshelfCard(
                                title = genre,
                                onClick = { onGenreClick(genre) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChamberCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, TarnishedGold)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DeepWalnut,
                            BloodWine.copy(alpha = 0.55f),
                            NightBlack
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = stringResource(R.string.main_hall),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TarnishedGold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.main_hall_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = OldIvory,
                    modifier = Modifier.width(260.dp)
                )
            }

            RowOfShelves(
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun RowOfShelves(
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        DecorativeShelf(height = 70.dp)
        DecorativeShelf(height = 95.dp)
        DecorativeShelf(height = 78.dp)
        DecorativeShelf(height = 110.dp)
        DecorativeShelf(height = 86.dp)
    }
}

@Composable
private fun DecorativeShelf(
    height: androidx.compose.ui.unit.Dp
) {
    Card(
        modifier = Modifier
            .width(54.dp)
            .height(height),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DeepWalnut),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.75f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            BloodWine.copy(alpha = 0.85f),
                            DeepWalnut,
                            Obsidian
                        )
                    )
                )
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(8.dp)
                        .background(
                            color = TarnishedGold.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun BookshelfCard(
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DeepWalnut),
        border = BorderStroke(1.dp, TarnishedGold)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BloodWine.copy(alpha = 0.42f),
                            DeepWalnut,
                            Obsidian
                        )
                    )
                )
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = title,
                tint = TarnishedGold,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = OldIvory
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .background(
                        color = TarnishedGold.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}