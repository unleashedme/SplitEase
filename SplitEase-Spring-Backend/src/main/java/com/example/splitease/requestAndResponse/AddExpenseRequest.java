package com.example.splitease.requestAndResponse;

import com.example.splitease.models.SplitType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class AddExpenseRequest {

    private UUID groupId;
    private BigDecimal amount;
    private String description;

    // Optional: when both are omitted the expense is split equally between all group members
    private SplitType splitType;
    private List<SplitEntry> splits;

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public void setSplitType(SplitType splitType) {
        this.splitType = splitType;
    }

    public List<SplitEntry> getSplits() {
        return splits;
    }

    public void setSplits(List<SplitEntry> splits) {
        this.splits = splits;
    }

    public static class SplitEntry {

        private UUID userId;
        // Meaning depends on splitType: ignored for EQUAL, an amount for EXACT,
        // a percentage for PERCENTAGE and a weight for SHARES
        private BigDecimal value;

        public SplitEntry() {
        }

        public SplitEntry(UUID userId, BigDecimal value) {
            this.userId = userId;
            this.value = value;
        }

        public UUID getUserId() {
            return userId;
        }

        public void setUserId(UUID userId) {
            this.userId = userId;
        }

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }
    }
}
