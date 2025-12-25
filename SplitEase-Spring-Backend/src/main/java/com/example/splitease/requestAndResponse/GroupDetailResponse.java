package com.example.splitease.requestAndResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GroupDetailResponse(
        UUID groupId,
        String groupName,
        boolean isActive,
        BigDecimal totalGroupExpense,
        int expenseCount,
        int memberCount,
        BigDecimal userShare,
        List<String> memberNames,
        List<SettlementDTO> settlements,
        List<ExpenseDTO> expenses
) {}
