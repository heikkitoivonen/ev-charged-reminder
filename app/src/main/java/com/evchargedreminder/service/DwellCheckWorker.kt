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
        const val KEY_ENTRY_TIME = "entry_time"
        private const val DWELL_TIME_MS = 120_000L // 2 minutes
        private const val CHECK_INTERVAL_SECONDS = 30L
    }

    override suspend fun doWork(): Result {
        val chargerId = inputData.getLong(KEY_CHARGER_ID, -1L)
        if (chargerId == -1L) return Result.failure()

        val entryTime = inputData.getLong(KEY_ENTRY_TIME, System.currentTimeMillis())

        val proximity = detectSession.checkProximity()
        val nearCharger = proximity.nearestCharger

        if (nearCharger != null && nearCharger.id == chargerId) {
            val elapsedMs = System.currentTimeMillis() - entryTime
            val dwellMinutes = (elapsedMs / 60_000).toInt()

            if (elapsedMs >= DWELL_TIME_MS) {
                // Dwell threshold met — start session
                if (detectSession.shouldStartSession(nearCharger, dwellMinutes.coerceAtLeast(3))) {
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

            // Schedule next check in 30 seconds
            scheduleNextCheck(chargerId, entryTime)
        }
        // If not near charger, dwell resets — don't schedule further checks

        return Result.success()
    }

    private fun scheduleNextCheck(chargerId: Long, entryTime: Long) {
        val inputData = Data.Builder()
            .putLong(KEY_CHARGER_ID, chargerId)
            .putLong(KEY_ENTRY_TIME, entryTime)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DwellCheckWorker>()
            .setInputData(inputData)
            .setInitialDelay(CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }
}
