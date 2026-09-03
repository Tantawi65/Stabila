package com.stabila.feature.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A compact row of up to [MAX_CHIPS] word-suggestion chips, displayed above the
 * main keyboard rows.
 *
 * Design goals aligned with the tremor-accessibility focus of Stabila:
 *  - Fixed [BAR_HEIGHT] so the keyboard layout never shifts when suggestions
 *    appear or disappear.
 *  - Large tap targets (full chip height = [BAR_HEIGHT] minus padding) to
 *    reduce mis-tap risk for users with hand tremors.
 *  - No entry/exit animations — instant content changes avoid disorienting
 *    motion for users sensitive to movement.
 *  - Three equal-width chip slots always rendered; empty slots are invisible
 *    (transparent background, no text) so the row keeps its size.
 *
 * @param suggestions Up to 3 suggestion strings. Pass an empty list to show
 *                    the bar with no visible chips (reserved space only).
 * @param onSuggestionSelected Called when the user taps a chip.
 * @param modifier Optional outer modifier (width / padding).
 */
@Composable
fun SuggestionBar(
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BAR_HEIGHT)
            .padding(horizontal = 3.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(MAX_CHIPS) { index ->
            val word = suggestions.getOrNull(index)
            SuggestionChip(
                word = word,
                onSuggestionSelected = onSuggestionSelected,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 3.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Total height reserved for the suggestion bar (including its vertical padding). */
internal val BAR_HEIGHT = 48.dp

private const val MAX_CHIPS = 3

/**
 * A single tappable suggestion chip.
 *
 * When [word] is null the chip renders as transparent space so the slot is
 * still occupied and the layout remains stable.
 */
@Composable
private fun SuggestionChip(
    word: String?,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentWord by rememberUpdatedState(word)
    val currentOnSelected by rememberUpdatedState(onSuggestionSelected)

    val isActive = word != null

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                color = if (isActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
            )
            // Only attach gesture detector when the chip has content — avoids
            // consuming touch events for empty/invisible slots.
            .then(
                if (isActive) {
                    Modifier.pointerInput(word) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            currentWord?.let { currentOnSelected(it) }
                        }
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isActive) {
            Text(
                text = word!!,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
