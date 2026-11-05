package com.purewhite

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val captureRequest = 4102
    private lateinit var projectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 40, 28, 24)
        }
        layout.addView(TextView(this).apply {
            text = "Pure White\n\n全屏纯白测试，并读取屏幕中心 5×5 区域的截图像素平均值。"
            textSize = 18f
        })
        layout.addView(Button(this).apply {
            text = "打开纯白测试画面"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, WhiteTestActivity::class.java))
            }
        })
        layout.addView(Button(this).apply {
            text = "开始颜色采样"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    ))
                    Toast.makeText(
                        this@MainActivity,
                        "请允许悬浮窗权限，返回后再次点击采样。",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    startActivityForResult(
                        projectionManager.createScreenCaptureIntent(),
                        captureRequest
                    )
                }
            }
        })
        layout.addView(TextView(this).apply {
            text = "\n说明：截图 RGB 不一定包含系统显示驱动、硬件低蓝光及面板色温处理后的实际发光颜色。"
            textSize = 14f
        })
        setContentView(layout)
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != captureRequest) return

        if (resultCode != RESULT_OK || data == null) {
            Toast.makeText(this, "未获得屏幕录制授权，请重新开始采样。", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val serviceIntent = Intent(this, CaptureService::class.java).apply {
                putExtra("resultCode", resultCode)
                putExtra("captureData", data)
            }
            startForegroundService(serviceIntent)
            Toast.makeText(this, "采样服务正在启动，马上进入白屏。", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, WhiteTestActivity::class.java))
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "启动采样失败：" + (e.message ?: e.javaClass.simpleName),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
