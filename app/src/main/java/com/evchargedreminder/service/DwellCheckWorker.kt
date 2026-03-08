package com.evchargedreminder.service

import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.evchargedreminder.domain.usecase.DetectChargingSessionUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DwellCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val detectSession: DetectChargingSessionUseCase
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_CHARGER_ID = "charger_id"
    }

    override suspend fun doWork(): Result {
        val chargerId = inputData.getLong(KEY_CHARGER_ID, -1L)
        if (chargerId == -1L) return Result.failure()

        // Find the charger — try proximity first, fall back to all nearby
        val charger = detectSession.checkProximity().nearestCharger?.takeIf { it.id == chargerId }
            ?: detectSession.checkAllNearby().find { it.charger.id == chargerId }?.charger
            ?: return Result.success()

        val session = detectSession.startSession(charger)
        if (session != null) {
            val serviceIntent = Intent(applicationContext, LocationMonitorService::class.java).apply {
                action = LocationMonitorService.ACTION_START_SESSION
                putExtra(LocationMonitorService.EXTRA_SESSION_ID, session.id)
            }
            applicationContext.startForegroundService(serviceIntent)
        }

        return Result.success()
    }
}
