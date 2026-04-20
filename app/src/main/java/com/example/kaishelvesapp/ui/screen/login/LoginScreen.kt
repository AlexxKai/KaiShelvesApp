package com.example.kaishelvesapp.ui.screen.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.ui.components.GuestMergeDecisionDialog
import com.example.kaishelvesapp.ui.components.GoogleSignInButton
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.example.kaishelvesapp.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    uiState.pendingGuestMergeDecision?.let { decision ->
        GuestMergeDecisionDialog(
            decision = decision,
            isLoading = uiState.isLoading,
            onDismiss = viewModel::dismissPendingGuestMergeDecision,
            onChoose = viewModel::resolvePendingGuestMerge
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(paddingValues)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.login_screen_title),
            style = MaterialTheme.typography.headlineLarge,
            color = TarnishedGold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.login_subtitle),
            color = OldIvory
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Obsidian),
            border = BorderStroke(1.dp, TarnishedGold)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.loginIdentifier,
                    onValueChange = viewModel::onLoginIdentifierChange,
                    label = { Text(stringResource(R.string.email_or_username)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = KaiShelvesThemeDefaults.outlinedTextFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text(stringResource(R.string.password)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = KaiShelvesThemeDefaults.outlinedTextFieldColors(),
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = { viewModel.login() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    colors = KaiShelvesThemeDefaults.primaryButtonColors()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = OldIvory)
                    } else {
                        Text(stringResource(R.string.login))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = TarnishedGold.copy(alpha = 0.35f))

                Spacer(modifier = Modifier.height(16.dp))

                GoogleSignInButton(
                    enabled = !uiState.isLoading,
                    onIdTokenReceived = viewModel::loginWithGoogle,
                    onError = viewModel::showError
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = TarnishedGold.copy(alpha = 0.35f))

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.guestUsername,
                    onValueChange = viewModel::onGuestUsernameChange,
                    label = { Text(stringResource(R.string.username)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = KaiShelvesThemeDefaults.outlinedTextFieldColors()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.guest_mode_warning),
                    color = OldIvory.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.continueAsGuest() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    colors = KaiShelvesThemeDefaults.primaryButtonColors()
                ) {
                    Text(stringResource(R.string.continue_without_account))
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        viewModel.clearError()
                        onGoToRegister()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = stringResource(R.string.create_account),
                        color = OldIvory
                    )
                }
            }
        }
    }
}
