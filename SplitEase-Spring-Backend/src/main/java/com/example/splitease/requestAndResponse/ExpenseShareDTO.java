package com.example.splitease.requestAndResponse;

import java.math.BigDecimal;

public record ExpenseShareDTO(
        String name,
        BigDecimal amount
) {}
