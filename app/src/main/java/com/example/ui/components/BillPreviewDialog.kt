package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BankAccount
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.ui.auth.UserRole
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RedAlert
import com.example.util.PdfExporter
import com.example.util.QrCodeUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BillPreviewDialog(
    invoice: Invoice,
    items: List<InvoiceItem>,
    bankAccount: BankAccount? = null,
    currentRole: UserRole = UserRole.ADMIN,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Fallback bank account if none provided
    val activeBank = bankAccount ?: BankAccount(
        bankName = "MB Bank",
        bankCode = "MB",
        accountNumber = "0369087887",
        accountHolder = "HỆ THỐNG VẬT TƯ XÂY DỰNG",
        isDefault = true
    )

    // Generate Payment QR code with Bank Account and Calculated Amount
    LaunchedEffect(invoice.invoiceCode, invoice.finalAmount, activeBank.accountNumber) {
        val payload = QrCodeUtil.buildPaymentQrPayload(
            bankAccount = activeBank,
            amount = invoice.finalAmount,
            invoiceCode = invoice.invoiceCode
        )
        qrBitmap = QrCodeUtil.generateQrBitmap(payload, 380)
    }

    val hasNonRefundableItems = items.any { it.isNonRefundable }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.95f)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Dialog Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hóa Đơn Thanh Toán",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_bill_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Scrollable Bill Content
                Card(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        item {
                            // Header Store Info (HOTLINE REMOVED AS REQUESTED)
                            Text(
                                text = "HỆ THỐNG VẬT TƯ & THIẾT BỊ XÂY DỰNG",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                color = BluePrimary
                            )
                            Text(
                                text = "Mã hóa đơn: ${invoice.invoiceCode}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(invoice.createdAt))
                            Text(
                                text = "Ngày lập: $dateStr",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (invoice.isRefunded) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .background(RedAlert.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "ĐÃ HOÀN TRẢ HÀNG & NHẬP LẠI KHO",
                                        color = RedAlert,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            // Customer info
                            Text("Khách hàng: ${invoice.customerName}", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                            if (invoice.customerPhone.isNotBlank()) {
                                Text("Số điện thoại: ${invoice.customerPhone}", fontSize = 12.sp)
                            }
                            Text("Hình thức: ${invoice.paymentMethod}", fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(12.dp))

                            // Table Header with item quantities
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                                    .padding(vertical = 6.dp, horizontal = 4.dp)
                            ) {
                                Text("Hàng hóa / Số lượng", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("SL", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                                Text("Đ.Giá", modifier = Modifier.weight(1.1f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End)
                                Text("T.Tiền", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End)
                            }
                        }

                        // Line items with exact quantities
                        items(items) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(item.productName, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    when {
                                        item.isPipeProduct -> Text("(Ống nước - mua rồi không đổi lại)", fontSize = 10.sp, color = Color(0xFF0369A1), fontWeight = FontWeight.SemiBold)
                                        item.isWireProduct -> Text("(Dây điện - không trả tiền hàng)", fontSize = 10.sp, color = Color(0xFFB45309), fontWeight = FontWeight.SemiBold)
                                        else -> Text("(${item.unit})", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                                Text(
                                    text = if (item.quantity % 1.0 == 0.0) item.quantity.toLong().toString() else "%.1f".format(item.quantity),
                                    modifier = Modifier.weight(0.7f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = PdfExporter.formatVnd(item.unitPrice),
                                    modifier = Modifier.weight(1.1f),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = PdfExporter.formatVnd(item.total),
                                    modifier = Modifier.weight(1.2f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.End
                                )
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            // Total Section (Số tiền cần tính toán)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SỐ TIỀN CẦN THANH TOÁN:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BluePrimary)
                                Text(
                                    text = PdfExporter.formatVnd(invoice.finalAmount),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = EmeraldSuccess
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Payment QR Code containing Bank Account and Calculated Amount
                            qrBitmap?.let { bmp ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.QrCode2, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "MÃ QR THANH TOÁN CHUYỂN KHOẢN",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BluePrimary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Mã QR thanh toán ngân hàng",
                                            modifier = Modifier
                                                .size(140.dp)
                                                .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(8.dp))
                                                .background(Color.White)
                                                .padding(6.dp)
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "Quét mã để chuyển khoản chính xác: ${PdfExporter.formatVnd(invoice.finalAmount)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldSuccess
                                        )

                                        // BANK ACCOUNT DISPLAY POLICY:
                                        // "nhân viên không thể xem được tài khoản ngân hàng"
                                        if (currentRole == UserRole.ADMIN) {
                                            Text(
                                                text = "${activeBank.bankName} • STK: ${activeBank.accountNumber} • ${activeBank.accountHolder}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155),
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "(Thông tin tài khoản ngân hàng được mã hóa bảo mật bởi Quản trị viên)",
                                                fontSize = 10.sp,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // NOTICE: WIRE AND PIPE CANNOT BE RETURNED
                            // "đối với vật tư ống nước mua rồi không được đổi lại, đối với dây điện không thể trả tiền hàng"
                            if (hasNonRefundableItems) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp),
                                    color = RedAlert.copy(alpha = 0.08f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, RedAlert.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = RedAlert, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "QUY ĐỊNH CỬA HÀNG: Đối với vật tư ống nước mua rồi không được đổi lại, và đối với dây điện thì không thể trả lại tiền hàng sau khi đã cắt/xuất bán.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RedAlert
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // THANK YOU NOTE (No Hotline)
                            Text(
                                text = Invoice.THANK_YOU_NOTE,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).testTag("dialog_dismiss_button")
                    ) {
                        Text("Đóng")
                    }

                    Button(
                        onClick = {
                            val pdfFile = PdfExporter.createInvoicePdf(
                                context = context,
                                invoice = invoice,
                                items = items,
                                qrBitmap = qrBitmap,
                                bankAccount = activeBank,
                                isAdmin = currentRole == UserRole.ADMIN
                            )
                            if (pdfFile != null) {
                                PdfExporter.sharePdf(context, pdfFile)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.weight(1.5f).testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("In / Xuất PDF")
                    }
                }
            }
        }
    }
}
