package com.evchargedreminder.ui.home

import com.evchargedreminder.domain.model.displayName
import com.evchargedreminder.domain.usecase.NearbyCharger
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    showOverride: Boolean = false,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(showOverride) {
        if (showOverride) {
            viewModel.showOverrideControls(true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("EV Charged Reminder") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                state.activeSession != null -> {
                    ChargingActiveContent(
                        state = state,
                        onEditClick = { viewModel.showOverrideControls(true) },
                        onStartPctChange = { viewModel.updateEditStartPct(it) },
                        onTargetPctChange = { viewModel.updateEditTargetPct(it) },
                        onApplyOverride = { viewModel.applyOverride() },
                        onCancelEdit = { viewModel.cancelEditing() },
                        onEndSession = { viewModel.endSession() }
                    )
                    NearbyChargersSection(
                        state = state,
                        onStartSession = { viewModel.manualStartSession(it) },
                        onSuppress = { viewModel.suppressAutoStart(it) },
                        onUnsuppress = { viewModel.unsuppressAutoStart(it) }
                    )
                }
                else -> {
                    NotChargingContent(
                        state = state,
                        onStartSession = { viewModel.manualStartSession(it) },
                        onSuppress = { viewModel.suppressAutoStart(it) },
                        onUnsuppress = { viewModel.unsuppressAutoStart(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotChargingContent(
    state: HomeUiState,
    onStartSession: (Long) -> Unit,
    onSuppress: (Long) -> Unit,
    onUnsuppress: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Not charging",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Charging sessions start automatically when you arrive at a saved charger location.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }

    NearbyChargersSection(
        state = state,
        onStartSession = onStartSession,
        onSuppress = onSuppress,
        onUnsuppress = onUnsuppress
    )
}

@Composable
private fun NearbyChargersSection(
    state: HomeUiState,
    onStartSession: (Long) -> Unit,
    onSuppress: (Long) -> Unit,
    onUnsuppress: (Long) -> Unit
) {
    // Filter out the charger that has an active session (already shown above)
    val chargersToShow = state.nearbyChargers.filter { nearby ->
        nearby.charger.id != state.activeSession?.chargerId
    }
    if (chargersToShow.isEmpty()) return

    Text(
        text = "Nearby Chargers",
        style = MaterialTheme.typography.titleMedium
    )

    val useImperial = remember {
        java.util.Locale.getDefault().country in setOf("US", "LR", "MM")
    }

    chargersToShow.forEach { nearby ->
        NearbyChargerCard(
            nearby = nearby,
            isSuppressed = nearby.charger.id in state.suppressedChargerIds,
            hasActiveSession = state.activeSession != null,
            isStarting = state.isStartingSession,
            autoStartCountdownSeconds = if (nearby.charger.id == state.autoStartChargerId)
                state.autoStartCountdownSeconds else null,
            useImperial = useImperial,
            onStartSession = onStartSession,
            onSuppress = onSuppress,
            onUnsuppress = onUnsuppress
        )
    }
}

@Composable
private fun NearbyChargerCard(
    nearby: NearbyCharger,
    isSuppressed: Boolean,
    hasActiveSession: Boolean,
    isStarting: Boolean,
    autoStartCountdownSeconds: Long?,
    useImperial: Boolean,
    onStartSession: (Long) -> Unit,
    onSuppress: (Long) -> Unit,
    onUnsuppress: (Long) -> Unit
) {
    val charger = nearby.charger
    val distanceText = if (useImperial) {
        "${(nearby.distanceMeters * 3.28084).toInt()}ft away"
    } else {
        "${nearby.distanceMeters.toInt()}m away"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = charger.name,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${charger.chargerType.label} \u2022 $distanceText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (autoStartCountdownSeconds != null && autoStartCountdownSeconds > 0 && !isSuppressed) {
                val minutes = autoStartCountdownSeconds / 60
                val seconds = autoStartCountdownSeconds % 60
                Text(
                    text = "Auto-starting in ${minutes}m ${seconds}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isSuppressed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-start suppressed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { onUnsuppress(charger.id) }) {
                        Text("Undo")
                    }
                }
            } else if (!hasActiveSession) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onStartSession(charger.id) },
                        enabled = !isStarting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Charging")
                    }
                    OutlinedButton(
                        onClick = { onSuppress(charger.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Don't Charge")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChargingActiveContent(
    state: HomeUiState,
    onEditClick: () -> Unit,
    onStartPctChange: (Int) -> Unit,
    onTargetPctChange: (Int) -> Unit,
    onApplyOverride: () -> Unit,
    onCancelEdit: () -> Unit,
    onEndSession: () -> Unit
) {
    val session = state.activeSession ?: return
    val chargerName = state.charger?.name ?: "Unknown charger"
    val carName = state.car?.displayName ?: "Unknown car"

    // Status card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Charging at $chargerName",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = carName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }

    // ETA card
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val timeText = formatTimeRemaining(state.estimatedMinutesRemaining)
            Text(
                text = timeText,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "remaining",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { state.progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${session.startPct}% \u2192 ${session.targetPct}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Percentage override section
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Charge Settings",
                    style = MaterialTheme.typography.titleSmall
                )
                if (!state.isEditing) {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit percentages"
                        )
                    }
                }
            }

            if (state.isEditing) {
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Start: ${state.editStartPct}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = state.editStartPct.toFloat(),
                    onValueChange = { onStartPctChange(it.toInt()) },
                    valueRange = 0f..100f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Target: ${state.editTargetPct}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = state.editTargetPct.toFloat(),
                    onValueChange = { onTargetPctChange(it.toInt()) },
                    valueRange = 0f..100f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onCancelEdit) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onApplyOverride,
                        enabled = state.editTargetPct > state.editStartPct
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }

    // End session button
    Button(
        onClick = onEndSession,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        )
    ) {
        Text("End Session")
    }

    Spacer(Modifier.height(8.dp))
}

private fun formatTimeRemaining(minutes: Long): String {
    return if (minutes >= 60) {
        val hours = minutes / 60
        val mins = minutes % 60
        if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
    } else {
        "${minutes}m"
    }
}
