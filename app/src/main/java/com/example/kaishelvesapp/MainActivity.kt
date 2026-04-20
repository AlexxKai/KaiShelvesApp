package com.example.kaishelvesapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.kaishelvesapp.data.notifications.DeviceNotificationManager
import com.example.kaishelvesapp.ui.components.GothicBackground
import com.example.kaishelvesapp.ui.navigation.AppNavigation
import com.example.kaishelvesapp.ui.theme.KaiShelvesAppTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceNotificationManager.ensureChannels(this)
        requestNotificationPermissionIfNeeded()
        setContent {
            KaiShelvesAppTheme {
                GothicBackground {
                    AppNavigation()
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!alreadyGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
    }
}
