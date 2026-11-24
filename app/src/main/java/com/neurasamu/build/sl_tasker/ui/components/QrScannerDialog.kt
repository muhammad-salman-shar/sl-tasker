package com.neurasamu.build.sl_tasker.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.neurasamu.build.sl_tasker.ui.theme.AccentCyan
import com.neurasamu.build.sl_tasker.ui.theme.AccentGold
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.DarkSurface
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.sl_tasker.ui.theme.TextOnBlue
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.theme.TextSecondary
import com.neurasamu.build.sl_tasker.util.QrBarcodeHelper
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScannerDialog(
    title: String = "SCAN QR / BARCODE SEAL",
    subtitle: String = "Point your camera at your registered QR code or household product barcode",
    expectedCode: String? = null,
    onDismiss: () -> Unit,
    onScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var statusText by remember { mutableStateOf("Align QR code or barcode inside frame") }
    var isMismatch by remember { mutableStateOf(false) }
    var torchOn by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    val completedOnce = remember { AtomicBoolean(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = title,
                color = AccentCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))

                if (!hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Camera permission is required to scan QR / Barcodes.",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { launcher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryManaBlue,
                                    contentColor = TextOnBlue
                                )
                            ) {
                                Text("GRANT CAMERA", fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
                    val scanner = remember {
                        val opts = BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                            .build()
                        BarcodeScanning.getClient(opts)
                    }
                    var boundProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

                    DisposableEffect(Unit) {
                        onDispose {
                            try { boundCamera?.cameraControl?.enableTorch(false) } catch (_: Throwable) {}
                            try { boundProvider?.unbindAll() } catch (_: Throwable) {}
                            try { scanner.close() } catch (_: Throwable) {}
                            try { cameraExecutor.shutdown() } catch (_: Throwable) {}
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(248.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black)
                            .border(
                                2.dp,
                                if (isMismatch) DangerPenaltyRed else AccentCyan,
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }
                                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                                providerFuture.addListener({
                                    try {
                                        val provider = providerFuture.get()
                                        boundProvider = provider
                                        val preview = Preview.Builder().build().also {
                                            it.surfaceProvider = previewView.surfaceProvider
                                        }
                                        val analysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()

                                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                            val mediaImage = imageProxy.image
                                            if (mediaImage == null) {
                                                imageProxy.close()
                                                return@setAnalyzer
                                            }
                                            val input = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )
                                            scanner.process(input)
                                                .addOnSuccessListener { barcodes ->
                                                    val value = barcodes
                                                        .firstOrNull { !it.rawValue.isNullOrBlank() }
                                                        ?.rawValue
                                                        ?.trim()
                                                    if (!value.isNullOrBlank() && !completedOnce.get()) {
                                                        val expected = expectedCode?.trim()
                                                        if (expected.isNullOrBlank() || value == expected) {
                                                            if (completedOnce.compareAndSet(false, true)) {
                                                                isMismatch = false
                                                                statusText = "Verified: ${value.take(24)}"
                                                                try { boundCamera?.cameraControl?.enableTorch(false) } catch (_: Throwable) {}
                                                                onScanned(value)
                                                            }
                                                        } else {
                                                            isMismatch = true
                                                            statusText = "Wrong code scanned (${value.take(16)}) — scan registered seal!"
                                                        }
                                                    }
                                                }
                                                .addOnCompleteListener {
                                                    imageProxy.close()
                                                }
                                        }

                                        provider.unbindAll()
                                        val cam = provider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            analysis
                                        )
                                        boundCamera = cam
                                    } catch (_: Throwable) {
                                        statusText = "Unable to start camera preview."
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            }
                        )

                        // Top-Right Flashlight (Torch) On/Off Button
                        val hasFlash = boundCamera?.cameraInfo?.hasFlashUnit() != false
                        if (hasFlash) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (torchOn) AccentGold else Color(0xCC0A1224))
                                    .border(
                                        1.dp,
                                        if (torchOn) AccentGold else AccentCyan.copy(alpha = 0.7f),
                                        RoundedCornerShape(50)
                                    )
                                    .clickable {
                                        val next = !torchOn
                                        torchOn = next
                                        try {
                                            boundCamera?.cameraControl?.enableTorch(next)
                                        } catch (_: Throwable) {}
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (torchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                                    contentDescription = "Toggle Flashlight",
                                    tint = if (torchOn) Color.Black else AccentCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (torchOn) "FLASH ON" else "FLASH",
                                    color = if (torchOn) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = statusText,
                    color = if (isMismatch) DangerPenaltyRed else SuccessGreen,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary, fontSize = 11.sp)
            }
        }
    )
}

@Composable
fun GeneratedQrSealDialog(
    payload: String,
    label: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qrBitmap: Bitmap? = remember(payload) {
        runCatching { QrBarcodeHelper.generateQrBitmap(payload, 520) }.getOrNull()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = "YOUR PHYSICAL QR SEAL",
                color = AccentGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label.ifBlank { "Physical Lock Key" },
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Physical QR Seal",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = payload,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Print or save this QR and stick it away from your desk/bed.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { QrBarcodeHelper.printQrSeal(context, payload, label) },
                        modifier = Modifier.weight(1f),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = SolidColor(AccentGold)
                        )
                    ) {
                        Text("PRINT QR", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { QrBarcodeHelper.shareQrSeal(context, payload, label) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryManaBlue,
                            contentColor = TextOnBlue
                        )
                    ) {
                        Text("SAVE / SHARE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("DONE", color = PrimaryManaBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    )
}
