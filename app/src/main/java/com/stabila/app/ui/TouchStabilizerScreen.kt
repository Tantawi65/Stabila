package com.stabila.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sqrt

import androidx.compose.ui.res.stringResource
import com.stabila.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchStabilizerScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val touchStabilizerEnabled by viewModel.touchStabilizerEnabled.collectAsState(initial = false)
    val savedRadius by viewModel.touchTremorRadius.collectAsState(initial = 0f)
    val context = androidx.compose.ui.platform.LocalContext.current
    val enableAccessibilityMsg = stringResource(R.string.stabilizer_please_enable_service)
    val allowOverlayMsg = stringResource(R.string.stabilizer_please_allow_overlay)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stabilizer_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.generic_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = stringResource(R.string.stabilizer_global_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = stringResource(R.string.stabilizer_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Calibration Game Area
            CalibrationGameBox(
                savedRadius = savedRadius,
                onCalibrationComplete = { radius ->
                    viewModel.setTouchTremorRadius(radius)
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(R.string.stabilizer_enable_system),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.stabilizer_require_accessibility),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = touchStabilizerEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            val am = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
                            val enabledServices = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                            val isAccessibilityEnabled = enabledServices?.contains(context.packageName) == true
                            val isOverlayEnabled = android.provider.Settings.canDrawOverlays(context)

                            if (!isAccessibilityEnabled) {
                                android.widget.Toast.makeText(context, enableAccessibilityMsg, android.widget.Toast.LENGTH_LONG).show()
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            } else if (!isOverlayEnabled) {
                                android.widget.Toast.makeText(context, allowOverlayMsg, android.widget.Toast.LENGTH_LONG).show()
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}")))
                            } else {
                                viewModel.setTouchStabilizerEnabled(true)
                            }
                        } else {
                            viewModel.setTouchStabilizerEnabled(false)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CalibrationGameBox(
    savedRadius: Float,
    onCalibrationComplete: (Float) -> Unit
) {
    var gameState by remember(savedRadius) { mutableStateOf(if (savedRadius > 0f && savedRadius != 50f) 2 else 0) } // 0: Start, 1: Playing, 2: Done
    var currentTargetIndex by remember { mutableStateOf(0) }
    
    // Metrics
    var maxRadiusCalculated by remember(savedRadius) { mutableFloatStateOf(if (savedRadius > 0f) savedRadius else 0f) }
    var avgDurationCalculated by remember { mutableLongStateOf(0L) }
    var totalTargets by remember { mutableIntStateOf(5) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // Dynamic positions for a better feeling of "calibration"
    val targetPositions = listOf(
        Offset(0.5f, 0.5f), // Center
        Offset(0.2f, 0.2f), // Top left
        Offset(0.8f, 0.8f), // Bottom right
        Offset(0.2f, 0.8f), // Bottom left
        Offset(0.8f, 0.2f)  // Top right
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (gameState == 0) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.stabilizer_advanced_calib), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.stabilizer_calib_desc), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { gameState = 1; currentTargetIndex = 0; maxRadiusCalculated = 0f; avgDurationCalculated = 0L }) {
                    Text(stringResource(R.string.stabilizer_start_analysis))
                }
            }
        } else if (gameState == 2) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.stabilizer_analysis_complete),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Advanced Metrics Display
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.stabilizer_jitter_radius), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${maxRadiusCalculated.toInt()} px", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.stabilizer_avg_duration), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${avgDurationCalculated} ms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { gameState = 1; currentTargetIndex = 0; maxRadiusCalculated = 0f; avgDurationCalculated = 0L }) {
                    Text(stringResource(R.string.stabilizer_recalibrate))
                }
            }
        } else {
            // Playing State
            val target = targetPositions[currentTargetIndex]
            
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val width = constraints.maxWidth
                val height = constraints.maxHeight
                
                val absX = target.x * width
                val absY = target.y * height

                // The Target Dot
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { absX.toDp() - 28.dp },
                            y = with(density) { absY.toDp() - 28.dp }
                        )
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val downTime = System.currentTimeMillis()
                                val points = mutableListOf(down.position)
                                
                                // Collect points while pressed
                                do {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { if (it.pressed) points.add(it.position) }
                                } while (event.changes.any { it.pressed })
                                
                                val upTime = System.currentTimeMillis()
                                val duration = upTime - downTime
                                
                                // Calculate scatter from center of mass
                                val avgX = points.map { it.x }.average().toFloat()
                                val avgY = points.map { it.y }.average().toFloat()
                                
                                var maxR = 0f
                                for (p in points) {
                                    val dist = sqrt((p.x - avgX) * (p.x - avgX) + (p.y - avgY) * (p.y - avgY))
                                    if (dist > maxR) maxR = dist
                                }
                                
                                maxRadiusCalculated = max(maxRadiusCalculated, maxR)
                                
                                // Running average for duration
                                avgDurationCalculated = if (currentTargetIndex == 0) duration else (avgDurationCalculated * currentTargetIndex + duration) / (currentTargetIndex + 1)
                                
                                // Next target
                                if (currentTargetIndex < targetPositions.size - 1) {
                                    currentTargetIndex++
                                } else {
                                    onCalibrationComplete(maxRadiusCalculated)
                                    gameState = 2
                                }
                            }
                        }
                ) {
                    // Inner solid dot
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).align(Alignment.Center))
                }
                
                Text(
                    text = stringResource(R.string.stabilizer_tap_dot_prompt, currentTargetIndex + 1, 5),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
