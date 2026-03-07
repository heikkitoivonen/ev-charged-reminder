package com.evchargedreminder.di

import com.evchargedreminder.data.OnboardingPreferences
import com.evchargedreminder.data.OnboardingPreferencesImpl
import com.evchargedreminder.data.repository.CarRepository
import com.evchargedreminder.data.repository.CarRepositoryImpl
import com.evchargedreminder.data.repository.ChargerRepository
import com.evchargedreminder.data.repository.ChargerRepositoryImpl
import com.evchargedreminder.data.repository.ChargingSessionRepository
import com.evchargedreminder.data.repository.ChargingSessionRepositoryImpl
import com.evchargedreminder.service.GeofenceManager
import com.evchargedreminder.service.GeofenceManagerImpl
import com.evchargedreminder.service.SessionServiceLauncher
import com.evchargedreminder.service.SessionServiceLauncherImpl
import com.evchargedreminder.util.FusedLocationProvider
import com.evchargedreminder.util.LocationProvider
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

    @Binds
    abstract fun bindLocationProvider(impl: FusedLocationProvider): LocationProvider

    @Binds
    abstract fun bindOnboardingPreferences(impl: OnboardingPreferencesImpl): OnboardingPreferences

    @Binds
    abstract fun bindSessionServiceLauncher(impl: SessionServiceLauncherImpl): SessionServiceLauncher

    @Binds
    abstract fun bindGeofenceManager(impl: GeofenceManagerImpl): GeofenceManager
}
