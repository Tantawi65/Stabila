package com.stabila.feature.camera.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CaptureRequest
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.stabila.feature.camera.CameraViewModel
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import kotlinx.coroutines.delay

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

// Colour tokens
private val Zinc950 = Color(0xFF09090B)
private val Zinc900 = Color(0xFF18181B)
private val Zinc400 = Color(0xFFA1A1AA)
private val Zinc50  = Color(0xFFFAFAFA)
private val Indigo500 = Color(0xFF6366F1)

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Show a Toast when the photo is saved to gallery
    LaunchedEffect(uiState.savedToGallery) {
        if (uiState.savedToGallery) {
            android.widget.Toast.makeText(context, "📷 Photo saved to Gallery → Pictures/Stabila", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // State object to track dynamic capture logic without triggering recompositions
    val cameraStateRef = remember {
        object {
            var control: androidx.camera.core.CameraControl? = null
            var latestNormalFrame: Bitmap? = null
            var isCollectingBurst = false
            var burstCount = 0
            var bestProFrame: Bitmap? = null
            var maxSharpness = -1f
        }
    }

    // Dynamic Hardware Switching Logic
    LaunchedEffect(uiState.cameraState) {
        if (uiState.cameraState == CameraViewModel.CameraState.STABILIZING) {
            val control = cameraStateRef.control
            val normalFrame = cameraStateRef.latestNormalFrame
            cameraStateRef.latestNormalFrame = null // Prevent analyzer from recycling the active frame!

            if (control == null || normalFrame == null) {
                viewModel.onBurstCaptured(null, null)
                return@LaunchedEffect
            }

            // 1. Switch to Pro Mode dynamically
            val ext = Camera2CameraControl.from(control)
            val captureRequestOptions = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, 4_000_000L) // 1/250s
                .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, 1100)
                .build()

            ext.setCaptureRequestOptions(captureRequestOptions)

            // 2. Wait for sensor to physically adjust
            delay(300)

            // 3. Collect 20 sharp Pro Mode frames (processed on the fly to save memory)
            cameraStateRef.burstCount = 0
            cameraStateRef.bestProFrame = null
            cameraStateRef.maxSharpness = -1f
            cameraStateRef.isCollectingBurst = true

            while (cameraStateRef.burstCount < 20) {
                delay(10)
            }
            cameraStateRef.isCollectingBurst = false
            val bestFrame = cameraStateRef.bestProFrame

            // 4. Revert to Auto-Exposure for bright preview
            ext.clearCaptureRequestOptions()

            // 5. Send to ViewModel for processing
            viewModel.onBurstCaptured(bestFrame, normalFrame)
        }
    }

    if (!hasPermission) {
        Box(modifier = Modifier.fillMaxSize().background(Zinc950), contentAlignment = Alignment.Center) {
            Text("Camera permission required", color = Zinc400)
        }
        return
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Zinc950)
        .focusRequester(focusRequester)
        .focusable()
        .onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown &&
                (event.key == Key.VolumeUp || event.key == Key.VolumeDown)) {
                if (uiState.cameraState == CameraViewModel.CameraState.IDLE) {
                    viewModel.captureStarted()
                }
                true
            } else {
                false
            }
        }
    ) {
        if (uiState.cameraState == CameraViewModel.CameraState.DONE && uiState.resultBitmap != null) {
            if (uiState.isCompareMode && uiState.originalBitmap != null) {
                // Top/Bottom Split
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Image(
                            bitmap = uiState.originalBitmap!!.asImageBitmap(),
                            contentDescription = "Original",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                        Text(
                            text = "Original (Blurry)",
                            color = Zinc50,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Zinc950.copy(alpha=0.7f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    // Divider
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Indigo500))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Image(
                            bitmap = uiState.resultBitmap!!.asImageBitmap(),
                            contentDescription = "Enhanced",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                        Text(
                            text = "Enhanced (Stabilized)",
                            color = Zinc50,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Zinc950.copy(alpha=0.7f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            } else {
                // Show standard result
                Image(
                    bitmap = uiState.resultBitmap!!.asImageBitmap(),
                    contentDescription = "Result",
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Saved banner at top
            if (uiState.savedToGallery) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Indigo500.copy(alpha = 0.9f))
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("✓ Saved to Gallery", color = Zinc50, fontWeight = FontWeight.SemiBold)
                }
            }

            IconButton(
                onClick = { viewModel.reset() },
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(Zinc900.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Zinc50)
            }
        } else {
            val lifecycleOwner = LocalLifecycleOwner.current
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        val builder = ImageAnalysis.Builder()
                            .setTargetResolution(android.util.Size(1920, 1080))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)

                        val imageAnalysis = builder.build()
                            
                        imageAnalysis.setAnalyzer(
                            ContextCompat.getMainExecutor(ctx),
                            { imageProxy ->
                                val bitmap = imageProxyToBitmap(imageProxy)
                                if (bitmap != null) {
                                    if (cameraStateRef.isCollectingBurst) {
                                        val sharpness = viewModel.calculateSharpnessBlocking(bitmap)
                                        if (sharpness > cameraStateRef.maxSharpness) {
                                            cameraStateRef.maxSharpness = sharpness
                                            cameraStateRef.bestProFrame?.recycle()
                                            cameraStateRef.bestProFrame = bitmap
                                        } else {
                                            bitmap.recycle()
                                        }
                                        cameraStateRef.burstCount++
                                    } else {
                                        // Keep the latest normal frame, recycle the old one
                                        cameraStateRef.latestNormalFrame?.recycle()
                                        cameraStateRef.latestNormalFrame = bitmap
                                    }
                                }
                                imageProxy.close()
                            }
                        )
                        
                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                            // Save CameraControl so we can dynamically adjust exposure later
                            cameraStateRef.control = camera.cameraControl
                        } catch (exc: Exception) {
                            exc.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    
                    previewView
                }
            )

            // Overlays
            
            // Compare Mode Toggle
            if (uiState.cameraState == CameraViewModel.CameraState.IDLE) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (uiState.isCompareMode) Indigo500 else Zinc900.copy(alpha=0.7f))
                        .clickable { viewModel.toggleCompareMode() }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.isCompareMode) "Compare: ON" else "Compare: OFF",
                        color = Zinc50,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            AnimatedVisibility(
                visible = uiState.cameraState == CameraViewModel.CameraState.STABILIZING || 
                          uiState.cameraState == CameraViewModel.CameraState.PROCESSING,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Zinc900.copy(alpha = 0.8f))
                        .padding(24.dp)
                ) {
                    CircularProgressIndicator(color = Indigo500)
                    androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (uiState.cameraState == CameraViewModel.CameraState.STABILIZING) 
                                "Stabilising..." else "Enhancing...",
                        color = Zinc50,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Shutter Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (uiState.cameraState == CameraViewModel.CameraState.IDLE) Zinc50 else Zinc400)
                    .border(4.dp, Zinc400, CircleShape)
                    .clickable(enabled = uiState.cameraState == CameraViewModel.CameraState.IDLE) {
                        viewModel.captureStarted()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Capture", tint = Zinc950)
            }
        }
    }
}



private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    try {
        val bitmap = image.toBitmap()
        
        // Fix rotation
        val matrix = android.graphics.Matrix()
        matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
        
        // Scale down to prevent OOM
        var scale = 1f
        val maxDim = Math.max(bitmap.width, bitmap.height)
        if (maxDim > 1000) {
            scale = 1000f / maxDim
        }
        matrix.postScale(scale, scale)
        
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
