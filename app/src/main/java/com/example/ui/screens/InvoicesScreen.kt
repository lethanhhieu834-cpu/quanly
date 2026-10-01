package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.ui.VatTuViewModel
import com.example.ui.auth.UserRole
import com.example.ui.components.BillPreviewDialog
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RedAlert
import com.example.util.PdfExporter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoicesScreen(
    viewModel: VatTuViewModel,
    highlightInvoiceCode: String? = null
) {
    val invoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val defaultBank by viewModel.defaultBankAccount.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf(highlightInvoiceCode ?: "") }
    var selectedInvoiceForDetail by remember { mutableStateOf<Invoice?>(null) }
    var itemsForSelectedInvoice by remember { mutableStateOf<List<InvoiceItem>>(emptyList()) }

    var invoiceToRefund by remember { mutableStateOf<Invoice?>(null) }
    var itemsForRefundInvoice by remember { mutableStateOf<List<InvoiceItem>>(emptyList()) }
    var refundReason by remember { mutableStateOf("Khách trả hàng thừa") }

    var invoiceToRevoke by remember { mutableStateOf<Invoice?>(null) }

    val filteredInvoices = invoices.filter { inv ->
        searchQuery.isBlank() ||
                inv.invoiceCode.contains(searchQuery.trim(), true) ||
                inv.customerName.contains(searchQuery.trim(), true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Header (HOTLINE REMOVED AS REQUESTED)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Lịch Sử Hóa Đơn & Trả Hàng",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
                Text(
                    text = "Định dạng mã hóa đơn: HD-ddMMyyyyHHmmss",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search by Invoice Code
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Tra cứu mã hóa đơn HD-... hoặc tên khách...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BluePrimary) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("invoice_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredInvoices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Không tìm thấy hóa đơn nào.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 70.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredInvoices, key = { it.id }) { invoice ->
                    InvoiceHistoryCard(
                        invoice = invoice,
                        isAdmin = currentRole == UserRole.ADMIN,
                        onViewDetail = {
                            coroutineScope.launch {
                                val items = viewModel.repository.getInvoiceItemsSync(invoice.invoiceCode)
                                itemsForSelectedInvoice = items
                                selectedInvoiceForDetail = invoice
                            }
                        },
                        onRefundClick = {
                            coroutineScope.launch {
                                val items = viewModel.repository.getInvoiceItemsSync(invoice.invoiceCode)
                                itemsForRefundInvoice = items
                                invoiceToRefund = invoice
                            }
                        },
                        onRevokeClick = { invoiceToRevoke = invoice }
                    )
                }
            }
        }
    }

    // Bill Details Dialog with Payment QR and PDF option
    selectedInvoiceForDetail?.let { inv ->
        BillPreviewDialog(
            invoice = inv,
            items = itemsForSelectedInvoice,
            bankAccount = defaultBank,
            currentRole = currentRole,
            onDismiss = { selectedInvoiceForDetail = null }
        )
    }

    // Refund Dialog with electrical wire & pipe non-refundable rule:
    // "đối với vật tư ống nước mua rồi không được đổi lại, đối với dây điện thì bạn không thể trả tiền hàng"
    invoiceToRefund?.let { inv ->
        val nonRefundableItems = itemsForRefundInvoice.filter { it.isNonRefundable }
        val hasNonRefundable = nonRefundableItems.isNotEmpty()

        if (hasNonRefundable) {
            // BLOCK REFUND FOR ELECTRICAL WIRE AND WATER PIPES
            AlertDialog(
                onDismissRequest = { invoiceToRefund = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = RedAlert)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Không Thể Đổi Lại / Trả Tiền Hàng", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RedAlert)
                    }
                },
                text = {
                    Column {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RedAlert.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "QUY ĐỊNH CỬA HÀNG: Đối với vật tư ống nước mua rồi không được đổi lại, và đối với dây điện không thể trả tiền hàng!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = RedAlert
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ống nước và dây điện đã xuất bán / cắt theo mét không được phép hoàn trả tiền hay nhập lại kho.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF7F1D1D)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Các mặt hàng không được đổi trả trong hóa đơn này:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        nonRefundableItems.forEach { item ->
                            Text(
                                text = "• ${item.productName} (SL: ${if (item.quantity % 1.0 == 0.0) item.quantity.toLong() else item.quantity} ${item.unit}) - ${PdfExporter.formatVnd(item.total)}",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { invoiceToRefund = null },
                        colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
                    ) {
                        Text("Đã hiểu quy định")
                    }
                }
            )
        } else {
            // Standard refund dialog for non-wire products
            AlertDialog(
                onDismissRequest = { invoiceToRefund = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Undo, contentDescription = null, tint = AmberAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hoàn Trả Đơn & Nhập Lại Kho", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Hệ thống sẽ hoàn lại số tiền ${PdfExporter.formatVnd(inv.finalAmount)} cho khách hàng và TỰ ĐỘNG CẬP NHẬT TĂNG LẠI SỐ LƯỢNG HÀNG VÀO KHO.",
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = refundReason,
                            onValueChange = { refundReason = it },
                            label = { Text("Lý do hoàn trả") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.processRefund(inv.invoiceCode, refundReason)
                            invoiceToRefund = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                    ) {
                        Text("Xác Nhận Hoàn Trả")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { invoiceToRefund = null }) { Text("Hủy") }
                }
            )
        }
    }

    // Revoke & Delete Invoice (Admin only)
    invoiceToRevoke?.let { inv ->
        AlertDialog(
            onDismissRequest = { invoiceToRevoke = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RedAlert)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Thu Hồi & Xóa Vĩnh Viễn Hóa Đơn", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RedAlert)
                }
            },
            text = {
                Text(
                    text = "CẢNH BÁO (Quyền Quản Trị Viên): Bạn có chắc chắn muốn thu hồi và xóa hoàn toàn hóa đơn ${inv.invoiceCode} khỏi hệ thống?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.revokeInvoice(inv.invoiceCode)
                        invoiceToRevoke = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
                ) {
                    Text("Xóa Vĩnh Viễn")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { invoiceToRevoke = null }) { Text("Hủy") }
            }
        )
    }
}

@Composable
fun InvoiceHistoryCard(
    invoice: Invoice,
    isAdmin: Boolean,
    onViewDetail: () -> Unit,
    onRefundClick: () -> Unit,
    onRevokeClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetail() }
            .testTag("invoice_card_${invoice.invoiceCode}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = invoice.invoiceCode,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = BluePrimary
                )
                Text(
                    text = PdfExporter.formatVnd(invoice.finalAmount),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = if (invoice.isRefunded) RedAlert else EmeraldSuccess
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Khách: ${invoice.customerName}" + if (invoice.customerPhone.isNotBlank()) " (${invoice.customerPhone})" else "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(invoice.createdAt))
                Text(text = dateStr, fontSize = 11.sp, color = Color.Gray)
            }

            if (invoice.discountAmount > 0) {
                Text(
                    text = "Đã chiết khấu cho khách: ${PdfExporter.formatVnd(invoice.discountAmount)} (${invoice.discountPercent}%)",
                    fontSize = 11.sp,
                    color = AmberAccent
                )
            }

            if (invoice.isRefunded) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .background(RedAlert.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ĐÃ HOÀN TRẢ & NHẬP LẠI KHO (${invoice.refundReason.ifBlank { "Khách trả hàng" }})",
                        color = RedAlert,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onViewDetail,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Xem bill & QR", fontSize = 11.sp)
                }

                if (!invoice.isRefunded) {
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = onRefundClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hoàn trả", fontSize = 11.sp, color = AmberAccent)
                    }
                }

                // Revoke button: Admin only
                if (isAdmin) {
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = onRevokeClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RedAlert, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thu hồi", fontSize = 11.sp, color = RedAlert)
                    }
                }
            }
        }
    }
}
