package com.evchargedreminder.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.evchargedreminder.data.repository.ChargerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class GeofenceRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val chargerRepository: ChargerRepository,
    private val geofenceManager: GeofenceManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "geofence_refresh"
    }

    override suspend fun doWork(): Result {
        val chargers = chargerRepository.getAll().first()
        geofenceManager.unregisterAll()
        geofenceManager.registerGeofences(chargers)
        return Result.success()
    }
}
