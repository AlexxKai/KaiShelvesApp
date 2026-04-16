package com.example.kaishelvesapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.kaishelvesapp.ui.theme.ColdAsh
import com.example.kaishelvesapp.ui.theme.TarnishedGold

@Composable
fun RatingStars(
    rating: Int,
    maxRating: Int = 5,
    onRatingSelected: ((Int) -> Unit)? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 1..maxRating) {
            val filled = i <= rating
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "Estrella $i",
                tint = if (filled) TarnishedGold else ColdAsh,
                modifier = if (onRatingSelected != null) {
                    androidx.compose.ui.Modifier.clickable { onRatingSelected(i) }
                } else {
                    androidx.compose.ui.Modifier
                }
            )
        }
    }
}
