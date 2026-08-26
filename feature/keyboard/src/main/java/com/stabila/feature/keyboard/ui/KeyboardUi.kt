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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun StabilaKeyboardLayout(
    tremorScore: Float,
    onKeyPress: (String) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit
) {
    val useMagnifier = tremorScore > 20f
    var currentState by remember { mutableStateOf(KeyboardState.NORMAL) }
    var magnifiedCenterKey by remember { mutableStateOf(' ') }

    // Hoist state so it's not lost when NormalKeyboard is removed from composition during magnification
    var isShift by remember { mutableStateOf(false) }
    var symbolState by remember { mutableIntStateOf(0) } // 0=letters, 1=symbols1, 2=symbols2
    var isArabic by remember { mutableStateOf(false) } // true=AR, false=EN

    val targetHeight = if (currentState == KeyboardState.MAGNIFIED) 450.dp else 300.dp
    val animatedHeight by animateDpAsState(targetValue = targetHeight, label = "KeyboardHeight")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AnimatedContent(targetState = currentState, label = "KeyboardState") { state ->
            when (state) {
                KeyboardState.NORMAL -> {
                    NormalKeyboard(
                        isShift = isShift,
                        symbolState = symbolState,
                        isArabic = isArabic,
                        onShiftChange = { isShift = it },
                        onSymbolStateChange = { symbolState = it },
                        onLanguageToggle = { isArabic = !isArabic; symbolState = 0 },
                        onCharPress = { char ->
                            if (useMagnifier) {
                                magnifiedCenterKey = char
                                currentState = KeyboardState.MAGNIFIED
                            } else {
                                onKeyPress(char.toString())
                            }
                        },
                        onDelete = onDelete,
                        onSpace = { onKeyPress(" ") },
                        onEnter = onEnter
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
                        }
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
    onShiftChange: (Boolean) -> Unit,
    onSymbolStateChange: (Int) -> Unit,
    onLanguageToggle: () -> Unit,
    onCharPress: (Char) -> Unit,
    onDelete: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit
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
        "@#£_&-+()\"".toList(),
        "*\"':;!?~`|".toList()
    )
    val symbolRows2 = listOf(
        "~`|•√π÷×¶∆".toList(),
        "£¢€¥^°={}\\".toList(),
        "%©®™✓[]<> ".toList() // Add space at the end to make it 10 chars
    )

    val currentRows = when {
        symbolState == 1 -> symbolRows1
        symbolState == 2 -> symbolRows2
        isArabic -> letterRowsAR
        else -> letterRowsEN
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Row 1
        KeyboardRow(currentRows[0], isShift, onCharPress)
        
        // Row 2
        KeyboardRow(currentRows[1], isShift, onCharPress)
        
        // Row 3
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            if (!(isArabic && symbolState == 0)) {
                ActionKey(
                    modifier = Modifier.weight(1.5f).padding(horizontal = 2.dp),
                    onClick = { 
                        if (symbolState == 0) {
                            onShiftChange(!isShift)
                        } else if (symbolState == 1) {
                            onSymbolStateChange(2)
                        } else {
                            onSymbolStateChange(1)
                        }
                    }
                ) {
                    if (symbolState == 0) {
                        Icon(
                            Icons.Default.KeyboardArrowUp, 
                            contentDescription = "Shift",
                            tint = if (isShift) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp).fillMaxHeight(),
                        onClick = { 
                            onCharPress(if (isShift && char.isLetter()) char.uppercaseChar() else char) 
                            if (isShift) onShiftChange(false)
                        }
                    )
                }
            }
            
            ActionKey(
                modifier = Modifier.weight(1.5f).padding(horizontal = 2.dp),
                onClick = onDelete
            ) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete")
            }
        }
        
        // Row 4
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.Center
        ) {
            ActionKey(
                modifier = Modifier.weight(1.5f).padding(horizontal = 2.dp),
                onClick = { 
                    if (symbolState == 0) onSymbolStateChange(1) else onSymbolStateChange(0) 
                }
            ) {
                Text(if (symbolState != 0) (if (isArabic) "أ ب ت" else "ABC") else "?123", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            
            ActionKey(
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                onClick = onLanguageToggle
            ) {
                Icon(Icons.Default.Language, contentDescription = "Language")
            }
            
            KeyButton(
                text = ",",
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp).fillMaxHeight(),
                onClick = { onCharPress(',') }
            )
            
            ActionKey(
                modifier = Modifier.weight(3f).padding(horizontal = 2.dp),
                onClick = onSpace
            ) {
                Icon(Icons.Default.SpaceBar, contentDescription = "Space")
            }
            
            KeyButton(
                text = ".",
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp).fillMaxHeight(),
                onClick = { onCharPress('.') }
            )
            
            ActionKey(
                modifier = Modifier.weight(1.5f).padding(horizontal = 2.dp),
                backgroundColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = onEnter
            ) {
                Icon(Icons.Default.KeyboardReturn, contentDescription = "Enter", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun ColumnScope.KeyboardRow(
    row: List<Char>,
    isShift: Boolean,
    onCharPress: (Char) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        if (row.size == 9) { 
            Spacer(modifier = Modifier.weight(0.5f))
        }
        row.forEach { char ->
            KeyButton(
                text = if (isShift && char.isLetter()) char.uppercase() else char.toString(),
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp).fillMaxHeight(),
                onClick = { onCharPress(if (isShift && char.isLetter()) char.uppercaseChar() else char) }
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
    onCancel: () -> Unit
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
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowDownward, contentDescription = "Tap to Cancel")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cancel (Tap here)", style = MaterialTheme.typography.bodyMedium)
        }

        // Dynamic Grid
        chunked.forEach { rowChars ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowChars.forEach { char ->
                    KeyButton(
                        text = char.toString(),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        textStyle = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, fontSize = 48.sp),
                        onClick = { onCharSelected(char) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionKey(
    modifier: Modifier = Modifier,
    backgroundColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
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
        content = content
    )
}

@Composable
private fun KeyButton(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge,
    onClick: () -> Unit
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
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurface
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
        "@#£_&-+()\"",
        "*\"':;!?~`|",
        ",        .",
        "~`|•√π÷×¶∆",
        "£¢€¥^°={}\\",
        "%©®™✓[]<> ",
        ",        .",
        "ضصثقفغعهخحجد",
        "شسيبلاتنمكط",
        "ذئءؤرىةوزظ",
        ",        ."
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
                // Prevent crossing between letters, symbols1, symbols2
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
