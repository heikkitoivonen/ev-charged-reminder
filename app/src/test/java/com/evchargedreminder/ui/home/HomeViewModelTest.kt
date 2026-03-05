package com.evchargedreminder.ui.home

import com.evchargedreminder.data.repository.CarRepository
import com.evchargedreminder.data.repository.ChargerRepository
import com.evchargedreminder.data.repository.ChargingSessionRepository
import com.evchargedreminder.domain.model.Car
import com.evchargedreminder.domain.model.Charger
import com.evchargedreminder.domain.model.ChargerType
import com.evchargedreminder.domain.model.ChargingSession
import com.evchargedreminder.domain.model.SessionEndReason
import com.evchargedreminder.domain.usecase.DetectChargingSessionUseCase
import com.evchargedreminder.domain.usecase.EstimateChargingTimeUseCase
import com.evchargedreminder.domain.usecase.ManageSessionUseCase
import com.evchargedreminder.util.LocationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeCarRepo: FakeHomeCarRepo
    private lateinit var fakeChargerRepo: FakeHomeChargerRepo
    private lateinit var fakeSessionRepo: FakeHomeSessionRepo
    private lateinit var manageSession: ManageSessionUseCase
    private lateinit var estimateUseCase: EstimateChargingTimeUseCase
    private lateinit var fakeLocationProvider: FakeLocationProvider
    private lateinit var detectUseCase: DetectChargingSessionUseCase
    private lateinit var nearbyTracker: NearbyChargerTracker

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeCarRepo = FakeHomeCarRepo()
        fakeChargerRepo = FakeHomeChargerRepo()
        fakeSessionRepo = FakeHomeSessionRepo()
        fakeLocationProvider = FakeLocationProvider()
        nearbyTracker = NearbyChargerTracker()
        manageSession = ManageSessionUseCase(fakeSessionRepo, fakeChargerRepo, fakeCarRepo)
        estimateUseCase = EstimateChargingTimeUseCase(fakeCarRepo, fakeChargerRepo)
        detectUseCase = DetectChargingSessionUseCase(
            fakeChargerRepo, fakeCarRepo, fakeSessionRepo, fakeLocationProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            manageSession, fakeCarRepo, fakeChargerRepo, fakeSessionRepo, estimateUseCase,
            detectUseCase, nearbyTracker
        )
    }

    /** Advance enough for the init coroutine to run one refresh cycle, then stop the loop. */
    private fun advanceAndStop(vm: HomeViewModel) {
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
        vm.stopRefreshing()
        testDispatcher.scheduler.runCurrent()
    }

    /** Advance to run pending coroutines after an action. */
    private fun advancePending() {
        testDispatcher.scheduler.advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()
    }

    @Test
    fun `shows not charging when no active session`() {
        val vm = createViewModel()
        advanceAndStop(vm)

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.activeSession)
        assertNull(state.car)
        assertNull(state.charger)
    }

    @Test
    fun `shows active session with car and charger`() {
        runBlocking {
            fakeCarRepo.insert(testCar())
            fakeChargerRepo.insert(testCharger())
            fakeSessionRepo.insert(testActiveSession(carId = 1, chargerId = 1))
        }

        val vm = createViewModel()
        advanceAndStop(vm)

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.activeSession)
        assertNotNull(state.car)
        assertEquals("Tesla", state.car!!.make)
        assertNotNull(state.charger)
        assertEquals("Home Charger", state.charger!!.name)
    }

    @Test
    fun `endSession clears active session`() {
        runBlocking {
            fakeCarRepo.insert(testCar())
            fakeChargerRepo.insert(testCharger())
            fakeSessionRepo.insert(testActiveSession(carId = 1, chargerId = 1))
        }

        val vm = createViewModel()
        advanceAndStop(vm)
        assertNotNull(vm.uiState.value.activeSession)

        vm.endSession()
        advancePending()

        assertNull(vm.uiState.value.activeSession)
    }

    @Test
    fun `showOverrideControls populates edit fields from session`() {
        runBlocking {
            fakeCarRepo.insert(testCar())
            fakeChargerRepo.insert(testCharger())
            fakeSessionRepo.insert(testActiveSession(carId = 1, chargerId = 1))
        }

        val vm = createViewModel()
        advanceAndStop(vm)

        vm.showOverrideControls(true)
        val state = vm.uiState.value
        assertTrue(state.showOverrideControls)
        assertTrue(state.isEditing)
        assertEquals(20, state.editStartPct)
        assertEquals(80, state.editTargetPct)
    }

    @Test
    fun `cancelEditing hides override controls`() {
        runBlocking {
            fakeCarRepo.insert(testCar())
            fakeChargerRepo.insert(testCharger())
            fakeSessionRepo.insert(testActiveSession(carId = 1, chargerId = 1))
        }

        val vm = createViewModel()
        advanceAndStop(vm)

        vm.showOverrideControls(true)
        assertTrue(vm.uiState.value.isEditing)

        vm.cancelEditing()
        assertFalse(vm.uiState.value.isEditing)
        assertFalse(vm.uiState.value.showOverrideControls)
    }

    @Test
    fun `applyOverride updates session and hides controls`() {
        runBlocking {
            fakeCarRepo.insert(testCar())
            fakeChargerRepo.insert(testCharger())
            fakeSessionRepo.insert(testActiveSession(carId = 1, chargerId = 1))
        }

        val vm = createViewModel()
        advanceAndStop(vm)

        vm.showOverrideControls(true)
        vm.updateEditStartPct(10)
        vm.updateEditTargetPct(90)
        vm.applyOverride()
        advancePending()

        val state = vm.uiState.value
        assertFalse(state.isEditing)
        assertFalse(state.showOverrideControls)
        // Session should be updated with new percentages
        runBlocking {
            val updatedSession = fakeSessionRepo.getById(1)
            assertNotNull(updatedSession)
            assertEquals(10, updatedSession!!.startPct)
            assertEquals(90, updatedSession.targetPct)
        }
    }

    @Test
    fun `updateEditStartPct updates state`() {
        val vm = createViewModel()
        advanceAndStop(vm)

        vm.updateEditStartPct(30)
        assertEquals(30, vm.uiState.value.editStartPct)
    }

    @Test
    fun `updateEditTargetPct updates state`() {
        val vm = createViewModel()
        advanceAndStop(vm)

        vm.updateEditTargetPct(95)
        assertEquals(95, vm.uiState.value.editTargetPct)
    }

    @Test
    fun `shows nearby chargers when within radius`() {
        fakeLocationProvider.location = Pair(37.7749, -122.4194) // Same as charger location
        runBlocking {
            fakeChargerRepo.insert(testCharger())
        }

        val vm = createViewModel()
        advanceAndStop(vm)

        val state = vm.uiState.value
        assertEquals(1, state.nearbyChargers.size)
        assertEquals("Home Charger", state.nearbyChargers[0].charger.name)
    }

    @Test
    fun `manualStartSession creates session and refreshes`() {
        fakeLocationProvider.location = Pair(37.7749, -122.4194)
        runBlocking {
            fakeCarRepo.insert(testCar().copy(isFavorite = true))
            fakeChargerRepo.insert(testCharger())
        }

        val vm = createViewModel()
        advanceAndStop(vm)
        assertNull(vm.uiState.value.activeSession)

        vm.manualStartSession(1)
        advancePending()

        assertNotNull(vm.uiState.value.activeSession)
    }

    @Test
    fun `suppressAutoStart adds charger id to suppressed set`() {
        val vm = createViewModel()
        advanceAndStop(vm)

        vm.suppressAutoStart(1)
        assertTrue(1L in vm.uiState.value.suppressedChargerIds)
    }

    @Test
    fun `unsuppressAutoStart removes charger id from suppressed set`() {
        val vm = createViewModel()
        advanceAndStop(vm)

        vm.suppressAutoStart(1)
        assertTrue(1L in vm.uiState.value.suppressedChargerIds)

        vm.unsuppressAutoStart(1)
        assertFalse(1L in vm.uiState.value.suppressedChargerIds)
    }

    private fun testCar() = Car(
        year = 2024, make = "Tesla", model = "Model 3",
        batteryCapacityKwh = 75.0, createdAt = Instant.now()
    )

    private fun testCharger() = Charger(
        name = "Home Charger", latitude = 37.7749, longitude = -122.4194,
        maxChargingSpeedKw = 7.7, chargerType = ChargerType.LEVEL2_EVSE_32A,
        createdAt = Instant.now()
    )

    private fun testActiveSession(carId: Long = 1, chargerId: Long = 1) = ChargingSession(
        carId = carId, chargerId = chargerId,
        startPct = 20, targetPct = 80,
        startedAt = Instant.now().minusSeconds(1800),
        estimatedEndAt = Instant.now().plusSeconds(5400)
    )
}

// --- Fakes ---

private class FakeHomeCarRepo : CarRepository {
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

private class FakeHomeChargerRepo : ChargerRepository {
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

private class FakeLocationProvider : LocationProvider {
    var location: Pair<Double, Double>? = null
    override suspend fun getCurrentLocation(): Pair<Double, Double>? = location
}

private class FakeHomeSessionRepo : ChargingSessionRepository {
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
