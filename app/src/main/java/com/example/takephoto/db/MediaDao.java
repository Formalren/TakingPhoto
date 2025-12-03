package com.example.takephoto.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface MediaDao {
    @Insert
    void insert(MediaItem item);

    @Query("SELECT * FROM media_items ORDER BY timestamp ASC")
    List<MediaItem> getOldestItems(); // 获取最旧的数据用于淘汰

    @Delete
    void delete(MediaItem item);
}