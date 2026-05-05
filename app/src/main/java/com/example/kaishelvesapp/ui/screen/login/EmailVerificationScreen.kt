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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold

@Composable
fun EmailVerificationScreen(
    email: String,
    isLoading: Boolean,
    message: String?,
    isError: Boolean,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onAlreadyVerified: () -> Unit,
    onResendVerificationEmail: () -> Unit,
    onDismiss: () -> Unit
) {
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
            text = stringResource(R.string.email_verification_title),
            style = MaterialTheme.typography.headlineLarge,
            color = TarnishedGold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Obsidian),
            border = BorderStroke(1.dp, TarnishedGold)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.email_verification_sent_to),
                    style = MaterialTheme.typography.bodyLarge,
                    color = OldIvory,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = email,
                    style = MaterialTheme.typography.titleMedium,
                    color = TarnishedGold,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                message?.let {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = it,
                        color = if (isError) MaterialTheme.colorScheme.error else OldIvory,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onAlreadyVerified,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = KaiShelvesThemeDefaults.primaryButtonColors()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = OldIvory)
                    } else {
                        Text(stringResource(R.string.email_verification_already_verified))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onResendVerificationEmail,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    border = BorderStroke(1.dp, TarnishedGold)
                ) {
                    Text(
                        text = stringResource(R.string.email_verification_resend),
                        color = TarnishedGold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.email_verification_close),
                        color = OldIvory
                    )
                }
            }
        }
    }
}
