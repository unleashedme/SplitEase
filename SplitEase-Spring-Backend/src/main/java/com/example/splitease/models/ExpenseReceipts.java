package com.example.splitease.models;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

/**
 * Photo of the bill for an expense, at most one per expense.
 * Kept out of the expenses table so loading expense lists never pulls image bytes.
 */
@Entity
@Table(name = "expense_receipts")
public class ExpenseReceipts {

    @Id
    @Column(name = "expense_id")
    private UUID expenseId;

    // The database removes the receipt when its expense (or the expense's group) is deleted
    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Expenses expense;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    // Plain bytea; @Lob would use Postgres large objects, which aren't removed with the row
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] data;

    // Small JPEG shown on the expense screen
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] thumbnail;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getExpenseId() {
        return expenseId;
    }

    public Expenses getExpense() {
        return expense;
    }

    public void setExpense(Expenses expense) {
        this.expense = expense;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(byte[] thumbnail) {
        this.thumbnail = thumbnail;
    }

    public int getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(int sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
