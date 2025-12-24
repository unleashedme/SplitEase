package com.example.splitease.models;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "group_members")
public class GroupMembers {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Groups group;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user;

    private String role; // admin / member

    @Column(name = "joined_at", nullable = false, updatable = false)
    final private Instant joinedAt = Instant.now();

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

}

