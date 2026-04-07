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
import com.example.kaishelvesapp.ui.theme.AntiqueGold
import com.example.kaishelvesapp.ui.theme.CharcoalBrown
import com.example.kaishelvesapp.ui.theme.OldPaper

@Composable
fun HomeScreen(
    userName: String?,
    onGoToCatalog: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CharcoalBrown),
            border = BorderStroke(1.dp, AntiqueGold)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Kai Shelves",
                    style = MaterialTheme.typography.headlineLarge,
                    color = AntiqueGold
                )

                Text(
                    text = if (!userName.isNullOrBlank()) {
                        "Bienvenido, $userName"
                    } else {
                        "Bienvenido a tu biblioteca oscura"
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    color = OldPaper
                )

                Button(
                    onClick = onGoToCatalog,
                    modifier = Modifier.padding(top = 20.dp)
                ) {
                    Text("Explorar catálogo")
                }

                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.padding(top = 12.dp),
                    border = BorderStroke(1.dp, AntiqueGold)
                ) {
                    Text("Cerrar sesión")
                }
            }
        }
    }
}