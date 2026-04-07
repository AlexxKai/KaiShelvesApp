package com.example.kaishelvesapp.ui.screen.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.ui.components.KaiBottomBar
import com.example.kaishelvesapp.ui.components.KaiSection
import com.example.kaishelvesapp.ui.components.KaiTopBar
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onSectionSelected: (KaiSection) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        if (uiState.user == null && uiState.isLoggedIn) {
            viewModel.loadCurrentUserProfile()
        }
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }

        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            KaiBottomBar(
                current = KaiSection.PROFILE,
                onSelect = onSectionSelected
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            KaiTopBar(
                title = "Perfil del lector",
                subtitle = "Consulta y edita tu identidad dentro del archivo."
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading && uiState.user == null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = TarnishedGold)
                    }
                }

                else -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Obsidian),
                        border = BorderStroke(1.dp, TarnishedGold)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            if (uiState.isEditingProfile) {
                                OutlinedTextField(
                                    value = uiState.username,
                                    onValueChange = viewModel::onUsernameChange,
                                    label = { Text("Nombre de usuario") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = KaiShelvesThemeDefaults.outlinedTextFieldColors(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                ProfileLine("Email", uiState.user?.email ?: "Sin email")
                                ProfileLine("UID", uiState.user?.uid ?: "No disponible")

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = { viewModel.saveProfileChanges() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = KaiShelvesThemeDefaults.primaryButtonColors(),
                                    enabled = !uiState.isLoading
                                ) {
                                    if (uiState.isLoading) {
                                        CircularProgressIndicator(color = OldIvory)
                                    } else {
                                        Text("Guardar cambios")
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = { viewModel.cancelEditingProfile() },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, TarnishedGold)
                                ) {
                                    Text("Cancelar", color = TarnishedGold)
                                }
                            } else {
                                ProfileLine("Usuario", uiState.user?.usuario ?: "Sin nombre")
                                ProfileLine("Email", uiState.user?.email ?: "Sin email")
                                ProfileLine("UID", uiState.user?.uid ?: "No disponible")

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = { viewModel.startEditingProfile() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = KaiShelvesThemeDefaults.secondaryButtonColors()
                                ) {
                                    Text("Editar perfil")
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = onLogout,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = KaiShelvesThemeDefaults.primaryButtonColors()
                                ) {
                                    Text("Cerrar sesión")
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = onBack,
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, TarnishedGold)
                                ) {
                                    Text("Volver", color = TarnishedGold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileLine(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TarnishedGold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = OldIvory
        )
    }
}