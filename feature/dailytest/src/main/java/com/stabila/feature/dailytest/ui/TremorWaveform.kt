package com.stabila.feature.dailytest.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private const val MAX_SAMPLES = 120

/**
 * A scrolling waveform that fills with sensor magnitude data in real-time.
 * Draws a smooth cubic bezier path through the data points.
 */
@Composable
fun TremorWaveform(
    magnitude: Float,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF6366F1),
    fillStartColor: Color = Color(0xFF6366F1).copy(alpha = 0.4f),
    fillEndColor: Color = Color(0xFF6366F1).copy(alpha = 0.0f),
) {
    // Ring buffer of magnitudes
    val samples = remember { mutableStateListOf<Float>() }

    // Animate smoothly to new magnitude
    val animatedMagnitude by animateFloatAsState(
        targetValue = magnitude,
        animationSpec = tween(durationMillis = 80),
        label = "waveform_magnitude"
    )

    // Push sample
    if (samples.size >= MAX_SAMPLES) samples.removeAt(0)
    samples.add(animatedMagnitude)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color.Transparent)
    ) {
        if (samples.size < 2) return@Canvas

        val w = size.width
        val h = size.height

        // Normalise: find range
        val maxVal = samples.maxOrNull()?.coerceAtLeast(0.01f) ?: 0.01f
        val step = w / (MAX_SAMPLES - 1).toFloat()

        fun xOf(i: Int) = i * step
        fun yOf(v: Float): Float {
            // Gravity magnitude is ~9.81 when still. We subtract 9.81 to centre around zero.
            val normalised = ((v - 9.81f) / maxVal.coerceAtLeast(0.1f)).coerceIn(-1f, 1f)
            return h / 2f - normalised * (h / 2f * 0.9f)
        }

        val path = Path()
        val fillPath = Path()

        path.moveTo(xOf(0), yOf(samples[0]))
        fillPath.moveTo(xOf(0), h)
        fillPath.lineTo(xOf(0), yOf(samples[0]))

        for (i in 1 until samples.size) {
            val x0 = xOf(i - 1)
            val y0 = yOf(samples[i - 1])
            val x1 = xOf(i)
            val y1 = yOf(samples[i])
            val cx = (x0 + x1) / 2f
            path.cubicTo(cx, y0, cx, y1, x1, y1)
            fillPath.cubicTo(cx, y0, cx, y1, x1, y1)
        }

        // Close fill path
        fillPath.lineTo(xOf(samples.size - 1), h)
        fillPath.close()

        // Draw fill gradient
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillStartColor, fillEndColor),
                startY = 0f,
                endY = h
            )
        )

        // Draw line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw baseline
        drawLine(
            color = Color.White.copy(alpha = 0.08f),
            start = Offset(0f, h / 2f),
            end = Offset(w, h / 2f),
            strokeWidth = 1.dp.toPx()
        )
    }
}
