package com.stabila.core.accessibility

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Shared in-memory state for tracking whether Auto-Scroll is currently active.
 * Used by TouchFilterService to avoid intercepting touches or synthetic gestures
 * while StabilaAccessibilityService is actively scrolling.
 */
object AutoScrollState {
    private val _isScrollingFlow = MutableStateFlow(false)
    val isScrollingFlow: StateFlow<Boolean> = _isScrollingFlow

    @Volatile
    var isScrolling: Boolean = false
        set(value) {
            field = value
            _isScrollingFlow.value = value
        }
}
