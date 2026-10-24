package com.example.splitease.data

import com.example.splitease.testutil.FakeRepository
import com.example.splitease.testutil.Fixtures
import com.example.splitease.testutil.MainDispatcherRule
import com.example.splitease.testutil.recordToasts
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class AppDataStoreTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeRepository()
    private val store = AppDataStore(repository)

    @After
    fun tearDown() = store.close()

    @Test
    fun loadIfNeededFetchesEverythingOnce() = runTest(mainRule.dispatcher) {
        repository.payables = { listOf(Fixtures.payable("Ravi", 40.0)) }
        repository.myGroups = { listOf(Fixtures.userGroup("Trip")) }

        store.loadIfNeeded()
        advanceUntilIdle()

        assertEquals(Fixtures.dashboardStat(), store.dashboardStats)
        assertEquals(Fixtures.groupScreenData(), store.groupScreenData)
        assertEquals(Fixtures.activitySummary(), store.activity)
        assertEquals(listOf(Fixtures.payable("Ravi", 40.0)), store.payables)
        assertEquals(listOf(Fixtures.userGroup("Trip")), store.myGroups)
        for (call in listOf("getDashboardStat", "getGroupScreenData", "getActivity", "getMyPayables", "getMyGroups")) {
            assertEquals(call, 1, repository.callCount(call))
        }
    }

    @Test
    fun switchingScreensDoesNotRefetch() = runTest(mainRule.dispatcher) {
        // Every screen's ViewModel calls loadIfNeeded when it's created
        repeat(5) {
            store.loadIfNeeded()
            advanceUntilIdle()
        }

        assertEquals(1, repository.storeLoadCount())
    }

    @Test
    fun refreshAlwaysRefetches() = runTest(mainRule.dispatcher) {
        store.loadIfNeeded()
        advanceUntilIdle()
        repository.dashboardStat = { Fixtures.dashboardStat(total = 999.0) }

        store.refresh()
        advanceUntilIdle()

        assertEquals(2, repository.storeLoadCount())
        assertEquals(999.0, store.dashboardStats!!.totalUserExpense, 0.0)
    }

    @Test
    fun isRefreshingIsTrueOnlyWhileLoading() = runTest(mainRule.dispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.dashboardStat = { gate.await(); Fixtures.dashboardStat() }

        assertFalse(store.isRefreshing)
        store.refresh()
        runCurrent()
        assertTrue(store.isRefreshing)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(store.isRefreshing)
    }

    @Test
    fun aNewRefreshReplacesOneInFlight() = runTest(mainRule.dispatcher) {
        val firstGate = CompletableDeferred<Unit>()
        repository.dashboardStat = { firstGate.await(); Fixtures.dashboardStat(total = 1.0) }
        store.refresh()
        runCurrent()

        repository.dashboardStat = { Fixtures.dashboardStat(total = 2.0) }
        store.refresh()
        advanceUntilIdle()

        // The stale first request never lands, and the spinner turns off with the newer refresh
        assertEquals(2.0, store.dashboardStats!!.totalUserExpense, 0.0)
        assertFalse(store.isRefreshing)
    }

    @Test
    fun aFailedLoadToastsAndIsRetriedOnTheNextScreenVisit() = runTest(mainRule.dispatcher) {
        val toasts = recordToasts()
        repository.activity = { throw IOException("offline") }

        store.loadIfNeeded()
        advanceUntilIdle()

        assertNull(store.activity)
        assertEquals("other data still loads", Fixtures.dashboardStat(), store.dashboardStats)
        assertEquals(listOf("Couldn't load activity: check your internet connection"), toasts)
        assertFalse(store.isRefreshing)

        repository.activity = { Fixtures.activitySummary() }
        store.loadIfNeeded()
        advanceUntilIdle()

        assertEquals(2, repository.storeLoadCount())
        assertEquals(Fixtures.activitySummary(), store.activity)
    }

    @Test
    fun refreshIfLoadedDoesNothingBeforeTheFirstLoad() = runTest(mainRule.dispatcher) {
        // e.g. a push notification arriving while the user is still on the login screen
        store.refreshIfLoaded()
        advanceUntilIdle()
        assertEquals(0, repository.storeLoadCount())

        store.loadIfNeeded()
        advanceUntilIdle()
        store.refreshIfLoaded()
        advanceUntilIdle()
        assertEquals(2, repository.storeLoadCount())
    }

    @Test
    fun closeStopsLoadsInFlight() = runTest(mainRule.dispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.dashboardStat = { gate.await(); Fixtures.dashboardStat() }
        store.refresh()
        runCurrent()

        store.close()
        gate.complete(Unit)
        advanceUntilIdle()

        assertNull("a logged-out user's data never lands", store.dashboardStats)
    }
}
