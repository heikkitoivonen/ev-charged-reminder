package com.evchargedreminder.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.evchargedreminder.data.local.entity.ChargerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargerDao {
    @Query("SELECT * FROM chargers ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ChargerEntity>>

    @Query("SELECT * FROM chargers WHERE id = :id")
    suspend fun getById(id: Long): ChargerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(charger: ChargerEntity): Long

    @Update
    suspend fun update(charger: ChargerEntity)

    @Delete
    suspend fun delete(charger: ChargerEntity)
}
