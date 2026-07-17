package com.example.smarthome.data

import com.example.smarthome.data.model.AlertDto
import com.example.smarthome.data.model.DeviceDto
import com.example.smarthome.data.model.FloorDto
import com.example.smarthome.data.model.HouseDto
import com.example.smarthome.data.model.UsageLogDto
import com.example.smarthome.domain.Alert
import com.example.smarthome.domain.Device
import com.example.smarthome.domain.Floor
import com.example.smarthome.domain.House
import com.example.smarthome.domain.UsageLog
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository(private val db: FirebaseFirestore, private val houseId: String) {

    fun getHouse(): Flow<House?> = callbackFlow {
        val listener = db.collection("houses").document(houseId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val house = snapshot.toObject(HouseDto::class.java)?.let {
                        House(id = snapshot.id, name = it.name)
                    }
                    trySend(house)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getFloors(): Flow<List<Floor>> = callbackFlow {
        val listener = db.collection("houses").document(houseId)
            .collection("floors")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val floors = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(FloorDto::class.java)?.toDomain(doc.id)
                    }
                    trySend(floors)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getFloor(floorId: String): Flow<Floor?> = callbackFlow {
        val listener = db.collection("houses").document(houseId)
            .collection("floors").document(floorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val floor = snapshot.toObject(FloorDto::class.java)?.toDomain(snapshot.id)
                    trySend(floor)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getDevices(floorId: String): Flow<List<Device>> = callbackFlow {
        val listener = db.collection("houses").document(houseId)
            .collection("floors").document(floorId)
            .collection("devices")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val devices = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(DeviceDto::class.java)?.toDomain(doc.id)
                    }
                    trySend(devices)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getDevice(floorId: String, deviceId: String): Flow<Device?> = callbackFlow {
        val listener = db.collection("houses").document(houseId)
            .collection("floors").document(floorId)
            .collection("devices").document(deviceId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val device = snapshot.toObject(DeviceDto::class.java)?.toDomain(snapshot.id)
                    trySend(device)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getAlerts(): Flow<List<Alert>> = callbackFlow {
        val listener = db.collection("alerts")
            .whereEqualTo("houseId", houseId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val alerts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(AlertDto::class.java)?.toDomain(doc.id)
                    }
                    trySend(alerts)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun acknowledgeAlert(alertId: String) {
        db.collection("alerts").document(alertId)
            .update("acknowledged", true)
            .await()
    }

    fun getUsageLogsToday(): Flow<List<UsageLog>> = callbackFlow {
        val startOfDay = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.time

        val listener = db.collection("usageLogs")
            .whereEqualTo("houseId", houseId)
            .whereGreaterThanOrEqualTo("timestamp", Timestamp(startOfDay))
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val logs = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UsageLogDto::class.java)?.toDomain(doc.id)
                    }
                    trySend(logs)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun addFloor(name: String, gridWidth: Int, gridHeight: Int, imageUrl: String) {
        val houseRef = db.collection("houses").document(houseId)
        houseRef.collection("floors").add(
            FloorDto(
                name = name,
                gridWidth = gridWidth,
                gridHeight = gridHeight,
                imageUrl = imageUrl
            )
        ).await()
    }

    suspend fun updateDeviceState(floorId: String, deviceId: String, state: String) {
        val deviceRef = db.collection("houses").document(houseId)
            .collection("floors").document(floorId)
            .collection("devices").document(deviceId)
        
        val updates = mutableMapOf<String, Any>("state" to state)
        if (state == "ON") {
            updates["turnedOnAt"] = Timestamp.now()
        } else {
            updates["turnedOnAt"] = com.google.firebase.firestore.FieldValue.delete()
        }
        
        deviceRef.update(updates).await()
    }

    suspend fun updateMultiSwitch(floorId: String, deviceId: String, switches: List<Map<String, Any>>) {
        db.collection("houses").document(houseId)
            .collection("floors").document(floorId)
            .collection("devices").document(deviceId)
            .update("switches", switches)
            .await()
    }

    suspend fun updateIronSettings(floorId: String, deviceId: String, maxDuration: Int) {
        db.collection("houses").document(houseId)
            .collection("floors").document(floorId)
            .collection("devices").document(deviceId)
            .update("maxOnDurationMin", maxDuration)
            .await()
    }

    suspend fun updateBulbSchedule(
        floorId: String,
        deviceId: String,
        enabled: Boolean,
        start: String,
        end: String
    ) {
        db.collection("houses").document(houseId)
            .collection("floors").document(floorId)
            .collection("devices").document(deviceId)
            .update(
                mapOf(
                    "scheduleEnabled" to enabled,
                    "scheduleStart" to start,
                    "scheduleEnd" to end
                )
            ).await()
    }

    suspend fun seedDatabase() {
        val houses = db.collection("houses")
        
        // Check if data already exists to make it "one-time"
        // val existing = houses.limit(1).get().await()
        // if (!existing.isEmpty) return

        //val houseId = "main-house"
        val houseRef = houses.document(houseId)

        val existingHouse = houseRef.get().await()

        if (existingHouse.exists()) {
            return
        }

        houseRef.set(HouseDto(name = "My Smart Home")).await()

        val floors = houseRef.collection("floors")
        
        // Ground Floor
        val groundFloorRef = floors.document("ground-floor")
        groundFloorRef.set(FloorDto(
            name = "Ground Floor",
            gridWidth = 10,
            gridHeight = 10,
            imageUrl = "https://drive.google.com/uc?export=view&id=14dM25N1Z4rDP30uVahuYEKj8ND_nuvnb"
        )).await()

        val groundDevices = groundFloorRef.collection("devices")
        val outletId = "living-room-outlet"
        groundDevices.document(outletId).set(DeviceDto(
            name = "Living Room Outlet",
            type = "outlet",
            state = "OFF",
            positionX = 0.2,
            positionY = 0.3
        )).await()

        groundDevices.document("kitchen-light").set(DeviceDto(
            name = "Kitchen Light",
            type = "bulb",
            state = "ON",
            positionX = 0.5,
            positionY = 0.5,
            scheduleEnabled = true,
            scheduleStart = "18:00",
            scheduleEnd = "06:00"
        )).await()

        // First Floor
        val firstFloorRef = floors.document("first-floor")
        firstFloorRef.set(FloorDto(
            name = "First Floor",
            gridWidth = 10,
            gridHeight = 8,
            imageUrl = "https://drive.google.com/uc?export=view&id=1i-o7LUzGsFtGHL8lBc3RPxAVgiL5x9V7"
        )).await()

        val firstDevices = firstFloorRef.collection("devices")
        val ironId = "bedroom-iron"
        firstDevices.document(ironId).set(DeviceDto(
            name = "Bedroom Iron",
            type = "iron",
            state = "OFF",
            positionX = 0.8,
            positionY = 0.2,
            maxOnDurationMin = 30
        )).await()

        firstDevices.document("hallway-multi").set(DeviceDto(
            name = "Hallway Switches",
            type = "multiswitch",
            state = "ON",
            positionX = 0.1,
            positionY = 0.5,
            switches = listOf(
                mapOf("id" to "sw1", "name" to "Main Light", "state" to "ON"),
                mapOf("id" to "sw2", "name" to "Fan", "state" to "OFF")
            )
        )).await()

        firstDevices.document("front-door-camera").set(DeviceDto(
            name = "Front Door Camera",
            type = "camera",
            state = "ON",
            positionX = 0.0,
            positionY = 0.1,
            cameraUri = "https://example.com/camera_snapshot.jpg"
        )).await()

        // Seed Usage Logs
        db.collection("usageLogs").add(UsageLogDto(
            houseId = houseId,
            deviceId = outletId,
            deviceName = "Living Room Outlet",
            event = "OFF",
            timestamp = Timestamp.now()
        )).await()

        // Seed Alerts
        db.collection("alerts").add(AlertDto(
            houseId = houseId,
            deviceId = ironId,
            deviceName = "Bedroom Iron",
            message = "Iron has been ON for too long!",
            timestamp = Timestamp.now(),
            acknowledged = false
        )).await()
    }
}
