package com.evchargedreminder.data.repository

import com.evchargedreminder.domain.model.Charger
import kotlinx.coroutines.flow.Flow

interface ChargerRepository {
    fun getAll(): Flow<List<Charger>>
    suspend fun getById(id: Long): Charger?
    suspend fun insert(charger: Charger): Long
    suspend fun update(charger: Charger)
    suspend fun delete(charger: Charger)
}
