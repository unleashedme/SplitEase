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
