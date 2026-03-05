package com.evchargedreminder.ui.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onAddCharger: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("EV Charged Reminder") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            when (state.step) {
                OnboardingStep.WELCOME -> WelcomeStep(onNext = { viewModel.nextStep() })
                OnboardingStep.ADD_CAR -> AddCarStep(state = state, viewModel = viewModel)
                OnboardingStep.PERMISSIONS -> PermissionsStep(
                    state = state,
                    viewModel = viewModel
                )
                OnboardingStep.ADD_CHARGER -> AddChargerStep(
                    onAddCharger = onAddCharger,
                    onSkip = { viewModel.skipCharger() }
                )
                OnboardingStep.DONE -> DoneStep(onComplete = onComplete)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            text = "EV Charged Reminder",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Never forget your car on the charger again. This app automatically detects when you're charging and reminds you when it's almost done.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("Get Started")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCarStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Text("Add Your Car", style = MaterialTheme.typography.headlineSmall)
    Text(
        "Tell us about your EV so we can estimate charging times.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // Custom toggle
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("My car isn't listed")
        Switch(checked = state.isCustom, onCheckedChange = { viewModel.setCustomMode(it) })
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

        if (state.make.isNotBlank()) {
            EditableDropdownField(
                label = "Model",
                value = state.model,
                options = state.availableModels,
                onValueChange = { viewModel.updateModel(it) }
            )
        }

        if (state.availableTrims.isNotEmpty()) {
            EditableDropdownField(
                label = "Trim",
                value = state.trim,
                options = state.availableTrims,
                onValueChange = { viewModel.updateTrim(it) }
            )
        }
    }

    // Battery capacity
    OutlinedTextField(
        value = state.batteryCapacityKwh,
        onValueChange = { viewModel.updateBatteryCapacity(it) },
        label = { Text("Battery Capacity (kWh)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        supportingText = if (state.autoFilled) {
            { Text("Auto-filled from database") }
        } else null
    )

    // Hybrid toggle
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Plug-in Hybrid (PHEV)")
        Switch(checked = state.isHybrid, onCheckedChange = { viewModel.updateIsHybrid(it) })
    }

    Button(
        onClick = { viewModel.saveCar() },
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.isSaving
    ) {
        Text(if (state.isSaving) "Saving..." else "Save & Continue")
    }
}

@Composable
private fun AddChargerStep(onAddCharger: () -> Unit, onSkip: () -> Unit) {
    Text("Add a Charger", style = MaterialTheme.typography.headlineSmall)
    Text(
        "Add your home charger or a charger you use frequently. You can always add more later.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(Modifier.height(16.dp))

    Button(onClick = onAddCharger, modifier = Modifier.fillMaxWidth()) {
        Text("Add a Charger")
    }

    OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
        Text("Skip for Now")
    }
}

@Composable
private fun PermissionsStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    val context = LocalContext.current

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onLocationPermissionResult(granted)
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onBackgroundLocationPermissionResult(granted)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    // Re-check background location permission when returning from Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val bgGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else true
                viewModel.onBackgroundLocationPermissionResult(bgGranted)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Text("Permissions", style = MaterialTheme.typography.headlineSmall)
    Text(
        "Location access lets the app detect when you arrive at a charger. Notifications alert you when charging is almost done.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(Modifier.height(16.dp))

    // Foreground location permission
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Location Access", style = MaterialTheme.typography.titleSmall)
            Text(
                if (state.locationGranted) "Granted" else "Required for charger detection",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!state.locationGranted) {
            Button(onClick = {
                locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }) {
                Text("Grant")
            }
        } else {
            Text("Granted", color = MaterialTheme.colorScheme.primary)
        }
    }

    // Background location permission (shown after foreground is granted)
    if (state.locationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Background Location", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (state.backgroundLocationGranted) "Granted"
                    else "Required for automatic charger detection",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!state.backgroundLocationGranted) {
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // API 30+: must send user to app settings
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } else {
                        // API 29: can request directly
                        backgroundLocationLauncher.launch(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
                    }
                }) {
                    Text(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "Open Settings" else "Grant")
                }
            } else {
                Text("Granted", color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    // Notification permission (API 33+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Notifications", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (state.notificationGranted) "Granted" else "For charging alerts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!state.notificationGranted) {
                Button(onClick = {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) {
                    Text("Grant")
                }
            } else {
                Text("Granted", color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    Button(
        onClick = { viewModel.completeOnboarding() },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Continue")
    }

    if (!state.locationGranted) {
        OutlinedButton(
            onClick = { viewModel.completeOnboarding() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip Permissions")
        }
    }
}

@Composable
private fun DoneStep(onComplete: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            text = "All Set!",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Your app is ready. When you park near a saved charger, we'll automatically track your charging session and notify you when it's almost done.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
            Text("Get Started")
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
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option); expanded = false }
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

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); expanded = true },
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable),
            singleLine = true
        )
        if (filteredOptions.isNotEmpty()) {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                filteredOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onValueChange(option); expanded = false }
                    )
                }
            }
        }
    }
}
