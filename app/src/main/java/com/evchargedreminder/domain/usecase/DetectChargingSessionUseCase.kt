package com.evchargedreminder.domain.usecase

import com.evchargedreminder.data.repository.CarRepository
import com.evchargedreminder.data.repository.ChargerRepository
import com.evchargedreminder.data.repository.ChargingSessionRepository
import com.evchargedreminder.domain.model.Charger
import com.evchargedreminder.domain.model.ChargingSession
import com.evchargedreminder.domain.model.SessionEndReason
import com.evchargedreminder.util.ChargingCurve
import com.evchargedreminder.util.DistanceUtils
import com.evchargedreminder.util.LocationProvider
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class ProximityResult(
    val nearestCharger: Charger?,
    val distanceMeters: Double?
)

data class NearbyCharger(
    val charger: Charger,
    val distanceMeters: Double
)

@Singleton
class DetectChargingSessionUseCase @Inject constructor(
    private val chargerRepository: ChargerRepository,
    private val carRepository: CarRepository,
    private val chargingSessionRepository: ChargingSessionRepository,
    private val locationProvider: LocationProvider
) {

    companion object {
        private const val COOLDOWN_SECONDS = 15L * 60 // 15 minutes
    }

    /**
     * Checks proximity to all saved chargers. Returns the nearest charger
     * within its configured radius, or null if none are nearby.
     */
    suspend fun checkProximity(): ProximityResult {
        val location = locationProvider.getCurrentLocation()
            ?: return ProximityResult(null, null)

        val (lat, lng) = location
        val chargers = chargerRepository.getAll().first()

        var nearestCharger: Charger? = null
        var nearestDistance: Double? = null

        for (charger in chargers) {
            val distance = DistanceUtils.haversineDistance(
                lat, lng, charger.latitude, charger.longitude
            )
            if (distance <= charger.radiusMeters) {
                if (nearestDistance == null || distance < nearestDistance) {
                    nearestCharger = charger
                    nearestDistance = distance
                }
            }
        }

        return ProximityResult(nearestCharger, nearestDistance)
    }

    /**
     * Returns all chargers within their configured radius, sorted by distance.
     */
    suspend fun checkAllNearby(): List<NearbyCharger> {
        val location = locationProvider.getCurrentLocation()
            ?: return emptyList()

        val (lat, lng) = location
        val chargers = chargerRepository.getAll().first()

        return chargers.mapNotNull { charger ->
            val distance = DistanceUtils.haversineDistance(
                lat, lng, charger.latitude, charger.longitude
            )
            if (distance <= charger.radiusMeters) {
                NearbyCharger(charger, distance)
            } else null
        }.sortedBy { it.distanceMeters }
    }

    /**
     * Returns true if a session should start: dwell time met, no active
     * session exists for this charger, and not in cooldown period.
     */
    suspend fun shouldStartSession(charger: Charger, dwellMinutes: Int): Boolean {
        if (dwellMinutes < 3) return false
        val activeSessions = chargingSessionRepository.getActiveSessions()
        if (activeSessions.any { it.chargerId == charger.id }) return false
        if (isInCooldownPeriod(charger.id)) return false
        return true
    }

    /**
     * Creates and persists a new charging session using the favorite car's defaults.
     * Returns null if no favorite car is set, an active session already exists,
     * the charger is in cooldown, or the battery is already full.
     */
    suspend fun startSession(charger: Charger): ChargingSession? {
        val activeSessions = chargingSessionRepository.getActiveSessions()
        if (activeSessions.any { it.chargerId == charger.id }) return null
        if (isInCooldownPeriod(charger.id)) return null

        val car = carRepository.getFavorite() ?: return null
        if (car.defaultStartPct >= car.defaultTargetPct) return null

        val estimatedMinutes = ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = car.batteryCapacityKwh,
            startPct = car.defaultStartPct,
            targetPct = car.defaultTargetPct,
            chargingSpeedKw = charger.maxChargingSpeedKw,
            isAc = charger.chargerType.isAc,
            maxAcceptRateKw = car.maxAcceptRateKw
        )

        val now = Instant.now()
        val session = ChargingSession(
            carId = car.id,
            chargerId = charger.id,
            startPct = car.defaultStartPct,
            targetPct = car.defaultTargetPct,
            startedAt = now,
            estimatedEndAt = now.plusSeconds(estimatedMinutes * 60)
        )

        val id = chargingSessionRepository.insert(session)
        return session.copy(id = id)
    }

    /**
     * Returns true if a session for the given charger was manually stopped or
     * reached its target within the last 15 minutes.
     */
    private suspend fun isInCooldownPeriod(chargerId: Long): Boolean {
        val sessions = chargingSessionRepository.getByChargerId(chargerId).first()
        val cooldownCutoff = Instant.now().minusSeconds(COOLDOWN_SECONDS)
        return sessions.any { session ->
            session.actualEndAt != null &&
                session.actualEndAt.isAfter(cooldownCutoff) &&
                (session.endReason == SessionEndReason.MANUAL || session.endReason == SessionEndReason.TARGET_REACHED)
        }
    }
}
