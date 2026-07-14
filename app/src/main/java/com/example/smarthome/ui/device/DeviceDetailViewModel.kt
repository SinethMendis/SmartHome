package com.example.smarthome.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.smarthome.data.FirestoreRepository
import com.example.smarthome.domain.Device
import com.example.smarthome.domain.DeviceState
import com.example.smarthome.domain.SubSwitch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class DeviceDetailViewModel(
    private val repository: FirestoreRepository,
    private val floorId: String,
    private val deviceId: String
) : ViewModel() {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val uiState: StateFlow<DeviceDetailUiState> = repository.getDevice(floorId, deviceId)
        .map { device ->
            if (device != null) DeviceDetailUiState.Success(device)
            else DeviceDetailUiState.Error("Device not found")
        }
        .catch { e -> emit(DeviceDetailUiState.Error(e.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DeviceDetailUiState.Loading
        )

    fun toggleDeviceState(currentState: DeviceState) {
        val newState = if (currentState == DeviceState.ON) "OFF" else "ON"
        viewModelScope.launch {
            try {
                repository.updateDeviceState(floorId, deviceId, newState)
            } catch (e: Exception) {
                // Error handled via realtime listener
            }
        }
    }

    fun updateSubSwitch(switches: List<SubSwitch>, subId: String, newState: DeviceState) {
        val updatedList = switches.map {
            if (it.id == subId) it.copy(state = newState) else it
        }.map {
            mapOf("id" to it.id, "name" to it.name, "state" to it.state.name)
        }
        viewModelScope.launch {
            try {
                repository.updateMultiSwitch(floorId, deviceId, updatedList)
            } catch (e: Exception) {}
        }
    }

    fun updateIronDuration(minutes: Int) {
        viewModelScope.launch {
            try {
                repository.updateIronSettings(floorId, deviceId, minutes)
            } catch (e: Exception) {}
        }
    }

    fun updateBulbSchedule(enabled: Boolean, start: LocalTime, end: LocalTime) {
        viewModelScope.launch {
            try {
                repository.updateBulbSchedule(
                    floorId, deviceId, enabled,
                    start.format(timeFormatter),
                    end.format(timeFormatter)
                )
            } catch (e: Exception) {}
        }
    }

    companion object {
        fun provideFactory(
            repository: FirestoreRepository,
            floorId: String,
            deviceId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DeviceDetailViewModel(repository, floorId, deviceId)
            }
        }
    }
}
