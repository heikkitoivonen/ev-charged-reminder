package com.evchargedreminder.ui.onboarding

import com.evchargedreminder.data.OnboardingPreferences
import com.evchargedreminder.data.repository.CarRepository
import com.evchargedreminder.data.repository.ChargerRepository
import com.evchargedreminder.domain.model.Car
import com.evchargedreminder.domain.model.Charger
import com.evchargedreminder.domain.model.ChargerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeCarRepo: FakeOnboardingCarRepo
    private lateinit var fakeChargerRepo: FakeOnboardingChargerRepo
    private lateinit var fakePrefs: FakeOnboardingPreferences
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeCarRepo = FakeOnboardingCarRepo()
        fakeChargerRepo = FakeOnboardingChargerRepo()
        fakePrefs = FakeOnboardingPreferences()
        viewModel = OnboardingViewModel(fakeCarRepo, fakeChargerRepo, fakePrefs)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial step is WELCOME`() {
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.step)
    }

    @Test
    fun `nextStep advances from WELCOME to ADD_CAR`() {
        viewModel.nextStep()
        assertEquals(OnboardingStep.ADD_CAR, viewModel.uiState.value.step)
    }

    @Test
    fun `nextStep advances through all steps`() {
        viewModel.nextStep() // WELCOME -> ADD_CAR
        assertEquals(OnboardingStep.ADD_CAR, viewModel.uiState.value.step)
        viewModel.nextStep() // ADD_CAR -> PERMISSIONS
        assertEquals(OnboardingStep.PERMISSIONS, viewModel.uiState.value.step)
        viewModel.nextStep() // PERMISSIONS -> ADD_CHARGER
        assertEquals(OnboardingStep.ADD_CHARGER, viewModel.uiState.value.step)
        viewModel.nextStep() // ADD_CHARGER -> DONE
        assertEquals(OnboardingStep.DONE, viewModel.uiState.value.step)
    }

    @Test
    fun `previousStep goes back`() {
        viewModel.nextStep() // WELCOME -> ADD_CAR
        viewModel.nextStep() // ADD_CAR -> PERMISSIONS
        viewModel.previousStep() // PERMISSIONS -> ADD_CAR
        assertEquals(OnboardingStep.ADD_CAR, viewModel.uiState.value.step)
    }

    @Test
    fun `saveCar inserts car and sets favorite`() = runTest {
        viewModel.updateMake("Tesla")
        viewModel.updateModel("Model 3")
        viewModel.updateBatteryCapacity("75.0")

        viewModel.saveCar()

        val car = fakeCarRepo.getFavorite()
        assertNotNull(car)
        assertEquals("Tesla", car!!.make)
        assertEquals("Model 3", car.model)
        assertEquals(75.0, car.batteryCapacityKwh, 0.01)
        assertTrue(car.isFavorite)
        assertEquals(OnboardingStep.PERMISSIONS, viewModel.uiState.value.step)
    }

    @Test
    fun `saveCar shows error when make is blank`() {
        viewModel.updateModel("Model 3")
        viewModel.updateBatteryCapacity("75.0")

        viewModel.saveCar()

        assertNotNull(viewModel.uiState.value.error)
        assertEquals(OnboardingStep.WELCOME, viewModel.uiState.value.step) // didn't advance
    }

    @Test
    fun `saveCar shows error when battery capacity is invalid`() {
        viewModel.updateMake("Tesla")
        viewModel.updateModel("Model 3")
        viewModel.updateBatteryCapacity("abc")

        viewModel.saveCar()

        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun `skipCharger advances to DONE and marks completed`() {
        viewModel.nextStep() // -> ADD_CAR
        viewModel.nextStep() // -> PERMISSIONS
        viewModel.nextStep() // -> ADD_CHARGER
        viewModel.skipCharger()
        assertEquals(OnboardingStep.DONE, viewModel.uiState.value.step)
        assertTrue(fakePrefs.isCompleted)
    }

    @Test
    fun `onLocationPermissionResult updates state`() {
        viewModel.onLocationPermissionResult(true)
        assertTrue(viewModel.uiState.value.locationGranted)
    }

    @Test
    fun `onBackgroundLocationPermissionResult updates state`() {
        viewModel.onBackgroundLocationPermissionResult(true)
        assertTrue(viewModel.uiState.value.backgroundLocationGranted)
    }

    @Test
    fun `onNotificationPermissionResult updates state`() {
        viewModel.onNotificationPermissionResult(true)
        assertTrue(viewModel.uiState.value.notificationGranted)
    }

    @Test
    fun `completeOnboarding marks preferences as completed`() {
        viewModel.completeOnboarding()
        assertTrue(fakePrefs.isCompleted)
    }

    @Test
    fun `updateMake populates available models`() {
        viewModel.updateMake("Tesla")
        val state = viewModel.uiState.value
        assertTrue(state.availableModels.isNotEmpty())
        assertEquals("Tesla", state.make)
    }

    @Test
    fun `setCustomMode clears form fields`() {
        viewModel.updateMake("Tesla")
        viewModel.setCustomMode(true)
        val state = viewModel.uiState.value
        assertTrue(state.isCustom)
        assertEquals("", state.make)
        assertEquals("", state.model)
        assertTrue(state.availableModels.isEmpty())
    }

    @Test
    fun `saveCar in custom mode succeeds with blank model`() = runTest {
        viewModel.setCustomMode(true)
        viewModel.updateMake("My Red HotRod")
        viewModel.updateBatteryCapacity("50.0")

        viewModel.saveCar()

        val car = fakeCarRepo.getFavorite()
        assertNotNull(car)
        assertEquals("My Red HotRod", car!!.make)
        assertEquals("", car.model)
        assertEquals(OnboardingStep.PERMISSIONS, viewModel.uiState.value.step)
    }

    @Test
    fun `saveCar in custom mode shows error when name is blank`() {
        viewModel.setCustomMode(true)
        viewModel.updateBatteryCapacity("50.0")

        viewModel.saveCar()

        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun `checkChargerAdded advances to DONE when charger exists`() = runTest {
        // Navigate to ADD_CHARGER step
        viewModel.nextStep() // -> ADD_CAR
        viewModel.nextStep() // -> PERMISSIONS
        viewModel.nextStep() // -> ADD_CHARGER
        assertEquals(OnboardingStep.ADD_CHARGER, viewModel.uiState.value.step)

        // Simulate a charger being added
        fakeChargerRepo.insert(Charger(
            name = "Home", latitude = 0.0, longitude = 0.0,
            maxChargingSpeedKw = 7.7, chargerType = ChargerType.LEVEL2_EVSE_32A
        ))

        viewModel.checkChargerAdded()

        assertEquals(OnboardingStep.DONE, viewModel.uiState.value.step)
        assertTrue(fakePrefs.isCompleted)
    }

    @Test
    fun `checkChargerAdded does nothing when no chargers`() = runTest {
        viewModel.nextStep() // -> ADD_CAR
        viewModel.nextStep() // -> PERMISSIONS
        viewModel.nextStep() // -> ADD_CHARGER

        viewModel.checkChargerAdded()

        assertEquals(OnboardingStep.ADD_CHARGER, viewModel.uiState.value.step)
    }
}

// --- Fakes ---

private class FakeOnboardingPreferences : OnboardingPreferences {
    private var _completed = false
    override var isCompleted: Boolean
        get() = _completed
        set(value) { _completed = value }
}

private class FakeOnboardingCarRepo : CarRepository {
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

private class FakeOnboardingChargerRepo : ChargerRepository {
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
