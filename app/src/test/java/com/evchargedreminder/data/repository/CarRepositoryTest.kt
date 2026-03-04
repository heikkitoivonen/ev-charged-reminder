package com.evchargedreminder.data.repository

import com.evchargedreminder.data.local.dao.CarDao
import com.evchargedreminder.data.local.entity.CarEntity
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

class CarRepositoryTest {

    private lateinit var fakeDao: FakeCarDao
    private lateinit var repository: CarRepositoryImpl

    @Before
    fun setup() {
        fakeDao = FakeCarDao()
        repository = CarRepositoryImpl(fakeDao)
    }

    @Test
    fun `insert returns id and car is retrievable`() = runTest {
        val id = repository.insert(testCar())
        val car = repository.getById(id)
        assertNotNull(car)
        assertEquals("Tesla", car!!.make)
        assertEquals("Model 3", car.model)
    }

    @Test
    fun `getAll emits updated list after insert`() = runTest {
        assertTrue(repository.getAll().first().isEmpty())
        repository.insert(testCar())
        assertEquals(1, repository.getAll().first().size)
    }

    @Test
    fun `delete removes car`() = runTest {
        val id = repository.insert(testCar())
        val car = repository.getById(id)!!
        repository.delete(car)
        assertNull(repository.getById(id))
    }

    @Test
    fun `count returns correct number`() = runTest {
        assertEquals(0, repository.count())
        repository.insert(testCar())
        assertEquals(1, repository.count())
        repository.insert(testCar(make = "BMW", model = "iX"))
        assertEquals(2, repository.count())
    }

    @Test
    fun `setFavorite clears previous and marks new`() = runTest {
        val id1 = repository.insert(testCar())
        val id2 = repository.insert(testCar(make = "BMW", model = "iX"))

        repository.setFavorite(id1)
        assertTrue(repository.getById(id1)!!.isFavorite)

        repository.setFavorite(id2)
        assertTrue(repository.getById(id2)!!.isFavorite)
        assertTrue(!repository.getById(id1)!!.isFavorite)
    }

    @Test
    fun `getFavorite returns the favorite car`() = runTest {
        val id = repository.insert(testCar())
        assertNull(repository.getFavorite())
        repository.setFavorite(id)
        assertNotNull(repository.getFavorite())
        assertEquals(id, repository.getFavorite()!!.id)
    }

    @Test
    fun `update modifies car fields`() = runTest {
        val id = repository.insert(testCar())
        val car = repository.getById(id)!!
        repository.update(car.copy(year = 2025))
        assertEquals(2025, repository.getById(id)!!.year)
    }

    private fun testCar(
        make: String = "Tesla",
        model: String = "Model 3"
    ) = com.evchargedreminder.domain.model.Car(
        year = 2024,
        make = make,
        model = model,
        batteryCapacityKwh = 75.0,
        createdAt = Instant.now()
    )
}

private class FakeCarDao : CarDao {
    private var nextId = 1L
    private val cars = mutableListOf<CarEntity>()
    private val flow = MutableStateFlow<List<CarEntity>>(emptyList())

    override fun getAll(): Flow<List<CarEntity>> = flow.map { it.sortedByDescending { c -> c.createdAt } }

    override suspend fun getById(id: Long): CarEntity? = cars.find { it.id == id }

    override suspend fun getFavorite(): CarEntity? = cars.find { it.isFavorite }

    override suspend fun count(): Int = cars.size

    override suspend fun insert(car: CarEntity): Long {
        val newCar = car.copy(id = nextId++)
        cars.add(newCar)
        flow.value = cars.toList()
        return newCar.id
    }

    override suspend fun update(car: CarEntity) {
        val index = cars.indexOfFirst { it.id == car.id }
        if (index >= 0) {
            cars[index] = car
            flow.value = cars.toList()
        }
    }

    override suspend fun delete(car: CarEntity) {
        cars.removeAll { it.id == car.id }
        flow.value = cars.toList()
    }

    override suspend fun clearFavorite() {
        cars.forEachIndexed { index, car ->
            if (car.isFavorite) cars[index] = car.copy(isFavorite = false)
        }
        flow.value = cars.toList()
    }
}
