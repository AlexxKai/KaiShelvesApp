package com.example.kaishelvesapp.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

data class GuestUiRestrictions(
    val disabledSections: Set<KaiSection> = emptySet(),
    val onBlockedSectionClick: ((KaiSection) -> Unit)? = null
)

val LocalGuestUiRestrictions = staticCompositionLocalOf {
    GuestUiRestrictions()
}
