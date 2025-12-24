package com.example.splitease.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val password: String,
    val email: String,
    val upiId: String,
    val phoneNumber: String,
    val createdAt: Long = System.currentTimeMillis()
)
