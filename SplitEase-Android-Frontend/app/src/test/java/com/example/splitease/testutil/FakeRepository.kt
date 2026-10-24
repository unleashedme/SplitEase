package com.example.splitease.testutil

import com.example.splitease.data.Repository
import com.example.splitease.dto.SettlementDto
import com.example.splitease.dto.SplitEntryDto
import com.example.splitease.dto.UserDto
import com.example.splitease.ui.model.ActivitySummaryDto
import com.example.splitease.ui.model.CreateGroupResponse
import com.example.splitease.ui.model.DashboardStatResponse
import com.example.splitease.ui.model.GroupScreenDataResponse
import com.example.splitease.ui.model.LogInResponse
import com.example.splitease.ui.model.SettlementSummary
import com.example.splitease.ui.model.UserGroupResponse
import retrofit2.Response

/**
 * In-memory Repository. Each call is answered by the matching handler (replace it to return
 * different data or throw) and counted in [calls], so tests can assert what hit the network.
 */
class FakeRepository : Repository {

    val calls = mutableMapOf<String, Int>()
    fun callCount(name: String) = calls[name] ?: 0
    private fun record(name: String) { calls[name] = callCount(name) + 1 }

    // Loads used by AppDataStore
    var dashboardStat: suspend () -> DashboardStatResponse = { Fixtures.dashboardStat() }
    var groupScreenData: suspend () -> GroupScreenDataResponse = { Fixtures.groupScreenData() }
    var activity: suspend () -> ActivitySummaryDto = { Fixtures.activitySummary() }
    var payables: suspend () -> List<SettlementSummary> = { emptyList() }
    var myGroups: suspend () -> List<UserGroupResponse> = { emptyList() }

    // Actions
    var login: suspend (String, String) -> LogInResponse =
        { email, _ -> LogInResponse("token-123", "Asha", email, "9876543210") }
    var register: suspend (UserDto) -> Unit = {}
    var createGroup: suspend (String, List<String>) -> Response<CreateGroupResponse> =
        { _, _ -> Response.success(CreateGroupResponse("g-new", "created")) }
    var addExpense: suspend (AddExpenseCall) -> Response<Unit> = { Response.success(Unit) }
    var recordSettlement: suspend (SettlementDto) -> Unit = {}
    var deleteGroup: suspend (String) -> Response<Unit> = { Response.success(Unit) }
    var deleteExpense: suspend (String) -> Response<Unit> = { Response.success(Unit) }
    var updateFcmToken: suspend (String) -> Response<Unit> = { Response.success(Unit) }

    var lastAddExpense: AddExpenseCall? = null
    var lastSettlement: SettlementDto? = null

    data class AddExpenseCall(
        val groupId: String,
        val amount: Double,
        val description: String,
        val splitType: String,
        val splits: List<SplitEntryDto>
    )

    override suspend fun login(email: String, password: String): LogInResponse {
        record("login"); return login.invoke(email, password)
    }

    override suspend fun register(userDetails: UserDto) {
        record("register"); register.invoke(userDetails)
    }

    override suspend fun createGroup(groupName: String, members: List<String>): Response<CreateGroupResponse> {
        record("createGroup"); return createGroup.invoke(groupName, members)
    }

    override suspend fun addExpense(
        groupId: String, amount: Double, description: String, splitType: String, splits: List<SplitEntryDto>
    ): Response<Unit> {
        record("addExpense")
        val call = AddExpenseCall(groupId, amount, description, splitType, splits)
        lastAddExpense = call
        return addExpense.invoke(call)
    }

    override suspend fun getMyGroups(): List<UserGroupResponse> {
        record("getMyGroups"); return myGroups()
    }

    override suspend fun getMyPayables(): List<SettlementSummary> {
        record("getMyPayables"); return payables()
    }

    override suspend fun recordSettlement(settlementDto: SettlementDto) {
        record("recordSettlement"); lastSettlement = settlementDto; recordSettlement.invoke(settlementDto)
    }

    override suspend fun getActivity(): ActivitySummaryDto {
        record("getActivity"); return activity()
    }

    override suspend fun getGroupScreenData(): GroupScreenDataResponse {
        record("getGroupScreenData"); return groupScreenData()
    }

    override suspend fun getDashboardStat(): DashboardStatResponse {
        record("getDashboardStat"); return dashboardStat()
    }

    override suspend fun deleteGroup(groupId: String): Response<Unit> {
        record("deleteGroup"); return deleteGroup.invoke(groupId)
    }

    override suspend fun deleteExpense(expenseId: String): Response<Unit> {
        record("deleteExpense"); return deleteExpense.invoke(expenseId)
    }

    override suspend fun updateFcmToken(token: String): Response<Unit> {
        record("updateFcmToken"); return updateFcmToken.invoke(token)
    }

    /** Requests made by one full AppDataStore refresh. */
    fun storeLoadCount(): Int = callCount("getDashboardStat")
}
