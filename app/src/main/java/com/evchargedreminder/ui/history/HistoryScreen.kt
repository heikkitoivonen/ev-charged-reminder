package com.evchargedreminder.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evchargedreminder.domain.model.SessionEndReason
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("History") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterDropdown(
                    label = "Car",
                    selectedLabel = state.filterCarId?.let { id ->
                        state.cars.find { it.id == id }?.let { "${it.year} ${it.make} ${it.model}" }
                    } ?: "All Cars",
                    options = listOf(null to "All Cars") + state.cars.map {
                        it.id to "${it.year} ${it.make} ${it.model}"
                    },
                    onSelect = { viewModel.setCarFilter(it) },
                    modifier = Modifier.weight(1f)
                )
                FilterDropdown(
                    label = "Charger",
                    selectedLabel = state.filterChargerId?.let { id ->
                        state.chargers.find { it.id == id }?.name
                    } ?: "All Chargers",
                    options = listOf(null to "All Chargers") + state.chargers.map {
                        it.id to it.name
                    },
                    onSelect = { viewModel.setChargerFilter(it) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 32.dp)
                    )
                }
                state.sessions.isEmpty() -> {
                    Text(
                        text = "No charging sessions yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp)
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.sessions, key = { it.session.id }) { item ->
                            SessionCard(item)
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(item: HistoryItem) {
    val session = item.session
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a")
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.chargerName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = formatEndReason(session.endReason),
                    style = MaterialTheme.typography.labelSmall,
                    color = endReasonColor(session.endReason)
                )
            }

            Text(
                text = item.carName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = session.startedAt
                        .atZone(ZoneId.systemDefault())
                        .format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${session.startPct}% \u2192 ${session.targetPct}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = formatDuration(session),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    label: String,
    selectedLabel: String,
    options: List<Pair<Long?, String>>,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatEndReason(reason: SessionEndReason?): String = when (reason) {
    SessionEndReason.TARGET_REACHED -> "Target reached"
    SessionEndReason.USER_LEFT -> "User left"
    SessionEndReason.MANUAL -> "Manual"
    null -> "Active"
}

@Composable
private fun endReasonColor(reason: SessionEndReason?) = when (reason) {
    SessionEndReason.TARGET_REACHED -> MaterialTheme.colorScheme.primary
    SessionEndReason.USER_LEFT -> MaterialTheme.colorScheme.tertiary
    SessionEndReason.MANUAL -> MaterialTheme.colorScheme.onSurfaceVariant
    null -> MaterialTheme.colorScheme.error
}

private fun formatDuration(session: com.evchargedreminder.domain.model.ChargingSession): String {
    val end = session.actualEndAt ?: return "In progress"
    val duration = Duration.between(session.startedAt, end)
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
