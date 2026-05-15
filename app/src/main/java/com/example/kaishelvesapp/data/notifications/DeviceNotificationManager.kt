package com.example.kaishelvesapp.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.example.kaishelvesapp.MainActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.kaishelvesapp.R

object DeviceNotificationManager {

    const val ACCOUNT_CHANNEL_ID = "account_updates"
    const val ACTIVITY_CHANNEL_ID = "activity_updates"
    const val LIBRARY_IMPORT_CHANNEL_ID = "library_import"
    const val EXTRA_ACTIVITY_NOTIFICATION_ID = "activity_notification_id"
    private const val LIBRARY_IMPORT_NOTIFICATION_ID = 3207

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val accountChannel = NotificationChannel(
            ACCOUNT_CHANNEL_ID,
            "Actualizaciones de cuenta",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notificaciones sobre cambios importantes en la cuenta"
        }
        val activityChannel = NotificationChannel(
            ACTIVITY_CHANNEL_ID,
            "Interacciones sociales",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notificaciones de me gusta y comentarios en tus publicaciones"
        }
        val libraryImportChannel = NotificationChannel(
            LIBRARY_IMPORT_CHANNEL_ID,
            "Importación de biblioteca",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Progreso de importaciones de libros desde otras plataformas"
        }
        manager.createNotificationChannels(listOf(accountChannel, activityChannel, libraryImportChannel))
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

    fun showActivityNotification(
        context: Context,
        notificationId: String,
        title: String,
        body: String
    ): Boolean {
        ensureChannels(context)
        if (!canPostNotifications(context)) {
            return false
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTIVITY_NOTIFICATION_ID, notificationId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ACTIVITY_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId.hashCode(), notification)
        return true
    }

    fun showLibraryImportProgress(
        context: Context,
        processedBooks: Int,
        totalBooks: Int
    ): Boolean {
        ensureChannels(context)
        if (!canPostNotifications(context)) {
            return false
        }

        val safeTotal = totalBooks.coerceAtLeast(1)
        val safeProcessed = processedBooks.coerceIn(0, safeTotal)
        val body = "$safeProcessed de $safeTotal libros importados"
        val notification = NotificationCompat.Builder(context, LIBRARY_IMPORT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Importando biblioteca")
            .setContentText(body)
            .setProgress(safeTotal, safeProcessed, false)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(context).notify(LIBRARY_IMPORT_NOTIFICATION_ID, notification)
        return true
    }

    fun showLibraryImportCompleted(
        context: Context,
        importedBooks: Int,
        skippedRows: Int
    ): Boolean {
        ensureChannels(context)
        if (!canPostNotifications(context)) {
            return false
        }

        val body = if (skippedRows > 0) {
            "$importedBooks libros añadidos. $skippedRows filas omitidas."
        } else {
            "$importedBooks libros añadidos."
        }
        val notification = NotificationCompat.Builder(context, LIBRARY_IMPORT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Importación completada")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(LIBRARY_IMPORT_NOTIFICATION_ID, notification)
        return true
    }

    fun cancelActivityNotification(context: Context, notificationId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId.hashCode())
    }
}
