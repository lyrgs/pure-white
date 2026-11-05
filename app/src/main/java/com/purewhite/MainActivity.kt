package com.purewhite

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            text = "共享屏幕取色"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    ))
                    Toast.makeText(
                        this@MainActivity,
                        "请允许悬浮窗权限，返回后再次点击取色。",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // 先把白屏 Activity 放到前台，再由它请求录屏授权。
                    // 授权返回时不再从后台 MainActivity 启动新 Activity。
                    startActivity(Intent(this@MainActivity, WhiteTestActivity::class.java).apply {
                        putExtra(WhiteTestActivity.EXTRA_START_CAPTURE, true)
                    })
                }
            }
        })
        layout.addView(TextView(this).apply {
            text = "\n说明：截图 RGB 不一定包含系统显示驱动、硬件低蓝光及面板色温处理后的实际发光颜色。"
            textSize = 14f
        })
        setContentView(layout)
    }
}
