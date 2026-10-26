package com.example.splitease.requestAndResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ExpenseDTO(
        String description,
        BigDecimal amount,
        String payerName,
        boolean userPaid,
        String date,
        BigDecimal splitPerPerson, // Null when the expense was not split equally
        String splitType,
        BigDecimal userShare,
        List<ExpenseShareDTO> shares,
        UUID expenseId,
        boolean hasReceipt
) {}
