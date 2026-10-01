package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.util.SpoolManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Quản Lý Vật Tư", appName)
    }

    @Test
    fun `verify invoice code format HD-ddMMyyyyHHmmss and policy`() {
        val code = Invoice.generateInvoiceCode()
        assertTrue(code.startsWith("HD-"))
        assertEquals(17, code.length) // "HD-" (3) + 14 digits = 17 chars
        assertEquals("Cảm ơn Quý Khách Và Hẹn Gặp Lại", Invoice.THANK_YOU_NOTE)
        assertTrue(Invoice.WIRE_RETURN_POLICY.contains("không thể trả lại tiền hàng"))
    }

    @Test
    fun `verify wire product cannot be refunded rule`() {
        val wireItem = InvoiceItem(
            id = 1,
            invoiceId = 1,
            invoiceCode = "HD-TEST",
            productId = 1,
            productName = "Dây điện đôi Cadisun 2x2.5mm",
            unit = "m",
            costPrice = 3000.0,
            unitPrice = 5000.0,
            quantity = 50.0,
            total = 250000.0
        )
        assertTrue(wireItem.isWireProduct)

        val switchItem = InvoiceItem(
            id = 2,
            invoiceId = 1,
            invoiceCode = "HD-TEST",
            productId = 2,
            productName = "Hộp công tắc đơn Sino",
            unit = "cái",
            costPrice = 12000.0,
            unitPrice = 18000.0,
            quantity = 5.0,
            total = 90000.0
        )
        assertFalse(switchItem.isWireProduct)
    }

    @Test
    fun `verify spool deduction wire rolls logic`() {
        // 3 rolls of 200m each
        val spools = listOf(200.0, 200.0, 200.0)

        // Deduct 150m from first roll -> 50m left on roll 1, roll 2 and 3 intact
        val (afterFirstSale, display1) = SpoolManager.deductFromSpools(spools, 150.0)
        assertEquals(listOf(50.0, 200.0, 200.0), afterFirstSale)
        assertEquals("Cuộn 1: 50m | Cuộn 2: 200m | Cuộn 3: 200m", display1)

        // Deduct next 100m -> Roll 1 has only 50m, so SpoolManager preserves Roll 1 (50m) as leftover and cuts 100m from Roll 2 (200m -> 100m)
        val (afterSecondSale, display2) = SpoolManager.deductFromSpools(afterFirstSale, 100.0)
        assertEquals(listOf(50.0, 100.0, 200.0), afterSecondSale)
        assertEquals("Cuộn 1: 50m | Cuộn 2: 100m | Cuộn 3: 200m", display2)
        assertTrue(afterSecondSale.size >= 2)
    }
}
