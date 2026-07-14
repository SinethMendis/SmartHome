package com.example.smarthome.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.smarthome.SmartHomeApplication
import com.example.smarthome.data.FirestoreRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: FirestoreRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = repository.getFloors()
        .map { floors -> HomeUiState.Success(floors) as HomeUiState }
        .catch { e -> emit(HomeUiState.Error(e.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState.Loading
        )

    fun addFloor(name: String, width: Int, height: Int, imageUrl: String) {
        if (name.isBlank()) return
        
        viewModelScope.launch {
            try {
                repository.addFloor(name, width, height, imageUrl)
            } catch (e: Exception) {
                // Real app would update an error state
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SmartHomeApplication)
                HomeViewModel(application.repository)
            }
        }
    }
}
