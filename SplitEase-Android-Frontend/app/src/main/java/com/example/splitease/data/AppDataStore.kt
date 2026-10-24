package com.example.splitease.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.splitease.ui.RequestToasts
import com.example.splitease.ui.model.ActivitySummaryDto
import com.example.splitease.ui.model.DashboardStatResponse
import com.example.splitease.ui.model.GroupScreenDataResponse
import com.example.splitease.ui.model.SettlementSummary
import com.example.splitease.ui.model.UserGroupResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * App-wide cache of the data shown on the Dashboard, Groups and Activity screens.
 *
 * Screens create fresh ViewModels every time they're opened, so fetching in each ViewModel
 * meant several requests per tab switch. Instead this store loads everything once (on app start
 * or after login) and reloads only after something changes, via [refresh].
 * It lives in the AppContainer, which is replaced on logout, so the next user starts empty.
 */
class AppDataStore(private val repository: Repository) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var refreshJob: Job? = null
    private var loaded = false

    // Compose state, so screens reading these through their ViewModels update automatically
    var dashboardStats by mutableStateOf<DashboardStatResponse?>(null)
        private set
    var groupScreenData by mutableStateOf<GroupScreenDataResponse?>(null)
        private set
    var activity by mutableStateOf<ActivitySummaryDto?>(null)
        private set
    var payables by mutableStateOf<List<SettlementSummary>>(emptyList())
        private set
    var myGroups by mutableStateOf<List<UserGroupResponse>>(emptyList())
        private set

    /** Loads everything the first time it's called; later calls do nothing. */
    fun loadIfNeeded() {
        if (!loaded) refresh()
    }

    /** Reloads everything, e.g. after a group, expense or settlement change. */
    fun refresh() {
        loaded = true
        // A refresh already in flight would fetch the same thing, so restart it with fresh requests
        refreshJob?.cancel()
        refreshJob = scope.launch {
            val results = listOf(
                async { load("Couldn't load dashboard") { dashboardStats = repository.getDashboardStat() } },
                async { load("Couldn't load groups") { groupScreenData = repository.getGroupScreenData() } },
                async { load("Couldn't load activity") { activity = repository.getActivity() } },
                async { load("Couldn't load what you owe") { payables = repository.getMyPayables() } },
                async { load("Couldn't load groups") { myGroups = repository.getMyGroups() } }
            ).awaitAll()
            // Retry on the next screen visit if anything failed, e.g. no connection at startup
            if (results.any { !it }) loaded = false
        }
    }

    /** For outside triggers like push notifications: refresh only if this user's data is in use. */
    fun refreshIfLoaded() {
        if (loaded) refresh()
    }

    fun close() {
        scope.cancel()
    }

    private suspend fun load(failureMessage: String, block: suspend () -> Unit): Boolean =
        try {
            block()
            true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            println("AppDataStore: $failureMessage: ${e::class.java} ${e.message}")
            RequestToasts.failure(failureMessage, e)
            false
        }
}
