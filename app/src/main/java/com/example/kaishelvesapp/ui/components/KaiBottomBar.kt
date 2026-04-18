package com.example.kaishelvesapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold

enum class KaiSection {
    HOME,
    MY_BOOKS,
    DISCOVER,
    SEARCH,
    LIBRARY,
    PROFILE,
    STATS,
    FRIENDS,
    GROUPS,
    CHALLENGES,
    FOR_YOU,
    HELP
}

@Composable
fun KaiBottomBar(
    current: KaiSection,
    onSelect: (KaiSection) -> Unit
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val compactLayout = screenWidthDp < 380
    val veryCompactLayout = screenWidthDp < 340
    val horizontalPadding = if (veryCompactLayout) 6.dp else if (compactLayout) 10.dp else 14.dp
    val itemHorizontalPadding = if (veryCompactLayout) 6.dp else if (compactLayout) 8.dp else 12.dp
    val itemVerticalPadding = if (compactLayout) 6.dp else 8.dp
    val iconSize = if (compactLayout) 18.dp else 20.dp
    val labelStyle = if (veryCompactLayout) {
        MaterialTheme.typography.labelSmall
    } else {
        MaterialTheme.typography.labelMedium
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .navigationBarsPadding(),
        color = Obsidian,
        shape = RoundedCornerShape(0.dp)
    ) {
        val items = listOf(
            Triple(KaiSection.HOME, stringResource(R.string.home), Icons.Filled.Home),
            Triple(KaiSection.MY_BOOKS, stringResource(R.string.my_books), Icons.AutoMirrored.Filled.LibraryBooks),
            Triple(KaiSection.DISCOVER, stringResource(R.string.discover), Icons.AutoMirrored.Filled.MenuBook),
            Triple(KaiSection.SEARCH, stringResource(R.string.search_tab), Icons.Filled.Search)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = horizontalPadding, end = horizontalPadding, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (section, label, icon) ->
                val selected = current == section
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(min = 0.dp)
                            .background(
                                color = if (selected) BloodWine else Obsidian,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onSelect(section) }
                            .padding(horizontal = itemHorizontalPadding, vertical = itemVerticalPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.size(iconSize),
                            tint = if (selected) TarnishedGold else OldIvory
                        )
                        Text(
                            text = label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            color = if (selected) TarnishedGold else OldIvory,
                            modifier = Modifier.padding(top = 4.dp),
                            style = labelStyle
                        )
                    }
                }
            }
        }
    }
}
