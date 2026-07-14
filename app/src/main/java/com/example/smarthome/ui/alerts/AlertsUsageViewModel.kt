package com.example.smarthome.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.smarthome.SmartHomeApplication
import com.example.smarthome.data.FirestoreRepository
import com.example.smarthome.domain.UsageLog
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

class AlertsUsageViewModel(private val repository: FirestoreRepository) : ViewModel() {

    val uiState: StateFlow<AlertsUsageUiState> = combine(
        repository.getAlerts(),
        repository.getUsageLogsToday()
    ) { alerts, logs ->
        AlertsUsageUiState.Success(
            alerts = alerts,
            usageData = calculateUsage(logs)
        ) as AlertsUsageUiState
    }.catch { e ->
        emit(AlertsUsageUiState.Error(e.message ?: "Unknown error"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlertsUsageUiState.Loading
    )

    fun acknowledgeAlert(alertId: String) {
        viewModelScope.launch {
            try {
                repository.acknowledgeAlert(alertId)
            } catch (e: Exception) {}
        }
    }

    private fun calculateUsage(logs: List<UsageLog>): List<DeviceUsage> {
        val deviceLogs = logs.groupBy { it.deviceId }
        return deviceLogs.map { (deviceId, logs) ->
            var totalMillis = 0L
            var lastOnTime: Instant? = null
            
            logs.forEach { log ->
                if (log.event == "ON") {
                    lastOnTime = log.timestamp
                } else if (lastOnTime != null) {
                    totalMillis += Duration.between(lastOnTime, log.timestamp).toMillis()
                    lastOnTime = null
                }
            }
            
            // If still ON, count until now
            lastOnTime?.let {
                totalMillis += Duration.between(it, Instant.now()).toMillis()
            }

            DeviceUsage(
                deviceName = logs.firstOrNull()?.deviceName ?: "Unknown",
                totalMinutes = totalMillis / 60000
            )
        }.sortedByDescending { it.totalMinutes }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SmartHomeApplication)
                AlertsUsageViewModel(application.repository)
            }
        }
    }
}
