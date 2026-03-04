package com.evchargedreminder.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ChargingCurveTest {

    @Test
    fun `linear estimate for 75kWh battery 20 to 80 percent at 7_7kW`() {
        // 75 * 0.6 = 45 kWh needed, 45 / 7.7 = 5.84 hours = 350 min
        val minutes = ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = 75.0,
            startPct = 20,
            targetPct = 80,
            chargingSpeedKw = 7.7
        )
        assertEquals(350, minutes)
    }

    @Test
    fun `returns 0 when target equals start`() {
        assertEquals(
            0,
            ChargingCurve.estimateChargingTimeMinutes(75.0, 50, 50, 7.7)
        )
    }

    @Test
    fun `returns 0 when charging speed is zero`() {
        assertEquals(
            0,
            ChargingCurve.estimateChargingTimeMinutes(75.0, 20, 80, 0.0)
        )
    }

    @Test
    fun `returns at least 1 minute for very fast charge`() {
        val minutes = ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = 40.0,
            startPct = 99,
            targetPct = 100,
            chargingSpeedKw = 350.0
        )
        assertEquals(1, minutes)
    }

    @Test
    fun `full charge at DC fast speed`() {
        // 75 * 0.8 = 60 kWh, 60 / 150 = 0.4 hours = 24 min
        val minutes = ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = 75.0,
            startPct = 10,
            targetPct = 90,
            chargingSpeedKw = 150.0
        )
        assertEquals(24, minutes)
    }
}
