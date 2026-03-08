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
            Geofence.GEOFENCE_TRANSITION_ENTER,
            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                // Start session immediately on any geofence entry or dwell event.
                // Background location updates are infrequent, so we don't wait.
                val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
                for (geofence in triggeringGeofences) {
                    val chargerId = geofence.requestId.toLongOrNull() ?: continue
                    enqueueSessionStart(context, chargerId)
                }
            }
            // EXIT is a no-op — session end is handled by the foreground service polling
        }
    }

    private fun enqueueSessionStart(context: Context, chargerId: Long) {
        val inputData = Data.Builder()
            .putLong(DwellCheckWorker.KEY_CHARGER_ID, chargerId)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DwellCheckWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
