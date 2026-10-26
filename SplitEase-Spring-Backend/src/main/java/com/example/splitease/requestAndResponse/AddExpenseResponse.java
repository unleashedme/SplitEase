package com.example.splitease.requestAndResponse;

import java.util.UUID;

// The app needs the id to upload the bill photo after creating the expense
public record AddExpenseResponse(
        UUID expenseId
) {}
