package com.example.splitease.models;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "expense_splits")
public class ExpenseSplits {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "expense_id")
    private Expenses expense;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user;

    private BigDecimal shareAmount;
    private Boolean settled;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Expenses getExpense() {
        return expense;
    }

    public void setExpense(Expenses expense) {
        this.expense = expense;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    public BigDecimal getShareAmount() {
        return shareAmount;
    }

    public void setShareAmount(BigDecimal shareAmount) {
        this.shareAmount = shareAmount;
    }

    public Boolean getSettled() {
        return settled;
    }

    public void setSettled(Boolean settled) {
        this.settled = settled;
    }

    @Override
    public String toString() {
        return "ExpenseSplits{" +
                "id=" + id +
                ", expense=" + expense +
                ", user=" + user +
                ", shareAmount=" + shareAmount +
                ", settled=" + settled +
                '}';
    }
}

