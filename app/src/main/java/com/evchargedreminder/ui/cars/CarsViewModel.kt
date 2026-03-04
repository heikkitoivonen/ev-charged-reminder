package com.evchargedreminder.ui.cars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evchargedreminder.data.repository.CarRepository
import com.evchargedreminder.domain.model.Car
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CarListUiState(
    val cars: List<Car> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CarsViewModel @Inject constructor(
    private val carRepository: CarRepository
) : ViewModel() {

    val uiState: StateFlow<CarListUiState> = carRepository.getAll()
        .map { cars -> CarListUiState(cars = cars, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CarListUiState()
        )

    fun deleteCar(car: Car) {
        viewModelScope.launch {
            carRepository.delete(car)
        }
    }

    fun setFavorite(carId: Long) {
        viewModelScope.launch {
            carRepository.setFavorite(carId)
        }
    }
}
