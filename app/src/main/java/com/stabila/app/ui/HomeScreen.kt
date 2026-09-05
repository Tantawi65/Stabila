package com.stabila.app.ui

import androidx.compose.ui.res.stringResource
import com.stabila.core.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.TouchApp
import com.stabila.core.ui.stabilizedClick
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stabila.core.ui.Amber500
import com.stabila.core.ui.Emerald500
import com.stabila.core.ui.LocalAdaptiveParams
import com.stabila.core.ui.Red500
import com.stabila.core.ui.TremorLevel
import com.stabila.core.ui.Violet500

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
    val adaptive = LocalAdaptiveParams.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(adaptive.spacingUnit + 8.dp))

        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = (26 * adaptive.fontScale).sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            // Brand mark — subtle glowing orb
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), MaterialTheme.colorScheme.primary.copy(alpha = 0f))
                        )
                    )
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        Spacer(Modifier.height(adaptive.spacingUnit + 4.dp))

        // ── ATI Banner (only visible on elevated/severe days) ─────────────────
        AnimatedVisibility(
            visible = adaptive.isHighTremorMode,
            enter = expandVertically() + fadeIn()
        ) {
            val bannerColor = if (adaptive.tremorLevel == TremorLevel.SEVERE) Red500 else Amber500
            val bannerMsg = when (adaptive.tremorLevel) {
                TremorLevel.ELEVATED -> stringResource(R.string.home_elevated_banner)
                TremorLevel.SEVERE -> stringResource(R.string.home_severe_banner)
                else -> ""
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(bannerColor.copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(bannerColor)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = bannerMsg,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = (12 * adaptive.fontScale).sp
                    ),
                    color = bannerColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(adaptive.spacingUnit))
        }

        Spacer(Modifier.height(adaptive.spacingUnit))

        // ── Score Ring Card ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface)
                    )
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val score = latestScore
                val ringColor = when {
                    score < 0f -> MaterialTheme.colorScheme.outline
                    score < 30f -> Emerald500
                    score < 65f -> Amber500
                    else -> Red500
                }
                val ringProgress = (score / 100f).coerceIn(0f, 1f)
                val animatedProgress by animateFloatAsState(
                    targetValue = ringProgress,
                    animationSpec = tween(1200, easing = EaseInOutCubic),
                    label = "score_ring"
                )

                // Score ring
                val ringSize = (160 * adaptive.fontScale).dp.coerceIn(140.dp, 200.dp)
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ringSize)) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = ringColor,
                        trackColor = MaterialTheme.colorScheme.outline,
                        strokeWidth = 10.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (score >= 0f) {
                            Text(
                                text = "${score.toInt()}",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontSize = (36 * adaptive.fontScale).sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "/ 100",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "—",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontSize = (36 * adaptive.fontScale).sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = when {
                        score < 0f -> stringResource(R.string.home_score_empty)
                        score < 30f -> stringResource(R.string.home_score_stable)
                        score < 65f -> stringResource(R.string.home_score_mild)
                        else -> stringResource(R.string.home_score_high)
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (14 * adaptive.fontScale).sp
                    ),
                    color = if (score < 0f) MaterialTheme.colorScheme.onSurfaceVariant else ringColor,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )

                if (score >= 0f) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.home_score_latest_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(adaptive.spacingUnit + 8.dp))

        // ── Quick Actions ─────────────────────────────────────────────────────
        Text(
            text = stringResource(R.string.home_quick_actions),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = (16 * adaptive.fontScale).sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = adaptive.spacingUnit / 2)
        )

        Spacer(Modifier.height(8.dp))

        if (adaptive.isHighTremorMode) {
            // One per row
            Column(verticalArrangement = Arrangement.spacedBy(adaptive.spacingUnit / 2)) {
                ActionCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.home_action_test_title),
                    subtitle = stringResource(R.string.home_action_test_subtitle),
                    icon = Icons.Default.MonitorHeart,
                    accentColor = MaterialTheme.colorScheme.primary,
                    adaptiveHeight = (120 * adaptive.fontScale).dp.coerceIn(110.dp, 160.dp),
                    onClick = onNavigateToDailyTest
                )
                ActionCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.home_action_camera_title),
                    subtitle = stringResource(R.string.home_action_camera_subtitle),
                    icon = Icons.Default.CameraAlt,
                    accentColor = Violet500,
                    adaptiveHeight = (120 * adaptive.fontScale).dp.coerceIn(110.dp, 160.dp),
                    onClick = onNavigateToCamera
                )
                ActionCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.home_action_history_title),
                    subtitle = stringResource(R.string.home_action_history_subtitle),
                    icon = Icons.Default.History,
                    accentColor = Emerald500,
                    adaptiveHeight = (120 * adaptive.fontScale).dp.coerceIn(110.dp, 160.dp),
                    onClick = onNavigateToHistory
                )
                ActionCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.home_action_autoscroll_title),
                    subtitle = stringResource(R.string.home_action_autoscroll_subtitle),
                    icon = Icons.Default.Keyboard,
                    accentColor = MaterialTheme.colorScheme.primary,
                    adaptiveHeight = (120 * adaptive.fontScale).dp.coerceIn(110.dp, 160.dp),
                    onClick = onNavigateToAutoScroll
                )
                ActionCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.home_action_keyboard_title),
                    subtitle = stringResource(R.string.home_action_keyboard_subtitle),
                    icon = Icons.Default.Keyboard,
                    accentColor = MaterialTheme.colorScheme.primary,
                    adaptiveHeight = (120 * adaptive.fontScale).dp.coerceIn(110.dp, 160.dp),
                    onClick = onNavigateToKeyboardSetup
                )
                ActionCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.home_action_stabilizer_title),
                    subtitle = stringResource(R.string.home_action_stabilizer_subtitle),
                    icon = Icons.Default.TouchApp,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    adaptiveHeight = (120 * adaptive.fontScale).dp.coerceIn(110.dp, 160.dp),
                    onClick = onNavigateToTouchStabilizer
                )
                ActionCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.home_action_settings_title),
                    subtitle = stringResource(R.string.home_action_settings_subtitle),
                    icon = Icons.Default.Settings,
                    accentColor = Amber500,
                    adaptiveHeight = (120 * adaptive.fontScale).dp.coerceIn(110.dp, 160.dp),
                    onClick = onNavigateToSettings
                )
            }
        } else {
            // Two per row
            Column(verticalArrangement = Arrangement.spacedBy(adaptive.spacingUnit / 2)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(adaptive.spacingUnit / 2)
                ) {
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.home_action_test_title),
                        subtitle = stringResource(R.string.home_action_test_subtitle),
                        icon = Icons.Default.MonitorHeart,
                        accentColor = MaterialTheme.colorScheme.primary,
                        adaptiveHeight = (120 * adaptive.fontScale).dp.coerceIn(110.dp, 160.dp),
                        onClick = onNavigateToDailyTest
                    )
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.home_action_camera_title),
                        subtitle = stringResource(R.string.home_action_camera_subtitle),
                        icon = Icons.Default.CameraAlt,
                        accentColor = Violet500,
                        adaptiveHeight = (120 * adaptive.fontScale).dp.coerceIn(110.dp, 160.dp),
                        onClick = onNavigateToCamera
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(adaptive.spacingUnit / 2)
                ) {
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.home_action_history_title),
                        subtitle = stringResource(R.string.home_action_history_subtitle),
                        icon = Icons.Default.History,
                        accentColor = Emerald500,
                        adaptiveHeight = (120 * adaptive.fontScale).dp.coerceIn(110.dp, 160.dp),
                        onClick = onNavigateToHistory
                    )
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.home_action_autoscroll_title),
                        subtitle = stringResource(R.string.home_action_autoscroll_subtitle),
                        icon = Icons.Default.Keyboard,
                        accentColor = MaterialTheme.colorScheme.primary,
                        adaptiveHeight = (120 * adaptive.fontScale).dp.coerceIn(110.dp, 160.dp),
                        onClick = onNavigateToAutoScroll
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(adaptive.spacingUnit / 2)
                ) {
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.home_action_keyboard_title),
                        subtitle = stringResource(R.string.home_action_keyboard_subtitle),
                        icon = Icons.Default.Keyboard,
                        accentColor = MaterialTheme.colorScheme.primary,
                        adaptiveHeight = (120 * adaptive.fontScale).dp.coerceIn(110.dp, 160.dp),
                        onClick = onNavigateToKeyboardSetup
                    )
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.home_action_stabilizer_title),
                        subtitle = stringResource(R.string.home_action_stabilizer_subtitle),
                        icon = Icons.Default.TouchApp,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        adaptiveHeight = (120 * adaptive.fontScale).dp.coerceIn(110.dp, 160.dp),
                        onClick = onNavigateToTouchStabilizer
                    )
                }
                
                ActionCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.home_action_settings_title),
                    subtitle = stringResource(R.string.home_action_settings_subtitle),
                    icon = Icons.Default.Settings,
                    accentColor = Amber500,
                    adaptiveHeight = (120 * adaptive.fontScale).dp.coerceIn(110.dp, 160.dp),
                    onClick = onNavigateToSettings
                )
            }
        }

        Spacer(Modifier.height(adaptive.spacingUnit + 8.dp))
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    adaptiveHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val adaptive = LocalAdaptiveParams.current
    val viewModel: HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val touchStabilizerEnabled by viewModel.touchStabilizerEnabled.collectAsState(initial = false)
    
    Box(
        modifier = modifier
            .height(adaptiveHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(accentColor.copy(alpha = 0.18f), MaterialTheme.colorScheme.surface)
                )
            )
            .stabilizedClick(enabled = touchStabilizerEnabled, onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        // Accent orb in top-right
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f))
                .align(Alignment.TopEnd),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (14 * adaptive.fontScale).sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = (11 * adaptive.fontScale).sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

