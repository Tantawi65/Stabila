package com.stabila.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * A custom modifier that stabilizes touch input for users with tremors.
 * It waits for a short window (e.g. 300ms), collects all jittery touch points,
 * calculates the common center (Centroid), and triggers the click if the 
 * centroid is within the bounds.
 * 
 * It properly delegates to standard compose clickable when disabled,
 * and correctly aborts if a parent scroll container intercepts the gesture.
 */
fun Modifier.stabilizedClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    if (!enabled) {
        // When disabled, use standard Compose clickable which perfectly handles scroll cancellation
        return@composed this.clickable { onClick() }
    }

    var clickCenter by remember { mutableStateOf<Offset?>(null) }
    var showSnap by remember { mutableStateOf(false) }
    
    // Animation for the "snap" ring
    val ringRadius = remember { Animatable(0f) }
    val ringAlpha = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    this
        .pointerInput(Unit) {
            awaitEachGesture {
                val initialDown = awaitFirstDown(requireUnconsumed = false)
                val points = mutableListOf(initialDown.position)
                
                // Do NOT consume the down event aggressively! 
                // If we consume it, parent scroll containers can't detect dragging.
                
                var isReleased = false
                var isCanceled = false
                
                // Collect points for up to 350ms to calculate the point cloud
                try {
                    withTimeout(350) {
                        while (true) {
                            val event = awaitPointerEvent()
                            
                            // If a parent (like a ScrollView) consumed the event, it means the user is scrolling.
                            if (event.changes.any { it.isConsumed }) {
                                isCanceled = true
                                break
                            }

                            event.changes.forEach { change ->
                                if (change.pressed) {
                                    points.add(change.position)
                                    // Intentionally not consuming position change so scroll container can monitor touch slop
                                } else {
                                    isReleased = true
                                }
                            }
                            if (isReleased) break
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    // Time is up, we collected the point cloud!
                }

                if (isCanceled) {
                    return@awaitEachGesture
                }

                // Wait for the user to lift their finger if they haven't yet
                if (!isReleased) {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.isConsumed }) {
                            isCanceled = true
                            break
                        }
                        if (event.changes.any { !it.pressed }) {
                            break
                        }
                    }
                }

                if (isCanceled) {
                    return@awaitEachGesture
                }

                // Calculate Centroid (Common Center Point)
                val avgX = points.map { it.x }.average().toFloat()
                val avgY = points.map { it.y }.average().toFloat()
                val centroid = Offset(avgX, avgY)

                // Trigger Visual Snap Feedback
                clickCenter = centroid
                showSnap = true
                
                scope.launch {
                    ringRadius.snapTo(80f) // Start large
                    ringAlpha.snapTo(1f)
                    
                    // "Snap" to the center
                    ringRadius.animateTo(10f, animationSpec = tween(150))
                    // Fade out
                    ringAlpha.animateTo(0f, animationSpec = tween(200))
                    showSnap = false
                }

                // Execute the actual intention
                onClick()
            }
        }
        .drawWithContent {
            drawContent()
            if (showSnap && clickCenter != null) {
                drawCircle(
                    color = Color(0xFF00E676).copy(alpha = ringAlpha.value), // Emerald/Success color
                    radius = ringRadius.value,
                    center = clickCenter!!,
                    style = Stroke(width = 8f)
                )
            }
        }
}
