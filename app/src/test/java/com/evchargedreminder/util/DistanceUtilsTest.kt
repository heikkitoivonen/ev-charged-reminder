package com.evchargedreminder.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceUtilsTest {

    @Test
    fun `same point returns zero distance`() {
        val distance = DistanceUtils.haversineDistance(40.7128, -74.0060, 40.7128, -74.0060)
        assertEquals(0.0, distance, 0.01)
    }

    @Test
    fun `known distance NYC to LA`() {
        // NYC: 40.7128, -74.0060
        // LA: 34.0522, -118.2437
        // Expected: ~3,944 km
        val distance = DistanceUtils.haversineDistance(40.7128, -74.0060, 34.0522, -118.2437)
        assertEquals(3_944_000.0, distance, 50_000.0) // within 50km tolerance
    }

    @Test
    fun `short distance approximately 1km`() {
        // Two points roughly 1km apart in Manhattan
        // Start: 40.7484 (Empire State Building)
        // End: ~0.009 degrees north ≈ 1km
        val distance = DistanceUtils.haversineDistance(40.7484, -73.9857, 40.7574, -73.9857)
        assertEquals(1_000.0, distance, 50.0) // within 50m tolerance
    }

    @Test
    fun `distance is symmetric`() {
        val d1 = DistanceUtils.haversineDistance(40.7128, -74.0060, 34.0522, -118.2437)
        val d2 = DistanceUtils.haversineDistance(34.0522, -118.2437, 40.7128, -74.0060)
        assertEquals(d1, d2, 0.01)
    }

    @Test
    fun `antipodal points are approximately half earth circumference`() {
        // North Pole to South Pole
        val distance = DistanceUtils.haversineDistance(90.0, 0.0, -90.0, 0.0)
        // Half circumference ≈ 20,015 km
        assertEquals(20_015_000.0, distance, 100_000.0)
    }

    @Test
    fun `small distance within charger radius`() {
        // Two points about 50 meters apart
        val distance = DistanceUtils.haversineDistance(37.7749, -122.4194, 37.7753, -122.4194)
        assertTrue("Distance should be < 100m (charger radius)", distance < 100.0)
        assertTrue("Distance should be > 0", distance > 0.0)
    }
}
