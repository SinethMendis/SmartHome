package com.example.smarthome.data.model

import com.google.firebase.Timestamp

data class HouseDto(
    val name: String = ""
)

data class FloorDto(
    val name: String = "",
    val gridWidth: Int = 0,
    val gridHeight: Int = 0,
    val imageUrl: String = ""
)

data class DeviceDto(
    val name: String = "",
    val type: String = "",
    val state: String = "OFF",
    val positionX: Double = 0.0,
    val positionY: Double = 0.0,
    val turnedOnAt: Timestamp? = null,
    val switches: List<Map<String, Any>>? = null,
    val maxOnDurationMin: Int? = null,
    val scheduleEnabled: Boolean? = null,
    val scheduleStart: String? = null,
    val scheduleEnd: String? = null,
    val cameraUri: String? = null
)

data class UsageLogDto(
    val houseId: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val event: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

data class AlertDto(
    val houseId: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val acknowledged: Boolean = false
)
