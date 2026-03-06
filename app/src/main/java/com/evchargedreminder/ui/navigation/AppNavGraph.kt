package com.evchargedreminder.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.evchargedreminder.R
import com.evchargedreminder.ui.cars.CarEditScreen
import com.evchargedreminder.ui.cars.CarListScreen
import com.evchargedreminder.ui.chargers.ChargerEditScreen
import com.evchargedreminder.ui.chargers.ChargerListScreen
import com.evchargedreminder.ui.chargers.MapPickerScreen
import com.evchargedreminder.ui.history.HistoryScreen
import com.evchargedreminder.ui.home.HomeScreen
import com.evchargedreminder.ui.onboarding.OnboardingScreen
import com.evchargedreminder.ui.settings.LicenseScreen
import com.evchargedreminder.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable object OnboardingRoute
@Serializable object HomeRoute
@Serializable object CarListRoute
@Serializable data class CarEditRoute(val carId: Long = -1L)
@Serializable object ChargerListRoute
@Serializable data class ChargerEditRoute(val chargerId: Long = -1L)
@Serializable data class MapPickerRoute(val initialLat: Double = 0.0, val initialLng: Double = 0.0, val radiusMeters: Int = 100)
@Serializable object HistoryRoute
@Serializable object SettingsRoute
@Serializable object LicenseRoute

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any
)

@Composable
fun AppNavHost(showOverride: Boolean = false, onboardingCompleted: Boolean = true) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem("Home", Icons.Default.Home, HomeRoute),
        BottomNavItem("Cars", ImageVector.vectorResource(R.drawable.ic_directions_car), CarListRoute),
        BottomNavItem("Chargers", Icons.Default.Place, ChargerListRoute),
        BottomNavItem("History", Icons.Default.DateRange, HistoryRoute)
    )

    val isOnboarding = currentDestination?.hasRoute(OnboardingRoute::class) == true
    val showBottomBar = !isOnboarding && bottomNavItems.any { item ->
        currentDestination?.hasRoute(item.route::class) == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hasRoute(item.route::class) == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (onboardingCompleted) HomeRoute else OnboardingRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<OnboardingRoute> {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(HomeRoute) {
                            popUpTo(OnboardingRoute) { inclusive = true }
                        }
                    },
                    onAddCharger = {
                        navController.navigate(ChargerEditRoute())
                    }
                )
            }
            composable<HomeRoute> {
                HomeScreen(
                    showOverride = showOverride,
                    onNavigateToSettings = { navController.navigate(SettingsRoute) }
                )
            }
            composable<CarListRoute> {
                CarListScreen(
                    onAddCar = { navController.navigate(CarEditRoute()) },
                    onEditCar = { carId -> navController.navigate(CarEditRoute(carId)) }
                )
            }
            composable<CarEditRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<CarEditRoute>()
                CarEditScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<ChargerListRoute> {
                ChargerListScreen(
                    onAddCharger = { navController.navigate(ChargerEditRoute()) },
                    onEditCharger = { chargerId -> navController.navigate(ChargerEditRoute(chargerId)) }
                )
            }
            composable<ChargerEditRoute> { backStackEntry ->
                // Listen for map picker result
                val mapLat = backStackEntry.savedStateHandle
                    .getStateFlow("map_lat", Double.NaN).collectAsState()
                val mapLng = backStackEntry.savedStateHandle
                    .getStateFlow("map_lng", Double.NaN).collectAsState()

                ChargerEditScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPickOnMap = { lat, lng, radius ->
                        navController.navigate(MapPickerRoute(lat, lng, radius))
                    },
                    mapPickerLat = mapLat.value,
                    mapPickerLng = mapLng.value,
                    onMapResultConsumed = {
                        backStackEntry.savedStateHandle["map_lat"] = Double.NaN
                        backStackEntry.savedStateHandle["map_lng"] = Double.NaN
                    }
                )
            }
            composable<MapPickerRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<MapPickerRoute>()
                MapPickerScreen(
                    initialLat = route.initialLat,
                    initialLng = route.initialLng,
                    radiusMeters = route.radiusMeters,
                    onLocationSelected = { lat, lng ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("map_lat", lat)
                        navController.previousBackStackEntry?.savedStateHandle?.set("map_lng", lng)
                        navController.popBackStack()
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<HistoryRoute> {
                HistoryScreen()
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onViewLicense = { navController.navigate(LicenseRoute) }
                )
            }
            composable<LicenseRoute> {
                LicenseScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
