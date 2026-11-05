# Pure White 1.2（启动流程修正版源码）

## 本次调整
- 点击“共享屏幕取色”后，先打开全屏白色 Activity。
- 屏幕共享授权对话框由前台白屏 Activity 发起。
- 授权成功后，由当前白屏 Activity 启动 CaptureService，不再从返回后台的 MainActivity 启动另一个 Activity。
- 目的是避开部分 Android 系统/厂商系统对后台 Activity 启动的限制。

## 构建
使用 Android Studio 打开项目根目录，等待 Gradle Sync，然后选择 Build > Build APK(s)。
生成的 APK 通常位于 app/build/outputs/apk/debug/app-debug.apk。

## 使用
1. 安装新 APK（若签名不一致，系统可能要求先卸载旧版；卸载前请注意历史数据会丢失）。
2. 允许 Pure White“显示在其他应用上层”。
3. 点击“共享屏幕取色”。
4. 系统询问屏幕共享时选择允许。
5. 授权返回后应停留在白屏页面；悬浮窗显示 RGB 读数。

## 注意
这是针对启动顺序的源码修正版，尚未在真实 Android 17 / HyperOS 4.0.3.0 设备上编译和验证。截图 RGB 不等于面板真实发光色度；硬件色温/低蓝光处理可能不会反映在截图中。
