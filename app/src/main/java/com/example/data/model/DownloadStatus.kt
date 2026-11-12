package com.example.data.model

enum class DownloadStatus {
    QUEUED,
    CONNECTING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    SCHEDULED,
    CANCELLED
}

enum class DownloadCategory(val displayName: String) {
    ALL("Semua"),
    VIDEO("Video"),
    DOCUMENT("Dokumen"),
    AUDIO("Musik"),
    ARCHIVE("Arsip"),
    PROGRAM("Program/APK"),
    OTHER("Lainnya");

    companion object {
        fun fromFileName(fileName: String, mimeType: String? = null): DownloadCategory {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return when {
                ext in listOf("mp4", "mkv", "avi", "mov", "webm", "flv", "wmv", "3gp") ||
                        (mimeType?.startsWith("video/") == true) -> VIDEO
                ext in listOf("mp3", "m4a", "flac", "wav", "ogg", "aac", "opus") ||
                        (mimeType?.startsWith("audio/") == true) -> AUDIO
                ext in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "epub") ||
                        (mimeType?.contains("pdf") == true || mimeType?.contains("document") == true) -> DOCUMENT
                ext in listOf("zip", "rar", "7z", "tar", "gz", "bz2", "iso", "tgz") ||
                        (mimeType?.contains("zip") == true || mimeType?.contains("compressed") == true) -> ARCHIVE
                ext in listOf("apk", "exe", "dmg", "deb", "rpm", "msi") ||
                        (mimeType?.contains("android.package-archive") == true) -> PROGRAM
                else -> OTHER
            }
        }
    }
}
