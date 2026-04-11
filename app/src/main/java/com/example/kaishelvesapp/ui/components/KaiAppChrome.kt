package com.example.kaishelvesapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold

@Composable
fun KaiPrimaryTopBar(
    onOpenMenu: () -> Unit,
    onGoToProfile: () -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit
) {
    var showUserMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DeepWalnut.copy(alpha = 0.98f),
                        Obsidian.copy(alpha = 0.96f)
                    )
                )
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenMenu) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = stringResource(R.string.open_navigation_menu),
                tint = TarnishedGold
            )
        }

        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
            color = TarnishedGold
        )

        Box {
            IconButton(onClick = { showUserMenu = true }) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = stringResource(R.string.profile_menu),
                    tint = TarnishedGold
                )
            }

            DropdownMenu(
                expanded = showUserMenu,
                onDismissRequest = { showUserMenu = false },
                containerColor = Obsidian
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.profile), color = OldIvory) },
                    onClick = {
                        showUserMenu = false
                        onGoToProfile()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings_privacy), color = OldIvory) },
                    onClick = {
                        showUserMenu = false
                        onGoToSettingsPrivacy()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.logout), color = TarnishedGold) },
                    onClick = {
                        showUserMenu = false
                        onLogout()
                    }
                )
            }
        }
    }
}

@Composable
fun KaiNavigationDrawerContent(
    currentSection: KaiSection,
    subtitle: String,
    onSectionSelected: (KaiSection) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = DeepWalnut,
        drawerContentColor = OldIvory
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = TarnishedGold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = OldIvory
            )

            Spacer(modifier = Modifier.height(24.dp))

            KaiDrawerItem(
                label = stringResource(R.string.home),
                selected = currentSection == KaiSection.HOME,
                onClick = { onSectionSelected(KaiSection.HOME) }
            )

            KaiDrawerItem(
                label = stringResource(R.string.catalog),
                selected = currentSection == KaiSection.CATALOG,
                onClick = { onSectionSelected(KaiSection.CATALOG) }
            )

            KaiDrawerItem(
                label = stringResource(R.string.my_readings),
                selected = currentSection == KaiSection.READING,
                onClick = { onSectionSelected(KaiSection.READING) }
            )

            KaiDrawerItem(
                label = stringResource(R.string.reading_statistics),
                selected = currentSection == KaiSection.STATS,
                leadingIcon = { Icon(Icons.Filled.BarChart, contentDescription = null, tint = TarnishedGold) },
                onClick = { onSectionSelected(KaiSection.STATS) }
            )
        }
    }
}

@Composable
private fun KaiDrawerItem(
    label: String,
    selected: Boolean,
    leadingIcon: @Composable (() -> Unit)? = null,
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
        icon = leadingIcon,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = BloodWine.copy(alpha = 0.6f),
            selectedTextColor = TarnishedGold,
            unselectedContainerColor = Color.Transparent,
            unselectedTextColor = OldIvory
        )
    )
}
