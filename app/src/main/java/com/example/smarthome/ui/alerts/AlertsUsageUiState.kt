package com.example.smarthome.ui.alerts

import com.example.smarthome.domain.Alert

sealed interface AlertsUsageUiState {
    object Loading : AlertsUsageUiState
    data class Success(
        val alerts: List<Alert>,
        val usageData: List<DeviceUsage>
    ) : AlertsUsageUiState
    data class Error(val message: String) : AlertsUsageUiState
}

data class DeviceUsage(
    val deviceName: String,
    val totalMinutes: Long
)
