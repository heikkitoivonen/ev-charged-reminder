package com.evchargedreminder.domain.usecase

import com.evchargedreminder.data.repository.CarRepository
import com.evchargedreminder.data.repository.ChargerRepository
import com.evchargedreminder.domain.model.Car
import com.evchargedreminder.domain.model.Charger
import com.evchargedreminder.domain.model.ChargerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

class EstimateChargingTimeUseCaseTest {

    private lateinit var fakeCarRepo: FakeEstimateCarRepo
    private lateinit var fakeChargerRepo: FakeEstimateChargerRepo
    private lateinit var useCase: EstimateChargingTimeUseCase

    @Before
    fun setup() {
        fakeCarRepo = FakeEstimateCarRepo()
        fakeChargerRepo = FakeEstimateChargerRepo()
        useCase = EstimateChargingTimeUseCase(fakeCarRepo, fakeChargerRepo)
    }

    @Test
    fun `estimate with AC charger uses constant rate`() = runTest {
        val carId = fakeCarRepo.insert(testCar())
        val chargerId = fakeChargerRepo.insert(testAcCharger())

        // 75 kWh, 20-80% at 7.7 kW AC → 45/7.7 = 350 min
        val minutes = useCase.estimate(carId, chargerId, startPct = 20, targetPct = 80)
        assertEquals(350, minutes)
    }

    @Test
    fun `estimate with DC charger uses piecewise taper`() = runTest {
        val carId = fakeCarRepo.insert(testCar())
        val chargerId = fakeChargerRepo.insert(testDcCharger())

        // 75 kWh, 20-80% at 150 kW DC → 45/150 = 0.3h = 18 min (100% rate segment)
        val minutes = useCase.estimate(carId, chargerId, startPct = 20, targetPct = 80)
        assertEquals(18, minutes)
    }

    @Test
    fun `estimate with DC charger 10 to 90 spans segments`() = runTest {
        val carId = fakeCarRepo.insert(testCar())
        val chargerId = fakeChargerRepo.insert(testDcCharger())

        // 10-20% at 85%, 20-80% at 100%, 80-90% at 50%
        val minutes = useCase.estimate(carId, chargerId, startPct = 10, targetPct = 90)
        assertEquals(27, minutes)
    }

    @Test
    fun `car maxAcceptRateKw limits effective rate`() = runTest {
        val car = testCar(maxAcceptRateKw = 50.0)
        val carId = fakeCarRepo.insert(car)
        val chargerId = fakeChargerRepo.insert(testDcCharger()) // 150 kW charger

        // Effective rate limited to 50 kW by car
        // 20-80% at 50 kW DC (100% rate): 45/50 = 0.9h = 54 min
        val minutes = useCase.estimate(carId, chargerId, startPct = 20, targetPct = 80)
        assertEquals(54, minutes)
    }

    @Test
    fun `estimate returns 0 when car not found`() = runTest {
        val chargerId = fakeChargerRepo.insert(testAcCharger())
        val minutes = useCase.estimate(999L, chargerId, startPct = 20, targetPct = 80)
        assertEquals(0, minutes)
    }

    @Test
    fun `estimate returns 0 when charger not found`() = runTest {
        val carId = fakeCarRepo.insert(testCar())
        val minutes = useCase.estimate(carId, 999L, startPct = 20, targetPct = 80)
        assertEquals(0, minutes)
    }

    @Test
    fun `estimateWithData uses charger type isAc flag`() {
        val car = testCar()
        val acCharger = testAcCharger().copy(id = 1, maxChargingSpeedKw = 7.7)
        val dcCharger = testDcCharger().copy(id = 2, maxChargingSpeedKw = 150.0)

        val acMinutes = useCase.estimateWithData(car, acCharger, 20, 80)
        val dcMinutes = useCase.estimateWithData(car, dcCharger, 20, 80)

        // AC: 45/7.7 = 350 min, DC: 45/150 = 18 min
        assertEquals(350, acMinutes)
        assertEquals(18, dcMinutes)
    }

    @Test
    fun `estimateWithData with custom AC type`() {
        val car = testCar()
        val charger = Charger(
            id = 1, name = "Custom AC", latitude = 0.0, longitude = 0.0,
            maxChargingSpeedKw = 5.0, chargerType = ChargerType.CUSTOM_AC,
            createdAt = Instant.now()
        )

        // 75 kWh, 20-80% at 5 kW AC → 45/5 = 9h = 540 min
        val minutes = useCase.estimateWithData(car, charger, 20, 80)
        assertEquals(540, minutes)
    }

    @Test
    fun `estimateWithData with custom DC type`() {
        val car = testCar()
        val charger = Charger(
            id = 1, name = "Custom DC", latitude = 0.0, longitude = 0.0,
            maxChargingSpeedKw = 50.0, chargerType = ChargerType.CUSTOM_DC,
            createdAt = Instant.now()
        )

        // 75 kWh, 20-80% at 50 kW DC (100% rate segment) → 45/50 = 0.9h = 54 min
        val minutes = useCase.estimateWithData(car, charger, 20, 80)
        assertEquals(54, minutes)
    }

    // --- Test data helpers ---

    private fun testCar(maxAcceptRateKw: Double? = null) = Car(
        year = 2024, make = "Tesla", model = "Model 3",
        batteryCapacityKwh = 75.0, maxAcceptRateKw = maxAcceptRateKw,
        isFavorite = true, createdAt = Instant.now()
    )

    private fun testAcCharger() = Charger(
        name = "Home L2", latitude = 37.7749, longitude = -122.4194,
        maxChargingSpeedKw = 7.7, chargerType = ChargerType.LEVEL2_EVSE_32A,
        createdAt = Instant.now()
    )

    private fun testDcCharger() = Charger(
        name = "DC Fast", latitude = 37.7749, longitude = -122.4194,
        maxChargingSpeedKw = 150.0, chargerType = ChargerType.DC_FAST_150KW,
        createdAt = Instant.now()
    )
}

// --- Fakes ---

private class FakeEstimateCarRepo : CarRepository {
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

private class FakeEstimateChargerRepo : ChargerRepository {
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
