package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_accounts")
data class BankAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankName: String, // e.g. "MB Bank", "Vietcombank", "Techcombank", "BIDV", "Agribank", "ACB"
    val bankCode: String = "", // e.g. "MB", "VCB", "TCB", "BIDV", "VBA", "ACB"
    val accountNumber: String, // Số tài khoản ngân hàng
    val accountHolder: String, // Tên chủ tài khoản
    val isDefault: Boolean = false,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
