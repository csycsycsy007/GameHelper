package com.example.gamehelper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

/**
 * 自动点击服务
 * 通过无障碍服务实现屏幕自动点击功能
 */
class AutoClickService : AccessibilityService() {

    companion object {
        // 服务实例
        var instance: AutoClickService? = null

        // 点击状态
        var isClicking = false

        // 点击参数
        var clickX = 500f
        var clickY = 500f
        var clickInterval = 1000L // 默认1秒间隔
    }

    private var clickJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 预览相关
    private var windowManager: WindowManager? = null
    private var previewView: ImageView? = null
    private var isPreviewShowing = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        stopClicking()
        hidePreview()
        serviceScope.cancel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不需要处理事件
    }

    override fun onInterrupt() {
        // 服务被中断时停止点击
        stopClicking()
        hidePreview()
    }

    /**
     * 开始自动点击
     */
    fun startClicking() {
        if (isClicking) return

        isClicking = true
        clickJob = serviceScope.launch {
            while (isClicking) {
                performClick(clickX, clickY)
                delay(clickInterval)
            }
        }
    }

    /**
     * 停止自动点击
     */
    fun stopClicking() {
        isClicking = false
        clickJob?.cancel()
        clickJob = null
    }

    /**
     * 设置点击位置
     */
    fun setClickPosition(x: Float, y: Float) {
        clickX = x
        clickY = y
        // 更新预览位置
        if (isPreviewShowing) {
            updatePreviewPosition()
        }
    }

    /**
     * 设置点击间隔
     */
    fun setClickInterval(interval: Long) {
        clickInterval = interval
    }

    /**
     * 显示坐标预览
     */
    fun showPreview() {
        if (isPreviewShowing || windowManager == null) return

        try {
            // 创建预览视图
            previewView = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_mylocation)
                setColorFilter(ContextCompat.getColor(this@AutoClickService, android.R.color.holo_red_dark))
            }

            // 设置窗口参数
            val params = WindowManager.LayoutParams(
                60, // 宽度
                60, // 高度
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (clickX - 30).toInt() // 居中显示
                y = (clickY - 30).toInt()
            }

            windowManager?.addView(previewView, params)
            isPreviewShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 隐藏坐标预览
     */
    fun hidePreview() {
        if (!isPreviewShowing || previewView == null || windowManager == null) return

        try {
            windowManager?.removeView(previewView)
            previewView = null
            isPreviewShowing = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 更新预览位置
     */
    private fun updatePreviewPosition() {
        if (!isPreviewShowing || previewView == null || windowManager == null) return

        try {
            val params = previewView?.layoutParams as? WindowManager.LayoutParams
            params?.let {
                it.x = (clickX - 30).toInt()
                it.y = (clickY - 30).toInt()
                windowManager?.updateViewLayout(previewView, it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 执行点击操作
     */
    private fun performClick(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)

        val gestureBuilder = GestureDescription.Builder()
        val strokeDescription = GestureDescription.StrokeDescription(path, 0, 100)
        gestureBuilder.addStroke(strokeDescription)

        val gesture = gestureBuilder.build()
        dispatchGesture(gesture, null, null)
    }
}
