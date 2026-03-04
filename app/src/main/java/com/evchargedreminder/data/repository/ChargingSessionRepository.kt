package com.evchargedreminder.data.repository

import com.evchargedreminder.domain.model.ChargingSession
import kotlinx.coroutines.flow.Flow

interface ChargingSessionRepository {
    fun getAll(): Flow<List<ChargingSession>>
    suspend fun getById(id: Long): ChargingSession?
    suspend fun getActiveSessions(): List<ChargingSession>
    fun getByCarId(carId: Long): Flow<List<ChargingSession>>
    fun getByChargerId(chargerId: Long): Flow<List<ChargingSession>>
    suspend fun insert(session: ChargingSession): Long
    suspend fun update(session: ChargingSession)
    suspend fun deleteOlderThan(cutoffEpochMilli: Long)
}
