package com.example.splitease.requestAndResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExpenseActivityDTO(
        UUID expenseId,
        String description,
        String date,
        boolean userWasPayer,
        String groupName,
        BigDecimal amount,
        BigDecimal userShare,
        boolean isSettled,
        String owesToName // Null if user was the payer or if already settled
) {}
