package com.example.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun KeystoreApp(viewModel: KeystoreViewModel, onLockRequest: () -> Unit) {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToGenerator = { navController.navigate("generator") },
                onNavigateToDetail = { id -> navController.navigate("detail/$id") },
                onNavigateToTools = { navController.navigate("tools") },
                onLockRequest = onLockRequest
            )
        }
        
        composable("generator") {
            GeneratorScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onGenerationSuccess = { id ->
                    // Navigate to details screen of the newly generated keystore
                    navController.popBackStack()
                    navController.navigate("detail/$id")
                }
            )
        }
        
        composable("tools") {
            ToolsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            DetailScreen(
                id = id,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
