package com.evchargedreminder.ui.chargers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    initialLat: Double,
    initialLng: Double,
    onLocationSelected: (Double, Double) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedLat by remember { mutableStateOf(if (initialLat != 0.0) initialLat else 39.8) }
    var selectedLng by remember { mutableStateOf(if (initialLng != 0.0) initialLng else -98.6) }
    var hasSelection by remember { mutableStateOf(initialLat != 0.0 && initialLng != 0.0) }

    // Configure osmdroid
    Configuration.getInstance().userAgentValue = "EVChargedReminder/1.0"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pick Location") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (hasSelection) {
                FloatingActionButton(onClick = { onLocationSelected(selectedLat, selectedLng) }) {
                    Icon(Icons.Default.Check, contentDescription = "Confirm location")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val mapView = remember { MapView(context) }

            DisposableEffect(Unit) {
                onDispose {
                    mapView.onDetach()
                }
            }

            AndroidView(
                factory = {
                    mapView.apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        controller.setCenter(GeoPoint(selectedLat, selectedLng))
                        overlays.add(CopyrightOverlay(context))

                        val marker = Marker(this).apply {
                            position = GeoPoint(selectedLat, selectedLng)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Charger location"
                            if (hasSelection) {
                                // Show marker at initial position
                            }
                        }
                        if (hasSelection) {
                            overlays.add(marker)
                        }

                        val mapViewRef = this
                        overlays.add(object : org.osmdroid.views.overlay.Overlay() {
                            override fun onSingleTapConfirmed(
                                e: android.view.MotionEvent?,
                                mapView: MapView?
                            ): Boolean {
                                if (e == null || mapView == null) return false
                                val projection = mapView.projection
                                val geoPoint = projection.fromPixels(
                                    e.x.toInt(), e.y.toInt()
                                ) as GeoPoint
                                selectedLat = geoPoint.latitude
                                selectedLng = geoPoint.longitude
                                hasSelection = true

                                // Update marker
                                mapViewRef.overlays.removeAll { it is Marker }
                                val newMarker = Marker(mapViewRef).apply {
                                    position = geoPoint
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = "Charger location"
                                }
                                mapViewRef.overlays.add(newMarker)
                                mapViewRef.invalidate()
                                return true
                            }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
