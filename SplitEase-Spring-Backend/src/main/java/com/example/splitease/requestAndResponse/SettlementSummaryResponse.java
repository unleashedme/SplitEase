package com.example.splitease.requestAndResponse;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementSummaryResponse(
        String toUserId,
        String toUserName,
        BigDecimal amount
) {}

