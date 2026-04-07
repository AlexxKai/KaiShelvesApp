package com.example.kaishelvesapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.ui.theme.NightBlack

@Composable
fun GothicBackground(
    modifier: Modifier = Modifier,
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
                .alpha(0.18f),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NightBlack.copy(alpha = 0.70f))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NightBlack.copy(alpha = 0.16f))
        )

        content()
    }
}