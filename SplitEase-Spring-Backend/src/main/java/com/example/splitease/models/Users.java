package com.example.splitease.models;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class Users {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true, nullable = false)
    private String phone;

    @Column(name = "upi_id", nullable = false)
    private String upiId;

    @Column(nullable = false)
    private String password;

    @Column(name = "created_at", nullable = false, updatable = false)
    private final Instant createdAt = Instant.now();

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "Users{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", UpiId='" + upiId + '\'' +
                ", password='" + password + '\'' +
                ", createdAt=" + createdAt +
                ", lastActiveAt=" + lastActiveAt +
                '}';
    }
}
