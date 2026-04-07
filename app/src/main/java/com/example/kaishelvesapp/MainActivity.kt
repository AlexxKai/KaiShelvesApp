package com.example.kaishelvesapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.kaishelvesapp.ui.components.GothicBackground
import com.example.kaishelvesapp.ui.navigation.AppNavigation
import com.example.kaishelvesapp.ui.theme.KaiShelvesAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KaiShelvesAppTheme {
                GothicBackground {
                    AppNavigation()
                }
            }
        }
    }
}