package com.evchargedreminder.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.evchargedreminder.data.repository.ChargerRepository
import com.evchargedreminder.data.repository.ChargingSessionRepository
import com.evchargedreminder.domain.model.SessionEndReason
import com.evchargedreminder.domain.usecase.ManageSessionUseCase
import com.evchargedreminder.util.DistanceUtils
import com.evchargedreminder.util.LocationProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationMonitorService : Service() {

    companion object {
        const val ACTION_START_SESSION = "com.evchargedreminder.START_SESSION"
        const val ACTION_END_SESSION = "com.evchargedreminder.END_SESSION"
        const val ACTION_UPDATE_PERCENTAGES = "com.evchargedreminder.UPDATE_PERCENTAGES"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_START_PCT = "start_pct"
        const val EXTRA_TARGET_PCT = "target_pct"
        private const val POLL_INTERVAL_MS = 60_000L // 1 minute
    }

    @Inject lateinit var manageSession: ManageSessionUseCase
    @Inject lateinit var locationProvider: LocationProvider
    @Inject lateinit var chargerRepository: ChargerRepository
    @Inject lateinit var chargingSessionRepository: ChargingSessionRepository
    @Inject lateinit var notificationManager: ChargingNotificationManager

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var pollingJob: Job? = null
    private var minutesAwayFromCharger = 0L
    private var wasNearCharger = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SESSION -> {
                val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
                if (sessionId != -1L) {
                    startMonitoring(sessionId)
                }
            }
            ACTION_END_SESSION -> {
                serviceScope.launch {
                    val session = manageSession.getActiveSession()
                    if (session != null) {
                        manageSession.endSession(session.id, SessionEndReason.MANUAL)
                        notificationManager.sendSessionEndedNotification(SessionEndReason.MANUAL)
                    }
                    stopSelf()
                }
            }
            ACTION_UPDATE_PERCENTAGES -> {
                val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
                val startPct = intent.getIntExtra(EXTRA_START_PCT, -1)
                val targetPct = intent.getIntExtra(EXTRA_TARGET_PCT, -1)
                if (sessionId != -1L) {
                    serviceScope.launch {
                        manageSession.updateEstimatedEndTime(
                            sessionId = sessionId,
                            newStartPct = if (startPct >= 0) startPct else null,
                            newTargetPct = if (targetPct >= 0) targetPct else null
                        )
                        // Refresh foreground notification with updated ETA
                        val session = chargingSessionRepository.getById(sessionId) ?: return@launch
                        val charger = chargerRepository.getById(session.chargerId) ?: return@launch
                        val minutesLeft = manageSession.getEstimatedMinutesRemaining(session)
                        val notification = notificationManager.buildForegroundNotification(
                            chargerName = charger.name,
                            estimatedMinutesLeft = minutesLeft
                        )
                        val nm = getSystemService(android.app.NotificationManager::class.java)
                        nm.notify(ChargingNotificationManager.FOREGROUND_NOTIFICATION_ID, notification)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startMonitoring(sessionId: Long) {
        serviceScope.launch {
            val session = chargingSessionRepository.getById(sessionId) ?: return@launch
            val charger = chargerRepository.getById(session.chargerId) ?: return@launch

            // Start as foreground service
            val notification = notificationManager.buildForegroundNotification(
                chargerName = charger.name,
                estimatedMinutesLeft = manageSession.getEstimatedMinutesRemaining(session)
            )
            ServiceCompat.startForeground(
                this@LocationMonitorService,
                ChargingNotificationManager.FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )

            // Start polling loop
            pollingJob = serviceScope.launch {
                while (true) {
                    delay(POLL_INTERVAL_MS)
                    pollAndUpdate(sessionId)
                }
            }
        }
    }

    private suspend fun pollAndUpdate(sessionId: Long) {
        val session = chargingSessionRepository.getById(sessionId)
        if (session == null || session.actualEndAt != null) {
            stopSelf()
            return
        }

        val charger = chargerRepository.getById(session.chargerId)
        if (charger == null) {
            stopSelf()
            return
        }

        // Check proximity
        val location = locationProvider.getCurrentLocation()
        val isNearCharger = if (location != null) {
            val (lat, lng) = location
            val distance = DistanceUtils.haversineDistance(
                lat, lng, charger.latitude, charger.longitude
            )
            distance <= charger.radiusMeters
        } else {
            // Can't determine — assume still near
            true
        }

        // Track departure
        if (!isNearCharger) {
            minutesAwayFromCharger++
            wasNearCharger = false
        } else if (!wasNearCharger) {
            // User just came back
            if (manageSession.shouldEndByUserLeft(isNearCharger = true, minutesAway = minutesAwayFromCharger)) {
                manageSession.endSession(session.id, SessionEndReason.USER_LEFT)
                notificationManager.sendSessionEndedNotification(SessionEndReason.USER_LEFT)
                stopSelf()
                return
            }
            // Brief departure — reset tracker
            minutesAwayFromCharger = 0
            wasNearCharger = true
        }

        // Check if target reached
        if (manageSession.shouldEndByTargetReached(session)) {
            manageSession.endSession(session.id, SessionEndReason.TARGET_REACHED)
            notificationManager.sendSessionEndedNotification(SessionEndReason.TARGET_REACHED)
            stopSelf()
            return
        }

        // Check notifications
        val notifNumber = manageSession.getNotificationToSend(session)
        if (notifNumber > 0) {
            val minutesLeft = manageSession.getEstimatedMinutesRemaining(session)
            notificationManager.sendChargingAlertNotification(minutesLeft)
            manageSession.incrementNotificationCount(session.id)
        }

        // Update foreground notification
        val minutesLeft = manageSession.getEstimatedMinutesRemaining(session)
        val notification = notificationManager.buildForegroundNotification(
            chargerName = charger.name,
            estimatedMinutesLeft = minutesLeft
        )
        val nm = getSystemService(android.app.NotificationManager::class.java)
        nm.notify(ChargingNotificationManager.FOREGROUND_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
