package com.evchargedreminder.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) return

        when (geofencingEvent.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
                for (geofence in triggeringGeofences) {
                    val chargerId = geofence.requestId.toLongOrNull() ?: continue
                    enqueueDwellCheck(context, chargerId)
                }
            }
            // EXIT is a no-op — session end is handled by the foreground service polling
        }
    }

    private fun enqueueDwellCheck(context: Context, chargerId: Long) {
        val inputData = Data.Builder()
            .putLong(DwellCheckWorker.KEY_CHARGER_ID, chargerId)
            .putLong(DwellCheckWorker.KEY_ENTRY_TIME, System.currentTimeMillis())
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DwellCheckWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
