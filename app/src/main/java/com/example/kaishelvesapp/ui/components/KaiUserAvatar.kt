package com.example.kaishelvesapp.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.kaishelvesapp.data.security.ProfileImageCodec
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.DeepWalnut
import com.example.kaishelvesapp.ui.theme.TarnishedGold

@Composable
fun KaiUserAvatar(
    displayName: String,
    imageUrl: String,
    modifier: Modifier = Modifier,
    placeholderContent: @Composable BoxScope.() -> Unit = {
        Text(
            text = displayName
                .trim()
                .firstOrNull()
                ?.uppercase()
                ?: "K",
            style = MaterialTheme.typography.titleMedium,
            color = TarnishedGold
        )
    }
) {
    val decryptedBitmap = remember(imageUrl) {
        if (imageUrl.isBlank() || imageUrl.startsWith("content://") || imageUrl.startsWith("http")) {
            null
        } else {
            ProfileImageCodec.decryptImageBytes(imageUrl)
                ?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .blur(16.dp)
                .background(
                    color = TarnishedGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(24.dp)
                )
        )

        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            TarnishedGold.copy(alpha = 0.32f),
                            BloodWine.copy(alpha = 0.52f),
                            DeepWalnut
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = TarnishedGold.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                decryptedBitmap != null -> {
                    Image(
                        bitmap = decryptedBitmap.asImageBitmap(),
                        contentDescription = "Avatar de $displayName",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                imageUrl.isNotBlank() -> {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Avatar de $displayName",
                    modifier = Modifier.fillMaxSize()
                )
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                        content = placeholderContent
                    )
                }
            }
        }
    }
}
