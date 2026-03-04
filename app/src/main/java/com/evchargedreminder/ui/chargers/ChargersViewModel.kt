package com.evchargedreminder.ui.chargers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evchargedreminder.data.repository.ChargerRepository
import com.evchargedreminder.domain.model.Charger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChargerListUiState(
    val chargers: List<Charger> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ChargersViewModel @Inject constructor(
    private val chargerRepository: ChargerRepository
) : ViewModel() {

    val uiState: StateFlow<ChargerListUiState> = chargerRepository.getAll()
        .map { chargers -> ChargerListUiState(chargers = chargers, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChargerListUiState()
        )

    fun deleteCharger(charger: Charger) {
        viewModelScope.launch {
            chargerRepository.delete(charger)
        }
    }
}
