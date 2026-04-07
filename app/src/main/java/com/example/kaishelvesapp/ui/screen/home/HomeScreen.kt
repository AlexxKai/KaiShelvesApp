package com.example.kaishelvesapp.ui.screen.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kaishelvesapp.ui.components.KaiScreen
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold

@Composable
fun HomeScreen(
    userName: String?,
    onGoToCatalog: () -> Unit,
    onGoToReadingList: () -> Unit,
    onGoToProfile: () -> Unit,
    onLogout: () -> Unit,
    onSectionSelected: (KaiSection) -> Unit
) {
    KaiScreen(
        title = "Kai Shelves",
        subtitle = if (!userName.isNullOrBlank()) "Bienvenido, $userName" else "Tu biblioteca oscura",
        currentSection = KaiSection.HOME,
        onSectionSelected = onSectionSelected
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Obsidian),
                border = BorderStroke(1.dp, TarnishedGold)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Desde aquí puedes explorar el archivo, revisar tus lecturas y consultar tu perfil.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OldIvory
                    )

                    Button(
                        onClick = onGoToCatalog,
                        modifier = Modifier.padding(top = 20.dp),
                        colors = KaiShelvesThemeDefaults.primaryButtonColors()
                    ) {
                        Text("Explorar catálogo")
                    }

                    Button(
                        onClick = onGoToReadingList,
                        modifier = Modifier.padding(top = 12.dp),
                        colors = KaiShelvesThemeDefaults.secondaryButtonColors()
                    ) {
                        Text("Mis lecturas")
                    }

                    Button(
                        onClick = onGoToProfile,
                        modifier = Modifier.padding(top = 12.dp),
                        colors = KaiShelvesThemeDefaults.secondaryButtonColors()
                    ) {
                        Text("Perfil")
                    }

                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.padding(top = 12.dp),
                        border = BorderStroke(1.dp, TarnishedGold)
                    ) {
                        Text("Cerrar sesión", color = TarnishedGold)
                    }
                }
            }
        }
    }
}