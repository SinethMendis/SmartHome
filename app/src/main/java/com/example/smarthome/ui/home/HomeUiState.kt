package com.example.smarthome.ui.home

import com.example.smarthome.domain.Alert
import com.example.smarthome.domain.Floor

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val floors: List<Floor> = emptyList(),
        val activeAlertCount: Int = 0
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
