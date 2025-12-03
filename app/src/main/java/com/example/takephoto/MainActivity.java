package com.example.takephoto;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.takephoto.databinding.ActivityMainBinding;
import com.example.takephoto.db.AppDatabase;
import com.example.takephoto.db.MediaItem;
import com.example.takephoto.utils.CustomStorageManager;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.View;
// ... 其他原有的 import
public class MainActivity extends AppCompatActivity {
    private boolean isFilterOn = false; // 记录滤镜是否开启
    private ActivityMainBinding binding;
    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;
    private Recording recording;
    private ExecutorService cameraExecutor;

    // 默认后置摄像头
    private int cameraSelectorFacing = CameraSelector.LENS_FACING_BACK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化后台线程池（用于数据库和文件操作）
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        setupUI();
    }
    // 必须定义在类的成员变量位置（onCreate 之外）
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri == null) return;

                    // 1. 获取文件类型（是图片还是视频？）
                    String mimeType = getContentResolver().getType(uri);

                    // 2. 显示预览容器
                    binding.previewContainer.setVisibility(View.VISIBLE);

                    if (mimeType != null && mimeType.startsWith("video")) {
                        // --- 视频处理 ---
                        binding.ivPreviewPhoto.setVisibility(View.GONE);
                        binding.vvPreviewVideo.setVisibility(View.VISIBLE);

                        binding.vvPreviewVideo.setVideoURI(uri);
                        binding.vvPreviewVideo.start(); // 自动播放

                        // 循环播放
                        binding.vvPreviewVideo.setOnCompletionListener(mp -> binding.vvPreviewVideo.start());

                    } else {
                        // --- 图片处理 ---
                        binding.vvPreviewVideo.setVisibility(View.GONE);
                        binding.ivPreviewPhoto.setVisibility(View.VISIBLE);

                        binding.ivPreviewPhoto.setImageURI(uri);
                    }

                    Toast.makeText(this, "正在预览: " + mimeType, Toast.LENGTH_SHORT).show();
                }
            }
    );
    private void setupUI() {
        binding.btnCapture.setOnClickListener(v -> takePhoto());
        binding.btnRecord.setOnClickListener(v -> captureVideo());
        binding.btnSwitchCamera.setOnClickListener(v -> {
            if (cameraSelectorFacing == CameraSelector.LENS_FACING_BACK) {
                cameraSelectorFacing = CameraSelector.LENS_FACING_FRONT;
            } else {
                cameraSelectorFacing = CameraSelector.LENS_FACING_BACK;
            }
            startCamera();
        });
        // --- 新增：点击预览层，关闭预览，回到相机 ---
        binding.previewContainer.setOnClickListener(v -> {
            // 停止视频播放
            if (binding.vvPreviewVideo.isPlaying()) {
                binding.vvPreviewVideo.stopPlayback();
            }
            // 隐藏预览层
            binding.previewContainer.setVisibility(View.GONE);
        });
        binding.btnGallery.setOnClickListener(v -> {
            Toast.makeText(this, "打开相册...", Toast.LENGTH_SHORT).show();

            // 1. 使用 ACTION_GET_CONTENT 或者 ACTION_PICK 来获取内容
            // 注意：不要传 MediaStore.Images.Media.EXTERNAL_CONTENT_URI，否则会限定在图片库
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

            // 2. 关键修改：类型设为 "*/*" 表示所有媒体
            intent.setType("*/*");

            // 3. 显式指定只显示 "图片" 和 "视频" (过滤掉音乐、文档等其他文件)
            String[] mimeTypes = {"image/*", "video/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

            intent.addCategory(Intent.CATEGORY_OPENABLE);

            // 4. 启动跳转
            galleryLauncher.launch(intent);
        });
        binding.btnEffect.setOnClickListener(v -> toggleFilter());
    }



    // 具体的滤镜实现方法
    private void toggleFilter() {
        isFilterOn = !isFilterOn;

        if (isFilterOn) {
            // 1. 创建黑白滤镜矩阵
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0); // 饱和度设为0，即变成黑白
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);

            // 2. 创建 Paint 并应用滤镜
            Paint paint = new Paint();
            paint.setColorFilter(filter);

            // 3. 将滤镜应用到 PreviewView 的内部 View 上
            // PreviewView 本身是一个容器，我们需要给它的子 View (TextureView) 设置 LayerType
            if (binding.viewFinder.getChildCount() > 0) {
                View textureView = binding.viewFinder.getChildAt(0);
                textureView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
            }
            binding.btnEffect.setText("Normal");
            Toast.makeText(this, "特效开启：黑白模式", Toast.LENGTH_SHORT).show();
        } else {
            // 关闭滤镜：移除 Paint
            if (binding.viewFinder.getChildCount() > 0) {
                View textureView = binding.viewFinder.getChildAt(0);
                textureView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
            binding.btnEffect.setText("Filter");
            Toast.makeText(this, "特效关闭", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // ================== 修正点 1: 设置 View 的模式 ==================
                // 注意：这一行是设置在 viewFinder (UI控件) 上，而不是 Preview (用例) 上
                // COMPATIBLE 模式会强制使用 TextureView，这样才支持滤镜
                binding.viewFinder.setImplementationMode(androidx.camera.view.PreviewView.ImplementationMode.COMPATIBLE);

                // ================== 修正点 2: 恢复正常的 Preview 构建 ==================
                Preview preview = new Preview.Builder().build();

                preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraSelectorFacing).build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("Camera", "Binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        // 1. 如果开启滤镜，使用 Bitmap 方式保存到系统相册
        if (isFilterOn) {
            Bitmap bitmap = binding.viewFinder.getBitmap();
            if (bitmap != null) {
                // 注意：这里需要你写一个新方法把 Bitmap 存入 MediaStore
                // 为了方便你，我在最后附带了这个 saveBitmapToGallery 的实现代码
                saveBitmapToGallery(bitmap);
            }
        } else {
            if (imageCapture == null) return;

            // 2. 准备存储参数 (存入系统相册)
            String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis());
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            // Android 9 (Q) 及以上可以指定文件夹，例如 "Pictures/MyCameraApp"
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyCameraApp");
            }

            // 3. 创建输出选项 (指向 MediaStore，而不是 File)
            ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                    getContentResolver(),
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
            ).build();

            // 4. 拍照
            imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            // 获取系统相册中的 Uri
                            Uri savedUri = outputFileResults.getSavedUri();
                            if (savedUri == null) {
                                savedUri = Uri.parse(""); // 防空处理
                            }

                            String msg = "Photo saved: " + savedUri.toString();
                            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();

                            // ⚠️ 注意：这里 saveToDb 可能需要修改
                            // 因为现在没有 File 对象了，建议修改 saveToDb 接收 Uri 字符串
                            // saveToDb(savedUri.toString(), "PHOTO");
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Log.e("Camera", "Photo capture failed: " + exception.getMessage(), exception);
                        }
                    });
        }
    }

    // 辅助方法：保存 Bitmap 到文件
    private void saveBitmapToGallery(Bitmap bitmap) {
        String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyCameraApp");
        }

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        if (uri != null) {
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                Toast.makeText(this, "滤镜照片已保存: " + uri.toString(), Toast.LENGTH_SHORT).show();
                // saveToDb(uri.toString(), "PHOTO");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void captureVideo() {
        if (videoCapture == null) return;

        if (recording != null) {
            recording.stop();
            recording = null;
            return;
        }

        // 1. 准备视频存储参数
        String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        // 指定存放在 Movies/MyCameraApp 文件夹下
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/MyCameraApp");
        }

        // 2. 使用 MediaStoreOutputOptions (关键修改)
// ✅ 正确写法：构造函数传2个参数，ContentValues 用方法设置
        MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(
                getContentResolver(),
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues) // <--- 改到这里
                .build();

        // 权限检查
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "请先授予录音权限", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. 开始录制
        recording = videoCapture.getOutput()
                .prepareRecording(this, options)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this), recordEvent -> {
                    if (recordEvent instanceof VideoRecordEvent.Start) {
                        binding.btnRecord.setSelected(true);
                    } else if (recordEvent instanceof VideoRecordEvent.Finalize) {
                        VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) recordEvent;

                        if (!finalizeEvent.hasError()) {
                            Uri savedUri = finalizeEvent.getOutputResults().getOutputUri();
                            String msg = "Video saved: " + savedUri.toString();
                            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();

                            // ⚠️ 同样注意更新 saveToDb 以支持 Uri
                            // saveToDb(savedUri.toString(), "VIDEO");

                            // 原来的 storagePolicy 可能需要逻辑调整，因为现在文件不在私有目录了
                            // cameraExecutor.execute(() -> CustomStorageManager.enforceStoragePolicy(...));
                        } else {
                            recording.close();
                            recording = null;
                            Log.e("Camera", "Video error: " + finalizeEvent.getError());
                        }
                        binding.btnRecord.setSelected(false);
                    }
                });
    }

    private void saveToDb(File file, String type) {
        cameraExecutor.execute(() -> {
            MediaItem item = new MediaItem(
                    file.getAbsolutePath(),
                    type,
                    System.currentTimeMillis(),
                    file.length()
            );
            AppDatabase.getDatabase(getApplicationContext()).mediaDao().insert(item);
        });
    }

    // 权限相关常量和方法
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE // Android 10以下需要
    };
    private static final int REQUEST_CODE_PERMISSIONS = 10;

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}