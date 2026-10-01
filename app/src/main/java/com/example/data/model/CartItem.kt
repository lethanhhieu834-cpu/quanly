package com.example.data.model

data class CartItem(
    val product: Product,
    val quantity: Double, // Number in sold unit (e.g. 5.0 for 5m, or 1.0 for 1 cây)
    val soldUnit: String, // "m", "cây", "cuộn", "cái", etc.
    val lengthInMeters: Double, // Exact length in meters for cuttable goods (e.g. 5.0m, or 6.0m for 1 cây)
    val unitPrice: Double, // Price per soldUnit (e.g. 20,000 VND/m or 120,000 VND/cây)
    val lineTotal: Double = quantity * unitPrice,
    val note: String = "", // e.g. "Đợt giá cũ (Hết đợt cũ)", "Đợt giá mới", "Cắt từ 1 cây 6m", etc.
    val batchId: Long = 0,
    val batchCode: String = "" // "Đợt giá cũ", "Đợt giá mới"
) {
    val cartItemId: String get() = "${product.id}_${unitPrice}_${soldUnit}_${batchId}"
    val isCuttable: Boolean get() = product.isCuttableProduct
    val isWire: Boolean get() = product.isWireProduct
    val isPipe: Boolean get() = product.isPipeProduct
    val isNonRefundable: Boolean get() = product.isNonRefundable
}

