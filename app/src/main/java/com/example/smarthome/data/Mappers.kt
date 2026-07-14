package com.example.smarthome.data

import androidx.compose.ui.geometry.Offset
import com.example.smarthome.data.model.AlertDto
import com.example.smarthome.data.model.DeviceDto
import com.example.smarthome.data.model.FloorDto
import com.example.smarthome.data.model.UsageLogDto
import com.example.smarthome.domain.Alert
import com.example.smarthome.domain.Device
import com.example.smarthome.domain.DeviceState
import com.example.smarthome.domain.Floor
import com.example.smarthome.domain.SubSwitch
import com.example.smarthome.domain.UsageLog
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun DeviceDto.toDomain(id: String): Device {
    val deviceState = try {
        DeviceState.valueOf(state)
    } catch (e: Exception) {
        DeviceState.OFF
    }
    val position = Offset(positionX.toFloat(), positionY.toFloat())

    return when (type) {
        "outlet" -> Device.Outlet(id, name, deviceState, position)
        "multiswitch" -> Device.MultiSwitch(
            id, name, deviceState, position,
            switches = switches?.map { 
                SubSwitch(
                    id = it["id"] as? String ?: "",
                    name = it["name"] as? String ?: "",
                    state = try { DeviceState.valueOf(it["state"] as? String ?: "OFF") } catch (e: Exception) { DeviceState.OFF }
                )
            } ?: emptyList()
        )
        "iron" -> Device.Iron(
            id, name, deviceState, position,
            maxOnDurationMin = maxOnDurationMin ?: 30,
            turnedOnAt = turnedOnAt?.toDate()?.toInstant()
        )
        "bulb" -> Device.Bulb(
            id, name, deviceState, position,
            scheduleEnabled = scheduleEnabled ?: false,
            scheduleStart = scheduleStart?.let { LocalTime.parse(it, timeFormatter) } ?: LocalTime.MIN,
            scheduleEnd = scheduleEnd?.let { LocalTime.parse(it, timeFormatter) } ?: LocalTime.MAX
        )
        "camera" -> Device.Camera(
            id, name, deviceState, position,
            cameraUri = cameraUri ?: ""
        )
        else -> Device.Outlet(id, name, deviceState, position) // Fallback
    }
}

fun FloorDto.toDomain(id: String, deviceCount: Int = 0, activeAlertCount: Int = 0): Floor {
    return Floor(
        id = id,
        name = name,
        gridWidth = gridWidth,
        gridHeight = gridHeight,
        imageUrl = imageUrl,
        deviceCount = deviceCount,
        activeAlertCount = activeAlertCount
    )
}

fun AlertDto.toDomain(id: String): Alert {
    return Alert(
        id = id,
        houseId = houseId,
        deviceId = deviceId,
        deviceName = deviceName,
        message = message,
        timestamp = timestamp.toDate().toInstant(),
        acknowledged = acknowledged
    )
}

fun UsageLogDto.toDomain(id: String): UsageLog {
    return UsageLog(
        id = id,
        houseId = houseId,
        deviceId = deviceId,
        deviceName = deviceName,
        event = event,
        timestamp = timestamp.toDate().toInstant()
    )
}
