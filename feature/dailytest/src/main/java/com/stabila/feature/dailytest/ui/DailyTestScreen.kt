package com.stabila.feature.dailytest.ui

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stabila.core.ui.LocalAdaptiveParams
import androidx.compose.ui.draw.shadow

import androidx.compose.ui.res.stringResource
import com.stabila.core.R
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.stabila.core.domain.TestType
import com.stabila.core.ui.components.StabilaPrimaryButton
import com.stabila.core.ui.components.StabilaSecondaryButton
import com.stabila.feature.dailytest.DailyTestViewModel
import com.stabila.feature.dailytest.DailyTestViewModel.TestState



// Exact colors from React code
private val BgCream = Color(0xFFFAF7F2)
private val TextDark = Color(0xFF1C2430)
private val TextGray = Color(0xFF6B7280)
private val NavyPrimary = Color(0xFF2E4B6B)
private val GreenAccent = Color(0xFF6E8B6B)
private val CardWhite = Color(0xFFFFFFFF)
private val ShadowColor = Color(0xFF1C2430)

@Composable
fun DailyTestScreen(
    viewModel: DailyTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCream)
    ) {


        when (uiState.testState) {
            TestState.IDLE     -> IdleContent(
                medicationTag = uiState.medicationTag,
                onStart = viewModel::startTest,
                onLogMedication = viewModel::setMedicationTag
            )
            TestState.COUNTDOWN -> CountdownContent(seconds = uiState.countdownSeconds)
            TestState.RECORDING -> {
                if (uiState.testType == TestType.SPIRAL) {
                    SpiralCanvasContent(
                        onPointDrawn = { x, y, timestamp -> viewModel.addSpiralPoint(x, y, timestamp) },
                        onFinish = { viewModel.finishSpiralTest() }
                    )
                } else {
                    RecordingContent(
                        progress = uiState.recordingProgress,
                        magnitude = uiState.currentMagnitude,
                        testType = uiState.testType
                    )
                }
            }
            TestState.PROCESSING -> ProcessingContent()
            TestState.COMPLETE   -> ResultContent(
                result = uiState.result,
                onRetake = { viewModel.startTest(uiState.testType) },
                onDone = viewModel::resetTest
            )
        }


    }
}

// ─── IDLE ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IdleContent(
    medicationTag: String?,
    onStart: (TestType) -> Unit,
    onLogMedication: (String?) -> Unit
) {
    val context = LocalContext.current
    val adaptive = LocalAdaptiveParams.current
    var selectedType by remember { mutableStateOf(TestType.ACTION) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pulsing icon placeholder
        Box(
            modifier = Modifier
                .size(96.dp)
                .shadow(elevation = 8.dp, shape = CircleShape, spotColor = ShadowColor.copy(alpha = 0.12f), ambientColor = ShadowColor.copy(alpha = 0.12f))
                .clip(CircleShape)
                .background(CardWhite)
                .border(1.dp, TextDark.copy(alpha = 0.06f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.test_title),
                tint = NavyPrimary,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.test_title),
            style = MaterialTheme.typography.headlineMedium,
            color = TextDark,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )
        Spacer(Modifier.height(24.dp))
        
        // Test Type Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(adaptive.buttonHeight)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(100.dp), spotColor = ShadowColor.copy(alpha = 0.08f), ambientColor = ShadowColor.copy(alpha = 0.08f))
                .clip(RoundedCornerShape(100.dp))
                .background(CardWhite)
                .border(1.dp, TextDark.copy(alpha = 0.06f), RoundedCornerShape(100.dp)),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (selectedType == TestType.ACTION) NavyPrimary else Color.Transparent)
                    .clickable { selectedType = TestType.ACTION },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.test_type_action), 
                    color = if (selectedType == TestType.ACTION) CardWhite else TextDark, 
                    fontWeight = FontWeight.SemiBold, 
                    fontSize = 14.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (selectedType == TestType.SPIRAL) NavyPrimary else Color.Transparent)
                    .clickable { selectedType = TestType.SPIRAL },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.test_type_spiral), 
                    color = if (selectedType == TestType.SPIRAL) CardWhite else TextDark, 
                    fontWeight = FontWeight.SemiBold, 
                    fontSize = 14.sp
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        Text(
            text = when (selectedType) {
                TestType.ACTION -> stringResource(R.string.test_action_desc)
                TestType.SPIRAL -> stringResource(R.string.test_spiral_desc)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = TextGray,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        Spacer(Modifier.height(32.dp))
        
        // Medication Toggle
        Text(
            text = stringResource(R.string.test_medication_title),
            style = MaterialTheme.typography.labelMedium,
            color = TextGray
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(adaptive.buttonHeight)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(100.dp), spotColor = ShadowColor.copy(alpha = 0.08f), ambientColor = ShadowColor.copy(alpha = 0.08f))
                .clip(RoundedCornerShape(100.dp))
                .background(CardWhite)
                .border(1.dp, TextDark.copy(alpha = 0.06f), RoundedCornerShape(100.dp)),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                null to stringResource(R.string.test_medication_none),
                "pre-dose" to stringResource(R.string.test_medication_pre),
                "post-dose" to stringResource(R.string.test_medication_post)
            ).forEach { (tag, label) ->
                val isSelected = medicationTag == tag
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (isSelected) NavyPrimary else Color.Transparent)
                        .clickable { onLogMedication(tag) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label, 
                        color = if (isSelected) CardWhite else TextGray, 
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, 
                        fontSize = 13.sp
                    )
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        StabilaPrimaryButton(
            text = stringResource(R.string.test_start_button),
            onClick = { onStart(selectedType) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── COUNTDOWN ───────────────────────────────────────────────────────────────

@Composable
private fun CountdownContent(seconds: Int) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600, easing = EaseInOutCubic),
        label = "countdown_scale"
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.test_get_ready),
            style = MaterialTheme.typography.headlineSmall,
            color = TextGray
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "$seconds",
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp),
            color = NavyPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.test_measuring_in),
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray
        )
    }
}

// ─── RECORDING ───────────────────────────────────────────────────────────────

@Composable
private fun RecordingContent(progress: Float, magnitude: Float, testType: TestType) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100),
        label = "recording_progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.test_measuring),
                style = MaterialTheme.typography.headlineMedium,
                color = TextDark,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.test_measuring_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )
        }

        Column {
            // Live waveform
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), spotColor = ShadowColor.copy(alpha = 0.12f), ambientColor = ShadowColor.copy(alpha = 0.12f))
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, TextDark.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                    .background(CardWhite)
                    .padding(vertical = 8.dp)
            ) {
                TremorWaveform(magnitude = magnitude)
            }
            Spacer(Modifier.height(24.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(100.dp)),
                color = NavyPrimary,
                trackColor = TextDark.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = TextGray
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ─── PROCESSING ──────────────────────────────────────────────────────────────

@Composable
private fun ProcessingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = NavyPrimary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.test_analysing),
            style = MaterialTheme.typography.bodyLarge,
            color = TextGray
        )
    }
}

// ─── RESULT ──────────────────────────────────────────────────────────────────

@Composable
private fun ResultContent(
    result: com.stabila.feature.dailytest.sensor.SignalProcessor.TremorAnalysisResult?,
    onRetake: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        // Big score ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .shadow(elevation = 8.dp, shape = CircleShape, spotColor = ShadowColor.copy(alpha = 0.12f), ambientColor = ShadowColor.copy(alpha = 0.12f))
                .clip(CircleShape)
                .background(CardWhite)
                .border(1.dp, TextDark.copy(alpha = 0.06f), CircleShape)
        ) {
            val score = result?.overallScore ?: 0f
            val ringColor = when {
                score < 30f -> GreenAccent
                score < 65f -> Color(0xFFEAB308)
                else        -> Color(0xFFEF4444)
            }
            
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(190.dp),
                color = TextDark.copy(alpha = 0.1f),
                strokeWidth = 10.dp
            )
            
            CircularProgressIndicator(
                progress = { (score / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.size(190.dp),
                color = ringColor,
                strokeWidth = 10.dp,
                strokeCap = StrokeCap.Round
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${score.toInt()}",
                    fontSize = 64.sp,
                    fontFamily = FontFamily.Serif,
                    color = TextDark,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "/ 100",
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    color = TextGray
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        
        if (result?.classification != null && result.classification != com.stabila.core.domain.TremorClassification.UNCLASSIFIED) {
            val title = when (result.classification) {
                com.stabila.core.domain.TremorClassification.ESSENTIAL_TREMOR -> stringResource(R.string.test_essential_tremor)
                com.stabila.core.domain.TremorClassification.PARKINSONS -> stringResource(R.string.test_parkinsons)
                com.stabila.core.domain.TremorClassification.NORMAL -> stringResource(R.string.test_normal)
                else -> ""
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = NavyPrimary,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = when {
                    (result?.overallScore ?: 0f) < 30f -> stringResource(R.string.test_res_stable)
                    (result?.overallScore ?: 0f) < 65f -> stringResource(R.string.test_res_mild)
                    else                                -> stringResource(R.string.test_res_high)
                },
                style = MaterialTheme.typography.titleMedium,
                color = TextDark
            )
        }

        Spacer(Modifier.height(32.dp))

        // Detail cards
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            DetailCard(
                label = stringResource(R.string.test_dominant_freq),
                value = "${String.format("%.1f", result?.dominantFrequencyHz ?: 0f)} Hz",
                modifier = Modifier.weight(1f)
            )
            DetailCard(
                label = stringResource(R.string.test_amplitude),
                value = "${String.format("%.3f", result?.amplitude ?: 0f)}",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(40.dp))

        StabilaPrimaryButton(
            text = stringResource(R.string.test_done),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        StabilaSecondaryButton(
            text = stringResource(R.string.test_retake),
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DetailCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), spotColor = ShadowColor.copy(alpha = 0.12f), ambientColor = ShadowColor.copy(alpha = 0.12f))
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, TextDark.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
            .background(CardWhite)
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextGray
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = TextDark,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SpiralCanvasContent(
    onPointDrawn: (Float, Float, Long) -> Unit,
    onFinish: () -> Unit
) {
    var userPath by remember { mutableStateOf(Path()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.test_trace_spiral_title),
                style = MaterialTheme.typography.headlineMedium,
                color = TextDark,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.test_trace_spiral_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 24.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), spotColor = ShadowColor.copy(alpha = 0.12f), ambientColor = ShadowColor.copy(alpha = 0.12f))
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, TextDark.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
                .background(CardWhite)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull()
                            if (change != null && change.pressed) {
                                val position = change.position
                                onPointDrawn(position.x, position.y, System.currentTimeMillis())
                                
                                val newPath = Path().apply {
                                    addPath(userPath)
                                    if (change.previousPressed) {
                                        lineTo(position.x, position.y)
                                    } else {
                                        moveTo(position.x, position.y)
                                    }
                                }
                                userPath = newPath
                            }
                        }
                    }
                }
        ) {
            val outlineColor = TextDark.copy(alpha = 0.1f)
            val primaryColor = NavyPrimary
            
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                
                // Draw Guide Spiral
                val guidePath = Path()
                val maxTheta = 6.0 * Math.PI
                val a = 0f
                val b = (size.width / 2.5f) / maxTheta.toFloat()
                
                var theta = 0f
                var isFirst = true
                while (theta <= maxTheta) {
                    val r = a + b * theta
                    val x = center.x + r * kotlin.math.cos(theta)
                    val y = center.y + r * kotlin.math.sin(theta)
                    if (isFirst) {
                        guidePath.moveTo(x, y)
                        isFirst = false
                    } else {
                        guidePath.lineTo(x, y)
                    }
                    theta += 0.1f
                }
                
                drawPath(
                    path = guidePath,
                    color = outlineColor,
                    style = Stroke(
                        width = 8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
                
                // Draw User Path
                drawPath(
                    path = userPath,
                    color = primaryColor,
                    style = Stroke(
                        width = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        }

        StabilaPrimaryButton(
            text = stringResource(R.string.test_finish_tracing),
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
    }
}

