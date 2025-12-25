package com.example.splitease.requestAndResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GroupDataResponse(
        long totalGroups,
        long totalMembersAcrossGroups,
        BigDecimal totalExpenses,
        long activeGroupsCount,
        List<GroupDetailResponse> groups
) { }

