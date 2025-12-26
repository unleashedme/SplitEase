package com.example.splitease.requestAndResponse;

import java.math.BigDecimal;
import java.util.List;

public record ActivityDashboardResponse(
        long totalExpensesCount,
        BigDecimal totalSpentCombined,
        long expensesPaidByUser,
        long settledExpensesCount,
        List<ExpenseActivityDTO> expenseHistory
) {}

