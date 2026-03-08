# EV Charged Reminder — Application Plan

## Overview
A free, open-source (MIT) Android app that automatically detects when a user is charging their EV and notifies them when charging is estimated to be complete.

---

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material Design 3 |
| Min SDK | 26 (Android 8.0) |
| Architecture | MVVM + Clean Architecture |
| Local DB | Room |
| DI | Hilt |
| Background work | WorkManager + ForegroundService |
| Location | Google Play Services FusedLocationProvider |
| Networking | Retrofit + Moshi (for EV database / charger API lookups) |
| Maps | osmdroid (OSM tiles, no API key, free) |
| Navigation | Compose Navigation (type-safe) |
| Build | Gradle (Kotlin DSL), single-module to start |

---

## Data Model

### Car
| Field | Type | Notes |
|---|---|---|
| id | Long (PK) | Auto-generated |
| year | Int | |
| make | String | |
| model | String | |
| trim | String? | Optional |
| isHybrid | Boolean | Affects default charge assumptions |
| batteryCapacityKwh | Double | Max usable battery capacity |
| maxAcceptRateKw | Double? | Max AC charge rate the onboard charger accepts; null = estimate from battery size |
| defaultStartPct | Int | Default: 20% (EV) / 0% (Hybrid) |
| defaultTargetPct | Int | Default: 80% (EV) / 100% (Hybrid) |
| isFavorite | Boolean | Exactly one car is favorite |
| createdAt | Instant | |

### Charger
| Field | Type | Notes |
|---|---|---|
| id | Long (PK) | Auto-generated |
| name | String | User-editable label (defaults to address) |
| latitude | Double | |
| longitude | Double | |
| radiusMeters | Int | Default 100 |
| maxChargingSpeedKw | Double | From API or user override |
| chargerType | Enum | See preset list below |
| notifyMinutesBefore | Int | Default 15 |
| createdAt | Instant | |

#### Charger Type Presets

Outlet-based presets use **NEC 80% continuous load derating** (amps and power
reflect the derated value, not the circuit rating). EVSE presets show their
rated output, which already accounts for circuit sizing.

| Label | Category | Voltage | Continuous Amps | Power (kW) |
|---|---|---|---|---|
| Standard Household Outlet (15A circuit) | AC | 120 | 12 | 1.4 |
| 120V / 20A Outlet | AC | 120 | 16 | 1.9 |
| 240V / 20A Outlet | AC | 240 | 16 | 3.8 |
| 240V / 30A Outlet | AC | 240 | 24 | 5.8 |
| 240V / 50A Outlet | AC | 240 | 40 | 9.6 |
| Level 2 EVSE (32A) | AC | 240 | 32 | 7.7 |
| Level 2 EVSE (48A) | AC | 240 | 48 | 11.5 |
| Level 2 EVSE (80A) | AC | 240 | 80 | 19.2 |
| DC Fast Charger (50 kW) | DC | — | — | 50.0 |
| DC Fast Charger (150 kW) | DC | — | — | 150.0 |
| DC Fast Charger (350 kW) | DC | — | — | 350.0 |
| Custom | User picks | — | — | User-entered |

### ChargingSession
| Field | Type | Notes |
|---|---|---|
| id | Long (PK) | Auto-generated |
| carId | Long (FK) | |
| chargerId | Long (FK) | |
| startPct | Int | Defaults from car, user can override |
| targetPct | Int | Defaults from car, user can override |
| startedAt | Instant | When session was detected |
| estimatedEndAt | Instant | Calculated, updated dynamically |
| actualEndAt | Instant? | Null while active |
| endReason | Enum | USER_LEFT, TARGET_REACHED, MANUAL |
| notificationsSent | Int | Track how many of the 3 alerts sent |

---

## Charging Curve Model

The charging curve depends on the **charger category**. Each charger type preset
maps to one of two categories:

| Category | Charger Types |
|---|---|
| **AC** (Level 1 & Level 2) | All outlet-based and Level 2 EVSE presets |
| **DC** (Fast Charging) | All DC Fast Charger presets |
| Custom | User specifies AC or DC when selecting Custom |

### AC Charging (Level 1 & Level 2)

AC charging is bottlenecked by the car's onboard charger, which draws at a
**constant rate** regardless of battery SOC. The effective rate is simply
`min(chargerMaxKw, carMaxAcceptRateKw)` applied uniformly from start to target.

```
time_hours = (target_kwh - start_kwh) / effective_rate
```

### DC Fast Charging

DC charging bypasses the onboard charger and feeds the battery directly, so the
battery's state of charge affects the rate. We approximate this with a
**piecewise model**:

```
0%–20%:   ~85% of max rate (battery warm-up / low SOC ramp)
20%–80%:  ~100% of max rate (optimal window)
80%–90%:  ~50% of max rate (taper begins)
90%–100%: ~20% of max rate (heavy taper)
```

The effective rate for each segment is `min(chargerMaxKw, carMaxAcceptRateKw)`,
then multiplied by the segment's rate factor.

**Time estimate formula**: For each segment the session passes through:
```
time_hours = segment_kwh / (effective_rate * segment_rate_factor)
```
Sum all segments to get total estimated time.

### Common

- `carMaxAcceptRateKw` is derived from battery size heuristic or can be
  user-overridden in the future.
- Recalculate whenever user overrides start/target percentages.

---

## Background Location Monitoring

### Strategy: Two-Tier Model
Use a **non-intrusive background approach** for passive monitoring, upgrading to a
foreground service only when a charging session is active or likely.

#### Tier 1 — Passive Monitoring (no foreground service)
- Use **Geofencing API** to register geofences around all saved charger locations,
  with both ENTER and DWELL transitions (2-minute loitering delay).
- When a geofence event fires, start a **short-lived WorkManager task** to confirm
  proximity and immediately start a charging session.
- Geofences are automatically re-registered when chargers are added, updated, or deleted,
  and on device boot.
- No persistent notification — completely invisible to the user.

#### Tier 2 — Active Session (foreground service)
- When a charging session starts, promote to a **foreground service** with a notification
  showing charging status, progress, and ETA.
- The foreground service polls location at **40-second intervals** for accurate session
  tracking and auto-end detection.
- The foreground service stops (drops back to Tier 1) when the session ends.

### Adaptive Polling
| State | Poll interval | Method |
|---|---|---|
| Outside all geofences | No polling | Geofence API triggers on entry |
| Session active (Tier 2) | 40 seconds | Foreground service |

### Session Detection Logic
```
1. Geofence ENTER or DWELL event fires
2. WorkManager task confirms proximity to charger
3. Start charging session immediately (use favorite car):
   → Promote to foreground service
   → Show "Charging started" notification with progress
   → At most one active session per car
4. While session is active (foreground service):
   → Poll location every 40 sec
   → Recalculate estimated end time
5. End session when:
   a. User leaves the geofence and re-enters → endReason=USER_LEFT
      (assumes they returned to unplug their car)
   b. Estimated charge target reached → endReason=TARGET_REACHED
   c. User manually ends → endReason=MANUAL
   Note: if the user stays away, the session continues until
   TARGET_REACHED or MANUAL end. The session only ends on
   re-entry, not on departure alone.
6. On session end → stop foreground service, return to Tier 1
   → 15-minute cooldown prevents auto-restart for that charger
   → User can override cooldown with "Start Charging Anyway"
```

### Notification Schedule (near completion)
When estimated time remaining ≤ `notifyMinutesBefore` (default 15 min):
1. **First notification**: at the threshold (e.g., 15 min before)
2. **Second notification**: 5 minutes later (e.g., 10 min before)
3. **Third notification**: 5 minutes after that (e.g., 5 min before)
4. No further notifications.

---

## External APIs

### EV Database (battery capacity)
- Ship a bundled JSON/Kotlin map of popular EV models with battery capacities
- User can always override manually when adding/editing a car

### Charger Location Info
- **OpenChargeMap API**: also provides charger/station info by lat/lng
- Query when user adds a charger to suggest max charging speed
- User can always override

### Reverse Geocoding (charger address label)
- **Nominatim API** (OpenStreetMap): `https://nominatim.openstreetmap.org/reverse`
- Free, no API key required
- Must set `User-Agent: EVChargedReminder/1.0`, limit 1 req/sec
- Used to auto-generate a human-readable name when adding a charger by GPS or map tap

### Map Tiles
- **osmdroid** with default OSM tile source (free, no API key)
- Requires OpenStreetMap attribution displayed on map

---

## Screen Flow

```
┌─────────────────────────────────────────────┐
│           FIRST LAUNCH / ONBOARDING         │
│                                             │
│  1. Welcome screen                          │
│  2. Add your first car                      │
│     - Year / Make / Model / Trim picker     │
│     - Battery capacity (auto-filled or      │
│       manual)                               │
│     - EV vs Hybrid toggle                   │
│     - Default start/target %                │
│  3. Add your first charger (optional, skip)  │
│     - Option A: "Add at current location"   │
│     - Option B: "Pick on map" (map view)    │
│     - Select charger type preset            │
│     - Override charging speed if needed     │
│  4. Grant location permission               │
│  5. Done → Main screen                      │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│              MAIN SCREEN (HOME)             │
│                                             │
│  Current status:                            │
│    - "Not charging" / "Charging at [name]"  │
│    - If charging: progress bar, ETA,        │
│      start/target % (editable)              │
│                                             │
│  Bottom Nav:                                │
│    🏠 Home | 🚗 Cars | ⚡ Chargers | 📊 History │
└─────────────────────────────────────────────┘

┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  CARS LIST   │  │ CHARGERS LIST│  │   HISTORY    │
│              │  │              │  │              │
│ + Add car    │  │ + Add charger│  │ Session list  │
│ ★ Favorite   │  │  (GPS or map)│  │ (up to 1 yr) │
│ Edit/Delete  │  │ 🗺 Map view  │  │ Filter by    │
│ Per-car      │  │ Edit/Delete  │  │  car/charger │
│  settings    │  │ Per-charger  │  │              │
│              │  │  settings    │  │              │
└──────────────┘  └──────────────┘  └──────────────┘
```

---

## Permissions
- `ACCESS_FINE_LOCATION` — for GPS-based charger detection
- `ACCESS_BACKGROUND_LOCATION` — for monitoring when app is not in foreground
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_LOCATION` — for persistent monitoring
- `POST_NOTIFICATIONS` — for charge notifications (API 33+)

---

## Project Structure

```
app/src/main/java/com/evchargedreminder/
├── di/                          # Hilt modules
│   ├── AppModule.kt
│   └── DatabaseModule.kt
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   ├── CarDao.kt
│   │   │   ├── ChargerDao.kt
│   │   │   └── ChargingSessionDao.kt
│   │   └── entity/
│   │       ├── CarEntity.kt
│   │       ├── ChargerEntity.kt
│   │       └── ChargingSessionEntity.kt
│   ├── remote/
│   │   ├── OpenChargeMapApi.kt
│   │   └── dto/
│   │       └── ChargePointDto.kt
│   ├── repository/
│   │   ├── CarRepository.kt
│   │   ├── ChargerRepository.kt
│   │   └── ChargingSessionRepository.kt
│   └── bundled/
│       └── BundledEvData.kt       # Fallback EV battery data
├── domain/
│   ├── model/
│   │   ├── Car.kt
│   │   ├── Charger.kt
│   │   ├── ChargerType.kt
│   │   ├── ChargingSession.kt
│   │   └── SessionEndReason.kt
│   └── usecase/
│       ├── EstimateChargingTimeUseCase.kt
│       ├── DetectChargingSessionUseCase.kt
│       └── ManageSessionUseCase.kt
├── service/
│   ├── LocationMonitorService.kt  # Foreground service
│   ├── LocationTracker.kt
│   └── ChargingNotificationManager.kt
├── ui/
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   ├── navigation/
│   │   └── AppNavGraph.kt
│   ├── onboarding/
│   │   ├── OnboardingScreen.kt
│   │   └── OnboardingViewModel.kt
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── cars/
│   │   ├── CarListScreen.kt
│   │   ├── CarEditScreen.kt
│   │   └── CarsViewModel.kt
│   ├── chargers/
│   │   ├── ChargerListScreen.kt
│   │   ├── ChargerEditScreen.kt
│   │   └── ChargersViewModel.kt
│   └── history/
│       ├── HistoryScreen.kt
│       └── HistoryViewModel.kt
└── util/
    ├── ChargingCurve.kt           # Piecewise charging model
    └── DistanceUtils.kt

app/src/test/java/com/evchargedreminder/     # Unit tests (JVM, no device)
├── domain/
│   └── usecase/
│       ├── EstimateChargingTimeUseCaseTest.kt
│       ├── DetectChargingSessionUseCaseTest.kt
│       └── ManageSessionUseCaseTest.kt
├── data/
│   ├── repository/
│   │   ├── CarRepositoryTest.kt
│   │   ├── ChargerRepositoryTest.kt
│   │   └── ChargingSessionRepositoryTest.kt
│   └── bundled/
│       └── BundledEvDataTest.kt
├── ui/
│   ├── onboarding/
│   │   └── OnboardingViewModelTest.kt
│   ├── home/
│   │   └── HomeViewModelTest.kt
│   ├── cars/
│   │   └── CarsViewModelTest.kt
│   ├── chargers/
│   │   └── ChargersViewModelTest.kt
│   └── history/
│       └── HistoryViewModelTest.kt
└── util/
    ├── ChargingCurveTest.kt
    └── DistanceUtilsTest.kt
```

### Testing Strategy
- **Unit tests** (JVM, `src/test/`) for all pure logic: charging curve math,
  distance calculations, use cases, repository logic (with fake DAOs),
  and ViewModel state/logic (with fake repositories).
- Network calls, location services, and notification APIs are behind
  interfaces — tests use fakes/mocks, no real I/O.
- **No instrumented tests** initially — add later if needed for Room DAOs
  or Compose UI tests.

---

## Implementation Phases

### Phase 1 — Foundation
- Project setup (Gradle, Hilt, Room, Compose, test dependencies)
- Data layer: entities, DAOs, database
- Domain models and repository interfaces
- Material 3 theme
- Tests: BundledEvDataTest, DistanceUtilsTest

### Phase 2 — Car Management
- Car add/edit/delete screens
- Year/Make/Model picker (bundled data)
- Favorite car logic
- Battery capacity auto-fill
- Tests: CarRepositoryTest, CarsViewModelTest

### Phase 3 — Charger Management
- Add charger at current GPS location + map view
- Charger type presets
- OpenChargeMap API integration
- Charger edit/delete screens
- Tests: ChargerRepositoryTest, ChargersViewModelTest

### Phase 4 — Location Monitoring & Session Detection
- Geofencing (Tier 1) + foreground service (Tier 2)
- Adaptive polling logic
- Session auto-start on geofence entry
- Session auto-end logic
- Tests: DetectChargingSessionUseCaseTest, ManageSessionUseCaseTest

### Phase 5 — Charging Estimation & Notifications
- Piecewise charging curve calculator
- Dynamic ETA updates
- 3-notification schedule before completion
- Notification tap → override charge percentages
- Tests: ChargingCurveTest, EstimateChargingTimeUseCaseTest

### Phase 6 — History & Polish
- Session history list with filtering
- Auto-cleanup of sessions older than 1 year
- Onboarding flow
- Permission request flow
- Edge cases and error handling
- Tests: ChargingSessionRepositoryTest, HistoryViewModelTest,
  OnboardingViewModelTest, HomeViewModelTest

---

## Bundled EV Data (Sample)

Ship with a JSON/Kotlin map of popular EVs:

```
Tesla Model 3 Standard Range (2024): 57.5 kWh
Tesla Model 3 Long Range (2024): 75 kWh
Tesla Model Y Long Range (2024): 75 kWh
Tesla Model S (2024): 100 kWh
Chevrolet Bolt EV (2023): 65 kWh
Chevrolet Equinox EV (2024): 85 kWh
Ford Mustang Mach-E Standard (2024): 72 kWh
Ford F-150 Lightning (2024): 98 kWh
Hyundai Ioniq 5 Long Range (2024): 77.4 kWh
Hyundai Ioniq 6 Long Range (2024): 77.4 kWh
Kia EV6 Long Range (2024): 77.4 kWh
Nissan Leaf (2024): 40 kWh
Nissan Ariya (2024): 87 kWh
Rivian R1T Large Pack (2024): 135 kWh
Rivian R1S Large Pack (2024): 135 kWh
BMW iX xDrive50 (2024): 76.6 kWh
Mercedes EQS 450+ (2024): 108.4 kWh
Volkswagen ID.4 Pro S (2024): 82 kWh
Polestar 2 Long Range (2024): 78 kWh
Toyota bZ4X (2024): 71.4 kWh
Toyota Prius Prime (2024, Hybrid): 13.6 kWh
Chevrolet Volt (2019, Hybrid): 18.4 kWh
BMW i3 REx (2021, Hybrid): 42.2 kWh
Hyundai Tucson PHEV (2024, Hybrid): 13.8 kWh
Jeep Wrangler 4xe (2024, Hybrid): 17.3 kWh
```

---

## License
MIT
