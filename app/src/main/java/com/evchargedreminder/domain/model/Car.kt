package com.evchargedreminder.domain.model

import java.time.Instant

data class Car(
    val id: Long = 0,
    val year: Int,
    val make: String,
    val model: String,
    val trim: String? = null,
    val isHybrid: Boolean = false,
    val batteryCapacityKwh: Double,
    val maxAcceptRateKw: Double? = null,
    val defaultStartPct: Int = if (isHybrid) 0 else 20,
    val defaultTargetPct: Int = if (isHybrid) 100 else 80,
    val isFavorite: Boolean = false,
    val createdAt: Instant = Instant.now()
)

val Car.displayName: String
    get() = if (model.isBlank()) make else "$year $make $model"
