package com.example.splitease.viewmodel

import com.example.splitease.data.AppDataStore
import com.example.splitease.testutil.FakeRepository
import com.example.splitease.testutil.Fixtures
import com.example.splitease.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The Dashboard, Groups and Activity ViewModels read from the shared AppDataStore. */
@OptIn(ExperimentalCoroutinesApi::class)
class CachedScreenViewModelsTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeRepository()
    private val store = AppDataStore(repository)

    @After
    fun tearDown() = store.close()

    /** stateIn(WhileSubscribed) flows only update while collected, like on screen. */
    private fun <T> TestScope.observe(flow: StateFlow<T>): StateFlow<T> {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
        return flow
    }

    @Test
    fun openingEveryScreenLoadsDataOnlyOnce() = runTest(mainRule.dispatcher) {
        repository.payables = { listOf(Fixtures.payable("Ravi", 40.0)) }
        repository.myGroups = { listOf(Fixtures.userGroup("Trip")) }

        // Each screen creates its own ViewModels; switch between them a few times
        repeat(3) {
            DashboardViewModel(store)
            GroupViewModel(repository, store)
            ActivityViewModel(repository, store)
            SettlementViewModel(repository, store)
            GroupListViewModel(store)
            advanceUntilIdle()
        }

        assertEquals(1, repository.storeLoadCount())
        assertEquals(Fixtures.dashboardStat(), DashboardViewModel(store).dashboardUiState)
        assertEquals(Fixtures.groupScreenData(), GroupViewModel(repository, store).groupUiState)
        assertEquals(Fixtures.activitySummary(), ActivityViewModel(repository, store).activityUiState)
        assertEquals(listOf(Fixtures.payable("Ravi", 40.0)), SettlementViewModel(repository, store).settlements)
        assertEquals(listOf(Fixtures.userGroup("Trip")), GroupListViewModel(store).groupListUiState.groups)
    }

    @Test
    fun pullToRefreshReloadsAndShowsTheSpinner() = runTest(mainRule.dispatcher) {
        val dashboard = DashboardViewModel(store)
        advanceUntilIdle()
        repository.dashboardStat = { Fixtures.dashboardStat(total = 500.0) }

        dashboard.refresh()
        assertTrue(dashboard.isRefreshing)
        advanceUntilIdle()

        assertFalse(dashboard.isRefreshing)
        assertEquals(500.0, dashboard.dashboardUiState!!.totalUserExpense, 0.0)
        assertEquals(2, repository.storeLoadCount())

        // The Groups and Activity screens refresh the same shared data
        GroupViewModel(repository, store).refresh()
        advanceUntilIdle()
        ActivityViewModel(repository, store).refresh()
        advanceUntilIdle()
        assertEquals(4, repository.storeLoadCount())
    }

    @Test
    fun groupsCanBeSorted() = runTest(mainRule.dispatcher) {
        repository.groupScreenData = {
            Fixtures.groupScreenData(
                listOf(
                    Fixtures.group("Flat", memberCount = 4, total = 300.0, expenses = listOf(Fixtures.expense("2026-01-05T00:00:00Z"))),
                    Fixtures.group("Trip", memberCount = 2, total = 900.0, expenses = listOf(Fixtures.expense("2026-03-01T00:00:00Z"))),
                    Fixtures.group("Office", memberCount = 3, total = 50.0, expenses = listOf(Fixtures.expense("2026-02-10T00:00:00Z")))
                )
            )
        }
        store.loadIfNeeded()
        advanceUntilIdle()
        val viewModel = GroupViewModel(repository, store)
        val sorted = observe(viewModel.sortedGroupList)
        advanceUntilIdle()

        // The sorted list is recomputed on viewModelScope, so let it run first
        fun names(): List<String> {
            advanceUntilIdle()
            return sorted.value.map { it.groupName }
        }
        assertEquals(listOf("Trip", "Office", "Flat"), names()) // most recent expense first

        viewModel.updateSortOrder("Name(A-Z)")
        assertEquals(listOf("Flat", "Office", "Trip"), names())

        viewModel.updateSortOrder("Most Members")
        assertEquals(listOf("Flat", "Office", "Trip"), names())

        viewModel.updateSortOrder("Highest Expenses")
        assertEquals(listOf("Trip", "Flat", "Office"), names())

        viewModel.updateSortOrder("Not a real option")
        assertEquals("unknown option leaves the order alone", listOf("Trip", "Flat", "Office"), names())
    }

    @Test
    fun activityCanBeSorted() = runTest(mainRule.dispatcher) {
        repository.activity = {
            Fixtures.activitySummary(
                listOf(
                    Fixtures.activityExpense("taxi", 40.0, "2026-02-01T09:00:00Z"),
                    Fixtures.activityExpense("hotel", 900.0, "2026-01-15T09:00:00Z"),
                    Fixtures.activityExpense("lunch", 120.0, "2026-03-01T09:00:00Z")
                )
            )
        }
        store.loadIfNeeded()
        advanceUntilIdle()
        val viewModel = ActivityViewModel(repository, store)
        val sorted = observe(viewModel.sortedExpenses)
        advanceUntilIdle()

        fun ids(): List<String> {
            advanceUntilIdle()
            return sorted.value.map { it.expenseId }
        }
        assertEquals(listOf("lunch", "taxi", "hotel"), ids()) // newest first

        viewModel.updateSortPreference("Oldest First")
        assertEquals(listOf("hotel", "taxi", "lunch"), ids())

        viewModel.updateSortPreference("Highest Amount")
        assertEquals(listOf("hotel", "lunch", "taxi"), ids())

        viewModel.updateSortPreference("Lowest Amount")
        assertEquals(listOf("taxi", "lunch", "hotel"), ids())
    }
}
