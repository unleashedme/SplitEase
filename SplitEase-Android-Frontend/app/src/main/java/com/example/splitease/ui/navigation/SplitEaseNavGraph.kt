package com.example.splitease.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import com.example.splitease.ui.viewmodel.ActivityViewModel

@Composable
fun SplitEaseNavHost(
    isLoggedIn:Boolean,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = if(!isLoggedIn) LogInDestination.route else DashboardDestination.route,
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
            val viewModel: ActivityViewModel = viewModel(factory = ActivityViewModel.Factory)
            ActivityScreen(
                activityViewModel = viewModel,
                navController = navController,
                onExpenseClick = { id ->
                    navController.navigate("${ExpenseDetailsDestination.route}/$id")
                }
            )
        }
        composable(route = ProfileDestination.route) {
            ProfileScreen(
                navController = navController
            )
        }
        composable(
            route = ExpenseDetailsDestination.routeWithArgs,
            arguments = listOf(navArgument(ExpenseDetailsDestination.EXPENSE_ID_ARG){
                type = NavType.StringType
            })
        ) { backStackEntry ->

            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(ActivityDestination.route)
            }

            val sharedViewModel: ActivityViewModel = viewModel(parentEntry)
            val expenseId = backStackEntry.arguments?.getString(ExpenseDetailsDestination.EXPENSE_ID_ARG)

            ExpenseDetailsScreen(
                expenseId = expenseId?:"",
                activityViewModel = sharedViewModel,
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
