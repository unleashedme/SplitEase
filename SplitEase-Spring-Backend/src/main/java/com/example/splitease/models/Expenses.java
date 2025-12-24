package com.example.splitease.models;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "expenses")
public class Expenses {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Groups group;

    @ManyToOne
    @JoinColumn(name = "payer_id")
    private Users payer;

    private BigDecimal amount;
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private final Instant createdAt = Instant.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Groups getGroup() {
        return group;
    }

    public void setGroup(Groups group) {
        this.group = group;
    }

    public Users getPayer() {
        return payer;
    }

    public void setPayer(Users payer) {
        this.payer = payer;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Expenses{" +
                "id=" + id +
                ", group=" + group +
                ", payer=" + payer +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

