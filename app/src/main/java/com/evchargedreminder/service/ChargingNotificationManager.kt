package com.evchargedreminder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.evchargedreminder.MainActivity
import com.evchargedreminder.domain.model.SessionEndReason
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargingNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_SESSION = "charging_session"
        const val CHANNEL_ALERTS = "charging_alerts"
        const val FOREGROUND_NOTIFICATION_ID = 1001
        const val ALERT_NOTIFICATION_ID = 1002
        const val SESSION_ENDED_NOTIFICATION_ID = 1003
        const val EXTRA_SHOW_OVERRIDE = "show_override"
    }

    fun createNotificationChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)

        val sessionChannel = NotificationChannel(
            CHANNEL_SESSION,
            "Charging Session",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing notification while charging session is active"
        }

        val alertChannel = NotificationChannel(
            CHANNEL_ALERTS,
            "Charging Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when charging is nearly complete"
        }

        manager.createNotificationChannel(sessionChannel)
        manager.createNotificationChannel(alertChannel)
    }

    private fun explicitMainActivityIntent(): Intent = Intent(context, MainActivity::class.java)

    fun buildForegroundNotification(
        chargerName: String,
        estimatedMinutesLeft: Long
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            explicitMainActivityIntent(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val endSessionIntent = Intent(context, LocationMonitorService::class.java).apply {
            action = LocationMonitorService.ACTION_END_SESSION
        }
        val endIntent = PendingIntent.getService(
            context, 0,
            endSessionIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val timeText = if (estimatedMinutesLeft > 0) {
            "${estimatedMinutesLeft}min remaining"
        } else {
            "Estimating..."
        }

        return NotificationCompat.Builder(context, CHANNEL_SESSION)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("Charging at $chargerName")
            .setContentText(timeText)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End Session",
                endIntent
            )
            .build()
    }

    fun sendChargingAlertNotification(minutesRemaining: Long) {
        val overrideIntent = explicitMainActivityIntent().apply {
            putExtra(EXTRA_SHOW_OVERRIDE, true)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("Charging almost done")
            .setContentText("About $minutesRemaining minutes remaining")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 1,
                    overrideIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    fun sendSessionEndedNotification(reason: SessionEndReason) {
        val message = when (reason) {
            SessionEndReason.TARGET_REACHED -> "Charging target reached"
            SessionEndReason.USER_LEFT -> "Session ended — you returned to your car"
            SessionEndReason.MANUAL -> "Session ended manually"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("Charging complete")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 0,
                    explicitMainActivityIntent(),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(SESSION_ENDED_NOTIFICATION_ID, notification)
    }
}
