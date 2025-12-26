package com.example.splitease.requestAndResponse;

import java.math.BigDecimal;

public record DashboardSummaryDTO(
        BigDecimal totalUserExpense,    // Total spent by user across all groups
        BigDecimal amountUserOwes,      // Negative balances (debts)
        BigDecimal amountOwedToUser     // Positive balances (credit)
) {}
