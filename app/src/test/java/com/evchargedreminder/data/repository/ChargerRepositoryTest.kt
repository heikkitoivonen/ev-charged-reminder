package com.evchargedreminder.data.repository

import com.evchargedreminder.data.local.dao.ChargerDao
import com.evchargedreminder.data.local.entity.ChargerEntity
import com.evchargedreminder.domain.model.ChargerType
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

class ChargerRepositoryTest {

    private lateinit var fakeDao: FakeChargerDao
    private lateinit var repository: ChargerRepositoryImpl

    @Before
    fun setup() {
        fakeDao = FakeChargerDao()
        repository = ChargerRepositoryImpl(fakeDao)
    }

    @Test
    fun `insert returns id and charger is retrievable`() = runTest {
        val id = repository.insert(testCharger())
        val charger = repository.getById(id)
        assertNotNull(charger)
        assertEquals("Home Charger", charger!!.name)
        assertEquals(7.7, charger.maxChargingSpeedKw, 0.01)
    }

    @Test
    fun `getAll emits updated list after insert`() = runTest {
        assertTrue(repository.getAll().first().isEmpty())
        repository.insert(testCharger())
        assertEquals(1, repository.getAll().first().size)
    }

    @Test
    fun `delete removes charger`() = runTest {
        val id = repository.insert(testCharger())
        val charger = repository.getById(id)!!
        repository.delete(charger)
        assertNull(repository.getById(id))
    }

    @Test
    fun `update modifies charger fields`() = runTest {
        val id = repository.insert(testCharger())
        val charger = repository.getById(id)!!
        repository.update(charger.copy(name = "Work Charger"))
        assertEquals("Work Charger", repository.getById(id)!!.name)
    }

    @Test
    fun `getById returns null for non-existent id`() = runTest {
        assertNull(repository.getById(999))
    }

    private fun testCharger(
        name: String = "Home Charger",
        type: ChargerType = ChargerType.LEVEL2_EVSE_32A
    ) = com.evchargedreminder.domain.model.Charger(
        name = name,
        latitude = 37.7749,
        longitude = -122.4194,
        maxChargingSpeedKw = type.powerKw,
        chargerType = type,
        createdAt = Instant.now()
    )
}

private class FakeChargerDao : ChargerDao {
    private var nextId = 1L
    private val chargers = mutableListOf<ChargerEntity>()
    private val flow = MutableStateFlow<List<ChargerEntity>>(emptyList())

    override fun getAll(): Flow<List<ChargerEntity>> =
        flow.map { it.sortedByDescending { c -> c.createdAt } }

    override suspend fun getById(id: Long): ChargerEntity? = chargers.find { it.id == id }

    override suspend fun insert(charger: ChargerEntity): Long {
        val newCharger = charger.copy(id = nextId++)
        chargers.add(newCharger)
        flow.value = chargers.toList()
        return newCharger.id
    }

    override suspend fun update(charger: ChargerEntity) {
        val index = chargers.indexOfFirst { it.id == charger.id }
        if (index >= 0) {
            chargers[index] = charger
            flow.value = chargers.toList()
        }
    }

    override suspend fun delete(charger: ChargerEntity) {
        chargers.removeAll { it.id == charger.id }
        flow.value = chargers.toList()
    }
}
