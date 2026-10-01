package com.example

import com.example.data.model.Invoice
import com.example.data.model.Product
import com.example.util.SpoolManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testUserPipeDeductionScenario() {
        // "thí dụ ta nhập 6 cây mỗi cây dài 6m"
        val initialStock = listOf(6.0, 6.0, 6.0, 6.0, 6.0, 6.0)

        // "khi ta bán 5m thì còn dư 5 cây 6m và 1 cây 1m"
        val step1 = SpoolManager.deductFromSpools(
            currentSpools = initialStock,
            lengthToDeduct = 5.0,
            unit = "m",
            pieceUnit = "cây",
            standardLength = 6.0
        )
        assertEquals(listOf(1.0, 6.0, 6.0, 6.0, 6.0, 6.0), step1.updatedSpools)
        assertEquals(5, step1.updatedSpools.count { it == 6.0 })
        assertEquals(1, step1.updatedSpools.count { it == 1.0 })
        assertEquals("5 cây 6m và 1 cây 1m (không được phép gộp chung)", step1.formattedBreakdown)

        // "khi còn dư 3 cây 6m, 1 cây 2m, 1 cây 1m không được phép gộp chung"
        val intermediateStock = listOf(6.0, 6.0, 6.0, 2.0, 1.0)
        val formattedIntermediate = SpoolManager.formatSpoolBreakdown(intermediateStock, "m", "cây")
        assertTrue(formattedIntermediate.contains("3 cây 6m"))
        assertTrue(formattedIntermediate.contains("1 cây 2m"))
        assertTrue(formattedIntermediate.contains("1 cây 1m"))
        assertTrue(formattedIntermediate.contains("(không được phép gộp chung)"))

        // "nếu bán thêm 1m nữa thì còn 3 cây 6m với 1 cây 2m, cứ vậy suy ra"
        // Smart deduction should prioritize the exact match 1m piece!
        val step2 = SpoolManager.deductFromSpools(
            currentSpools = intermediateStock,
            lengthToDeduct = 1.0,
            unit = "m",
            pieceUnit = "cây",
            standardLength = 6.0
        )
        assertEquals(listOf(6.0, 6.0, 6.0, 2.0), step2.updatedSpools)
        assertTrue(step2.exactMatchUsed)
        assertEquals("3 cây 6m và 1 cây 2m (không được phép gộp chung)", step2.formattedBreakdown)
    }

    @Test
    fun testSmartConversionPricing() {
        // Ống PVC giá 120,000 VND / cây 6m
        // Bán 5m
        val conversion5m = SpoolManager.calculateConversion(
            enteredQty = 5.0,
            isMeterMode = true,
            standardPieceLength = 6.0,
            productBasePrice = 120_000.0,
            productBaseUnit = "cây",
            pieceUnit = "cây"
        )
        assertEquals(5.0, conversion5m.lengthInMeters, 0.001)
        assertEquals(100_000.0, conversion5m.calculatedPrice, 0.001)

        // Bán 2 cây
        val conversion2Cay = SpoolManager.calculateConversion(
            enteredQty = 2.0,
            isMeterMode = false,
            standardPieceLength = 6.0,
            productBasePrice = 120_000.0,
            productBaseUnit = "cây",
            pieceUnit = "cây"
        )
        assertEquals(12.0, conversion2Cay.lengthInMeters, 0.001)
        assertEquals(240_000.0, conversion2Cay.calculatedPrice, 0.001)
    }

    @Test
    fun testReturnPolicies() {
        val pipeProduct = Product(
            code = "ONG01",
            name = "Ống nhựa PVC Tiền Phong D21 C2",
            category = "Ống Nước",
            unit = "cây",
            costPrice = 80_000.0,
            sellPrice = 120_000.0,
            quantity = 6.0,
            spoolLengths = "6,6,6"
        )
        val wireProduct = Product(
            code = "DAY01",
            name = "Dây điện Cadivi 2x2.5",
            category = "Dây Cáp Điện",
            unit = "cuộn",
            costPrice = 800_000.0,
            sellPrice = 1_000_000.0,
            quantity = 3.0,
            spoolLengths = "100,100,100"
        )
        assertTrue(pipeProduct.isPipeProduct)
        assertTrue(pipeProduct.isNonRefundable)
        assertTrue(wireProduct.isWireProduct)
        assertTrue(wireProduct.isNonRefundable)

        assertTrue(Invoice.WIRE_RETURN_POLICY.contains("không được đổi lại"))
    }
}
