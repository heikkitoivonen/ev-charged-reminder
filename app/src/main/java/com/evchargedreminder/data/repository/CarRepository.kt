package com.evchargedreminder.data.repository

import com.evchargedreminder.domain.model.Car
import kotlinx.coroutines.flow.Flow

interface CarRepository {
    fun getAll(): Flow<List<Car>>
    suspend fun getById(id: Long): Car?
    suspend fun getFavorite(): Car?
    suspend fun count(): Int
    suspend fun insert(car: Car): Long
    suspend fun update(car: Car)
    suspend fun delete(car: Car)
    suspend fun setFavorite(carId: Long)
}
