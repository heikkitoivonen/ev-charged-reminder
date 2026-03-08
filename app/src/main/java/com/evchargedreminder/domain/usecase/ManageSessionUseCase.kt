package com.evchargedreminder.domain.usecase

import com.evchargedreminder.data.repository.CarRepository
import com.evchargedreminder.data.repository.ChargerRepository
import com.evchargedreminder.data.repository.ChargingSessionRepository
import com.evchargedreminder.domain.model.ChargingSession
import com.evchargedreminder.domain.model.SessionEndReason
import com.evchargedreminder.util.ChargingCurve
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManageSessionUseCase @Inject constructor(
    private val chargingSessionRepository: ChargingSessionRepository,
    private val chargerRepository: ChargerRepository,
    private val carRepository: CarRepository
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
     * Returns true if the user re-entered the charger area after having left.
     * This assumes the user returned to unplug their car.
     */
    fun shouldEndByUserLeft(
        isNearCharger: Boolean,
        hasLeftArea: Boolean
    ): Boolean {
        return isNearCharger && hasLeftArea
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

    /**
     * Changes the car linked to a session and recalculates the estimated end time.
     */
    suspend fun changeSessionCar(sessionId: Long, newCarId: Long) {
        val session = chargingSessionRepository.getById(sessionId) ?: return
        val car = carRepository.getById(newCarId) ?: return
        val charger = chargerRepository.getById(session.chargerId) ?: return

        val totalMinutes = ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = car.batteryCapacityKwh,
            startPct = session.startPct,
            targetPct = session.targetPct,
            chargingSpeedKw = charger.maxChargingSpeedKw,
            isAc = charger.chargerType.isAc,
            maxAcceptRateKw = car.maxAcceptRateKw
        )

        chargingSessionRepository.update(
            session.copy(
                carId = newCarId,
                estimatedEndAt = session.startedAt.plusSeconds(totalMinutes * 60)
            )
        )
    }

    /**
     * Recalculates the estimated end time for an active session.
     * Optionally updates start/target percentages if overridden by the user.
     * The new estimatedEndAt is computed from startedAt + full charge duration.
     */
    suspend fun updateEstimatedEndTime(
        sessionId: Long,
        newStartPct: Int? = null,
        newTargetPct: Int? = null
    ) {
        val session = chargingSessionRepository.getById(sessionId) ?: return
        val car = carRepository.getById(session.carId) ?: return
        val charger = chargerRepository.getById(session.chargerId) ?: return

        val startPct = newStartPct ?: session.startPct
        val targetPct = newTargetPct ?: session.targetPct

        val totalMinutes = ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = car.batteryCapacityKwh,
            startPct = startPct,
            targetPct = targetPct,
            chargingSpeedKw = charger.maxChargingSpeedKw,
            isAc = charger.chargerType.isAc,
            maxAcceptRateKw = car.maxAcceptRateKw
        )

        chargingSessionRepository.update(
            session.copy(
                startPct = startPct,
                targetPct = targetPct,
                estimatedEndAt = session.startedAt.plusSeconds(totalMinutes * 60)
            )
        )
    }
}
