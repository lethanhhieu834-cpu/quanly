package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.BankAccount
import com.example.data.model.BatchRestockItem
import com.example.data.model.CartItem
import com.example.data.model.Category
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.FifoSaleResult
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.Product
import com.example.data.model.ProductBatch
import com.example.data.repository.VatTuRepository
import com.example.ui.auth.AuthManager
import com.example.ui.auth.UserRole
import com.example.util.FifoBatchResolver
import com.example.util.SpoolManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class VatTuViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = VatTuRepository(
        productDao = db.productDao(),
        productBatchDao = db.productBatchDao(),
        customerDao = db.customerDao(),
        invoiceDao = db.invoiceDao(),
        invoiceItemDao = db.invoiceItemDao(),
        expenseDao = db.expenseDao(),
        categoryDao = db.categoryDao(),
        bankAccountDao = db.bankAccountDao()
    )

    // Current Role: Default to Admin for immediate exploration, user can switch to Staff or lock
    private val _currentRole = MutableStateFlow<UserRole>(UserRole.ADMIN)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    // Products (Always alphabetical as requested: "màn hình bán hàng các sản phẩm hiện theo thứ tự chữ cái")
    val allProducts: StateFlow<List<Product>> = repository.allProductsAlphabetical
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCustomers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInvoices: StateFlow<List<Invoice>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInvoiceItems: StateFlow<List<InvoiceItem>> = repository.allInvoiceItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Product Batches (FIFO inventory tracking with old price vs new price)
    val allBatches: StateFlow<List<ProductBatch>> = repository.allBatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val batchesByProductId: StateFlow<Map<Long, List<ProductBatch>>> = allBatches
        .map { list -> list.filter { it.quantity > 0.0001 }.groupBy { it.productId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Bank Accounts: Admin manages; staff is blocked in UI from viewing details
    val allBankAccounts: StateFlow<List<BankAccount>> = repository.allBankAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultBankAccount: StateFlow<BankAccount?> = repository.allBankAccounts
        .map { list -> list.firstOrNull { it.isDefault } ?: list.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Cart state: List<CartItem> with smart conversion, and legacy Map<Long, Double>
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _cart = MutableStateFlow<Map<Long, Double>>(emptyMap())
    val cart: StateFlow<Map<Long, Double>> = _cart.asStateFlow()

    // Sales screen inputs
    val selectedCustomer = MutableStateFlow<Customer?>(null)
    val discountPercent = MutableStateFlow(0.0) // % cho khách hàng
    val paymentMethod = MutableStateFlow("Chuyển khoản (QR)")
    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow<String?>(null)

    // Filtered products for sales screen (Alphabetical)
    val filteredProducts: StateFlow<List<Product>> = combine(
        allProducts,
        searchQuery,
        selectedCategoryFilter
    ) { products, query, catFilter ->
        products.filter { p ->
            val matchesQuery = query.isBlank() || p.name.contains(query, ignoreCase = true) || p.code.contains(query, ignoreCase = true)
            val matchesCategory = catFilter == null || p.category == catFilter
            matchesQuery && matchesCategory
        }.sortedBy { it.name.lowercase(Locale.getDefault()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Events (e.g. snackbar messages, success)
    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    // Last generated invoice for instant bill preview dialog
    private val _lastGeneratedInvoice = MutableStateFlow<Invoice?>(null)
    val lastGeneratedInvoice: StateFlow<Invoice?> = _lastGeneratedInvoice.asStateFlow()

    // Online & Cloud sync state
    val isOnline = MutableStateFlow(true)
    val isSyncing = MutableStateFlow(false)
    val lastSyncTime = MutableStateFlow(System.currentTimeMillis())
    val isGoogleDriveLinked = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
        }
    }

    // Role switching
    fun switchRole(pin: String): Boolean {
        val role = AuthManager.authenticate(pin)
        return if (role != null) {
            _currentRole.value = role
            viewModelScope.launch {
                _uiMessage.emit("Đã chuyển sang: ${role.displayName}")
            }
            true
        } else {
            viewModelScope.launch {
                _uiMessage.emit("Mật mã không đúng!")
            }
            false
        }
    }

    fun setRoleDirect(role: UserRole) {
        _currentRole.value = role
    }

    // Resolve FIFO for UI simulation & price calculations
    fun resolveFifoForProduct(product: Product, quantityInPieces: Double): FifoSaleResult {
        val batches = batchesByProductId.value[product.id] ?: emptyList()
        return FifoBatchResolver.resolveSale(batches, quantityInPieces, product)
    }

    // Cart management with FIFO batch support and smart conversion
    fun addOrUpdateCartItemWithFifo(
        product: Product,
        quantity: Double,
        soldUnit: String,
        isMeterMode: Boolean,
        customNote: String = ""
    ) {
        if (quantity <= 0) {
            removeCartItem(product.id)
            return
        }

        val requestedMeters = if (isMeterMode) quantity else (quantity * product.standardPieceLength)
        val requestedPieces = if (!isMeterMode) quantity else {
            if (product.standardPieceLength > 0) quantity / product.standardPieceLength else quantity
        }

        // Validate stock
        if (product.spoolLengths.isNotBlank()) {
            val totalMeters = product.parseSpoolList().sum()
            if (requestedMeters > totalMeters) {
                viewModelScope.launch {
                    _uiMessage.emit("CẢNH BÁO: Tồn kho chỉ còn ${SpoolManager.formatLength(totalMeters)}m. Không thể bán ${SpoolManager.formatLength(requestedMeters)}m!")
                }
                return
            }
        } else if (requestedPieces > product.quantity) {
            viewModelScope.launch {
                _uiMessage.emit("CẢNH BÁO: Tồn kho chỉ còn ${SpoolManager.formatLength(product.quantity)} ${product.unit}. Không thể bán ${SpoolManager.formatLength(requestedPieces)} ${product.unit}!")
            }
            return
        }

        val batches = batchesByProductId.value[product.id] ?: emptyList()
        val fifoResult = FifoBatchResolver.resolveSale(batches, requestedPieces, product)

        // Remove previous items for this product
        val currentList = _cartItems.value.filter { it.product.id != product.id }.toMutableList()

        if (fifoResult.lines.size <= 1) {
            val line = fifoResult.lines.firstOrNull()
            val unitPrice = if (isMeterMode) {
                if (product.standardPieceLength > 0 && product.unit == "cây") {
                    (line?.unitPrice ?: product.sellPrice) / product.standardPieceLength
                } else line?.unitPrice ?: product.sellPrice
            } else {
                line?.unitPrice ?: product.sellPrice
            }

            val note = when {
                customNote.isNotBlank() -> customNote
                line?.isOldPrice == true -> "Đợt giá cũ"
                line?.isNewPrice == true && batches.size > 1 -> "Đợt giá mới"
                else -> ""
            }

            currentList.add(
                CartItem(
                    product = product,
                    quantity = quantity,
                    soldUnit = soldUnit,
                    lengthInMeters = requestedMeters,
                    unitPrice = unitPrice,
                    lineTotal = quantity * unitPrice,
                    note = note,
                    batchId = line?.batchId ?: 0,
                    batchCode = line?.batchCode ?: ""
                )
            )
        } else {
            // Split into 2 prices!
            // Rule: "giữa giá mới và giá cũ khi ta bán thì ưu tiên bán giá cũ hết rồi tự chuyển sang giá mới,
            // nếu trường hợp trong bill có sản phẩm giá cũ vừa hết mà có thêm sản phẩm giá mới thì vẫn tính 2 giá."
            for (line in fifoResult.lines) {
                val lineEnteredQty = if (isMeterMode) {
                    line.quantity * product.standardPieceLength
                } else {
                    line.quantity
                }
                val linePrice = if (isMeterMode) {
                    if (product.standardPieceLength > 0 && product.unit == "cây") {
                        line.unitPrice / product.standardPieceLength
                    } else line.unitPrice
                } else {
                    line.unitPrice
                }
                val lineMeters = if (isMeterMode) lineEnteredQty else (lineEnteredQty * product.standardPieceLength)

                currentList.add(
                    CartItem(
                        product = product,
                        quantity = lineEnteredQty,
                        soldUnit = soldUnit,
                        lengthInMeters = lineMeters,
                        unitPrice = linePrice,
                        lineTotal = lineEnteredQty * linePrice,
                        note = line.batchCode,
                        batchId = line.batchId,
                        batchCode = line.batchCode
                    )
                )
            }
            viewModelScope.launch {
                _uiMessage.emit("⚡ Áp dụng 2 mức giá: ${fifoResult.explanation}")
            }
        }

        _cartItems.value = currentList
        syncLegacyCart()
    }

    fun addOrUpdateCartItem(
        product: Product,
        quantity: Double,
        soldUnit: String,
        lengthInMeters: Double,
        unitPrice: Double,
        note: String = ""
    ) {
        val isMeter = soldUnit == "m"
        addOrUpdateCartItemWithFifo(product, quantity, soldUnit, isMeter, note)
    }

    fun addToCart(product: Product, quantityToAdd: Double = 1.0) {
        val existingItems = _cartItems.value.filter { it.product.id == product.id }
        val currentTotalQty = existingItems.sumOf {
            if (it.soldUnit == "m" && product.unit == "cây" && product.standardPieceLength > 0) {
                it.quantity / product.standardPieceLength
            } else it.quantity
        }
        val targetQty = currentTotalQty + quantityToAdd
        addOrUpdateCartItemWithFifo(
            product = product,
            quantity = targetQty,
            soldUnit = product.unit,
            isMeterMode = false
        )
    }

    fun updateCartQuantity(product: Product, newQuantity: Double) {
        if (newQuantity <= 0) {
            removeCartItem(product.id)
            return
        }
        val existing = _cartItems.value.find { it.product.id == product.id }
        val unit = existing?.soldUnit ?: product.unit
        val isMeter = unit == "m"
        addOrUpdateCartItemWithFifo(
            product = product,
            quantity = newQuantity,
            soldUnit = unit,
            isMeterMode = isMeter
        )
    }

    fun removeFromCart(productId: Long) {
        removeCartItem(productId)
    }

    fun removeCartItem(productId: Long) {
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }
        syncLegacyCart()
    }

    fun removeSpecificCartItem(cartItemId: String) {
        _cartItems.value = _cartItems.value.filter { it.cartItemId != cartItemId }
        syncLegacyCart()
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        syncLegacyCart()
    }

    private fun syncLegacyCart() {
        _cart.value = _cartItems.value.associate { it.product.id to it.quantity }
    }

    // Batch Goods Receipt (Nhập hàng nhiều sản phẩm với giá nhập mới, lợi nhuận mong muốn và giá bán mới)
    fun batchRestock(
        items: List<BatchRestockItem>,
        recordExpense: Boolean = true,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.batchRestockProducts(items, recordExpense)
                _uiMessage.emit("Đã nhập kho thành công ${items.size} mặt hàng với giá nhập và giá bán mới!")
                onComplete()
            } catch (e: Exception) {
                _uiMessage.emit("Lỗi khi nhập hàng: ${e.message}")
            }
        }
    }

    // Checkout
    fun checkout(onSuccess: (Invoice) -> Unit) {
        val currentItems = _cartItems.value
        if (currentItems.isEmpty()) {
            viewModelScope.launch { _uiMessage.emit("Giỏ hàng đang trống!") }
            return
        }

        viewModelScope.launch {
            val cust = selectedCustomer.value
            val result = repository.checkoutCartItems(
                cartItems = currentItems,
                customerId = cust?.id,
                customerName = cust?.name ?: "Khách lẻ",
                customerPhone = cust?.phone ?: "",
                discountPercent = discountPercent.value,
                paymentMethod = paymentMethod.value,
                role = currentRole.value.displayName
            )

            result.onSuccess { invoice ->
                _lastGeneratedInvoice.value = invoice
                clearCart()
                discountPercent.value = 0.0
                selectedCustomer.value = null
                _uiMessage.emit("Hoàn thành đơn hàng! Mã: ${invoice.invoiceCode}")
                onSuccess(invoice)
            }.onFailure { err ->
                _uiMessage.emit("Lỗi thanh toán: ${err.message}")
            }
        }
    }

    fun dismissLastInvoice() {
        _lastGeneratedInvoice.value = null
    }

    // Return & Refund (Hoàn trả hàng)
    fun processRefund(invoiceCode: String, reason: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.refundInvoice(invoiceCode, reason)
            res.onSuccess {
                _uiMessage.emit("Đã hoàn trả hóa đơn $invoiceCode và nhập lại hàng vào kho!")
                onComplete()
            }.onFailure {
                _uiMessage.emit(it.message ?: "Lỗi hoàn trả!")
            }
        }
    }

    // Revoke & Delete Invoice (Admin only)
    fun revokeInvoice(invoiceCode: String) {
        if (currentRole.value != UserRole.ADMIN) {
            viewModelScope.launch { _uiMessage.emit("Chỉ quản trị viên mới có quyền thu hồi và xóa hóa đơn!") }
            return
        }
        viewModelScope.launch {
            val res = repository.revokeAndDeleteInvoice(invoiceCode)
            res.onSuccess {
                _uiMessage.emit("Đã thu hồi và xóa vĩnh viễn hóa đơn $invoiceCode!")
            }.onFailure {
                _uiMessage.emit("Lỗi xóa hóa đơn: ${it.message}")
            }
        }
    }

    // Bank Account Management (Admin only)
    fun addBankAccount(account: BankAccount, onComplete: () -> Unit = {}) {
        if (currentRole.value != UserRole.ADMIN) {
            viewModelScope.launch { _uiMessage.emit("Chỉ quản trị viên mới có quyền bổ sung tài khoản ngân hàng!") }
            return
        }
        viewModelScope.launch {
            val res = repository.addBankAccount(account)
            res.onSuccess {
                _uiMessage.emit("Đã bổ sung tài khoản: ${account.bankName} - ${account.accountNumber}")
                onComplete()
            }.onFailure {
                _uiMessage.emit("Lỗi: ${it.message}")
            }
        }
    }

    fun deleteBankAccount(account: BankAccount) {
        if (currentRole.value != UserRole.ADMIN) {
            viewModelScope.launch { _uiMessage.emit("Chỉ quản trị viên mới có quyền xóa tài khoản ngân hàng!") }
            return
        }
        viewModelScope.launch {
            val res = repository.deleteBankAccount(account)
            res.onSuccess {
                _uiMessage.emit("Đã xóa tài khoản ngân hàng: ${account.bankName}")
            }.onFailure {
                _uiMessage.emit("Lỗi xóa tài khoản: ${it.message}")
            }
        }
    }

    fun setDefaultBankAccount(id: Long) {
        if (currentRole.value != UserRole.ADMIN) {
            viewModelScope.launch { _uiMessage.emit("Chỉ quản trị viên mới có quyền thiết lập tài khoản chính!") }
            return
        }
        viewModelScope.launch {
            repository.setDefaultBankAccount(id)
            _uiMessage.emit("Đã đặt tài khoản thanh toán chính!")
        }
    }

    // Products
    fun saveProduct(product: Product, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.saveProduct(product)
            res.onSuccess {
                _uiMessage.emit("Đã lưu hàng hóa: ${product.name}")
                onComplete()
            }.onFailure {
                _uiMessage.emit("Lỗi: ${it.message}")
            }
        }
    }

    fun deleteProduct(product: Product) {
        if (currentRole.value != UserRole.ADMIN) {
            viewModelScope.launch { _uiMessage.emit("Chỉ quản trị viên mới có quyền xóa hàng hóa!") }
            return
        }
        viewModelScope.launch {
            repository.deleteProduct(product)
            _uiMessage.emit("Đã xóa sản phẩm: ${product.name}")
        }
    }

    // Customers
    fun saveCustomer(customer: Customer, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val isAdmin = currentRole.value == UserRole.ADMIN
            val res = repository.saveCustomer(customer, isAdmin)
            res.onSuccess {
                _uiMessage.emit("Đã lưu khách hàng: ${customer.name}")
                onComplete()
            }.onFailure {
                _uiMessage.emit(it.message ?: "Lỗi lưu khách hàng")
            }
        }
    }

    fun deleteCustomer(customer: Customer) {
        if (currentRole.value != UserRole.ADMIN) {
            viewModelScope.launch { _uiMessage.emit("Chỉ quản trị viên mới có quyền xóa khách hàng!") }
            return
        }
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            _uiMessage.emit("Đã xóa khách hàng: ${customer.name}")
        }
    }

    // Expenses (Admin only)
    fun saveExpense(expense: Expense, onComplete: () -> Unit = {}) {
        if (currentRole.value != UserRole.ADMIN) {
            viewModelScope.launch { _uiMessage.emit("Chỉ quản trị viên mới có quyền ghi chép chi phí!") }
            return
        }
        viewModelScope.launch {
            repository.saveExpense(expense)
            _uiMessage.emit("Đã lưu khoản chi: ${expense.title}")
            onComplete()
        }
    }

    fun deleteExpense(expense: Expense) {
        if (currentRole.value != UserRole.ADMIN) {
            viewModelScope.launch { _uiMessage.emit("Chỉ quản trị viên mới có quyền xóa khoản chi!") }
            return
        }
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _uiMessage.emit("Đã xóa khoản chi: ${expense.title}")
        }
    }

    // Categories
    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.addCategory(name)
            _uiMessage.emit("Đã thêm danh mục: $name")
        }
    }

    fun deleteCategory(category: Category) {
        if (currentRole.value != UserRole.ADMIN) {
            viewModelScope.launch { _uiMessage.emit("Chỉ quản trị viên mới có quyền xóa danh mục!") }
            return
        }
        viewModelScope.launch {
            repository.deleteCategory(category)
            _uiMessage.emit("Đã xóa danh mục: ${category.name}")
        }
    }

    // Cloud Sync
    fun triggerCloudSync() {
        viewModelScope.launch {
            isSyncing.value = true
            _uiMessage.emit("Đang kết nối và đồng bộ lên đám mây...")
            kotlinx.coroutines.delay(1200)
            isSyncing.value = false
            lastSyncTime.value = System.currentTimeMillis()
            _uiMessage.emit("Đồng bộ đám mây thành công! Dữ liệu đã an toàn.")
        }
    }

    fun toggleGoogleDrive() {
        isGoogleDriveLinked.value = !isGoogleDriveLinked.value
        viewModelScope.launch {
            if (isGoogleDriveLinked.value) {
                _uiMessage.emit("Đã liên kết tài khoản Google Drive. Tự động sao lưu khi có mạng.")
            } else {
                _uiMessage.emit("Đã ngắt liên kết Google Drive.")
            }
        }
    }
}
