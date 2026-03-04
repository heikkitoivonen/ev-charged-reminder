package com.evchargedreminder.data.repository

import com.evchargedreminder.data.local.dao.ChargingSessionDao
import com.evchargedreminder.data.local.entity.ChargingSessionEntity
import com.evchargedreminder.domain.model.ChargingSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargingSessionRepositoryImpl @Inject constructor(
    private val chargingSessionDao: ChargingSessionDao
) : ChargingSessionRepository {

    override fun getAll(): Flow<List<ChargingSession>> =
        chargingSessionDao.getAll().map { entities -> entities.map { it.toDomainModel() } }

    override suspend fun getById(id: Long): ChargingSession? =
        chargingSessionDao.getById(id)?.toDomainModel()

    override suspend fun getActiveSessions(): List<ChargingSession> =
        chargingSessionDao.getActiveSessions().map { it.toDomainModel() }

    override fun getByCarId(carId: Long): Flow<List<ChargingSession>> =
        chargingSessionDao.getByCarId(carId).map { entities -> entities.map { it.toDomainModel() } }

    override fun getByChargerId(chargerId: Long): Flow<List<ChargingSession>> =
        chargingSessionDao.getByChargerId(chargerId).map { entities -> entities.map { it.toDomainModel() } }

    override suspend fun insert(session: ChargingSession): Long =
        chargingSessionDao.insert(ChargingSessionEntity.fromDomainModel(session))

    override suspend fun update(session: ChargingSession) =
        chargingSessionDao.update(ChargingSessionEntity.fromDomainModel(session))

    override suspend fun deleteOlderThan(cutoffEpochMilli: Long) =
        chargingSessionDao.deleteOlderThan(cutoffEpochMilli)
}
