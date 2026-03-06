package com.evchargedreminder.ui.chargers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evchargedreminder.domain.model.ChargerType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargerEditScreen(
    onNavigateBack: () -> Unit,
    onPickOnMap: (Double, Double, Int) -> Unit,
    mapPickerLat: Double = Double.NaN,
    mapPickerLng: Double = Double.NaN,
    onMapResultConsumed: () -> Unit = {},
    viewModel: ChargerEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle map picker result
    LaunchedEffect(mapPickerLat, mapPickerLng) {
        if (!mapPickerLat.isNaN() && !mapPickerLng.isNaN()) {
            viewModel.setLocationFromMap(mapPickerLat, mapPickerLng)
            onMapResultConsumed()
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit Charger" else "Add Charger") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Location section
            Text("Location", style = MaterialTheme.typography.titleSmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isCurrentSource = state.locationSource == LocationSource.CURRENT_LOCATION
                val isMapSource = state.locationSource == LocationSource.MAP

                if (isCurrentSource) {
                    Button(
                        onClick = { viewModel.useCurrentLocation() },
                        enabled = !state.isLoadingLocation,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.isLoadingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        Text("Current Location")
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.useCurrentLocation() },
                        enabled = !state.isLoadingLocation,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (state.isLoadingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        Text("Current Location")
                    }
                }

                if (isMapSource) {
                    Button(
                        onClick = {
                            val lat = state.latitude.toDoubleOrNull() ?: 0.0
                            val lng = state.longitude.toDoubleOrNull() ?: 0.0
                            onPickOnMap(lat, lng, state.radiusMeters)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Pick on Map")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            val lat = state.latitude.toDoubleOrNull() ?: 0.0
                            val lng = state.longitude.toDoubleOrNull() ?: 0.0
                            onPickOnMap(lat, lng, state.radiusMeters)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Pick on Map")
                    }
                }
            }

            if (state.hasLocation) {
                Text(
                    text = "${state.latitude}, ${state.longitude}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "No location selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Name
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Name") },
                supportingText = if (state.isLoadingAddress) {
                    { Text("Loading address...") }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            // Charger Type
            ChargerTypeDropdown(
                selectedType = state.chargerType,
                onSelect = { viewModel.updateChargerType(it) }
            )

            // Custom power (only for custom types)
            if (state.chargerType == ChargerType.CUSTOM_AC || state.chargerType == ChargerType.CUSTOM_DC) {
                OutlinedTextField(
                    value = state.customPowerKw,
                    onValueChange = { viewModel.updateCustomPower(it) },
                    label = { Text("Custom Power (kW)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Max charging speed
            OutlinedTextField(
                value = state.maxChargingSpeedKw,
                onValueChange = { viewModel.updateMaxChargingSpeed(it) },
                label = { Text("Max Charging Speed (kW)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = if (state.suggestedSpeedKw != null) {
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Nearby charger: ${state.suggestedSpeedKw} kW")
                            TextButton(onClick = { viewModel.applySuggestedSpeed() }) {
                                Text("Apply")
                            }
                        }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            // Radius — display in locale-appropriate units
            val useImperial = remember {
                java.util.Locale.getDefault().country in setOf("US", "LR", "MM")
            }
            if (useImperial) {
                val feet = (state.radiusMeters * 3.28084).toInt()
                Text("Geofence Radius: ${feet}ft")
                Slider(
                    value = state.radiusMeters.toFloat(),
                    onValueChange = { viewModel.updateRadius(it.toInt()) },
                    valueRange = 25f..500f,
                    steps = 18
                )
            } else {
                Text("Geofence Radius: ${state.radiusMeters}m")
                Slider(
                    value = state.radiusMeters.toFloat(),
                    onValueChange = { viewModel.updateRadius(it.toInt()) },
                    valueRange = 25f..500f,
                    steps = 18
                )
            }

            // Notify minutes before
            Text("Notify Before: ${state.notifyMinutesBefore} min")
            Slider(
                value = state.notifyMinutesBefore.toFloat(),
                onValueChange = { viewModel.updateNotifyMinutesBefore(it.toInt()) },
                valueRange = 5f..60f,
                steps = 10
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.saveCharger() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                Text(if (state.isEditMode) "Save Changes" else "Add Charger")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChargerTypeDropdown(
    selectedType: ChargerType,
    onSelect: (ChargerType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedType.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Charger Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ChargerType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label) },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    }
                )
            }
        }
    }
}
