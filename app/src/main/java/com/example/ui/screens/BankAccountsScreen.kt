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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BankAccount
import com.example.ui.VatTuViewModel
import com.example.ui.auth.UserRole
import com.example.ui.components.AuthDialog
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RedAlert

@Composable
fun BankAccountsScreen(
    viewModel: VatTuViewModel,
    onBack: () -> Unit = {}
) {
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    val isAdmin = currentRole == UserRole.ADMIN

    var showAddDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<BankAccount?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }

    // CRITICAL USER REQUIREMENT:
    // "tài khoản ngân hàng được bổ sung và xóa trong quản trị viên, nhân viên không thể xem được tài khoản ngân hàng"
    if (!isAdmin) {
        if (showAuthDialog) {
            AuthDialog(
                currentRole = currentRole,
                onDismiss = { showAuthDialog = false },
                onPinSubmit = { pin ->
                    val ok = viewModel.switchRole(pin)
                    if (ok) showAuthDialog = false
                    ok
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(RedAlert.copy(alpha = 0.15f), RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = RedAlert, modifier = Modifier.size(36.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "QUYỀN TRUY CẬP BỊ TỪ CHỐI",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = RedAlert
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Nhân viên không thể xem được tài khoản ngân hàng.\nChỉ Quản trị viên (Admin) mới có quyền xem, bổ sung và xóa số tài khoản ngân hàng để đảm bảo an toàn tài chính.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showAuthDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("admin_unlock_bank_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nhập mã PIN Quản trị viên")
                    }
                }
            }
        }
        return
    }

    // Admin view: Can view, add, and delete bank accounts
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tài Khoản Ngân Hàng",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
                Text(
                    text = "Dành riêng cho Quản trị viên • Tự động tạo mã QR hóa đơn",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("add_bank_account_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Bổ sung TK")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (bankAccounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Chưa có tài khoản ngân hàng nào.", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(onClick = { showAddDialog = true }) {
                        Text("Bổ sung tài khoản đầu tiên")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 70.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(bankAccounts, key = { it.id }) { acc ->
                    BankAccountCard(
                        account = acc,
                        onSetDefault = { viewModel.setDefaultBankAccount(acc.id) },
                        onDelete = { accountToDelete = acc }
                    )
                }
            }
        }
    }

    // Add Bank Account Dialog
    if (showAddDialog) {
        AddBankAccountDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newAccount ->
                viewModel.addBankAccount(newAccount) {
                    showAddDialog = false
                }
            }
        )
    }

    // Delete Confirmation Dialog
    accountToDelete?.let { acc ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = RedAlert)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Xóa Tài Khoản Ngân Hàng", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "Bạn có chắc chắn muốn xóa tài khoản ngân hàng ${acc.bankName} (STK: ${acc.accountNumber}) của chủ TK ${acc.accountHolder}?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBankAccount(acc)
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
                ) {
                    Text("Xóa tài khoản")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { accountToDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun BankAccountCard(
    account: BankAccount,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bank_account_card_${account.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (account.isDefault) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = account.bankName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BluePrimary
                    )
                }

                if (account.isDefault) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldSuccess.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tài khoản chính", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("SỐ TÀI KHOẢN:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Text(
                        text = account.accountNumber,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("CHỦ TÀI KHOẢN:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Text(
                        text = account.accountHolder,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                }
            }

            if (account.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ghi chú: ${account.note}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!account.isDefault) {
                    OutlinedButton(
                        onClick = onSetDefault,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(13.dp), tint = AmberAccent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Đặt làm chính", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("delete_bank_account_${account.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa tài khoản", tint = RedAlert, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AddBankAccountDialog(
    onDismiss: () -> Unit,
    onSave: (BankAccount) -> Unit
) {
    val commonBanks = listOf(
        "MB Bank" to "MB",
        "Vietcombank" to "VCB",
        "Techcombank" to "TCB",
        "BIDV" to "BIDV",
        "VietinBank" to "ICB",
        "Agribank" to "VBA",
        "ACB" to "ACB",
        "VPBank" to "VPB",
        "TPBank" to "TPB"
    )

    var selectedBankName by remember { mutableStateOf("MB Bank") }
    var selectedBankCode by remember { mutableStateOf("MB") }
    var customBankName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var accountHolder by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = BluePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bổ Sung Tài Khoản Ngân Hàng", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Chọn ngân hàng:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BluePrimary)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(commonBanks) { (name, code) ->
                        FilterChip(
                            selected = selectedBankName == name,
                            onClick = {
                                selectedBankName = name
                                selectedBankCode = code
                            },
                            label = { Text(name, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it.filter { char -> char.isDigit() } },
                    label = { Text("Số tài khoản ngân hàng *") },
                    placeholder = { Text("Ví dụ: 0369087887") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bank_account_number_input")
                )

                OutlinedTextField(
                    value = accountHolder,
                    onValueChange = { accountHolder = it.uppercase() },
                    label = { Text("Tên chủ tài khoản (In hoa không dấu) *") },
                    placeholder = { Text("Ví dụ: NGUYEN VAN A") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bank_account_holder_input")
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    placeholder = { Text("Ví dụ: Dùng nhận tiền thanh toán tại quầy") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { isDefault = !isDefault },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                    Text("Đặt làm tài khoản thanh toán mặc định cho hóa đơn", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (customBankName.isNotBlank()) customBankName else selectedBankName
                    if (accountNumber.isNotBlank() && accountHolder.isNotBlank()) {
                        onSave(
                            BankAccount(
                                bankName = finalName,
                                bankCode = selectedBankCode,
                                accountNumber = accountNumber.trim(),
                                accountHolder = accountHolder.trim(),
                                isDefault = isDefault,
                                note = note.trim()
                            )
                        )
                    }
                },
                enabled = accountNumber.isNotBlank() && accountHolder.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                modifier = Modifier.testTag("save_bank_account_button")
            ) {
                Text("Lưu Tài Khoản")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
