package com.example.smarthome.ui.device

import com.example.smarthome.domain.Device

sealed interface DeviceDetailUiState {
    object Loading : DeviceDetailUiState
    data class Success(val device: Device) : DeviceDetailUiState
    data class Error(val message: String) : DeviceDetailUiState
}
