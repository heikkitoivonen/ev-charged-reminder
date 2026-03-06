package com.evchargedreminder.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evchargedreminder.data.repository.CarRepository
import com.evchargedreminder.data.repository.ChargerRepository
import com.evchargedreminder.data.repository.ChargingSessionRepository
import com.evchargedreminder.domain.model.Car
import com.evchargedreminder.domain.model.Charger
import com.evchargedreminder.domain.model.ChargingSession
import com.evchargedreminder.domain.model.SessionEndReason
import com.evchargedreminder.domain.usecase.DetectChargingSessionUseCase
import com.evchargedreminder.domain.usecase.EstimateChargingTimeUseCase
import com.evchargedreminder.domain.usecase.ManageSessionUseCase
import com.evchargedreminder.domain.usecase.NearbyCharger
import com.evchargedreminder.service.SessionServiceLauncher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val activeSession: ChargingSession? = null,
    val car: Car? = null,
    val charger: Charger? = null,
    val estimatedMinutesRemaining: Long = 0,
    val progressPercent: Float = 0f,
    val isEditing: Boolean = false,
    val editStartPct: Int = 20,
    val editTargetPct: Int = 80,
    val showOverrideControls: Boolean = false,
    val nearbyChargers: List<NearbyCharger> = emptyList(),
    val suppressedChargerIds: Set<Long> = emptySet(),
    val isStartingSession: Boolean = false,
    val autoStartChargerId: Long? = null,
    val autoStartCountdownSeconds: Long = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val manageSession: ManageSessionUseCase,
    private val carRepository: CarRepository,
    private val chargerRepository: ChargerRepository,
    private val chargingSessionRepository: ChargingSessionRepository,
    private val estimateUseCase: EstimateChargingTimeUseCase,
    private val detectUseCase: DetectChargingSessionUseCase,
    private val nearbyTracker: NearbyChargerTracker,
    private val sessionServiceLauncher: SessionServiceLauncher
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var countdownJob: Job? = null

    companion object {
        private const val REFRESH_INTERVAL_MS = 30_000L // 30 seconds
        private const val AUTO_START_DWELL_SECONDS = 120L // 2 minutes
    }

    init {
        startRefreshing()
        startCountdownTicker()
    }

    private fun startRefreshing() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                refreshSession()
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    private fun startCountdownTicker() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                updateCountdown()
            }
        }
    }

    private fun updateCountdown() {
        val state = _uiState.value
        if (state.activeSession != null || state.nearbyChargers.isEmpty() || state.isStartingSession) return

        val target = state.nearbyChargers.firstOrNull { it.charger.id !in state.suppressedChargerIds }
            ?: return
        val firstSeen = nearbyTracker.getFirstSeen(target.charger.id) ?: return
        val elapsed = Duration.between(firstSeen, Instant.now()).seconds
        val countdown = (AUTO_START_DWELL_SECONDS - elapsed).coerceAtLeast(0)

        _uiState.update {
            it.copy(
                autoStartChargerId = target.charger.id,
                autoStartCountdownSeconds = countdown
            )
        }

        if (countdown == 0L) {
            startSessionWithService(target.charger)
        }
    }

    private suspend fun refreshSession() {
        val session = manageSession.getActiveSession()
        val nearby = try { detectUseCase.checkAllNearby() } catch (_: Exception) { emptyList() }

        // Track first-seen times via singleton (survives ViewModel recreation)
        nearbyTracker.update(nearby.map { it.charger.id }.toSet())

        // Compute auto-start countdown for closest unsuppressed charger (only when no active session)
        val suppressedIds = _uiState.value.suppressedChargerIds
        val autoStartTarget = if (session == null) {
            nearby.firstOrNull { it.charger.id !in suppressedIds }
        } else null
        val autoStartChargerId = autoStartTarget?.charger?.id
        val autoStartCountdown = if (autoStartTarget != null) {
            val firstSeen = nearbyTracker.getFirstSeen(autoStartTarget.charger.id) ?: Instant.now()
            val elapsed = Duration.between(firstSeen, Instant.now()).seconds
            (AUTO_START_DWELL_SECONDS - elapsed).coerceAtLeast(0)
        } else 0L

        if (session == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    activeSession = null,
                    car = null,
                    charger = null,
                    estimatedMinutesRemaining = 0,
                    progressPercent = 0f,
                    nearbyChargers = nearby,
                    autoStartChargerId = autoStartChargerId,
                    autoStartCountdownSeconds = autoStartCountdown
                )
            }
            return
        }

        val car = carRepository.getById(session.carId)
        val charger = chargerRepository.getById(session.chargerId)
        val minutesRemaining = manageSession.getEstimatedMinutesRemaining(session)
        val progress = calculateProgress(session, minutesRemaining, car)

        _uiState.update {
            it.copy(
                isLoading = false,
                activeSession = session,
                car = car,
                charger = charger,
                estimatedMinutesRemaining = minutesRemaining,
                progressPercent = progress,
                nearbyChargers = nearby,
                autoStartChargerId = null,
                autoStartCountdownSeconds = 0
            )
        }
    }

    private fun calculateProgress(
        session: ChargingSession,
        minutesRemaining: Long,
        car: Car?
    ): Float {
        if (car == null) return 0f
        val totalMinutes = estimateUseCase.estimateWithData(
            car,
            // Use a minimal charger object just for calculation — we just need the session data
            _uiState.value.charger ?: return 0f,
            session.startPct,
            session.targetPct
        )
        if (totalMinutes <= 0) return 1f
        val elapsed = totalMinutes - minutesRemaining
        return (elapsed.toFloat() / totalMinutes).coerceIn(0f, 1f)
    }

    fun showOverrideControls(show: Boolean) {
        val session = _uiState.value.activeSession ?: return
        _uiState.update {
            it.copy(
                showOverrideControls = show,
                isEditing = show,
                editStartPct = session.startPct,
                editTargetPct = session.targetPct
            )
        }
    }

    fun updateEditStartPct(pct: Int) {
        _uiState.update { it.copy(editStartPct = pct) }
    }

    fun updateEditTargetPct(pct: Int) {
        _uiState.update { it.copy(editTargetPct = pct) }
    }

    fun applyOverride() {
        val session = _uiState.value.activeSession ?: return
        viewModelScope.launch {
            manageSession.updateEstimatedEndTime(
                sessionId = session.id,
                newStartPct = _uiState.value.editStartPct,
                newTargetPct = _uiState.value.editTargetPct
            )
            _uiState.update { it.copy(isEditing = false, showOverrideControls = false) }
            refreshSession()
        }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false, showOverrideControls = false) }
    }

    fun endSession() {
        val session = _uiState.value.activeSession ?: return
        viewModelScope.launch {
            manageSession.endSession(session.id, SessionEndReason.MANUAL)
            nearbyTracker.clear()
            refreshSession()
        }
    }

    fun manualStartSession(chargerId: Long) {
        val charger = _uiState.value.nearbyChargers.find { it.charger.id == chargerId }?.charger
            ?: return
        startSessionWithService(charger)
    }

    private fun startSessionWithService(charger: Charger) {
        _uiState.update { it.copy(isStartingSession = true) }
        viewModelScope.launch {
            val session = detectUseCase.startSession(charger)
            if (session != null) {
                sessionServiceLauncher.startSession(session.id)
            }
            refreshSession()
            _uiState.update { it.copy(isStartingSession = false) }
        }
    }

    fun suppressAutoStart(chargerId: Long) {
        _uiState.update {
            it.copy(suppressedChargerIds = it.suppressedChargerIds + chargerId)
        }
    }

    fun unsuppressAutoStart(chargerId: Long) {
        _uiState.update {
            it.copy(suppressedChargerIds = it.suppressedChargerIds - chargerId)
        }
    }

    /** Visible for testing — cancels the periodic refresh and countdown loops. */
    fun stopRefreshing() {
        refreshJob?.cancel()
        countdownJob?.cancel()
    }

    override fun onCleared() {
        stopRefreshing()
        super.onCleared()
    }
}
