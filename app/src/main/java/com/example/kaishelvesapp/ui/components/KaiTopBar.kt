package com.example.kaishelvesapp.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.kaishelvesapp.ui.theme.Obsidian
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold

@Composable
fun KaiTopBar(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = null,
    navigationContentDescription: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    actionIconContentDescription: String? = null,
    onActionIconClick: (() -> Unit)? = null,
    centerTitle: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Obsidian),
        border = BorderStroke(1.dp, TarnishedGold)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            if (navigationIcon != null || actionIcon != null || centerTitle) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (navigationIcon != null && onNavigationClick != null) {
                            IconButton(onClick = onNavigationClick) {
                                Icon(
                                    imageVector = navigationIcon,
                                    contentDescription = navigationContentDescription,
                                    tint = TarnishedGold
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(40.dp))
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (actionIcon != null && onActionIconClick != null) {
                            IconButton(onClick = onActionIconClick) {
                                Icon(
                                    imageVector = actionIcon,
                                    contentDescription = actionIconContentDescription,
                                    tint = TarnishedGold
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(40.dp))
                        }
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TarnishedGold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 52.dp)
                    )
                }
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TarnishedGold
                )
            }

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OldIvory,
                    textAlign = if (centerTitle) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }

            if (!actionText.isNullOrBlank() && onActionClick != null) {
                TextButton(
                    onClick = onActionClick,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(actionText, color = TarnishedGold)
                }
            }
        }
    }
}
