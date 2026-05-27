package com.example.kaishelvesapp.ui.screen.help

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.kaishelvesapp.data.help.HelpKnowledgeBase
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiNavigationDrawerContent
import com.example.kaishelvesapp.ui.components.KaiPrimaryTopBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import kotlinx.coroutines.launch

@Composable
fun HelpScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScanResult: (String) -> Unit,
    userName: String? = null,
    profileImageUrl: String? = null,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onGoToProfile: () -> Unit,
    onGoToSettingsPrivacy: () -> Unit,
    onLogout: () -> Unit,
    pendingRequestCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onStartChat: () -> Unit,
    onSectionSelected: (KaiSection) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerExpanded = drawerState.targetValue == DrawerValue.Open || drawerState.currentValue == DrawerValue.Open
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            KaiNavigationDrawerContent(
                currentSection = KaiSection.HELP,
                subtitle = "Asistencia guiada para usar la app.",
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
                KaiPrimaryTopBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onSearch = onSearch,
                    onScanResult = onScanResult,
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    notificationCount = pendingRequestCount,
                    onOpenNotifications = onOpenNotifications
                )
            },
            bottomBar = {
                KaiBottomBar(
                    current = KaiSection.HELP,
                    onSelect = onSectionSelected
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Obsidian),
                    border = BorderStroke(1.dp, TarnishedGold)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        BloodWine.copy(alpha = 0.24f),
                                        DeepWalnut,
                                        Obsidian
                                    )
                                )
                            )
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Ayuda",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TarnishedGold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Inicia el chat para recibir guía contextual sobre la pantalla en la que estés.",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = OldIvory,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        TextButton(
                            onClick = onStartChat,
                            colors = ButtonDefaults.textButtonColors(contentColor = TarnishedGold)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Chat,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Iniciar chat")
                        }
                    }
                }

                HelpKnowledgeCard(
                    title = "Preguntas frecuentes",
                    items = HelpKnowledgeBase.faq.take(4)
                )
                HelpKnowledgeCard(
                    title = "Flujos guiados",
                    items = HelpKnowledgeBase.flows.take(4)
                )
                HelpKnowledgeCard(
                    title = "Errores frecuentes",
                    items = HelpKnowledgeBase.commonErrors.take(4)
                )
            }
        }
    }
}

@Composable
private fun HelpKnowledgeCard(
    title: String,
    items: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian.copy(alpha = 0.94f)),
        border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TarnishedGold
            )

            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "-",
                        modifier = Modifier.padding(end = 8.dp),
                        color = TarnishedGold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OldIvory
                    )
                }
            }
        }
    }
}
