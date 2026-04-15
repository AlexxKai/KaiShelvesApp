package com.example.kaishelvesapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        color = Obsidian,
        shape = RoundedCornerShape(0.dp)
    ) {
        val items = listOf(
            Triple(KaiSection.HOME, stringResource(R.string.home), Icons.Filled.Home),
            Triple(KaiSection.CATALOG, stringResource(R.string.catalog), Icons.AutoMirrored.Filled.MenuBook),
            Triple(KaiSection.LISTS, stringResource(R.string.my_books), Icons.AutoMirrored.Filled.LibraryBooks)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (section, label, icon) ->
                val selected = current == section
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                color = if (selected) BloodWine else Obsidian,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onSelect(section) }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.size(20.dp),
                            tint = if (selected) TarnishedGold else OldIvory
                        )
                        Text(
                            text = label,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            color = if (selected) TarnishedGold else OldIvory,
                            modifier = Modifier.padding(top = 4.dp),
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
