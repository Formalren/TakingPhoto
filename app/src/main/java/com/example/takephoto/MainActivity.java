package com.example.tiktoklite;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
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
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

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

    private void setupUI() {
        // 拍照
        binding.btnCapture.setOnClickListener(v -> takePhoto());

        // 录像
        binding.btnRecord.setOnClickListener(v -> captureVideo());

        // 翻转镜头
        binding.btnSwitchCamera.setOnClickListener(v -> {
            if (cameraSelectorFacing == CameraSelector.LENS_FACING_BACK) {
                cameraSelectorFacing = CameraSelector.LENS_FACING_FRONT;
            } else {
                cameraSelectorFacing = CameraSelector.LENS_FACING_BACK;
            }
            startCamera();
        });

        // 相册 (仅作示例，需自行实现跳转)
        binding.btnGallery.setOnClickListener(v ->
                Toast.makeText(this, "跳转相册页面...", Toast.LENGTH_SHORT).show()
        );
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 预览用例
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());

                // 拍照用例
                imageCapture = new ImageCapture.Builder().build();

                // 录像用例
                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                // 选择摄像头
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
        if (imageCapture == null) return;

        // 1. 在后台执行存储策略检查 (异步)
        cameraExecutor.execute(() -> CustomStorageManager.enforceStoragePolicy(getApplicationContext()));

        // 2. 准备文件
        File photoFile = new File(CustomStorageManager.getOutputDirectory(this),
                new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // 3. 拍照
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        String msg = "Photo saved: " + photoFile.getAbsolutePath();
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();

                        // 保存记录到数据库
                        saveToDb(photoFile, "PHOTO");
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("Camera", "Photo capture failed: " + exception.getMessage(), exception);
                    }
                });
    }

    private void captureVideo() {
        if (videoCapture == null) return;

        if (recording != null) {
            // 停止录制
            recording.stop();
            recording = null;
            return;
        }

        File videoFile = new File(CustomStorageManager.getOutputDirectory(this),
                new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".mp4");

        FileOutputOptions options = new FileOutputOptions.Builder(videoFile).build();

        // 开始录制
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return; // 简单处理权限
        }

        recording = videoCapture.getOutput()
                .prepareRecording(this, options)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this), recordEvent -> {
                    if (recordEvent instanceof VideoRecordEvent.Start) {
                        binding.btnRecord.setText("Stop");
                    } else if (recordEvent instanceof VideoRecordEvent.Finalize) {
                        VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) recordEvent;
                        if (!finalizeEvent.hasError()) {
                            String msg = "Video saved: " + videoFile.getAbsolutePath();
                            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                            saveToDb(videoFile, "VIDEO");

                            // 录制完成后检查存储空间
                            cameraExecutor.execute(() -> CustomStorageManager.enforceStoragePolicy(getApplicationContext()));
                        } else {
                            recording.close();
                            recording = null;
                        }
                        binding.btnRecord.setText("Record");
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