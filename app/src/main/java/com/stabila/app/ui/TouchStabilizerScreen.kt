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

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import com.stabila.core.ui.components.StabilaPrimaryButton
import com.stabila.core.ui.LocalAdaptiveParams

import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sqrt

import androidx.compose.ui.res.stringResource
import com.stabila.app.R


private val BgCream = Color(0xFFFAF7F2)
private val NavyPrimary = Color(0xFF1A1F36)
private val TextDark = Color(0xFF2D3748)
private val TextGray = Color(0xFF718096)
private val CardWhite = Color(0xFFFFFFFF)
private val ShadowColor = Color(0xFF1A1F36)

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
    
    val adaptive = LocalAdaptiveParams.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stabilizer_title), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.generic_back), tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgCream,
                    titleContentColor = TextDark
                )
            )
        },
        containerColor = BgCream
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Column {
                Text(
                    text = stringResource(R.string.stabilizer_global_title),
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.stabilizer_desc),
                    fontSize = 16.sp,
                    color = TextGray
                )
            }

            // Calibration Game Area
            CalibrationGameBox(
                savedRadius = savedRadius,
                onCalibrationComplete = { radius ->
                    viewModel.setTouchTremorRadius(radius)
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Enable Toggle Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .shadow(8.dp, androidx.compose.foundation.shape.RoundedCornerShape(24.dp), spotColor = ShadowColor.copy(alpha = 0.12f))
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                    .background(CardWhite)
                    .border(1.dp, TextDark.copy(alpha = 0.06f), androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = stringResource(R.string.stabilizer_enable_system),
                            fontSize = 18.sp,
                            color = TextDark,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.stabilizer_require_accessibility),
                            fontSize = 13.sp,
                            color = TextGray
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
                                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:")))
                                } else {
                                    viewModel.setTouchStabilizerEnabled(true)
                                }
                            } else {
                                viewModel.setTouchStabilizerEnabled(false)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = NavyPrimary)
                    )
                }
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
    
    val targetPositions = listOf(
        Offset(0.5f, 0.5f),
        Offset(0.2f, 0.2f),
        Offset(0.8f, 0.8f),
        Offset(0.2f, 0.8f),
        Offset(0.8f, 0.2f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .shadow(12.dp, androidx.compose.foundation.shape.RoundedCornerShape(24.dp), spotColor = ShadowColor.copy(alpha = 0.12f))
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .background(CardWhite)
            .border(1.dp, TextDark.copy(alpha = 0.06f), androidx.compose.foundation.shape.RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (gameState == 0) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text(stringResource(R.string.stabilizer_advanced_calib), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.stabilizer_calib_desc), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = TextGray, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(24.dp))
                StabilaPrimaryButton(
                    text = stringResource(R.string.stabilizer_start_analysis),
                    onClick = { gameState = 1; currentTargetIndex = 0; maxRadiusCalculated = 0f; avgDurationCalculated = 0L },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (gameState == 2) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.stabilizer_analysis_complete),
                    fontSize = 20.sp,
                    color = NavyPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.stabilizer_jitter_radius), fontSize = 14.sp, color = TextGray)
                        Text(" px", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.stabilizer_avg_duration), fontSize = 14.sp, color = TextGray)
                        Text(" ms", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                StabilaPrimaryButton(
                    text = stringResource(R.string.stabilizer_recalibrate),
                    onClick = { gameState = 1; currentTargetIndex = 0; maxRadiusCalculated = 0f; avgDurationCalculated = 0L },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // Playing State
            val target = targetPositions[currentTargetIndex]
            
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val width = constraints.maxWidth
                val height = constraints.maxHeight
                
                val absX = target.x * width
                val absY = target.y * height

                // Target Dot
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { absX.toDp() - 28.dp },
                            y = with(density) { absY.toDp() - 28.dp }
                        )
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(NavyPrimary.copy(alpha = 0.15f))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val downTime = System.currentTimeMillis()
                                val points = mutableListOf(down.position)
                                
                                do {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { if (it.pressed) points.add(it.position) }
                                } while (event.changes.any { it.pressed })
                                
                                val upTime = System.currentTimeMillis()
                                val duration = upTime - downTime
                                
                                val avgX = points.map { it.x }.average().toFloat()
                                val avgY = points.map { it.y }.average().toFloat()
                                
                                var maxR = 0f
                                for (p in points) {
                                    val dist = sqrt((p.x - avgX) * (p.x - avgX) + (p.y - avgY) * (p.y - avgY))
                                    if (dist > maxR) maxR = dist
                                }
                                
                                maxRadiusCalculated = max(maxRadiusCalculated, maxR)
                                avgDurationCalculated = if (currentTargetIndex == 0) duration else (avgDurationCalculated * currentTargetIndex + duration) / (currentTargetIndex + 1)
                                
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
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(NavyPrimary).align(Alignment.Center))
                }
                
                Text(
                    text = stringResource(R.string.stabilizer_tap_dot_prompt, currentTargetIndex + 1, 5),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    color = TextGray
                )
            }
        }
    }
}

