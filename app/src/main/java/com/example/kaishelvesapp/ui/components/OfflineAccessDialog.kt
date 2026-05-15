package com.example.kaishelvesapp.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold

@Composable
fun OfflineAccessDialog(
    isRetrying: Boolean,
    onRetry: () -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = {
            Toast.makeText(
                context,
                context.getString(R.string.home_offline_outside_message),
                Toast.LENGTH_SHORT
            ).show()
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(28.dp),
            color = Obsidian,
            border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.home_offline_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = TarnishedGold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.home_offline_dialog_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = OldIvory,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenLibrary,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, TarnishedGold)
                    ) {
                        Text(
                            text = stringResource(R.string.library),
                            color = TarnishedGold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = onRetry,
                        enabled = !isRetrying,
                        modifier = Modifier.weight(1f),
                        colors = KaiShelvesThemeDefaults.primaryButtonColors()
                    ) {
                        if (isRetrying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = OldIvory,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(text = stringResource(R.string.retry))
                        }
                    }
                }
            }
        }
    }
}
