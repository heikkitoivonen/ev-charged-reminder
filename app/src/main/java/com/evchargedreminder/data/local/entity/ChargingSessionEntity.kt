package com.evchargedreminder.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.evchargedreminder.domain.model.ChargingSession
import com.evchargedreminder.domain.model.SessionEndReason
import java.time.Instant

@Entity(
    tableName = "charging_sessions",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChargerEntity::class,
            parentColumns = ["id"],
            childColumns = ["chargerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("carId"),
        Index("chargerId")
    ]
)
data class ChargingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val carId: Long,
    val chargerId: Long,
    val startPct: Int,
    val targetPct: Int,
    val startedAt: Instant,
    val estimatedEndAt: Instant,
    val actualEndAt: Instant?,
    val endReason: String?,
    val notificationsSent: Int
) {
    fun toDomainModel() = ChargingSession(
        id = id,
        carId = carId,
        chargerId = chargerId,
        startPct = startPct,
        targetPct = targetPct,
        startedAt = startedAt,
        estimatedEndAt = estimatedEndAt,
        actualEndAt = actualEndAt,
        endReason = endReason?.let { SessionEndReason.valueOf(it) },
        notificationsSent = notificationsSent
    )

    companion object {
        fun fromDomainModel(session: ChargingSession) = ChargingSessionEntity(
            id = session.id,
            carId = session.carId,
            chargerId = session.chargerId,
            startPct = session.startPct,
            targetPct = session.targetPct,
            startedAt = session.startedAt,
            estimatedEndAt = session.estimatedEndAt,
            actualEndAt = session.actualEndAt,
            endReason = session.endReason?.name,
            notificationsSent = session.notificationsSent
        )
    }
}
