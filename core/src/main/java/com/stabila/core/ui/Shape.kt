package com.stabila.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape tokens for Stabila.
 * All corner radii are generous — rounded shapes feel friendlier
 * and are easier to tap for tremor users.
 */
val StabilaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small       = RoundedCornerShape(10.dp),
    medium      = RoundedCornerShape(16.dp),   // Cards
    large       = RoundedCornerShape(24.dp),   // Bottom sheets
    extraLarge  = RoundedCornerShape(32.dp)    // Dialogs, FABs
)
