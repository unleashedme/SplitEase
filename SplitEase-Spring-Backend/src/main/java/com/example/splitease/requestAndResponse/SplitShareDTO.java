package com.example.splitease.requestAndResponse;

import java.math.BigDecimal;

/** One person's part of an expense, as shown on the expense details screen. */
public record SplitShareDTO(
        String name,
        BigDecimal amount,
        boolean isPayer,
        boolean isCurrentUser,
        boolean isSettled
) {}
