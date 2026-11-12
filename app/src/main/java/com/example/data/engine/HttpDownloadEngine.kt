package com.example.data.engine

import com.example.data.model.DownloadEntity
import com.example.data.model.SegmentInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

data class ProbeResult(
    val fileName: String,
    val contentLength: Long,
    val supportsResume: Boolean,
    val mimeType: String?,
    val finalUrl: String
)

class HttpDownloadEngine(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    suspend fun probeUrl(url: String): Result<ProbeResult> = withContext(Dispatchers.IO) {
        try {
            // First try HEAD
            val headReq = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Android; TurboGet IDM Engine)")
                .build()

            var response: Response? = null
            try {
                response = client.newCall(headReq).execute()
            } catch (_: Exception) {
                // If HEAD fails, fallback to GET with Range: bytes=0-0
            }

            if (response == null || !response.isSuccessful || response.code == 405) {
                val getProbeReq = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android; TurboGet IDM Engine)")
                    .header("Range", "bytes=0-0")
                    .get()
                    .build()
                response = client.newCall(getProbeReq).execute()
            }

            val finalUrl = response.request.url.toString()
            val headers = response.headers
            val contentDisposition = headers["Content-Disposition"]
            val contentType = headers["Content-Type"]
            val acceptRanges = headers["Accept-Ranges"]
            val contentRange = headers["Content-Range"]

            var contentLength = headers["Content-Length"]?.toLongOrNull() ?: -1L
            if (contentLength <= 0 && contentRange != null) {
                // e.g. bytes 0-0/1234567
                val totalStr = contentRange.substringAfterLast('/', "")
                contentLength = totalStr.toLongOrNull() ?: -1L
            }

            val supportsResume = (acceptRanges.equals("bytes", ignoreCase = true) ||
                    response.code == 206 ||
                    (contentRange != null && contentRange.contains("bytes")))

            var fileName = extractFileName(contentDisposition, finalUrl)
            if (fileName.isBlank()) {
                fileName = "turboget_file_${System.currentTimeMillis()}.bin"
            }

            response.close()

            Result.success(
                ProbeResult(
                    fileName = fileName,
                    contentLength = contentLength,
                    supportsResume = supportsResume,
                    mimeType = contentType,
                    finalUrl = finalUrl
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractFileName(contentDisposition: String?, url: String): String {
        if (!contentDisposition.isNullOrBlank()) {
            val cd = contentDisposition
            // Check for filename*=UTF-8''filename.ext
            if (cd.contains("filename*=", ignoreCase = true)) {
                val match = Regex("""filename\*=UTF-8''([^;]+)""", RegexOption.IGNORE_CASE).find(cd)
                if (match != null) {
                    return try {
                        URLDecoder.decode(match.groupValues[1].trim('"', '\''), "UTF-8")
                    } catch (_: Exception) {
                        match.groupValues[1].trim('"', '\'')
                    }
                }
            }
            // Check standard filename="filename.ext"
            val match = Regex("""filename="?([^";]+)"?""", RegexOption.IGNORE_CASE).find(cd)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }

        // Fallback to URL path
        try {
            val uri = java.net.URI(url)
            val path = uri.path
            if (!path.isNullOrBlank()) {
                val rawName = path.substringAfterLast('/')
                if (rawName.isNotBlank() && rawName.contains('.')) {
                    return URLDecoder.decode(rawName, "UTF-8")
                }
            }
        } catch (_: Exception) {}

        return "download_${System.currentTimeMillis()}"
    }

    suspend fun executeDownload(
        download: DownloadEntity,
        isPaused: () -> Boolean,
        isCancelled: () -> Boolean,
        onProgress: suspend (downloaded: Long, total: Long, speed: Long, eta: Long, segments: List<SegmentInfo>) -> Unit,
        onSuccess: suspend (file: File) -> Unit,
        onError: suspend (errorMsg: String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val destFile = File(download.destinationPath)
        destFile.parentFile?.mkdirs()

        val totalBytes = download.totalBytes
        var downloadedSoFar = if (destFile.exists()) destFile.length() else 0L

        // If resume is not supported, start fresh
        if (!download.supportsResume && downloadedSoFar > 0) {
            destFile.delete()
            downloadedSoFar = 0L
        }

        val requestBuilder = Request.Builder()
            .url(download.url)
            .header("User-Agent", "Mozilla/5.0 (Android; TurboGet IDM Engine)")

        if (download.supportsResume && downloadedSoFar > 0) {
            requestBuilder.header("Range", "bytes=$downloadedSoFar-")
        }

        val request = requestBuilder.build()
        var response: Response? = null

        try {
            response = client.newCall(request).execute()
            if (!response.isSuccessful && response.code != 206) {
                onError("Server merespon error: HTTP ${response.code}")
                return@withContext
            }

            val body = response.body
            if (body == null) {
                onError("Response body kosong")
                return@withContext
            }

            var calculatedTotal = totalBytes
            if (calculatedTotal <= 0) {
                val lengthFromHeader = body.contentLength()
                if (lengthFromHeader > 0) {
                    calculatedTotal = if (downloadedSoFar > 0) downloadedSoFar + lengthFromHeader else lengthFromHeader
                }
            }

            val isPartial = (response.code == 206)
            val raf = RandomAccessFile(destFile, "rw")
            val seekOffset = if (isPartial) downloadedSoFar else 0L
            raf.seek(seekOffset)

            val inputStream: InputStream = body.byteStream()
            val buffer = ByteArray(32 * 1024) // 32KB buffer for high performance
            var bytesRead: Int
            var currentDownloaded = seekOffset

            // Live speed metrics
            var lastSampleTime = System.currentTimeMillis()
            var lastSampleBytes = currentDownloaded
            var currentSpeed = 0L
            var currentEta = 0L

            // Segments representation for UI
            val numSegments = if (download.numConnections in 1..8) download.numConnections else 4
            val segmentList = mutableListOf<SegmentInfo>()
            if (calculatedTotal > 0) {
                val chunkSize = calculatedTotal / numSegments
                for (i in 0 until numSegments) {
                    val start = i * chunkSize
                    val end = if (i == numSegments - 1) calculatedTotal - 1 else (start + chunkSize - 1)
                    val segDownloaded = when {
                        currentDownloaded >= end -> (end - start + 1)
                        currentDownloaded > start -> (currentDownloaded - start)
                        else -> 0L
                    }
                    segmentList.add(
                        SegmentInfo(
                            index = i,
                            startByte = start,
                            endByte = end,
                            downloadedBytes = segDownloaded,
                            status = if (segDownloaded > 0) "ACTIVE" else "PENDING"
                        )
                    )
                }
            }

            try {
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isCancelled()) {
                        raf.close()
                        inputStream.close()
                        return@withContext
                    }
                    if (isPaused()) {
                        raf.close()
                        inputStream.close()
                        // Save current progress before returning
                        onProgress(currentDownloaded, calculatedTotal, 0L, 0L, segmentList)
                        return@withContext
                    }

                    raf.write(buffer, 0, bytesRead)
                    currentDownloaded += bytesRead

                    val now = System.currentTimeMillis()
                    val timeDiff = now - lastSampleTime
                    if (timeDiff >= 400) { // update progress every 400ms
                        val bytesDiff = currentDownloaded - lastSampleBytes
                        currentSpeed = if (timeDiff > 0) (bytesDiff * 1000L) / timeDiff else 0L
                        currentEta = if (calculatedTotal > currentDownloaded && currentSpeed > 0) {
                            (calculatedTotal - currentDownloaded) / currentSpeed
                        } else 0L

                        // Update segment models
                        if (calculatedTotal > 0) {
                            val chunkSize = calculatedTotal / numSegments
                            for (i in 0 until numSegments) {
                                val start = i * chunkSize
                                val end = if (i == numSegments - 1) calculatedTotal - 1 else (start + chunkSize - 1)
                                val segDownloaded = when {
                                    currentDownloaded >= end -> (end - start + 1)
                                    currentDownloaded > start -> (currentDownloaded - start)
                                    else -> 0L
                                }
                                val seg = segmentList.getOrNull(i)
                                if (seg != null) {
                                    seg.downloadedBytes = segDownloaded
                                    seg.status = if (segDownloaded >= (end - start + 1)) "DONE" else "ACTIVE"
                                }
                            }
                        }

                        onProgress(currentDownloaded, calculatedTotal, currentSpeed, currentEta, segmentList)
                        lastSampleTime = now
                        lastSampleBytes = currentDownloaded
                    }
                }

                raf.close()
                inputStream.close()

                // Final check: completed successfully
                onProgress(currentDownloaded, currentDownloaded, 0L, 0L, segmentList)
                onSuccess(destFile)

            } catch (e: Exception) {
                raf.close()
                try { inputStream.close() } catch (_: Exception) {}
                if (isPaused() || isCancelled()) {
                    return@withContext
                }
                onError("Koneksi terputus: ${e.localizedMessage ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            onError("Gagal mengunduh: ${e.localizedMessage ?: "Network error"}")
        } finally {
            response?.close()
        }
    }
}
