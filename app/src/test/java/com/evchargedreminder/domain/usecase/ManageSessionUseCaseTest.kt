package com.evchargedreminder.domain.usecase

import com.evchargedreminder.data.repository.ChargerRepository
import com.evchargedreminder.data.repository.ChargingSessionRepository
import com.evchargedreminder.domain.model.Charger
import com.evchargedreminder.domain.model.ChargerType
import com.evchargedreminder.domain.model.ChargingSession
import com.evchargedreminder.domain.model.SessionEndReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ManageSessionUseCaseTest {

    private lateinit var fakeSessionRepo: FakeSessionRepo
    private lateinit var fakeChargerRepo: FakeChargerRepo
    private lateinit var useCase: ManageSessionUseCase

    @Before
    fun setup() {
        fakeSessionRepo = FakeSessionRepo()
        fakeChargerRepo = FakeChargerRepo()
        useCase = ManageSessionUseCase(fakeSessionRepo, fakeChargerRepo)
    }

    @Test
    fun `getActiveSession returns session with no actualEndAt`() = runTest {
        fakeSessionRepo.insert(testSession())
        val active = useCase.getActiveSession()
        assertNotNull(active)
    }

    @Test
    fun `getActiveSession returns null when all sessions ended`() = runTest {
        fakeSessionRepo.insert(
            testSession().copy(
                actualEndAt = Instant.now(),
                endReason = SessionEndReason.MANUAL
            )
        )
        val active = useCase.getActiveSession()
        assertNull(active)
    }

    @Test
    fun `endSession sets actualEndAt and reason`() = runTest {
        val id = fakeSessionRepo.insert(testSession())
        useCase.endSession(id, SessionEndReason.TARGET_REACHED)

        val session = fakeSessionRepo.getById(id)!!
        assertNotNull(session.actualEndAt)
        assertEquals(SessionEndReason.TARGET_REACHED, session.endReason)
    }

    @Test
    fun `shouldEndByUserLeft returns true after 15 min away and re-entry`() {
        assertTrue(useCase.shouldEndByUserLeft(isNearCharger = true, minutesAway = 16))
    }

    @Test
    fun `shouldEndByUserLeft returns false when away less than 15 min`() {
        assertEquals(false, useCase.shouldEndByUserLeft(isNearCharger = true, minutesAway = 10))
    }

    @Test
    fun `shouldEndByUserLeft returns false when not near charger`() {
        assertEquals(false, useCase.shouldEndByUserLeft(isNearCharger = false, minutesAway = 20))
    }

    @Test
    fun `shouldNotify returns notification number when within window`() = runTest {
        val chargerId = fakeChargerRepo.insert(testCharger())
        val session = testSession().copy(
            chargerId = chargerId,
            estimatedEndAt = Instant.now().plusSeconds(600) // 10 min from now
        )
        val sessionId = fakeSessionRepo.insert(session)
        val savedSession = fakeSessionRepo.getById(sessionId)!!

        val notifNumber = useCase.getNotificationToSend(savedSession)
        assertEquals(1, notifNumber) // First notification (within 15 min window)
    }

    @Test
    fun `shouldNotify returns 0 when all 3 notifications sent`() = runTest {
        val chargerId = fakeChargerRepo.insert(testCharger())
        val session = testSession().copy(
            chargerId = chargerId,
            notificationsSent = 3,
            estimatedEndAt = Instant.now().plusSeconds(60)
        )
        val sessionId = fakeSessionRepo.insert(session)
        val savedSession = fakeSessionRepo.getById(sessionId)!!

        assertEquals(0, useCase.getNotificationToSend(savedSession))
    }

    @Test
    fun `incrementNotificationCount updates count`() = runTest {
        val id = fakeSessionRepo.insert(testSession())
        assertEquals(0, fakeSessionRepo.getById(id)!!.notificationsSent)

        useCase.incrementNotificationCount(id)
        assertEquals(1, fakeSessionRepo.getById(id)!!.notificationsSent)
    }

    private fun testSession() = ChargingSession(
        carId = 1,
        chargerId = 1,
        startPct = 20,
        targetPct = 80,
        startedAt = Instant.now(),
        estimatedEndAt = Instant.now().plusSeconds(3600)
    )

    private fun testCharger() = Charger(
        name = "Home Charger",
        latitude = 37.7749,
        longitude = -122.4194,
        maxChargingSpeedKw = 7.7,
        chargerType = ChargerType.LEVEL2_EVSE_32A,
        notifyMinutesBefore = 15,
        createdAt = Instant.now()
    )
}

// --- Fakes ---

private class FakeSessionRepo : ChargingSessionRepository {
    private var nextId = 1L
    private val sessions = mutableListOf<ChargingSession>()
    private val flow = MutableStateFlow<List<ChargingSession>>(emptyList())

    override fun getAll(): Flow<List<ChargingSession>> = flow.map { it.toList() }
    override suspend fun getById(id: Long): ChargingSession? = sessions.find { it.id == id }
    override suspend fun getActiveSessions(): List<ChargingSession> =
        sessions.filter { it.actualEndAt == null }
    override fun getByCarId(carId: Long): Flow<List<ChargingSession>> =
        flow.map { it.filter { s -> s.carId == carId } }
    override fun getByChargerId(chargerId: Long): Flow<List<ChargingSession>> =
        flow.map { it.filter { s -> s.chargerId == chargerId } }
    override suspend fun insert(session: ChargingSession): Long {
        val s = session.copy(id = nextId++)
        sessions.add(s)
        flow.value = sessions.toList()
        return s.id
    }
    override suspend fun update(session: ChargingSession) {
        val i = sessions.indexOfFirst { it.id == session.id }
        if (i >= 0) { sessions[i] = session; flow.value = sessions.toList() }
    }
    override suspend fun deleteOlderThan(cutoffEpochMilli: Long) {
        sessions.removeAll { it.actualEndAt != null && it.startedAt.toEpochMilli() < cutoffEpochMilli }
        flow.value = sessions.toList()
    }
}

private class FakeChargerRepo : ChargerRepository {
    private var nextId = 1L
    private val chargers = mutableListOf<Charger>()
    private val flow = MutableStateFlow<List<Charger>>(emptyList())

    override fun getAll(): Flow<List<Charger>> = flow.map { it.toList() }
    override suspend fun getById(id: Long): Charger? = chargers.find { it.id == id }
    override suspend fun insert(charger: Charger): Long {
        val c = charger.copy(id = nextId++)
        chargers.add(c)
        flow.value = chargers.toList()
        return c.id
    }
    override suspend fun update(charger: Charger) {
        val i = chargers.indexOfFirst { it.id == charger.id }
        if (i >= 0) { chargers[i] = charger; flow.value = chargers.toList() }
    }
    override suspend fun delete(charger: Charger) {
        chargers.removeAll { it.id == charger.id }
        flow.value = chargers.toList()
    }
}
