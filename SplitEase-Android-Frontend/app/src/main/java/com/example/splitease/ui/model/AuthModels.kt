package com.example.splitease.ui.model

data class LogInRequests(
    val email: String,
    val password: String
)

data class LogInResponse(
    val token: String,
    val name: String,
    val email: String,
    val phone: String
)
