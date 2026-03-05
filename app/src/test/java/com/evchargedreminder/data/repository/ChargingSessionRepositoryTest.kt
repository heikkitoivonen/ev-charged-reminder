package com.evchargedreminder.data.repository

import com.evchargedreminder.data.local.dao.ChargingSessionDao
import com.evchargedreminder.data.local.entity.ChargingSessionEntity
import com.evchargedreminder.domain.model.ChargingSession
import com.evchargedreminder.domain.model.SessionEndReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ChargingSessionRepositoryTest {

    private lateinit var fakeDao: FakeChargingSessionDao
    private lateinit var repository: ChargingSessionRepositoryImpl

    @Before
    fun setup() {
        fakeDao = FakeChargingSessionDao()
        repository = ChargingSessionRepositoryImpl(fakeDao)
    }

    @Test
    fun `insert returns id and session is retrievable`() = runTest {
        val id = repository.insert(testSession())
        val session = repository.getById(id)
        assertNotNull(session)
        assertEquals(20, session!!.startPct)
        assertEquals(80, session.targetPct)
    }

    @Test
    fun `getAll emits updated list after insert`() = runTest {
        assertTrue(repository.getAll().first().isEmpty())
        repository.insert(testSession())
        assertEquals(1, repository.getAll().first().size)
    }

    @Test
    fun `getActiveSessions returns only sessions without actualEndAt`() = runTest {
        repository.insert(testSession()) // active
        repository.insert(testSession().copy(
            actualEndAt = Instant.now(),
            endReason = SessionEndReason.MANUAL
        )) // ended

        val active = repository.getActiveSessions()
        assertEquals(1, active.size)
        assertNull(active[0].actualEndAt)
    }

    @Test
    fun `getByCarId filters by car`() = runTest {
        repository.insert(testSession(carId = 1))
        repository.insert(testSession(carId = 2))
        repository.insert(testSession(carId = 1))

        val car1Sessions = repository.getByCarId(1).first()
        assertEquals(2, car1Sessions.size)
        assertTrue(car1Sessions.all { it.carId == 1L })
    }

    @Test
    fun `getByChargerId filters by charger`() = runTest {
        repository.insert(testSession(chargerId = 10))
        repository.insert(testSession(chargerId = 20))
        repository.insert(testSession(chargerId = 10))

        val charger10Sessions = repository.getByChargerId(10).first()
        assertEquals(2, charger10Sessions.size)
        assertTrue(charger10Sessions.all { it.chargerId == 10L })
    }

    @Test
    fun `update modifies session fields`() = runTest {
        val id = repository.insert(testSession())
        val session = repository.getById(id)!!
        repository.update(session.copy(targetPct = 90))
        assertEquals(90, repository.getById(id)!!.targetPct)
    }

    @Test
    fun `deleteOlderThan removes ended sessions older than cutoff`() = runTest {
        val oldTime = Instant.now().minusSeconds(400 * 86400L) // 400 days ago
        val recentTime = Instant.now().minusSeconds(30 * 86400L) // 30 days ago

        // Old ended session — should be deleted
        repository.insert(testSession().copy(
            startedAt = oldTime,
            estimatedEndAt = oldTime.plusSeconds(3600),
            actualEndAt = oldTime.plusSeconds(3600),
            endReason = SessionEndReason.TARGET_REACHED
        ))
        // Recent ended session — should remain
        repository.insert(testSession().copy(
            startedAt = recentTime,
            estimatedEndAt = recentTime.plusSeconds(3600),
            actualEndAt = recentTime.plusSeconds(3600),
            endReason = SessionEndReason.MANUAL
        ))
        // Old active session — should remain (no actualEndAt)
        repository.insert(testSession().copy(
            startedAt = oldTime,
            estimatedEndAt = oldTime.plusSeconds(3600)
        ))

        val cutoff = Instant.now().minusSeconds(365 * 86400L).toEpochMilli()
        repository.deleteOlderThan(cutoff)

        val all = repository.getAll().first()
        assertEquals(2, all.size)
    }

    private fun testSession(
        carId: Long = 1,
        chargerId: Long = 1
    ) = ChargingSession(
        carId = carId,
        chargerId = chargerId,
        startPct = 20,
        targetPct = 80,
        startedAt = Instant.now(),
        estimatedEndAt = Instant.now().plusSeconds(3600)
    )
}

private class FakeChargingSessionDao : ChargingSessionDao {
    private var nextId = 1L
    private val sessions = mutableListOf<ChargingSessionEntity>()
    private val flow = MutableStateFlow<List<ChargingSessionEntity>>(emptyList())

    override fun getAll(): Flow<List<ChargingSessionEntity>> =
        flow.map { it.sortedByDescending { s -> s.startedAt } }

    override suspend fun getById(id: Long): ChargingSessionEntity? =
        sessions.find { it.id == id }

    override suspend fun getActiveSessions(): List<ChargingSessionEntity> =
        sessions.filter { it.actualEndAt == null }

    override fun getByCarId(carId: Long): Flow<List<ChargingSessionEntity>> =
        flow.map { it.filter { s -> s.carId == carId }.sortedByDescending { s -> s.startedAt } }

    override fun getByChargerId(chargerId: Long): Flow<List<ChargingSessionEntity>> =
        flow.map { it.filter { s -> s.chargerId == chargerId }.sortedByDescending { s -> s.startedAt } }

    override suspend fun insert(session: ChargingSessionEntity): Long {
        val s = session.copy(id = nextId++)
        sessions.add(s)
        flow.value = sessions.toList()
        return s.id
    }

    override suspend fun update(session: ChargingSessionEntity) {
        val i = sessions.indexOfFirst { it.id == session.id }
        if (i >= 0) { sessions[i] = session; flow.value = sessions.toList() }
    }

    override suspend fun deleteOlderThan(cutoff: Long) {
        sessions.removeAll {
            it.actualEndAt != null && it.startedAt.toEpochMilli() < cutoff
        }
        flow.value = sessions.toList()
    }
}
