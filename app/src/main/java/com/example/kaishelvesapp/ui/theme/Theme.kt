package com.example.kaishelvesapp.ui.theme

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GothicDarkScheme = darkColorScheme(
    primary = TarnishedGold,
    onPrimary = NightBlack,
    secondary = BloodWine,
    onSecondary = OldIvory,
    tertiary = DarkCrimson,
    background = NightBlack,
    onBackground = OldIvory,
    surface = Obsidian,
    onSurface = OldIvory,
    surfaceVariant = DeepWalnut,
    onSurfaceVariant = FadedBone,
    outline = TarnishedGold,
    error = DarkCrimson,
    onError = OldIvory
)

@Composable
fun KaiShelvesAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GothicDarkScheme,
        typography = Typography,
        content = content
    )
}

object KaiShelvesThemeDefaults {

    @Composable
    fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = TarnishedGold,
        unfocusedBorderColor = ColdAsh,
        focusedLabelColor = TarnishedGold,
        unfocusedLabelColor = FadedBone,
        cursorColor = TarnishedGold,
        focusedTextColor = OldIvory,
        unfocusedTextColor = OldIvory,
        focusedContainerColor = DeepWalnut,
        unfocusedContainerColor = DeepWalnut,
        disabledContainerColor = DeepWalnut,
        errorBorderColor = DarkCrimson,
        errorLabelColor = DarkCrimson
    )

    @Composable
    fun primaryButtonColors() = ButtonDefaults.buttonColors(
        containerColor = BloodWine,
        contentColor = OldIvory,
        disabledContainerColor = DeepWalnut,
        disabledContentColor = FadedBone
    )

    @Composable
    fun secondaryButtonColors() = ButtonDefaults.buttonColors(
        containerColor = DeepWalnut,
        contentColor = TarnishedGold,
        disabledContainerColor = DeepWalnut,
        disabledContentColor = FadedBone
    )
}