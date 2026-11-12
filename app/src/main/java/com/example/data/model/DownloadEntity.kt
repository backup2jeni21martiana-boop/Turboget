package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val fileName: String,
    val destinationPath: String,
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val supportsResume: Boolean = false,
    val numConnections: Int = 4,
    val category: DownloadCategory = DownloadCategory.OTHER,
    val mimeType: String? = null,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val scheduledTimeMillis: Long? = null,
    val scheduledStopMillis: Long? = null,
    val wifiOnly: Boolean = false,
    val priority: Int = 0,
    val segmentsJson: String? = null
) {
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val isFinished: Boolean
        get() = status == DownloadStatus.COMPLETED || status == DownloadStatus.FAILED || status == DownloadStatus.CANCELLED

    val isRunning: Boolean
        get() = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.CONNECTING
}
