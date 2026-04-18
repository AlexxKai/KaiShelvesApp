package com.example.kaishelvesapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kaishelvesapp.ui.theme.ColdAsh

@Composable
fun RatingStars(
    rating: Int,
    maxRating: Int = 5,
    iconSize: Dp = 20.dp,
    onRatingSelected: ((Int) -> Unit)? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 1..maxRating) {
            val filled = i <= rating
            val modifier = if (onRatingSelected != null) {
                Modifier.clickable { onRatingSelected(i) }
            } else {
                Modifier
            }
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "Estrella $i",
                tint = if (filled) Color(0xFFFFB326) else ColdAsh.copy(alpha = 0.72f),
                modifier = modifier.size(iconSize),
            )
        }
    }
}
