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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle

import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.ui.text.style.TextOverflow

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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.scale


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

@Composable
private fun IdleContent(
    medicationTag: String?,
    onStart: (TestType) -> Unit,
    onLogMedication: (String?) -> Unit
) {
    val adaptive = LocalAdaptiveParams.current
    var selectedType by remember { mutableStateOf(TestType.ACTION) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))

        // Hero Header
        Text(
            text = "Daily Assessment",
            fontSize = 36.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Choose how you'd like to measure your steadiness today.",
            fontSize = 16.sp,
            color = TextGray,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(48.dp))

        // Advanced Animated Test Cards
        AdvancedTestCard(
            title = stringResource(R.string.test_type_action),
            subtitle = stringResource(R.string.test_action_desc),
            icon = Icons.Default.TouchApp,
            selected = selectedType == TestType.ACTION,
            onClick = { selectedType = TestType.ACTION }
        )
        
        Spacer(Modifier.height(16.dp))
        
        AdvancedTestCard(
            title = stringResource(R.string.test_type_spiral),
            subtitle = stringResource(R.string.test_spiral_desc),
            icon = Icons.Default.Edit,
            selected = selectedType == TestType.SPIRAL,
            onClick = { selectedType = TestType.SPIRAL }
        )
        
        Spacer(Modifier.height(48.dp))
        
        // Medication Section
        Text(
            text = "Medication Status",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextDark,
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
        )
        Spacer(Modifier.height(12.dp))
        AnimatedMedicationSelector(selectedTag = medicationTag, onSelect = onLogMedication)

        Spacer(Modifier.height(48.dp))
        
        StabilaPrimaryButton(
            text = stringResource(R.string.test_start_button),
            onClick = { onStart(selectedType) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
        )
    }
}

@Composable
fun AdvancedTestCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (selected) 1.02f else 1f)
    val elevation by animateDpAsState(if (selected) 16.dp else 2.dp)
    val borderColor by animateColorAsState(if (selected) NavyPrimary else Color.Transparent)
    val bgColor by animateColorAsState(if (selected) CardWhite else CardWhite.copy(alpha = 0.6f))
    val adaptive = LocalAdaptiveParams.current
    
    // Base height grows slightly if high tremor, but text stays normal.
    val cardMinHeight = if (adaptive.isHighTremorMode) 140.dp else 100.dp
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(elevation, RoundedCornerShape(24.dp), spotColor = ShadowColor.copy(alpha = 0.15f))
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = cardMinHeight - 48.dp) // minus padding
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (selected) NavyPrimary else BgCream),
                contentAlignment = Alignment.Center
            ) {
                 Icon(icon, contentDescription = null, tint = if (selected) CardWhite else NavyPrimary)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                 Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                 Spacer(Modifier.height(4.dp))
                 Text(subtitle, fontSize = 14.sp, color = TextGray, lineHeight = 18.sp)
            }
            if (selected) {
                 Spacer(Modifier.width(8.dp))
                 Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
fun AnimatedMedicationSelector(selectedTag: String?, onSelect: (String?) -> Unit) {
    val adaptive = LocalAdaptiveParams.current
    val selectorHeight = if (adaptive.isHighTremorMode) 72.dp else 56.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(selectorHeight)
            .background(TextDark.copy(alpha = 0.05f), RoundedCornerShape(100.dp))
            .padding(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            listOf(
                null to "None", 
                "pre-dose" to "Pre-dose", 
                "post-dose" to "Post-dose"
            ).forEach { (tag, label) ->
                val isSelected = selectedTag == tag
                val bgColor by animateColorAsState(if (isSelected) CardWhite else Color.Transparent)
                val elevation by animateDpAsState(if (isSelected) 4.dp else 0.dp)
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .shadow(elevation, RoundedCornerShape(100.dp), spotColor = ShadowColor.copy(alpha = 0.1f))
                        .clip(RoundedCornerShape(100.dp))
                        .background(bgColor)
                        .clickable { onSelect(tag) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label, 
                        color = if (isSelected) TextDark else TextGray, 
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }
            }
        }
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


