package com.example.kaishelvesapp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold

enum class KaiSection(val label: String) {
    HOME("Inicio"),
    CATALOG("Catálogo"),
    READING("Lecturas"),
    PROFILE("Perfil")
}

@Composable
fun KaiBottomBar(
    current: KaiSection,
    onSelect: (KaiSection) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        containerColor = Obsidian
    ) {
        KaiSection.entries.forEach { item ->
            NavigationBarItem(
                selected = current == item,
                onClick = { onSelect(item) },
                label = {
                    Text(item.label)
                },
                icon = {
                    when (item) {
                        KaiSection.HOME -> Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = item.label
                        )

                        KaiSection.CATALOG -> Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = item.label
                        )

                        KaiSection.READING -> Icon(
                            imageVector = Icons.Filled.AutoStories,
                            contentDescription = item.label
                        )

                        KaiSection.PROFILE -> Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = item.label
                        )
                    }
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