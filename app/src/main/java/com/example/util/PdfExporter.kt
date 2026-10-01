package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.BankAccount
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    fun formatVnd(amount: Double): String {
        return currencyFormatter.format(amount).replace("₫", "đ")
    }

    /**
     * Generates a printable PDF invoice with Payment QR and returns the File.
     * HOTLINE REMOVED AS REQUESTED.
     */
    fun createInvoicePdf(
        context: Context,
        invoice: Invoice,
        items: List<InvoiceItem>,
        qrBitmap: Bitmap?,
        bankAccount: BankAccount? = null,
        isAdmin: Boolean = true
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }

        // Header Background
        val headerPaint = Paint().apply {
            color = Color.rgb(30, 58, 138) // Deep Blue
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 595f, 80f, headerPaint)

        // Store Title (NO HOTLINE)
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("HỆ THỐNG VẬT TƯ & THIẾT BỊ XÂY DỰNG", 30f, 48f, paint)

        // Invoice Code & Date
        paint.color = Color.BLACK
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("HÓA ĐƠN BÁN HÀNG", 30f, 120f, paint)

        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Mã hóa đơn: ${invoice.invoiceCode}", 30f, 142f, paint)

        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(invoice.createdAt))
        canvas.drawText("Thời gian: $dateStr", 30f, 162f, paint)
        canvas.drawText("Khách hàng: ${invoice.customerName}", 30f, 182f, paint)
        if (invoice.customerPhone.isNotBlank()) {
            canvas.drawText("Số điện thoại: ${invoice.customerPhone}", 30f, 202f, paint)
        }
        canvas.drawText("Phương thức thanh toán: ${invoice.paymentMethod}", 30f, 222f, paint)

        // Draw Bank Payment QR Code on top right (Contains Bank Account and Calculated Amount)
        qrBitmap?.let {
            val scaledQr = Bitmap.createScaledBitmap(it, 115, 115, false)
            canvas.drawBitmap(scaledQr, 440f, 105f, null)
            paint.textSize = 8.5f
            paint.color = Color.rgb(30, 58, 138)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("QR Thanh Toán Ngân Hàng", 435f, 230f, paint)
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.GRAY
            canvas.drawText("Số tiền: ${formatVnd(invoice.finalAmount)}", 435f, 242f, paint)
        }

        // Table Header
        var y = 260f
        paint.color = Color.rgb(241, 245, 249)
        paint.style = Paint.Style.FILL
        canvas.drawRect(30f, y, 565f, y + 26f, paint)

        paint.color = Color.BLACK
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("STT", 36f, y + 18f, paint)
        canvas.drawText("Tên hàng hóa / Vật tư", 70f, y + 18f, paint)
        canvas.drawText("ĐVT", 320f, y + 18f, paint)
        canvas.drawText("SL", 365f, y + 18f, paint)
        canvas.drawText("Đơn giá", 410f, y + 18f, paint)
        canvas.drawText("Thành tiền", 490f, y + 18f, paint)

        // Line items with exact quantities
        paint.typeface = Typeface.DEFAULT
        y += 28f
        val hasNonRefundable = items.any { it.isNonRefundable }

        items.forEachIndexed { index, item ->
            y += 22f
            paint.color = Color.DKGRAY
            canvas.drawText("${index + 1}", 36f, y, paint)
            val nameDisplay = if (item.productName.length > 28) item.productName.substring(0, 26) + ".." else item.productName
            canvas.drawText(nameDisplay, 70f, y, paint)
            canvas.drawText(item.unit, 320f, y, paint)
            canvas.drawText(if (item.quantity % 1.0 == 0.0) item.quantity.toLong().toString() else "%.1f".format(item.quantity), 365f, y, paint)
            canvas.drawText(formatVnd(item.unitPrice), 410f, y, paint)
            canvas.drawText(formatVnd(item.total), 490f, y, paint)

            val linePaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
            }
            canvas.drawLine(30f, y + 6f, 565f, y + 6f, linePaint)
        }

        // Total Section (Số tiền cần tính toán)
        y += 35f
        paint.color = Color.rgb(30, 58, 138)
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SỐ TIỀN CẦN THANH TOÁN:", 290f, y, paint)
        paint.color = Color.rgb(5, 150, 105)
        canvas.drawText(formatVnd(invoice.finalAmount), 460f, y, paint)

        // Status badge
        if (invoice.isRefunded) {
            paint.color = Color.RED
            paint.textSize = 13f
            canvas.drawText("[ĐÃ HOÀN TRẢ / THU HỒI HÀNG]", 30f, y, paint)
        }

        // Return policy warning if non-refundable items exist
        if (hasNonRefundable) {
            y += 30f
            paint.color = Color.rgb(180, 83, 9)
            paint.textSize = 10.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("• Quy định: Đối với vật tư ống nước và dây điện mua rồi không được đổi lại/trả tiền hàng.", 30f, y, paint)
        }

        // Footer Note
        y += 45f
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val textWidth = paint.measureText(Invoice.THANK_YOU_NOTE)
        canvas.drawText(Invoice.THANK_YOU_NOTE, (595f - textWidth) / 2f, y, paint)

        document.finishPage(page)

        // Write to file
        val outputDir = File(context.cacheDir, "invoices")
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, "${invoice.invoiceCode}.pdf")
        return try {
            val fos = FileOutputStream(file)
            document.writeTo(fos)
            document.close()
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            null
        }
    }

    data class ItemProfitSummary(
        val productName: String,
        val unit: String,
        val quantity: Double,
        val totalCost: Double,
        val totalRevenue: Double,
        val totalProfit: Double
    )

    /**
     * Creates a PDF report for daily, monthly, or yearly revenue and profits.
     * HOTLINE REMOVED.
     */
    fun createRevenueReportPdf(
        context: Context,
        reportPeriodTitle: String,
        grossRevenue: Double,
        discountsGiven: Double,
        netRevenue: Double,
        totalCostOfGoods: Double,
        grossProfit: Double,
        expensesTotal: Double,
        finalNetProfit: Double,
        invoicesCount: Int,
        lowStockItems: List<String>,
        itemSummaries: List<ItemProfitSummary> = emptyList()
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // Header
        paint.color = Color.rgb(30, 58, 138)
        canvas.drawRect(0f, 0f, 595f, 80f, paint)

        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BÁO CÁO DOANH THU, VỐN & LỢI NHUẬN", 30f, 40f, paint)

        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Kỳ báo cáo: $reportPeriodTitle", 30f, 65f, paint)

        // Financial Summary Box
        var y = 110f
        paint.color = Color.BLACK
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("1. TỔNG HỢP DOANH THU & TIỀN LỜI TRONG NGÀY", 30f, y, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 11f
        y += 24f
        canvas.drawText("Số lượng hóa đơn bán hoàn tất: $invoicesCount", 40f, y, paint)
        y += 18f
        canvas.drawText("Tổng doanh thu bán ra: ${formatVnd(netRevenue)}", 40f, y, paint)
        if (discountsGiven > 0) {
            canvas.drawText(" (Đã trừ chiết khấu cho khách: -${formatVnd(discountsGiven)})", 320f, y, paint)
        }
        y += 18f
        paint.color = Color.rgb(180, 83, 9)
        canvas.drawText("Tổng tiền vốn hàng bán ra: -${formatVnd(totalCostOfGoods)}", 40f, y, paint)

        y += 20f
        paint.color = Color.rgb(30, 58, 138)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("Tiền lời gộp từ bán hàng (Doanh thu - Vốn): ${formatVnd(grossProfit)}", 40f, y, paint)

        y += 20f
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 11f
        paint.color = Color.rgb(220, 38, 38)
        canvas.drawText("Các chi phí khác (Điện, nước, lương nhân viên): -${formatVnd(expensesTotal)}", 40f, y, paint)

        // SỐ CUỐI: TIỀN LỜI THỰC TẾ
        y += 26f
        val boxPaint = Paint().apply {
            color = if (finalNetProfit >= 0) Color.rgb(240, 253, 244) else Color.rgb(254, 242, 242)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(30f, y - 18f, 565f, y + 26f, 8f, 8f, boxPaint)

        val borderPaint = Paint().apply {
            color = if (finalNetProfit >= 0) Color.rgb(5, 150, 105) else Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(30f, y - 18f, 565f, y + 26f, 8f, 8f, borderPaint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        paint.color = if (finalNetProfit >= 0) Color.rgb(5, 150, 105) else Color.RED
        canvas.drawText("SỐ CUỐI: TIỀN LỜI THỰC TẾ SAU MỘT NGÀY:", 40f, y + 8f, paint)
        canvas.drawText(formatVnd(finalNetProfit), 380f, y + 8f, paint)

        // Item-by-item table
        y += 45f
        paint.color = Color.BLACK
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("2. CHI TIẾT LỢI NHUẬN TỪNG SẢN PHẨM ĐÃ BÁN", 30f, y, paint)

        y += 18f
        paint.color = Color.rgb(241, 245, 249)
        paint.style = Paint.Style.FILL
        canvas.drawRect(30f, y, 565f, y + 20f, paint)

        paint.color = Color.BLACK
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Tên hàng hóa / Vật tư", 36f, y + 14f, paint)
        canvas.drawText("SL bán", 250f, y + 14f, paint)
        canvas.drawText("Tiền vốn", 320f, y + 14f, paint)
        canvas.drawText("Doanh thu", 400f, y + 14f, paint)
        canvas.drawText("Tiền lời", 490f, y + 14f, paint)

        paint.typeface = Typeface.DEFAULT
        y += 20f
        val topItems = itemSummaries.take(12)
        topItems.forEach { item ->
            y += 18f
            paint.color = Color.DKGRAY
            val nameDisplay = if (item.productName.length > 25) item.productName.substring(0, 23) + ".." else item.productName
            canvas.drawText(nameDisplay, 36f, y, paint)
            canvas.drawText("${if (item.quantity % 1.0 == 0.0) item.quantity.toLong() else "%.1f".format(item.quantity)} ${item.unit}", 250f, y, paint)
            canvas.drawText(formatVnd(item.totalCost), 320f, y, paint)
            canvas.drawText(formatVnd(item.totalRevenue), 400f, y, paint)
            paint.color = if (item.totalProfit >= 0) Color.rgb(5, 150, 105) else Color.RED
            canvas.drawText(formatVnd(item.totalProfit), 490f, y, paint)

            val linePaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 0.8f
            }
            canvas.drawLine(30f, y + 4f, 565f, y + 4f, linePaint)
        }

        // Low Stock alert section
        y += 28f
        paint.color = Color.BLACK
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("3. CẢNH BÁO TỒN KHO DƯỚI 5 ĐƠN VỊ", 30f, y, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 10f
        y += 16f
        if (lowStockItems.isEmpty()) {
            paint.color = Color.rgb(5, 150, 105)
            canvas.drawText("Tất cả hàng hóa trong kho đều an toàn (>= 5 đơn vị).", 40f, y, paint)
        } else {
            paint.color = Color.rgb(220, 38, 38)
            lowStockItems.take(5).forEach { item ->
                y += 15f
                canvas.drawText("• $item", 40f, y, paint)
            }
        }

        // Footer
        y = 810f
        paint.color = Color.GRAY
        paint.textSize = 9f
        val genDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Báo cáo xuất tự động từ Hệ Thống Quản Lý Vật Tư lúc $genDate", 30f, y, paint)

        document.finishPage(page)

        val outputDir = File(context.cacheDir, "reports")
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, "BaoCao_${System.currentTimeMillis()}.pdf")
        return try {
            val fos = FileOutputStream(file)
            document.writeTo(fos)
            document.close()
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            null
        }
    }

    /**
     * Shares or opens the PDF via Intent.
     */
    fun sharePdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Chia sẻ file PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
