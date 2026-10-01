package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String, // "Lương nhân viên", "Tiền điện", "Tiền nước", "Mặt bằng", "Khác"
    val amount: Double,
    val dateStr: String, // "YYYY-MM-DD"
    val monthYear: String, // "MM/YYYY"
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
