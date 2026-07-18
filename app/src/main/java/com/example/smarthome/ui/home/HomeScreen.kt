package com.example.smarthome.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smarthome.R
import com.example.smarthome.domain.Floor
import com.example.smarthome.ui.components.ErrorScreen
import com.example.smarthome.ui.components.LoadingScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onFloorClick: (String) -> Unit,
    onAlertsClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onAlertsClick) {
                        BadgedBox(
                            badge = {
                                val successState = uiState as? HomeUiState.Success
                                if (successState != null && successState.activeAlertCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(8.dp)
                                    )
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Alerts & Usage")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_floor_plan))
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = uiState) {
                is HomeUiState.Loading -> LoadingScreen()
                is HomeUiState.Error -> ErrorScreen(
                    message = state.message,
                    onRetry = { /* StateFlow automatically retries if connection recovers */ }
                )
                is HomeUiState.Success -> {
                    if (state.floors.isEmpty()) {
                        Text(text = "No floors added yet.", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.floors) { floor ->
                                FloorCard(floor = floor, onClick = { onFloorClick(floor.id) })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFloorDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, width, height, url ->
                viewModel.addFloor(name, width, height, url)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddFloorDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("10") }
    var height by remember { mutableStateOf("10") }
    var imageUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_floor_plan)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                TextField(value = width, onValueChange = { width = it }, label = { Text("Grid Width") })
                TextField(value = height, onValueChange = { height = it }, label = { Text("Grid Height") })
                TextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Image URL") })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    onConfirm(name, width.toIntOrNull() ?: 10, height.toIntOrNull() ?: 10, imageUrl)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FloorCard(floor: Floor, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = floor.name, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Devices: ${floor.deviceCount}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Active: ${floor.activeDeviceCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (floor.activeDeviceCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (floor.activeAlertCount > 0) {
                    Text(
                        text = "Alerts: ${floor.activeAlertCount}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
