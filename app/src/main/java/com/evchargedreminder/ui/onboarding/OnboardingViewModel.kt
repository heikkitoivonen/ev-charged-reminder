package com.evchargedreminder.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evchargedreminder.data.OnboardingPreferences
import com.evchargedreminder.data.bundled.BundledEvData
import com.evchargedreminder.data.repository.CarRepository
import com.evchargedreminder.domain.model.Car
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep {
    WELCOME, ADD_CAR, ADD_CHARGER, PERMISSIONS, DONE
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val isCustom: Boolean = false,
    val year: Int = 2024,
    val make: String = "",
    val model: String = "",
    val trim: String = "",
    val isHybrid: Boolean = false,
    val batteryCapacityKwh: String = "",
    val autoFilled: Boolean = false,
    val availableMakes: List<String> = BundledEvData.getAllMakes(),
    val availableModels: List<String> = emptyList(),
    val availableTrims: List<String> = emptyList(),
    val locationGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val isSaving: Boolean = false,
    val carSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun nextStep() {
        _uiState.update {
            val next = when (it.step) {
                OnboardingStep.WELCOME -> OnboardingStep.ADD_CAR
                OnboardingStep.ADD_CAR -> OnboardingStep.ADD_CHARGER
                OnboardingStep.ADD_CHARGER -> OnboardingStep.PERMISSIONS
                OnboardingStep.PERMISSIONS -> OnboardingStep.DONE
                OnboardingStep.DONE -> OnboardingStep.DONE
            }
            it.copy(step = next)
        }
    }

    fun previousStep() {
        _uiState.update {
            val prev = when (it.step) {
                OnboardingStep.WELCOME -> OnboardingStep.WELCOME
                OnboardingStep.ADD_CAR -> OnboardingStep.WELCOME
                OnboardingStep.ADD_CHARGER -> OnboardingStep.ADD_CAR
                OnboardingStep.PERMISSIONS -> OnboardingStep.ADD_CHARGER
                OnboardingStep.DONE -> OnboardingStep.PERMISSIONS
            }
            it.copy(step = prev)
        }
    }

    fun setCustomMode(custom: Boolean) {
        _uiState.update {
            it.copy(
                isCustom = custom,
                make = "",
                model = "",
                trim = "",
                batteryCapacityKwh = "",
                autoFilled = false,
                availableModels = emptyList(),
                availableTrims = emptyList()
            )
        }
    }

    fun updateYear(year: Int) {
        _uiState.update { it.copy(year = year) }
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
        val state = _uiState.value
        val trims = BundledEvData.getTrimsForMakeAndModel(state.make, model)
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
        _uiState.update { it.copy(isHybrid = isHybrid) }
    }

    fun updateBatteryCapacity(value: String) {
        _uiState.update { it.copy(batteryCapacityKwh = value, autoFilled = false) }
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
                autoFilled = true
            )
        }
    }

    fun saveCar() {
        val state = _uiState.value
        val nameValid = if (state.isCustom) state.make.isNotBlank() else state.make.isNotBlank() && state.model.isNotBlank()
        if (!nameValid) {
            val msg = if (state.isCustom) "Car name is required" else "Make and model are required"
            _uiState.update { it.copy(error = msg) }
            return
        }
        val capacity = state.batteryCapacityKwh.toDoubleOrNull()
        if (capacity == null || capacity <= 0) {
            _uiState.update { it.copy(error = "Valid battery capacity is required") }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val car = Car(
                year = state.year,
                make = state.make,
                model = if (state.isCustom) "" else state.model,
                trim = if (state.isCustom) null else state.trim.ifBlank { null },
                isHybrid = state.isHybrid,
                batteryCapacityKwh = capacity,
                defaultStartPct = if (state.isHybrid) 0 else 20,
                defaultTargetPct = if (state.isHybrid) 100 else 80,
                isFavorite = true
            )
            val id = carRepository.insert(car)
            carRepository.setFavorite(id)
            _uiState.update { it.copy(isSaving = false, carSaved = true, step = OnboardingStep.ADD_CHARGER) }
        }
    }

    fun skipCharger() {
        _uiState.update { it.copy(step = OnboardingStep.PERMISSIONS) }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(locationGranted = granted) }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(notificationGranted = granted) }
    }

    fun completeOnboarding() {
        onboardingPreferences.isCompleted = true
        _uiState.update { it.copy(step = OnboardingStep.DONE) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
