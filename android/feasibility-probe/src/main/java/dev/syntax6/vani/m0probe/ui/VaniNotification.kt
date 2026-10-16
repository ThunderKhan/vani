package dev.syntax6.vani.m0probe.ui

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import dev.syntax6.vani.m0probe.R
import dev.syntax6.vani.m4.M4Priority
import dev.syntax6.vani.m4.SemanticBundle

/**
 * Foreground-app receiver alerts. Explicit user opt-in only, no content or keys
 * on lock-screen notifications, and no volume/DND/OS policy bypass.
 */
object VaniNotification {
    private const val CHANNEL_ID = "vani_incoming_urgent_v1"
    private const val MIN_INTERVAL_MS = 30_000L
    private var lastAlertElapsed: Long = Long.MIN_VALUE

    @Synchronized
    fun maybePost(context: Context, message: SemanticBundle): Boolean {
        if (message.priority != M4Priority.URGENT) return false
        if (!context.getSharedPreferences("vani_field_ui", Context.MODE_PRIVATE)
                .getBoolean("urgent_notify", false)) return false
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) return false

        val manager = context.getSystemService(NotificationManager::class.java)
            ?: return false
        if (!manager.areNotificationsEnabled()) return false

        val now = SystemClock.elapsedRealtime()
        if (lastAlertElapsed != Long.MIN_VALUE && now - lastAlertElapsed < MIN_INTERVAL_MS) {
            return false // Bounded attention/alert rate, never a distress fast lane.
        }
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Urgent local messages",
                NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Opt-in alerts for authenticated urgent local Vāṇī messages"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
        )
        val intent = Intent(context, VaniFieldActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_vani_notification)
            .setContentTitle("Urgent message received")
            .setContentText("Open Vāṇī to review the encrypted local message.")
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        manager.notify(message.messageId.hashCode(), notification)
        lastAlertElapsed = now
        return true
    }
}
