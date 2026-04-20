package com.example.kaishelvesapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.data.repository.GuestMergeDecision
import com.example.kaishelvesapp.data.repository.GuestMergeStrategy
import com.example.kaishelvesapp.ui.theme.KaiShelvesThemeDefaults
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold
import androidx.compose.ui.unit.dp

@Composable
fun GuestMergeDecisionDialog(
    decision: GuestMergeDecision,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onChoose: (GuestMergeStrategy) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Obsidian),
            border = BorderStroke(1.dp, TarnishedGold)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.guest_merge_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = TarnishedGold,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = OldIvory
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.guest_merge_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldIvory
                )

                Spacer(modifier = Modifier.height(16.dp))

                MergeSummaryBlock(
                    title = stringResource(R.string.guest_merge_local_summary),
                    customLists = decision.localSummary.customLists,
                    organizedBooks = decision.localSummary.organizedBooks,
                    readBooks = decision.localSummary.readBooks,
                    tags = decision.localSummary.tags
                )

                Spacer(modifier = Modifier.height(12.dp))

                MergeSummaryBlock(
                    title = stringResource(R.string.guest_merge_cloud_summary),
                    customLists = decision.cloudSummary.customLists,
                    organizedBooks = decision.cloudSummary.organizedBooks,
                    readBooks = decision.cloudSummary.readBooks,
                    tags = decision.cloudSummary.tags
                )

                Spacer(modifier = Modifier.height(18.dp))

                TextButton(
                    onClick = { onChoose(GuestMergeStrategy.MERGE) },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.guest_merge_action_merge),
                        color = TarnishedGold
                    )
                }

                OutlinedButton(
                    onClick = { onChoose(GuestMergeStrategy.KEEP_CLOUD) },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, TarnishedGold.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = stringResource(R.string.guest_merge_action_keep_cloud),
                        color = OldIvory
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                androidx.compose.material3.Button(
                    onClick = { onChoose(GuestMergeStrategy.REPLACE_CLOUD) },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = KaiShelvesThemeDefaults.primaryButtonColors()
                ) {
                    Text(stringResource(R.string.guest_merge_action_replace_cloud))
                }
            }
        }
    }
}

@Composable
private fun MergeSummaryBlock(
    title: String,
    customLists: Int,
    organizedBooks: Int,
    readBooks: Int,
    tags: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TarnishedGold
        )
        Text(
            text = stringResource(
                R.string.guest_merge_summary_format,
                customLists,
                organizedBooks,
                readBooks,
                tags
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = OldIvory
        )
    }
}
