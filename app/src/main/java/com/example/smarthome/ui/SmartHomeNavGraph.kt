package com.example.smarthome.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.smarthome.ui.alerts.AlertsUsageScreen
import com.example.smarthome.ui.dashboard.FloorDashboardScreen
import com.example.smarthome.ui.device.DeviceDetailScreen
import com.example.smarthome.ui.home.HomeScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object FloorDashboard : Screen("floor/{floorId}") {
        fun createRoute(floorId: String) = "floor/$floorId"
    }
    object DeviceDetail : Screen("floor/{floorId}/device/{deviceId}/{type}") {
        fun createRoute(floorId: String, deviceId: String, type: String) = "floor/$floorId/device/$deviceId/$type"
    }
    object Alerts : Screen("alerts")
}

@Composable
fun SmartHomeNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onFloorClick = { floorId ->
                    navController.navigate(Screen.FloorDashboard.createRoute(floorId))
                },
                onAlertsClick = {
                    navController.navigate(Screen.Alerts.route)
                }
            )
        }
        composable(
            route = Screen.FloorDashboard.route,
            arguments = listOf(navArgument("floorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val floorId = backStackEntry.arguments?.getString("floorId") ?: ""
            FloorDashboardScreen(
                floorId = floorId,
                onDeviceClick = { device ->
                    navController.navigate(Screen.DeviceDetail.createRoute(floorId, device.id, device.javaClass.simpleName.lowercase()))
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.DeviceDetail.route,
            arguments = listOf(
                navArgument("floorId") { type = NavType.StringType },
                navArgument("deviceId") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val floorId = backStackEntry.arguments?.getString("floorId") ?: ""
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            DeviceDetailScreen(
                floorId = floorId,
                deviceId = deviceId,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Alerts.route) {
            AlertsUsageScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
