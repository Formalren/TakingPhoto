package com.example.takephoto.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "media_items")
public class MediaItem {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String filePath;
    public String type; // "PHOTO" or "VIDEO"
    public long timestamp;
    public long fileSize;

    public MediaItem(String filePath, String type, long timestamp, long fileSize) {
        this.filePath = filePath;
        this.type = type;
        this.timestamp = timestamp;
        this.fileSize = fileSize;
    }
}