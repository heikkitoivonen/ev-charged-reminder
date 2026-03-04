package com.evchargedreminder.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.evchargedreminder.data.local.entity.CarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Query("SELECT * FROM cars ORDER BY createdAt DESC")
    fun getAll(): Flow<List<CarEntity>>

    @Query("SELECT * FROM cars WHERE id = :id")
    suspend fun getById(id: Long): CarEntity?

    @Query("SELECT * FROM cars WHERE isFavorite = 1 LIMIT 1")
    suspend fun getFavorite(): CarEntity?

    @Query("SELECT COUNT(*) FROM cars")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(car: CarEntity): Long

    @Update
    suspend fun update(car: CarEntity)

    @Delete
    suspend fun delete(car: CarEntity)

    @Query("UPDATE cars SET isFavorite = 0 WHERE isFavorite = 1")
    suspend fun clearFavorite()
}
