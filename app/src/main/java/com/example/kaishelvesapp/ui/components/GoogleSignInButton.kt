package com.example.kaishelvesapp.ui.components

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

@Composable
fun GoogleSignInButton(
    enabled: Boolean,
    onIdTokenReceived: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()

    OutlinedButton(
        onClick = {
            if (activity == null) {
                onError(context.getString(R.string.google_sign_in_failed))
                return@OutlinedButton
            }

            val webClientId = context.getString(R.string.google_web_client_id).trim()
            if (webClientId.isBlank()) {
                onError(context.getString(R.string.google_sign_in_not_available))
                return@OutlinedButton
            }

            scope.launch {
                runCatching {
                    val credentialManager = CredentialManager.create(context)
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .setAutoSelectEnabled(false)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    credentialManager.getCredential(
                        context = activity,
                        request = request
                    )
                }.onSuccess { result ->
                    handleGoogleCredential(
                        result = result,
                        onIdTokenReceived = onIdTokenReceived,
                        onError = onError,
                        context = context
                    )
                }.onFailure {
                    onError(context.getString(R.string.google_sign_in_cancelled))
                }
            }
        },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        border = BorderStroke(1.dp, TarnishedGold),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Obsidian,
            contentColor = OldIvory
        )
    ) {
        Text(text = stringResource(R.string.continue_with_google))
    }
}

private fun handleGoogleCredential(
    result: GetCredentialResponse,
    onIdTokenReceived: (String) -> Unit,
    onError: (String) -> Unit,
    context: Context
) {
    val credential = result.credential

    if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        onError(context.getString(R.string.google_sign_in_failed))
        return
    }

    try {
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        onIdTokenReceived(googleCredential.idToken)
    } catch (_: GoogleIdTokenParsingException) {
        onError(context.getString(R.string.google_sign_in_failed))
    }
}

private fun Context.findActivity(): ComponentActivity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) {
            return current
        }
        current = current.baseContext
    }
    return null
}
