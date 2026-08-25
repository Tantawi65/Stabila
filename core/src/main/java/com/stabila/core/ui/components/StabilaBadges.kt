package com.stabila.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stabila.core.ui.Amber500
import com.stabila.core.ui.Amber900
import com.stabila.core.ui.Emerald500
import com.stabila.core.ui.Emerald900
import com.stabila.core.ui.Indigo200
import com.stabila.core.ui.Indigo900
import com.stabila.core.ui.Red500
import com.stabila.core.ui.Red900

enum class TremorLevel { STABLE, ELEVATED, SEVERE }

/**
 * A small rounded badge that communicates the user's tremor level.
 * Displays a color-coded label: Stable (green), Elevated (amber), Severe (red).
 */
@Composable
fun TremorLevelBadge(level: TremorLevel, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (level) {
        TremorLevel.STABLE   -> Triple(Emerald900, Emerald500, "Stable")
        TremorLevel.ELEVATED -> Triple(Amber900, Amber500, "Elevated")
        TremorLevel.SEVERE   -> Triple(Red900, Red500, "Severe")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

/**
 * A generic info chip in indigo — used for tags like "pre-dose", "post-dose".
 */
@Composable
fun StabilaChip(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(Indigo900)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Indigo200
        )
    }
}
