package com.example.kaishelvesapp.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
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
    size: Dp = 52.dp,
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
    val normalizedImageUrl = remember(imageUrl) { imageUrl.trim() }
    val dataUriBitmap = remember(normalizedImageUrl) {
        if (!normalizedImageUrl.startsWith("data:image", ignoreCase = true)) {
            null
        } else {
            runCatching {
                val base64Payload = normalizedImageUrl.substringAfter("base64,", "")
                if (base64Payload.isBlank()) {
                    null
                } else {
                    val bytes = Base64.decode(base64Payload, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }.getOrNull()
        }
    }
    val isUriBackedImage = remember(normalizedImageUrl) {
        normalizedImageUrl.startsWith("http", ignoreCase = true) ||
            normalizedImageUrl.startsWith("content://", ignoreCase = true) ||
            normalizedImageUrl.startsWith("file://", ignoreCase = true) ||
            normalizedImageUrl.startsWith("android.resource://", ignoreCase = true) ||
            normalizedImageUrl.startsWith("data:image", ignoreCase = true)
    }

    val localBitmap = remember(normalizedImageUrl, isUriBackedImage, dataUriBitmap) {
        if (dataUriBitmap != null) {
            dataUriBitmap
        } else if (normalizedImageUrl.isBlank() || isUriBackedImage) {
            null
        } else {
            val decryptedBytes = ProfileImageCodec.decryptImageBytes(normalizedImageUrl)
            val rawBase64Bytes = if (decryptedBytes == null) {
                runCatching {
                    Base64.decode(normalizedImageUrl, Base64.DEFAULT)
                }.getOrNull()
            } else {
                null
            }

            (decryptedBytes ?: rawBase64Bytes)
                ?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size + 16.dp)
                .blur(16.dp)
                .background(
                    color = TarnishedGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(24.dp)
                )
        )

        Box(
            modifier = Modifier
                .size(size)
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
                localBitmap != null -> {
                    Image(
                        bitmap = localBitmap.asImageBitmap(),
                        contentDescription = "Avatar de $displayName",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                normalizedImageUrl.isNotBlank() -> {
                    AsyncImage(
                        model = normalizedImageUrl,
                        contentDescription = "Avatar de $displayName",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
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
