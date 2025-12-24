package com.example.splitease.data

import androidx.room.*
import java.time.LocalDate
import java.util.Date


@Entity(
    tableName = "settlements",
    foreignKeys = [
        ForeignKey(
            entity = Group::class,
            parentColumns = ["id"],
            childColumns = ["group_id"]
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["from_user"]
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["to_user"]
        )
    ],
    indices = [
        Index("group_id"),
        Index("from_user"),
        Index("to_user")
    ]
)
data class Settlement(

    @PrimaryKey
    val id: Long,

    @ColumnInfo(name = "group_id")
    val groupId: Long,

    @ColumnInfo(name = "from_user")
    val fromUser: Long,

    @ColumnInfo(name = "to_user")
    val toUser: Long,

    val amount: Double,

    val status: SettlementStatus = SettlementStatus.PENDING,

    @ColumnInfo(name = "payment_ref")
    val paymentRef: String? = null,

    val paymentDescription: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: LocalDate,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Date? = null
)

enum class SettlementStatus {
    PENDING,
    INITIATED,
    SUCCESS,
    FAILED
}
