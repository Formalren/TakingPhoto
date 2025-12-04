# TakePhoto Demo (Android CameraX Sample)

📸 **TakePhoto** 是一个基于 Android Jetpack **CameraX** 库开发的相机应用示例。

该项目重点展示了如何在 Android 10+ (API 29+) 设备上正确处理**分区存储 (Scoped Storage)**，实现了照片/视频的拍摄、滤镜处理、以及系统相册的混合媒体调用与预览。
### 拍照展示
<img width="715" height="1178" alt="TakingPhoto" src="https://github.com/user-attachments/assets/371cc0dc-63d2-43cb-b68a-87f818c4fc3d" />
---
## 📱 功能特性 (Features)

* **📷 高清拍照 (Photo Capture)**
    * 使用 `ImageCapture` 用例。
    * 支持前后置摄像头切换。
    * **存储优化**：通过 `MediaStore` API 将照片直接保存至系统公共目录 (`Pictures/MyCameraApp`)，相册立即可见。
* **🎥 视频录制 (Video Recording)**
    * 使用 `VideoCapture` 用例。
    * 支持带音频的高清录制。
    * **存储优化**：视频自动保存至系统公共目录 (`Movies/MyCameraApp`)。
* **🎨 实时/后期滤镜 (Filters)**
    * 演示了获取当前 `ViewFinder` 的 Bitmap 并应用黑白滤镜 (B&W Filter) 的逻辑。
    * 滤镜照片同样通过 `ContentResolver` 插入系统相册。
* **🖼️ 混合相册跳转 (Gallery Integration)**
    * 使用 `ActivityResultLauncher` 替代过时的 `startActivityForResult`。
    * 支持 `*/*` MIME 类型，同时筛选显示图片和视频。
* **👁️ 应用内预览 (In-App Preview)**
    * 从相册返回后，直接在当前页面覆盖预览层。
    * 自动识别媒体类型：图片使用 `ImageView` 显示，视频使用 `VideoView` 自动循环播放。

---

## 🛠️ 技术栈 (Tech Stack)

* **Language**: Java
* **Camera**: [Android Jetpack CameraX](https://developer.android.com/training/camerax) (Preview, ImageCapture, VideoCapture)
* **UI Architecture**: ViewBinding
* **Storage**: Android MediaStore API (ContentResolver & ContentValues) - **适配 Android 10/11/12/13+ 分区存储**
* **Permission**: Runtime Permissions (Camera, Audio, Storage)
* **Interaction**: ActivityResultContracts (New Intent API)

---


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
1. Android Studio Ladybug | 2024.2.1 或更高版本

2. minSdkVersion 21

3. targetSdkVersion 34 (Android 14)

### 安装步骤
1. Clone 本仓库到本地：
```text
git clone [https://github.com/your-username/TakePhoto.git](https://github.com/your-username/TakePhoto.git)
```

2. 在 Android Studio 中打开项目。

3. 连接真机或使用模拟器（建议使用真机以测试相机功能）。

4. 运行 App，并授予 相机 和 麦克风 权限。


## 📝 版本历史
v1.0.0

1. 完成拍摄、录像功能、相册、翻转镜头、取景增加黑白特效。

2. 接入 Room 数据库。

3. 实现 LRU 存储淘汰策略。

## 📄 License
Apache License 2.0

