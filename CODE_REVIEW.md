# Code Review — EV Charged Reminder

## Overall Status
The project is in excellent shape, following the established `PLAN.md` and `AGENTS.md` closely. The architecture is clean, using Hilt for DI, Room for local storage, and MVVM with Jetpack Compose. Unit tests cover the core logic (charging curves, distance math, use cases).

## Findings

### 1. High Priority: Logic Bugs in Session Detection
*   **Poll Interval vs. Minutes:** In `LocationMonitorService.kt`, the variable `minutesAwayFromCharger` is incremented on every poll interval (40 seconds). However, it is passed to `ManageSessionUseCase.shouldEndByUserLeft`, which treats it as actual minutes.
    *   *Result:* Sessions end after 10 minutes of being away (15 * 40s = 600s) instead of the intended 15 minutes.
*   **Dwell Threshold:** In `DwellCheckWorker.kt`, `DWELL_THRESHOLD` is set to `2`. With 1-minute intervals, this results in a 1-minute dwell time from entry (Entry check at 0 min, 1st delay at 1 min).
    *   *Result:* The dwell time is 1 minute, but `PLAN.md` specifies 2 minutes.

### 2. Adherence to Design Principles (`AGENTS.md`)
*   **Privacy & Local-only:** Confirmed. No telemetry, no cloud accounts, no external tracking SDKs.
*   **API Constraints:**
    *   **Nominatim:** `User-Agent` is correctly set in `NominatimApi.kt`.
    *   **Rate Limiting:** Nominatim rate limiting (1 req/sec) is handled implicitly by user actions in `ChargerEditViewModel.kt`, but no global interceptor or limiter is implemented.
    *   **OSM Attribution:** `PLAN.md` and `AGENTS.md` require OSM attribution on all map views. `MapPickerScreen.kt` uses osmdroid's `MapView` but does not explicitly ensure attribution is visible/styled correctly.

### 3. Architecture & Style
*   **MVVM/Clean Architecture:** Well-implemented. Use cases are properly scoped and injected. Repository pattern is used consistently.
*   **Tech Stack:** Matches the plan (Material 3, Hilt, Room, Compose, osmdroid, Retrofit/Moshi).
*   **Single-Module:** Correct.

### 4. Areas for Improvement
*   **Error Handling:** Some network calls in `ChargerEditViewModel.kt` use empty catch blocks. While non-critical, adding minimal logging or user feedback (e.g., "Could not fetch address") would improve UX.
*   **Testing:** ViewModel tests for `HomeViewModel` and others are present, but adding more edge-case testing for the foreground service lifecycle would be beneficial.
*   **Strings:** Hardcoded strings in some UI components (`HomeScreen.kt`, `MapPickerScreen.kt`) should be moved to `strings.xml` for better maintainability and potential localization.

## Recommended Actions
1.  **Fix `minutesAwayFromCharger` naming and calculation** in `LocationMonitorService.kt`.
2.  **Increase `DWELL_THRESHOLD` to 3** in `DwellCheckWorker.kt` to ensure a true 2-minute dwell.
3.  **Add explicit OSM attribution text** to `MapPickerScreen.kt` to strictly comply with OSM license requirements.
4.  **Consider adding a basic rate-limiting interceptor** for Nominatim requests if more frequent usage is expected in the future.
