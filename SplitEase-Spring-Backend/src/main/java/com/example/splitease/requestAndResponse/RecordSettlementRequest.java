package com.example.splitease.requestAndResponse;

import java.math.BigDecimal;
import java.util.UUID;

public class RecordSettlementRequest {
    private UUID toUserId;
    private BigDecimal amount;
    private String note;

    public UUID getToUserId() {
        return toUserId;
    }

    public void setToUserId(UUID toUserId) {
        this.toUserId = toUserId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

