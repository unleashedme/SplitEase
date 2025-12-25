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


