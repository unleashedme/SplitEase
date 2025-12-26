package com.example.splitease.dto

data class UserDto(
    val name: String,
    val email: String,
    val phone: String,
    val upiId: String,
    val password: String
)

data class GroupDto(
    val name: String,
    val members: List<String>
)



data class ExpenseDto(
    val groupId: String,
    val amount: Double,
    val description: String
)

data class ExpenseSplitDto(
    val id: String,
    val expenseId: String,
    val userId: String,
    val shareAmount: Double,
    val settled: Boolean
)


data class SettlementDto(
    val groupId: String,
    val toUserId: String,
    val amount: Double,
    val note: String
)

data class AttachmentDto(
    val id: String,
    val expenseId: String,
    val url: String,
    val uploadedBy: String,
    val uploadedAt: Long
)

data class ReminderRuleDto(
    val id: String,
    val groupId: String,
    val userId: String,
    val thresholdDays: Int,
    val enabled: Boolean,
    val lastSentAt: Long?
)


data class NotificationDto(
    val id: String,
    val userId: String,
    val type: String,
    val payload: Map<String, Any>?,
    val sentAt: Long
)



