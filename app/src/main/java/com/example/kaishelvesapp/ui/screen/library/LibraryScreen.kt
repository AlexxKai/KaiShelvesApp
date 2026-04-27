package com.example.kaishelvesapp.ui.screen.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.PsychologyAlt
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    pendingRequestCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onSectionSelected: (KaiSection) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showLibraryIntro by rememberSaveable { mutableStateOf(true) }
    val welcomeText = stringResource(R.string.library_guest_subtitle)
    val drawerExpanded = drawerState.targetValue == DrawerValue.Open || drawerState.currentValue == DrawerValue.Open
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KaiNavigationDrawerContent(
                currentSection = KaiSection.SEARCH,
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
                if (!showLibraryIntro) {
                    KaiPrimaryTopBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        onSearch = onSearch,
                        onScanResult = onScanResult,
                        onOpenMenu = { scope.launch { drawerState.open() } },
                        notificationCount = pendingRequestCount,
                        onOpenNotifications = onOpenNotifications
                    )
                }
            },
            bottomBar = {
                if (!showLibraryIntro) {
                    KaiBottomBar(
                        current = KaiSection.SEARCH,
                        onSelect = onSectionSelected
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(genres) { genre ->
                        ShelfVolumeRow(
                            genre = genre,
                            onGenreClick = onGenreClick
                        )
                    }
                }

                LibraryIntroOverlay(
                    visible = showLibraryIntro,
                    welcomeText = welcomeText,
                    onDismiss = { showLibraryIntro = false }
                )
            }
        }
    }
}

@Composable
private fun LibraryIntroOverlay(
    visible: Boolean,
    welcomeText: String,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(380)) + scaleIn(
            initialScale = 0.96f,
            animationSpec = tween(380)
        ),
        exit = fadeOut(animationSpec = tween(760))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NightBlack.copy(alpha = 0.92f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss
                )
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    visible = visible,
                    enter = slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(520)
                    ) + fadeIn(animationSpec = tween(360)),
                    exit = slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(760)
                    )
                ) {
                    CurtainPanel(
                        modifier = Modifier.fillMaxSize(),
                        reversed = false
                    )
                }

                AnimatedVisibility(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    visible = visible,
                    enter = slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(520)
                    ) + fadeIn(animationSpec = tween(360)),
                    exit = slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(760)
                    )
                ) {
                    CurtainPanel(
                        modifier = Modifier.fillMaxSize(),
                        reversed = true
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {}
                    )
            ) {
                LibraryHero(
                    welcomeText = welcomeText
                )
            }

            Text(
                text = stringResource(R.string.library_intro_dismiss_hint),
                style = MaterialTheme.typography.bodySmall,
                color = TarnishedGold.copy(alpha = 0.56f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
            )
        }
    }
}

@Composable
private fun CurtainPanel(
    reversed: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = if (reversed) {
        listOf(
            DeepWalnut.copy(alpha = 0.92f),
            BloodWine.copy(alpha = 0.78f),
            NightBlack
        )
    } else {
        listOf(
            NightBlack,
            BloodWine.copy(alpha = 0.78f),
            DeepWalnut.copy(alpha = 0.92f)
        )
    }

    Box(
        modifier = modifier.background(
            brush = Brush.horizontalGradient(colors = colors)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .width(if (index % 2 == 0) 10.dp else 5.dp)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    NightBlack.copy(alpha = 0.18f),
                                    TarnishedGold.copy(alpha = 0.1f),
                                    NightBlack.copy(alpha = 0.24f)
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun LibraryHero(
    welcomeText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            NightBlack.copy(alpha = 0.28f),
                            BloodWine.copy(alpha = 0.22f),
                            DeepWalnut.copy(alpha = 0.26f),
                            BloodWine.copy(alpha = 0.18f),
                            NightBlack.copy(alpha = 0.24f)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(6) { index ->
                    Box(
                        modifier = Modifier
                            .width(if (index % 2 == 0) 8.dp else 4.dp)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        TarnishedGold.copy(alpha = 0.08f),
                                        NightBlack.copy(alpha = 0.18f)
                                    )
                                )
                            )
                    )
                }
            }

            Column {
                Text(
                    text = stringResource(R.string.main_hall),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TarnishedGold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = welcomeText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OldIvory.copy(alpha = 0.94f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.main_hall_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldIvory.copy(alpha = 0.9f)
                )
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
private fun ShelfVolumeRow(
    genre: String,
    onGenreClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BloodWine.copy(alpha = 0.16f),
                        DeepWalnut.copy(alpha = 0.85f),
                        Obsidian.copy(alpha = 0.98f)
                    )
                )
            )
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Column {
            GenreVolume(
                title = genre,
                onClick = { onGenreClick(genre) },
                modifier = Modifier.fillMaxWidth(),
            )

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
    val icon = remember(title) { genreIconFor(title) }
    val accent = remember(title) { genreAccentFor(title) }
    val volumeTransition = rememberInfiniteTransition(label = "genre_volume_$title")
    val glowAlpha by volumeTransition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "genre_glow_$title"
    )
    val iconScale by volumeTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "genre_icon_scale_$title"
    )

    Card(
        modifier = modifier
            .height(112.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.68f + glowAlpha * 0.34f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = glowAlpha),
                            BloodWine.copy(alpha = 0.18f + glowAlpha * 0.18f),
                            Obsidian
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(18.dp)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.8f),
                                BloodWine.copy(alpha = 0.52f),
                                NightBlack.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeepWalnut.copy(alpha = 0.68f + glowAlpha * 0.28f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier
                        .size(27.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 36.dp, end = 84.dp, top = 18.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = OldIvory,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .height(4.dp)
                        .background(
                            color = accent.copy(alpha = 0.52f),
                            shape = RoundedCornerShape(6.dp)
                        )
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(34.dp)
                    .height(96.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                TarnishedGold.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
            )

            GenreSpineGleam(
                accent = accent,
                glowAlpha = glowAlpha,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 22.dp)
            )
        }
    }
}

@Composable
private fun GenreSpineGleam(
    accent: Color,
    glowAlpha: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(2.dp)
            .height(70.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        accent.copy(alpha = 0.24f + glowAlpha * 0.34f),
                        TarnishedGold.copy(alpha = 0.12f + glowAlpha * 0.16f),
                        Color.Transparent
                    )
                )
            )
    )
}

private fun genreIconFor(title: String): ImageVector {
    return when (title.lowercase()) {
        "fantasia", "fantasy" -> Icons.Filled.AutoAwesome
        "misterio", "mystery" -> Icons.Filled.Visibility
        "terror", "horror" -> Icons.Filled.PsychologyAlt
        "romance" -> Icons.Filled.Favorite
        "ciencia ficcion", "science fiction" -> Icons.Filled.RocketLaunch
        "historia", "history" -> Icons.Filled.AccountBalance
        "aventura", "adventure" -> Icons.Filled.Explore
        "poesia", "poetry" -> Icons.Filled.FormatQuote
        else -> Icons.AutoMirrored.Filled.MenuBook
    }
}

private fun genreAccentFor(title: String): Color {
    return when (title.lowercase()) {
        "fantasia", "fantasy" -> Color(0xFFB5965C)
        "misterio", "mystery" -> Color(0xFF8D8174)
        "terror", "horror" -> Color(0xFF8E3144)
        "romance" -> Color(0xFFA85A64)
        "ciencia ficcion", "science fiction" -> Color(0xFF7B8C92)
        "historia", "history" -> Color(0xFFAA8351)
        "aventura", "adventure" -> Color(0xFF7D8B5F)
        "poesia", "poetry" -> Color(0xFFB8AA98)
        else -> TarnishedGold
    }
}

@Composable
private fun DecorativeShelfLine(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
