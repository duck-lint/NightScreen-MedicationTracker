package com.redpillz.pixelutility.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.redpillz.pixelutility.MainActivity
import com.redpillz.pixelutility.R
import com.redpillz.pixelutility.RedPillzApp
import kotlin.math.roundToInt

class OverlayService : Service() {
    private val appContainer by lazy { (application as RedPillzApp).appContainer }
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        ensureNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopOverlay("Overlay stopped.")
            ACTION_START, ACTION_UPDATE -> {
                if (!Settings.canDrawOverlays(this)) {
                    stopOverlay("Overlay permission missing.")
                    return START_NOT_STICKY
                }
                val alpha = intent.getFloatExtra(EXTRA_ALPHA, 0.45f).coerceIn(0.1f, 0.8f)
                startForegroundCompat(buildNotification())
                showOverlay(alpha)
            }
            else -> Unit
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removeOverlayView()
        appContainer.overlayRuntimeStore.setStopped("Overlay stopped.")
        super.onDestroy()
    }

    private fun showOverlay(alpha: Float) {
        val overlay = overlayView ?: View(this).also { view ->
            windowManager.addView(view, createLayoutParams())
            overlayView = view
        }
        overlay.setBackgroundColor(
            Color.argb(
                (alpha * 255).roundToInt(),
                255,
                0,
                0,
            ),
        )
        appContainer.overlayRuntimeStore.setRunning(alpha)
    }

    private fun stopOverlay(message: String) {
        removeOverlayView()
        appContainer.overlayRuntimeStore.setStopped(message)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun removeOverlayView() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: IllegalArgumentException) {
                // The view can already be detached if Android tears down the window.
            }
        }
        overlayView = null
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.overlay_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.overlay_notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            1,
            createStopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val contentIntent = PendingIntent.getActivity(
            this,
            2,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.overlay_notification_stop),
                stopIntent,
            )
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "overlay_channel"
        private const val NOTIFICATION_ID = 1001
        private const val EXTRA_ALPHA = "extra_alpha"
        private const val ACTION_START = "com.redpillz.pixelutility.action.START_OVERLAY"
        private const val ACTION_UPDATE = "com.redpillz.pixelutility.action.UPDATE_OVERLAY"
        private const val ACTION_STOP = "com.redpillz.pixelutility.action.STOP_OVERLAY"

        fun createStartIntent(context: Context, alpha: Float): Intent {
            return Intent(context, OverlayService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ALPHA, alpha.coerceIn(0.1f, 0.8f))
            }
        }

        fun createUpdateIntent(context: Context, alpha: Float): Intent {
            return Intent(context, OverlayService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_ALPHA, alpha.coerceIn(0.1f, 0.8f))
            }
        }

        fun createStopIntent(context: Context): Intent {
            return Intent(context, OverlayService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
}
