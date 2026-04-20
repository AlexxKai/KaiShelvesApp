package com.example.kaishelvesapp.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.kaishelvesapp.R

object DeviceNotificationManager {

    const val ACCOUNT_CHANNEL_ID = "account_updates"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            ACCOUNT_CHANNEL_ID,
            "Actualizaciones de cuenta",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notificaciones sobre cambios importantes en la cuenta"
        }
        manager.createNotificationChannel(channel)
    }

    fun canPostNotifications(context: Context): Boolean {
        val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!notificationsEnabled) return false

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun showAccountNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ): Boolean {
        ensureChannels(context)
        if (!canPostNotifications(context)) {
            return false
        }

        val notification = NotificationCompat.Builder(context, ACCOUNT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
        return true
    }
}
