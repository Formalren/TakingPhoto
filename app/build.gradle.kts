plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.takephoto"

    // 你使用的是较新的 SDK 版本配置方式
    compileSdk {
        // 如果这里报错，可以改回 compileSdk = 34
        // version = release(36)
    }
    compileSdk = 34 // 建议暂时使用稳定版 34，以防 36 预览版出现兼容问题

    defaultConfig {
        applicationId = "com.example.takephoto"
        minSdk = 24
        targetSdk = 34 // 建议与 compileSdk 保持一致
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // 确保支持 Java 8 (CameraX 和 Room 需要)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // 开启 ViewBinding (项目代码中用到了)
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // -----------------------------------------------------------
    //原有依赖 (保持不变)
    // -----------------------------------------------------------
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // -----------------------------------------------------------
    // 新增依赖 (TikTok Lite 项目所需)
    // -----------------------------------------------------------

    // 1. CameraX (相机核心)
    val cameraxVersion = "1.4.0"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-video:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-extensions:$cameraxVersion")

    // 2. Room 数据库 (Java 项目使用 annotationProcessor)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")

    // 3. Glide (图片加载)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // 4. Guava (用于 CameraX 的 ListenableFuture 异步处理)
    implementation("com.google.guava:guava:31.1-android")
}