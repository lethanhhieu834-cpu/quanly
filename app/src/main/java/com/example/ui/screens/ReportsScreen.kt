package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.InvoiceItem
import com.example.ui.VatTuViewModel
import com.example.ui.auth.UserRole
import com.example.ui.components.AuthDialog
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RedAlert
import com.example.util.PdfExporter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: VatTuViewModel
) {
    val context = LocalContext.current
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val isAdmin = currentRole == UserRole.ADMIN

    // CRITICAL USER REQUIREMENT: "nhân viên không được xem báo cáo doanh thu"
    if (!isAdmin) {
        var showPinDialog by remember { mutableStateOf(false) }

        if (showPinDialog) {
            AuthDialog(
                currentRole = currentRole,
                onDismiss = { showPinDialog = false },
                onPinSubmit = { pin ->
                    val ok = viewModel.switchRole(pin)
                    if (ok) showPinDialog = false
                    ok
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(RedAlert.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Bị khóa",
                            tint = RedAlert,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "GIỚI HẠN QUYỀN TRUY CẬP",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = RedAlert
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tài khoản Nhân viên không được phép xem báo cáo doanh thu, tiền vốn và lợi nhuận bán hàng.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Chỉ Quản Trị Viên mới có quyền xem các số liệu tài chính này.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showPinDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.SupervisorAccount, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Đăng nhập Quản Trị Viên (PIN)")
                    }
                }
            }
        }
        return
    }

    val invoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val allInvoiceItems by viewModel.allInvoiceItems.collectAsStateWithLifecycle()
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()

    var selectedPeriodTab by remember { mutableStateOf(0) } // 0: Hôm nay, 1: Tháng này, 2: Năm nay

    val now = Calendar.getInstance()
    val todaySdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val monthSdf = SimpleDateFormat("MM/yyyy", Locale.getDefault())
    val yearSdf = SimpleDateFormat("yyyy", Locale.getDefault())

    val todayStr = todaySdf.format(now.time)
    val thisMonthStr = monthSdf.format(now.time)
    val thisYearStr = yearSdf.format(now.time)

    // Filter invoices by selected period
    val relevantInvoices = invoices.filter { inv ->
        val invDate = Date(inv.createdAt)
        when (selectedPeriodTab) {
            0 -> todaySdf.format(invDate) == todayStr
            1 -> monthSdf.format(invDate) == thisMonthStr
            else -> yearSdf.format(invDate) == thisYearStr
        }
    }

    // Filter non-refunded invoices for revenue & cost calculations
    val completedInvoices = relevantInvoices.filter { !it.isRefunded }
    val completedInvoiceCodes = completedInvoices.map { it.invoiceCode }.toSet()

    // Relevant sold items for completed invoices
    val relevantItems = allInvoiceItems.filter { it.invoiceCode in completedInvoiceCodes }

    // 1. Doanh thu bán ra (Niêm yết, chiết khấu và thực tế)
    val grossRevenue = completedInvoices.sumOf { it.totalAmount }
    val discountsGiven = completedInvoices.sumOf { it.discountAmount }
    val netRevenue = completedInvoices.sumOf { it.finalAmount } // = grossRevenue - discountsGiven

    // 2. Tiền vốn của các sản phẩm bán ra: Lấy tổng (số lượng * giá vốn)
    val totalCostOfGoods = relevantItems.sumOf { it.quantity * it.costPrice }

    // 3. Tiền lời gộp từ bán hàng: Lấy doanh thu trừ cho giá vốn của mỗi sản phẩm bán ra
    val grossProfit = netRevenue - totalCostOfGoods

    // 4. Chi phí khác trong kỳ (Điện, nước, lương nhân viên...)
    val relevantExpenses = if (isAdmin) {
        expenses.filter { exp ->
            when (selectedPeriodTab) {
                0 -> exp.dateStr == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
                1 -> exp.monthYear == thisMonthStr
                else -> exp.dateStr.startsWith(thisYearStr)
            }
        }
    } else emptyList()

    val totalExpenses = relevantExpenses.sumOf { it.amount }

    // 5. SỐ CUỐI TIỀN LỜI SAU MỘT NGÀY / KỲ BÁO CÁO: Tiền lời gộp - Chi phí khác
    val finalNetProfit = grossProfit - totalExpenses

    // Bảng chi tiết từng sản phẩm bán ra trong kỳ
    val itemProfitSummaries = relevantItems
        .groupBy { it.productId }
        .map { (_, items) ->
            val first = items.first()
            val totalQty = items.sumOf { it.quantity }
            val itemCost = items.sumOf { it.quantity * it.costPrice }
            val itemRev = items.sumOf { it.total }
            val itemProfit = itemRev - itemCost
            PdfExporter.ItemProfitSummary(
                productName = first.productName,
                unit = first.unit,
                quantity = totalQty,
                totalCost = itemCost,
                totalRevenue = itemRev,
                totalProfit = itemProfit
            )
        }.sortedByDescending { it.totalProfit }

    val periodTitle = when (selectedPeriodTab) {
        0 -> "Hôm nay ($todayStr)"
        1 -> "Tháng này ($thisMonthStr)"
        else -> "Năm nay ($thisYearStr)"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Báo Cáo Doanh Thu & Lợi Nhuận",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
                Text(
                    text = "Doanh thu trừ tiền vốn từng sản phẩm, chi phí và số cuối tiền lời",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Button(
                onClick = {
                    val lowStockList = lowStockProducts.map { "${it.name}: Còn ${it.quantity} ${it.unit} (<5)" }
                    val pdfFile = PdfExporter.createRevenueReportPdf(
                        context = context,
                        reportPeriodTitle = periodTitle,
                        grossRevenue = grossRevenue,
                        discountsGiven = discountsGiven,
                        netRevenue = netRevenue,
                        totalCostOfGoods = totalCostOfGoods,
                        grossProfit = grossProfit,
                        expensesTotal = totalExpenses,
                        finalNetProfit = finalNetProfit,
                        invoicesCount = completedInvoices.size,
                        lowStockItems = lowStockList,
                        itemSummaries = itemProfitSummaries
                    )
                    if (pdfFile != null) {
                        PdfExporter.sharePdf(context, pdfFile)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("export_report_pdf_button")
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Xuất PDF", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Period Tabs: Hôm Nay, Tháng Này, Năm Nay
        ScrollableTabRow(
            selectedTabIndex = selectedPeriodTab,
            edgePadding = 0.dp
        ) {
            Tab(selected = selectedPeriodTab == 0, onClick = { selectedPeriodTab = 0 }, text = { Text("Hôm Nay") })
            Tab(selected = selectedPeriodTab == 1, onClick = { selectedPeriodTab = 1 }, text = { Text("Tháng Này") })
            Tab(selected = selectedPeriodTab == 2, onClick = { selectedPeriodTab = 2 }, text = { Text("Năm Nay") })
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 70.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // CARD 1: TỔNG HỢP TÀI CHÍNH & SỐ CUỐI TIỀN LỜI
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BẢNG TỔNG HỢP: $periodTitle",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = BluePrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 1. Doanh thu bán trong ngày / kỳ
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("1. Doanh thu bán hàng thực tế:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(PdfExporter.formatVnd(netRevenue), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                        }

                        if (discountsGiven > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("   (Chiết khấu % cho khách đã giảm):", fontSize = 11.sp, color = AmberAccent)
                                Text("-${PdfExporter.formatVnd(discountsGiven)}", fontSize = 11.sp, color = AmberAccent)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // 2. Tiền vốn hàng bán ra (COGS)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("2. Tiền vốn hàng bán ra (Giá vốn):", fontSize = 13.sp, color = Color(0xFFB45309))
                            Text(
                                "-${PdfExporter.formatVnd(totalCostOfGoods)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // 3. Tiền lời gộp từ bán hàng (Doanh thu - Vốn)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("3. Tiền lời từ bán hàng (Doanh thu - Vốn):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                PdfExporter.formatVnd(grossProfit),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (grossProfit >= 0) EmeraldSuccess else RedAlert
                            )
                        }

                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(6.dp))

                            // 4. Chi phí khác (vận hành, điện nước, lương)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("4. Chi phí khác (Điện, nước, lương...):", fontSize = 13.sp, color = RedAlert)
                                Text("-${PdfExporter.formatVnd(totalExpenses)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RedAlert)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            // 5. SỐ CUỐI TIỀN LỜI SAU MỘT NGÀY
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (finalNetProfit >= 0) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "SỐ CUỐI TIỀN LỜI THỰC TẾ:",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = if (finalNetProfit >= 0) Color(0xFF065F46) else Color(0xFF991B1B)
                                        )
                                        Text(
                                            text = "Sau khi trừ vốn và chi phí khác",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Text(
                                        text = PdfExporter.formatVnd(finalNetProfit),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = if (finalNetProfit >= 0) EmeraldSuccess else RedAlert
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "(Đăng nhập Quản Trị Viên để xem chi phí vận hành & lợi nhuận ròng sau chi phí)",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Số lượng hóa đơn: ${completedInvoices.size} hoàn tất | ${relevantInvoices.count { it.isRefunded }} hoàn trả",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // CARD 2: BẢNG BÁO CÁO CHI TIẾT GỒM DOANH THU, TIỀN VỐN, TIỀN LỜI TỪNG MẶT HÀNG
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BẢNG CHI TIẾT SẢN PHẨM ĐÃ BÁN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BluePrimary
                            )
                        }
                        Text(
                            text = "Doanh thu, tiền vốn và tiền lời từng sản phẩm bán ra",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (itemProfitSummaries.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Chưa có đơn hàng nào được bán trong kỳ này.", color = Color.Gray, fontSize = 12.sp)
                            }
                        } else {
                            // Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Tên vật tư", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2f))
                                Text("SL bán", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("Tiền vốn", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
                                Text("Doanh thu", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.4f), textAlign = TextAlign.End)
                                Text("Tiền lời", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.4f), textAlign = TextAlign.End)
                            }

                            // Table Rows
                            itemProfitSummaries.forEachIndexed { index, item ->
                                val rowBg = if (index % 2 == 0) Color.Transparent else Color(0xFFF8FAFC)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBg)
                                        .padding(vertical = 8.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(2f)) {
                                        Text(
                                            text = item.productName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }

                                    Text(
                                        text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toLong() else "%.1f".format(item.quantity)} ${item.unit}",
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = PdfExporter.formatVnd(item.totalCost),
                                        fontSize = 11.sp,
                                        color = Color(0xFFB45309),
                                        modifier = Modifier.weight(1.3f),
                                        textAlign = TextAlign.End
                                    )

                                    Text(
                                        text = PdfExporter.formatVnd(item.totalRevenue),
                                        fontSize = 11.sp,
                                        color = BluePrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1.4f),
                                        textAlign = TextAlign.End
                                    )

                                    Text(
                                        text = PdfExporter.formatVnd(item.totalProfit),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.totalProfit >= 0) EmeraldSuccess else RedAlert,
                                        modifier = Modifier.weight(1.4f),
                                        textAlign = TextAlign.End
                                    )
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE2E8F0))
                            }

                            // Table Total Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFEFF6FF), RoundedCornerShape(6.dp))
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("TỔNG CỘNG", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BluePrimary, modifier = Modifier.weight(3f))
                                Text(
                                    PdfExporter.formatVnd(totalCostOfGoods),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.weight(1.3f),
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    PdfExporter.formatVnd(grossRevenue),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = BluePrimary,
                                    modifier = Modifier.weight(1.4f),
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    PdfExporter.formatVnd(grossRevenue - totalCostOfGoods),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = EmeraldSuccess,
                                    modifier = Modifier.weight(1.4f),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }

            // CARD 3: CÁC KHOẢN CHI PHÍ KHÁC TRONG KỲ (Admin only)
            if (isAdmin && relevantExpenses.isNotEmpty()) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingDown, contentDescription = null, tint = RedAlert, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CÁC CHI PHÍ VẬN HÀNH KHÁC", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RedAlert)
                                }
                                Text("-${PdfExporter.formatVnd(totalExpenses)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RedAlert)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            relevantExpenses.forEach { exp ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(exp.title, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text("${exp.category} • ${exp.dateStr}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Text("-${PdfExporter.formatVnd(exp.amount)}", fontSize = 12.sp, color = RedAlert, fontWeight = FontWeight.SemiBold)
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }
            }

            // CARD 4: CẢNH BÁO TỒN KHO DƯỚI 5 ĐƠN VỊ TRONG NGÀY
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (lowStockProducts.isNotEmpty()) RedAlert.copy(alpha = 0.08f) else Color(0xFFF0FDF4)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (lowStockProducts.isNotEmpty()) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (lowStockProducts.isNotEmpty()) RedAlert else EmeraldSuccess
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BÁO CÁO HÀNG HÓA TỒN KHO TRONG NGÀY",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (lowStockProducts.isNotEmpty()) RedAlert else EmeraldSuccess
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (lowStockProducts.isEmpty()) {
                            Text("Tất cả các sản phẩm đều đạt định mức tồn kho an toàn (>= 5 đơn vị).", fontSize = 12.sp, color = EmeraldSuccess)
                        } else {
                            Text(
                                text = "Có ${lowStockProducts.size} sản phẩm tồn kho dưới 5 đơn vị cần nhập gấp:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RedAlert
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            lowStockProducts.forEach { prod ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• ${prod.name}", fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    Text(
                                        "${if (prod.quantity % 1.0 == 0.0) prod.quantity.toLong() else prod.quantity} ${prod.unit}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = RedAlert
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
