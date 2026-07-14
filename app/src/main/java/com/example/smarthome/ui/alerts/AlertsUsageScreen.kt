package com.example.smarthome.ui.alerts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smarthome.R
import com.example.smarthome.domain.Alert
import com.example.smarthome.ui.components.ErrorScreen
import com.example.smarthome.ui.components.LoadingScreen
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsUsageScreen(
    onBackClick: () -> Unit,
    viewModel: AlertsUsageViewModel = viewModel(factory = AlertsUsageViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.alerts_usage_title)) },
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
                is AlertsUsageUiState.Loading -> LoadingScreen()
                is AlertsUsageUiState.Error -> ErrorScreen(
                    message = state.message,
                    onRetry = { /* Auto-retries */ }
                )
                is AlertsUsageUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Active Alerts",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                        AlertsList(
                            alerts = state.alerts,
                            onAcknowledge = { viewModel.acknowledgeAlert(it) },
                            modifier = Modifier.weight(1f)
                        )
                        Divider()
                        Text(
                            text = "Today's Usage (Minutes)",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                        UsageChart(
                            usageData = state.usageData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlertsList(
    alerts: List<Alert>,
    onAcknowledge: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeAlerts = alerts.filter { !it.acknowledged }
    
    if (activeAlerts.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("No active alerts")
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activeAlerts) { alert ->
                AlertCard(alert = alert, onClick = { onAcknowledge(alert.id) })
            }
        }
    }
}

@Composable
fun AlertCard(alert: Alert, onClick: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = alert.deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatter.format(alert.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = alert.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun UsageChart(
    usageData: List<DeviceUsage>,
    modifier: Modifier = Modifier
) {
    if (usageData.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No usage data today")
        }
        return
    }

    val maxMinutes = usageData.maxOf { it.totalMinutes }.coerceAtLeast(1).toFloat()
    val barColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Canvas(modifier = modifier) {
        val spacing = 20.dp.toPx()
        val barWidth = (size.width - (spacing * (usageData.size + 1))) / usageData.size
        
        usageData.forEachIndexed { index, device ->
            val barHeight = (device.totalMinutes / maxMinutes) * (size.height - 40.dp.toPx())
            val x = spacing + index * (barWidth + spacing)
            val y = size.height - barHeight - 20.dp.toPx()

            drawRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )

            // Label
            drawContext.canvas.nativeCanvas.drawText(
                device.deviceName.take(6),
                x + barWidth / 2,
                size.height,
                android.graphics.Paint().apply {
                    color = textColor
                    textSize = 12.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )

            // Value
            drawContext.canvas.nativeCanvas.drawText(
                device.totalMinutes.toString(),
                x + barWidth / 2,
                y - 5.dp.toPx(),
                android.graphics.Paint().apply {
                    color = textColor
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}

private fun Color.toArgb(): Int {
    return (alpha * 255.0f + 0.5f).toInt() shl 24 or
           (red * 255.0f + 0.5f).toInt() shl 16 or
           (green * 255.0f + 0.5f).toInt() shl 8 or
           (blue * 255.0f + 0.5f).toInt()
}
