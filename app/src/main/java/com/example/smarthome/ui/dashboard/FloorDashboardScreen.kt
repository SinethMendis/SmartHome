package com.example.smarthome.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.smarthome.R
import com.example.smarthome.SmartHomeApplication
import com.example.smarthome.domain.Device
import com.example.smarthome.domain.DeviceState
import com.example.smarthome.domain.Floor
import com.example.smarthome.ui.components.ErrorScreen
import com.example.smarthome.ui.components.LoadingScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorDashboardScreen(
    floorId: String,
    onDeviceClick: (Device) -> Unit,
    onBackClick: () -> Unit,
    viewModel: FloorDashboardViewModel = viewModel(
        key = floorId,
        factory = FloorDashboardViewModel.provideFactory(
            (LocalContext.current.applicationContext as SmartHomeApplication).repository,
            floorId
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (val state = uiState) {
                            is FloorDashboardUiState.Success -> state.floor.name
                            else -> stringResource(R.string.floor_dashboard_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = uiState) {
                is FloorDashboardUiState.Loading -> LoadingScreen()
                is FloorDashboardUiState.Error -> ErrorScreen(
                    message = state.message,
                    onRetry = { /* Auto-retries via Flow */ }
                )
                is FloorDashboardUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        FloorPlanGrid(
                            floor = state.floor,
                            devices = state.devices,
                            onDeviceClick = onDeviceClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                        Divider()
                        DeviceList(
                            devices = state.devices,
                            onDeviceClick = onDeviceClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FloorPlanGrid(
    floor: Floor,
    devices: List<Device>,
    onDeviceClick: (Device) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        AsyncImage(
            model = floor.imageUrl,
            contentDescription = "Floor Plan",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        devices.forEach { device ->
            DevicePin(
                device = device,
                onClick = { onDeviceClick(device) },
                modifier = Modifier.offset(
                    x = maxWidth * device.position.x - 20.dp,
                    y = maxHeight * device.position.y - 20.dp
                )
            )
        }
    }
}

@Composable
fun DevicePin(
    device: Device,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = getDeviceColor(device)
    val icon = getDeviceIcon(device)

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(40.dp)
            .background(color, CircleShape)
            .clip(CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = device.name,
            tint = if (color == MaterialTheme.colorScheme.surfaceVariant) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun DeviceList(
    devices: List<Device>,
    onDeviceClick: (Device) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(devices) { device ->
            DeviceRow(device = device, onClick = { onDeviceClick(device) })
        }
    }
}

@Composable
fun DeviceRow(device: Device, onClick: () -> Unit) {
    val color = getDeviceColor(device)
    val icon = getDeviceIcon(device)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = device.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = device.state.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun getDeviceColor(device: Device): Color {
    return when (device.state) {
        DeviceState.ERROR -> MaterialTheme.colorScheme.error
        DeviceState.DISCONNECTED -> MaterialTheme.colorScheme.outline
        DeviceState.OFF -> MaterialTheme.colorScheme.surfaceVariant
        DeviceState.ON -> {
            val isScheduledOrTimed = when (device) {
                is Device.Bulb -> device.scheduleEnabled
                is Device.Iron -> device.turnedOnAt != null
                else -> false
            }
            if (isScheduledOrTimed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
        }
    }
}

@Composable
fun getDeviceIcon(device: Device): ImageVector {
    return when (device) {
        is Device.Outlet -> Icons.Default.Power
        is Device.MultiSwitch -> Icons.Default.SettingsInputComponent
        is Device.Iron -> Icons.Default.Iron
        is Device.Bulb -> Icons.Default.Lightbulb
        is Device.Camera -> Icons.Default.Videocam
    }
}
