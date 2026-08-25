package com.stabila.feature.history.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.stabila.core.domain.TremorReading
import com.stabila.core.ui.LocalAdaptiveParams

@Composable
fun HistoryLineChart(
    readings: List<TremorReading>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    val adaptive = LocalAdaptiveParams.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    
    Canvas(modifier = modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 16.dp)) {
        if (readings.size < 2) return@Canvas

        val w = size.width
        val h = size.height
        
        // Internal padding so points don't clip at the very top (score=max) or bottom (score=0)
        val padY = (8 * adaptive.fontScale).dp.toPx()
        val usableH = h - (padY * 2)

        // Sort ascending by time for plotting left-to-right
        val sorted = readings.sortedBy { it.timestampEpochMs }
        val maxScore = sorted.maxOfOrNull { it.score }?.coerceAtLeast(10f) ?: 100f
        val stepX = w / (sorted.size - 1)

        val path = Path()
        
        // Start at first point
        val firstY = padY + usableH - ((sorted[0].score / maxScore) * usableH)
        path.moveTo(0f, firstY)

        for (i in 1 until sorted.size) {
            val currX = i * stepX
            val currY = padY + usableH - ((sorted[i].score / maxScore) * usableH)
            
            // Draw a straight line to the next point
            path.lineTo(currX, currY)
        }

        // Draw the path
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = (3 * adaptive.fontScale).dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw points
        for (i in sorted.indices) {
            val currX = i * stepX
            val currY = padY + usableH - ((sorted[i].score / maxScore) * usableH)
            drawCircle(
                color = surfaceColor,
                radius = (5 * adaptive.fontScale).dp.toPx(),
                center = Offset(currX, currY)
            )
            drawCircle(
                color = lineColor,
                radius = (3 * adaptive.fontScale).dp.toPx(),
                center = Offset(currX, currY)
            )
        }
    }
}
