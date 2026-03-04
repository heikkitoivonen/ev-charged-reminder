package com.evchargedreminder.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.evchargedreminder.data.local.dao.CarDao
import com.evchargedreminder.data.local.dao.ChargerDao
import com.evchargedreminder.data.local.dao.ChargingSessionDao
import com.evchargedreminder.data.local.entity.CarEntity
import com.evchargedreminder.data.local.entity.ChargerEntity
import com.evchargedreminder.data.local.entity.ChargingSessionEntity

@Database(
    entities = [CarEntity::class, ChargerEntity::class, ChargingSessionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
    abstract fun chargerDao(): ChargerDao
    abstract fun chargingSessionDao(): ChargingSessionDao
}
