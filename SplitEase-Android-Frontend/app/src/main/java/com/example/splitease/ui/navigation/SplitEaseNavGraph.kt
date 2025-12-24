package com.example.splitease.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.splitease.ui.screens.ActivityDestination
import com.example.splitease.ui.screens.ActivityScreen
import com.example.splitease.ui.screens.DashboardDestination
import com.example.splitease.ui.screens.DashboardScreen
import com.example.splitease.ui.screens.ExpenseDetailsDestination
import com.example.splitease.ui.screens.ExpenseDetailsScreen
import com.example.splitease.ui.screens.GroupDestination
import com.example.splitease.ui.screens.GroupDetailsDestination
import com.example.splitease.ui.screens.GroupDetailsScreen
import com.example.splitease.ui.screens.GroupScreen
import com.example.splitease.ui.screens.LogInDestination
import com.example.splitease.ui.screens.LogInScreen
import com.example.splitease.ui.screens.ProfileDestination
import com.example.splitease.ui.screens.ProfileScreen
import com.example.splitease.ui.screens.RegisterDestination
import com.example.splitease.ui.screens.RegisterScreen

@Composable
fun SplitEaseNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = LogInDestination.route,
        modifier = modifier
    ){
        composable(route = LogInDestination.route){
            LogInScreen(navController = navController)
        }
        composable(route = RegisterDestination.route){
            RegisterScreen(navController = navController)
        }
        composable(route = DashboardDestination.route) {
            DashboardScreen(navController = navController)
        }
        composable(route = GroupDestination.route) {
            GroupScreen(navController = navController)
        }
        composable(route = ActivityDestination.route) {
            ActivityScreen(
                navController = navController,
                navigateToExpenseDetails = {navController.navigate("${ExpenseDetailsDestination.route}/$it")}
            )
        }
        composable(route = ProfileDestination.route) {
            ProfileScreen(
                navController = navController
            )
        }
        composable(
            route = ExpenseDetailsDestination.routeWithArgs,
            arguments = listOf(navArgument(ExpenseDetailsDestination.expenseIdArg){
                type = NavType.LongType
            })
        ) {
            ExpenseDetailsScreen(
                navigateBack = { navController.navigateUp()}
            )
        }
        composable(
            route = GroupDetailsDestination.routeWithArgs,
            arguments = listOf(navArgument(GroupDetailsDestination.GROUP_ID_ARG){
                type = NavType.LongType
            })
        ) {
            GroupDetailsScreen(
                navigateBack = { navController.navigateUp() }
            )
        }
    }
}
