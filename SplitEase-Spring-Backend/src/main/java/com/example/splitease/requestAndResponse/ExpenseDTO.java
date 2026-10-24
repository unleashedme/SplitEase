package com.example.splitease.requestAndResponse;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseDTO(
        String description,
        BigDecimal amount,
        String payerName,
        boolean userPaid,
        String date,
        BigDecimal splitPerPerson, // Null when the expense was not split equally
        String splitType,
        BigDecimal userShare,
        List<ExpenseShareDTO> shares
) {}
