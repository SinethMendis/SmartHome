package com.example.smarthome.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.smarthome.data.FirestoreRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class FloorDashboardViewModel(
    private val repository: FirestoreRepository,
    private val floorId: String
) : ViewModel() {

    val uiState: StateFlow<FloorDashboardUiState> = combine(
        repository.getFloor(floorId),
        repository.getDevices(floorId)
    ) { floor, devices ->
        if (floor != null) {
            FloorDashboardUiState.Success(floor, devices)
        } else {
            FloorDashboardUiState.Error("Floor not found")
        }
    }.catch { e ->
        emit(FloorDashboardUiState.Error(e.message ?: "Unknown error"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FloorDashboardUiState.Loading
    )

    companion object {
        fun provideFactory(
            repository: FirestoreRepository,
            floorId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                FloorDashboardViewModel(repository, floorId)
            }
        }
    }
}
