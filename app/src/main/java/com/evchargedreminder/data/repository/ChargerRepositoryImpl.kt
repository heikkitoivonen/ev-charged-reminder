package com.evchargedreminder.data.repository

import com.evchargedreminder.data.local.dao.ChargerDao
import com.evchargedreminder.data.local.entity.ChargerEntity
import com.evchargedreminder.domain.model.Charger
import com.evchargedreminder.service.GeofenceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargerRepositoryImpl @Inject constructor(
    private val chargerDao: ChargerDao,
    private val geofenceManager: GeofenceManager
) : ChargerRepository {

    override fun getAll(): Flow<List<Charger>> =
        chargerDao.getAll().map { entities -> entities.map { it.toDomainModel() } }

    override suspend fun getById(id: Long): Charger? =
        chargerDao.getById(id)?.toDomainModel()

    override suspend fun insert(charger: Charger): Long {
        val id = chargerDao.insert(ChargerEntity.fromDomainModel(charger))
        refreshGeofences()
        return id
    }

    override suspend fun update(charger: Charger) {
        chargerDao.update(ChargerEntity.fromDomainModel(charger))
        refreshGeofences()
    }

    override suspend fun delete(charger: Charger) {
        chargerDao.delete(ChargerEntity.fromDomainModel(charger))
        refreshGeofences()
    }

    private suspend fun refreshGeofences() {
        val chargers = getAll().first()
        geofenceManager.registerGeofences(chargers)
    }
}
