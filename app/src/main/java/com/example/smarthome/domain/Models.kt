package com.example.smarthome.domain

import androidx.compose.ui.geometry.Offset
import java.time.Instant
import java.time.LocalTime

enum class DeviceState { ON, OFF, ERROR, DISCONNECTED }

sealed class Device {
    abstract val id: String
    abstract val name: String
    abstract val state: DeviceState
    abstract val position: Offset

    data class Outlet(
        override val id: String,
        override val name: String,
        override val state: DeviceState,
        override val position: Offset
    ) : Device()

    data class MultiSwitch(
        override val id: String,
        override val name: String,
        override val state: DeviceState,
        override val position: Offset,
        val switches: List<SubSwitch>
    ) : Device()

    data class Iron(
        override val id: String,
        override val name: String,
        override val state: DeviceState,
        override val position: Offset,
        val maxOnDurationMin: Int,
        val turnedOnAt: Instant?
    ) : Device()

    data class Bulb(
        override val id: String,
        override val name: String,
        override val state: DeviceState,
        override val position: Offset,
        val scheduleEnabled: Boolean,
        val scheduleStart: LocalTime,
        val scheduleEnd: LocalTime
    ) : Device()

    data class Camera(
        override val id: String,
        override val name: String,
        override val state: DeviceState,
        override val position: Offset,
        val cameraUri: String
    ) : Device()
}

data class SubSwitch(
    val id: String,
    val name: String,
    val state: DeviceState
)

data class Floor(
    val id: String,
    val name: String,
    val gridWidth: Int,
    val gridHeight: Int,
    val imageUrl: String,
    val deviceCount: Int = 0,
    val activeAlertCount: Int = 0
)

data class House(
    val id: String,
    val name: String
)

data class Alert(
    val id: String,
    val houseId: String,
    val deviceId: String,
    val deviceName: String,
    val message: String,
    val timestamp: Instant,
    val acknowledged: Boolean
)

data class UsageLog(
    val id: String,
    val houseId: String,
    val deviceId: String,
    val deviceName: String,
    val event: String,
    val timestamp: Instant
)
