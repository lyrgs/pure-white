package com.purewhite

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast

class WhiteTestActivity : Activity() {
    companion object {
        const val EXTRA_START_CAPTURE = "startCapture"
        private const val REQUEST_SCREEN_CAPTURE = 4102
    }

    private var captureRequested = false

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        window.decorView.setBackgroundColor(Color.WHITE)

        if (intent.getBooleanExtra(EXTRA_START_CAPTURE, false) && !captureRequested) {
            captureRequested = true
            window.decorView.post { requestScreenCapture() }
        }
    }

    @Suppress("DEPRECATION")
    private fun requestScreenCapture() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        try {
            startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_SCREEN_CAPTURE)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "无法请求屏幕共享：" + (e.message ?: e.javaClass.simpleName),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_SCREEN_CAPTURE) return
        if (resultCode != RESULT_OK || data == null) {
            Toast.makeText(this, "未获得屏幕录制授权。", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val serviceIntent = Intent(this, CaptureService::class.java).apply {
                putExtra("resultCode", resultCode)
                putExtra("captureData", data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Toast.makeText(this, "屏幕取色服务正在启动。", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "启动取色服务失败：" + (e.message ?: e.javaClass.simpleName),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
