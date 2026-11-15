package com.autoscroll.prov2

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class AutoScrollAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private val handler = Handler(Looper.getMainLooper())

    private var isRunning = false
    private var autoClickEnabled = true
    private var speedLevel = 3 // 1..10
    private var scrollDir = 1  // 1: cuộn xuống, -1: cuộn lên
    private var swipeCountInDir = 0

    private val keywords = listOf(
        "ấn vào màn hình",
        "chạm vào màn hình",
        "nhấn vào màn hình",
        "bấm vào màn hình",
        "nhấn để tiếp tục",
        "chạm để tiếp tục",
        "bấm để tiếp tục",
        "ấn để tiếp tục",
        "click to continue",
        "tap to continue"
    )

    private val scrollRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            performSmoothScrollStep()
            val delayMs = (2200L - (speedLevel * 160L)).coerceAtLeast(450L)
            handler.postDelayed(this, delayMs)
        }
    }

    private val clickScanRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            if (autoClickEnabled) {
                scanAndClickTargetText()
            }
            handler.postDelayed(this, 450L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        createFloatingPanel()
    }

    private fun createFloatingPanel() {
        if (floatingView != null) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 300
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 16, 24, 16)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E6181825"))
                cornerRadius = 50f
                setStroke(3, Color.parseColor("#89b4fa"))
            }
        }

        val dragHandle = TextView(this).apply {
            text = "✥ "
            setTextColor(Color.parseColor("#89b4fa"))
            textSize = 15f
            setPadding(4, 0, 12, 0)
        }

        val toggleBtn = Button(this).apply {
            text = "▶ Chạy"
            textSize = 12f
            setTextColor(Color.parseColor("#11111b"))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#a6e3a1"))
                cornerRadius = 30f
            }
            setPadding(22, 8, 22, 8)
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
        }

        val speedBtn = Button(this).apply {
            text = "⚡3x"
            textSize = 12f
            setTextColor(Color.parseColor("#cdd6f4"))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#313244"))
                cornerRadius = 30f
            }
            setPadding(18, 8, 18, 8)
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = 12 }
        }

        val targetBtn = Button(this).apply {
            text = "🎯 Bật"
            textSize = 12f
            setTextColor(Color.parseColor("#f9e2af"))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#313244"))
                cornerRadius = 30f
            }
            setPadding(18, 8, 18, 8)
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = 12 }
        }

        val dirBtn = Button(this).apply {
            text = "⇅"
            textSize = 13f
            setTextColor(Color.parseColor("#89b4fa"))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#313244"))
                cornerRadius = 30f
            }
            setPadding(16, 8, 16, 8)
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = 12 }
        }

        toggleBtn.setOnClickListener {
            isRunning = !isRunning
            if (isRunning) {
                toggleBtn.text = "⏹ Dừng"
                toggleBtn.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#f38ba8"))
                    cornerRadius = 30f
                }
                handler.post(scrollRunnable)
                handler.post(clickScanRunnable)
            } else {
                toggleBtn.text = "▶ Chạy"
                toggleBtn.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#a6e3a1"))
                    cornerRadius = 30f
                }
                handler.removeCallbacks(scrollRunnable)
                handler.removeCallbacks(clickScanRunnable)
            }
        }

        speedBtn.setOnClickListener {
            speedLevel = if (speedLevel >= 10) 1 else speedLevel + 2
            if (speedLevel > 10) speedLevel = 10
            speedBtn.text = "⚡${speedLevel}x"
        }

        targetBtn.setOnClickListener {
            autoClickEnabled = !autoClickEnabled
            targetBtn.text = if (autoClickEnabled) "🎯 Bật" else "🎯 Tắt"
            targetBtn.setTextColor(
                if (autoClickEnabled) Color.parseColor("#f9e2af") else Color.parseColor("#6c7086")
            )
        }

        dirBtn.setOnClickListener {
            scrollDir *= -1
            swipeCountInDir = 0
            dirBtn.text = if (scrollDir == 1) "⬇" else "⬆"
        }

        // Kéo thả thanh điều khiển nổi trên màn hình
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(container, params)
                    true
                }
                else -> false
            }
        }

        container.addView(dragHandle)
        container.addView(toggleBtn)
        container.addView(speedBtn)
        container.addView(targetBtn)
        container.addView(dirBtn)

        floatingView = container
        windowManager?.addView(floatingView, params)
    }

    private fun performSmoothScrollStep() {
        val metrics = resources.displayMetrics
        val centerX = metrics.widthPixels / 2f
        val startY: Float
        val endY: Float

        if (scrollDir == 1) {
            // Cuộn xuống: vuốt từ dưới lên trên
            startY = metrics.heightPixels * 0.72f
            endY = metrics.heightPixels * 0.28f
        } else {
            // Cuộn lên: vuốt từ trên xuống dưới
            startY = metrics.heightPixels * 0.28f
            endY = metrics.heightPixels * 0.72f
        }

        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }

        val duration = (750L - (speedLevel * 45L)).coerceAtLeast(220L)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        dispatchGesture(gesture, null, null)

        swipeCountInDir++
        if (swipeCountInDir >= 12) {
            swipeCountInDir = 0
            scrollDir *= -1
        }
    }

    private fun scanAndClickTargetText() {
        val rootNode = rootInActiveWindow ?: return
        findAndClickNode(rootNode)
    }

    private fun findAndClickNode(node: AccessibilityNodeInfo): Boolean {
        val nodeText = (node.text?.toString() ?: "") + " " + (node.contentDescription?.toString() ?: "")
        val lower = nodeText.trim().lowercase()

        if (lower.isNotEmpty() && keywords.any { lower.contains(it) }) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                val clickX = rect.exactCenterX()
                val clickY = rect.exactCenterY()
                if (clickX > 0 && clickY > 0) {
                    performTap(clickX, clickY)
                    return true
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickNode(child)) {
                return true
            }
        }
        return false
    }

    private fun performTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 60L))
            .build()
        dispatchGesture(gesture, null, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
            floatingView = null
        }
    }
}
