package com.evchargedreminder.domain.model

import java.time.Instant

data class Charger(
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int = 100,
    val maxChargingSpeedKw: Double,
    val chargerType: ChargerType,
    val notifyMinutesBefore: Int = 15,
    val createdAt: Instant = Instant.now()
)
