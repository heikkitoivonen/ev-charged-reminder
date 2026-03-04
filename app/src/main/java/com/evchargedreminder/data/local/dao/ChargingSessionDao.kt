package com.evchargedreminder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.evchargedreminder.data.local.entity.ChargingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargingSessionDao {
    @Query("SELECT * FROM charging_sessions ORDER BY startedAt DESC")
    fun getAll(): Flow<List<ChargingSessionEntity>>

    @Query("SELECT * FROM charging_sessions WHERE id = :id")
    suspend fun getById(id: Long): ChargingSessionEntity?

    @Query("SELECT * FROM charging_sessions WHERE actualEndAt IS NULL")
    suspend fun getActiveSessions(): List<ChargingSessionEntity>

    @Query("SELECT * FROM charging_sessions WHERE carId = :carId ORDER BY startedAt DESC")
    fun getByCarId(carId: Long): Flow<List<ChargingSessionEntity>>

    @Query("SELECT * FROM charging_sessions WHERE chargerId = :chargerId ORDER BY startedAt DESC")
    fun getByChargerId(chargerId: Long): Flow<List<ChargingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ChargingSessionEntity): Long

    @Update
    suspend fun update(session: ChargingSessionEntity)

    @Query("DELETE FROM charging_sessions WHERE actualEndAt IS NOT NULL AND startedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
