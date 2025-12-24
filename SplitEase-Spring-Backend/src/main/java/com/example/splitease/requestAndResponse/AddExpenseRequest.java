package com.example.splitease.requestAndResponse;

import java.math.BigDecimal;
import java.util.UUID;

public class AddExpenseRequest {

    private UUID groupId;
    private BigDecimal amount;
    private String description;

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
}
