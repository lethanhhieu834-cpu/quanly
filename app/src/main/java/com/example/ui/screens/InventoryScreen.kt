package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.ProductBatch
import com.example.ui.VatTuViewModel
import com.example.ui.auth.UserRole
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RedAlert
import com.example.util.PdfExporter
import com.example.util.SpoolManager

@Composable
fun InventoryScreen(
    viewModel: VatTuViewModel
) {
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val batchesByProduct by viewModel.batchesByProductId.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: Tất cả, 1: Cảnh báo tồn thấp (< 5), 2..: Các danh mục
    var showProductDialog by remember { mutableStateOf(false) }
    var showBatchRestockDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val filteredList = allProducts.filter { prod ->
        val matchesSearch = searchQuery.isBlank() || prod.name.contains(searchQuery, true) || prod.code.contains(searchQuery, true)
        val matchesTab = when (selectedTab) {
            0 -> true
            1 -> prod.isLowStock
            else -> {
                val catIndex = selectedTab - 2
                if (catIndex in categories.indices) prod.category == categories[catIndex].name else true
            }
        }
        matchesSearch && matchesTab
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Kho Hàng & Nhập Vật Tư",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                    Text(
                        text = "Tổng: ${allProducts.size} mặt hàng | Tồn thấp: ${lowStockProducts.size} mặt hàng",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { showBatchRestockDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("batch_restock_header_btn")
                    ) {
                        Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nhập hàng loạt", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (currentRole == UserRole.ADMIN) {
                        OutlinedButton(
                            onClick = { showCategoryDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("manage_categories_button")
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Danh mục", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Low Stock Warning Banner (< 5 units)
            if (lowStockProducts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RedAlert.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = RedAlert, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "CẢNH BÁO TỒN KHO DƯỚI 5 ĐƠN VỊ (${lowStockProducts.size} sản phẩm)",
                                color = RedAlert,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Cần lập kế hoạch nhập hàng cho: " + lowStockProducts.take(3).joinToString(", ") { it.name } + if (lowStockProducts.size > 3) "..." else "",
                                fontSize = 11.sp,
                                color = RedAlert.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm kiếm mã, tên vật tư...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BluePrimary) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("inventory_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Action Bar: Explicit "Thêm" and "Xóa" buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Category, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Danh mục (${categories.size}):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { showCategoryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp).testTag("category_add_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Thêm", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showCategoryDialog = true },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp).testTag("category_delete_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(13.dp), tint = RedAlert)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Xóa", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RedAlert)
                    }
                }
            }

            // Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Tất cả (${allProducts.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tồn dưới 5")
                            if (lowStockProducts.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(RedAlert, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        "${lowStockProducts.size}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                )
                categories.forEachIndexed { index, cat ->
                    Tab(
                        selected = selectedTab == index + 2,
                        onClick = { selectedTab = index + 2 },
                        text = { Text(cat.name) }
                    )
                }
                Tab(
                    selected = false,
                    onClick = { showCategoryDialog = true },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(13.dp), tint = BluePrimary)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("+ Thêm/Xóa DM", color = BluePrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Products list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList, key = { it.id }) { product ->
                    InventoryProductCard(
                        product = product,
                        batches = batchesByProduct[product.id] ?: emptyList(),
                        isAdmin = currentRole == UserRole.ADMIN,
                        onEdit = {
                            productToEdit = product
                            showProductDialog = true
                        },
                        onDelete = { viewModel.deleteProduct(product) }
                    )
                }
            }
        }

        // Floating Action Buttons (Batch Restock & Single Product Add)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = { showBatchRestockDialog = true },
                containerColor = EmeraldSuccess,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Inventory, contentDescription = null) },
                text = { Text("Nhập Hàng Hàng Loạt", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.testTag("batch_restock_fab")
            )

            FloatingActionButton(
                onClick = {
                    productToEdit = null
                    showProductDialog = true
                },
                containerColor = BluePrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm 1 vật tư mới")
            }
        }
    }

    // Batch Restock Dialog
    if (showBatchRestockDialog) {
        BatchRestockDialog(
            allProducts = allProducts,
            categories = categories.map { it.name },
            onDismiss = { showBatchRestockDialog = false },
            onConfirmRestock = { items, recordExp ->
                viewModel.batchRestock(items, recordExp) {
                    showBatchRestockDialog = false
                }
            }
        )
    }

    // Add / Edit Product Dialog (with Wire Spool & Packaging Calculators)
    if (showProductDialog) {
        ProductEditorDialog(
            initialProduct = productToEdit,
            categories = categories,
            onAddCategory = { viewModel.addCategory(it) },
            onDeleteCategory = { viewModel.deleteCategory(it) },
            onDismiss = { showProductDialog = false },
            onSave = { updatedProduct ->
                viewModel.saveProduct(updatedProduct) {
                    showProductDialog = false
                }
            }
        )
    }

    // Manage Categories Dialog
    if (showCategoryDialog) {
        CategoryManagerDialog(
            categories = categories,
            onAddCategory = { viewModel.addCategory(it) },
            onDeleteCategory = { viewModel.deleteCategory(it) },
            onDismiss = { showCategoryDialog = false }
        )
    }
}

@Composable
fun InventoryProductCard(
    product: Product,
    batches: List<ProductBatch> = emptyList(),
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().testTag("inventory_item_${product.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                    Text(
                        text = "Mã: ${product.code} | Danh mục: ${product.category}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Chỉnh sửa", tint = BluePrimary, modifier = Modifier.size(18.dp))
                    }
                    if (isAdmin) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = RedAlert, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Pricing details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Giá nhập", fontSize = 10.sp, color = Color.Gray)
                    Text(PdfExporter.formatVnd(product.costPrice), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Lợi nhuận", fontSize = 10.sp, color = Color.Gray)
                    val margin = if (product.costPrice > 0) ((product.sellPrice - product.costPrice) / product.costPrice) * 100 else 0.0
                    Text("+%.1f%%".format(margin), fontSize = 12.sp, color = AmberAccent, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Giá bán ra", fontSize = 10.sp, color = Color.Gray)
                    Text(PdfExporter.formatVnd(product.sellPrice), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                }
                Column {
                    Text("Tồn kho", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = "${if (product.quantity % 1.0 == 0.0) product.quantity.toLong() else product.quantity} ${product.unit}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (product.isLowStock) RedAlert else Color.Black
                    )
                }
            }

            // FIFO Batches breakdown (Old price vs New price)
            if (batches.size >= 2) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93C5FD))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ÁP DỤNG 2 ĐỢT GIÁ (FIFO - Bán hết giá cũ tự chuyển sang giá mới):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        batches.forEachIndexed { i, b ->
                            val label = if (i == 0) "• Lô đợt cũ" else "• Lô nhập mới (${b.batchCode.ifBlank { "Mới" }})"
                            val margin = if (b.costPrice > 0) ((b.sellPrice - b.costPrice) / b.costPrice) * 100 else 0.0
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$label: ${SpoolManager.formatLength(b.quantity)} ${product.unit}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E3A8A)
                                )
                                Text(
                                    text = "Giá bán: ${PdfExporter.formatVnd(b.sellPrice)} (+${margin.toInt()}%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (i == 0) AmberAccent else EmeraldSuccess
                                )
                            }
                        }
                    }
                }
            }

            // Wire spool or packaging breakdown
            val spools = product.parseSpoolList()
            if (spools.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                if (spools.size >= 2) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = AmberAccent.copy(alpha = 0.12f)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "DƯ ${spools.size} CUỘN LẺ TÁCH BIỆT (Không thể gộp chung đoạn liền):",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                            Text(
                                text = spools.mapIndexed { idx, len ->
                                    "Cuộn ${idx + 1}: ${SpoolManager.formatLength(len)}${product.unit}"
                                }.joinToString("  |  "),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF78350F)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE0F2FE), RoundedCornerShape(6.dp))
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "Tồn cuộn: Cuộn 1: ${SpoolManager.formatLength(spools[0])}${product.unit}",
                            fontSize = 11.sp,
                            color = Color(0xFF0369A1),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (product.packagingDetails.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Quy cách đóng gói: ${product.packagingDetails}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ProductEditorDialog(
    initialProduct: Product?,
    categories: List<Category>,
    onAddCategory: (String) -> Unit = {},
    onDeleteCategory: (Category) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var code by remember { mutableStateOf(initialProduct?.code ?: "VT-${System.currentTimeMillis() % 10000}") }
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: categories.firstOrNull()?.name ?: "Vật tư điện") }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "cái") }
    var costPriceStr by remember { mutableStateOf(if (initialProduct != null) initialProduct.costPrice.toLong().toString() else "0") }
    var sellPriceStr by remember { mutableStateOf(if (initialProduct != null) initialProduct.sellPrice.toLong().toString() else "0") }
    var quantityStr by remember { mutableStateOf(if (initialProduct != null) initialProduct.quantity.toString() else "10") }
    var minAlertStr by remember { mutableStateOf(if (initialProduct != null) initialProduct.minStockAlert.toString() else "5") }
    var markupPercentStr by remember { mutableStateOf(if (initialProduct != null && initialProduct.markupPercent > 0) initialProduct.markupPercent.toString() else "30") }

    // Category sub-dialogs
    var showCategoryManagerDialog by remember { mutableStateOf(false) }
    var showQuickAddCategoryDialog by remember { mutableStateOf(false) }
    var quickNewCatName by remember { mutableStateOf("") }

    // Wire Spool Tool
    var showSpoolTool by remember { mutableStateOf(false) }
    var spoolCountStr by remember { mutableStateOf("3") }
    var spoolLengthStr by remember { mutableStateOf("200") }
    var rollCostStr by remember { mutableStateOf("1000000") }

    var spoolLengthsState by remember { mutableStateOf(initialProduct?.spoolLengths ?: "") }
    var packagingDetailsState by remember { mutableStateOf(initialProduct?.packagingDetails ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Quick Add Category Dialog
    if (showQuickAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showQuickAddCategoryDialog = false },
            title = { Text("Thêm Danh Mục Mới", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = quickNewCatName,
                    onValueChange = { quickNewCatName = it },
                    label = { Text("Tên danh mục mới") },
                    placeholder = { Text("Ví dụ: Dụng cụ cầm tay") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (quickNewCatName.isNotBlank()) {
                            onAddCategory(quickNewCatName.trim())
                            category = quickNewCatName.trim()
                            quickNewCatName = ""
                            showQuickAddCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("Thêm & Chọn")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showQuickAddCategoryDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Category Manager Dialog (to delete categories)
    if (showCategoryManagerDialog) {
        CategoryManagerDialog(
            categories = categories,
            onAddCategory = onAddCategory,
            onDeleteCategory = onDeleteCategory,
            onDismiss = { showCategoryManagerDialog = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialProduct == null) "Nhập Hàng Hóa Mới" else "Chỉnh Sửa Vật Tư",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên hàng hóa / vật tư *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Mã vật tư") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("ĐVT (m, cái, hộp)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // CATEGORY SELECTION WITH ADD AND DELETE BUTTONS
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Danh mục sản phẩm:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = { showQuickAddCategoryDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp).testTag("quick_add_category_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp), tint = BluePrimary)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Thêm DM", fontSize = 10.sp, color = BluePrimary, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { showCategoryManagerDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp).testTag("manage_delete_category_btn")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp), tint = RedAlert)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Xóa DM", fontSize = 10.sp, color = RedAlert, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = category == cat.name
                            Button(
                                onClick = { category = cat.name },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) BluePrimary else Color(0xFFF1F5F9),
                                    contentColor = if (isSelected) Color.White else Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(cat.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                // TOOL 1: CÔNG CỤ NHẬP DÂY ĐIỆN THEO CUỘN (Spool Tool)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Calculate, contentDescription = null, tint = EmeraldSuccess)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Công cụ: Dây điện theo cuộn", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EmeraldSuccess)
                                }
                                Button(
                                    onClick = { showSpoolTool = !showSpoolTool },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(if (showSpoolTool) "Thu gọn" else "Mở tính toán", fontSize = 11.sp)
                                }
                            }

                            if (showSpoolTool) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Ví dụ nhập: 3 cuộn x 200m = 600m, giá 1.000.000đ/cuộn", fontSize = 11.sp, color = Color.Gray)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(
                                        value = spoolCountStr,
                                        onValueChange = { spoolCountStr = it },
                                        label = { Text("Số cuộn") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = spoolLengthStr,
                                        onValueChange = { spoolLengthStr = it },
                                        label = { Text("Mét/cuộn") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = rollCostStr,
                                    onValueChange = { rollCostStr = it },
                                    label = { Text("Giá tiền 1 cuộn (VNĐ)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        val count = spoolCountStr.toIntOrNull() ?: 1
                                        val len = spoolLengthStr.toDoubleOrNull() ?: 200.0
                                        val rollCost = rollCostStr.toDoubleOrNull() ?: 1000000.0
                                        val totalMeters = count * len
                                        val costPerMeter = if (len > 0) rollCost / len else 0.0

                                        val markup = markupPercentStr.toDoubleOrNull() ?: 30.0
                                        val sellPriceMeter = costPerMeter * (1.0 + markup / 100.0)

                                        unit = "m"
                                        quantityStr = totalMeters.toString()
                                        costPriceStr = costPerMeter.toLong().toString()
                                        sellPriceStr = sellPriceMeter.toLong().toString()

                                        val list = List(count) { len }
                                        spoolLengthsState = list.joinToString(",")
                                        packagingDetailsState = "Nhập $count cuộn mỗi cuộn ${len}m. Tự động chia nhỏ khi bán lẻ."
                                        showSpoolTool = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                                ) {
                                    Text("Áp Dụng Tính Tự Động Mét & Giá Bán")
                                }
                            }
                        }
                    }
                }

                // THIẾT LẬP GIÁ NHẬP, LỢI NHUẬN MONG MUỐN & GIÁ BÁN RA
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "THIẾT LẬP GIÁ NHẬP, LỢI NHUẬN & GIÁ BÁN RA",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary
                                )
                            }
                            Text(
                                text = "Tự động tính giá bán theo % lợi nhuận mong muốn. Nếu tự ý chỉnh giá bán ra thì % lợi nhuận sẽ tự động nhảy theo!",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                            )

                            // 1. Giá nhập
                            OutlinedTextField(
                                value = costPriceStr,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() || it == '.' }) {
                                        costPriceStr = input
                                        val cost = input.toDoubleOrNull() ?: 0.0
                                        val markup = markupPercentStr.toDoubleOrNull() ?: 0.0
                                        if (cost > 0) {
                                            val calcSell = cost * (1.0 + markup / 100.0)
                                            sellPriceStr = calcSell.toLong().toString()
                                        }
                                    }
                                },
                                label = { Text("Giá nhập (VNĐ) *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("cost_price_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 2. Lợi nhuận mong muốn (%)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Lợi nhuận mong muốn (%):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Chọn nhanh:", fontSize = 10.sp, color = Color.Gray)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(10, 15, 20, 25, 30, 40, 50).forEach { pct ->
                                        val isSelected = markupPercentStr == pct.toString()
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                markupPercentStr = pct.toString()
                                                val cost = costPriceStr.toDoubleOrNull() ?: 0.0
                                                if (cost > 0) {
                                                    val calcSell = cost * (1.0 + pct / 100.0)
                                                    sellPriceStr = calcSell.toLong().toString()
                                                }
                                            },
                                            label = { Text("+$pct%", fontSize = 11.sp) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = markupPercentStr,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() || it == '.' }) {
                                            markupPercentStr = input
                                            val markup = input.toDoubleOrNull() ?: 0.0
                                            val cost = costPriceStr.toDoubleOrNull() ?: 0.0
                                            if (cost > 0) {
                                                val calcSell = cost * (1.0 + markup / 100.0)
                                                sellPriceStr = calcSell.toLong().toString()
                                            }
                                        }
                                    },
                                    label = { Text("Lợi nhuận mong muốn (%)") },
                                    placeholder = { Text("30") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    trailingIcon = { Text("%", fontWeight = FontWeight.Bold, color = BluePrimary, modifier = Modifier.padding(end = 8.dp)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("markup_percent_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 3. Giá bán ra (Tự ý chỉnh giá bán ra thì % lợi nhuận mong muốn nhảy tự động)
                            OutlinedTextField(
                                value = sellPriceStr,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() || it == '.' }) {
                                        sellPriceStr = input
                                        val sell = input.toDoubleOrNull() ?: 0.0
                                        val cost = costPriceStr.toDoubleOrNull() ?: 0.0
                                        if (cost > 0) {
                                            val calculatedMarkup = ((sell - cost) / cost) * 100.0
                                            markupPercentStr = if (calculatedMarkup % 1.0 == 0.0) {
                                                calculatedMarkup.toLong().toString()
                                            } else {
                                                "%.1f".format(java.util.Locale.US, calculatedMarkup)
                                            }
                                        }
                                    }
                                },
                                label = { Text("Giá bán ra (VNĐ) *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("sell_price_input")
                            )

                            // Live summary / rule check
                            val costVal = costPriceStr.toDoubleOrNull() ?: 0.0
                            val sellVal = sellPriceStr.toDoubleOrNull() ?: 0.0
                            Spacer(modifier = Modifier.height(6.dp))

                            if (costVal > 0 && sellVal >= costVal) {
                                val profitUnit = sellVal - costVal
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldSuccess.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Tiền lời dự kiến: +${PdfExporter.formatVnd(profitUnit)} / $unit  (Tỷ suất lợi nhuận: $markupPercentStr%)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldSuccess
                                        )
                                    }
                                }
                            } else if (costVal > 0 && sellVal < costVal) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = RedAlert.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = RedAlert, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "CẢNH BÁO: Giá bán đang thấp hơn giá nhập (Lỗ: -${PdfExporter.formatVnd(costVal - sellVal)} / $unit)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RedAlert
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = { Text("Số lượng tồn kho *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("quantity_input")
                        )
                        OutlinedTextField(
                            value = minAlertStr,
                            onValueChange = { minAlertStr = it },
                            label = { Text("Báo tồn thấp (<)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                errorMessage?.let {
                    item {
                        Text(it, color = RedAlert, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costPriceStr.toDoubleOrNull() ?: 0.0
                    val sell = sellPriceStr.toDoubleOrNull() ?: 0.0
                    val qty = quantityStr.toDoubleOrNull() ?: 0.0
                    val minAlert = minAlertStr.toDoubleOrNull() ?: 5.0
                    val markup = markupPercentStr.toDoubleOrNull() ?: 0.0

                    if (name.isBlank()) {
                        errorMessage = "Vui lòng nhập tên hàng hóa"
                        return@Button
                    }
                    if (sell < cost) {
                        errorMessage = "QUY TẮC: Giá bán không được nhỏ hơn giá nhập!"
                        return@Button
                    }

                    val product = Product(
                        id = initialProduct?.id ?: 0L,
                        code = code.ifBlank { "VT-${System.currentTimeMillis() % 10000}" },
                        name = name.trim(),
                        category = category,
                        unit = unit.trim().ifBlank { "cái" },
                        costPrice = cost,
                        sellPrice = sell,
                        markupPercent = markup,
                        quantity = qty,
                        minStockAlert = minAlert,
                        spoolLengths = spoolLengthsState,
                        packagingDetails = packagingDetailsState,
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(product)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("Lưu Hàng Hóa")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun CategoryManagerDialog(
    categories: List<Category>,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    var newCatName by remember { mutableStateOf("") }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    // Confirm Delete Category Dialog
    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Xác nhận xóa danh mục", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa danh mục '${categoryToDelete?.name}' không?") },
            confirmButton = {
                Button(
                    onClick = {
                        categoryToDelete?.let { onDeleteCategory(it) }
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
                ) {
                    Text("Xác nhận Xóa")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { categoryToDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Category, contentDescription = null, tint = BluePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quản Lý Danh Mục Vật Tư", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                Text("THÊM DANH MỤC MỚI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        placeholder = { Text("Nhập tên danh mục mới...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (newCatName.isNotBlank()) {
                                onAddCategory(newCatName.trim())
                                newCatName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Thêm")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("DANH SÁCH DANH MỤC (${categories.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))

                if (categories.isEmpty()) {
                    Text("Chưa có danh mục nào.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(categories) { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Button(
                                    onClick = { categoryToDelete = cat },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RedAlert.copy(alpha = 0.12f),
                                        contentColor = RedAlert
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = RedAlert, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Xóa", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)) {
                Text("Đóng")
            }
        }
    )
}
