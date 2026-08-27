package com.stabila.app.accessibility

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.hypot
import kotlin.math.max

/**
 * Manages the floating auto-scroll button and invisible touch shield overlays.
 */
class OverlayManager(private val service: AccessibilityService) {

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // Invisible Emergency Brake Shield Overlay
    private var shieldView: FrameLayout? = null
    private var isShieldVisible = false

    // Floating Draggable Auto-Scroll Control Button
    private var pillView: FrameLayout? = null
    private var iconView: ImageView? = null
    private var isPillVisible = false
    private var currentScrollingState: Boolean? = null
    private var snapAnimator: ValueAnimator? = null

    // Callbacks for button actions
    private var onToggleScrollListener: (() -> Unit)? = null
    private var onPositionSavedListener: ((Float, Float) -> Unit)? = null

    /**
     * Shows the full-screen transparent shield overlay.
     * Any tap on this shield will trigger [onTouchIntercepted], consuming the touch event
     * to immediately stop auto-scroll without triggering underlying app UI controls.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun showInvisibleShield(onTouchIntercepted: () -> Unit) {
        if (isShieldVisible && shieldView != null) return

        val createShield = FrameLayout(service).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    // Emergency Brake: Intercept tap, consume event, trigger stop callback
                    onTouchIntercepted()
                    return@setOnTouchListener true
                }
                true
            }
        }

        shieldView = createShield

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        try {
            windowManager.addView(shieldView, params)
            isShieldVisible = true

            // Re-add pillView if visible so it stays on top of the shield overlay
            pillView?.let { pill ->
                val pillParams = pill.layoutParams as? WindowManager.LayoutParams
                if (pillParams != null) {
                    try {
                        windowManager.removeView(pill)
                        windowManager.addView(pill, pillParams)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideInvisibleShield() {
        if (!isShieldVisible) return
        shieldView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        shieldView = null
        isShieldVisible = false
    }

    /**
     * Temporarily enables or disables touch interception on the shield.
     * Used during AccessibilityService gesture dispatches so synthetic swipe strokes pass through.
     */
    fun setShieldTouchable(touchable: Boolean) {
        val shield = shieldView ?: return
        val params = shield.layoutParams as? WindowManager.LayoutParams ?: return
        val baseFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        val targetFlags = if (touchable) baseFlags else (baseFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        if (params.flags != targetFlags) {
            params.flags = targetFlags
            try {
                windowManager.updateViewLayout(shield, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Shows or updates the floating draggable control button.
     * @param tremorScore Tremor severity (0 to 100) to adjust touch target size and drag slop.
     * @param savedX Stored X coordinate (-1f for default).
     * @param savedY Stored Y coordinate (-1f for default).
     * @param isScrolling Current state: false = Paused (▶ icon), true = Scrolling (Ⅱ icon).
     * @param onToggleScroll Callback when button is tapped.
     * @param onPositionSaved Callback when button is moved and released.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun showFloatingButton(
        tremorScore: Float,
        savedX: Float,
        savedY: Float,
        isScrolling: Boolean,
        onToggleScroll: () -> Unit,
        onPositionSaved: (Float, Float) -> Unit
    ) {
        this.onToggleScrollListener = onToggleScroll
        this.onPositionSavedListener = onPositionSaved

        val density = service.resources.displayMetrics.density
        val displayMetrics = service.resources.displayMetrics
        val scaleMultiplier = 1f + (tremorScore / 100f).coerceIn(0f, 0.5f)
        val size = (56 * density * scaleMultiplier).toInt()

        // If pill already exists, just update state seamlessly
        if (isPillVisible && pillView != null) {
            updateButtonVisualState(isScrolling, density)
            return
        }

        val backgroundDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#E60F172A")) // Modern dark slate glassmorphism
            val strokeColor = if (isScrolling) Color.parseColor("#0284C7") else Color.parseColor("#40FFFFFF")
            setStroke((2.5f * density).toInt(), strokeColor)
        }

        val icon = ImageView(service).apply {
            val resId = if (isScrolling) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            try {
                val drawable = service.getDrawable(resId)?.mutate()
                drawable?.setTint(Color.WHITE)
                setImageDrawable(drawable)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            val pad = (14 * density * scaleMultiplier).toInt()
            setPadding(pad, pad, pad, pad)
        }
        this.iconView = icon

        val pillContainer = FrameLayout(service).apply {
            setBackground(backgroundDrawable)
            addView(icon, FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER
            })
            elevation = 12f * density
        }

        // Calculate initial layout params
        val margin = (12 * density).toInt()
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val initialX = if (savedX >= 0f) {
            savedX.toInt().coerceIn(margin, maxOf(margin, screenWidth - size - margin))
        } else {
            screenWidth - size - (16 * density).toInt()
        }

        val initialY = if (savedY >= 0f) {
            savedY.toInt().coerceIn(margin, maxOf(margin, screenHeight - size - margin))
        } else {
            (screenHeight / 2) - (size / 2)
        }

        val params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }

        // Setup Messenger Floating Pop Draggable Touch Listener with Edge-Snapping
        val dragSlop = 8f * density

        pillContainer.setOnTouchListener(object : View.OnTouchListener {
            private var downX = 0f
            private var downY = 0f
            private var startParamX = 0
            private var startParamY = 0
            private var isDragging = false

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event == null) return false
                val currentParams = pillContainer.layoutParams as? WindowManager.LayoutParams ?: return false

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        snapAnimator?.cancel()
                        downX = event.rawX
                        downY = event.rawY
                        startParamX = currentParams.x
                        startParamY = currentParams.y
                        isDragging = false

                        // Grab feedback (Messenger floating bubble elevation & scale expansion)
                        pillContainer.animate()
                            .scaleX(1.12f)
                            .scaleY(1.12f)
                            .setDuration(100)
                            .start()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downX
                        val dy = event.rawY - downY

                        if (!isDragging && hypot(dx.toDouble(), dy.toDouble()) > dragSlop) {
                            isDragging = true
                        }

                        if (isDragging) {
                            val curScreenWidth = service.resources.displayMetrics.widthPixels
                            val curScreenHeight = service.resources.displayMetrics.heightPixels
                            val minX = margin
                            val maxX = maxOf(margin, curScreenWidth - size - margin)
                            val minY = margin
                            val maxY = maxOf(margin, curScreenHeight - size - margin)

                            val newX = (startParamX + dx).toInt().coerceIn(minX, maxX)
                            val newY = (startParamY + dy).toInt().coerceIn(minY, maxY)
                            currentParams.x = newX
                            currentParams.y = newY
                            try {
                                windowManager.updateViewLayout(pillContainer, currentParams)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // Release scale feedback
                        pillContainer.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start()

                        if (isDragging) {
                            val curScreenWidth = service.resources.displayMetrics.widthPixels
                            val curScreenHeight = service.resources.displayMetrics.heightPixels
                            val minX = margin
                            val maxX = maxOf(margin, curScreenWidth - size - margin)

                            // Messenger Chat Head edge-snapping (snap to nearest left/right edge smoothly)
                            val pillCenterX = currentParams.x + size / 2
                            val targetX = if (pillCenterX < curScreenWidth / 2) minX else maxX
                            val targetY = currentParams.y.coerceIn(margin, maxOf(margin, curScreenHeight - size - margin))

                            snapAnimator?.cancel()
                            snapAnimator = ValueAnimator.ofInt(currentParams.x, targetX).apply {
                                duration = 200L
                                interpolator = DecelerateInterpolator()
                                addUpdateListener { anim ->
                                    val animatedX = anim.animatedValue as Int
                                    currentParams.x = animatedX
                                    currentParams.y = targetY
                                    try {
                                        windowManager.updateViewLayout(pillContainer, currentParams)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                addListener(object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: Animator) {
                                        onPositionSavedListener?.invoke(targetX.toFloat(), targetY.toFloat())
                                    }
                                })
                                start()
                            }
                        } else {
                            onToggleScrollListener?.invoke()
                        }
                        isDragging = false
                        return true
                    }
                }
                return false
            }
        })

        pillView = pillContainer
        currentScrollingState = isScrolling

        try {
            windowManager.addView(pillView, params)
            isPillVisible = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Updates the visual indicator (icon & background tint) with a smooth transition.
     */
    private fun updateButtonVisualState(isScrolling: Boolean, density: Float) {
        if (currentScrollingState == isScrolling) return
        currentScrollingState = isScrolling

        val targetIconRes = if (isScrolling) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val strokeColor = if (isScrolling) Color.parseColor("#0284C7") else Color.parseColor("#40FFFFFF")

        (pillView?.background as? GradientDrawable)?.apply {
            setStroke((2.5f * density).toInt(), strokeColor)
        }

        iconView?.let { img ->
            img.animate()
                .alpha(0f)
                .scaleX(0.7f)
                .scaleY(0.7f)
                .setDuration(100)
                .withEndAction {
                    try {
                        val newDrawable = service.getDrawable(targetIconRes)?.mutate()
                        newDrawable?.setTint(Color.WHITE)
                        img.setImageDrawable(newDrawable)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    img.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
        }
    }

    // Backward compatibility helper
    fun showResumePill(tremorScore: Float, onResumeClicked: () -> Unit) {
        showFloatingButton(
            tremorScore = tremorScore,
            savedX = -1f,
            savedY = -1f,
            isScrolling = false,
            onToggleScroll = onResumeClicked,
            onPositionSaved = { _, _ -> }
        )
    }

    fun hideResumePill() {
        hideFloatingButton()
    }

    fun hideFloatingButton() {
        if (!isPillVisible) return
        snapAnimator?.cancel()
        snapAnimator = null
        pillView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        pillView = null
        iconView = null
        isPillVisible = false
        currentScrollingState = null
    }

    fun hideAll() {
        hideInvisibleShield()
        hideFloatingButton()
    }
}
