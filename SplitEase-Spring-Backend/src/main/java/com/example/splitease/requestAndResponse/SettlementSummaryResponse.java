package com.example.splitease.requestAndResponse;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementSummaryResponse(
        UUID toUserId,
        String toUserName,
        BigDecimal amount,
        UUID groupId,
        String groupName
) {}

