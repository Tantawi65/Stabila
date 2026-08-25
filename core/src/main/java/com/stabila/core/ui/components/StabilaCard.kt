package com.stabila.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Stabila's standard content card.
 *
 * A zinc-900 rounded container with consistent padding.
 * Used throughout the app for grouping related content.
 *
 * @param modifier Optional modifier
 * @param title Optional title rendered above the content
 * @param innerPadding Padding inside the card (default 20dp)
 * @param content Card body content
 */
@Composable
fun StabilaCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    innerPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFFFAFAFA),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            content()
        }
    }
}
