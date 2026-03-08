package com.evchargedreminder.service

import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.evchargedreminder.domain.model.Charger
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
        val dwellConfirmed = entryTime == 0L // DWELL transition already confirmed by platform

        if (dwellConfirmed) {
            // Platform confirmed dwell — start session using charger from DB
            val charger = findCharger(chargerId)
            if (charger != null) {
                tryStartSession(charger)
            }
            return Result.success()
        }

        // Fallback: manual dwell check via proximity polling
        val proximity = detectSession.checkProximity()
        val nearCharger = proximity.nearestCharger

        if (nearCharger != null && nearCharger.id == chargerId) {
            val elapsedMs = System.currentTimeMillis() - entryTime

            if (elapsedMs >= DWELL_TIME_MS) {
                if (detectSession.shouldStartSession(nearCharger, (elapsedMs / 60_000).toInt().coerceAtLeast(3))) {
                    tryStartSession(nearCharger)
                }
                return Result.success()
            }

            // Schedule next check in 30 seconds
            scheduleNextCheck(chargerId, entryTime)
        }

        return Result.success()
    }

    private suspend fun findCharger(chargerId: Long): Charger? {
        val proximity = detectSession.checkProximity()
        return proximity.nearestCharger?.takeIf { it.id == chargerId }
            ?: detectSession.checkAllNearby().find { it.charger.id == chargerId }?.charger
    }

    private suspend fun tryStartSession(charger: Charger) {
        val session = detectSession.startSession(charger)
        if (session != null) {
            val serviceIntent = Intent(applicationContext, LocationMonitorService::class.java).apply {
                action = LocationMonitorService.ACTION_START_SESSION
                putExtra(LocationMonitorService.EXTRA_SESSION_ID, session.id)
            }
            applicationContext.startForegroundService(serviceIntent)
        }
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
