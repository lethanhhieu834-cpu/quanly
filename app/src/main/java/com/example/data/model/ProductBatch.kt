package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ProductBatch tracks distinct inventory batches with individual cost and sell prices.
 * Used for FIFO inventory management:
 * "giữa giá mới và giá cũ khi ta bán thì ưu tiên bán giá cũ hết rồi tự chuyển sang giá mới,
 * nếu trường hợp trong bill có sản phẩm giá cũ vừa hết mà có thêm sản phẩm giá mới thì vẫn tính 2 giá."
 */
@Entity(
    tableName = "product_batches",
    indices = [Index(value = ["productId"])]
)
data class ProductBatch(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val batchCode: String = "", // e.g. "Đợt giá cũ" or "Đợt nhập 01/10"
    val quantity: Double, // Remaining quantity in this batch
    val initialQuantity: Double = quantity, // Original quantity imported
    val costPrice: Double, // Cost price of this batch
    val sellPrice: Double, // Selling price of this batch
    val markupPercent: Double = 0.0, // Profit margin %: ((sellPrice - costPrice) / costPrice) * 100
    val spoolLengths: String = "", // For cuttable materials (pipes/wire)
    val createdAt: Long = System.currentTimeMillis() // Used for FIFO ordering (oldest first)
) {
    val isDepleted: Boolean get() = quantity <= 0.0001
}

/**
 * Model representing an item being restocked in batch mode:
 * "có thêm chế độ nhập hàng cho nhiều loại sản phẩm khi ta chọn nó để nhập số lượng
 * và giá nhập mới lợi nhuận mong muốn và giá bán mới"
 */
data class BatchRestockItem(
    val product: Product,
    val newQuantity: Double,
    val newCostPrice: Double,
    val markupPercent: Double,
    val newSellPrice: Double,
    val spoolLengths: String = "" // For pipes/wires
)

/**
 * Result of FIFO resolution when selling a quantity of a product across batches
 */
data class FifoSaleLine(
    val batchId: Long,
    val batchCode: String,
    val isOldPrice: Boolean,
    val isNewPrice: Boolean,
    val quantity: Double,
    val unitPrice: Double,
    val costPrice: Double,
    val lineTotal: Double = quantity * unitPrice
)

data class FifoSaleResult(
    val lines: List<FifoSaleLine>,
    val totalQuantity: Double,
    val totalAmount: Double,
    val isSplitTwoPrices: Boolean,
    val explanation: String
)
