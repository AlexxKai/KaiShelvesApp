package com.example.kaishelvesapp.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
        Text(
            text = "Sesión iniciada correctamente",
            style = MaterialTheme.typography.headlineSmall
        )

        if (!userName.isNullOrBlank()) {
            Text(
                text = "Bienvenido, $userName",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = onGoToCatalog,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Ir al catálogo")
        }

        Button(
            onClick = onLogout,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("Cerrar sesión")
        }
    }
}