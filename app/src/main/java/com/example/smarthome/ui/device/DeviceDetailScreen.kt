package com.example.smarthome.ui.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.smarthome.SmartHomeApplication
import com.example.smarthome.domain.Device
import com.example.smarthome.domain.DeviceState
import com.example.smarthome.ui.components.ErrorScreen
import com.example.smarthome.ui.components.LoadingScreen
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    floorId: String,
    deviceId: String,
    onBackClick: () -> Unit,
    viewModel: DeviceDetailViewModel = viewModel(
        key = deviceId,
        factory = DeviceDetailViewModel.provideFactory(
            (LocalContext.current.applicationContext as SmartHomeApplication).repository,
            floorId,
            deviceId
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf<Pair<String, LocalTime>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (val state = uiState) {
                            is DeviceDetailUiState.Success -> state.device.name
                            else -> "Device Detail"
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
                is DeviceDetailUiState.Loading -> LoadingScreen()
                is DeviceDetailUiState.Error -> ErrorScreen(
                    message = state.message,
                    onRetry = { /* Auto-retries */ }
                )
                is DeviceDetailUiState.Success -> {
                    DeviceDetailContent(
                        device = state.device,
                        viewModel = viewModel,
                        onTimeClick = { label, time -> showTimePicker = label to time }
                    )
                }
            }
        }
    }

    showTimePicker?.let { (label, time) ->
        val timeState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = null },
            confirmButton = {
                TextButton(onClick = {
                    val newTime = LocalTime.of(timeState.hour, timeState.minute)
                    val device = (uiState as? DeviceDetailUiState.Success)?.device as? Device.Bulb
                    if (device != null) {
                        if (label == "Start") {
                            viewModel.updateBulbSchedule(device.scheduleEnabled, newTime, device.scheduleEnd)
                        } else {
                            viewModel.updateBulbSchedule(device.scheduleEnabled, device.scheduleStart, newTime)
                        }
                    }
                    showTimePicker = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = null }) { Text("Cancel") }
            },
            title = { Text("Select $label Time") },
            text = { TimePicker(state = timeState) }
        )
    }
}

@Composable
fun DeviceDetailContent(
    device: Device,
    viewModel: DeviceDetailViewModel,
    onTimeClick: (String, LocalTime) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StateBadge(state = device.state)
        Spacer(modifier = Modifier.height(24.dp))

        when (device) {
            is Device.Outlet -> OutletControls(device, viewModel)
            is Device.MultiSwitch -> MultiSwitchControls(device, viewModel)
            is Device.Iron -> IronControls(device, viewModel)
            is Device.Bulb -> BulbControls(device, viewModel, onTimeClick)
            is Device.Camera -> CameraControls(device)
        }
    }
}

@Composable
fun StateBadge(state: DeviceState) {
    val containerColor = when (state) {
        DeviceState.ON -> MaterialTheme.colorScheme.primaryContainer
        DeviceState.ERROR -> MaterialTheme.colorScheme.errorContainer
        DeviceState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
        DeviceState.OFF -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (state) {
        DeviceState.ON -> MaterialTheme.colorScheme.onPrimaryContainer
        DeviceState.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        DeviceState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
        DeviceState.OFF -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = state.name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun OutletControls(device: Device.Outlet, viewModel: DeviceDetailViewModel) {
    Switch(
        checked = device.state == DeviceState.ON,
        onCheckedChange = { viewModel.toggleDeviceState(device.state) },
        modifier = Modifier.scale(2f)
    )
}

@Composable
fun MultiSwitchControls(device: Device.MultiSwitch, viewModel: DeviceDetailViewModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(device.switches) { sub ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = sub.name, modifier = Modifier.weight(1f))
                    Switch(
                        checked = sub.state == DeviceState.ON,
                        onCheckedChange = { 
                            val newState = if (sub.state == DeviceState.ON) DeviceState.OFF else DeviceState.ON
                            viewModel.updateSubSwitch(device.switches, sub.id, newState)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun IronControls(device: Device.Iron, viewModel: DeviceDetailViewModel) {
    var timeLeft by remember(device.turnedOnAt, device.state) {
        mutableStateOf(calculateTimeLeft(device))
    }

    LaunchedEffect(device.turnedOnAt, device.state) {
        if (device.state == DeviceState.ON && device.turnedOnAt != null) {
            while (true) {
                timeLeft = calculateTimeLeft(device)
                delay(1000)
            }
        } else {
            timeLeft = Duration.ZERO
        }
    }

    if (device.state == DeviceState.ON) {
        Text(
            text = formatDuration(timeLeft),
            style = MaterialTheme.typography.displayLarge,
            color = if (timeLeft.isNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Text(text = "Remaining", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { viewModel.toggleDeviceState(device.state) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("TURN OFF NOW")
        }
    } else {
        Button(onClick = { viewModel.toggleDeviceState(device.state) }) {
            Text("TURN ON")
        }
    }

    Spacer(modifier = Modifier.height(32.dp))
    Text(text = "Max Duration: ${device.maxOnDurationMin} min")
    Slider(
        value = device.maxOnDurationMin.toFloat(),
        onValueChange = { viewModel.updateIronDuration(it.toInt()) },
        valueRange = 5f..120f,
        steps = 23
    )
}

@Composable
fun BulbControls(
    device: Device.Bulb,
    viewModel: DeviceDetailViewModel,
    onTimeClick: (String, LocalTime) -> Unit
) {
    Switch(
        checked = device.state == DeviceState.ON,
        onCheckedChange = { viewModel.toggleDeviceState(device.state) },
        modifier = Modifier.scale(1.5f)
    )
    Spacer(modifier = Modifier.height(32.dp))
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Schedule Enabled", modifier = Modifier.weight(1f))
                Switch(
                    checked = device.scheduleEnabled,
                    onCheckedChange = { viewModel.updateBulbSchedule(it, device.scheduleStart, device.scheduleEnd) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            
            OutlinedButton(
                onClick = { onTimeClick("Start", device.scheduleStart) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Time: ${device.scheduleStart.format(formatter)}")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onTimeClick("End", device.scheduleEnd) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("End Time: ${device.scheduleEnd.format(formatter)}")
            }
        }
    }
}

@Composable
fun CameraControls(device: Device.Camera) {
    var timestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = "${device.cameraUri}?t=$timestamp",
                contentDescription = "Camera Feed",
                modifier = Modifier.fillMaxWidth().height(240.dp),
                contentScale = ContentScale.Crop
            )
            IconButton(onClick = { timestamp = System.currentTimeMillis() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
    }
}

private fun calculateTimeLeft(device: Device.Iron): Duration {
    if (device.state != DeviceState.ON || device.turnedOnAt == null) return Duration.ZERO
    val elapsed = Duration.between(device.turnedOnAt, Instant.now())
    val max = Duration.ofMinutes(device.maxOnDurationMin.toLong())
    return max.minus(elapsed)
}

private fun formatDuration(duration: Duration): String {
    val seconds = duration.abs().seconds
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (duration.isNegative) {
        String.format("-%02d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d:%02d", h, m, s)
    }
}
