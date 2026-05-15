package com.example.kaishelvesapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.ui.theme.NightBlack

@Composable
fun GothicBackground(
    modifier: Modifier = Modifier,
    imageAlpha: Float = 0.24f,
    mainScrimAlpha: Float = 0.62f,
    secondaryScrimAlpha: Float = 0.12f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NightBlack)
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_bookshelf),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(imageAlpha),
            contentScale = ContentScale.Crop
        )

        if (mainScrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NightBlack.copy(alpha = mainScrimAlpha))
            )
        }

        if (secondaryScrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = secondaryScrimAlpha))
            )
        }

        content()
    }
}
