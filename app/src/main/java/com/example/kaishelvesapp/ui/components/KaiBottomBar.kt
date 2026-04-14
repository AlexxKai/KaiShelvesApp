package com.example.kaishelvesapp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold

enum class KaiSection {
    HOME,
    CATALOG,
    LISTS,
    READING,
    PROFILE,
    STATS
}

@Composable
fun KaiBottomBar(
    current: KaiSection,
    onSelect: (KaiSection) -> Unit
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = Obsidian
    ) {
        val items = listOf(
            Triple(KaiSection.HOME, stringResource(R.string.home), Icons.Filled.Home),
            Triple(KaiSection.CATALOG, stringResource(R.string.catalog), Icons.AutoMirrored.Filled.MenuBook),
            Triple(KaiSection.LISTS, stringResource(R.string.my_books), Icons.AutoMirrored.Filled.LibraryBooks)
        )

        items.forEach { (section, label, icon) ->
            NavigationBarItem(
                selected = current == section,
                onClick = { onSelect(section) },
                label = {
                    Text(label)
                },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TarnishedGold,
                    selectedTextColor = TarnishedGold,
                    indicatorColor = BloodWine,
                    unselectedIconColor = OldIvory,
                    unselectedTextColor = OldIvory
                )
            )
        }
    }
}
