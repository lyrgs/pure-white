# Pure White — Android Studio 工程

## 当前包含
- 全屏纯白测试页（数字 RGB 255,255,255）。
- Android MediaProjection 屏幕捕获。
- 屏幕中心 5×5 像素 RGB 平均值。
- 左上角悬浮窗显示 RGB 和粗略偏暖/偏冷提示。
- 每次采样写入本地历史，最多保留最近 500 条（时间戳、R、G、B）。

## 构建方式
1. 使用 Android Studio 打开工程根目录。
2. 安装并使用 JDK 17，等待 Gradle 同步。
3. 选择 Build > Build Bundle(s) / APK(s) > Build APK(s)。

## 重要测量边界
MediaProjection 读取的是系统屏幕捕获图像，不保证包含 HyperOS 显示驱动、硬件低蓝光、硬件色温或面板自身处理之后的真实发光颜色。截图 RGB 不等于人眼实际看到的光谱/白点；RGB(255,255,255) 也不意味着面板一定发出 D65 标准白。真正测量面板发光需要外置色度计或校色仪。

## 兼容性状态
这是一个基础源码工程，尚未在小米 HyperOS 4.0.3 / Android 17 真机编译和验证。Android 不同版本对 MediaProjection 前台服务和授权流程有差异，实际运行可能需要针对设备适配。本工程不应作为专业校色仪器使用。
