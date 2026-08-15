# Smart Home Monitoring & Control System

A mobile Smart Home Monitoring and Control system built for **SCS3311: Mobile Application Design & Development**. The system lets a user manage multiple house floor plans, monitor and control heterogeneous smart devices, and enforce backend-driven safety rules for hazardous appliances — all synced in real time between the mobile app, a companion web-based hardware simulator, and Firebase.

---

## Overview

Users log in with email/password authentication, view their house across one or more floor plans, and interact with devices placed on each floor: power outlets, multi-switch gang boxes, safety-critical appliances (e.g. an iron) with an enforced auto-shutoff, schedulable light bulbs, and mock security cameras. All state changes sync bidirectionally through Firebase Firestore, so updates made in the app, in the hardware simulator, or by the backend's own safety-cutoff logic appear on every connected client within about a second — with no manual refresh.

## Features

- **Multi-floor dashboard** — add and browse multiple floor plans, each showing device pins positioned on a floor-plan image using normalized (0.0–1.0) coordinates so layouts stay aligned across screen sizes.
- **Heterogeneous device types**
  - **Outlet** — simple ON/OFF toggle.
  - **Multi-switch unit** — a single gang-box entity containing several independently toggleable sub-switches (e.g. Main Light, Fan).
  - **Iron (safety-critical)** — live ON-duration countdown against a configurable `maxOnDurationMin`, with server-enforced auto-shutoff independent of any client being open.
  - **Bulb** — supports a daily ON/OFF schedule (start time / end time) and schedule enable/disable toggle.
  - **Camera** — displays a mock snapshot with manual refresh functionality.
- **Real-time bidirectional sync** — every read is a live Firestore listener (no polling), so app ↔ simulator ↔ backend all stay in sync automatically within ~1 second.
- **Server-side safety cutoffs** — a backend process enforces `maxOnDurationMin` on safety-critical devices, flips the device to `OFF`, and raises an alert even if no client app is open at the time.
- **Alerts & usage reporting** — an in-app alerts feed with acknowledge functionality and a usage chart summarizing device ON-time by device.
- **Companion Hardware Simulator** — a separate web dashboard that mimics physical devices, listening to and writing the same Firestore data so it can be demoed independently of real hardware.
- **One-time seeding on first login** — new accounts automatically seed a demo house with sample floors and devices covering every device type, plus usage logs and alerts for demonstration.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose (Material 3) — 100% Compose, no XML layouts |
| Architecture | MVVM (`ViewModel` + `StateFlow`, unidirectional data flow) |
| Navigation | Jetpack Navigation Compose 2.9.8 |
| Backend | Firebase Firestore (realtime snapshot listeners via `callbackFlow`) |
| Auth | Firebase Authentication (Email/Password) |
| Async | Kotlin Coroutines & Flow |
| Image loading | Coil 2.7.0 |
| Compile SDK / Target SDK | 37 |
| Min SDK | 26 |
| Java Compatibility | JDK 11 |
| Testing | JUnit 4 |

## Architecture

```
app/src/main/java/com/example/smarthome/
├── data/
│   ├── model/
│   │   └── Dtos.kt              # Firestore DTOs (DeviceDto, FloorDto, AlertDto, UsageLogDto, HouseDto)
│   ├── Mappers.kt                # DTO ↔ domain model conversion (devices, floors, alerts, usage logs)
│   └── FirestoreRepository.kt    # Flow-based reads (snapshot listeners) + suspend writes
├── domain/
│   └── Models.kt                 # Plain Kotlin domain models — Device is a sealed class per type
├── ui/
│   ├── auth/
│   │   └── LoginScreen.kt        # Email/Password login UI
│   ├── home/
│   │   ├── HomeScreen.kt         # Floor list with device/alert summary
│   │   └── HomeViewModel.kt      # Combines floors, devices, alerts into UiState
│   ├── dashboard/
│   │   ├── FloorDashboardScreen.kt   # Floor dashboard (device pins)
│   │   ├── FloorDashboardViewModel.kt
│   │   └── FloorDashboardUiState.kt
│   ├── device/
│   │   ├── DeviceDetailScreen.kt     # Per-device-type detail screen + controls
│   │   └── DeviceDetailViewModel.kt
│   ├── alerts/
│   │   ├── AlertsUsageScreen.kt      # Alerts feed + usage chart by device
│   │   └── AlertsUsageViewModel.kt
│   ├── components/
│   │   └── Shared composables (LoadingScreen, ErrorScreen, etc.)
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   └── Theme.kt                  # Material 3 color/type theme
│   └── SmartHomeNavGraph.kt          # Navigation routing
├── SmartHomeApplication.kt           # App-wide singletons (Firebase, repository)
└── MainActivity.kt                   # Single Activity — hosts Compose NavHost
```

### Data Flow

```
Firestore ←→ FirestoreRepository (callbackFlow + StateFlow)
                ↓
            Mappers (DTO → Domain)
                ↓
            ViewModel (StateFlow<UiState>)
                ↓
            @Composable (collects & renders)
```

Each screen follows the same MVVM pattern:
- The `ViewModel` wraps Firestore `addSnapshotListener` in a `callbackFlow`
- Exposes state as a `StateFlow<UiState>` (Loading, Success, Error)
- The Composable screen collects that state and re-renders automatically on every change — no manual refresh anywhere in the app
- All UI is built with `@Composable` functions, no XML layouts

## Firestore Data Model

```
houses/{houseId}
  name: string

  floors/{floorId}
    name: string
    gridWidth, gridHeight: number (0-10 typically)
    imageUrl: string (Google Drive direct download link)

    devices/{deviceId}
      name: string
      type: "outlet" | "multiswitch" | "iron" | "bulb" | "camera"
      state: "ON" | "OFF" | "ERROR" | "DISCONNECTED"
      positionX, positionY: number        # 0.0–1.0, normalized coordinates

      # type == "multiswitch"
      switches: [{ id, name, state }]

      # type == "iron" / safety-critical
      maxOnDurationMin: number
      turnedOnAt: Timestamp | null         # set when turned ON, deleted when turned OFF

      # type == "bulb"
      scheduleEnabled: boolean
      scheduleStart, scheduleEnd: "HH:mm"  # e.g. "18:00", "06:00"

      # type == "camera"
      cameraUri: string

  usageLogs/{logId}
    houseId: string
    deviceId: string
    deviceName: string
    event: "ON" | "OFF"
    timestamp: Timestamp

  alerts/{alertId}
    houseId: string
    deviceId: string
    deviceName: string
    message: string
    timestamp: Timestamp
    acknowledged: boolean
```

### User Account Mapping

A user's Firebase Auth `uid` doubles as their `houses/{uid}` document ID — each account maps to exactly one house. On first sign-in for a new account, the app calls `seedDatabase()` to populate that house with:
- 2 demo floors (Ground Floor, First Floor) with floor plan images
- 6 demo devices covering all device types
- 5 usage logs (device turn-on/off events from the past)
- 1 sample alert (Iron safety warning)

## Getting Started

### Prerequisites
- **Android Studio** (latest stable)
- **Android SDK 37** (or higher for compilation)
- **JDK 11** or higher
- **A Firebase project** with:
  - **Firestore** enabled (native mode)
  - **Authentication (Email/Password)** enabled
- Your own **`google-services.json`** placed in `app/`

### Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/SinethMendis/SmartHome.git
   cd SmartHome
   ```

2. **Add Firebase configuration:**
   - Go to your Firebase Console → Project Settings → Android app
   - Download `google-services.json` (ensure package name is `com.example.smarthome`)
   - Place the file at `app/google-services.json`

3. **Open in Android Studio:**
   - Open the project root in Android Studio
   - Let Gradle sync automatically
   - If prompted, accept any SDK or plugin updates

4. **Run on device or emulator:**
   ```bash
   # Using Android Studio: click "Run" (Shift+F10)
   # Or via CLI:
   ./gradlew build
   ./gradlew installDebug
   ```
   - Minimum SDK 26 (Android 8.0)
   - Target SDK 37 (Android 15)

5. **First login:**
   - Sign up with an email and password
   - The app automatically seeds your house with demo data
   - You'll see the Home screen with 2 floors and 6 pre-loaded devices

### Running Tests

```bash
./gradlew test
```

Includes:
- DTO → domain model mapping tests (verifying floor/device/alert conversions)
- Iron countdown-calculation logic tests
- Device state parsing tests

Run specific test file:
```bash
./gradlew test --tests com.example.smarthome.data.MappersKt
```

## Project Structure Details

### Domain Models (`domain/Models.kt`)

**Device (sealed class):**
- `Outlet(id, name, state, position)`
- `MultiSwitch(id, name, state, position, switches: List<SubSwitch>)`
- `Iron(id, name, state, position, maxOnDurationMin, turnedOnAt: Instant?)`
- `Bulb(id, name, state, position, scheduleEnabled, scheduleStart, scheduleEnd: LocalTime)`
- `Camera(id, name, state, position, cameraUri)`

**Other domain models:**
- `Floor(id, name, gridWidth, gridHeight, imageUrl, deviceCount, activeDeviceCount, activeAlertCount)`
- `House(id, name)`
- `Alert(id, houseId, deviceId, deviceName, message, timestamp, acknowledged)`
- `UsageLog(id, houseId, deviceId, deviceName, event, timestamp)`
- `DeviceState` enum: `ON, OFF, ERROR, DISCONNECTED`

### Data Models (`data/model/Dtos.kt`)

Firestore Data Transfer Objects (DTOs):
- `HouseDto`, `FloorDto`, `DeviceDto`, `UsageLogDto`, `AlertDto`
- Automatically serialized/deserialized by Firestore SDK
- Mappers convert to domain models for business logic

### Repository Pattern (`data/FirestoreRepository.kt`)

Provides reactive, Flow-based API:
- **Read operations** (return `Flow<T>`):
  - `getHouse()`, `getFloors()`, `getFloor(floorId)`, `getDevices(floorId)`, `getDevice(...)`
  - `getAlerts()`, `getUsageLogsToday()`
  - Each wraps a `callbackFlow` around Firestore `addSnapshotListener`

- **Write operations** (suspend functions):
  - `updateDeviceState(floorId, deviceId, state)` — sets `turnedOnAt` for ON, deletes for OFF
  - `updateMultiSwitch(floorId, deviceId, switches)` — updates sub-switch states
  - `updateIronSettings(floorId, deviceId, maxDuration)` — changes max on-time
  - `updateBulbSchedule(floorId, deviceId, enabled, start, end)` — schedules bulb times
  - `acknowledgeAlert(alertId)` — marks alert as seen

- **Admin operation**:
  - `seedDatabase()` — one-time initialization with demo data

### ViewModels

All ViewModels:
- Extend `ViewModel()`
- Expose `uiState: StateFlow<SomeUiState>`
- Use `callbackFlow` internally to bridge Firestore listeners
- Combine multiple flows when needed (e.g., HomeViewModel combines floors + alerts)
- Provide Factory companion objects for dependency injection

**HomeViewModel:**
- Combines floors (with active device count) + unacknowledged alert count
- Provides `addFloor(name, width, height, imageUrl)` action

**FloorDashboardViewModel:**
- Observes a single floor + its devices
- Provides device interaction methods

**DeviceDetailViewModel:**
- Observes a single device
- Provides device-type-specific update methods (toggle, schedule, settings, etc.)

**AlertsUsageViewModel:**
- Fetches today's usage logs + all alerts
- Computes usage duration per device for charting

### Authentication Flow

1. **Unauthenticated** → User sees LoginScreen
2. **Login success** → `MainActivity.seedIfReady(uid)` runs
3. **Seed completes** → Status → Success → NavHost starts
4. **Logout** — Currently requires manual data clearing (limitation noted below)

### Navigation (`ui/SmartHomeNavGraph.kt`)

Single-Activity navigation using Jetpack Navigation Compose:

```
Home (list of floors)
  ↓ click floor
  → FloorDashboard (floor + device pins)
    ↓ click device
    → DeviceDetail (device-specific UI)

Home (menu icon)
  ↓ click alerts icon
  → Alerts (alerts + usage chart)
```

## Known Limitations & Future Work

1. **Logout not implemented** — signing out requires clearing app data or reinstalling until a sign-out action is added to the UI.

2. **Seeding is one-time per house** — changes made to seed data after a house document is created require deleting the `houses/{uid}` document in Firestore console to see the update.

3. **No offline persistence** — the app requires internet connectivity; offline support via Firestore's offline persistence layer is not yet configured.

4. **Manual camera refresh only** — camera feeds use a static mock image; real-time camera streaming is not implemented.

5. **No device deletion** — floors and devices can be created but not deleted from the UI (only via Firestore console).

6. **Limited device customization** — device names and positions are set at creation; in-app editing is not yet available.

## Dependencies

**Firebase:**
- `com.google.firebase:firebase-bom:34.16.0`
- `com.google.firebase:firebase-auth`
- `com.google.firebase:firebase-firestore:26.4.1`

**Jetpack:**
- `androidx.activity:activity-compose:1.13.0`
- `androidx.lifecycle:lifecycle-runtime-ktx:2.11.0`
- `androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0`
- `androidx.navigation:navigation-compose:2.9.8`
- `androidx.compose.bom:2026.02.01` (BOM managing all Compose libs)

**Other:**
- `io.coil-kt:coil-compose:2.7.0` (image loading)
- `org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0` (Firebase-Kotlin bridge)
- `junit:junit:4.13.2` (unit tests)

See `gradle/libs.versions.toml` for all versions and exact dependency specs.

## Seeding Details

On first login, `seedDatabase()` creates:

**Floors:**
- **Ground Floor** (10×10 grid): Living Room Outlet, Kitchen Light (bulb with schedule), Bedroom Bulb
- **First Floor** (10×8 grid): Bedroom Iron (safety-critical), Hallway Switches (multi-switch), Front Door Camera

**Demo Usage Logs** (showing device usage):
- Living Room Outlet: ON for 45 mins
- Bedroom Bulb: ON for 120 mins
- Kitchen Light: still ON (started 4 hours ago)
- Bedroom Iron: ON for 20 mins (and currently still ON)
- Hallway Main Light: still ON (started 1 hour ago)

**Demo Alerts:**
- "Iron has been ON for too long!" (bedroom-iron)

This data demonstrates all device types and patterns; it can be cleared by deleting the house document in Firestore.

## Team

| Name | Role / Contribution |
|---|---|
| Sineth Mendis | Lead developer — Firebase integration, repository, app architecture |
| Bihansa | Mobile application UI — screens, navigation, device detail screens |
| _[Name]_ | Hardware simulator, cross-client sync validation, safety automation |

## Submission Deliverables

- ✅ **Source code:** This repository
- ⬜ **Final APK:** _[add link]_
- ⬜ **Technical documentation:** _[add link]_
- ⬜ **Recorded demonstration video:** _[add link]_

## Development Tips

- **Enable Firestore offline persistence** in `SmartHomeApplication.kt` for offline support (add `db.firestoreSettings = FirestoreSettings.Builder().setPersistenceEnabled(true).build()`)
- **Add more floors** via the Home screen UI or Firestore console
- **Simulate device errors** by changing device state to `ERROR` in Firestore
- **Test safety cutoffs** by manually updating iron `maxOnDurationMin` and `turnedOnAt`
- **Monitor sync** by opening the same house in the web simulator and mobile app simultaneously

## Troubleshooting

| Issue | Solution |
|---|---|
| "google-services.json not found" | Ensure the file is at `app/google-services.json` with correct package name `com.example.smarthome` |
| "Compilation failed" | Check Java version is 11+: `java -version` |
| "Firestore permission denied" | Verify Firestore rules allow reads/writes to `houses/{uid}/*` |
| "Devices not showing" | Check network — app requires internet for real-time sync |
| "Seeds not appearing" | Delete the house document in Firestore and log in again |
| Gradle sync issues | Run `./gradlew clean && ./gradlew build` |

---

**Built with ❤️ for SCS3311: Mobile Application Design & Development**
