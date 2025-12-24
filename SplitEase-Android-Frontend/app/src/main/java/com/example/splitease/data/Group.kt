package com.example.splitease.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.sql.Time
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Entity(
    tableName = "groups",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],          // User.id
            childColumns = ["creatorId"],    // this field
            onDelete = ForeignKey.CASCADE,   // delete groups when user is deleted
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["creatorId"])        // index FK column (recommended)
    ]
)
data class Group(
    @PrimaryKey(autoGenerate = true)
    val groupId: Long = 0,
    val name: String,
    val creatorId: Long,
    val createdAt: Long = System.currentTimeMillis()
)
