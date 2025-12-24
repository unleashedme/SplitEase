package com.example.splitease.models;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "settlements")
public class Settlements {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Groups group;

    @ManyToOne
    @JoinColumn(name = "from_user")
    private Users fromUser;

    @ManyToOne
    @JoinColumn(name = "to_user")
    private Users toUser;

    private BigDecimal amount;
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Users getToUser() {
        return toUser;
    }

    public void setToUser(Users toUser) {
        this.toUser = toUser;
    }

    public Users getFromUser() {
        return fromUser;
    }

    public void setFromUser(Users fromUser) {
        this.fromUser = fromUser;
    }

    public Groups getGroup() {
        return group;
    }

    public void setGroup(Groups group) {
        this.group = group;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Settlements{" +
                "id=" + id +
                ", group=" + group +
                ", fromUser=" + fromUser +
                ", toUser=" + toUser +
                ", amount=" + amount +
                ", note='" + note + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}


