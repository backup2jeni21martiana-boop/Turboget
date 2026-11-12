package com.example.data.model

data class SegmentInfo(
    val index: Int,
    val startByte: Long,
    val endByte: Long,
    var downloadedBytes: Long = 0L,
    var status: String = "IDLE"
) {
    val totalBytes: Long
        get() = if (endByte >= startByte) (endByte - startByte + 1) else 0L

    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}
