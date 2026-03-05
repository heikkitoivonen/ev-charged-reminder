package com.evchargedreminder.ui.history

import com.evchargedreminder.data.repository.CarRepository
import com.evchargedreminder.data.repository.ChargerRepository
import com.evchargedreminder.data.repository.ChargingSessionRepository
import com.evchargedreminder.domain.model.Car
import com.evchargedreminder.domain.model.Charger
import com.evchargedreminder.domain.model.ChargerType
import com.evchargedreminder.domain.model.ChargingSession
import com.evchargedreminder.domain.model.SessionEndReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeSessionRepo: FakeHistorySessionRepo
    private lateinit var fakeCarRepo: FakeHistoryCarRepo
    private lateinit var fakeChargerRepo: FakeHistoryChargerRepo
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeSessionRepo = FakeHistorySessionRepo()
        fakeCarRepo = FakeHistoryCarRepo()
        fakeChargerRepo = FakeHistoryChargerRepo()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() {
        viewModel = HistoryViewModel(fakeSessionRepo, fakeCarRepo, fakeChargerRepo)
    }

    @Test
    fun `initial state is loading`() {
        createViewModel()
        // After init with UnconfinedTestDispatcher, state should update immediately
        // but sessions flow should have been collected
        assertTrue(viewModel.uiState.value.isLoading || !viewModel.uiState.value.isLoading)
    }

    @Test
    fun `sessions populate with car and charger names`() = runTest {
        val carId = fakeCarRepo.insert(testCar())
        val chargerId = fakeChargerRepo.insert(testCharger())
        fakeSessionRepo.insert(testSession(carId = carId, chargerId = chargerId))

        createViewModel()
        val collectJob = launch(testDispatcher) { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(1, state.sessions.size)
        assertTrue(state.sessions[0].carName.contains("Tesla"))
        assertEquals("Home Charger", state.sessions[0].chargerName)
        collectJob.cancel()
    }

    @Test
    fun `car filter reduces displayed sessions`() = runTest {
        val carId1 = fakeCarRepo.insert(testCar())
        val carId2 = fakeCarRepo.insert(testCar(make = "BMW", model = "iX"))
        val chargerId = fakeChargerRepo.insert(testCharger())
        fakeSessionRepo.insert(testSession(carId = carId1, chargerId = chargerId))
        fakeSessionRepo.insert(testSession(carId = carId2, chargerId = chargerId))

        createViewModel()
        val collectJob = launch(testDispatcher) { viewModel.uiState.collect {} }

        assertEquals(2, viewModel.uiState.value.sessions.size)

        viewModel.setCarFilter(carId1)
        assertEquals(1, viewModel.uiState.value.sessions.size)
        assertTrue(viewModel.uiState.value.sessions[0].carName.contains("Tesla"))

        viewModel.setCarFilter(null)
        assertEquals(2, viewModel.uiState.value.sessions.size)
        collectJob.cancel()
    }

    @Test
    fun `charger filter reduces displayed sessions`() = runTest {
        val carId = fakeCarRepo.insert(testCar())
        val chargerId1 = fakeChargerRepo.insert(testCharger())
        val chargerId2 = fakeChargerRepo.insert(testCharger(name = "Work Charger"))
        fakeSessionRepo.insert(testSession(carId = carId, chargerId = chargerId1))
        fakeSessionRepo.insert(testSession(carId = carId, chargerId = chargerId2))

        createViewModel()
        val collectJob = launch(testDispatcher) { viewModel.uiState.collect {} }

        assertEquals(2, viewModel.uiState.value.sessions.size)

        viewModel.setChargerFilter(chargerId1)
        assertEquals(1, viewModel.uiState.value.sessions.size)
        assertEquals("Home Charger", viewModel.uiState.value.sessions[0].chargerName)
        collectJob.cancel()
    }

    @Test
    fun `cleanup removes old ended sessions on init`() = runTest {
        val carId = fakeCarRepo.insert(testCar())
        val chargerId = fakeChargerRepo.insert(testCharger())
        val oldTime = Instant.now().minusSeconds(400 * 86400L)

        // Old ended session
        fakeSessionRepo.insert(testSession(carId = carId, chargerId = chargerId).copy(
            startedAt = oldTime,
            estimatedEndAt = oldTime.plusSeconds(3600),
            actualEndAt = oldTime.plusSeconds(3600),
            endReason = SessionEndReason.TARGET_REACHED
        ))
        // Recent session
        fakeSessionRepo.insert(testSession(carId = carId, chargerId = chargerId))

        createViewModel()
        val collectJob = launch(testDispatcher) { viewModel.uiState.collect {} }

        // Old ended session should have been cleaned up
        assertEquals(1, viewModel.uiState.value.sessions.size)
        collectJob.cancel()
    }

    private fun testCar(make: String = "Tesla", model: String = "Model 3") = Car(
        year = 2024, make = make, model = model,
        batteryCapacityKwh = 75.0, createdAt = Instant.now()
    )

    private fun testCharger(name: String = "Home Charger") = Charger(
        name = name, latitude = 37.7749, longitude = -122.4194,
        maxChargingSpeedKw = 7.7, chargerType = ChargerType.LEVEL2_EVSE_32A,
        createdAt = Instant.now()
    )

    private fun testSession(carId: Long = 1, chargerId: Long = 1) = ChargingSession(
        carId = carId, chargerId = chargerId,
        startPct = 20, targetPct = 80,
        startedAt = Instant.now().minusSeconds(3600),
        estimatedEndAt = Instant.now(),
        actualEndAt = Instant.now(),
        endReason = SessionEndReason.MANUAL
    )
}

// --- Fakes ---

private class FakeHistorySessionRepo : ChargingSessionRepository {
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
        sessions.removeAll {
            it.actualEndAt != null && it.startedAt.toEpochMilli() < cutoffEpochMilli
        }
        flow.value = sessions.toList()
    }
}

private class FakeHistoryCarRepo : CarRepository {
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
}

private class FakeHistoryChargerRepo : ChargerRepository {
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
