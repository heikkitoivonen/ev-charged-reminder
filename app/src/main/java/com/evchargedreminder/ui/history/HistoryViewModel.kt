package com.evchargedreminder.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evchargedreminder.data.repository.CarRepository
import com.evchargedreminder.data.repository.ChargerRepository
import com.evchargedreminder.data.repository.ChargingSessionRepository
import com.evchargedreminder.domain.model.Car
import com.evchargedreminder.domain.model.Charger
import com.evchargedreminder.domain.model.ChargingSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class HistoryItem(
    val session: ChargingSession,
    val carName: String,
    val chargerName: String
)

data class HistoryUiState(
    val sessions: List<HistoryItem> = emptyList(),
    val isLoading: Boolean = true,
    val filterCarId: Long? = null,
    val filterChargerId: Long? = null,
    val cars: List<Car> = emptyList(),
    val chargers: List<Charger> = emptyList()
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionRepository: ChargingSessionRepository,
    private val carRepository: CarRepository,
    private val chargerRepository: ChargerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _filterCarId = MutableStateFlow<Long?>(null)
    private val _filterChargerId = MutableStateFlow<Long?>(null)

    init {
        cleanupOldSessions()
        observeSessions()
    }

    private fun observeSessions() {
        viewModelScope.launch {
            combine(
                sessionRepository.getAll(),
                carRepository.getAll(),
                chargerRepository.getAll(),
                _filterCarId,
                _filterChargerId
            ) { sessions, cars, chargers, carFilter, chargerFilter ->
                val carMap = cars.associateBy { it.id }
                val chargerMap = chargers.associateBy { it.id }

                val filtered = sessions.filter { session ->
                    (carFilter == null || session.carId == carFilter) &&
                        (chargerFilter == null || session.chargerId == chargerFilter)
                }

                val items = filtered.map { session ->
                    val car = carMap[session.carId]
                    val charger = chargerMap[session.chargerId]
                    HistoryItem(
                        session = session,
                        carName = car?.let { "${it.year} ${it.make} ${it.model}" } ?: "Unknown car",
                        chargerName = charger?.name ?: "Unknown charger"
                    )
                }

                HistoryUiState(
                    sessions = items,
                    isLoading = false,
                    filterCarId = carFilter,
                    filterChargerId = chargerFilter,
                    cars = cars,
                    chargers = chargers
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setCarFilter(carId: Long?) {
        _filterCarId.value = carId
    }

    fun setChargerFilter(chargerId: Long?) {
        _filterChargerId.value = chargerId
    }

    private fun cleanupOldSessions() {
        viewModelScope.launch {
            val oneYearAgo = Instant.now().minusSeconds(365 * 86400L)
            sessionRepository.deleteOlderThan(oneYearAgo.toEpochMilli())
        }
    }
}
