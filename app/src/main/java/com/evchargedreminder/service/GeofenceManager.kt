package com.evchargedreminder.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.evchargedreminder.domain.model.Charger
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface GeofenceManager {
    fun registerGeofences(chargers: List<Charger>)
    fun unregisterAll()
}

@Singleton
class GeofenceManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GeofenceManager {

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            setPackage(context.packageName)
        }
        PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    companion object {
        private const val LOITERING_DELAY_MS = 120_000 // 2 minutes
    }

    @SuppressLint("MissingPermission")
    override fun registerGeofences(chargers: List<Charger>) {
        if (chargers.isEmpty()) return

        val geofences = chargers.map { charger ->
            Geofence.Builder()
                .setRequestId(charger.id.toString())
                .setCircularRegion(
                    charger.latitude,
                    charger.longitude,
                    charger.radiusMeters.toFloat()
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setLoiteringDelay(LOITERING_DELAY_MS)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or
                        Geofence.GEOFENCE_TRANSITION_DWELL or
                        Geofence.GEOFENCE_TRANSITION_EXIT
                )
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent)
    }

    override fun unregisterAll() {
        geofencingClient.removeGeofences(geofencePendingIntent)
    }
}
