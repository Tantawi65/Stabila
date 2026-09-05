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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.stabila.feature.camera.CameraViewModel
import com.stabila.core.ui.LocalAdaptiveParams
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



import androidx.compose.ui.res.stringResource
import com.stabila.core.R

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val adaptive = LocalAdaptiveParams.current
    var manualIso by remember { mutableFloatStateOf(800f) }
    var isSmoothMode by remember { mutableStateOf(false) } // Default: Texture
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
    val savedToastMessage = stringResource(R.string.camera_photo_saved_toast)
    LaunchedEffect(uiState.savedToGallery) {
        if (uiState.savedToGallery) {
            android.widget.Toast.makeText(context, savedToastMessage, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // Track the camera control separately so LaunchedEffect can observe its assignment instantly
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    // State object to track dynamic capture logic without triggering recompositions
    val cameraStateRef = remember {
        object {
            var latestNormalFrame: Bitmap? = null
            var isCollectingBurst = false
            var burstCount = 0
            var bestProFrame: Bitmap? = null
            var maxSharpness = -1f
        }
    }

    // Instant Pro Mode Initialization & Restoration
    LaunchedEffect(cameraControl, manualIso, uiState.cameraState, uiState.isOutdoorBright) {
        val control = cameraControl ?: return@LaunchedEffect
        val ext = Camera2CameraControl.from(control)

        if (uiState.cameraState == CameraViewModel.CameraState.IDLE) {
            // If it's a bright outdoor scene, use 1/1000s. Otherwise 1/250s.
            val exposureTime = if (uiState.isOutdoorBright) 1_000_000L else 4_000_000L
            
            val captureRequestOptions = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime)
                .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, manualIso.toInt())
                .build()
            ext.setCaptureRequestOptions(captureRequestOptions)
        }
    }

    // Dynamic Hardware Switching Logic
    LaunchedEffect(uiState.cameraState) {
        if (uiState.cameraState == CameraViewModel.CameraState.STABILIZING) {
            val control = cameraControl
            if (control == null) {
                viewModel.onBurstCaptured(null, null, isSmoothMode)
                return@LaunchedEffect
            }
            val ext = Camera2CameraControl.from(control)

            // 1. We are already in Pro Mode, collect 20 sharp frames
            cameraStateRef.burstCount = 0
            cameraStateRef.bestProFrame = null
            cameraStateRef.maxSharpness = -1f
            cameraStateRef.isCollectingBurst = true

            while (cameraStateRef.burstCount < 20) {
                delay(10)
            }
            cameraStateRef.isCollectingBurst = false
            val bestFrame = cameraStateRef.bestProFrame

            // 2. Switch to Auto-Exposure for the Normal Frame
            ext.clearCaptureRequestOptions()
            val captureRequestOptions = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                .build()
            ext.setCaptureRequestOptions(captureRequestOptions)
            delay(1000) // Wait significantly longer for physical lens aperture to fully adapt to light

            // 3. Grab the newly exposed normal frame safely using a lock
            val normalFrame = synchronized(cameraStateRef) {
                val frame = cameraStateRef.latestNormalFrame
                cameraStateRef.latestNormalFrame = null
                frame
            }

            // 4. Send to ViewModel for processing
            viewModel.onBurstCaptured(bestFrame, normalFrame, isSmoothMode)
        }
    }

    if (!hasPermission) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.camera_permission_required), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
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
                            contentDescription = stringResource(R.string.camera_original_label),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                        Text(
                            text = stringResource(R.string.camera_original_label),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background.copy(alpha=0.7f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    // Divider
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.primary))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Image(
                            bitmap = uiState.resultBitmap!!.asImageBitmap(),
                            contentDescription = stringResource(R.string.camera_enhanced_label),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                        Text(
                            text = stringResource(R.string.camera_enhanced_label),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background.copy(alpha=0.7f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            } else {
                // Show standard result
                Image(
                    bitmap = uiState.resultBitmap!!.asImageBitmap(),
                    contentDescription = stringResource(R.string.camera_enhanced_label),
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
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(stringResource(R.string.camera_saved_banner), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold, fontSize = (14 * adaptive.fontScale).sp)
                }
            }

            IconButton(
                onClick = { viewModel.reset() },
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.generic_cancel), tint = MaterialTheme.colorScheme.onBackground)
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
                                synchronized(cameraStateRef) {
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
                                        // Keep the latest normal frame, recycle the old one safely
                                        cameraStateRef.latestNormalFrame?.recycle()
                                        cameraStateRef.latestNormalFrame = bitmap
                                    }
                                }
                                
                                // While idle, run the AI + Luminance checks (doesn't block UI since it's on a background thread from CameraX)
                                if (uiState.cameraState == CameraViewModel.CameraState.IDLE && bitmap != null) {
                                    viewModel.updateSceneAnalysis(bitmap)
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
                            // Save CameraControl to the MutableState so UI detects it instantly!
                            cameraControl = camera.cameraControl
                        } catch (exc: Exception) {
                            exc.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    
                    previewView
                }
            )

            // ── TOP BAR ──────────────────────────────────────────────────────
            if (uiState.cameraState == CameraViewModel.CameraState.IDLE) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = 52.dp, start = 20.dp, end = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Brightness label (left side)
                        Text(
                            text = "ISO ${manualIso.toInt()}",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = (13 * adaptive.fontScale).sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        
                        // High Light indicator
                        if (uiState.isOutdoorBright) {
                            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.camera_high_light),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = (12 * adaptive.fontScale).sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFE6A000)) // Sunny orange
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    // Compare toggle (right side)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (uiState.isCompareMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                else MaterialTheme.colorScheme.background.copy(alpha = 0.55f)
                            )
                            .clickable { viewModel.toggleCompareMode() }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (uiState.isCompareMode) stringResource(R.string.camera_compare_on) else stringResource(R.string.camera_compare),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = if (uiState.isCompareMode) FontWeight.Bold else FontWeight.Normal,
                            fontSize = (13 * adaptive.fontScale).sp
                        )
                    }
                }
            }

            // ── PROCESSING SPINNER ────────────────────────────────────────────
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
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                        .padding(32.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                    androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (uiState.cameraState == CameraViewModel.CameraState.STABILIZING)
                            stringResource(R.string.camera_analysing_frames) else stringResource(R.string.camera_enhancing),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (16 * adaptive.fontScale).sp
                    )
                }
            }

            // ── BOTTOM CONTROL PANEL ──────────────────────────────────────────
            if (uiState.cameraState == CameraViewModel.CameraState.IDLE) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
                            )
                        )
                        .padding(bottom = 36.dp, top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ISO Slider
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.camera_brightness),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = (11 * adaptive.fontScale).sp,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = manualIso,
                            onValueChange = { manualIso = it },
                            valueRange = 50f..3200f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))

                    // Smooth / Texture segmented control
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            stringResource(R.string.camera_texture) to false,
                            stringResource(R.string.camera_smooth) to true
                        ).forEach { (label, mode) ->
                            val selected = isSmoothMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .clickable { isSmoothMode = mode }
                                    .padding(horizontal = (adaptive.spacingUnit * 1.2f).value.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = (14 * adaptive.fontScale).sp
                                )
                            }
                        }
                    }

                    androidx.compose.foundation.layout.Spacer(Modifier.height(20.dp))

                    // Shutter button row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val shutterSize = (adaptive.buttonHeight.value * 1.4f).dp
                        val ringSize = (shutterSize.value + 12f).dp
                        // Outer ring
                        Box(
                            modifier = Modifier
                                .size(ringSize)
                                .clip(CircleShape)
                                .border(3.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // Inner shutter button
                            Box(
                                modifier = Modifier
                                    .size(shutterSize)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onBackground)
                                    .clickable { viewModel.captureStarted() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Capture",
                                    tint = MaterialTheme.colorScheme.background,
                                    modifier = Modifier.size((shutterSize.value * 0.4f).dp)
                                )
                            }
                        }
                    }
                }
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
