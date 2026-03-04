package com.evchargedreminder.data.repository

import com.evchargedreminder.data.local.dao.CarDao
import com.evchargedreminder.data.local.entity.CarEntity
import com.evchargedreminder.domain.model.Car
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarRepositoryImpl @Inject constructor(
    private val carDao: CarDao
) : CarRepository {

    override fun getAll(): Flow<List<Car>> =
        carDao.getAll().map { entities -> entities.map { it.toDomainModel() } }

    override suspend fun getById(id: Long): Car? =
        carDao.getById(id)?.toDomainModel()

    override suspend fun getFavorite(): Car? =
        carDao.getFavorite()?.toDomainModel()

    override suspend fun count(): Int =
        carDao.count()

    override suspend fun insert(car: Car): Long =
        carDao.insert(CarEntity.fromDomainModel(car))

    override suspend fun update(car: Car) =
        carDao.update(CarEntity.fromDomainModel(car))

    override suspend fun delete(car: Car) =
        carDao.delete(CarEntity.fromDomainModel(car))

    override suspend fun setFavorite(carId: Long) {
        carDao.clearFavorite()
        carDao.getById(carId)?.let { car ->
            carDao.update(car.copy(isFavorite = true))
        }
    }
}
