package com.example.kaishelvesapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GothicLibraryColorScheme = darkColorScheme(
    primary = AntiqueGold,
    onPrimary = AshBlack,
    secondary = BurgundyDark,
    onSecondary = Parchment,
    tertiary = DriedRose,
    background = AshBlack,
    onBackground = OldPaper,
    surface = CharcoalBrown,
    onSurface = OldPaper,
    surfaceVariant = DarkWalnut,
    onSurfaceVariant = Dust
)

@Composable
fun KaiShelvesAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GothicLibraryColorScheme,
        typography = Typography,
        content = content
    )
}