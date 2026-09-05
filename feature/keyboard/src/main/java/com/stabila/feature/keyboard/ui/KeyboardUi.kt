package com.stabila.feature.keyboard.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class KeyboardState { NORMAL, MAGNIFIED }

/**
 * Root keyboard layout composable.
 *
 * New parameters compared to the original:
 * @param suggestions     The current list of word suggestions (up to 3). May be
 *                        empty when there is nothing to suggest.
 * @param suggestionsEnabled Whether the target field allows suggestions at all
 *                        (determined from [EditorInfo] by the service).
 * @param onSuggestionSelected Called when the user taps a suggestion chip.
 *
 * All other parameters and behaviour are unchanged.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun StabilaKeyboardLayout(
    tremorScore: Float,
    canUndo: Boolean = false,
    suggestions: List<String> = emptyList(),
    suggestionsEnabled: Boolean = false,
    onSuggestionSelected: (String) -> Unit = {},
    onLanguageChange: (Boolean) -> Unit = {},
    onKeyPress: (String) -> Unit,
    onDelete: () -> Unit,
    onDeleteWord: () -> Unit = {},
    onClearAll: () -> Unit = {},
    onUndo: () -> Unit = {},
    onEnter: () -> Unit,
) {
    val useMagnifier = tremorScore > 20f
    var currentState by remember { mutableStateOf(KeyboardState.NORMAL) }
    var magnifiedCenterKey by remember { mutableStateOf(' ') }

    // Hoist state so it's not lost when NormalKeyboard is removed from composition during magnification
    var isShift by remember { mutableStateOf(false) }
    var symbolState by remember { mutableIntStateOf(0) } // 0=letters, 1=symbols1, 2=symbols2
    var isArabic by remember { mutableStateOf(false) } // true=AR, false=EN

    // In MAGNIFIED mode the suggestion bar is not shown, so height stays 420dp.
    // In NORMAL mode, when suggestions are active we add BAR_HEIGHT to keep the key sizes
    // identical to the original 300dp layout. When there are no suggestions, height remains 300dp.
    val hasSuggestions = suggestionsEnabled && symbolState == 0 && suggestions.isNotEmpty()
    val targetHeight = if (currentState == KeyboardState.MAGNIFIED) 420.dp
    else if (hasSuggestions) 300.dp + BAR_HEIGHT
    else 300.dp

    val animatedHeight by animateDpAsState(targetValue = targetHeight, label = "KeyboardHeight")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AnimatedContent(targetState = currentState, label = "KeyboardState") { state ->
            when (state) {
                KeyboardState.NORMAL -> {
                    NormalKeyboard(
                        isShift = isShift,
                        symbolState = symbolState,
                        isArabic = isArabic,
                        canUndo = canUndo,
                        suggestions = suggestions,
                        suggestionsEnabled = suggestionsEnabled,
                        onSuggestionSelected = onSuggestionSelected,
                        onShiftChange = { isShift = it },
                        onSymbolStateChange = { symbolState = it },
                        onLanguageToggle = {
                            isArabic = !isArabic
                            symbolState = 0
                            onLanguageChange(isArabic)
                        },
                        onCharPress = { char ->
                            if (useMagnifier) {
                                magnifiedCenterKey = char
                                currentState = KeyboardState.MAGNIFIED
                            } else {
                                onKeyPress(char.toString())
                            }
                        },
                        onDelete = onDelete,
                        onDeleteWord = onDeleteWord,
                        onClearAll = onClearAll,
                        onUndo = onUndo,
                        onSpace = { onKeyPress(" ") },
                        onEnter = onEnter,
                    )
                }
                KeyboardState.MAGNIFIED -> {
                    MagnifiedKeyboard(
                        centerChar = magnifiedCenterKey,
                        symbolState = symbolState,
                        isArabic = isArabic,
                        onCharSelected = { char ->
                            onKeyPress(char.toString())
                            currentState = KeyboardState.NORMAL
                        },
                        onCancel = {
                            currentState = KeyboardState.NORMAL
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NormalKeyboard(
    isShift: Boolean,
    symbolState: Int,
    isArabic: Boolean,
    canUndo: Boolean,
    suggestions: List<String>,
    suggestionsEnabled: Boolean,
    onSuggestionSelected: (String) -> Unit,
    onShiftChange: (Boolean) -> Unit,
    onSymbolStateChange: (Int) -> Unit,
    onLanguageToggle: () -> Unit,
    onCharPress: (Char) -> Unit,
    onDelete: () -> Unit,
    onDeleteWord: () -> Unit,
    onClearAll: () -> Unit,
    onUndo: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
) {
    val letterRowsEN = listOf(
        "qwertyuiop".toList(),
        "asdfghjkl".toList(),
        "zxcvbnm".toList()
    )
    val letterRowsAR = listOf(
        "ضصثقفغعهخحجد".toList(),
        "شسيبلاتنمكط".toList(),
        "ذئءؤرىةوزظ".toList()
    )
    val symbolRows1 = listOf(
        "1234567890".toList(),
        "@#£_&-+()\u0022".toList(),
        "*\u0022':;!?~`|".toList()
    )
    val symbolRows2 = listOf(
        "~`|•√π÷×¶∆".toList(),
        "£¢€¥^°={}\u0022".toList(),
        "%©®™✓[]<> ".toList() // space placeholder at end
    )

    val currentRows = when {
        symbolState == 1 -> symbolRows1
        symbolState == 2 -> symbolRows2
        isArabic -> letterRowsAR
        else -> letterRowsEN
    }

    val hasSuggestions = suggestionsEnabled && symbolState == 0 && suggestions.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(3.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // ── 1. Top Action Navigation Bar (Clear, Clear Word, Undo) ────────────
        KeyboardTopBar(
            isArabic = isArabic,
            canUndo = canUndo,
            onDeleteWord = onDeleteWord,
            onClearAll = onClearAll,
            onUndo = onUndo,
        )

        // ── 2. Suggestion bar (under action bar; dynamic and only when suggestions exist) ─
        if (hasSuggestions) {
            SuggestionBar(
                suggestions = suggestions,
                onSuggestionSelected = onSuggestionSelected,
            )
        }

        // ── Row 1 ─────────────────────────────────────────────────────────────
        KeyboardRow(currentRows[0], isShift, onCharPress)

        // ── Row 2 ─────────────────────────────────────────────────────────────
        KeyboardRow(currentRows[1], isShift, onCharPress)

        // ── Row 3 (shift / letters row 3 / delete) ────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            if (!(isArabic && symbolState == 0)) {
                ActionKey(
                    modifier = Modifier
                        .weight(1.5f)
                        .padding(horizontal = 2.dp),
                    onClick = {
                        if (symbolState == 0) {
                            onShiftChange(!isShift)
                        } else if (symbolState == 1) {
                            onSymbolStateChange(2)
                        } else {
                            onSymbolStateChange(1)
                        }
                    },
                ) {
                    if (symbolState == 0) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Shift",
                            tint = if (isShift) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    } else if (symbolState == 1) {
                        Text("=\\<", color = MaterialTheme.colorScheme.onSurface)
                    } else {
                        Text("?123", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            currentRows[2].forEach { char ->
                if (char == ' ') {
                    Spacer(modifier = Modifier.weight(1f).padding(horizontal = 2.dp))
                } else {
                    KeyButton(
                        text = if (isShift && char.isLetter()) char.uppercase() else char.toString(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .fillMaxHeight(),
                        onClick = {
                            onCharPress(if (isShift && char.isLetter()) char.uppercaseChar() else char)
                            if (isShift) onShiftChange(false)
                        },
                    )
                }
            }

            ActionKey(
                modifier = Modifier
                    .weight(1.5f)
                    .padding(horizontal = 2.dp),
                onClick = onDelete,
            ) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete")
            }
        }

        // ── Row 4 (symbols toggle / lang / comma / space / period / enter) ────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.Center,
        ) {
            ActionKey(
                modifier = Modifier
                    .weight(1.5f)
                    .padding(horizontal = 2.dp),
                onClick = {
                    if (symbolState == 0) onSymbolStateChange(1) else onSymbolStateChange(0)
                },
            ) {
                Text(
                    if (symbolState != 0) (if (isArabic) "أ ب ت" else "ABC") else "?123",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            ActionKey(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
                onClick = onLanguageToggle,
            ) {
                Icon(Icons.Default.Language, contentDescription = "Language")
            }

            KeyButton(
                text = ",",
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .fillMaxHeight(),
                onClick = { onCharPress(',') },
            )

            ActionKey(
                modifier = Modifier
                    .weight(3f)
                    .padding(horizontal = 2.dp),
                onClick = onSpace,
            ) {
                Icon(Icons.Default.SpaceBar, contentDescription = "Space")
            }

            KeyButton(
                text = ".",
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .fillMaxHeight(),
                onClick = { onCharPress('.') },
            )

            ActionKey(
                modifier = Modifier
                    .weight(1.5f)
                    .padding(horizontal = 2.dp),
                backgroundColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = onEnter,
            ) {
                Icon(
                    Icons.Default.KeyboardReturn,
                    contentDescription = "Enter",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.KeyboardRow(
    row: List<Char>,
    isShift: Boolean,
    onCharPress: (Char) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        if (row.size == 9) {
            Spacer(modifier = Modifier.weight(0.5f))
        }
        row.forEach { char ->
            KeyButton(
                text = if (isShift && char.isLetter()) char.uppercase() else char.toString(),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .fillMaxHeight(),
                onClick = { onCharPress(if (isShift && char.isLetter()) char.uppercaseChar() else char) },
            )
        }
        if (row.size == 9) {
            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}

@Composable
private fun MagnifiedKeyboard(
    centerChar: Char,
    symbolState: Int,
    isArabic: Boolean,
    onCharSelected: (Char) -> Unit,
    onCancel: () -> Unit,
) {
    val neighbors = getNeighbors(centerChar, symbolState, isArabic)
    val chars = neighbors.flatten().filterNotNull().filter { it != ' ' }

    val numRows = when (chars.size) {
        in 1..3 -> 1
        4 -> 2
        in 5..6 -> 2
        else -> 3
    }

    val itemsPerRow = kotlin.math.ceil(chars.size.toFloat() / numRows).toInt()
    val chunked = chars.chunked(if (itemsPerRow > 0) itemsPerRow else 1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val currentOnCancel by rememberUpdatedState(onCancel)
        // Top instruction / cancel bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown().consume()
                        currentOnCancel()
                    }
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.ArrowDownward, contentDescription = androidx.compose.ui.res.stringResource(com.stabila.core.R.string.keyboard_tap_to_cancel))
            Spacer(modifier = Modifier.width(8.dp))
            Text(androidx.compose.ui.res.stringResource(com.stabila.core.R.string.keyboard_cancel_tap), style = MaterialTheme.typography.bodyMedium)
        }

        // Dynamic Grid
        chunked.forEach { rowChars ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                rowChars.forEach { char ->
                    KeyButton(
                        text = char.toString(),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        textStyle = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 48.sp,
                        ),
                        onClick = { onCharSelected(char) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.KeyboardTopBar(
    isArabic: Boolean,
    canUndo: Boolean,
    onDeleteWord: () -> Unit,
    onClearAll: () -> Unit,
    onUndo: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(bottom = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Delete Word Button
        ActionKey(
            modifier = Modifier.weight(2.2f),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            onClick = onDeleteWord,
        ) {
            Text(
                text = if (isArabic) "حذف كلمة" else "Delete Word",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Clear All Button
        ActionKey(
            modifier = Modifier.weight(2.2f),
            backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            onClick = onClearAll,
        ) {
            Text(
                text = if (isArabic) "مسح الكل" else "Clear All",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                ),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }

        // Undo Icon Button
        ActionKey(
            modifier = Modifier.weight(1.2f),
            backgroundColor = if (canUndo) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
            contentColor = if (canUndo) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            onClick = { if (canUndo) onUndo() },
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = if (isArabic) "تراجع" else "Undo",
                tint = if (canUndo) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
private fun ActionKey(
    modifier: Modifier = Modifier,
    backgroundColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val currentOnClick by rememberUpdatedState(onClick)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    currentOnClick()
                }
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun KeyButton(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge,
    onClick: () -> Unit,
) {
    val currentOnClick by rememberUpdatedState(onClick)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    currentOnClick()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun getNeighbors(center: Char, symbolState: Int, isArabic: Boolean): List<List<Char?>> {
    val layout = listOf(
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm",
        ",     .",
        "1234567890",
        "@#£_&-+()\u0022",
        "*\u0022':;!?~`|",
        ",        .",
        "~`|•√π÷×¶∆",
        "£¢€¥^°={}\u0022",
        "%©®™✓[]<> ",
        ",        .",
        "ضصثقفغعهخحجد",
        "شسيبلاتنمكط",
        "ذئءؤرىةوزظ",
        ",        .",
    )

    val lowerCenter = center.lowercaseChar()
    val isUpper = center.isUpperCase()

    // Restrict search to the active symbol group
    val startRow = when {
        symbolState == 1 -> 4
        symbolState == 2 -> 8
        isArabic -> 12
        else -> 0
    }
    val endRow = startRow + 3

    var r = -1
    var c = -1
    for (i in startRow..endRow) {
        val idx = layout[i].indexOf(lowerCenter)
        if (idx != -1) {
            r = i
            c = idx
            break
        }
    }

    if (r == -1) {
        val grid = MutableList(3) { MutableList<Char?>(3) { null } }
        grid[1][1] = center
        return grid
    }

    val grid = mutableListOf<List<Char?>>()
    for (i in r - 1..r + 1) {
        val rowList = mutableListOf<Char?>()
        for (j in c - 1..c + 1) {
            if (i in layout.indices && j in layout[i].indices) {
                val currentGroup = r / 4
                val neighborGroup = i / 4
                if (currentGroup != neighborGroup) {
                    rowList.add(null)
                } else {
                    val char = layout[i][j]
                    rowList.add(if (isUpper && char.isLetter()) char.uppercaseChar() else char)
                }
            } else {
                rowList.add(null)
            }
        }
        grid.add(rowList)
    }
    return grid
}
