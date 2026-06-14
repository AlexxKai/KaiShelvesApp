package com.example.kaishelvesapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.kaishelvesapp.data.local.AppContextProvider
import com.example.kaishelvesapp.data.notifications.DeviceNotificationManager
import com.example.kaishelvesapp.ui.components.GothicBackground
import com.example.kaishelvesapp.ui.navigation.AppNavigation
import com.example.kaishelvesapp.ui.theme.KaiShelvesAppTheme

class MainActivity : AppCompatActivity() {
    private val activityNotificationToOpen = mutableStateOf<String?>(null)
    private val deviceLibraryBookToOpen = mutableStateOf<String?>(null)
    private val closeAfterExternalBook = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContextProvider.initialize(applicationContext)
        DeviceNotificationManager.ensureChannels(this)
        requestNotificationPermissionIfNeeded()
        activityNotificationToOpen.value = intent.activityNotificationId()
        deviceLibraryBookToOpen.value = intent.deviceLibraryBookUri()
        closeAfterExternalBook.value = intent.isExternalDeviceBookRequest()
        setContent {
            KaiShelvesAppTheme {
                GothicBackground {
                    AppNavigation(
                        activityNotificationToOpen = activityNotificationToOpen.value,
                        onActivityNotificationOpenConsumed = {
                            activityNotificationToOpen.value = null
                        },
                        deviceLibraryBookToOpen = deviceLibraryBookToOpen.value,
                        closeAfterExternalBook = closeAfterExternalBook.value,
                        onDeviceLibraryBookOpenConsumed = {
                            deviceLibraryBookToOpen.value = null
                        },
                        onExternalDeviceBookClosed = {
                            closeAfterExternalBook.value = false
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        activityNotificationToOpen.value = intent.activityNotificationId()
        deviceLibraryBookToOpen.value = intent.deviceLibraryBookUri()
        closeAfterExternalBook.value = intent.isExternalDeviceBookRequest()
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

    private fun Intent.activityNotificationId(): String? {
        return getStringExtra(DeviceNotificationManager.EXTRA_ACTIVITY_NOTIFICATION_ID)
            ?.takeIf { it.isNotBlank() }
    }

    private fun Intent.deviceLibraryBookUri(): String? {
        return getStringExtra(EXTRA_DEVICE_BOOK_URI)
            ?.takeIf { it.isNotBlank() }
            ?: data?.toString()
            ?.takeIf { it.isNotBlank() }
    }

    private fun Intent.isExternalDeviceBookRequest(): Boolean {
        return action == Intent.ACTION_VIEW &&
            data != null &&
            getStringExtra(EXTRA_DEVICE_BOOK_URI).isNullOrBlank()
    }

    companion object {
        const val ACTION_OPEN_DEVICE_BOOK = "com.example.kaishelvesapp.action.OPEN_DEVICE_BOOK"
        const val EXTRA_DEVICE_BOOK_URI = "com.example.kaishelvesapp.extra.DEVICE_BOOK_URI"
    }
}
