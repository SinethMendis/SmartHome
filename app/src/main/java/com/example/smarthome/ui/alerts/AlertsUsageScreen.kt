package com.example.smarthome.ui.alerts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
    val labelColor = android.graphics.Color.BLACK

    Box(modifier = modifier.horizontalScroll(rememberScrollState())) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .width((usageData.size * 60).dp)
                .padding(bottom = 40.dp, top = 20.dp) // more room for 2-line labels
        ) {
            val spacing = 12.dp.toPx()
            val availableWidth = size.width - spacing
            val barWidth = (availableWidth / usageData.size) - spacing

            val labelPaint = android.graphics.Paint().apply {
                color = labelColor
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            val valuePaint = android.graphics.Paint().apply {
                color = labelColor
                textSize = 11.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            val lineHeight = labelPaint.textSize + 2.dp.toPx()

            usageData.forEachIndexed { index, device ->
                val ratio = device.totalMinutes.toFloat() / maxMinutes
                val barHeight = ratio * size.height
                val x = spacing + index * (barWidth + spacing)
                val y = size.height - barHeight

                val statusColor = when {
                    ratio < 0.33f -> Color(0xFF4CAF50)
                    ratio < 0.66f -> Color(0xFFFFA000)
                    else -> Color(0xFFF44336)
                }

                drawRect(
                    color = statusColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )

                // Wrap label into lines no wider than barWidth, max 2 lines
                val lines = wrapTextToWidth(
                    text = device.deviceName,
                    paint = labelPaint,
                    maxWidth = barWidth,
                    maxLines = 2
                )
                lines.forEachIndexed { lineIndex, line ->
                    drawContext.canvas.nativeCanvas.drawText(
                        line,
                        x + barWidth / 2,
                        size.height + 16.dp.toPx() + lineIndex * lineHeight,
                        labelPaint
                    )
                }

                drawContext.canvas.nativeCanvas.drawText(
                    "${device.totalMinutes}m",
                    x + barWidth / 2,
                    y - 8.dp.toPx(),
                    valuePaint
                )
            }
        }
    }
}

private fun wrapTextToWidth(
    text: String,
    paint: android.graphics.Paint,
    maxWidth: Float,
    maxLines: Int
): List<String> {
    if (text.isEmpty()) return emptyList()

    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var current = StringBuilder()

    fun flushCurrent() {
        if (current.isNotEmpty()) {
            lines.add(current.toString())
            current = StringBuilder()
        }
    }

    for (word in words) {
        val candidate = if (current.isEmpty()) word else "${current} $word"
        if (paint.measureText(candidate) <= maxWidth) {
            current = StringBuilder(candidate)
        } else {
            if (current.isEmpty()) {
                var chars = StringBuilder()
                for (c in word) {
                    val test = chars.toString() + c
                    if (paint.measureText(test) <= maxWidth) {
                        chars.append(c)
                    } else {
                        lines.add(chars.toString())
                        chars = StringBuilder().append(c)
                        if (lines.size == maxLines) break
                    }
                }
                current = chars
            } else {
                flushCurrent()
                current = StringBuilder(word)
            }
        }
        if (lines.size == maxLines) break
    }
    if (lines.size < maxLines) flushCurrent()

    val consumedLength = lines.joinToString(" ").length
    if (lines.size == maxLines && consumedLength < text.length) {
        var lastLine = lines.last()
        while (lastLine.isNotEmpty() &&
            paint.measureText("$lastLine…") > maxWidth
        ) {
            lastLine = lastLine.dropLast(1)
        }
        lines[lines.lastIndex] = "$lastLine…"
    }

    return lines.take(maxLines)
}

private fun Color.toArgb(): Int {
    return (alpha * 255.0f + 0.5f).toInt() shl 24 or
           (red * 255.0f + 0.5f).toInt() shl 16 or
           (green * 255.0f + 0.5f).toInt() shl 8 or
           (blue * 255.0f + 0.5f).toInt()
}
