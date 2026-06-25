package com.example.roomservice.ui.auth

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera

@SuppressLint("UnsafeOptInUsageError")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffQRScannerScreen(
    onQRScanned: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    var isProcessing by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var linkInput by remember { mutableStateOf("") }
    
    var hasCameraPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        ) 
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1233))) {
        // 1. Camera Section (Top 65%)
        Box(modifier = Modifier.weight(0.65f).fillMaxWidth()) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (!isProcessing) {
                                    try {
                                        val plane = imageProxy.planes[0]
                                        val buffer = plane.buffer
                                        val data = ByteArray(buffer.remaining())
                                        buffer.get(data)

                                        val source = PlanarYUVLuminanceSource(
                                            data, plane.rowStride, imageProxy.height,
                                            0, 0, imageProxy.width, imageProxy.height, false
                                        )

                                        val reader = MultiFormatReader().apply {
                                            val hints = mutableMapOf<DecodeHintType, Any>()
                                            hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(BarcodeFormat.QR_CODE)
                                            hints[DecodeHintType.TRY_HARDER] = true
                                            setHints(hints)
                                        }

                                        var result: Result? = null
                                        try {
                                            result = reader.decode(BinaryBitmap(HybridBinarizer(source)))
                                        } catch (e: Exception) {
                                            try {
                                                result = reader.decode(BinaryBitmap(HybridBinarizer(source.rotateCounterClockwise())))
                                            } catch (e2: Exception) {
                                                try {
                                                    val rotated180 = source.rotateCounterClockwise().rotateCounterClockwise()
                                                    result = reader.decode(BinaryBitmap(HybridBinarizer(rotated180)))
                                                } catch (e3: Exception) { }
                                            }
                                        }

                                        result?.let {
                                            val scannedText = it.text
                                            if (scannedText.contains("staff_login")) {
                                                isProcessing = true
                                                
                                                val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE))

                                                val mainExecutor = ContextCompat.getMainExecutor(context)
                                                mainExecutor.execute { onQRScanned(scannedText) }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        imageProxy.close()
                                    }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                            } catch (e: Exception) { e.printStackTrace() }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Cutout Overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cutoutSize = 220.dp.toPx()
                    val left = (size.width - cutoutSize) / 2
                    val top = (size.height - cutoutSize) / 2
                    drawRect(color = Color.Black.copy(alpha = 0.5f))
                    drawRoundRect(color = Color.Transparent, topLeft = Offset(left, top), size = androidx.compose.ui.geometry.Size(cutoutSize, cutoutSize), cornerRadius = CornerRadius(20.dp.toPx()), blendMode = BlendMode.Clear)
                    drawRoundRect(color = Color.White.copy(alpha = 0.8f), topLeft = Offset(left, top), size = androidx.compose.ui.geometry.Size(cutoutSize, cutoutSize), cornerRadius = CornerRadius(20.dp.toPx()), style = Stroke(width = 2.dp.toPx()))
                }
            }

            // Top Bar Overlay
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Scan Staff QR", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = { 
                        isFlashOn = !isFlashOn
                        camera?.cameraControl?.enableTorch(isFlashOn)
                    },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(if (isFlashOn) Icons.Default.FlashlightOff else Icons.Default.FlashlightOn, null, tint = Color.White)
                }
            }
        }

        // 2. Manual Link Section (Bottom 35%)
        Surface(
            modifier = Modifier.weight(0.35f).fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Login via Link", 
                    fontWeight = FontWeight.ExtraBold, 
                    fontSize = 18.sp, 
                    color = Color(0xFF0D1233)
                )
                Text(
                    "Paste the login link provided by your admin below",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                OutlinedTextField(
                    value = linkInput,
                    onValueChange = { linkInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Paste link here...") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1976D2),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    maxLines = 1,
                    trailingIcon = {
                        if (linkInput.isNotEmpty()) {
                            IconButton(onClick = { linkInput = "" }) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { 
                        if (linkInput.contains("staff_login")) {
                            onQRScanned(linkInput)
                        } else {
                            android.widget.Toast.makeText(context, "Invalid login link", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    enabled = linkInput.isNotBlank()
                ) {
                    Icon(Icons.Default.Login, null)
                    Spacer(Modifier.width(12.dp))
                    Text("LOGIN NOW", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
