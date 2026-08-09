package com.example.smarthome.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.smarthome.data.FirestoreRepository
import com.example.smarthome.data.model.DeviceDto
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    fun addDevice(
        name: String,
        type: String,
        x: Double,
        y: Double,
        cameraUri: String? = null,
        switches: List<Map<String, Any>>? = null
    ) {
        viewModelScope.launch {
            try {
                val deviceDto = when (type) {
                    "multiswitch" -> DeviceDto(
                        name = name,
                        type = type,
                        positionX = x,
                        positionY = y,
                        switches = switches
                    )
                    "camera" -> DeviceDto(
                        name = name,
                        type = type,
                        positionX = x,
                        positionY = y,
                        cameraUri = "https://smart-home-monitor-c8015.web.app/assets/mock-camera-2.jpg?t=1786212321467"
                    )
                    "iron" -> DeviceDto(
                        name = name,
                        type = type,
                        positionX = x,
                        positionY = y,
                        maxOnDurationMin = 30
                    )
                    else -> DeviceDto(
                        name = name,
                        type = type,
                        positionX = x,
                        positionY = y
                    )
                }
                repository.addDevice(floorId, deviceDto)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

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
