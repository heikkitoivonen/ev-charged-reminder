package com.evchargedreminder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.evchargedreminder.domain.model.Charger
import com.evchargedreminder.domain.model.ChargerType
import java.time.Instant

@Entity(tableName = "chargers")
data class ChargerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val maxChargingSpeedKw: Double,
    val chargerType: String,
    val notifyMinutesBefore: Int,
    val createdAt: Instant
) {
    fun toDomainModel() = Charger(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        radiusMeters = radiusMeters,
        maxChargingSpeedKw = maxChargingSpeedKw,
        chargerType = ChargerType.valueOf(chargerType),
        notifyMinutesBefore = notifyMinutesBefore,
        createdAt = createdAt
    )

    companion object {
        fun fromDomainModel(charger: Charger) = ChargerEntity(
            id = charger.id,
            name = charger.name,
            latitude = charger.latitude,
            longitude = charger.longitude,
            radiusMeters = charger.radiusMeters,
            maxChargingSpeedKw = charger.maxChargingSpeedKw,
            chargerType = charger.chargerType.name,
            notifyMinutesBefore = charger.notifyMinutesBefore,
            createdAt = charger.createdAt
        )
    }
}
