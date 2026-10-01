package com.example.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Invoice
import com.example.ui.auth.UserRole
import com.example.ui.components.AuthDialog
import com.example.ui.components.BillPreviewDialog
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.BankAccountsScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.InvoicesScreen
import com.example.ui.screens.QrScannerScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SalesScreen
import com.example.ui.screens.SyncScreen
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RedAlert
import kotlinx.coroutines.flow.collectLatest

enum class MainTab(val title: String) {
    SALES("Bán Hàng"),
    INVENTORY("Kho Hàng"),
    INVOICES("Hóa Đơn"),
    QR_SCANNER("Quét QR"),
    CUSTOMERS("Khách Hàng"),
    BANK_ACCOUNTS("Tài Khoản"),
    EXPENSES("Chi Phí"),
    REPORTS("Báo Cáo"),
    SYNC("Đồng Bộ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: VatTuViewModel) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val lastGeneratedInvoice by viewModel.lastGeneratedInvoice.collectAsStateWithLifecycle()
    val defaultBank by viewModel.defaultBankAccount.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(MainTab.SALES) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // BackHandler: return to Sales tab if on other tabs
    BackHandler(enabled = currentTab != MainTab.SALES) {
        currentTab = MainTab.SALES
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Vật Tư Pro",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Role Badge (Clickable to switch with PIN)
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showAuthDialog = true },
                            color = if (currentRole == UserRole.ADMIN) AmberAccent else Color(0xFF3B82F6)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.SupervisorAccount,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentRole.displayName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Online/Offline status indicator
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { currentTab = MainTab.SYNC },
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) EmeraldSuccess else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isOnline) "Online" else "Offline",
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Role switch button
                    IconButton(
                        onClick = { showAuthDialog = true },
                        modifier = Modifier.testTag("switch_role_topbar_button")
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Chuyển vai trò",
                            tint = Color.White
                        )
                    }

                    // More Menu dropdown
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Thêm", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Quản lý Khách Hàng") },
                                onClick = {
                                    currentTab = MainTab.CUSTOMERS
                                    showMoreMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.People, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (currentRole == UserRole.STAFF) "Tài Khoản Ngân Hàng (Admin 🔒)" else "Tài Khoản Ngân Hàng (Admin)") },
                                onClick = {
                                    currentTab = MainTab.BANK_ACCOUNTS
                                    showMoreMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Chi Phí Vận Hành (Admin)") },
                                onClick = {
                                    currentTab = MainTab.EXPENSES
                                    showMoreMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Paid, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (currentRole == UserRole.STAFF) "Báo Cáo Doanh Thu (Admin Only 🔒)" else "Báo Cáo Doanh Thu (Admin)") },
                                onClick = {
                                    currentTab = MainTab.REPORTS
                                    showMoreMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Assessment, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Đồng Bộ & Lưu Trữ (4GB)") },
                                onClick = {
                                    currentTab = MainTab.SYNC
                                    showMoreMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        },
        bottomBar = {
            if (!isLandscape) {
                // Bottom Navigation for Portrait
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == MainTab.SALES,
                        onClick = { currentTab = MainTab.SALES },
                        icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) },
                        label = { Text("Bán hàng", fontSize = 10.sp) },
                        modifier = Modifier.testTag("tab_sales")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.INVENTORY,
                        onClick = { currentTab = MainTab.INVENTORY },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (lowStockProducts.isNotEmpty()) {
                                        Badge(containerColor = RedAlert, contentColor = Color.White) {
                                            Text("${lowStockProducts.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Inventory2, contentDescription = null)
                            }
                        },
                        label = { Text("Kho hàng", fontSize = 10.sp) },
                        modifier = Modifier.testTag("tab_inventory")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.INVOICES,
                        onClick = { currentTab = MainTab.INVOICES },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                        label = { Text("Hóa đơn", fontSize = 10.sp) },
                        modifier = Modifier.testTag("tab_invoices")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.QR_SCANNER,
                        onClick = { currentTab = MainTab.QR_SCANNER },
                        icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                        label = { Text("Quét QR", fontSize = 10.sp) },
                        modifier = Modifier.testTag("tab_qr_scanner")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.REPORTS,
                        onClick = { currentTab = MainTab.REPORTS },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (currentRole == UserRole.STAFF) {
                                        Badge(containerColor = RedAlert) {
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription = "Khóa đối với Nhân viên",
                                                modifier = Modifier.size(10.dp),
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Assessment, contentDescription = null)
                            }
                        },
                        label = { Text(if (currentRole == UserRole.STAFF) "Báo cáo 🔒" else "Báo cáo", fontSize = 10.sp) },
                        modifier = Modifier.testTag("tab_reports")
                    )
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLandscape) {
                // Navigation Rail for Landscape (POS / Tablet mode)
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    NavigationRailItem(
                        selected = currentTab == MainTab.SALES,
                        onClick = { currentTab = MainTab.SALES },
                        icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) },
                        label = { Text("Bán hàng") }
                    )
                    NavigationRailItem(
                        selected = currentTab == MainTab.INVENTORY,
                        onClick = { currentTab = MainTab.INVENTORY },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (lowStockProducts.isNotEmpty()) {
                                        Badge(containerColor = RedAlert) { Text("${lowStockProducts.size}") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Inventory2, contentDescription = null)
                            }
                        },
                        label = { Text("Kho") }
                    )
                    NavigationRailItem(
                        selected = currentTab == MainTab.INVOICES,
                        onClick = { currentTab = MainTab.INVOICES },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                        label = { Text("Hóa đơn") }
                    )
                    NavigationRailItem(
                        selected = currentTab == MainTab.QR_SCANNER,
                        onClick = { currentTab = MainTab.QR_SCANNER },
                        icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                        label = { Text("Quét QR") }
                    )
                    NavigationRailItem(
                        selected = currentTab == MainTab.CUSTOMERS,
                        onClick = { currentTab = MainTab.CUSTOMERS },
                        icon = { Icon(Icons.Default.People, contentDescription = null) },
                        label = { Text("Khách") }
                    )
                    NavigationRailItem(
                        selected = currentTab == MainTab.EXPENSES,
                        onClick = { currentTab = MainTab.EXPENSES },
                        icon = { Icon(Icons.Default.Paid, contentDescription = null) },
                        label = { Text("Chi phí") }
                    )
                    NavigationRailItem(
                        selected = currentTab == MainTab.REPORTS,
                        onClick = { currentTab = MainTab.REPORTS },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (currentRole == UserRole.STAFF) {
                                        Badge(containerColor = RedAlert) {
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription = "Khóa đối với Nhân viên",
                                                modifier = Modifier.size(10.dp),
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Assessment, contentDescription = null)
                            }
                        },
                        label = { Text(if (currentRole == UserRole.STAFF) "Báo cáo 🔒" else "Báo cáo") }
                    )
                    NavigationRailItem(
                        selected = currentTab == MainTab.SYNC,
                        onClick = { currentTab = MainTab.SYNC },
                        icon = { Icon(Icons.Default.CloudSync, contentDescription = null) },
                        label = { Text("Đồng bộ") }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                when (currentTab) {
                    MainTab.SALES -> SalesScreen(
                        viewModel = viewModel,
                        isLandscape = isLandscape,
                        onInvoiceCreated = { invoice ->
                            // Handled by lastGeneratedInvoice
                        }
                    )
                    MainTab.INVENTORY -> InventoryScreen(viewModel = viewModel)
                    MainTab.INVOICES -> InvoicesScreen(viewModel = viewModel)
                    MainTab.QR_SCANNER -> QrScannerScreen(viewModel = viewModel)
                    MainTab.CUSTOMERS -> CustomersScreen(viewModel = viewModel)
                    MainTab.BANK_ACCOUNTS -> BankAccountsScreen(viewModel = viewModel, onBack = { currentTab = MainTab.SALES })
                    MainTab.EXPENSES -> ExpensesScreen(viewModel = viewModel)
                    MainTab.REPORTS -> ReportsScreen(viewModel = viewModel)
                    MainTab.SYNC -> SyncScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Role Switch Dialog (PIN masked)
    if (showAuthDialog) {
        AuthDialog(
            currentRole = currentRole,
            onDismiss = { showAuthDialog = false },
            onPinSubmit = { pin ->
                viewModel.switchRole(pin)
            }
        )
    }

    // Last Generated Invoice Dialog (Instant Bill preview after sale)
    lastGeneratedInvoice?.let { inv ->
        var items by remember { mutableStateOf<List<com.example.data.model.InvoiceItem>>(emptyList()) }
        LaunchedEffect(inv.invoiceCode) {
            items = viewModel.repository.getInvoiceItemsSync(inv.invoiceCode)
        }

        BillPreviewDialog(
            invoice = inv,
            items = items,
            bankAccount = defaultBank,
            currentRole = currentRole,
            onDismiss = { viewModel.dismissLastInvoice() }
        )
    }
}
