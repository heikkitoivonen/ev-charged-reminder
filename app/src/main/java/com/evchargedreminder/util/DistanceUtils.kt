package com.evchargedreminder.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object DistanceUtils {

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /**
     * Calculates the great-circle distance between two GPS coordinates
     * using the Haversine formula.
     *
     * @return distance in meters
     */
    fun haversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val radLat1 = Math.toRadians(lat1)
        val radLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(radLat1) * cos(radLat2) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }
}
