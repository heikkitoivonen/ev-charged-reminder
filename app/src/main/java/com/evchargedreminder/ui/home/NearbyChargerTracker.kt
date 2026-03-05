package com.evchargedreminder.ui.home

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton that tracks when each charger was first detected as nearby.
 * Survives ViewModel recreation (e.g. navigating away and back).
 * Resets when the app process is killed, which is acceptable since
 * DwellCheckWorker handles actual auto-start independently.
 */
@Singleton
class NearbyChargerTracker @Inject constructor() {

    private val firstSeen = mutableMapOf<Long, Instant>()

    fun update(nearbyChargerIds: Set<Long>, now: Instant = Instant.now()) {
        firstSeen.keys.retainAll(nearbyChargerIds)
        for (id in nearbyChargerIds) {
            firstSeen.putIfAbsent(id, now)
        }
    }

    fun getFirstSeen(chargerId: Long): Instant? = firstSeen[chargerId]

    fun clear() {
        firstSeen.clear()
    }
}
