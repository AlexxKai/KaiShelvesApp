package com.example.kaishelvesapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun KaiScreen(
    title: String,
    subtitle: String? = null,
    currentSection: KaiSection? = null,
    onSectionSelected: ((KaiSection) -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (currentSection != null && onSectionSelected != null) {
                KaiBottomBar(
                    current = currentSection,
                    onSelect = onSectionSelected
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            KaiTopBar(
                title = title,
                subtitle = subtitle
            )

            Spacer(modifier = Modifier.height(16.dp))

            content(PaddingValues(0.dp))
        }
    }
}