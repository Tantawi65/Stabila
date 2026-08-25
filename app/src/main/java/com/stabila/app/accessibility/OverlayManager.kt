package com.stabila.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Manages the State A (Shield) and State B (Pill) overlays using SYSTEM_ALERT_WINDOW.
 */
class OverlayManager(private val service: AccessibilityService) {

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // State A: Invisible Shield
    private var leftShieldView: FrameLayout? = null
    private var rightShieldView: FrameLayout? = null
    private var isShieldVisible = false

    // State B: Resume Pill
    private var pillView: FrameLayout? = null
    private var isPillVisible = false

    /**
     * Shows the transparent shield (State A).
     * Any tap on this shield will trigger [onTouchIntercepted] and immediately remove the shield.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun showInvisibleShield(onTouchIntercepted: () -> Unit) {
        if (isShieldVisible) return

        val displayMetrics = service.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val shieldWidth = (screenWidth / 2) - 4 // Leave an 8px gap in the absolute center

        val createShield = { 
            FrameLayout(service).apply {
                // Completely transparent
                setBackgroundColor(Color.TRANSPARENT)
                
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        // Emergency Brake: Intercept the touch, consume it, and trigger the callback
                        onTouchIntercepted()
                        return@setOnTouchListener true
                    }
                    false
                }
            }
        }

        leftShieldView = createShield()
        rightShieldView = createShield()

        val leftParams = WindowManager.LayoutParams(
            shieldWidth,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.FILL_VERTICAL
        }

        val rightParams = WindowManager.LayoutParams(
            shieldWidth,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.FILL_VERTICAL
        }

        try {
            windowManager.addView(leftShieldView, leftParams)
            windowManager.addView(rightShieldView, rightParams)
            isShieldVisible = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideInvisibleShield() {
        if (!isShieldVisible) return
        leftShieldView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { e.printStackTrace() }
        }
        rightShieldView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { e.printStackTrace() }
        }
        leftShieldView = null
        rightShieldView = null
        isShieldVisible = false
    }

    /**
     * Shows the Resume Pill (State B).
     * Tapping it triggers [onResumeClicked].
     */
    fun showResumePill(tremorScore: Float, onResumeClicked: () -> Unit) {
        if (isPillVisible) return

        pillView = FrameLayout(service)

        // Dynamic scaling based on Tremor Score (0 to 100)
        // Cap the maximum scaling so the button doesn't become grotesquely large on high tremor days.
        val scaleMultiplier = 1f + (tremorScore / 100f).coerceIn(0f, 0.8f) // max 1.8x size
        val density = service.resources.displayMetrics.density

        // Create a circular modern sleek FAB background
        val background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#D90EA5E9")) // Translucent Stabila Blue
            setStroke((2 * density).toInt(), Color.parseColor("#4DFFFFFF")) // Soft semi-transparent white border
        }

        val iconView = android.widget.ImageView(service).apply {
            try {
                val icon = service.getDrawable(android.R.drawable.ic_media_play)
                if (icon != null) {
                    icon.setTint(Color.WHITE)
                    setImageDrawable(icon)
                }
            } catch (e: Exception) { e.printStackTrace() }
            
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            val pad = (16 * density * scaleMultiplier).toInt()
            setPadding(pad, pad, pad, pad)
        }

        pillView?.apply {
            setBackground(background)
            val size = (64 * density * scaleMultiplier).toInt()
            addView(iconView, FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER
            })
            
            setOnClickListener {
                onResumeClicked()
            }
            
            elevation = 16f * density
        }

        // Place it on the middle-right edge of the screen
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            x = (16 * density).toInt() // Margin from right edge
        }

        try {
            windowManager.addView(pillView, params)
            isPillVisible = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideResumePill() {
        if (!isPillVisible) return
        pillView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        pillView = null
        isPillVisible = false
    }

    fun hideAll() {
        hideInvisibleShield()
        hideResumePill()
    }
}
