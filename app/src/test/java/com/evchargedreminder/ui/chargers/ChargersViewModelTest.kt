package com.evchargedreminder.ui.chargers

import com.evchargedreminder.data.repository.ChargerRepository
import com.evchargedreminder.domain.model.Charger
import com.evchargedreminder.domain.model.ChargerType
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
class ChargersViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeChargerRepository
    private lateinit var viewModel: ChargersViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeChargerRepository()
        viewModel = ChargersViewModel(fakeRepository)
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
    fun `state shows chargers after loading`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.uiState.collect {} }
        fakeRepository.insert(testCharger())
        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(1, state.chargers.size)
        collectJob.cancel()
    }

    @Test
    fun `deleteCharger removes charger from state`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.uiState.collect {} }
        val id = fakeRepository.insert(testCharger())
        assertEquals(1, viewModel.uiState.value.chargers.size)

        val charger = fakeRepository.getById(id)!!
        viewModel.deleteCharger(charger)
        assertEquals(0, viewModel.uiState.value.chargers.size)
        collectJob.cancel()
    }

    private fun testCharger(
        name: String = "Home Charger"
    ) = Charger(
        name = name,
        latitude = 37.7749,
        longitude = -122.4194,
        maxChargingSpeedKw = 7.7,
        chargerType = ChargerType.LEVEL2_EVSE_32A,
        createdAt = Instant.now()
    )
}

private class FakeChargerRepository : ChargerRepository {
    private var nextId = 1L
    private val chargers = mutableListOf<Charger>()
    private val flow = MutableStateFlow<List<Charger>>(emptyList())

    override fun getAll(): Flow<List<Charger>> = flow.map { it.toList() }

    override suspend fun getById(id: Long): Charger? = chargers.find { it.id == id }

    override suspend fun insert(charger: Charger): Long {
        val newCharger = charger.copy(id = nextId++)
        chargers.add(newCharger)
        flow.value = chargers.toList()
        return newCharger.id
    }

    override suspend fun update(charger: Charger) {
        val index = chargers.indexOfFirst { it.id == charger.id }
        if (index >= 0) {
            chargers[index] = charger
            flow.value = chargers.toList()
        }
    }

    override suspend fun delete(charger: Charger) {
        chargers.removeAll { it.id == charger.id }
        flow.value = chargers.toList()
    }
}
