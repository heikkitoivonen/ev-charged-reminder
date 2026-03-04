package com.evchargedreminder.util

/**
 * Charging curve calculator. Handles both AC (constant rate) and DC (piecewise taper).
 *
 * AC charging is bottlenecked by the car's onboard charger, which draws at a constant
 * rate regardless of battery SOC.
 *
 * DC charging bypasses the onboard charger and feeds the battery directly, so the
 * battery's state of charge affects the rate via a piecewise taper model:
 *   0%-20%:   85% of max rate
 *   20%-80%:  100% of max rate
 *   80%-90%:  50% of max rate
 *   90%-100%: 20% of max rate
 */
object ChargingCurve {

    private data class DcSegment(val startPct: Int, val endPct: Int, val rateFactor: Double)

    private val DC_SEGMENTS = listOf(
        DcSegment(0, 20, 0.85),
        DcSegment(20, 80, 1.0),
        DcSegment(80, 90, 0.50),
        DcSegment(90, 100, 0.20)
    )

    /**
     * Estimates charging time in minutes.
     *
     * @param batteryCapacityKwh total usable battery capacity
     * @param startPct starting state of charge percentage (0-100)
     * @param targetPct target state of charge percentage (0-100)
     * @param chargingSpeedKw max power output of the charger
     * @param isAc true for AC (constant rate), false for DC (piecewise taper)
     * @param maxAcceptRateKw car's max AC charge rate (onboard charger limit); null = no limit
     * @return estimated charging time in minutes, minimum 1 if charging is needed
     */
    fun estimateChargingTimeMinutes(
        batteryCapacityKwh: Double,
        startPct: Int,
        targetPct: Int,
        chargingSpeedKw: Double,
        isAc: Boolean = true,
        maxAcceptRateKw: Double? = null
    ): Long {
        if (chargingSpeedKw <= 0 || targetPct <= startPct) return 0

        val effectiveRate = if (maxAcceptRateKw != null && maxAcceptRateKw > 0) {
            minOf(chargingSpeedKw, maxAcceptRateKw)
        } else {
            chargingSpeedKw
        }

        val hours = if (isAc) {
            estimateAcHours(batteryCapacityKwh, startPct, targetPct, effectiveRate)
        } else {
            estimateDcHours(batteryCapacityKwh, startPct, targetPct, effectiveRate)
        }

        return (hours * 60).toLong().coerceAtLeast(1)
    }

    private fun estimateAcHours(
        batteryCapacityKwh: Double,
        startPct: Int,
        targetPct: Int,
        effectiveRateKw: Double
    ): Double {
        val kwhNeeded = batteryCapacityKwh * (targetPct - startPct) / 100.0
        return kwhNeeded / effectiveRateKw
    }

    private fun estimateDcHours(
        batteryCapacityKwh: Double,
        startPct: Int,
        targetPct: Int,
        effectiveRateKw: Double
    ): Double {
        var totalHours = 0.0

        for (segment in DC_SEGMENTS) {
            // Find overlap between [startPct, targetPct] and [segment.startPct, segment.endPct]
            val overlapStart = maxOf(startPct, segment.startPct)
            val overlapEnd = minOf(targetPct, segment.endPct)

            if (overlapStart >= overlapEnd) continue

            val segmentKwh = batteryCapacityKwh * (overlapEnd - overlapStart) / 100.0
            val segmentRate = effectiveRateKw * segment.rateFactor
            totalHours += segmentKwh / segmentRate
        }

        return totalHours
    }
}
