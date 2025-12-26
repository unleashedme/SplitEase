package com.example.splitease.requestAndResponse;

import java.math.BigDecimal;

public record ExpenseDTO(
        String description,
        BigDecimal amount,
        String payerName,
        boolean userPaid,
        String date,
        BigDecimal splitPerPerson
) {}
