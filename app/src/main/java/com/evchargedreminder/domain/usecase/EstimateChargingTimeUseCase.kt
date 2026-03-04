package com.evchargedreminder.domain.usecase

import com.evchargedreminder.data.repository.CarRepository
import com.evchargedreminder.data.repository.ChargerRepository
import com.evchargedreminder.domain.model.Car
import com.evchargedreminder.domain.model.Charger
import com.evchargedreminder.util.ChargingCurve
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EstimateChargingTimeUseCase @Inject constructor(
    private val carRepository: CarRepository,
    private val chargerRepository: ChargerRepository
) {

    /**
     * Estimates charging time by loading car and charger from repositories.
     * @return estimated minutes, or 0 if car/charger not found
     */
    suspend fun estimate(
        carId: Long,
        chargerId: Long,
        startPct: Int,
        targetPct: Int
    ): Long {
        val car = carRepository.getById(carId) ?: return 0
        val charger = chargerRepository.getById(chargerId) ?: return 0
        return estimateWithData(car, charger, startPct, targetPct)
    }

    /**
     * Pure function for estimating charging time when car and charger data are already loaded.
     */
    fun estimateWithData(
        car: Car,
        charger: Charger,
        startPct: Int,
        targetPct: Int
    ): Long {
        return ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = car.batteryCapacityKwh,
            startPct = startPct,
            targetPct = targetPct,
            chargingSpeedKw = charger.maxChargingSpeedKw,
            isAc = charger.chargerType.isAc,
            maxAcceptRateKw = car.maxAcceptRateKw
        )
    }
}
