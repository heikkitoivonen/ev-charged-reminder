package com.evchargedreminder.ui.cars

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.evchargedreminder.data.bundled.BundledEvData
import com.evchargedreminder.data.repository.CarRepository
import com.evchargedreminder.domain.model.Car
import com.evchargedreminder.ui.navigation.CarEditRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CarEditUiState(
    val isEditMode: Boolean = false,
    val existingCarId: Long? = null,
    val year: Int = 2024,
    val make: String = "",
    val model: String = "",
    val trim: String = "",
    val isHybrid: Boolean = false,
    val batteryCapacityKwh: String = "",
    val maxAcceptRateKw: String = "",
    val defaultStartPct: Int = 20,
    val defaultTargetPct: Int = 80,
    val availableMakes: List<String> = BundledEvData.getAllMakes(),
    val availableModels: List<String> = emptyList(),
    val availableTrims: List<String> = emptyList(),
    val autoFilled: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CarEditViewModel @Inject constructor(
    private val carRepository: CarRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route = savedStateHandle.toRoute<CarEditRoute>()
    private val editCarId: Long? = if (route.carId == -1L) null else route.carId

    private val _uiState = MutableStateFlow(CarEditUiState())
    val uiState: StateFlow<CarEditUiState> = _uiState.asStateFlow()

    init {
        if (editCarId != null) {
            loadExistingCar(editCarId)
        }
    }

    private fun loadExistingCar(carId: Long) {
        viewModelScope.launch {
            val car = carRepository.getById(carId) ?: return@launch
            _uiState.update {
                it.copy(
                    isEditMode = true,
                    existingCarId = car.id,
                    year = car.year,
                    make = car.make,
                    model = car.model,
                    trim = car.trim ?: "",
                    isHybrid = car.isHybrid,
                    batteryCapacityKwh = car.batteryCapacityKwh.toString(),
                    maxAcceptRateKw = car.maxAcceptRateKw?.toString() ?: "",
                    defaultStartPct = car.defaultStartPct,
                    defaultTargetPct = car.defaultTargetPct,
                    availableModels = BundledEvData.getModelsForMake(car.make),
                    availableTrims = BundledEvData.getTrimsForMakeAndModel(car.make, car.model)
                )
            }
        }
    }

    fun updateYear(year: Int) {
        _uiState.update { it.copy(year = year) }
        tryAutoFill()
    }

    fun updateMake(make: String) {
        val models = BundledEvData.getModelsForMake(make)
        _uiState.update {
            it.copy(
                make = make,
                model = "",
                trim = "",
                availableModels = models,
                availableTrims = emptyList(),
                autoFilled = false
            )
        }
    }

    fun updateModel(model: String) {
        val make = _uiState.value.make
        val trims = BundledEvData.getTrimsForMakeAndModel(make, model)
        _uiState.update {
            it.copy(
                model = model,
                trim = "",
                availableTrims = trims,
                autoFilled = false
            )
        }
        tryAutoFill()
    }

    fun updateTrim(trim: String) {
        _uiState.update { it.copy(trim = trim, autoFilled = false) }
        tryAutoFill()
    }

    fun updateIsHybrid(isHybrid: Boolean) {
        _uiState.update {
            it.copy(
                isHybrid = isHybrid,
                defaultStartPct = if (isHybrid) 0 else 20,
                defaultTargetPct = if (isHybrid) 100 else 80
            )
        }
    }

    fun updateBatteryCapacity(value: String) {
        _uiState.update { it.copy(batteryCapacityKwh = value, autoFilled = false) }
    }

    fun updateMaxAcceptRate(value: String) {
        _uiState.update { it.copy(maxAcceptRateKw = value) }
    }

    fun updateDefaultStartPct(value: Int) {
        _uiState.update { it.copy(defaultStartPct = value) }
    }

    fun updateDefaultTargetPct(value: Int) {
        _uiState.update { it.copy(defaultTargetPct = value) }
    }

    private fun tryAutoFill() {
        val state = _uiState.value
        if (state.make.isBlank() || state.model.isBlank()) return

        val matches = BundledEvData.findByMakeAndModel(state.make, state.model)
        val match = if (state.trim.isNotBlank()) {
            matches.find { it.trim.equals(state.trim, ignoreCase = true) }
        } else {
            matches.singleOrNull()
        } ?: return

        _uiState.update {
            it.copy(
                batteryCapacityKwh = match.batteryCapacityKwh.toString(),
                isHybrid = match.isHybrid,
                defaultStartPct = if (match.isHybrid) 0 else 20,
                defaultTargetPct = if (match.isHybrid) 100 else 80,
                autoFilled = true
            )
        }
    }

    fun saveCar() {
        val state = _uiState.value
        val capacity = state.batteryCapacityKwh.toDoubleOrNull()
        if (state.make.isBlank() || state.model.isBlank() || capacity == null || capacity <= 0) {
            _uiState.update { it.copy(error = "Please fill in make, model, and a valid battery capacity.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            val maxRate = state.maxAcceptRateKw.toDoubleOrNull()
            val car = Car(
                id = state.existingCarId ?: 0,
                year = state.year,
                make = state.make.trim(),
                model = state.model.trim(),
                trim = state.trim.trim().ifBlank { null },
                isHybrid = state.isHybrid,
                batteryCapacityKwh = capacity,
                maxAcceptRateKw = maxRate,
                defaultStartPct = state.defaultStartPct,
                defaultTargetPct = state.defaultTargetPct
            )

            if (state.isEditMode) {
                val existingCar = carRepository.getById(car.id)
                carRepository.update(car.copy(
                    isFavorite = existingCar?.isFavorite ?: false,
                    createdAt = existingCar?.createdAt ?: car.createdAt
                ))
            } else {
                val isFirst = carRepository.count() == 0
                val newId = carRepository.insert(car)
                if (isFirst) {
                    carRepository.setFavorite(newId)
                }
            }
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
