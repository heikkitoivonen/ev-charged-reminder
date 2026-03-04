package com.evchargedreminder.domain.model

import java.time.Instant

data class ChargingSession(
    val id: Long = 0,
    val carId: Long,
    val chargerId: Long,
    val startPct: Int,
    val targetPct: Int,
    val startedAt: Instant,
    val estimatedEndAt: Instant,
    val actualEndAt: Instant? = null,
    val endReason: SessionEndReason? = null,
    val notificationsSent: Int = 0
)
