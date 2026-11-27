package com.cloudflaresecure.vpn.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.cloudflaresecure.vpn.MainActivity
import com.cloudflaresecure.vpn.R
import com.cloudflaresecure.vpn.vpn.service.CloudflareVpnService

/** Foreground bildirim merkezi (spec §4G). */
object VpnNotificationHelper {

    const val CHANNEL_ID = "vpn_connection"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(
        context: Context,
        serverName: String,
        connectedSeconds: Long
    ): Notification {
        ensureChannel(context)
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val disconnectIntent = PendingIntent.getService(
            context, 1,
            Intent(context, CloudflareVpnService::class.java).setAction(CloudflareVpnService.ACTION_DISCONNECT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val reconnectIntent = PendingIntent.getService(
            context, 2,
            Intent(context, CloudflareVpnService::class.java).setAction(CloudflareVpnService.ACTION_RECONNECT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val mm = connectedSeconds / 60
        val ss = connectedSeconds % 60
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Cloudflare Secure VPN — Bağlı")
            .setContentText("$serverName • ${mm}dk ${ss}sn")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Kes", disconnectIntent)
            .addAction(android.R.drawable.ic_menu_rotate, "Yeniden Bağlan", reconnectIntent)
            .build()
    }
}
