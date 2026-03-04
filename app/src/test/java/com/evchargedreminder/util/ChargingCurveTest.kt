package com.evchargedreminder.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ChargingCurveTest {

    // --- AC (constant rate) tests ---

    @Test
    fun `AC - 75kWh battery 20 to 80 percent at 7_7kW`() {
        // 75 * 0.6 = 45 kWh needed, 45 / 7.7 = 5.844 hours = 350 min
        val minutes = ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = 75.0,
            startPct = 20,
            targetPct = 80,
            chargingSpeedKw = 7.7,
            isAc = true
        )
        assertEquals(350, minutes)
    }

    @Test
    fun `AC - car accept rate limits effective speed`() {
        // Charger is 11.5 kW but car only accepts 7.7 kW
        // 75 * 0.6 = 45 kWh, 45 / 7.7 = 350 min
        val minutes = ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = 75.0,
            startPct = 20,
            targetPct = 80,
            chargingSpeedKw = 11.5,
            isAc = true,
            maxAcceptRateKw = 7.7
        )
        assertEquals(350, minutes)
    }

    @Test
    fun `AC - full charge on household outlet`() {
        // 40 kWh Leaf, 0 to 100% at 1.4 kW
        // 40 / 1.4 = 28.57 hours = 1714 min
        val minutes = ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = 40.0,
            startPct = 0,
            targetPct = 100,
            chargingSpeedKw = 1.4,
            isAc = true
        )
        assertEquals(1714, minutes)
    }

    // --- DC (piecewise taper) tests ---

    @Test
    fun `DC - 20 to 80 percent at 100pct rate`() {
        // 75 kWh, 20-80% is entirely in the 100% rate segment
        // 75 * 0.6 = 45 kWh, 45 / 150 = 0.3 hours = 18 min
        val minutes = ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = 75.0,
            startPct = 20,
            targetPct = 80,
            chargingSpeedKw = 150.0,
            isAc = false
        )
        assertEquals(18, minutes)
    }

    @Test
    fun `DC - 10 to 90 percent spans three segments`() {
        // 75 kWh battery at 150 kW DC
        // Segment 1: 10-20% = 7.5 kWh at 150*0.85 = 127.5 kW → 0.05882h
        // Segment 2: 20-80% = 45 kWh at 150*1.0 = 150 kW → 0.3h
        // Segment 3: 80-90% = 7.5 kWh at 150*0.50 = 75 kW → 0.1h
        // Total = 0.45882h = 27.53 min → 27 min
        val minutes = ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = 75.0,
            startPct = 10,
            targetPct = 90,
            chargingSpeedKw = 150.0,
            isAc = false
        )
        assertEquals(27, minutes)
    }

    @Test
    fun `DC - 0 to 100 percent full charge all segments`() {
        // 75 kWh battery at 150 kW DC
        // Segment 0-20%: 15 kWh at 127.5 kW → 0.11765h
        // Segment 20-80%: 45 kWh at 150 kW → 0.3h
        // Segment 80-90%: 7.5 kWh at 75 kW → 0.1h
        // Segment 90-100%: 7.5 kWh at 30 kW → 0.25h
        // Total = 0.76765h = 46.06 min → 46 min
        val minutes = ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = 75.0,
            startPct = 0,
            targetPct = 100,
            chargingSpeedKw = 150.0,
            isAc = false
        )
        assertEquals(46, minutes)
    }

    @Test
    fun `DC - 80 to 100 percent taper zone only`() {
        // 75 kWh battery at 150 kW DC
        // Segment 80-90%: 7.5 kWh at 75 kW → 0.1h
        // Segment 90-100%: 7.5 kWh at 30 kW → 0.25h
        // Total = 0.35h = 21 min
        val minutes = ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = 75.0,
            startPct = 80,
            targetPct = 100,
            chargingSpeedKw = 150.0,
            isAc = false
        )
        assertEquals(21, minutes)
    }

    @Test
    fun `DC - within single segment 85 to 90`() {
        // 75 kWh at 150 kW DC, 80-90% segment (50% rate)
        // 75 * 0.05 = 3.75 kWh at 75 kW → 0.05h = 3 min
        val minutes = ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = 75.0,
            startPct = 85,
            targetPct = 90,
            chargingSpeedKw = 150.0,
            isAc = false
        )
        assertEquals(3, minutes)
    }

    @Test
    fun `DC - car accept rate limits DC speed`() {
        // Car maxAcceptRateKw limits the effective rate for DC too
        // 75 kWh, 20-80% at min(350, 150) = 150 kW effective
        // 45 kWh / 150 = 0.3h = 18 min
        val minutes = ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = 75.0,
            startPct = 20,
            targetPct = 80,
            chargingSpeedKw = 350.0,
            isAc = false,
            maxAcceptRateKw = 150.0
        )
        assertEquals(18, minutes)
    }

    // --- Edge cases ---

    @Test
    fun `returns 0 when target equals start`() {
        assertEquals(
            0,
            ChargingCurve.estimateChargingTimeMinutes(75.0, 50, 50, 7.7)
        )
    }

    @Test
    fun `returns 0 when target less than start`() {
        assertEquals(
            0,
            ChargingCurve.estimateChargingTimeMinutes(75.0, 80, 20, 7.7)
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
    fun `returns 0 when charging speed is negative`() {
        assertEquals(
            0,
            ChargingCurve.estimateChargingTimeMinutes(75.0, 20, 80, -5.0)
        )
    }

    @Test
    fun `returns at least 1 minute for very fast charge`() {
        val minutes = ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = 40.0,
            startPct = 99,
            targetPct = 100,
            chargingSpeedKw = 350.0,
            isAc = true
        )
        assertEquals(1, minutes)
    }

    @Test
    fun `default isAc parameter is true for backward compatibility`() {
        // Same as AC test - verifies default parameter works
        val minutes = ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = 75.0,
            startPct = 20,
            targetPct = 80,
            chargingSpeedKw = 7.7
        )
        assertEquals(350, minutes)
    }

    @Test
    fun `null maxAcceptRateKw does not limit speed`() {
        val withNull = ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = 75.0, startPct = 20, targetPct = 80,
            chargingSpeedKw = 7.7, isAc = true, maxAcceptRateKw = null
        )
        val withoutParam = ChargingCurve.estimateChargingTimeMinutes(
            batteryCapacityKwh = 75.0, startPct = 20, targetPct = 80,
            chargingSpeedKw = 7.7, isAc = true
        )
        assertEquals(withoutParam, withNull)
    }
}
