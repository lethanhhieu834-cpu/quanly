package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.ui.VatTuViewModel
import com.example.ui.components.BillPreviewDialog
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RedAlert
import com.example.util.PdfExporter
import com.example.util.QrCodeUtil
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(
    viewModel: VatTuViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val defaultBank by viewModel.defaultBankAccount.collectAsStateWithLifecycle()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var scannedCode by remember { mutableStateOf("") }
    var matchedInvoice by remember { mutableStateOf<Invoice?>(null) }
    var matchedItems by remember { mutableStateOf<List<InvoiceItem>>(emptyList()) }
    var searchStatusMessage by remember { mutableStateOf<String?>(null) }
    var showFullBillDialog by remember { mutableStateOf(false) }
    var showWireRefundAlert by remember { mutableStateOf(false) }

    fun lookupInvoice(code: String) {
        val cleanCode = code.trim()
        if (cleanCode.isBlank()) return
        scannedCode = cleanCode
        coroutineScope.launch {
            val inv = viewModel.repository.findInvoiceByCode(cleanCode)
            if (inv != null) {
                matchedInvoice = inv
                matchedItems = viewModel.repository.getInvoiceItemsSync(inv.invoiceCode)
                searchStatusMessage = null
            } else {
                matchedInvoice = null
                matchedItems = emptyList()
                searchStatusMessage = "Không tìm thấy hóa đơn có mã '$cleanCode'"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Header
        Text(
            text = "Quét Mã QR Hóa Đơn (Bill)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = BluePrimary
        )
        Text(
            text = "Quét mã QR in trên bill để kiểm tra sản phẩm đã bán và hỗ trợ hoàn trả hàng",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Camera viewfinder or Permission request
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            if (hasCameraPermission) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            val cameraExecutor = Executors.newSingleThreadExecutor()

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalyzer = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { analysis ->
                                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                            val buffer = imageProxy.planes[0].buffer
                                            val bytes = ByteArray(buffer.remaining())
                                            buffer.get(bytes)

                                            val source = PlanarYUVLuminanceSource(
                                                bytes,
                                                imageProxy.width,
                                                imageProxy.height,
                                                0,
                                                0,
                                                imageProxy.width,
                                                imageProxy.height,
                                                false
                                            )
                                            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                                            try {
                                                val result = MultiFormatReader().decode(binaryBitmap)
                                                val text = result.text
                                                if (text != null && text.isNotBlank() && text != scannedCode) {
                                                    previewView.post {
                                                        lookupInvoice(text)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                // Frame did not contain QR code, ignore
                                            } finally {
                                                imageProxy.close()
                                            }
                                        }
                                    }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalyzer
                                    )
                                } catch (exc: Exception) {
                                    exc.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Target scanning frame overlay
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .align(Alignment.Center)
                            .border(2.dp, AmberAccent, RoundedCornerShape(12.dp))
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cần quyền Camera để quét mã QR", color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                        modifier = Modifier.testTag("request_camera_permission_button")
                    ) {
                        Text("Cấp quyền Camera")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Manual Input / Search by code
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = scannedCode,
                onValueChange = { scannedCode = it },
                placeholder = { Text("Nhập mã bill (HD-ddMMyyyyHHmmss)...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).testTag("manual_qr_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { lookupInvoice(scannedCode) },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("lookup_bill_button")
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tra cứu")
            }
        }

        searchStatusMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = RedAlert, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Matched Invoice Card
        matchedInvoice?.let { inv ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("matched_invoice_result"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TÌM THẤY HÓA ĐƠN",
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = inv.invoiceCode,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = BluePrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Khách mua: ${inv.customerName}" + if (inv.customerPhone.isNotBlank()) " - ${inv.customerPhone}" else "", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Tổng thanh toán: ${PdfExporter.formatVnd(inv.finalAmount)} (${inv.paymentMethod})", fontSize = 13.sp, color = BluePrimary, fontWeight = FontWeight.Bold)

                    if (inv.isRefunded) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(RedAlert.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(6.dp)
                        ) {
                            Text("Đơn này đã được hoàn trả hàng trước đó", color = RedAlert, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Các sản phẩm đã bán trong đơn:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    matchedItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• ${item.productName} (x${if (item.quantity % 1.0 == 0.0) item.quantity.toLong() else item.quantity} ${item.unit})", fontSize = 12.sp)
                            Text(PdfExporter.formatVnd(item.total), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showFullBillDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Xem Bill Chi Tiết")
                        }

                        if (!inv.isRefunded) {
                            val hasNonRefundable = matchedItems.any { it.isNonRefundable }
                            Button(
                                onClick = {
                                    if (hasNonRefundable) {
                                        showWireRefundAlert = true
                                    } else {
                                        viewModel.processRefund(inv.invoiceCode, "Khách trả hàng qua quét QR") {
                                            lookupInvoice(inv.invoiceCode)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (hasNonRefundable) RedAlert else AmberAccent),
                                modifier = Modifier.weight(1f).testTag("qr_refund_button")
                            ) {
                                Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (hasNonRefundable) "Không thể đổi trả" else "Hoàn Trả & Nhập Kho")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWireRefundAlert) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showWireRefundAlert = false },
            title = {
                Text("Không Thể Đổi Lại / Trả Tiền Hàng", fontWeight = FontWeight.Bold, color = RedAlert)
            },
            text = {
                Text(
                    text = "QUY ĐỊNH CỬA HÀNG: Đối với vật tư ống nước mua rồi không được đổi lại, và đối với dây điện thì không thể trả lại tiền hàng sau khi đã cắt/hoàn thành hóa đơn!",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showWireRefundAlert = false },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
                ) {
                    Text("Đã hiểu")
                }
            }
        )
    }

    if (showFullBillDialog && matchedInvoice != null) {
        BillPreviewDialog(
            invoice = matchedInvoice!!,
            items = matchedItems,
            bankAccount = defaultBank,
            currentRole = currentRole,
            onDismiss = { showFullBillDialog = false }
        )
    }
}
