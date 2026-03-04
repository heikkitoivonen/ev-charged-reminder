package com.evchargedreminder.di

import android.content.Context
import androidx.room.Room
import com.evchargedreminder.data.local.AppDatabase
import com.evchargedreminder.data.local.dao.CarDao
import com.evchargedreminder.data.local.dao.ChargerDao
import com.evchargedreminder.data.local.dao.ChargingSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ev_charged_reminder_db"
        ).build()

    @Provides
    fun provideCarDao(database: AppDatabase): CarDao = database.carDao()

    @Provides
    fun provideChargerDao(database: AppDatabase): ChargerDao = database.chargerDao()

    @Provides
    fun provideChargingSessionDao(database: AppDatabase): ChargingSessionDao =
        database.chargingSessionDao()
}
