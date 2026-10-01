package com.example.util

import com.example.data.model.FifoSaleLine
import com.example.data.model.FifoSaleResult
import com.example.data.model.Product
import com.example.data.model.ProductBatch
import java.util.Locale

object FifoBatchResolver {

    /**
     * Resolves sales quantity against product batches using FIFO (First In, First Out).
     * Rule:
     * "giữa giá mới và giá cũ khi ta bán thì ưu tiên bán giá cũ hết rồi tự chuyển sang giá mới,
     * nếu trường hợp trong bill có sản phẩm giá cũ vừa hết mà có thêm sản phẩm giá mới thì vẫn tính 2 giá."
     */
    fun resolveSale(
        batches: List<ProductBatch>,
        requestedQty: Double,
        product: Product
    ): FifoSaleResult {
        if (requestedQty <= 0.0) {
            return FifoSaleResult(
                lines = emptyList(),
                totalQuantity = 0.0,
                totalAmount = 0.0,
                isSplitTwoPrices = false,
                explanation = "Số lượng bằng 0"
            )
        }

        val activeBatches = batches.filter { it.quantity > 0.0001 }.sortedBy { it.createdAt }

        // If no batches exist in DB, synthesize one from the product's base data
        if (activeBatches.isEmpty()) {
            val singleLine = FifoSaleLine(
                batchId = 0,
                batchCode = "Đợt hiện tại",
                isOldPrice = false,
                isNewPrice = true,
                quantity = requestedQty,
                unitPrice = product.sellPrice,
                costPrice = product.costPrice
            )
            return FifoSaleResult(
                lines = listOf(singleLine),
                totalQuantity = requestedQty,
                totalAmount = singleLine.lineTotal,
                isSplitTwoPrices = false,
                explanation = "${formatQty(requestedQty)} ${product.unit} x ${PdfExporter.formatVnd(product.sellPrice)}"
            )
        }

        val lines = mutableListOf<FifoSaleLine>()
        var remainingNeeded = requestedQty
        val latestBatch = activeBatches.last()

        for (batch in activeBatches) {
            if (remainingNeeded <= 0.0001) break

            val qtyFromBatch = if (remainingNeeded <= batch.quantity) remainingNeeded else batch.quantity
            if (qtyFromBatch > 0.0001) {
                val isOld = batch.id != latestBatch.id
                val isNew = batch.id == latestBatch.id
                val code = when {
                    batch.batchCode.isNotBlank() -> batch.batchCode
                    isOld -> "Đợt giá cũ"
                    else -> "Đợt giá mới"
                }

                lines.add(
                    FifoSaleLine(
                        batchId = batch.id,
                        batchCode = code,
                        isOldPrice = isOld,
                        isNewPrice = isNew,
                        quantity = qtyFromBatch,
                        unitPrice = batch.sellPrice,
                        costPrice = batch.costPrice
                    )
                )
                remainingNeeded -= qtyFromBatch
            }
        }

        // If requestedQty > all active batches combined (oversell)
        if (remainingNeeded > 0.0001) {
            val fallbackPrice = latestBatch.sellPrice
            val fallbackCost = latestBatch.costPrice
            lines.add(
                FifoSaleLine(
                    batchId = latestBatch.id,
                    batchCode = "Đợt giá mới (Vượt tồn)",
                    isOldPrice = false,
                    isNewPrice = true,
                    quantity = remainingNeeded,
                    unitPrice = fallbackPrice,
                    costPrice = fallbackCost
                )
            )
        }

        val totalAmount = lines.sumOf { it.lineTotal }
        val distinctPrices = lines.map { it.unitPrice }.distinct()
        val isSplitTwoPrices = distinctPrices.size > 1

        val explanation = if (isSplitTwoPrices) {
            val oldLine = lines.firstOrNull { it.isOldPrice } ?: lines[0]
            val newLine = lines.lastOrNull { it.isNewPrice } ?: lines[1]
            "Ưu tiên bán hết ${formatQty(oldLine.quantity)} ${product.unit} giá cũ (${PdfExporter.formatVnd(oldLine.unitPrice)}), " +
                    "tự động chuyển ${formatQty(newLine.quantity)} ${product.unit} giá mới (${PdfExporter.formatVnd(newLine.unitPrice)})"
        } else {
            "${formatQty(requestedQty)} ${product.unit} x ${PdfExporter.formatVnd(lines.firstOrNull()?.unitPrice ?: product.sellPrice)}"
        }

        return FifoSaleResult(
            lines = lines,
            totalQuantity = requestedQty,
            totalAmount = totalAmount,
            isSplitTwoPrices = isSplitTwoPrices,
            explanation = explanation
        )
    }

    private fun formatQty(q: Double): String {
        return if (q % 1.0 == 0.0) q.toLong().toString() else "%.1f".format(Locale.US, q)
    }
}
