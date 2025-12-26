package com.example.splitease.ui.model



data class CreateGroupResponse(
    val groupId: String,
    val message: String
)

data class UserGroupResponse(
    val groupId: String,
    val groupName: String,
    val role: String
)

data class SettlementSummary(
    val toUserId: String,
    val toUserName: String,
    val amount: Double,
    val groupId: String,
    val groupName: String
)

data class ActivitySummaryDto(
    val totalExpensesCount: Long,
    val totalSpentCombined: Double,
    val expensesPaidByUser: Long,
    val settledExpensesCount: Long,
    val expenseHistory: List<ActivityExpenseDto>
)

data class ActivityExpenseDto(
    val expenseId: String,
    val description: String,
    val date: String,
    val userWasPayer: Boolean,
    val groupName: String,
    val amount: Double,
    val userShare: Double,
    val isSettled: Boolean,
    val owesToName: String?
)

data class GroupScreenDataResponse(
    val totalGroups: Long,
    val totalMembersAcrossGroups: Long,
    val totalExpenses: Double,
    val activeGroupsCount: Long,
    val groups: List<GroupDetailResponse>
)

data class GroupDetailResponse(
    val groupId: String,
    val groupName: String,
    val isActive: Boolean,
    val totalGroupExpense: Double,
    val expenseCount: Int,
    val memberCount: Int,
    val userShare: Double,
    val memberNames: List<String>,
    val settlements: List<SettlementDTO>,
    val expenses: List<ExpenseDTO>
)

data class ExpenseDTO(
    val description: String,
    val amount: Double,
    val payerName: String,
    val userPaid: Boolean,
    val date: String,
    val splitPerPerson: Double
)

data class SettlementDTO(
    val fromUser: String,
    val toUser: String,
    val amount: Double,
    val date: String,
    val note: String?
)

data class DashboardStatResponse(
    val totalUserExpense: Double,
    val amountUserOwes: Double,
    val amountOwedToUser: Double
)
