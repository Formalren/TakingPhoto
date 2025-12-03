package com.example.takephoto.utils;

import android.content.Context;
import android.util.Log;
import com.example.takephoto.db.AppDatabase;
import com.example.takephoto.db.MediaItem;
import java.io.File;
import java.util.List;

public class CustomStorageManager {
    private static final long MAX_CACHE_SIZE = 500 * 1024 * 1024; // 500MB
    private static final String FOLDER_NAME = "MyTikTokApp";

    public static File getOutputDirectory(Context context) {
        File[] mediaDirs = context.getExternalMediaDirs();
        File mediaDir = null;
        if (mediaDirs != null && mediaDirs.length > 0) {
            mediaDir = new File(mediaDirs[0], FOLDER_NAME);
            mediaDir.mkdirs();
        }
        if (mediaDir != null && mediaDir.exists()) {
            return mediaDir;
        }
        return context.getFilesDir();
    }

    // 核心淘汰策略：在后台线程中调用此方法
    public static void enforceStoragePolicy(Context context) {
        File dir = getOutputDirectory(context);
        long currentSize = getFolderSize(dir);

        if (currentSize > MAX_CACHE_SIZE) {
            Log.d("Storage", "触发淘汰策略: 当前 " + currentSize);
            AppDatabase db = AppDatabase.getDatabase(context);

            // 获取最旧的文件
            List<MediaItem> oldestItems = db.mediaDao().getOldestItems();

            for (MediaItem item : oldestItems) {
                File file = new File(item.filePath);
                if (file.exists()) {
                    if (file.delete()) {
                        db.mediaDao().delete(item);
                        currentSize -= item.fileSize;
                        Log.d("Storage", "已删除: " + item.filePath);
                    }
                } else {
                    // 文件不存在但数据库有记录，清理脏数据
                    db.mediaDao().delete(item);
                }

                // 如果清理到阈值的 80% 以下，停止清理
                if (currentSize <= MAX_CACHE_SIZE * 0.8) break;
            }
        }
    }

    private static long getFolderSize(File directory) {
        long length = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) length += file.length();
                else length += getFolderSize(file);
            }
        }
        return length;
    }
}