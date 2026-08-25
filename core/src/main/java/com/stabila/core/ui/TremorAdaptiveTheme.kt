package com.stabila.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tremor-aware UI scaling system.
 *
 * The last tremor test score (0–100) drives adaptive parameters that all
 * composables can read from [LocalAdaptiveParams]. Higher scores expand
 * interactive targets, increase spacing, and simplify controls — making
 * the UI genuinely more usable on high-tremor days.
 *
 * Usage:
 *   TremorAdaptiveTheme(score = latestScore) {
 *       // All children can read LocalAdaptiveParams.current
 *   }
 */

enum class TremorLevel {
    STABLE,   // 0–30: crisp, detailed UI
    MILD,     // 31–60: slightly expanded
    ELEVATED, // 61–80: accessibility mode
    SEVERE    // 81–100: extreme accessibility
}

/**
 * All UI scaling parameters derived from the tremor score.
 *
 * @param buttonHeight Minimum height for all interactive buttons
 * @param spacingUnit Base spacing unit; multiply this for padding/gaps
 * @param fontScale Multiplier applied to headline font sizes
 * @param isHighTremorMode True when ELEVATED or SEVERE — hides complex gestures
 * @param tremorLevel The categorical level for banner display
 * @param score Raw score 0–100 for fine-grained interpolation
 */
data class AdaptiveParams(
    val buttonHeight: Dp,
    val spacingUnit: Dp,
    val fontScale: Float,
    val isHighTremorMode: Boolean,
    val tremorLevel: TremorLevel,
    val score: Float
)

/**
 * CompositionLocal providing adaptive UI parameters throughout the tree.
 * Default value is STABLE (normal healthy user, no test taken yet).
 */
val LocalAdaptiveParams = staticCompositionLocalOf {
    AdaptiveParams(
        buttonHeight = 56.dp,
        spacingUnit = 16.dp,
        fontScale = 1.0f,
        isHighTremorMode = false,
        tremorLevel = TremorLevel.STABLE,
        score = 0f
    )
}

/**
 * Calculates adaptive parameters from a raw tremor score.
 * Uses smooth step interpolation so the UI grows gradually, not in jumps.
 */
fun scoreToAdaptiveParams(score: Float): AdaptiveParams {
    val s = score.coerceIn(0f, 100f)

    // Smooth interpolation within each band
    return when {
        s <= 30f -> {
            val t = s / 30f
            AdaptiveParams(
                buttonHeight = lerp(56f, 62f, t).dp,
                spacingUnit = lerp(16f, 18f, t).dp,
                fontScale = lerp(1.0f, 1.05f, t),
                isHighTremorMode = false,
                tremorLevel = TremorLevel.STABLE,
                score = s
            )
        }
        s <= 60f -> {
            val t = (s - 30f) / 30f
            AdaptiveParams(
                buttonHeight = lerp(62f, 76f, t).dp,
                spacingUnit = lerp(18f, 22f, t).dp,
                fontScale = lerp(1.05f, 1.2f, t),
                isHighTremorMode = false,
                tremorLevel = TremorLevel.MILD,
                score = s
            )
        }
        s <= 80f -> {
            val t = (s - 60f) / 20f
            AdaptiveParams(
                buttonHeight = lerp(76f, 88f, t).dp,
                spacingUnit = lerp(22f, 26f, t).dp,
                fontScale = lerp(1.2f, 1.32f, t),
                isHighTremorMode = true,
                tremorLevel = TremorLevel.ELEVATED,
                score = s
            )
        }
        else -> {
            val t = (s - 80f) / 20f
            AdaptiveParams(
                buttonHeight = lerp(88f, 100f, t).dp,
                spacingUnit = lerp(26f, 32f, t).dp,
                fontScale = lerp(1.32f, 1.45f, t),
                isHighTremorMode = true,
                tremorLevel = TremorLevel.SEVERE,
                score = s
            )
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction.coerceIn(0f, 1f)

/**
 * Root composable that provides tremor-adaptive parameters to all children.
 * Place this near the top of the composition tree, just inside [StabilaTheme].
 *
 * @param score The latest tremor test score (0–100). Defaults to 0 (stable).
 */
@Composable
fun TremorAdaptiveTheme(
    score: Float = 0f,
    content: @Composable () -> Unit
) {
    val params = remember(score) { scoreToAdaptiveParams(score) }
    CompositionLocalProvider(LocalAdaptiveParams provides params) {
        content()
    }
}
