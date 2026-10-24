package com.example.splitease.testutil

import com.example.splitease.ui.RequestToasts
import com.example.splitease.ui.model.ActivityExpenseDto
import com.example.splitease.ui.model.ActivitySummaryDto
import com.example.splitease.ui.model.DashboardStatResponse
import com.example.splitease.ui.model.ExpenseDTO
import com.example.splitease.ui.model.GroupDetailResponse
import com.example.splitease.ui.model.GroupMemberDto
import com.example.splitease.ui.model.GroupScreenDataResponse
import com.example.splitease.ui.model.SettlementSummary
import com.example.splitease.ui.model.UserGroupResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

object Fixtures {

    val asha = GroupMemberDto("u-asha", "Asha", "asha@gmail.com")
    val ravi = GroupMemberDto("u-ravi", "Ravi", "ravi@gmail.com")
    val meera = GroupMemberDto("u-meera", "Meera", "meera@gmail.com")

    fun dashboardStat(total: Double = 120.0) = DashboardStatResponse(
        totalUserExpense = total, amountUserOwes = 20.0, amountOwedToUser = 50.0
    )

    fun expense(date: String = "2026-01-01T10:00:00Z", amount: Double = 90.0) = ExpenseDTO(
        description = "Dinner", amount = amount, payerName = "Asha", userPaid = true,
        date = date, splitPerPerson = amount / 3
    )

    fun group(
        name: String,
        memberCount: Int = 3,
        total: Double = 90.0,
        expenses: List<ExpenseDTO> = listOf(expense())
    ) = GroupDetailResponse(
        groupId = "g-$name", groupName = name, isActive = true, totalGroupExpense = total,
        expenseCount = expenses.size, memberCount = memberCount, userShare = total / memberCount,
        memberNames = emptyList(), expenses = expenses
    )

    fun groupScreenData(groups: List<GroupDetailResponse> = listOf(group("Trip"))) = GroupScreenDataResponse(
        totalGroups = groups.size.toLong(), totalMembersAcrossGroups = 3, totalExpenses = groups.sumOf { it.totalGroupExpense },
        activeGroupsCount = groups.size.toLong(), groups = groups
    )

    fun activityExpense(id: String, amount: Double, date: String) = ActivityExpenseDto(
        expenseId = id, description = id, date = date, userWasPayer = true, groupName = "Trip",
        amount = amount, userShare = amount / 2, isSettled = false, owesToName = null
    )

    fun activitySummary(expenses: List<ActivityExpenseDto> = emptyList()) = ActivitySummaryDto(
        totalExpensesCount = expenses.size.toLong(), totalSpentCombined = expenses.sumOf { it.userShare },
        expensesPaidByUser = 0, settledExpensesCount = 0, expenseHistory = expenses
    )

    fun userGroup(name: String, members: List<GroupMemberDto> = listOf(asha, ravi)) =
        UserGroupResponse(groupId = "g-$name", groupName = name, role = "admin", members = members)

    fun payable(name: String, amount: Double) = SettlementSummary("u-$name", name, amount)

    /** A backend error in the {"message": ...} shape it sends. */
    fun <T> errorResponse(code: Int, message: String? = null): Response<T> {
        val body = (if (message == null) "" else """{"message":"$message"}""")
            .toResponseBody("application/json".toMediaType())
        return Response.error(code, body)
    }

    fun httpException(code: Int, message: String? = null) = HttpException(errorResponse<Unit>(code, message))
}

/** Starts collecting RequestToasts and returns the list the messages land in. */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.recordToasts(): List<String> {
    val toasts = mutableListOf<String>()
    // Unconfined so the collector subscribes before the code under test emits
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        RequestToasts.messages.toList(toasts)
    }
    return toasts
}
