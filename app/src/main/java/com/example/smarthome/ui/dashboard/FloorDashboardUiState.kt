package com.example.smarthome.ui.dashboard

import com.example.smarthome.domain.Device
import com.example.smarthome.domain.Floor

sealed interface FloorDashboardUiState {
    object Loading : FloorDashboardUiState
    data class Success(
        val floor: Floor,
        val devices: List<Device>
    ) : FloorDashboardUiState
    data class Error(val message: String) : FloorDashboardUiState
}
