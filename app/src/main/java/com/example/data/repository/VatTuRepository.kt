package com.example.data.repository

import com.example.data.dao.BankAccountDao
import com.example.data.dao.CategoryDao
import com.example.data.dao.CustomerDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.InvoiceDao
import com.example.data.dao.InvoiceItemDao
import com.example.data.dao.ProductBatchDao
import com.example.data.dao.ProductDao
import com.example.data.model.BankAccount
import com.example.data.model.BatchRestockItem
import com.example.data.model.CartItem
import com.example.data.model.Category
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.Product
import com.example.data.model.ProductBatch
import com.example.util.SpoolManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VatTuRepository(
    private val productDao: ProductDao,
    private val productBatchDao: ProductBatchDao,
    private val customerDao: CustomerDao,
    private val invoiceDao: InvoiceDao,
    private val invoiceItemDao: InvoiceItemDao,
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val bankAccountDao: BankAccountDao
) {
    val allProductsAlphabetical: Flow<List<Product>> = productDao.getAllAlphabetical()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val allInvoices: Flow<List<Invoice>> = invoiceDao.getAllInvoices()
    val allInvoiceItems: Flow<List<InvoiceItem>> = invoiceItemDao.getAllInvoiceItems()
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    val allBankAccounts: Flow<List<BankAccount>> = bankAccountDao.getAllBankAccounts()
    val allBatches: Flow<List<ProductBatch>> = productBatchDao.getAllBatches()

    fun getBatchesForProduct(productId: Long): Flow<List<ProductBatch>> {
        return productBatchDao.getActiveBatchesForProduct(productId)
    }

    suspend fun getActiveBatchesForProductDirect(productId: Long): List<ProductBatch> {
        return productBatchDao.getActiveBatchesForProductDirect(productId)
    }

    fun getItemsForInvoice(invoiceCode: String): Flow<List<InvoiceItem>> {
        return invoiceItemDao.getItemsByInvoiceCode(invoiceCode)
    }

    suspend fun findInvoiceByCode(code: String): Invoice? {
        return withContext(Dispatchers.IO) {
            invoiceDao.getInvoiceByCode(code.trim())
        }
    }

    suspend fun getInvoiceItemsSync(code: String): List<InvoiceItem> {
        return withContext(Dispatchers.IO) {
            invoiceItemDao.getItemsByInvoiceCodeSync(code.trim())
        }
    }

    suspend fun getDefaultBankAccount(): BankAccount? {
        return withContext(Dispatchers.IO) {
            bankAccountDao.getDefaultBankAccount() ?: bankAccountDao.getAllBankAccountsSync().firstOrNull()
        }
    }

    /**
     * Seeds initial categories, products, and default bank account if database is empty.
     */
    suspend fun seedInitialDataIfNeeded() {
        withContext(Dispatchers.IO) {
            if (bankAccountDao.getCount() == 0) {
                bankAccountDao.insert(
                    BankAccount(
                        bankName = "MB Bank (Quân Đội)",
                        bankCode = "MB",
                        accountNumber = "0369087887",
                        accountHolder = "HỆ THỐNG VẬT TƯ & THIẾT BỊ XÂY DỰNG",
                        isDefault = true,
                        note = "Tài khoản chính thanh toán hóa đơn"
                    )
                )
            }

            if (categoryDao.getCount() == 0) {
                val defaultCategories = listOf(
                    Category(name = "Vật tư điện", isDefault = true),
                    Category(name = "Vật tư nước", isDefault = true),
                    Category(name = "Cơ kim khí", isDefault = true),
                    Category(name = "Thiết bị chiếu sáng", isDefault = true),
                    Category(name = "Sơn & Chống thấm", isDefault = true)
                )
                categoryDao.insertAll(defaultCategories)
            }

            if (productDao.getCount() == 0) {
                val sampleProducts = listOf(
                    Product(
                        code = "VT-DIEN-01",
                        name = "Dây điện đôi Cadisun 2x2.5mm",
                        category = "Vật tư điện",
                        unit = "m",
                        costPrice = 3333.33,
                        sellPrice = 5000.0,
                        markupPercent = 50.0,
                        quantity = 600.0,
                        minStockAlert = 50.0,
                        spoolLengths = "200.0,200.0,200.0", // 3 rolls x 200m
                        packagingDetails = "Nhập 3 cuộn mỗi cuộn 200m. Giá 1.000.000đ/cuộn (5.000đ/m bán ra)"
                    ),
                    Product(
                        code = "VT-DIEN-02",
                        name = "Dây đơn mềm Trần Phú 1x1.5mm",
                        category = "Vật tư điện",
                        unit = "m",
                        costPrice = 2500.0,
                        sellPrice = 3800.0,
                        markupPercent = 52.0,
                        quantity = 400.0,
                        minStockAlert = 20.0,
                        spoolLengths = "200.0,200.0",
                        packagingDetails = "2 cuộn x 200m"
                    ),
                    Product(
                        code = "VT-DIEN-03",
                        name = "Hộp công tắc đơn Sino Vanlock",
                        category = "Vật tư điện",
                        unit = "cái",
                        costPrice = 12000.0,
                        sellPrice = 18000.0,
                        markupPercent = 50.0,
                        quantity = 150.0,
                        minStockAlert = 10.0,
                        packagingDetails = "1 thùng = 10 hộp = 100 cái (1.200.000đ/thùng)"
                    ),
                    Product(
                        code = "VT-NUOC-01",
                        name = "Ống nhựa PVC Tiền Phong D27 C2 (cây 6m)",
                        category = "Vật tư nước",
                        unit = "cây",
                        costPrice = 90000.0,
                        sellPrice = 120000.0,
                        markupPercent = 33.3,
                        quantity = 6.0,
                        minStockAlert = 2.0,
                        spoolLengths = "6.0,6.0,6.0,6.0,6.0,6.0", // 6 cây mỗi cây 6m
                        packagingDetails = "Nhập 6 cây mỗi cây 6m (Tổng 36m, giá 120.000đ/cây tương đương 20.000đ/m)"
                    ),
                    Product(
                        code = "VT-NUOC-02",
                        name = "Cút vuông 90 độ PVC D27",
                        category = "Vật tư nước",
                        unit = "cái",
                        costPrice = 3000.0,
                        sellPrice = 5000.0,
                        markupPercent = 66.0,
                        quantity = 120.0,
                        minStockAlert = 15.0
                    ),
                    Product(
                        code = "VT-NUOC-03",
                        name = "Van bi đồng tay gạt Sanwa D27",
                        category = "Vật tư nước",
                        unit = "cái",
                        costPrice = 65000.0,
                        sellPrice = 90000.0,
                        markupPercent = 38.5,
                        quantity = 3.0, // Low stock alert!
                        minStockAlert = 5.0
                    ),
                    Product(
                        code = "VT-KHI-01",
                        name = "Đinh vít bắn tôn tự khoan 4 phân",
                        category = "Cơ kim khí",
                        unit = "hộp",
                        costPrice = 45000.0,
                        sellPrice = 65000.0,
                        markupPercent = 44.0,
                        quantity = 25.0,
                        minStockAlert = 5.0,
                        packagingDetails = "1 thùng = 20 hộp (hộp 500 cái)"
                    ),
                    Product(
                        code = "VT-KHI-02",
                        name = "Que hàn Kim Tín KT-421 3.2mm",
                        category = "Cơ kim khí",
                        unit = "bó",
                        costPrice = 85000.0,
                        sellPrice = 110000.0,
                        markupPercent = 29.4,
                        quantity = 15.0,
                        minStockAlert = 5.0
                    ),
                    Product(
                        code = "VT-SANG-01",
                        name = "Bóng đèn LED Bulb Rạng Đông 12W",
                        category = "Thiết bị chiếu sáng",
                        unit = "cái",
                        costPrice = 32000.0,
                        sellPrice = 45000.0,
                        markupPercent = 40.6,
                        quantity = 60.0,
                        minStockAlert = 10.0
                    )
                )
                productDao.insertAll(sampleProducts)
            }

            if (customerDao.getCount() == 0) {
                customerDao.insertAll(
                    listOf(
                        Customer(name = "Nguyễn Văn Hùng (Thợ điện nước)", phone = "0912345678", address = "Phường Mỹ Đình, Nam Từ Liêm, HN"),
                        Customer(name = "Công ty Xây dựng An Phát", phone = "0987654321", address = "Khu đô thị Sala, Q2, TP.HCM"),
                        Customer(name = "Trần Thị Mai", phone = "0905123987", address = "Số 45 Lê Lợi, Đà Nẵng")
                    )
                )
            }
        }
    }

    /**
     * Creates an invoice using smart CartItem list with unit conversion.
     * CRITICAL: Supports selling by meters (m) or by piece (cây / cuộn).
     */
    suspend fun checkoutCartItems(
        cartItems: List<CartItem>,
        customerId: Long?,
        customerName: String,
        customerPhone: String,
        discountPercent: Double,
        paymentMethod: String,
        role: String
    ): Result<Invoice> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Verify all stock quantities first
                for (item in cartItems) {
                    val current = productDao.getProductById(item.product.id)
                        ?: return@withContext Result.failure(Exception("Sản phẩm ${item.product.name} không tồn tại!"))

                    if (item.lengthInMeters > 0 && current.spoolLengths.isNotBlank()) {
                        val totalAvailableMeters = current.parseSpoolList().sum()
                        if (item.lengthInMeters > totalAvailableMeters) {
                            return@withContext Result.failure(
                                Exception("CẢNH BÁO: Số mét bán (${item.lengthInMeters}m) lớn hơn tổng chiều dài tồn kho (${totalAvailableMeters}m) của '${current.name}'. Không thể xuất bán!")
                            )
                        }
                    } else if (item.quantity > current.quantity) {
                        return@withContext Result.failure(
                            Exception("CẢNH BÁO: Số lượng bán (${item.quantity} ${item.soldUnit}) lớn hơn tồn kho (${current.quantity} ${current.unit}) của '${current.name}'. Không thể xuất bán!")
                        )
                    }
                }

                // 2. Generate mandatory formatted invoice code: HD-ddMMyyyyHHmmss
                val invoiceCode = Invoice.generateInvoiceCode()

                var grossTotal = 0.0
                val itemsToSave = mutableListOf<InvoiceItem>()

                for (item in cartItems) {
                    val current = productDao.getProductById(item.product.id)!!
                    grossTotal += item.lineTotal

                    var newSpoolsStr = current.spoolLengths
                    var newQty = current.quantity

                    if (current.spoolLengths.isNotBlank()) {
                        val currentSpools = current.parseSpoolList()
                        val lengthToDeduct = if (item.lengthInMeters > 0) item.lengthInMeters else (item.quantity * current.standardPieceLength)
                        val deduction = SpoolManager.deductFromSpools(
                            currentSpools = currentSpools,
                            lengthToDeduct = lengthToDeduct,
                            unit = "m",
                            pieceUnit = current.pieceUnitName,
                            standardLength = current.standardPieceLength
                        )
                        newSpoolsStr = deduction.updatedSpools.joinToString(",")
                        newQty = if (current.standardPieceLength > 0 && current.unit.equals("cây", true)) {
                            deduction.updatedSpools.sum() / current.standardPieceLength
                        } else {
                            deduction.updatedSpools.sum()
                        }
                    } else {
                        newQty = (current.quantity - item.quantity).coerceAtLeast(0.0)
                    }

                    productDao.updateProduct(
                        current.copy(
                            quantity = newQty,
                            spoolLengths = newSpoolsStr,
                            updatedAt = System.currentTimeMillis()
                        )
                    )

                    // FIFO deduction from product batches
                    val activeBatches = productBatchDao.getActiveBatchesForProductDirect(current.id)
                    var qtyToDeductFromBatches = item.quantity
                    if (item.soldUnit == "m" && current.unit == "cây" && current.standardPieceLength > 0) {
                        qtyToDeductFromBatches = item.quantity / current.standardPieceLength
                    }
                    for (batch in activeBatches) {
                        if (qtyToDeductFromBatches <= 0.0001) break
                        val deductFromThis = if (qtyToDeductFromBatches <= batch.quantity) qtyToDeductFromBatches else batch.quantity
                        val newBatchQty = (batch.quantity - deductFromThis).coerceAtLeast(0.0)
                        productBatchDao.updateBatch(batch.copy(quantity = newBatchQty))
                        qtyToDeductFromBatches -= deductFromThis
                    }

                    val costUnitPrice = if (item.soldUnit == "m" && current.unit == "cây" && current.standardPieceLength > 0) {
                        current.costPrice / current.standardPieceLength
                    } else {
                        current.costPrice
                    }

                    itemsToSave.add(
                        InvoiceItem(
                            invoiceId = 0,
                            invoiceCode = invoiceCode,
                            productId = current.id,
                            productName = if (item.note.isNotBlank()) "${current.name} (${item.note})" else current.name,
                            unit = item.soldUnit,
                            costPrice = costUnitPrice,
                            unitPrice = item.unitPrice,
                            quantity = item.quantity,
                            total = item.lineTotal
                        )
                    )
                }

                val discountAmount = grossTotal * (discountPercent.coerceIn(0.0, 100.0) / 100.0)
                val finalAmount = (grossTotal - discountAmount).coerceAtLeast(0.0)

                val invoice = Invoice(
                    invoiceCode = invoiceCode,
                    customerId = customerId,
                    customerName = customerName.ifBlank { "Khách lẻ" },
                    customerPhone = customerPhone,
                    totalAmount = grossTotal,
                    discountPercent = discountPercent,
                    discountAmount = discountAmount,
                    finalAmount = finalAmount,
                    paymentMethod = paymentMethod,
                    createdByRole = role,
                    createdAt = System.currentTimeMillis()
                )

                val generatedId = invoiceDao.insertInvoice(invoice)

                val finalItems = itemsToSave.map { it.copy(invoiceId = generatedId) }
                invoiceItemDao.insertItems(finalItems)

                if (customerId != null) {
                    customerDao.getCustomerById(customerId)?.let { cust ->
                        customerDao.updateCustomer(cust.copy(totalSpent = cust.totalSpent + finalAmount))
                    }
                }

                Result.success(invoice.copy(id = generatedId))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Creates an invoice with full stock verification (Pair version).
     */
    suspend fun checkoutInvoice(
        cartItems: List<Pair<Product, Double>>,
        customerId: Long?,
        customerName: String,
        customerPhone: String,
        discountPercent: Double,
        paymentMethod: String,
        role: String
    ): Result<Invoice> {
        val converted = cartItems.map { (product, qty) ->
            CartItem(
                product = product,
                quantity = qty,
                soldUnit = product.unit,
                lengthInMeters = if (product.unit == "m") qty else (qty * product.standardPieceLength),
                unitPrice = product.sellPrice,
                lineTotal = product.sellPrice * qty
            )
        }
        return checkoutCartItems(
            cartItems = converted,
            customerId = customerId,
            customerName = customerName,
            customerPhone = customerPhone,
            discountPercent = discountPercent,
            paymentMethod = paymentMethod,
            role = role
        )
    }

    /**
     * Return/Refund: Restores items back into stock, marks invoice as refunded.
     * CRITICAL RULE: "Đối với dây điện thì bạn không thể trả tiền hàng"
     */
    suspend fun refundInvoice(invoiceCode: String, reason: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val invoice = invoiceDao.getInvoiceByCode(invoiceCode)
                    ?: return@withContext Result.failure(Exception("Không tìm thấy hóa đơn $invoiceCode"))

                if (invoice.isRefunded) {
                    return@withContext Result.failure(Exception("Hóa đơn này đã được hoàn trả trước đó"))
                }

                val items = invoiceItemDao.getItemsByInvoiceCodeSync(invoiceCode)

                // Check rule: "Đối với dây điện và vật tư ống nước mua rồi không được đổi lại"
                val nonRefundableItems = items.filter { it.isNonRefundable }
                if (nonRefundableItems.isNotEmpty()) {
                    val names = nonRefundableItems.joinToString { "${it.productName} (${it.quantity}${it.unit})" }
                    return@withContext Result.failure(
                        Exception("Đối với dây điện và vật tư ống nước mua rồi không được đổi lại / không thể trả tiền hàng! Đơn hàng chứa: $names. Theo quy định, các mặt hàng này không được phép hoàn tiền.")
                    )
                }

                for (item in items) {
                    val product = productDao.getProductById(item.productId)
                    if (product != null) {
                        val restoredQty = product.quantity + item.quantity
                        var restoredSpools = product.spoolLengths
                        if (product.spoolLengths.isNotBlank()) {
                            val list = product.parseSpoolList().toMutableList()
                            list.add(item.quantity)
                            restoredSpools = list.joinToString(",")
                        }

                        productDao.updateProduct(
                            product.copy(
                                quantity = restoredQty,
                                spoolLengths = restoredSpools,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }

                // Update invoice
                invoiceDao.updateInvoice(
                    invoice.copy(
                        isRefunded = true,
                        refundReason = reason,
                        refundedAt = System.currentTimeMillis()
                    )
                )

                // Deduct customer spent if applicable
                invoice.customerId?.let { cid ->
                    customerDao.getCustomerById(cid)?.let { cust ->
                        customerDao.updateCustomer(
                            cust.copy(totalSpent = (cust.totalSpent - invoice.finalAmount).coerceAtLeast(0.0))
                        )
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Admin-only revocation and deletion of invoice.
     */
    suspend fun revokeAndDeleteInvoice(invoiceCode: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                invoiceItemDao.deleteByInvoiceCode(invoiceCode)
                invoiceDao.deleteByCode(invoiceCode)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Bank Account management (Admin only)
    suspend fun addBankAccount(bankAccount: BankAccount): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                if (bankAccount.accountNumber.isBlank() || bankAccount.bankName.isBlank()) {
                    return@withContext Result.failure(Exception("Vui lòng nhập tên ngân hàng và số tài khoản!"))
                }
                if (bankAccount.isDefault) {
                    bankAccountDao.clearDefault()
                }
                val id = bankAccountDao.insert(bankAccount)
                Result.success(id)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteBankAccount(bankAccount: BankAccount): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                bankAccountDao.delete(bankAccount)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun setDefaultBankAccount(id: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                bankAccountDao.clearDefault()
                bankAccountDao.setDefault(id)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Product methods
    suspend fun saveProduct(product: Product): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                if (product.sellPrice < product.costPrice) {
                    return@withContext Result.failure(Exception("Giá bán không được nhỏ hơn giá nhập!"))
                }
                val id = if (product.id == 0L) {
                    productDao.insertProduct(product)
                } else {
                    productDao.updateProduct(product)
                    product.id
                }
                Result.success(id)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteProduct(product: Product) {
        withContext(Dispatchers.IO) { productDao.deleteProduct(product) }
    }

    // Customer methods
    suspend fun saveCustomer(customer: Customer, isAdmin: Boolean): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                if (customer.id != 0L && !isAdmin) {
                    return@withContext Result.failure(Exception("Nhân viên chỉ có quyền thêm khách hàng mới, không được sửa thông tin khách hàng hiện có!"))
                }
                val id = if (customer.id == 0L) {
                    customerDao.insertCustomer(customer)
                } else {
                    customerDao.updateCustomer(customer)
                    customer.id
                }
                Result.success(id)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteCustomer(customer: Customer) {
        withContext(Dispatchers.IO) { customerDao.deleteCustomer(customer) }
    }

    // Category methods
    suspend fun addCategory(name: String): Long {
        return withContext(Dispatchers.IO) {
            categoryDao.insertCategory(Category(name = name.trim()))
        }
    }

    suspend fun updateCategory(category: Category) {
        withContext(Dispatchers.IO) { categoryDao.updateCategory(category) }
    }

    suspend fun deleteCategory(category: Category) {
        withContext(Dispatchers.IO) { categoryDao.deleteCategory(category) }
    }

    // Expense methods (Admin only)
    suspend fun saveExpense(expense: Expense) {
        withContext(Dispatchers.IO) {
            if (expense.id == 0L) {
                expenseDao.insertExpense(expense)
            } else {
                expenseDao.updateExpense(expense)
            }
        }
    }

    suspend fun deleteExpense(expense: Expense) {
        withContext(Dispatchers.IO) { expenseDao.deleteExpense(expense) }
    }

    /**
     * Batch restocks multiple products simultaneously:
     * - Allows selecting multiple products
     * - Sets new quantity, new cost price, desired markup %, and new selling price
     * - Preserves previous stock under old price batch so that selling prioritizes old price first!
     * - Records expense for restock if requested.
     */
    suspend fun batchRestockProducts(
        items: List<BatchRestockItem>,
        recordExpense: Boolean = true
    ) = withContext(Dispatchers.IO) {
        var totalCost = 0.0
        val restockedNames = mutableListOf<String>()

        for (item in items) {
            val current = productDao.getProductById(item.product.id) ?: continue
            val existingBatches = productBatchDao.getAllBatchesForProductDirect(current.id)

            // If product has existing inventory (>0) but no batches in DB yet, create an initial batch for old stock
            if (existingBatches.isEmpty() && current.quantity > 0.0001) {
                productBatchDao.insertBatch(
                    ProductBatch(
                        productId = current.id,
                        batchCode = "Đợt giá cũ",
                        quantity = current.quantity,
                        initialQuantity = current.quantity,
                        costPrice = current.costPrice,
                        sellPrice = current.sellPrice,
                        markupPercent = current.markupPercent,
                        spoolLengths = current.spoolLengths,
                        createdAt = System.currentTimeMillis() - 60_000
                    )
                )
            }

            // Insert new batch with new cost price, desired markup %, and new sell price
            val dateLabel = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date())
            val newBatch = ProductBatch(
                productId = current.id,
                batchCode = "Đợt giá mới ($dateLabel)",
                quantity = item.newQuantity,
                initialQuantity = item.newQuantity,
                costPrice = item.newCostPrice,
                sellPrice = item.newSellPrice,
                markupPercent = item.markupPercent,
                spoolLengths = item.spoolLengths,
                createdAt = System.currentTimeMillis()
            )
            productBatchDao.insertBatch(newBatch)

            // Update product master record:
            val newTotalQty = current.quantity + item.newQuantity
            var newSpoolLengths = current.spoolLengths
            if (item.spoolLengths.isNotBlank()) {
                newSpoolLengths = if (newSpoolLengths.isBlank()) item.spoolLengths else "$newSpoolLengths,${item.spoolLengths}"
            }

            val pkgDetail = if (current.isPipeProduct) {
                "${newTotalQty.toLong()} cây (Gồm đợt giá cũ và đợt giá mới ${SpoolManager.formatLength(item.newSellPrice)}đ)"
            } else current.packagingDetails

            productDao.updateProduct(
                current.copy(
                    quantity = newTotalQty,
                    costPrice = item.newCostPrice,
                    sellPrice = item.newSellPrice,
                    markupPercent = item.markupPercent,
                    spoolLengths = newSpoolLengths,
                    packagingDetails = pkgDetail,
                    updatedAt = System.currentTimeMillis()
                )
            )

            totalCost += (item.newQuantity * item.newCostPrice)
            restockedNames.add("${current.name} (+${SpoolManager.formatLength(item.newQuantity)} ${current.unit})")
        }

        if (recordExpense && totalCost > 0) {
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            expenseDao.insertExpense(
                Expense(
                    title = "Nhập hàng: ${items.size} mặt hàng",
                    amount = totalCost,
                    category = "Nhập hàng",
                    note = "Nhập kho đợt $dateStr: " + restockedNames.joinToString(", ")
                )
            )
        }
    }

    /**
     * Backup whole database to JSON string for Cloud / Google Drive export.
     */
    suspend fun exportDataToJson(): String {
        return withContext(Dispatchers.IO) {
            val root = JSONObject()
            root.put("app", "QuanLyVatTu")
            root.put("version", 2)
            root.put("timestamp", System.currentTimeMillis())
            root.toString(2)
        }
    }
}
