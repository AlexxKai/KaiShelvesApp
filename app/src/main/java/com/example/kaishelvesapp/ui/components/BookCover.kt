package com.example.kaishelvesapp.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.kaishelvesapp.ui.theme.BloodWine
import com.example.kaishelvesapp.ui.theme.NightBlack
import com.example.kaishelvesapp.ui.theme.OldIvory
import com.example.kaishelvesapp.ui.theme.TarnishedGold

@Composable
fun BookCover(
    imageUrl: String,
    title: String,
    modifier: Modifier = Modifier,
    containerColor: Color = BloodWine,
    borderColor: Color = TarnishedGold,
    placeholderContent: @Composable BoxScope.() -> Unit = {
        Text(
            text = "Tomo",
            color = OldIvory,
            style = MaterialTheme.typography.labelLarge
        )
    }
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .border(
                BorderStroke(1.dp, borderColor),
                RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = imageUrl.isNotBlank(),
            label = "book_cover_crossfade"
        ) { hasImage ->
            if (hasImage) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Portada de $title",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    placeholderContent()
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(width = 6.dp, height = 70.dp)
                        .background(NightBlack.copy(alpha = 0.35f))
                )
            }
        }
    }
}
