package com.example.splitease.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.Date


@Entity(tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Group::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["payer_id"]
        )
    ],
    indices = [
        Index("group_id"),
        Index("payer_id")
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val expenseId: Long = 0,
    @ColumnInfo(name = "group_id")
    val groupId: Long,
    @ColumnInfo(name = "payer_id")
    val payerId: Long,
    val amount: Double,
    val description: String,
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDate,
    val metadata: String? = null
)
