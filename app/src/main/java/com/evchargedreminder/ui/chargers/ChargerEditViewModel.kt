package com.evchargedreminder.ui.chargers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.evchargedreminder.data.remote.NominatimApi
import com.evchargedreminder.data.remote.OpenChargeMapApi
import com.evchargedreminder.data.repository.ChargerRepository
import com.evchargedreminder.domain.model.Charger
import com.evchargedreminder.domain.model.ChargerType
import com.evchargedreminder.ui.navigation.ChargerEditRoute
import com.evchargedreminder.util.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChargerEditUiState(
    val isEditMode: Boolean = false,
    val existingChargerId: Long? = null,
    val name: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val hasLocation: Boolean = false,
    val chargerType: ChargerType = ChargerType.STANDARD_HOUSEHOLD_OUTLET,
    val customPowerKw: String = "",
    val maxChargingSpeedKw: String = ChargerType.STANDARD_HOUSEHOLD_OUTLET.powerKw.toString(),
    val radiusMeters: Int = 100,
    val notifyMinutesBefore: Int = 15,
    val suggestedSpeedKw: Double? = null,
    val isLoadingLocation: Boolean = false,
    val isLoadingAddress: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChargerEditViewModel @Inject constructor(
    private val chargerRepository: ChargerRepository,
    private val openChargeMapApi: OpenChargeMapApi,
    private val nominatimApi: NominatimApi,
    private val locationProvider: LocationProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ChargerEditRoute>()
    private val editChargerId: Long? = if (route.chargerId == -1L) null else route.chargerId

    private val _uiState = MutableStateFlow(ChargerEditUiState())
    val uiState: StateFlow<ChargerEditUiState> = _uiState.asStateFlow()

    init {
        if (editChargerId != null) {
            loadExistingCharger(editChargerId)
        }
    }

    private fun loadExistingCharger(chargerId: Long) {
        viewModelScope.launch {
            val charger = chargerRepository.getById(chargerId) ?: return@launch
            _uiState.update {
                it.copy(
                    isEditMode = true,
                    existingChargerId = charger.id,
                    name = charger.name,
                    latitude = charger.latitude.toString(),
                    longitude = charger.longitude.toString(),
                    hasLocation = true,
                    chargerType = charger.chargerType,
                    maxChargingSpeedKw = charger.maxChargingSpeedKw.toString(),
                    customPowerKw = if (charger.chargerType == ChargerType.CUSTOM_AC ||
                        charger.chargerType == ChargerType.CUSTOM_DC
                    ) charger.maxChargingSpeedKw.toString() else "",
                    radiusMeters = charger.radiusMeters,
                    notifyMinutesBefore = charger.notifyMinutesBefore
                )
            }
        }
    }

    fun useCurrentLocation() {
        _uiState.update { it.copy(isLoadingLocation = true, error = null) }
        viewModelScope.launch {
            val location = locationProvider.getCurrentLocation()
            if (location == null) {
                _uiState.update {
                    it.copy(
                        isLoadingLocation = false,
                        error = "Could not get current location. Check location permissions."
                    )
                }
                return@launch
            }
            val (lat, lng) = location
            _uiState.update {
                it.copy(
                    latitude = lat.toString(),
                    longitude = lng.toString(),
                    hasLocation = true,
                    isLoadingLocation = false
                )
            }
            fetchAddressAndSuggestions(lat, lng)
        }
    }

    fun setLocationFromMap(lat: Double, lng: Double) {
        _uiState.update {
            it.copy(
                latitude = lat.toString(),
                longitude = lng.toString(),
                hasLocation = true
            )
        }
        viewModelScope.launch {
            fetchAddressAndSuggestions(lat, lng)
        }
    }

    private suspend fun fetchAddressAndSuggestions(lat: Double, lng: Double) {
        // Reverse geocode for name
        _uiState.update { it.copy(isLoadingAddress = true) }
        try {
            val result = nominatimApi.reverseGeocode(lat, lng)
            val displayName = result.displayName
            if (!displayName.isNullOrBlank() && _uiState.value.name.isBlank()) {
                // Use first two parts of the address for a concise name
                val shortName = displayName.split(",")
                    .take(2)
                    .joinToString(",") { it.trim() }
                _uiState.update { it.copy(name = shortName) }
            }
        } catch (_: Exception) {
            // Non-critical — user can enter name manually
        }
        _uiState.update { it.copy(isLoadingAddress = false) }

        // Query OpenChargeMap for speed suggestion
        try {
            val chargePoints = openChargeMapApi.getNearbyChargers(lat, lng)
            val maxPower = chargePoints
                .flatMap { it.connections.orEmpty() }
                .mapNotNull { it.powerKw }
                .maxOrNull()
            if (maxPower != null && maxPower > 0) {
                _uiState.update { it.copy(suggestedSpeedKw = maxPower) }
            }
        } catch (_: Exception) {
            // Non-critical — user can enter speed manually
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateChargerType(type: ChargerType) {
        val isCustom = type == ChargerType.CUSTOM_AC || type == ChargerType.CUSTOM_DC
        _uiState.update {
            it.copy(
                chargerType = type,
                maxChargingSpeedKw = if (isCustom) it.customPowerKw else type.powerKw.toString(),
                customPowerKw = if (isCustom) it.customPowerKw else ""
            )
        }
    }

    fun updateCustomPower(value: String) {
        _uiState.update {
            it.copy(customPowerKw = value, maxChargingSpeedKw = value)
        }
    }

    fun updateMaxChargingSpeed(value: String) {
        _uiState.update { it.copy(maxChargingSpeedKw = value) }
    }

    fun applySuggestedSpeed() {
        val suggested = _uiState.value.suggestedSpeedKw ?: return
        _uiState.update { it.copy(maxChargingSpeedKw = suggested.toString()) }
    }

    fun updateRadius(value: Int) {
        _uiState.update { it.copy(radiusMeters = value) }
    }

    fun updateNotifyMinutesBefore(value: Int) {
        _uiState.update { it.copy(notifyMinutesBefore = value) }
    }

    fun saveCharger() {
        val state = _uiState.value
        val lat = state.latitude.toDoubleOrNull()
        val lng = state.longitude.toDoubleOrNull()
        val power = state.maxChargingSpeedKw.toDoubleOrNull()

        if (state.name.isBlank() || lat == null || lng == null || power == null || power <= 0) {
            _uiState.update {
                it.copy(error = "Please provide a name, location, and valid charging speed.")
            }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            val charger = Charger(
                id = state.existingChargerId ?: 0,
                name = state.name.trim(),
                latitude = lat,
                longitude = lng,
                radiusMeters = state.radiusMeters,
                maxChargingSpeedKw = power,
                chargerType = state.chargerType,
                notifyMinutesBefore = state.notifyMinutesBefore
            )

            if (state.isEditMode) {
                val existing = chargerRepository.getById(charger.id)
                chargerRepository.update(
                    charger.copy(createdAt = existing?.createdAt ?: charger.createdAt)
                )
            } else {
                chargerRepository.insert(charger)
            }
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
