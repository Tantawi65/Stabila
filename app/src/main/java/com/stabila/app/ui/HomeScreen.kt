package com.stabila.app.ui

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stabila.core.ui.LocalAdaptiveParams
import com.stabila.core.ui.stabilizedClick
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// Exact colors from React code
private val BgCream = Color(0xFFFAF7F2)
private val TextDark = Color(0xFF1C2430)
private val TextGray = Color(0xFF6B7280)
private val NavyPrimary = Color(0xFF2E4B6B)
private val GreenAccent = Color(0xFF6E8B6B)
private val CardWhite = Color(0xFFFFFFFF)
private val ShadowColor = Color(0xFF1C2430)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToDailyTest: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToKeyboardSetup: () -> Unit,
    onNavigateToTouchStabilizer: () -> Unit,
    onNavigateToAutoScroll: () -> Unit
) {
    val latestScore by viewModel.latestScore.collectAsState()
    val touchStabilizerEnabled by viewModel.touchStabilizerEnabled.collectAsState(initial = false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCream)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        // Header
        HeaderSection()

        Spacer(Modifier.height(24.dp))

        // Score Ring Card
        ScoreRingCard(score = latestScore)

        Spacer(Modifier.height(24.dp))

        // SteadyCam Card
        SteadyCamCard(
            onClick = onNavigateToCamera,
            touchStabilizerEnabled = touchStabilizerEnabled
        )

        Spacer(Modifier.height(16.dp))

        // 2x2 Grid
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GridTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.TouchApp,
                    iconColor = TextDark,
                    label = "Touch\nStabilizer",
                    showGreenDot = true,
                    onClick = onNavigateToTouchStabilizer,
                    touchStabilizerEnabled = touchStabilizerEnabled
                )
                GridTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Keyboard,
                    iconColor = TextDark,
                    label = "Adaptive\nKeyboard",
                    showGreenDot = false,
                    onClick = onNavigateToKeyboardSetup,
                    touchStabilizerEnabled = touchStabilizerEnabled
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GridTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.KeyboardArrowDown,
                    iconColor = TextDark,
                    label = "Auto-Scroll",
                    showGreenDot = false,
                    onClick = onNavigateToAutoScroll,
                    touchStabilizerEnabled = touchStabilizerEnabled
                )
                GridTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.MonitorHeart,
                    iconColor = NavyPrimary,
                    label = "Daily Test",
                    showGreenDot = false,
                    onClick = onNavigateToDailyTest,
                    touchStabilizerEnabled = touchStabilizerEnabled
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // History Row
        HistoryRow(
            onClick = onNavigateToHistory,
            touchStabilizerEnabled = touchStabilizerEnabled
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun HeaderSection() {
    val adaptive = LocalAdaptiveParams.current
    val dateText = try {
        val date = LocalDate.now()
        date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)).uppercase()
    } catch (e: Exception) { "TUESDAY, APRIL 9" }

    Column {
        Text(
            text = "Good morning, Mohamed",
            fontSize = 24.sp * adaptive.fontScale,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Serif,
            color = TextDark,
            lineHeight = 32.sp * adaptive.fontScale
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = dateText,
            fontSize = 12.sp * adaptive.fontScale,
            fontWeight = FontWeight.Medium,
            color = TextGray,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun ScoreRingCard(score: Float) {
    val adaptive = LocalAdaptiveParams.current
    val displayScore = if (score >= 0f) score else 78f // fallback for design demo
    val animatedProgress by animateFloatAsState(
        targetValue = (displayScore / 100f).coerceIn(0f, 1f),
        animationSpec = tween(1400, easing = EaseInOutCubic),
        label = "ring"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), spotColor = ShadowColor.copy(alpha = 0.12f), ambientColor = ShadowColor.copy(alpha = 0.12f))
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, TextDark.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
            .background(CardWhite)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background ticks
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = 86.dp.toPx()
                    for (i in 0 until 12) {
                        val angle = Math.toRadians((i * 30).toDouble())
                        val startX = center.x + (radius + 2.dp.toPx()) * cos(angle).toFloat()
                        val startY = center.y + (radius + 2.dp.toPx()) * sin(angle).toFloat()
                        val endX = center.x + (radius - 6.dp.toPx()) * cos(angle).toFloat()
                        val endY = center.y + (radius - 6.dp.toPx()) * sin(angle).toFloat()
                        drawLine(
                            color = TextDark.copy(alpha = 0.3f),
                            start = androidx.compose.ui.geometry.Offset(startX, startY),
                            end = androidx.compose.ui.geometry.Offset(endX, endY),
                            strokeWidth = 2f
                        )
                    }
                }

                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(172.dp),
                    color = TextDark.copy(alpha = 0.1f),
                    strokeWidth = 8.dp
                )

                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(172.dp),
                    color = NavyPrimary,
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round
                )

                Row(verticalAlignment = Alignment.Bottom) {
                    val scoreFontSize = if (displayScore >= 100f) 52.sp else 60.sp
                    Text(
                        text = "${displayScore.toInt()}",
                        fontSize = scoreFontSize * adaptive.fontScale,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Serif,
                        color = TextDark,
                        lineHeight = scoreFontSize * adaptive.fontScale
                    )
                    Text(
                        text = "/100",
                        fontSize = 20.sp * adaptive.fontScale,
                        fontFamily = FontFamily.Serif,
                        color = TextGray,
                        modifier = Modifier.padding(start = 4.dp, bottom = if (displayScore >= 100f) 2.dp else 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Today's Steadiness",
                fontSize = 18.sp * adaptive.fontScale,
                fontWeight = FontWeight.Medium,
                color = TextDark
            )

            Spacer(Modifier.height(8.dp))

            val statusText = if (score >= 0f) {
                when {
                    score < 30f -> "Stable — excellent day"
                    score < 65f -> "Mild tremor detected"
                    else        -> "High tremor — take it easy"
                }
            } else "Take your daily test"

            val statusIcon = if (score >= 65f) Icons.Default.MonitorHeart else Icons.Default.TrendingUp

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = GreenAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = statusText,
                    fontSize = 16.sp * adaptive.fontScale,
                    fontWeight = FontWeight.Medium,
                    color = GreenAccent
                )
            }
        }
    }
}

@Composable
private fun SteadyCamCard(onClick: () -> Unit, touchStabilizerEnabled: Boolean) {
    val adaptive = LocalAdaptiveParams.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), spotColor = ShadowColor.copy(alpha = 0.16f), ambientColor = ShadowColor.copy(alpha = 0.16f))
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, CardWhite.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .background(NavyPrimary)
            .stabilizedClick(enabled = touchStabilizerEnabled, onClick = onClick)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(CardWhite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = NavyPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SteadyCam",
                fontSize = 24.sp * adaptive.fontScale,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = CardWhite
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Stabilized camera for steady, blur-free photos",
                fontSize = 18.sp * adaptive.fontScale,
                color = CardWhite.copy(alpha = 0.85f),
                lineHeight = 24.sp * adaptive.fontScale
            )
        }
    }
}

@Composable
private fun GridTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    label: String,
    showGreenDot: Boolean,
    onClick: () -> Unit,
    touchStabilizerEnabled: Boolean
) {
    val adaptive = LocalAdaptiveParams.current
    Box(
        modifier = modifier
            .height(130.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), spotColor = ShadowColor.copy(alpha = 0.1f), ambientColor = ShadowColor.copy(alpha = 0.1f))
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, TextDark.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
            .background(CardWhite)
            .stabilizedClick(enabled = touchStabilizerEnabled, onClick = onClick)
            .padding(16.dp)
    ) {
        if (showGreenDot) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(GreenAccent)
                    .align(Alignment.TopEnd)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 18.sp * adaptive.fontScale,
                fontWeight = FontWeight.Medium,
                color = TextDark,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp * adaptive.fontScale
            )
        }
    }
}

@Composable
private fun HistoryRow(onClick: () -> Unit, touchStabilizerEnabled: Boolean) {
    val adaptive = LocalAdaptiveParams.current
    Column(modifier = Modifier.fillMaxWidth()) {
        // Top border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TextDark)
        )
        
        Spacer(Modifier.height(24.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .stabilizedClick(enabled = touchStabilizerEnabled, onClick = onClick)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "History",
                    fontSize = 20.sp * adaptive.fontScale,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Serif,
                    color = TextDark
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Your last 7 tests",
                    fontSize = 18.sp * adaptive.fontScale,
                    color = TextGray
                )
            }

            // Sparkline
            val pts = listOf(0.8f, 0.65f, 0.75f, 0.5f, 0.4f, 0.2f, 0.1f)
            Canvas(modifier = Modifier.width(80.dp).height(32.dp)) {
                val w = size.width
                val h = size.height
                val step = w / (pts.size - 1)
                val path = Path()
                pts.forEachIndexed { i, v ->
                    val x = i * step
                    val y = v * h
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = NavyPrimary,
                    style = Stroke(width = 2.5f.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
