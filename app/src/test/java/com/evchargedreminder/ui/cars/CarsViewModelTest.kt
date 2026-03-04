package com.evchargedreminder.ui.cars

import com.evchargedreminder.data.repository.CarRepository
import com.evchargedreminder.domain.model.Car
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
class CarsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeCarRepository
    private lateinit var viewModel: CarsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeCarRepository()
        viewModel = CarsViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() {
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `state shows cars after loading`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.uiState.collect {} }
        fakeRepository.insert(testCar())
        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(1, state.cars.size)
        collectJob.cancel()
    }

    @Test
    fun `deleteCar removes car from state`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.uiState.collect {} }
        val id = fakeRepository.insert(testCar())
        assertEquals(1, viewModel.uiState.value.cars.size)

        val car = fakeRepository.getById(id)!!
        viewModel.deleteCar(car)
        assertEquals(0, viewModel.uiState.value.cars.size)
        collectJob.cancel()
    }

    @Test
    fun `setFavorite updates favorite car`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.uiState.collect {} }
        val id1 = fakeRepository.insert(testCar())
        val id2 = fakeRepository.insert(testCar(make = "BMW", model = "iX"))
        fakeRepository.setFavorite(id1)

        viewModel.setFavorite(id2)

        val cars = viewModel.uiState.value.cars
        val car1 = cars.find { it.id == id1 }
        val car2 = cars.find { it.id == id2 }
        assertTrue(!car1!!.isFavorite)
        assertTrue(car2!!.isFavorite)
        collectJob.cancel()
    }

    private fun testCar(
        make: String = "Tesla",
        model: String = "Model 3"
    ) = Car(
        year = 2024,
        make = make,
        model = model,
        batteryCapacityKwh = 75.0,
        createdAt = Instant.now()
    )
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
        val newCar = car.copy(id = nextId++)
        cars.add(newCar)
        flow.value = cars.toList()
        return newCar.id
    }

    override suspend fun update(car: Car) {
        val index = cars.indexOfFirst { it.id == car.id }
        if (index >= 0) {
            cars[index] = car
            flow.value = cars.toList()
        }
    }

    override suspend fun delete(car: Car) {
        cars.removeAll { it.id == car.id }
        flow.value = cars.toList()
    }

    override suspend fun setFavorite(carId: Long) {
        cars.forEachIndexed { index, car ->
            cars[index] = car.copy(isFavorite = car.id == carId)
        }
        flow.value = cars.toList()
    }
}
