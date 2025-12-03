# 📹 TikTok-Lite (Android Camera Demo)

> 一个基于 **CameraX** + **Room** + **Java** 实现的简易短视频拍摄应用。
> 实现了类似抖音的拍摄流程，包含实时取景、拍照/录像、滤镜特效以及自定义的存储淘汰策略。

## 📖 项目简介 (Introduction)

本项目是一个 Android 原生相机应用 Demo，演示了如何使用 Google 官方推荐的 **CameraX** 库进行多媒体开发。项目不依赖大型第三方框架，从零实现了相机预览、媒体采集、本地数据库存储以及一套**自研的文件存储管理与淘汰算法**。

主要用于展示 Android 多媒体开发、数据库操作以及架构设计的最佳实践。

## 📱 功能特性 (Features)

* **📸 高清拍摄**：支持高清拍照 (Photo) 和 视频录制 (Video)。
* **🔄 镜头切换**：支持前置/后置摄像头一键切换。
* **✨ 实时特效**：
    * 集成 `ColorMatrix` 实现实时 **黑白滤镜 (B&W Filter)**。
    * 支持在预览流中直接查看特效，并保存带特效的截图。
* **💾 数据持久化**：
    * 使用 **Room Database** 保存媒体文件的元数据（路径、类型、时间戳）。
    * 使用 **Java NIO** 进行文件操作。
* **🧹 智能存储管理 (自研)**：
    * 实现了 `CustomStorageManager`。
    * **淘汰策略**：当缓存目录超过阈值 (500MB) 时，自动查询数据库中时间戳最早的文件进行删除，直到空间释放至安全水位 (80%)。
* **🛠️ 兼容性适配**：
    * 适配 **Android 15 (API 35)** 的 16KB Page Size 要求。
    * 解决模拟器环境下的预览黑屏/绿屏问题。

## 🛠 技术栈 (Tech Stack)

* **语言**: Java 8+
* **核心组件**:
    * [CameraX](https://developer.android.com/training/camerax) (v1.4.0) - 相机预览与生命周期绑定
    * [Jetpack Room](https://developer.android.com/training/data-storage/room) (v2.6.1) - ORM 数据库
    * [ViewBinding](https://developer.android.com/topic/libraries/view-binding) - UI 绑定
* **异步处理**: `ExecutorService` (线程池) / `ListenableFuture`
* **架构模式**: MVVM (View - Dao - Utils)

## 📂 项目结构 (Project Structure)

```text
com.example.takephoto
├── db/                     # 数据库层
│   ├── AppDatabase.java    # Room 数据库实例
│   ├── MediaDao.java       # 数据访问对象 (CRUD)
│   └── MediaItem.java      # 媒体实体类
├── utils/                  # 工具层
│   └── CustomStorageManager.java # 核心：自定义存储管理与淘汰策略
└── MainActivity.java       # UI 与 业务逻辑 (相机控制、滤镜、权限)
```

## 🚀 快速开始 (Getting Started)
### 环境要求
Android Studio Ladybug | 2024.2.1+

minSdk 24

targetSdk 34 (或更高)

Gradle 8.0+

### 模拟器运行说明 (重要)
如果你在 Android 模拟器上运行，默认情况下可能会显示绿色背景或黑屏。请按照以下步骤配置：

打开 Device Manager -> 编辑模拟器 (Edit)。

点击 Show Advanced Settings。

将 Camera Back 设置从 VirtualScene 改为 Webcam0 (使用电脑摄像头)。

冷启动 (Cold Boot) 模拟器。

核心代码片段

## 📝 版本历史
v1.0.0

完成基础拍摄功能。

接入 Room 数据库。

实现 LRU 存储淘汰策略。

修复 Android 15 .so 库 16KB 对齐问题 (CameraX 1.4.0)。

## 📄 License
Apache License 2.0

