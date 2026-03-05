package com.evchargedreminder.ui.cars

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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: CarEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
                title = { Text(if (state.isEditMode) "Edit Car" else "Add Car") },
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
            // Custom toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("My car isn't listed")
                Switch(
                    checked = state.isCustom,
                    onCheckedChange = { viewModel.setCustomMode(it) }
                )
            }

            if (state.isCustom) {
                // Custom mode: single name field
                OutlinedTextField(
                    value = state.make,
                    onValueChange = { viewModel.updateMake(it) },
                    label = { Text("Car Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else {
                // Standard mode: Year / Make / Model / Trim
                DropdownField(
                    label = "Year",
                    value = state.year.toString(),
                    options = (2026 downTo 2015).map { it.toString() },
                    onSelect = { viewModel.updateYear(it.toInt()) }
                )

                EditableDropdownField(
                    label = "Make",
                    value = state.make,
                    options = state.availableMakes,
                    onValueChange = { viewModel.updateMake(it) }
                )

                EditableDropdownField(
                    label = "Model",
                    value = state.model,
                    options = state.availableModels,
                    onValueChange = { viewModel.updateModel(it) }
                )

                if (state.availableTrims.isNotEmpty()) {
                    EditableDropdownField(
                        label = "Trim (optional)",
                        value = state.trim,
                        options = state.availableTrims,
                        onValueChange = { viewModel.updateTrim(it) }
                    )
                } else {
                    OutlinedTextField(
                        value = state.trim,
                        onValueChange = { viewModel.updateTrim(it) },
                        label = { Text("Trim (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // EV / Hybrid toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Plug-in Hybrid (PHEV)")
                Switch(
                    checked = state.isHybrid,
                    onCheckedChange = { viewModel.updateIsHybrid(it) }
                )
            }

            // Battery capacity
            OutlinedTextField(
                value = state.batteryCapacityKwh,
                onValueChange = { viewModel.updateBatteryCapacity(it) },
                label = { Text("Battery Capacity (kWh)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = if (state.autoFilled) {
                    { Text("Auto-filled from database") }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            // Max accept rate (optional)
            OutlinedTextField(
                value = state.maxAcceptRateKw,
                onValueChange = { viewModel.updateMaxAcceptRate(it) },
                label = { Text("Max AC Charge Rate (kW, optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Default start %
            Text("Default Start: ${state.defaultStartPct}%")
            Slider(
                value = state.defaultStartPct.toFloat(),
                onValueChange = { viewModel.updateDefaultStartPct(it.toInt()) },
                valueRange = 0f..100f,
                steps = 19
            )

            // Default target %
            Text("Default Target: ${state.defaultTargetPct}%")
            Slider(
                value = state.defaultTargetPct.toFloat(),
                onValueChange = { viewModel.updateDefaultTargetPct(it.toInt()) },
                valueRange = 0f..100f,
                steps = 19
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.saveCar() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                Text(if (state.isEditMode) "Save Changes" else "Add Car")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredOptions = if (value.isBlank()) options
        else options.filter { it.contains(value, ignoreCase = true) }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredOptions.isNotEmpty(),
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable)
        )
        if (filteredOptions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filteredOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
