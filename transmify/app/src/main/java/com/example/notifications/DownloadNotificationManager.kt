package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class DownloadNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "transmission_completed_downloads"
        const val CHANNEL_NAME = "Завершенные загрузки Transmission"
        const val CHANNEL_DESC = "Уведомления о завершении скачивания торрентов"
        const val EXTRA_TORRENT_ID = "extra_torrent_id"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun notifyDownloadCompleted(torrentName: String, totalSize: String, torrentId: Long) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TORRENT_ID, torrentId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            torrentId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_fg_img)
            .setContentTitle("Загрузка завершена! ✨")
            .setContentText("$torrentName ($totalSize) готов к раздаче или просмотру")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Торрент \"$torrentName\" ($totalSize) успешно скачан и перешел в режим раздачи.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(torrentId.toInt(), notification)
        } catch (_: SecurityException) {
            // Permission revoked at runtime
        }
    }

    fun sendTestNotification() {
        notifyDownloadCompleted(
            torrentName = "Ubuntu 24.04.1 LTS Desktop amd64",
            totalSize = "5.8 GB",
            torrentId = 99999L
        )
    }
}
