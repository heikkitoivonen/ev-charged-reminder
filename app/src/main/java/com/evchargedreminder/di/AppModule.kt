package com.evchargedreminder.di

import com.evchargedreminder.data.repository.CarRepository
import com.evchargedreminder.data.repository.CarRepositoryImpl
import com.evchargedreminder.data.repository.ChargerRepository
import com.evchargedreminder.data.repository.ChargerRepositoryImpl
import com.evchargedreminder.data.repository.ChargingSessionRepository
import com.evchargedreminder.data.repository.ChargingSessionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindCarRepository(impl: CarRepositoryImpl): CarRepository

    @Binds
    abstract fun bindChargerRepository(impl: ChargerRepositoryImpl): ChargerRepository

    @Binds
    abstract fun bindChargingSessionRepository(
        impl: ChargingSessionRepositoryImpl
    ): ChargingSessionRepository
}
