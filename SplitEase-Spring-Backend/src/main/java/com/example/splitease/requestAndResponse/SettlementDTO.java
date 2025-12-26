package com.example.splitease.requestAndResponse;

import java.math.BigDecimal;

public record SettlementDTO(
        String fromUser,
        String toUser,
        BigDecimal amount,
        String date,
        String note
) {}
