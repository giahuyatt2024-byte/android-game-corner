package com.rog.gamecorner

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import kotlin.math.roundToInt

/**
 * Foreground overlay service for the in-game ROG button and performance HUD.
 *
 * Tap the floating eye to expand/collapse the HUD. Each hexagonal setting
 * responds to a tap and cycles through its supported values.
 */
class GameOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayRoot: FrameLayout? = null
    private var iconView: RogOverlayIconView? = null
    private var hudView: HudOverlayView? = null
    private var isExpanded = false

    private lateinit var statsReader: DeviceStatsReader
    private val statsHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val statsUpdater = object : Runnable {
        override fun run() {
            updateHud()
            statsHandler.postDelayed(this, 1000L)
        }
    }
    private var xMode = false

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        statsReader = DeviceStatsReader(this)
        addOverlay()
        statsHandler.post(statsUpdater)
    }

    override fun onDestroy() {
        statsHandler.removeCallbacks(statsUpdater)
        overlayRoot?.let { root ->
            runCatching { windowManager.removeView(root) }
        }
        overlayRoot = null
        iconView = null
        hudView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun addOverlay() {
        val root = FrameLayout(this)
        val icon = RogOverlayIconView(this).apply {
            setOnClickListener {
                if (isExpanded) hideHud() else showHud()
            }
        }
        val hud = HudOverlayView(this).apply {
            visibility = View.GONE
            onSettingChanged = { setting ->
                when (setting) {
                    HudSetting.X_MODE -> {
                        xMode = !xMode
                        hudView?.setXMode(xMode)
                    }
                    HudSetting.FPS,
                    HudSetting.RAM,
                    HudSetting.TEMPERATURE -> Unit
                }
                updateHud()
            }
            onClose = {
                hideHud()
            }
        }

        root.addView(
            hud,
            FrameLayout.LayoutParams(dp(300), dp(250)).apply {
                gravity = Gravity.CENTER
            },
        )
        root.addView(
            icon,
            FrameLayout.LayoutParams(dp(68), dp(68)).apply {
                gravity = Gravity.TOP or Gravity.START
                leftMargin = dp(8)
                topMargin = dp(8)
            },
        )

        val params = WindowManager.LayoutParams(
            dp(360),
            dp(320),
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(24)
            y = dp(180)
        }

        windowManager.addView(root, params)
        overlayRoot = root
        iconView = icon
        hudView = hud
        installDragHandler(icon, params)
        updateHud()
    }

    private fun showHud() {
        isExpanded = true
        val hud = hudView ?: return
        hud.visibility = View.VISIBLE
        hud.alpha = 0f
        hud.scaleX = 0.82f
        hud.scaleY = 0.82f
        hud.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(260L)
            .start()
        iconView?.setExpanded(true)
        iconView?.animate()?.rotationBy(180f)?.setDuration(260L)?.start()
    }

    private fun hideHud() {
        isExpanded = false
        val hud = hudView ?: return
        hud.animate()
            .alpha(0f)
            .scaleX(0.82f)
            .scaleY(0.82f)
            .setDuration(180L)
            .withEndAction { hud.visibility = View.GONE }
            .start()
        iconView?.setExpanded(false)
        iconView?.animate()?.rotationBy(-180f)?.setDuration(180L)?.start()
    }

    private fun installDragHandler(view: View, params: WindowManager.LayoutParams) {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - downX).roundToInt()
                    val deltaY = (event.rawY - downY).roundToInt()
                    if (kotlin.math.abs(deltaX) > dp(6) || kotlin.math.abs(deltaY) > dp(6)) {
                        moved = true
                    }
                    if (moved) {
                        params.x = startX + deltaX
                        params.y = startY + deltaY
                        overlayRoot?.let { root ->
                            windowManager.updateViewLayout(root, params)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) view.performClick()
                    true
                }
                else -> true
            }
        }
    }

    private fun updateHud() {
        val stats = statsReader.read()
        hudView?.setValues(
            xMode = if (xMode) "ON" else "OFF",
            fps = stats.refreshRateText,
            ram = stats.ramText,
            temperature = stats.temperatureText,
        )
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "GAME CORNER HUD",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Keeps the GAME CORNER performance HUD active."
                },
            )
        }
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("GAME CORNER HUD đang hoạt động")
            .setContentText("Chạm biểu tượng ROG trên màn hình để mở menu.")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .build()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val CHANNEL_ID = "game_corner_hud"
        private const val NOTIFICATION_ID = 4101
    }
}