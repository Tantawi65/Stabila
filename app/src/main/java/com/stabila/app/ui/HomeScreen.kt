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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stabila.core.R
import com.stabila.core.ui.Amber500
import com.stabila.core.ui.Emerald500
import com.stabila.core.ui.LocalAdaptiveParams
import com.stabila.core.ui.Red500
import com.stabila.core.ui.stabilizedClick

// ── Design Tokens ──────────────────────────────────────────────────────────────
private val TealPrimary   = Color(0xFF1B4F5C)
private val TealLight     = Color(0xFFE8F2F5)
private val WarmBg        = Color(0xFFF5F2ED)
private val CardWhite     = Color(0xFFFFFFFF)
private val DividerColor  = Color(0xFFEAE6E0)
private val TextPrimary   = Color(0xFF16140F)
private val TextSecondary = Color(0xFFA39D94)
private val AmberDot      = Color(0xFFD4845A)
private val GreenDot      = Color(0xFF4A9D7A)

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
            .background(WarmBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        // Header
        HomeHeader()

        Spacer(Modifier.height(20.dp))

        // Score ring
        ScoreRingCard(score = latestScore)

        Spacer(Modifier.height(14.dp))

        // Featured SteadyCam
        FeaturedCard(
            title = "SteadyCam",
            subtitle = "Stabilized camera · blur-free photos",
            icon = Icons.Default.CameraAlt,
            onClick = onNavigateToCamera,
            touchStabilizerEnabled = touchStabilizerEnabled
        )

        Spacer(Modifier.height(16.dp))

        // Section label
        Text(
            text = "QUICK ACCESS",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // 2×2 grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Touch Stabilizer",
                    icon = Icons.Default.TouchApp,
                    dotColor = TealPrimary,
                    onClick = onNavigateToTouchStabilizer,
                    touchStabilizerEnabled = touchStabilizerEnabled
                )
                SmallActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Adaptive Keyboard",
                    icon = Icons.Default.Keyboard,
                    dotColor = AmberDot,
                    onClick = onNavigateToKeyboardSetup,
                    touchStabilizerEnabled = touchStabilizerEnabled
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Auto Scroll",
                    icon = Icons.Default.Keyboard,
                    dotColor = TealPrimary,
                    onClick = onNavigateToAutoScroll,
                    touchStabilizerEnabled = touchStabilizerEnabled
                )
                SmallActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Daily Test",
                    icon = Icons.Default.MonitorHeart,
                    dotColor = GreenDot,
                    onClick = onNavigateToDailyTest,
                    touchStabilizerEnabled = touchStabilizerEnabled
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Divider before history
        HorizontalDivider(color = DividerColor, thickness = 1.dp)
        Spacer(Modifier.height(14.dp))

        // History row
        HistoryRow(
            onClick = onNavigateToHistory,
            touchStabilizerEnabled = touchStabilizerEnabled
        )

        Spacer(Modifier.height(28.dp))
    }
}

// ── Header ─────────────────────────────────────────────────────────────────────
@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Good morning",
                fontSize = 13.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(TealLight)
                .border(1.5.dp, TealPrimary.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "S",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TealPrimary
            )
        }
    }
}

// ── Score Ring Card ─────────────────────────────────────────────────────────────
@Composable
private fun ScoreRingCard(score: Float) {
    val ringColor = when {
        score < 0f  -> Color(0xFFCCC8C0)
        score < 30f -> Emerald500
        score < 65f -> Amber500
        else        -> Red500
    }
    val animatedProgress by animateFloatAsState(
        targetValue = (score / 100f).coerceIn(0f, 1f),
        animationSpec = tween(1400, easing = EaseInOutCubic),
        label = "ring"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardWhite)
            .border(1.dp, DividerColor, RoundedCornerShape(24.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = ringColor,
                    trackColor = Color(0xFFEDE9E3),
                    strokeWidth = 14.dp,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (score >= 0f) "${score.toInt()}" else "—",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (score >= 0f) TextPrimary else Color(0xFFCCC8C0)
                    )
                    if (score >= 0f) {
                        Text(text = "/ 100", fontSize = 14.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Today's Steadiness",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(Modifier.height(6.dp))

            if (score >= 0f) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = when {
                            score < 30f -> "Stable — low tremor"
                            score < 65f -> "Mild tremor detected"
                            else        -> "High tremor — rest up"
                        },
                        fontSize = 13.sp,
                        color = TealPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Text(
                    text = "Take a daily test to see your score",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

// ── Featured Card ───────────────────────────────────────────────────────────────
@Composable
private fun FeaturedCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    touchStabilizerEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(TealPrimary)
            .stabilizedClick(enabled = touchStabilizerEnabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(CardWhite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TealPrimary,
                modifier = Modifier.size(26.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = CardWhite)
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = CardWhite.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = CardWhite.copy(alpha = 0.8f),
            modifier = Modifier.size(22.dp)
        )
    }
}

// ── Small Action Tile ───────────────────────────────────────────────────────────
@Composable
private fun SmallActionTile(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    dotColor: Color,
    onClick: () -> Unit,
    touchStabilizerEnabled: Boolean
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(CardWhite)
            .border(1.dp, DividerColor, RoundedCornerShape(18.dp))
            .stabilizedClick(enabled = touchStabilizerEnabled, onClick = onClick)
            .padding(14.dp)
    ) {
        // Status dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
                .align(Alignment.TopEnd)
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(TealLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── History Row ─────────────────────────────────────────────────────────────────
@Composable
private fun HistoryRow(onClick: () -> Unit, touchStabilizerEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
            .stabilizedClick(enabled = touchStabilizerEnabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(TealPrimary)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "History", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(text = "Your last 7 tests", fontSize = 13.sp, color = TextSecondary)
        }
        MiniSparkline()
        Spacer(Modifier.width(10.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ── Mini Sparkline (Canvas) ─────────────────────────────────────────────────────
@Composable
private fun MiniSparkline() {
    val points = listOf(0.6f, 0.42f, 0.55f, 0.32f, 0.5f, 0.28f, 0.44f)
    Canvas(modifier = Modifier.width(58.dp).height(30.dp)) {
        val w = size.width
        val h = size.height
        val stepX = w / (points.size - 1)

        val path = Path()
        points.forEachIndexed { i, v ->
            val x = i * stepX
            val y = h - (v * h)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = TealPrimary,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        points.forEachIndexed { i, v ->
            drawCircle(
                color = TealPrimary,
                radius = 3.dp.toPx(),
                center = Offset(i * stepX, h - (v * h))
            )
        }
    }
}
