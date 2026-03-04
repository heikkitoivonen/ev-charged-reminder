package com.evchargedreminder.domain.usecase

import com.evchargedreminder.data.repository.CarRepository
import com.evchargedreminder.data.repository.ChargerRepository
import com.evchargedreminder.data.repository.ChargingSessionRepository
import com.evchargedreminder.domain.model.Car
import com.evchargedreminder.domain.model.Charger
import com.evchargedreminder.domain.model.ChargerType
import com.evchargedreminder.domain.model.ChargingSession
import com.evchargedreminder.domain.model.SessionEndReason
import com.evchargedreminder.util.LocationProvider
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

class DetectChargingSessionUseCaseTest {

    private lateinit var fakeChargerRepo: FakeChargerRepository
    private lateinit var fakeCarRepo: FakeCarRepository
    private lateinit var fakeSessionRepo: FakeChargingSessionRepository
    private lateinit var fakeLocationProvider: FakeLocationProvider
    private lateinit var useCase: DetectChargingSessionUseCase

    @Before
    fun setup() {
        fakeChargerRepo = FakeChargerRepository()
        fakeCarRepo = FakeCarRepository()
        fakeSessionRepo = FakeChargingSessionRepository()
        fakeLocationProvider = FakeLocationProvider()
        useCase = DetectChargingSessionUseCase(
            fakeChargerRepo, fakeCarRepo, fakeSessionRepo, fakeLocationProvider
        )
    }

    @Test
    fun `checkProximity returns nearest charger within radius`() = runTest {
        val charger = testCharger()
        fakeChargerRepo.insert(charger)
        // Set location very close to charger
        fakeLocationProvider.location = Pair(37.7749, -122.4194)

        val result = useCase.checkProximity()
        assertNotNull(result.nearestCharger)
        assertEquals("Home Charger", result.nearestCharger!!.name)
    }

    @Test
    fun `checkProximity returns null when no charger nearby`() = runTest {
        val charger = testCharger()
        fakeChargerRepo.insert(charger)
        // Set location far from charger
        fakeLocationProvider.location = Pair(40.7128, -74.0060) // NYC

        val result = useCase.checkProximity()
        assertNull(result.nearestCharger)
    }

    @Test
    fun `checkProximity returns null when location unavailable`() = runTest {
        fakeChargerRepo.insert(testCharger())
        fakeLocationProvider.location = null

        val result = useCase.checkProximity()
        assertNull(result.nearestCharger)
    }

    @Test
    fun `shouldStartSession returns true after 3 min dwell`() = runTest {
        val charger = testCharger()
        val id = fakeChargerRepo.insert(charger)
        val savedCharger = fakeChargerRepo.getById(id)!!

        assertTrue(useCase.shouldStartSession(savedCharger, 3))
    }

    @Test
    fun `shouldStartSession returns false when dwell too short`() = runTest {
        val charger = testCharger()
        val id = fakeChargerRepo.insert(charger)
        val savedCharger = fakeChargerRepo.getById(id)!!

        assertEquals(false, useCase.shouldStartSession(savedCharger, 2))
    }

    @Test
    fun `shouldStartSession returns false when session already active`() = runTest {
        val charger = testCharger()
        val chargerId = fakeChargerRepo.insert(charger)
        val savedCharger = fakeChargerRepo.getById(chargerId)!!

        // Insert an active session for this charger
        fakeSessionRepo.insert(
            ChargingSession(
                carId = 1, chargerId = chargerId,
                startPct = 20, targetPct = 80,
                startedAt = Instant.now(),
                estimatedEndAt = Instant.now().plusSeconds(3600)
            )
        )

        assertEquals(false, useCase.shouldStartSession(savedCharger, 5))
    }

    @Test
    fun `startSession creates session with favorite car defaults`() = runTest {
        val car = Car(
            year = 2024, make = "Tesla", model = "Model 3",
            batteryCapacityKwh = 75.0, isFavorite = true
        )
        fakeCarRepo.insertAndSetFavorite(car)

        val charger = testCharger()
        val chargerId = fakeChargerRepo.insert(charger)
        val savedCharger = fakeChargerRepo.getById(chargerId)!!

        val session = useCase.startSession(savedCharger)
        assertNotNull(session)
        assertEquals(20, session!!.startPct) // EV default
        assertEquals(80, session.targetPct) // EV default
    }

    @Test
    fun `startSession returns null when no favorite car`() = runTest {
        val charger = testCharger()
        val chargerId = fakeChargerRepo.insert(charger)
        val savedCharger = fakeChargerRepo.getById(chargerId)!!

        val session = useCase.startSession(savedCharger)
        assertNull(session)
    }

    private fun testCharger() = Charger(
        name = "Home Charger",
        latitude = 37.7749,
        longitude = -122.4194,
        radiusMeters = 100,
        maxChargingSpeedKw = 7.7,
        chargerType = ChargerType.LEVEL2_EVSE_32A,
        createdAt = Instant.now()
    )
}

// --- Fakes ---

private class FakeLocationProvider : LocationProvider {
    var location: Pair<Double, Double>? = null
    override suspend fun getCurrentLocation(): Pair<Double, Double>? = location
}

private class FakeChargerRepository : ChargerRepository {
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

private class FakeCarRepository : CarRepository {
    private var nextId = 1L
    private val cars = mutableListOf<Car>()
    private val flow = MutableStateFlow<List<Car>>(emptyList())

    override fun getAll(): Flow<List<Car>> = flow.map { it.toList() }
    override suspend fun getById(id: Long): Car? = cars.find { it.id == id }
    override suspend fun getFavorite(): Car? = cars.find { it.isFavorite }
    override suspend fun count(): Int = cars.size
    override suspend fun insert(car: Car): Long {
        val c = car.copy(id = nextId++)
        cars.add(c)
        flow.value = cars.toList()
        return c.id
    }
    override suspend fun update(car: Car) {
        val i = cars.indexOfFirst { it.id == car.id }
        if (i >= 0) { cars[i] = car; flow.value = cars.toList() }
    }
    override suspend fun delete(car: Car) {
        cars.removeAll { it.id == car.id }
        flow.value = cars.toList()
    }
    override suspend fun setFavorite(carId: Long) {
        cars.forEachIndexed { i, car -> cars[i] = car.copy(isFavorite = car.id == carId) }
        flow.value = cars.toList()
    }

    suspend fun insertAndSetFavorite(car: Car) {
        val id = insert(car)
        setFavorite(id)
    }
}

private class FakeChargingSessionRepository : ChargingSessionRepository {
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
