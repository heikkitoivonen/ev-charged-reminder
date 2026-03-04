package com.evchargedreminder.domain.usecase

import com.evchargedreminder.data.repository.ChargerRepository
import com.evchargedreminder.data.repository.ChargingSessionRepository
import com.evchargedreminder.domain.model.ChargingSession
import com.evchargedreminder.domain.model.SessionEndReason
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManageSessionUseCase @Inject constructor(
    private val chargingSessionRepository: ChargingSessionRepository,
    private val chargerRepository: ChargerRepository
) {

    /**
     * Returns the first active session (actualEndAt == null), or null.
     */
    suspend fun getActiveSession(): ChargingSession? {
        return chargingSessionRepository.getActiveSessions().firstOrNull()
    }

    /**
     * Ends a session with the given reason.
     */
    suspend fun endSession(sessionId: Long, reason: SessionEndReason) {
        val session = chargingSessionRepository.getById(sessionId) ?: return
        chargingSessionRepository.update(
            session.copy(
                actualEndAt = Instant.now(),
                endReason = reason
            )
        )
    }

    /**
     * Determines if a session should end because the user left and came back.
     * Returns true if the user re-entered the charger area after being away
     * for more than 15 minutes.
     *
     * @param isNearCharger whether the user is currently within the charger radius
     * @param minutesAway how many minutes the user has been outside the radius
     *        (0 if currently near, accumulated while away)
     */
    fun shouldEndByUserLeft(
        isNearCharger: Boolean,
        minutesAway: Long
    ): Boolean {
        // User re-entered after being away >15 min
        return isNearCharger && minutesAway > 15
    }

    /**
     * Checks if the estimated end time has been reached.
     */
    fun shouldEndByTargetReached(session: ChargingSession): Boolean {
        return Instant.now() >= session.estimatedEndAt
    }

    /**
     * Checks if a notification should be sent based on remaining time
     * and the charger's notifyMinutesBefore setting.
     * Returns the notification number (1, 2, or 3) or 0 if no notification needed.
     */
    suspend fun getNotificationToSend(session: ChargingSession): Int {
        if (session.notificationsSent >= 3) return 0

        val charger = chargerRepository.getById(session.chargerId) ?: return 0
        val remaining = Duration.between(Instant.now(), session.estimatedEndAt)
        val remainingMinutes = remaining.toMinutes()

        if (remainingMinutes > charger.notifyMinutesBefore) return 0

        // Notification schedule: at threshold, threshold-5, threshold-10
        val threshold = charger.notifyMinutesBefore.toLong()
        return when {
            session.notificationsSent == 0 && remainingMinutes <= threshold -> 1
            session.notificationsSent == 1 && remainingMinutes <= threshold - 5 -> 2
            session.notificationsSent == 2 && remainingMinutes <= threshold - 10 -> 3
            else -> 0
        }
    }

    /**
     * Increments the notification count for a session.
     */
    suspend fun incrementNotificationCount(sessionId: Long) {
        val session = chargingSessionRepository.getById(sessionId) ?: return
        chargingSessionRepository.update(
            session.copy(notificationsSent = session.notificationsSent + 1)
        )
    }

    /**
     * Returns estimated minutes remaining for a session.
     */
    fun getEstimatedMinutesRemaining(session: ChargingSession): Long {
        val remaining = Duration.between(Instant.now(), session.estimatedEndAt)
        return remaining.toMinutes().coerceAtLeast(0)
    }
}
