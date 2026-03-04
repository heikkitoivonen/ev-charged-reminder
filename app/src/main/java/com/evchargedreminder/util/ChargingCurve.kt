package com.evchargedreminder.util

/**
 * Charging curve calculator. Handles both AC (constant rate) and DC (piecewise taper).
 * Full piecewise model will be implemented in Phase 5.
 * Current implementation uses a simple linear estimate.
 */
object ChargingCurve {

    /**
     * Estimates charging time in minutes using a simple linear model.
     * Phase 5 will add AC vs DC distinction and piecewise taper for DC.
     */
    fun estimateChargingTimeMinutes(
        batteryCapacityKwh: Double,
        startPct: Int,
        targetPct: Int,
        chargingSpeedKw: Double
    ): Long {
        if (chargingSpeedKw <= 0 || targetPct <= startPct) return 0
        val kwhNeeded = batteryCapacityKwh * (targetPct - startPct) / 100.0
        val hours = kwhNeeded / chargingSpeedKw
        return (hours * 60).toLong().coerceAtLeast(1)
    }
}
