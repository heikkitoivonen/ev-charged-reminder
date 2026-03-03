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
| Label | Voltage | Amps | Power (kW) |
|---|---|---|---|
| Standard Household Outlet (120V/12A) | 120 | 12 | 1.4 |
| 120V / 20A Outlet | 120 | 20 | 2.4 |
| 240V / 20A (Dryer-style) | 240 | 20 | 4.8 |
| 240V / 30A Outlet | 240 | 30 | 7.2 |
| 240V / 50A Outlet | 240 | 50 | 12.0 |
| Level 2 EVSE (32A) | 240 | 32 | 7.7 |
| Level 2 EVSE (48A) | 240 | 48 | 11.5 |
| Level 2 EVSE (80A) | 240 | 80 | 19.2 |
| DC Fast Charger (50 kW) | — | — | 50.0 |
| DC Fast Charger (150 kW) | — | — | 150.0 |
| DC Fast Charger (350 kW) | — | — | 350.0 |
| Custom | — | — | User-entered |

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

We approximate real-world EV charging curves with a **piecewise model**:

```
0%–20%:   ~85% of max charger rate (battery warm-up / low SOC taper)
20%–80%:  ~100% of max charger rate (optimal window)
80%–90%:  ~50% of max charger rate (taper begins)
90%–100%: ~20% of max charger rate (heavy taper)
```

The effective charge rate is `min(chargerMaxKw, carMaxAcceptRateKw)` for each segment (car max accept rate is derived from battery size heuristic or can be user-overridden in the future).

**Time estimate formula**: For each segment the session passes through, calculate:
```
time_hours = (segment_kwh) / (effective_rate * segment_efficiency)
```
Sum all segments to get total estimated time. Recalculate whenever user overrides start/target percentages.

---

## Background Location Monitoring

### Strategy
Use a **foreground service** with a persistent notification ("EV Charged Reminder is monitoring your location") to ensure reliable location updates.

### Adaptive Polling Frequency
| Distance to nearest charger | Poll interval |
|---|---|
| > 10 km | 10 minutes |
| 1–10 km | 5 minutes |
| 100 m – 1 km | 2 minutes |
| < 100 m (in range) | 1 minute |

### Session Detection Logic
```
1. Poll location
2. If within 100m of a charger AND duration >= 3 minutes:
   → Start a charging session (use favorite car)
   → Show "Charging started" notification
3. While session is active:
   → Continue polling at 1-min interval
   → Recalculate estimated end time
4. End session when:
   a. User has been >100m away for >15 minutes → endReason=USER_LEFT
   b. Estimated charge target reached → endReason=TARGET_REACHED
   c. User manually ends → endReason=MANUAL
```

### Notification Schedule (near completion)
When estimated time remaining ≤ `notifyMinutesBefore` (default 15 min):
1. **First notification**: at the threshold (e.g., 15 min before)
2. **Second notification**: 5 minutes later (e.g., 10 min before)
3. **Third notification**: 5 minutes after that (e.g., 5 min before)
4. No further notifications.

---

## External APIs

### EV Database Lookup (battery capacity)
- **Primary**: [OpenChargeMap API](https://openchargemap.org/site/develop/api) — free, no key required for basic use
- **Fallback**: Ship a bundled JSON of ~50 popular EV models with battery capacities
- Endpoint: query by make/model/year → extract battery capacity

### Charger Location Info
- **OpenChargeMap API**: also provides charger/station info by lat/lng
- Query when user adds a charger to suggest max charging speed
- User can always override

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
│  3. Add your first charger                  │
│     - "Add charger at current location"     │
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
│ ★ Favorite   │  │ Edit/Delete  │  │ (up to 1 yr) │
│ Edit/Delete  │  │ Per-charger  │  │ Filter by    │
│ Per-car      │  │  settings    │  │  car/charger │
│  settings    │  │              │  │              │
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
```

---

## Implementation Phases

### Phase 1 — Foundation
- Project setup (Gradle, Hilt, Room, Compose)
- Data layer: entities, DAOs, database
- Domain models and repository interfaces
- Material 3 theme

### Phase 2 — Car Management
- Car add/edit/delete screens
- Year/Make/Model picker (bundled data + API lookup)
- Favorite car logic
- Battery capacity auto-fill

### Phase 3 — Charger Management
- Add charger at current GPS location
- Charger type presets
- OpenChargeMap API integration
- Charger edit/delete screens

### Phase 4 — Location Monitoring & Session Detection
- Foreground service with persistent notification
- Adaptive polling logic
- Geofence proximity detection
- Session auto-start after 3 min in range
- Session auto-end logic (left for 15 min / target reached)

### Phase 5 — Charging Estimation & Notifications
- Piecewise charging curve calculator
- Dynamic ETA updates
- 3-notification schedule before completion
- Notification tap → override charge percentages

### Phase 6 — History & Polish
- Session history list with filtering
- Auto-cleanup of sessions older than 1 year
- Onboarding flow
- Permission request flow
- Edge cases and error handling

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
