package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val name: String,
    val category: String,
    val unit: String, // e.g. "cây", "cuộn", "m", "cái", "hộp", "bó"
    val costPrice: Double,
    val sellPrice: Double,
    val markupPercent: Double = 0.0,
    val quantity: Double,
    val minStockAlert: Double = 5.0,
    val spoolLengths: String = "", // e.g. "6.0,6.0,6.0,6.0,6.0,6.0" or "200.0,200.0,200.0"
    val packagingDetails: String = "", // e.g. "6 cây mỗi cây 6m (Tổng 36m)"
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean get() = quantity <= minStockAlert

    /**
     * Determines whether this item is electrical wire.
     */
    val isWireProduct: Boolean
        get() {
            val lower = name.lowercase()
            return lower.contains("dây") ||
                    lower.contains("day") ||
                    lower.contains("dây điện") ||
                    lower.contains("dây cáp") ||
                    lower.contains("cáp điện") ||
                    (category.contains("điện", ignoreCase = true) && (unit.equals("cuộn", ignoreCase = true) || unit.equals("m", ignoreCase = true)))
        }

    /**
     * Determines whether this item is pipe/plumbing material.
     * Rule: Đơn vị tính cho ống các loại là cây. Đối với vật tư ống nước mua rồi không được đổi lại.
     */
    val isPipeProduct: Boolean
        get() {
            val lower = name.lowercase()
            return lower.contains("ống") ||
                    lower.contains("ong") ||
                    lower.contains("pvc") ||
                    lower.contains("ppr") ||
                    lower.contains("hdpe") ||
                    lower.contains("tiền phong") ||
                    category.contains("nước", ignoreCase = true) ||
                    unit.equals("cây", ignoreCase = true)
        }

    /**
     * Products that can be cut by length (meters or pieces).
     */
    val isCuttableProduct: Boolean
        get() = isWireProduct || isPipeProduct || spoolLengths.isNotBlank()

    /**
     * Both wire and pipe materials cannot be returned/refunded.
     */
    val isNonRefundable: Boolean
        get() = isWireProduct || isPipeProduct

    /**
     * Piece unit name: "cây" for pipes, "cuộn" for wire, or fallback to unit.
     */
    val pieceUnitName: String
        get() = when {
            isPipeProduct -> "cây"
            isWireProduct -> "cuộn"
            else -> unit
        }

    /**
     * Length in meters of a single standard full uncut piece (e.g. 6.0m for 6m pipe, 4.0m, or 200.0m for wire).
     */
    val standardPieceLength: Double
        get() {
            val list = parseSpoolList()
            if (list.isNotEmpty()) {
                return list.maxOrNull() ?: 6.0
            }
            val lower = name.lowercase()
            return when {
                lower.contains("6m") -> 6.0
                lower.contains("4m") -> 4.0
                lower.contains("200m") -> 200.0
                lower.contains("100m") -> 100.0
                isPipeProduct -> 6.0
                isWireProduct -> 100.0
                else -> 1.0
            }
        }

    fun parseSpoolList(): List<Double> {
        if (spoolLengths.isBlank()) return emptyList()
        return spoolLengths.split(",")
            .mapNotNull { it.trim().toDoubleOrNull() }
            .filter { it > 0.0 }
    }
}
