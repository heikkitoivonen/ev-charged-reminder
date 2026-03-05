package com.evchargedreminder.ui.cars

import com.evchargedreminder.domain.model.displayName
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evchargedreminder.domain.model.Car

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarListScreen(
    onAddCar: () -> Unit,
    onEditCar: (Long) -> Unit,
    viewModel: CarsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var carToDelete by remember { mutableStateOf<Car?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Cars") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCar) {
                Icon(Icons.Default.Add, contentDescription = "Add car")
            }
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.cars.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No cars added yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + to add your first car", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(state.cars, key = { it.id }) { car ->
                    CarCard(
                        car = car,
                        onTap = { onEditCar(car.id) },
                        onFavorite = { viewModel.setFavorite(car.id) },
                        onDelete = { carToDelete = car }
                    )
                }
            }
        }
    }

    carToDelete?.let { car ->
        AlertDialog(
            onDismissRequest = { carToDelete = null },
            title = { Text("Delete car?") },
            text = { Text("Delete ${car.displayName}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCar(car)
                    carToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { carToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CarCard(
    car: Car,
    onTap: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildString {
                        append(car.displayName)
                        if (car.model.isNotBlank()) car.trim?.let { append(" $it") }
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${car.batteryCapacityKwh} kWh",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (car.isHybrid) {
                        Text(
                            text = "Hybrid",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Favorite",
                    tint = if (car.isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
