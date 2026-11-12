package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.DownloadCategory
import com.example.data.model.DownloadStatus

class DownloadTypeConverters {
    @TypeConverter
    fun fromStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): DownloadStatus = try {
        DownloadStatus.valueOf(value)
    } catch (_: Exception) {
        DownloadStatus.QUEUED
    }

    @TypeConverter
    fun fromCategory(category: DownloadCategory): String = category.name

    @TypeConverter
    fun toCategory(value: String): DownloadCategory = try {
        DownloadCategory.valueOf(value)
    } catch (_: Exception) {
        DownloadCategory.OTHER
    }
}
