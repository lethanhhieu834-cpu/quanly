package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceCode: String, // Format: HD-ddMMyyyyHHmmss (e.g. HD-30092026095530)
    val customerId: Long? = null,
    val customerName: String = "Khách lẻ",
    val customerPhone: String = "",
    val totalAmount: Double, // Tổng tiền hàng
    val discountPercent: Double = 0.0, // % chiết khấu cho khách
    val discountAmount: Double = 0.0, // Số tiền chiết khấu
    val finalAmount: Double, // Doanh thu thực tế của đơn hàng (Số tiền cần tính toán)
    val paymentMethod: String = "Tiền mặt",
    val isRefunded: Boolean = false,
    val refundReason: String = "",
    val refundedAt: Long? = null,
    val createdByRole: String = "Nhân viên",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val THANK_YOU_NOTE = "Cảm ơn Quý Khách Và Hẹn Gặp Lại"
        const val WIRE_RETURN_POLICY = "Đối với dây điện và vật tư ống nước: Mua rồi không được đổi lại / không thể trả tiền hàng"
        const val PIPE_RETURN_POLICY = "Đối với vật tư ống nước mua rồi không được đổi lại"
        const val NON_REFUNDABLE_POLICY = "Quy định: Dây điện và vật tư ống nước mua rồi không được đổi lại / không thể trả lại tiền hàng"

        fun generateInvoiceCode(timestamp: Long = System.currentTimeMillis()): String {
            val sdf = SimpleDateFormat("ddMMyyyyHHmmss", Locale.getDefault())
            return "HD-" + sdf.format(Date(timestamp))
        }
    }
}
