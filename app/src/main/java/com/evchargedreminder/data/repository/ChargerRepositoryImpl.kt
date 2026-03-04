package com.evchargedreminder.data.repository

import com.evchargedreminder.data.local.dao.ChargerDao
import com.evchargedreminder.data.local.entity.ChargerEntity
import com.evchargedreminder.domain.model.Charger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargerRepositoryImpl @Inject constructor(
    private val chargerDao: ChargerDao
) : ChargerRepository {

    override fun getAll(): Flow<List<Charger>> =
        chargerDao.getAll().map { entities -> entities.map { it.toDomainModel() } }

    override suspend fun getById(id: Long): Charger? =
        chargerDao.getById(id)?.toDomainModel()

    override suspend fun insert(charger: Charger): Long =
        chargerDao.insert(ChargerEntity.fromDomainModel(charger))

    override suspend fun update(charger: Charger) =
        chargerDao.update(ChargerEntity.fromDomainModel(charger))

    override suspend fun delete(charger: Charger) =
        chargerDao.delete(ChargerEntity.fromDomainModel(charger))
}
