package com.evchargedreminder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.evchargedreminder.domain.model.Car
import java.time.Instant

@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val year: Int,
    val make: String,
    val model: String,
    val trim: String?,
    val isHybrid: Boolean,
    val batteryCapacityKwh: Double,
    val maxAcceptRateKw: Double?,
    val defaultStartPct: Int,
    val defaultTargetPct: Int,
    val isFavorite: Boolean,
    val createdAt: Instant
) {
    fun toDomainModel() = Car(
        id = id,
        year = year,
        make = make,
        model = model,
        trim = trim,
        isHybrid = isHybrid,
        batteryCapacityKwh = batteryCapacityKwh,
        maxAcceptRateKw = maxAcceptRateKw,
        defaultStartPct = defaultStartPct,
        defaultTargetPct = defaultTargetPct,
        isFavorite = isFavorite,
        createdAt = createdAt
    )

    companion object {
        fun fromDomainModel(car: Car) = CarEntity(
            id = car.id,
            year = car.year,
            make = car.make,
            model = car.model,
            trim = car.trim,
            isHybrid = car.isHybrid,
            batteryCapacityKwh = car.batteryCapacityKwh,
            maxAcceptRateKw = car.maxAcceptRateKw,
            defaultStartPct = car.defaultStartPct,
            defaultTargetPct = car.defaultTargetPct,
            isFavorite = car.isFavorite,
            createdAt = car.createdAt
        )
    }
}
