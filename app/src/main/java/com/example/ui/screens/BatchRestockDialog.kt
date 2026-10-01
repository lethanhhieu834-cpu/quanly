package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BatchRestockItem
import com.example.data.model.Product
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RedAlert
import com.example.util.PdfExporter
import com.example.util.SpoolManager
import java.util.Locale

/**
 * State for each product selected for batch restock
 */
class RestockEntryState(
    val product: Product,
    initialQuantity: Double = 10.0,
    initialCostPrice: Double = product.costPrice,
    initialMarkup: Double = if (product.markupPercent > 0) product.markupPercent else 25.0,
    initialSellPrice: Double = product.sellPrice
) {
    var quantityText by mutableStateOf(if (initialQuantity % 1.0 == 0.0) initialQuantity.toLong().toString() else initialQuantity.toString())
    var costPriceText by mutableStateOf(initialCostPrice.toLong().toString())
    var markupText by mutableStateOf(if (initialMarkup % 1.0 == 0.0) initialMarkup.toLong().toString() else "%.1f".format(Locale.US, initialMarkup))
    var sellPriceText by mutableStateOf(initialSellPrice.toLong().toString())

    val quantity: Double get() = quantityText.toDoubleOrNull() ?: 0.0
    val costPrice: Double get() = costPriceText.toDoubleOrNull() ?: 0.0
    val markupPercent: Double get() = markupText.toDoubleOrNull() ?: 0.0
    val sellPrice: Double get() = sellPriceText.toDoubleOrNull() ?: 0.0

    val totalCost: Double get() = quantity * costPrice

    fun onCostChange(newCostStr: String) {
        costPriceText = newCostStr
        val cost = newCostStr.toDoubleOrNull() ?: 0.0
        val markup = markupText.toDoubleOrNull() ?: 0.0
        if (cost > 0) {
            val calcSell = cost * (1.0 + markup / 100.0)
            sellPriceText = calcSell.toLong().toString()
        }
    }

    fun onMarkupChange(newMarkupStr: String) {
        markupText = newMarkupStr
        val markup = newMarkupStr.toDoubleOrNull() ?: 0.0
        val cost = costPriceText.toDoubleOrNull() ?: 0.0
        if (cost > 0) {
            val calcSell = cost * (1.0 + markup / 100.0)
            sellPriceText = calcSell.toLong().toString()
        }
    }

    fun onSellPriceChange(newSellStr: String) {
        sellPriceText = newSellStr
        val sell = newSellStr.toDoubleOrNull() ?: 0.0
        val cost = costPriceText.toDoubleOrNull() ?: 0.0
        if (cost > 0) {
            val calcMarkup = ((sell - cost) / cost) * 100.0
            markupText = if (calcMarkup % 1.0 == 0.0) calcMarkup.toLong().toString() else "%.1f".format(Locale.US, calcMarkup)
        }
    }

    fun applyMarkupQuick(percent: Double) {
        markupText = if (percent % 1.0 == 0.0) percent.toLong().toString() else percent.toString()
        val cost = costPriceText.toDoubleOrNull() ?: 0.0
        if (cost > 0) {
            val calcSell = cost * (1.0 + percent / 100.0)
            sellPriceText = calcSell.toLong().toString()
        }
    }
}

/**
 * Dialog for Batch Goods Receipt ("Nhập Hàng Hàng Loạt Nhiều Loại Sản Phẩm"):
 * Features:
 * - Multi-select products from inventory
 * - For each selected product: sets new quantity, new cost price, desired markup %, new sell price
 * - Bidirectional auto-calculation between cost, markup % and sell price
 * - Preserves existing stock under old price batch (FIFO)
 * - Optional expense record creation
 */
@Composable
fun BatchRestockDialog(
    allProducts: List<Product>,
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirmRestock: (items: List<BatchRestockItem>, recordExpense: Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var recordExpense by remember { mutableStateOf(true) }

    // Map of ProductId -> RestockEntryState
    val selectedEntries = remember { mutableStateMapOf<Long, RestockEntryState>() }

    val filteredProducts = allProducts.filter { prod ->
        val matchesSearch = searchQuery.isBlank() || prod.name.contains(searchQuery, true) || prod.code.contains(searchQuery, true)
        val matchesCat = selectedCategory == null || prod.category == selectedCategory
        matchesSearch && matchesCat
    }

    val totalRestockCost = selectedEntries.values.sumOf { it.totalCost }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = BluePrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Inventory, contentDescription = null, tint = BluePrimary)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Nhập Hàng Hàng Loạt (Nhiều Sản Phẩm)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = BluePrimary
                            )
                            Text(
                                text = "Cập nhật số lượng, giá nhập mới, % lợi nhuận & giá bán mới (FIFO ưu tiên giá cũ)",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Search & Category Filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Tìm sản phẩm để nhập...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("restock_search_input")
                    )

                    if (selectedEntries.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldSuccess.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess)
                        ) {
                            Text(
                                text = "Đã chọn: ${selectedEntries.size}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Categories Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("Tất cả", fontSize = 11.sp) }
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Main Content: Split into Selectable Products & Configured Restock Items
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left Column: Catalog selection (width ~ 35%)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "1. Chọn sản phẩm nhập kho (${filteredProducts.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = BluePrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredProducts) { prod ->
                                    val isSelected = selectedEntries.containsKey(prod.id)
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isSelected) {
                                                    selectedEntries.remove(prod.id)
                                                } else {
                                                    selectedEntries[prod.id] = RestockEntryState(prod)
                                                }
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) BluePrimary.copy(alpha = 0.12f) else Color.White,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) BluePrimary else Color(0xFFE2E8F0)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    if (checked) {
                                                        selectedEntries[prod.id] = RestockEntryState(prod)
                                                    } else {
                                                        selectedEntries.remove(prod.id)
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = BluePrimary),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = prod.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Tồn: ${SpoolManager.formatLength(prod.quantity)} ${prod.unit} • Giá cũ: ${PdfExporter.formatVnd(prod.sellPrice)}",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Column: Batch items configuration (width ~ 65%)
                    Card(
                        modifier = Modifier
                            .weight(1.8f)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "2. Thiết lập giá nhập mới, lợi nhuận & giá bán (${selectedEntries.size} mặt hàng)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = EmeraldSuccess
                                )
                                if (selectedEntries.isNotEmpty()) {
                                    Text(
                                        text = "Xóa tất cả",
                                        fontSize = 11.sp,
                                        color = RedAlert,
                                        modifier = Modifier.clickable { selectedEntries.clear() }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (selectedEntries.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Inventory, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Chưa chọn mặt hàng nào để nhập.\nHãy chọn các mặt hàng ở cột bên trái!",
                                            textAlign = TextAlign.Center,
                                            fontSize = 13.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(selectedEntries.values.toList()) { entry ->
                                        RestockItemCard(
                                            entry = entry,
                                            onRemove = { selectedEntries.remove(entry.product.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Bar: Expense Checkbox, Total Cost & Confirm Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = recordExpense,
                            onCheckedChange = { recordExpense = it },
                            colors = CheckboxDefaults.colors(checkedColor = BluePrimary)
                        )
                        Text(
                            text = "Ghi nhận vào sổ chi phí (Chi nhập hàng)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TỔNG TIỀN VỐN NHẬP:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                text = PdfExporter.formatVnd(totalRestockCost),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldSuccess
                            )
                        }

                        Button(
                            onClick = {
                                val items = selectedEntries.values.map { entry ->
                                    val spoolStr = if (entry.product.isCuttableProduct && entry.product.standardPieceLength > 0) {
                                        val count = entry.quantity.toInt().coerceAtLeast(1)
                                        val len = entry.product.standardPieceLength
                                        List(count) { SpoolManager.formatLength(len) }.joinToString(",")
                                    } else ""

                                    BatchRestockItem(
                                        product = entry.product,
                                        newQuantity = entry.quantity,
                                        newCostPrice = entry.costPrice,
                                        markupPercent = entry.markupPercent,
                                        newSellPrice = entry.sellPrice,
                                        spoolLengths = spoolStr
                                    )
                                }
                                onConfirmRestock(items, recordExpense)
                            },
                            enabled = selectedEntries.isNotEmpty() && selectedEntries.values.all { it.quantity > 0 && it.costPrice > 0 },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            modifier = Modifier.testTag("confirm_batch_restock_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Xác Nhận Nhập Kho (${selectedEntries.size} Mặt Hàng)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RestockItemCard(
    entry: RestockEntryState,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Product Name & Quick Old Info & Remove Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BluePrimary
                    )
                    Text(
                        text = "Hiện tại: Tồn ${SpoolManager.formatLength(entry.product.quantity)} ${entry.product.unit} • Giá nhập cũ: ${PdfExporter.formatVnd(entry.product.costPrice)} • Giá bán cũ: ${PdfExporter.formatVnd(entry.product.sellPrice)}",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }

                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = RedAlert, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4 Input fields: Số lượng nhập, Giá nhập mới, % Lợi nhuận, Giá bán mới
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quantity
                OutlinedTextField(
                    value = entry.quantityText,
                    onValueChange = { entry.quantityText = it },
                    label = { Text("SL nhập (${entry.product.unit})", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                // New Cost Price
                OutlinedTextField(
                    value = entry.costPriceText,
                    onValueChange = { entry.onCostChange(it) },
                    label = { Text("Giá nhập mới (đ)", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1.3f)
                )

                // Desired Markup %
                OutlinedTextField(
                    value = entry.markupText,
                    onValueChange = { entry.onMarkupChange(it) },
                    label = { Text("Lợi nhuận (%)", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    trailingIcon = { Text("%", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(end = 6.dp)) },
                    singleLine = true,
                    modifier = Modifier.weight(1.1f)
                )

                // New Sell Price
                OutlinedTextField(
                    value = entry.sellPriceText,
                    onValueChange = { entry.onSellPriceChange(it) },
                    label = { Text("Giá bán mới (đ)", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1.3f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Quick markup helper chips & Line total preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Gợi ý LN:", fontSize = 10.sp, color = Color.Gray)
                    listOf(15.0, 20.0, 25.0, 30.0, 40.0).forEach { p ->
                        Surface(
                            modifier = Modifier.clickable { entry.applyMarkupQuick(p) },
                            shape = RoundedCornerShape(4.dp),
                            color = BluePrimary.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, BluePrimary.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "+${p.toInt()}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BluePrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Thành tiền nhập: ${PdfExporter.formatVnd(entry.totalCost)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldSuccess
                )
            }

            // Pipe or Wire reminder
            if (entry.product.isPipeProduct) {
                Text(
                    text = "• Đơn vị chuẩn: cây (dài ${SpoolManager.formatLength(entry.product.standardPieceLength)}m). Tách biệt từng cây, không gộp chung.",
                    fontSize = 10.sp,
                    color = Color(0xFF0369A1),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
