package com.evchargedreminder.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object CarListRoute
@Serializable data class CarEditRoute(val carId: Long = -1L)
@Serializable object ChargerListRoute
@Serializable object HistoryRoute

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any
)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem("Home", Icons.Default.Home, HomeRoute),
        BottomNavItem("Cars", ImageVector.vectorResource(R.drawable.ic_directions_car), CarListRoute),
        BottomNavItem("Chargers", Icons.Default.Place, ChargerListRoute),
        BottomNavItem("History", Icons.Default.DateRange, HistoryRoute)
    )

    val showBottomBar = bottomNavItems.any { item ->
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
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<HomeRoute> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Not charging")
                }
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
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Chargers — coming soon")
                }
            }
            composable<HistoryRoute> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("History — coming soon")
                }
            }
        }
    }
}
