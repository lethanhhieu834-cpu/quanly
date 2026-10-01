package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_items")
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceId: Long,
    val invoiceCode: String,
    val productId: Long,
    val productName: String,
    val unit: String,
    val costPrice: Double,
    val unitPrice: Double,
    val quantity: Double,
    val total: Double
) {
    /**
     * Determines whether this item is electrical wire.
     * Rule: Đối với dây điện thì không thể trả tiền hàng.
     */
    val isWireProduct: Boolean
        get() {
            val lower = productName.lowercase()
            return lower.contains("dây") ||
                    lower.contains("day") ||
                    lower.contains("dây điện") ||
                    lower.contains("dây cáp") ||
                    lower.contains("cáp điện") ||
                    unit.equals("cuộn", ignoreCase = true)
        }

    /**
     * Determines whether this item is pipe/plumbing material.
     * Rule: Đối với vật tư ống nước mua rồi không được đổi lại.
     */
    val isPipeProduct: Boolean
        get() {
            val lower = productName.lowercase()
            return lower.contains("ống") ||
                    lower.contains("ong") ||
                    lower.contains("pvc") ||
                    lower.contains("ppr") ||
                    lower.contains("hdpe") ||
                    lower.contains("tiền phong") ||
                    unit.equals("cây", ignoreCase = true)
        }

    /**
     * Both electrical wire and water pipes cannot be returned/refunded.
     */
    val isNonRefundable: Boolean
        get() = isWireProduct || isPipeProduct
}
