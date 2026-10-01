package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CartItem
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.Product
import com.example.data.model.ProductBatch
import com.example.ui.VatTuViewModel
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RedAlert
import com.example.util.FifoBatchResolver
import com.example.util.PdfExporter
import com.example.util.SpoolManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    viewModel: VatTuViewModel,
    isLandscape: Boolean = false,
    onInvoiceCreated: (Invoice) -> Unit
) {
    val products by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val batchesByProduct by viewModel.batchesByProductId.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val discountPercent by viewModel.discountPercent.collectAsStateWithLifecycle()
    val paymentMethod by viewModel.paymentMethod.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCat by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()

    var showCartSheet by remember { mutableStateOf(false) }
    var showCustomerSelectDialog by remember { mutableStateOf(false) }
    var showQuickAddCustomerDialog by remember { mutableStateOf(false) }
    var productForQuantityDialog by remember { mutableStateOf<Product?>(null) }
    var initialQtyForDialog by remember { mutableStateOf(1.0) }
    var showOrderCompletionDialog by remember { mutableStateOf(false) }

    val totalCartItems = cartItems.sumOf { it.quantity }
    val totalCartAmount = cartItems.sumOf { it.lineTotal }
    val discountAmount = totalCartAmount * (discountPercent.coerceIn(0.0, 100.0) / 100.0)
    val finalCartAmount = (totalCartAmount - discountAmount).coerceAtLeast(0.0)

    if (isLandscape) {
        // Landscape Mode: 2-column layout (Left: Alphabetical products, Right: Cart & Checkout)
        Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(modifier = Modifier.weight(1.4f).fillMaxHeight()) {
                SalesCatalogHeader(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.searchQuery.value = it },
                    categories = allCategories.map { it.name },
                    selectedCategory = selectedCat,
                    onCategorySelect = { viewModel.selectedCategoryFilter.value = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ProductListAlphabetical(
                    products = products,
                    cart = cart,
                    batchesByProduct = batchesByProduct,
                    onAddToCart = { prod -> viewModel.addToCart(prod, 1.0) },
                    onOpenQuantityDialog = { prod ->
                        productForQuantityDialog = prod
                        val existing = cartItems.find { it.product.id == prod.id }
                        initialQtyForDialog = existing?.quantity ?: 1.0
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                CartCheckoutContent(
                    viewModel = viewModel,
                    cartItems = cartItems,
                    selectedCustomer = selectedCustomer,
                    discountPercent = discountPercent,
                    paymentMethod = paymentMethod,
                    totalCartAmount = totalCartAmount,
                    discountAmount = discountAmount,
                    finalCartAmount = finalCartAmount,
                    onSelectCustomerClick = { showCustomerSelectDialog = true },
                    onEditItemQuantity = { prod, qty ->
                        productForQuantityDialog = prod
                        initialQtyForDialog = qty
                    },
                    onCheckout = { showOrderCompletionDialog = true }
                )
            }
        }
    } else {
        // Portrait Mode: Products list with floating Cart Bar / ModalBottomSheet
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (cart.isNotEmpty()) 80.dp else 0.dp)
            ) {
                SalesCatalogHeader(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.searchQuery.value = it },
                    categories = allCategories.map { it.name },
                    selectedCategory = selectedCat,
                    onCategorySelect = { viewModel.selectedCategoryFilter.value = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ProductListAlphabetical(
                    products = products,
                    cart = cart,
                    batchesByProduct = batchesByProduct,
                    onAddToCart = { prod -> viewModel.addToCart(prod, 1.0) },
                    onOpenQuantityDialog = { prod ->
                        productForQuantityDialog = prod
                        val existing = cartItems.find { it.product.id == prod.id }
                        initialQtyForDialog = existing?.quantity ?: 1.0
                    }
                )
            }

            // Bottom Floating Cart Bar
            if (cartItems.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = BluePrimary,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCartSheet = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = AmberAccent,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            if (totalCartItems % 1.0 == 0.0) totalCartItems.toLong().toString() else "%.1f".format(totalCartItems)
                                        )
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Giỏ hàng", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                Text(
                                    PdfExporter.formatVnd(finalCartAmount),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Button(
                            onClick = { showCartSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("open_cart_sheet_button")
                        ) {
                            Text("Xem đơn & Tính tiền", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Cart Modal Bottom Sheet for Portrait
        if (showCartSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCartSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                CartCheckoutContent(
                    viewModel = viewModel,
                    cartItems = cartItems,
                    selectedCustomer = selectedCustomer,
                    discountPercent = discountPercent,
                    paymentMethod = paymentMethod,
                    totalCartAmount = totalCartAmount,
                    discountAmount = discountAmount,
                    finalCartAmount = finalCartAmount,
                    onSelectCustomerClick = { showCustomerSelectDialog = true },
                    onEditItemQuantity = { prod, qty ->
                        productForQuantityDialog = prod
                        initialQtyForDialog = qty
                    },
                    onCheckout = {
                        showOrderCompletionDialog = true
                    }
                )
            }
        }
    }

    // Smart Quantity & Unit Conversion Dialog
    if (productForQuantityDialog != null) {
        val prod = productForQuantityDialog!!
        val currentCartItem = cartItems.find { it.product.id == prod.id }
        val batches = batchesByProduct[prod.id] ?: emptyList()
        SmartQuantityConversionDialog(
            product = prod,
            batches = batches,
            existingCartItem = currentCartItem,
            onDismiss = { productForQuantityDialog = null },
            onConfirmWithFifo = { qty, unit, isMeter ->
                viewModel.addOrUpdateCartItemWithFifo(
                    product = prod,
                    quantity = qty,
                    soldUnit = unit,
                    isMeterMode = isMeter
                )
                productForQuantityDialog = null
            }
        )
    }

    // Customer Select Dialog
    if (showCustomerSelectDialog) {
        CustomerSelectDialog(
            customers = allCustomers,
            selected = selectedCustomer,
            onSelect = {
                viewModel.selectedCustomer.value = it
                showCustomerSelectDialog = false
            },
            onAddNewClick = {
                showCustomerSelectDialog = false
                showQuickAddCustomerDialog = true
            },
            onDismiss = { showCustomerSelectDialog = false }
        )
    }

    // Quick Add Customer Dialog
    if (showQuickAddCustomerDialog) {
        QuickAddCustomerDialog(
            onDismiss = { showQuickAddCustomerDialog = false },
            onSave = { name, phone, address ->
                viewModel.saveCustomer(
                    Customer(name = name, phone = phone, address = address)
                ) {
                    showQuickAddCustomerDialog = false
                }
            }
        )
    }

    // Order Completion Confirmation Dialog with Item Quantities and Wire & Water Pipes Notice
    if (showOrderCompletionDialog) {
        OrderCompletionDialog(
            cartItems = cartItems,
            customerName = selectedCustomer?.name ?: "Khách lẻ",
            totalAmount = totalCartAmount,
            discountAmount = discountAmount,
            finalAmount = finalCartAmount,
            onConfirm = {
                showOrderCompletionDialog = false
                showCartSheet = false
                viewModel.checkout(onInvoiceCreated)
            },
            onDismiss = { showOrderCompletionDialog = false }
        )
    }
}

@Composable
fun SalesCatalogHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Tìm theo tên hoặc mã vật tư (A-Z)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BluePrimary) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Xóa")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("sales_search_input")
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text("Tất cả vật tư (A-Z)") }
                )
            }
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onCategorySelect(if (selectedCategory == cat) null else cat) },
                    label = { Text(cat) }
                )
            }
        }
    }
}

@Composable
fun ProductListAlphabetical(
    products: List<Product>,
    cart: Map<Long, Double>,
    batchesByProduct: Map<Long, List<ProductBatch>> = emptyMap(),
    onAddToCart: (Product) -> Unit,
    onOpenQuantityDialog: (Product) -> Unit
) {
    if (products.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Không tìm thấy vật tư nào phù hợp.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(products, key = { it.id }) { product ->
                val qtyInCart = cart[product.id] ?: 0.0
                ProductSalesCard(
                    product = product,
                    qtyInCart = qtyInCart,
                    batches = batchesByProduct[product.id] ?: emptyList(),
                    onAddToCart = { onAddToCart(product) },
                    onOpenQuantityDialog = { onOpenQuantityDialog(product) }
                )
            }
        }
    }
}

@Composable
fun ProductSalesCard(
    product: Product,
    qtyInCart: Double,
    batches: List<ProductBatch> = emptyList(),
    onAddToCart: () -> Unit,
    onOpenQuantityDialog: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenQuantityDialog() }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = BluePrimary
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = product.category,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", fontSize = 11.sp, color = Color.LightGray)
                    Text(
                        text = "Mã: ${product.code}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Spool / Pipe remaining breakdown or packaging details
                val spools = product.parseSpoolList()
                val pieceUnit = product.pieceUnitName
                if (spools.isNotEmpty()) {
                    val breakdown = SpoolManager.formatSpoolBreakdown(spools, unit = "m", pieceUnit = pieceUnit)
                    if (spools.distinct().size >= 2) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = AmberAccent.copy(alpha = 0.14f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "TỒN KHO: CÁC ĐOẠN LẺ KHÔNG GỘP CHUNG:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                                Text(
                                    text = breakdown,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF78350F)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Tồn: $breakdown",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0284C7),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                } else if (product.packagingDetails.isNotBlank()) {
                    Text(
                        text = product.packagingDetails,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (product.isNonRefundable) {
                    Text(
                        text = "🔒 ${if (product.isPipeProduct) "Ống nước" else "Dây điện"}: Không đổi trả",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RedAlert,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // FIFO 2-Price Badge
                if (batches.size >= 2) {
                    val oldB = batches.first()
                    val newB = batches.last()
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFEFF6FF),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF3B82F6))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "2 đợt giá (FIFO): Cũ ${PdfExporter.formatVnd(oldB.sellPrice)} (còn ${SpoolManager.formatLength(oldB.quantity)}) ➔ Mới ${PdfExporter.formatVnd(newB.sellPrice)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = PdfExporter.formatVnd(product.sellPrice) + "/${product.unit}",
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldSuccess,
                        fontSize = 14.sp
                    )

                    // Stock status and low stock alert (< 5)
                    if (product.isLowStock) {
                        Box(
                            modifier = Modifier
                                .background(RedAlert.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = RedAlert, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Còn ${product.quantity} ${product.unit} (<5)",
                                    color = RedAlert,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Tồn: ${if (product.quantity % 1.0 == 0.0) product.quantity.toLong() else product.quantity} ${product.unit}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Action buttons: "Nhập SL" button & quick Add button
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onOpenQuantityDialog,
                    enabled = product.quantity > 0,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("custom_quantity_btn_${product.id}")
                ) {
                    Icon(
                        if (product.isCuttableProduct) Icons.Default.ContentCut else Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (product.isCuttableProduct) "⚡ Quy đổi" else "Nhập SL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onAddToCart,
                    enabled = product.quantity > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("add_to_cart_${product.id}")
                ) {
                    if (qtyInCart > 0) {
                        Badge(containerColor = AmberAccent, contentColor = Color.White) {
                            Text(if (qtyInCart % 1.0 == 0.0) qtyInCart.toLong().toString() else qtyInCart.toString())
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Icon(Icons.Default.AddShoppingCart, contentDescription = "Thêm 1", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun CartCheckoutContent(
    viewModel: VatTuViewModel,
    cartItems: List<CartItem>,
    selectedCustomer: Customer?,
    discountPercent: Double,
    paymentMethod: String,
    totalCartAmount: Double,
    discountAmount: Double,
    finalCartAmount: Double,
    onSelectCustomerClick: () -> Unit,
    onEditItemQuantity: (Product, Double) -> Unit,
    onCheckout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Đơn Hàng Đang Bán",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BluePrimary
            )
            if (cartItems.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearCart() }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Xóa giỏ", tint = RedAlert)
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Selected Customer Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectCustomerClick() },
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = BluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = selectedCustomer?.name ?: "Khách lẻ (Bấm để chọn/thêm)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        if (selectedCustomer != null && selectedCustomer.phone.isNotBlank()) {
                            Text(selectedCustomer.phone, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
                Icon(Icons.Default.Search, contentDescription = "Chọn khách", modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Cart items list with direct quantity edit and unit conversion notes
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false).heightIn(max = 240.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(cartItems, key = { it.product.id }) { item ->
                CartItemRow(
                    cartItem = item,
                    onIncrease = {
                        val newQty = item.quantity + 1.0
                        val lenM = if (item.soldUnit == "m") newQty else (newQty * item.product.standardPieceLength)
                        viewModel.addOrUpdateCartItem(
                            product = item.product,
                            quantity = newQty,
                            soldUnit = item.soldUnit,
                            lengthInMeters = lenM,
                            unitPrice = item.unitPrice,
                            note = item.note
                        )
                    },
                    onDecrease = {
                        val newQty = item.quantity - 1.0
                        val lenM = if (item.soldUnit == "m") newQty else (newQty * item.product.standardPieceLength)
                        viewModel.addOrUpdateCartItem(
                            product = item.product,
                            quantity = newQty,
                            soldUnit = item.soldUnit,
                            lengthInMeters = lenM,
                            unitPrice = item.unitPrice,
                            note = item.note
                        )
                    },
                    onEditQuantity = { onEditItemQuantity(item.product, item.quantity) },
                    onRemove = { viewModel.removeCartItem(item.product.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        // Customer discount % input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.5f)) {
                Text("% Chiết khấu cho khách:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("(Không hiện trên bill giấy, trừ vào doanh thu thực)", fontSize = 10.sp, color = Color.Gray)
            }
            OutlinedTextField(
                value = if (discountPercent == 0.0) "" else discountPercent.toString(),
                onValueChange = {
                    val p = it.toDoubleOrNull() ?: 0.0
                    viewModel.discountPercent.value = p.coerceIn(0.0, 100.0)
                },
                placeholder = { Text("0%") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(90.dp).testTag("discount_percent_input")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Summary Calculations
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Tổng tiền hàng niêm yết:", fontSize = 12.sp, color = Color.Gray)
            Text(PdfExporter.formatVnd(totalCartAmount), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }

        if (discountAmount > 0) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Chiết khấu khách đã trừ (-$discountPercent%):", fontSize = 12.sp, color = AmberAccent)
                Text("-${PdfExporter.formatVnd(discountAmount)}", fontSize = 12.sp, color = AmberAccent, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("KHÁCH PHẢI TRẢ:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BluePrimary)
            Text(
                PdfExporter.formatVnd(finalCartAmount),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = EmeraldSuccess
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Checkout Button
        Button(
            onClick = onCheckout,
            enabled = cartItems.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("checkout_invoice_button")
        ) {
            Icon(Icons.Default.PointOfSale, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Hoàn Thành Hàng & Thanh Toán", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun CartItemRow(
    cartItem: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onEditQuantity: () -> Unit,
    onRemove: () -> Unit
) {
    val product = cartItem.product
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.3f)) {
            Text(product.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${PdfExporter.formatVnd(cartItem.unitPrice)}/${cartItem.soldUnit}",
                fontSize = 11.sp,
                color = Color.Gray
            )
            if (cartItem.note.isNotBlank()) {
                Text(
                    text = cartItem.note,
                    fontSize = 10.sp,
                    color = BluePrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (cartItem.isNonRefundable) {
                Text(
                    text = "🔒 Không đổi trả",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = RedAlert
                )
            }
        }

        Row(
            modifier = Modifier.weight(1.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = onDecrease, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "Giảm", modifier = Modifier.size(16.dp))
            }

            // Clickable quantity badge with pencil to directly type quantity
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onEditQuantity() }
                    .border(1.dp, BluePrimary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = Color.White
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${SpoolManager.formatLength(cartItem.quantity)} ${cartItem.soldUnit}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = BluePrimary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Sửa số lượng",
                        tint = BluePrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            IconButton(onClick = onIncrease, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Tăng", modifier = Modifier.size(16.dp))
            }
        }

        Text(
            text = PdfExporter.formatVnd(cartItem.lineTotal),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = BluePrimary,
            modifier = Modifier.weight(1.1f),
            textAlign = TextAlign.End
        )

        IconButton(onClick = onRemove, modifier = Modifier.size(26.dp).padding(start = 2.dp)) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Xóa", tint = RedAlert, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * Smart Quantity & Unit Conversion Dialog:
 * Allows user to choose between selling in Meters (m) or Piece (cây / cuộn).
 * Displays live conversion, price calculation, and real-time spool / pipe deduction simulation.
 * BUSINESS RULES:
 * 1. "đối với vật tư ống nước mua rồi không được đổi lại"
 * 2. "đơn vị tính cho ống các loại là cây. thí dụ ta nhập 6 cây mỗi cây dài 6m,
 *     khi ta bán 5m thì còn dư 5 cây 6m và 1 cây 1m,
 *     nếu ta bán 8m thì còn dư 3 cây 6m, 1 cây 1m, 1 cây 2m không được phép gộp chung,
 *     nếu bán thêm 1m nữa thì còn 3 cây 6m với 1 cây 2m, cứ vậy suy ra,
 *     chế độ mua bán dây điện hay ống hay cây khi có quy đổi thì thông minh thêm"
 */
@Composable
fun SmartQuantityConversionDialog(
    product: Product,
    existingCartItem: CartItem?,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, Double, Double, String) -> Unit // quantity, soldUnit, lengthInMeters, unitPrice, note
) {
    val isCuttable = product.isCuttableProduct
    val pieceUnit = product.pieceUnitName
    val stdLen = product.standardPieceLength
    val spools = product.parseSpoolList()

    // 0 = Bán theo Mét (m), 1 = Bán theo Cây/Cuộn (pieceUnit)
    var selectedTab by remember {
        val defaultTab = if (existingCartItem != null) {
            if (existingCartItem.soldUnit == "m") 0 else 1
        } else if (product.unit.equals("m", ignoreCase = true)) {
            0
        } else if (product.isPipeProduct) {
            0
        } else {
            1
        }
        mutableIntStateOf(defaultTab)
    }

    var textValue by remember {
        val initial = if (existingCartItem != null) {
            val q = existingCartItem.quantity
            if (q % 1.0 == 0.0) q.toLong().toString() else q.toString()
        } else if (selectedTab == 0) {
            if (product.isPipeProduct) "5" else "1"
        } else {
            "1"
        }
        mutableStateOf(initial)
    }

    val parsedQty = textValue.toDoubleOrNull() ?: 0.0
    val isMeterMode = selectedTab == 0

    // Smart Conversion calculation
    val conv = SpoolManager.calculateConversion(
        enteredQty = parsedQty,
        isMeterMode = isMeterMode,
        standardPieceLength = stdLen,
        productBasePrice = product.sellPrice,
        productBaseUnit = product.unit,
        pieceUnit = pieceUnit
    )

    // Stock verification
    val totalAvailableMeters = if (spools.isNotEmpty()) spools.sum() else (product.quantity * stdLen)
    val totalAvailablePieces = if (product.unit == pieceUnit) product.quantity else (totalAvailableMeters / stdLen)

    val isOverStock = if (isMeterMode) {
        conv.lengthInMeters > totalAvailableMeters
    } else {
        parsedQty > product.quantity
    }
    val isValid = parsedQty > 0.0 && !isOverStock

    // Live deduction simulation
    val sim = if (spools.isNotEmpty() && parsedQty > 0 && !isOverStock) {
        SpoolManager.simulateDeduction(
            currentSpools = spools,
            lengthToDeduct = conv.lengthInMeters,
            unit = "m",
            pieceUnit = pieceUnit,
            standardLength = stdLen
        )
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isCuttable) Icons.Default.ContentCut else Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCuttable) "Quy Đổi Thông Minh & Cắt Hàng" else "Nhập Số Lượng Bán Ra",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BluePrimary
                    )
                }
                Text(
                    text = product.name,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                // Non-refundable policy notice
                if (product.isNonRefundable) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RedAlert.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RedAlert.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = RedAlert, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "QUY ĐỊNH: ${if (product.isPipeProduct) "Vật tư ống nước" else "Dây điện"} mua rồi không được đổi lại / không trả tiền hàng.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedAlert
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Current Inventory Breakdown Card
                if (spools.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF0F9FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "TỒN KHO HIỆN TẠI (Tổng ${SpoolManager.formatLength(totalAvailableMeters)}m = ${SpoolManager.formatLength(totalAvailablePieces)} $pieceUnit):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = SpoolManager.formatSpoolBreakdown(spools, unit = "m", pieceUnit = pieceUnit),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0369A1)
                            )
                            if (spools.distinct().size >= 2) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "⚠️ Các cây/đoạn lẻ không được phép gộp chung, phải quản lý và cắt bán riêng biệt!",
                                    fontSize = 10.sp,
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Mode Tabs (if cuttable)
                if (isCuttable) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = BluePrimary,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                if (textValue == "1" && product.isPipeProduct) textValue = "5"
                            },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bán theo Mét (m)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                if (textValue == "5" || textValue == "8") textValue = "1"
                            },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bán theo $pieceUnit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Remnant Quick-Action Chips (If remnant pieces exist in stock!)
                val remnantCandidates = spools.filter { it < stdLen && it > 0.0 }
                if (remnantCandidates.isNotEmpty() && isMeterMode) {
                    Text(
                        text = "⚡ Có đoạn lẻ trong kho - Bấm để bán ngay (không cắt cây mới):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(remnantCandidates.distinct().sorted()) { remLen ->
                            OutlinedButton(
                                onClick = {
                                    textValue = SpoolManager.formatLength(remLen)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFB45309)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Đoạn lẻ ${SpoolManager.formatLength(remLen)}m", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Input Field
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) {
                            textValue = input
                        }
                    },
                    label = {
                        Text(if (isMeterMode) "Số mét bán ra (m)" else "Số lượng bán (${product.unit})")
                    },
                    placeholder = {
                        Text(if (isMeterMode) "Ví dụ: 5 hoặc 8 hoặc 1" else "Ví dụ: 1")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = isOverStock,
                    modifier = Modifier.fillMaxWidth().testTag("smart_quantity_input_field"),
                    trailingIcon = {
                        Text(
                            text = if (isMeterMode) "mét (m)" else product.unit,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                )

                if (isOverStock) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "CẢNH BÁO: Số lượng yêu cầu vượt quá tồn kho khả dụng!",
                        color = RedAlert,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Presets
                Text("Gợi ý chọn nhanh:", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = if (isMeterMode) {
                        if (product.isPipeProduct) listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 12.0)
                        else listOf(5.0, 10.0, 20.0, 50.0, 100.0, 150.0, 200.0)
                    } else {
                        listOf(1.0, 2.0, 3.0, 5.0, 10.0)
                    }
                    items(presets) { pVal ->
                        OutlinedButton(
                            onClick = {
                                textValue = if (pVal % 1.0 == 0.0) pVal.toLong().toString() else pVal.toString()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                "${if (pVal % 1.0 == 0.0) pVal.toLong() else pVal} ${if (isMeterMode) "m" else pieceUnit}",
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Live Calculation & Price Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quy đổi đơn vị:", fontSize = 11.sp, color = Color.Gray)
                            Text(conv.conversionExplanation, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Thành tiền tạm tính:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = PdfExporter.formatVnd(conv.calculatedPrice),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldSuccess
                            )
                        }
                    }
                }

                // Live Simulation of Spool Cutting & Remaining Breakdown
                if (sim != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldSuccess.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            if (sim.skippedSpoolsSummary != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (sim.exactMatchUsed) Icons.Default.PointOfSale else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (sim.exactMatchUsed) EmeraldSuccess else BluePrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = sim.skippedSpoolsSummary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sim.exactMatchUsed) EmeraldSuccess else BluePrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Text(
                                text = "-> Sau khi bán: ${sim.formattedBreakdown}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF047857)
                            )

                            if (sim.isMultiplePartialSpools) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "⚠️ Các đoạn lẻ không được phép gộp chung, phải tách biệt!",
                                    fontSize = 10.sp,
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        val soldUnit = if (isMeterMode) "m" else pieceUnit
                        val lengthInMeters = conv.lengthInMeters
                        val unitPrice = if (isMeterMode && parsedQty > 0) (conv.calculatedPrice / parsedQty) else product.sellPrice
                        val note = if (isCuttable) conv.conversionExplanation else ""
                        onConfirm(parsedQty, soldUnit, lengthInMeters, unitPrice, note)
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                modifier = Modifier.testTag("confirm_smart_quantity_button")
            ) {
                Text("Xác Nhận Thêm Giỏ")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun CustomerSelectDialog(
    customers: List<Customer>,
    selected: Customer?,
    onSelect: (Customer?) -> Unit,
    onAddNewClick: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Chọn Khách Hàng", fontWeight = FontWeight.Bold)
                IconButton(onClick = onAddNewClick) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Thêm khách mới", tint = BluePrimary)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                // Khách lẻ option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(null) }
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected == null) BluePrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = BluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Khách lẻ (Mặc định)", fontWeight = FontWeight.Medium)
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(customers) { cust ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(cust) }
                                .padding(vertical = 3.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected?.id == cust.id) BluePrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (cust.phone.isNotBlank()) {
                                    Text(cust.phone, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onAddNewClick, colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm khách mới")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Đóng") }
        }
    )
}

@Composable
fun QuickAddCustomerDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm Khách Hàng Nhanh", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên khách hàng *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_customer_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_customer_phone_input")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Địa chỉ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, phone, address)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text("Lưu Khách Hàng")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

/**
 * Order Completion Summary Dialog:
 * Shows the exact quantity of each product in the invoice, highlights wire and water pipe items
 * and enforces the rules:
 * - "Đối với dây điện thì bạn không thể trả tiền hàng"
 * - "Đối với vật tư ống nước mua rồi không được đổi lại"
 */
@Composable
fun OrderCompletionDialog(
    cartItems: List<CartItem>,
    customerName: String,
    totalAmount: Double,
    discountAmount: Double,
    finalAmount: Double,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val wireItems = cartItems.filter { it.isWire }
    val pipeItems = cartItems.filter { it.isPipe }
    val nonRefundableItems = cartItems.filter { it.isNonRefundable }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PointOfSale, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Hoàn Thành Hàng Trong Hóa Đơn", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BluePrimary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                Text(
                    text = "Khách hàng: $customerName",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Số lượng từng mặt hàng trong hóa đơn (${cartItems.size} mặt hàng):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(cartItems) { item ->
                        val isSpecial = item.isWire || item.isPipe
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                item.isPipe -> Color(0xFFE0F2FE) // Light Cyan
                                item.isWire -> AmberAccent.copy(alpha = 0.08f)
                                else -> Color(0xFFF8FAFC)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when {
                                    item.isPipe -> Color(0xFF0284C7).copy(alpha = 0.5f)
                                    item.isWire -> AmberAccent.copy(alpha = 0.4f)
                                    else -> Color(0xFFE2E8F0)
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.product.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "SL: ${SpoolManager.formatLength(item.quantity)} ${item.soldUnit}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = BluePrimary
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Đơn giá: ${PdfExporter.formatVnd(item.unitPrice)}/${item.soldUnit}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = PdfExporter.formatVnd(item.lineTotal),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EmeraldSuccess
                                    )
                                }
                                if (item.note.isNotBlank()) {
                                    Text(
                                        text = "✂️ ${item.note}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF0369A1),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                if (item.isPipe) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "⚠️ ỐNG NƯỚC: Mua rồi không được đổi lại (đoạn lẻ không gộp chung)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0369A1)
                                    )
                                } else if (item.isWire) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "⚠️ DÂY ĐIỆN: Không thể trả tiền hàng sau khi bán / cắt",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))

                // Special Warnings banner
                if (nonRefundableItems.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = RedAlert.copy(alpha = 0.1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = RedAlert, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "QUY ĐỊNH HÀNG HÓA TRONG ĐƠN:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RedAlert
                                )
                            }
                            if (pipeItems.isNotEmpty()) {
                                Text(
                                    text = "• Vật tư ống nước: MUA RỒI KHÔNG ĐƯỢC ĐỔI LẠI!",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0369A1),
                                    modifier = Modifier.padding(start = 22.dp, top = 2.dp)
                                )
                            }
                            if (wireItems.isNotEmpty()) {
                                Text(
                                    text = "• Dây điện: KHÔNG THỂ TRẢ TIỀN HÀNG sau khi xuất!",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RedAlert,
                                    modifier = Modifier.padding(start = 22.dp, top = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SỐ TIỀN CẦN TÍNH TOÁN:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BluePrimary)
                    Text(
                        text = PdfExporter.formatVnd(finalAmount),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = EmeraldSuccess
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                modifier = Modifier.testTag("confirm_complete_order_button")
            ) {
                Text("Xác Nhận Hoàn Thành")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Kiểm tra lại")
            }
        }
    )
}

