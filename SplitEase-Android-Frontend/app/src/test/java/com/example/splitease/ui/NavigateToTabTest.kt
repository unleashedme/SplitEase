package com.example.splitease.ui

import android.app.Application
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.example.splitease.ui.screens.ActivityDestination
import com.example.splitease.ui.screens.DashboardDestination
import com.example.splitease.ui.screens.GroupDestination
import com.example.splitease.ui.screens.ProfileDestination
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Bottom-bar navigation, using a real NavController under Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NavigateToTabTest {

    private val dashboard = DashboardDestination.route
    private val groups = GroupDestination.route
    private val activity = ActivityDestination.route
    private val profile = ProfileDestination.route

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        navController.graph = navController.createGraph(startDestination = dashboard) {
            composable(dashboard) {}
            composable(groups) {}
            composable(activity) {}
            composable(profile) {}
        }
    }

    private fun current() = navController.currentDestination?.route

    /** Screens on the back stack, bottom first (without the graph itself). */
    private fun backStack() = navController.currentBackStack.value.mapNotNull { it.destination.route }

    @Test
    fun dashboardTabReturnsToDashboard() {
        navController.navigateToTab(groups)
        navController.navigateToTab(dashboard)

        assertEquals(dashboard, current())
        assertEquals(listOf(dashboard), backStack())
    }

    @Test
    fun dashboardTabReturnsToDashboardWhenGroupsWasOpenedDirectly() {
        // Regression: Dashboard's "View all groups" used a plain navigate(), and tapping the
        // Dashboard tab afterwards restored the saved Groups stack instead of showing Dashboard
        navController.navigate(groups)
        navController.navigateToTab(dashboard)

        assertEquals(dashboard, current())
        assertEquals(listOf(dashboard), backStack())

        // ...and it keeps working on later visits
        navController.navigateToTab(activity)
        navController.navigateToTab(dashboard)
        assertEquals(dashboard, current())
    }

    @Test
    fun dashboardTabWorksAfterVisitingSeveralTabs() {
        navController.navigateToTab(groups)
        navController.navigateToTab(activity)
        navController.navigateToTab(profile)
        navController.navigateToTab(dashboard)

        assertEquals(dashboard, current())

        // And again, after the tabs have saved state
        navController.navigateToTab(activity)
        navController.navigateToTab(groups)
        navController.navigateToTab(dashboard)
        assertEquals(dashboard, current())
    }

    @Test
    fun otherTabsSitDirectlyOnTopOfDashboard() {
        navController.navigateToTab(groups)
        navController.navigateToTab(activity)

        assertEquals(activity, current())
        // So the system back button always goes to Dashboard
        assertEquals(listOf(dashboard, activity), backStack())
    }

    @Test
    fun tappingTheCurrentTabDoesNotStackDuplicates() {
        navController.navigateToTab(groups)
        navController.navigateToTab(groups)
        navController.navigateToTab(dashboard)
        navController.navigateToTab(dashboard)

        assertEquals(listOf(dashboard), backStack())
    }

    @Test
    fun everyTabIsReachableFromEveryOtherTab() {
        val tabs = listOf(dashboard, groups, activity, profile)
        for (from in tabs) {
            for (to in tabs) {
                navController.navigateToTab(from)
                navController.navigateToTab(to)
                assertEquals("$from -> $to", to, current())
            }
        }
    }
}
