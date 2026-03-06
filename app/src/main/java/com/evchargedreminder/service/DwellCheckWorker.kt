package com.evchargedreminder.service

import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.evchargedreminder.domain.usecase.DetectChargingSessionUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class DwellCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val detectSession: DetectChargingSessionUseCase
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_CHARGER_ID = "charger_id"
        const val KEY_DWELL_COUNT = "dwell_count"
        private const val DWELL_THRESHOLD = 2
    }

    override suspend fun doWork(): Result {
        val chargerId = inputData.getLong(KEY_CHARGER_ID, -1L)
        if (chargerId == -1L) return Result.failure()

        val dwellCount = inputData.getInt(KEY_DWELL_COUNT, 0)

        val proximity = detectSession.checkProximity()
        val nearCharger = proximity.nearestCharger

        if (nearCharger != null && nearCharger.id == chargerId) {
            val newDwellCount = dwellCount + 1

            if (newDwellCount >= DWELL_THRESHOLD) {
                // Dwell threshold met — start session
                if (detectSession.shouldStartSession(nearCharger, newDwellCount)) {
                    val session = detectSession.startSession(nearCharger)
                    if (session != null) {
                        // Start foreground service
                        val serviceIntent = Intent(applicationContext, LocationMonitorService::class.java).apply {
                            action = LocationMonitorService.ACTION_START_SESSION
                            putExtra(LocationMonitorService.EXTRA_SESSION_ID, session.id)
                        }
                        applicationContext.startForegroundService(serviceIntent)
                    }
                }
                return Result.success()
            }

            // Schedule next check in 1 minute
            scheduleNextCheck(chargerId, newDwellCount)
        }
        // If not near charger, dwell resets — don't schedule further checks

        return Result.success()
    }

    private fun scheduleNextCheck(chargerId: Long, dwellCount: Int) {
        val inputData = Data.Builder()
            .putLong(KEY_CHARGER_ID, chargerId)
            .putInt(KEY_DWELL_COUNT, dwellCount)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DwellCheckWorker>()
            .setInputData(inputData)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }
}
