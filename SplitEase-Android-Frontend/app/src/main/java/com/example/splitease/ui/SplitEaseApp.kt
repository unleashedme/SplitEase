package com.example.splitease.ui


import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.splitease.R
import com.example.splitease.datastore.AuthStore
import com.example.splitease.ui.navigation.SplitEaseNavHost
import com.example.splitease.ui.screens.ActivityDestination
import com.example.splitease.ui.screens.DashboardDestination
import com.example.splitease.ui.screens.GroupDestination
import com.example.splitease.ui.screens.LogInDestination
import com.example.splitease.ui.screens.ProfileDestination


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitEaseRoot(
    navController: NavHostController = rememberNavController()
) {
    val authStore =
        (LocalContext.current.applicationContext as AppApplication)
            .authStore

    AuthGate(
        authStore = authStore,
        navController = navController
    )

    SplitEaseNavHost( navController = navController )
}

@Composable
fun AuthGate(
    authStore: AuthStore,
    navController: NavHostController
) {
    val token by authStore.tokenFlow.collectAsState(initial = null)

    LaunchedEffect(token) {
        if (token == null) {
            navController.navigate(LogInDestination.route) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            navController.navigate(DashboardDestination.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
}


sealed class BottomNavItem(
    val route: String,
    @DrawableRes val icon: Int,
    val label: String
) {
    object Dashboard : BottomNavItem(
        route = DashboardDestination.route,
        icon = R.drawable.home_96,
        label = "Dashboard"
    )

    object Groups : BottomNavItem(
        route = GroupDestination.route,
        icon = R.drawable.people_96,
        label = "Groups"
    )

    object Activities : BottomNavItem(
        route = ActivityDestination.route,
        icon = R.drawable.payment_96,
        label = "Activity"
    )

    object Profile : BottomNavItem(
        route = ProfileDestination.route,
        icon = R.drawable.profile_96,
        label = "My Profile"
    )
}

@Composable
fun SplitEaseBottomBar(
    navController: NavHostController = rememberNavController()
) {
    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Groups,
        BottomNavItem.Activities,
        BottomNavItem.Profile
    )

    val currentRoute =
        navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(DashboardDestination.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitEaseTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior? = null,
    modifier: Modifier = Modifier
){
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallPadding))
            ) {
                Icon(
                    painter = painterResource(R.drawable.splitease_logo_without_bottom_tag),
                    contentDescription = "SplitEase Logo",
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "SplitEase",
                    fontSize = 32.sp
                )
            }
        },
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
}

