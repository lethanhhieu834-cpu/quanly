package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.model.BankAccount
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

object QrCodeUtil {

    /**
     * Generates a QR code Bitmap for general content or invoice code.
     */
    fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
        return try {
            val bitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size
            )
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates standard bank transfer QR payload containing:
     * - Bank code & name
     * - Bank account number (Số tài khoản)
     * - Account holder name (Tên chủ TK)
     * - Calculated payment amount (Số tiền cần tính toán)
     * - Invoice code (Nội dung chuyển khoản)
     */
    fun buildPaymentQrPayload(
        bankAccount: BankAccount,
        amount: Double,
        invoiceCode: String
    ): String {
        val code = bankAccount.bankCode.ifBlank { "MB" }.uppercase()
        val accNum = bankAccount.accountNumber.trim()
        val holder = bankAccount.accountHolder.trim()
        val amt = amount.toLong().coerceAtLeast(0L)
        // VietQR standard payment payload URL readable by banking apps & scanners
        return "https://img.vietqr.io/image/$code-$accNum-compact2.png?amount=$amt&addInfo=$invoiceCode&accountName=$holder"
    }

    /**
     * Decodes QR code from a Bitmap image.
     */
    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap)
            result.text
        } catch (e: Exception) {
            null
        }
    }
}
