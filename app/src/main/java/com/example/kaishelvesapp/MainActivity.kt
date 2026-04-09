package com.example.kaishelvesapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.example.kaishelvesapp.ui.components.GothicBackground
import com.example.kaishelvesapp.ui.navigation.AppNavigation
import com.example.kaishelvesapp.ui.theme.KaiShelvesAppTheme

class MainActivity : AppCompatActivity() {
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