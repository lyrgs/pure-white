package com.purewhite

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class CaptureService : Service() {
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var overlay: TextView? = null
    private lateinit var windowManager: WindowManager
    private lateinit var worker: Handler
    private var workerThread: HandlerThread? = null

    override fun onCreate() {
        super.onCreate()
        workerThread = HandlerThread("PureWhiteCapture").apply { start() }
        worker = Handler(workerThread!!.looper)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotification()
        showOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", 0) ?: 0
        @Suppress("DEPRECATION")
        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra("captureData", Intent::class.java)
        } else {
            intent?.getParcelableExtra("captureData")
        }

        if (data == null) {
            showError("没有收到屏幕录制授权数据")
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopSelf()
                }
            }, worker)
            startCapture()
            overlay?.post { overlay?.text = "Pure White\n正在读取屏幕…" }
        } catch (e: Exception) {
            showError("采样启动失败：${e.message ?: e.javaClass.simpleName}")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun createNotification() {
        val channelId = "purewhite_capture"
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                channelId, "Pure White 采样", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, channelId)
                .setContentTitle("Pure White 正在采样")
                .setContentText("每秒读取屏幕中心区域")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Pure White 正在采样")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                7, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(7, notification)
        }
    }

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            showError("悬浮窗权限未开启")
            return
        }
        val view = TextView(this).apply {
            text = "Pure White\n等待采样…"
            textSize = 13f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xE6202020.toInt())
            setPadding(16, 12, 16, 12)
            elevation = 12f
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 12
            y = 48
        }
        try {
            windowManager.addView(view, params)
            overlay = view
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "悬浮窗创建失败：" + (e.message ?: e.javaClass.simpleName),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showError(message: String) {
        android.util.Log.e("PureWhite", message)
        overlay?.post { overlay?.text = message }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun startCapture() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
        reader = imageReader

        imageReader.setOnImageAvailableListener({ source ->
            var image: Image? = null
            try {
                image = source.acquireLatestImage()
                if (image != null) {
                    val plane = image.planes[0]
                    val buffer = plane.buffer
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val startX = width / 2 - 2
                    val startY = height / 2 - 2
                    var red = 0L
                    var green = 0L
                    var blue = 0L
                    var count = 0
                    for (y in startY..(startY + 4)) {
                        for (x in startX..(startX + 4)) {
                            val index = y * rowStride + x * pixelStride
                            if (index >= 0 && index + 2 < buffer.limit()) {
                                red += buffer.get(index).toInt() and 0xff
                                green += buffer.get(index + 1).toInt() and 0xff
                                blue += buffer.get(index + 2).toInt() and 0xff
                                count++
                            }
                        }
                    }
                    if (count > 0) {
                        val r = (red / count).toInt()
                        val g = (green / count).toInt()
                        val b = (blue / count).toInt()
                        val hint = when {
                            r - b > 8 -> "偏暖"
                            b - r > 8 -> "偏冷"
                            else -> "接近中性"
                        }
                        overlay?.post {
                            overlay?.text = String.format(
                                Locale.US, "RGB %d, %d, %d\n%s\n每秒更新", r, g, b, hint
                            )
                        }
                        saveSample(r, g, b)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PureWhite", "读取屏幕像素失败", e)
            } finally {
                image?.close()
            }
        }, worker)

        virtualDisplay = projection?.createVirtualDisplay(
            "PureWhiteDisplay",
            width,
            height,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            worker
        )
        if (virtualDisplay == null) {
            throw IllegalStateException("虚拟屏幕创建失败")
        }
    }

    private fun saveSample(r: Int, g: Int, b: Int) {
        val preferences = getSharedPreferences("history", MODE_PRIVATE)
        val previous = preferences.getString("samples", "") ?: ""
        val newLine = "${System.currentTimeMillis()},$r,$g,$b"
        val rows = (previous.split("\n").filter { it.isNotBlank() } + newLine).takeLast(500)
        preferences.edit().putString("samples", rows.joinToString("\n")).apply()
    }

    override fun onDestroy() {
        try {
            virtualDisplay?.release()
            reader?.close()
            projection?.stop()
            overlay?.let { windowManager.removeView(it) }
            workerThread?.quitSafely()
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
