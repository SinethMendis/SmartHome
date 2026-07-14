# Smart Home Monitoring & Control — Android App Specification

This document is a complete build specification for an Android application. Implement it using Kotlin and Jetpack Compose, following Android/Firebase best practices. Ask clarifying questions only if something below is genuinely ambiguous — otherwise proceed with the defaults stated.

## 1. Project overview

Build a Smart Home Monitoring and Control mobile app. Users manage multiple house floor plans, each containing a set of smart devices (outlets, multi-switch gang boxes, safety-critical appliances like irons, schedulable light bulbs, and security cameras). Device state must sync in real time with a Firebase Firestore backend, which is shared with a separate web-based hardware simulator (not part of this build — treat Firestore as an external, already-defined contract).

## 2. Tech stack

- Language: Kotlin
- UI: Jetpack Compose (Material 3)
- Architecture: MVVM — `ViewModel` + `StateFlow`/`MutableStateFlow`, unidirectional data flow
- Backend: Firebase Firestore (realtime listeners, no manual polling/refresh)
- Auth: Firebase Anonymous Authentication (single implicit user per install; no login screen)
- Navigation: Jetpack Navigation Compose
- Dependency injection: manual factory or Hilt (Hilt preferred if available)
- Async: Kotlin Coroutines + Flow
- Min SDK: 26, Target SDK: latest stable
- Image loading: Coil (for camera mock snapshots and floor plan background images)

## 3. Firestore data model (shared contract — do not deviate)

```
houses/{houseId}
  name: string

  floors/{floorId}
    name: string
    gridWidth: number
    gridHeight: number
    imageUrl: string              // background floor plan image

    devices/{deviceId}
      name: string
      type: string                // "outlet" | "multiswitch" | "iron" | "bulb" | "camera"
      state: string               // "ON" | "OFF" | "ERROR" | "DISCONNECTED"
      positionX: number           // 0.0–1.0, normalized position on the floor grid
      positionY: number           // 0.0–1.0
      turnedOnAt: timestamp | null

      // type == "multiswitch" only
      switches: array<map>        // [{ id: string, name: string, state: "ON"|"OFF" }]

      // type == "iron" (or any safety-critical device) only
      maxOnDurationMin: number

      // type == "bulb" only
      scheduleEnabled: boolean
      scheduleStart: string       // "HH:mm"
      scheduleEnd: string         // "HH:mm"

      // type == "camera" only
      cameraUri: string           // mock snapshot/stream URL

  usageLogs/{logId}
    houseId: string
    deviceId: string
    deviceName: string
    event: string                // "ON" | "OFF" | "ERROR" | "DISCONNECTED"
    timestamp: timestamp

  alerts/{alertId}
    houseId: string
    deviceId: string
    deviceName: string
    message: string
    timestamp: timestamp
    acknowledged: boolean
```

Assume this schema is already seeded with at least one house, two floors, and 5–6 sample devices covering every type. All reads/writes must go through Firestore's realtime listeners (`addSnapshotListener` equivalents in the Kotlin SDK, e.g. `Flow` built on `callbackFlow`), never one-off `get()` calls for UI-bound data.

## 4. App architecture

- `data/` — Firestore data models (`DeviceDto`, `FloorDto`, `AlertDto`, `UsageLogDto`) and a `FirestoreRepository` exposing `Flow`-based read functions and suspend functions for writes.
- `domain/` — plain Kotlin domain models (`Device` sealed class per type below) and mapping functions from DTOs.
- `ui/` — one package per screen, each with a `Screen.kt` (Composable), `ViewModel.kt`, and `UiState.kt` (data class or sealed interface: `Loading`, `Success(data)`, `Error(message)`).
- `di/` — Hilt modules for Firestore instance, repositories, and (if used) Firebase Auth.

Model devices as a sealed class so each type carries only its relevant fields:

```kotlin
sealed class Device {
    abstract val id: String
    abstract val name: String
    abstract val state: DeviceState
    abstract val position: Offset // 0f..1f, 0f..1f

    data class Outlet(...) : Device()
    data class MultiSwitch(val switches: List<SubSwitch>, ...) : Device()
    data class Iron(val maxOnDurationMin: Int, val turnedOnAt: Instant?, ...) : Device()
    data class Bulb(val scheduleEnabled: Boolean, val scheduleStart: LocalTime, val scheduleEnd: LocalTime, ...) : Device()
    data class Camera(val cameraUri: String, ...) : Device()
}

enum class DeviceState { ON, OFF, ERROR, DISCONNECTED }
```

## 5. Screens

### 5.1 Home — floor list
- Lists all floors for the current house as cards (name, device count, active alert count).
- "Add floor plan" action opens a dialog: name, grid dimensions, and an image picker/URL field for the floor plan background; writes a new `floors/{floorId}` document.
- Tapping a floor card navigates to Floor Dashboard.

### 5.2 Floor dashboard
- Top app bar: back button, floor name.
- A `Box` showing the floor plan background image with device pins overlaid using `positionX`/`positionY` as fractional offsets within the box's measured size.
- Each pin is a small colored icon button, color-coded by `DeviceState` (green=ON, amber=ON+scheduled/timed, red=ERROR, gray=DISCONNECTED, using Material 3 `ColorScheme` roles — do not hardcode hex values).
- Below the grid, a scrollable list of the same devices as compact rows (icon, name, state badge) for accessibility and for devices that overlap visually on the grid.
- Tapping a pin or row navigates to that device's detail screen (route depends on device type).

### 5.3 Device detail screens
Route to a different Composable per device type, all sharing a common header (device name, floor name, current state badge):

- **Outlet detail**: single large ON/OFF toggle switch. Toggling writes `state` to Firestore immediately (optimistic UI update, rolled back if the write fails).
- **Multi-switch detail**: a list of sub-switch rows, each with its own toggle. Toggling one sub-switch updates only that entry inside the `switches` array in Firestore (use `FieldValue.arrayUnion`/`arrayRemove` replacement pattern, or read-modify-write the array — read-modify-write is acceptable here since contention is low).
- **Iron detail** (and template for any future safety-critical device): shows a live countdown (`maxOnDurationMin` minus elapsed time since `turnedOnAt`, recomputed every second on a `Timer`/`delay` loop) when ON. An editable slider or number field for `maxOnDurationMin` (5–120 minutes). A prominent "turn off now" button. When the device is ON, poll/derive the countdown client-side only for display — the authoritative cutoff is enforced server-side (Cloud Function, out of scope for this app but assume `state` may flip to OFF externally at any time, so the UI must react to that via the realtime listener, not just local timer expiry).
- **Bulb detail**: ON/OFF toggle, plus a schedule section: enable/disable switch, start-time and end-time pickers (`TimePicker` from Material 3). Saves `scheduleEnabled`, `scheduleStart`, `scheduleEnd`.
- **Camera detail**: displays the mock snapshot image from `cameraUri` (Coil `AsyncImage`), with a manual "refresh snapshot" button that re-triggers image load (cache-bust with a timestamp query param).

### 5.4 Alerts & usage
- Top section: scrollable list of `alerts` for the current house, most recent first, each as a card (danger-colored, device name, message, relative timestamp). Tapping marks `acknowledged: true`.
- Bottom section: a simple bar chart (use a lightweight Compose canvas bar chart, no heavy charting library required) of total ON-minutes per device today, aggregated client-side from `usageLogs` for the current day.

## 6. Realtime sync requirements

- All device lists and detail screens must use Firestore snapshot listeners wrapped in `Flow` (via `callbackFlow { ... awaitClose { listener.remove() } }`), collected in the ViewModel and exposed as `StateFlow<UiState>` to Compose via `collectAsStateWithLifecycle()`.
- Writes (toggles, schedule edits, max-duration edits) are suspend functions on the repository using Firestore's coroutine-friendly APIs (`.await()` from `kotlinx-coroutines-play-services`).
- No manual "refresh" buttons for device state anywhere in the app — updates must appear automatically within roughly one second of a Firestore write, whether that write came from this app or externally (e.g. the web simulator or the server-side safety cutoff).
- Handle offline/loading/error states explicitly in every screen's `UiState` — don't let the UI silently show stale or blank data.

## 7. Non-functional requirements

- Use Material 3 theming (dynamic color if available, sensible light/dark fallback otherwise) — no hardcoded colors in Composables.
- All strings in `strings.xml`, no hardcoded UI text in Composables.
- Handle configuration changes (rotation) without losing state — state lives in ViewModel, not in Composable-local `remember` for anything Firestore-backed.
- Basic input validation: max duration between 5–120 minutes, schedule end time must be after start time (or clearly support overnight schedules if you choose to allow it — state your choice).
- Include at least one unit test for a ViewModel (e.g. iron countdown calculation logic) and one Firestore repository mapping test.

## 8. Suggested build order (for iterative prompting to Gemini)

1. Project scaffold, Firebase setup (`google-services.json` placeholder), theme, navigation graph with empty screens.
2. Data layer: DTOs, domain models, `FirestoreRepository` with read flows for floors and devices.
3. Home screen (floor list) wired to real data.
4. Floor dashboard with grid pins.
5. Device detail screens, one type at a time: outlet → multiswitch → iron → bulb → camera.
6. Alerts & usage screen.
7. Polish: loading/error states, theming, input validation, tests.

Build and verify each numbered step compiles and runs before moving to the next — don't generate the whole app in one pass.
