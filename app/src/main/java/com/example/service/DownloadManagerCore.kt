package com.example.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import com.example.data.engine.HttpDownloadEngine
import com.example.data.engine.ProbeResult
import com.example.data.local.AppDatabase
import com.example.data.model.DownloadCategory
import com.example.data.model.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.SegmentInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

data class LiveDownloadState(
    val id: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
    val etaSeconds: Long,
    val status: DownloadStatus,
    val segments: List<SegmentInfo> = emptyList()
)

data class AutoSchedulerSettings(
    val enabled: Boolean = false,
    val startHour: Int = 1,
    val startMinute: Int = 0,
    val stopHour: Int = 5,
    val stopMinute: Int = 0,
    val wifiOnly: Boolean = false,
    val maxConcurrent: Int = 2
)

class DownloadManagerCore(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.downloadDao()
    private val engine = HttpDownloadEngine()
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val pauseFlags = ConcurrentHashMap<String, AtomicBoolean>()
    private val cancelFlags = ConcurrentHashMap<String, AtomicBoolean>()

    // In-memory live states for ultra-responsive UI updates
    private val _liveStates = MutableStateFlow<Map<String, LiveDownloadState>>(emptyMap())
    val liveStates: StateFlow<Map<String, LiveDownloadState>> = _liveStates.asStateFlow()

    private val _totalSpeedFlow = MutableStateFlow(0L)
    val totalSpeedFlow: StateFlow<Long> = _totalSpeedFlow.asStateFlow()

    private val _activeCountFlow = MutableStateFlow(0)
    val activeCountFlow: StateFlow<Int> = _activeCountFlow.asStateFlow()

    private val _schedulerSettings = MutableStateFlow(AutoSchedulerSettings())
    val schedulerSettings: StateFlow<AutoSchedulerSettings> = _schedulerSettings.asStateFlow()

    val allDownloadsFlow: Flow<List<DownloadEntity>> = dao.getAllDownloads()

    fun init() {
        // Observe and update total speed
        scope.launch {
            while (isActive) {
                var totalSpeed = 0L
                var count = 0
                _liveStates.value.values.forEach { state ->
                    if (state.status == DownloadStatus.DOWNLOADING) {
                        totalSpeed += state.speedBytesPerSec
                        count++
                    }
                }
                _totalSpeedFlow.value = totalSpeed
                _activeCountFlow.value = count
                delay(500)
            }
        }

        // Start scheduler loop
        startSchedulerLoop()
    }

    suspend fun probe(url: String): Result<ProbeResult> {
        return engine.probeUrl(url)
    }

    fun updateSchedulerSettings(settings: AutoSchedulerSettings) {
        _schedulerSettings.value = settings
    }

    private fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun startSchedulerLoop() {
        scope.launch {
            while (isActive) {
                try {
                    val settings = _schedulerSettings.value
                    val cal = Calendar.getInstance()
                    val currentHour = cal.get(Calendar.HOUR_OF_DAY)
                    val currentMinute = cal.get(Calendar.MINUTE)
                    val currentTotalMinutes = currentHour * 60 + currentMinute

                    val scheduledStartMinutes = settings.startHour * 60 + settings.startMinute
                    val scheduledStopMinutes = settings.stopHour * 60 + settings.stopMinute

                    val isInsideWindow = if (scheduledStartMinutes <= scheduledStopMinutes) {
                        currentTotalMinutes in scheduledStartMinutes until scheduledStopMinutes
                    } else {
                        // overnight window, e.g. 23:00 to 05:00
                        currentTotalMinutes >= scheduledStartMinutes || currentTotalMinutes < scheduledStopMinutes
                    }

                    // Check master scheduler
                    if (settings.enabled) {
                        val wifiOk = !settings.wifiOnly || isWifiConnected()
                        if (isInsideWindow && wifiOk) {
                            // Find scheduled or queued downloads and start them
                            val scheduledList = dao.getScheduledDownloads()
                            for (item in scheduledList) {
                                if (activeJobs.size < settings.maxConcurrent) {
                                    startDownloadInternal(item.id)
                                }
                            }
                        } else if (!isInsideWindow && activeJobs.isNotEmpty()) {
                            // Pause active scheduled downloads when window ends
                            pauseAll()
                        }
                    }

                    // Also check individual scheduled tasks
                    val nowMillis = System.currentTimeMillis()
                    val individualScheduled = dao.getScheduledDownloads()
                    for (item in individualScheduled) {
                        val scheduledTime = item.scheduledTimeMillis
                        if (scheduledTime != null && nowMillis >= scheduledTime) {
                            val wifiOk = !item.wifiOnly || isWifiConnected()
                            if (wifiOk) {
                                dao.updateStatus(item.id, DownloadStatus.QUEUED)
                                checkQueueAndStart()
                            }
                        }
                    }

                } catch (_: Exception) {}
                delay(10_000) // check every 10 seconds
            }
        }
    }

    suspend fun addDownload(
        url: String,
        fileName: String,
        totalBytes: Long,
        supportsResume: Boolean,
        mimeType: String?,
        category: DownloadCategory,
        numConnections: Int = 4,
        scheduledTimeMillis: Long? = null,
        scheduledStopMillis: Long? = null,
        wifiOnly: Boolean = false,
        startImmediately: Boolean = true
    ): String {
        val id = UUID.randomUUID().toString()
        val downloadDir = getDownloadDirectory()
        val destFile = File(downloadDir, sanitizeFileName(fileName))

        val status = when {
            scheduledTimeMillis != null && scheduledTimeMillis > System.currentTimeMillis() -> DownloadStatus.SCHEDULED
            startImmediately -> DownloadStatus.QUEUED
            else -> DownloadStatus.PAUSED
        }

        val entity = DownloadEntity(
            id = id,
            url = url,
            fileName = destFile.name,
            destinationPath = destFile.absolutePath,
            totalBytes = totalBytes,
            downloadedBytes = 0L,
            status = status,
            supportsResume = supportsResume,
            numConnections = numConnections,
            category = category,
            mimeType = mimeType,
            scheduledTimeMillis = scheduledTimeMillis,
            scheduledStopMillis = scheduledStopMillis,
            wifiOnly = wifiOnly,
            createdAt = System.currentTimeMillis()
        )

        dao.insertDownload(entity)

        if (status == DownloadStatus.QUEUED) {
            checkQueueAndStart()
        }

        return id
    }

    fun resumeDownload(id: String) {
        scope.launch {
            val download = dao.getDownloadById(id) ?: return@launch
            if (download.status == DownloadStatus.DOWNLOADING || download.status == DownloadStatus.CONNECTING) {
                return@launch
            }
            dao.updateStatus(id, DownloadStatus.QUEUED)
            checkQueueAndStart()
        }
    }

    fun pauseDownload(id: String) {
        pauseFlags[id]?.set(true)
        scope.launch {
            activeJobs[id]?.cancel()
            activeJobs.remove(id)
            dao.updateStatus(id, DownloadStatus.PAUSED)
            updateLiveState(id) { it.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L, etaSeconds = 0L) }
            checkQueueAndStart()
        }
    }

    fun pauseAll() {
        pauseFlags.forEach { (_, flag) -> flag.set(true) }
        activeJobs.forEach { (id, job) ->
            job.cancel()
            scope.launch {
                dao.updateStatus(id, DownloadStatus.PAUSED)
                updateLiveState(id) { it.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L, etaSeconds = 0L) }
            }
        }
        activeJobs.clear()
    }

    fun resumeAll() {
        scope.launch {
            val all = dao.getPendingOrActiveDownloads()
            for (item in all) {
                if (item.status == DownloadStatus.PAUSED || item.status == DownloadStatus.QUEUED) {
                    dao.updateStatus(item.id, DownloadStatus.QUEUED)
                }
            }
            checkQueueAndStart()
        }
    }

    fun restartDownload(id: String) {
        scope.launch {
            pauseDownload(id)
            delay(200)
            val download = dao.getDownloadById(id) ?: return@launch
            val file = File(download.destinationPath)
            if (file.exists()) {
                file.delete()
            }
            dao.updateProgress(id, 0L, download.totalBytes, 0L, 0L, DownloadStatus.QUEUED)
            updateLiveState(id) {
                it.copy(downloadedBytes = 0L, speedBytesPerSec = 0L, etaSeconds = 0L, status = DownloadStatus.QUEUED)
            }
            checkQueueAndStart()
        }
    }

    fun deleteDownload(id: String, deleteFile: Boolean = false) {
        scope.launch {
            cancelFlags[id]?.set(true)
            activeJobs[id]?.cancel()
            activeJobs.remove(id)
            val download = dao.getDownloadById(id)
            if (download != null && deleteFile) {
                val file = File(download.destinationPath)
                if (file.exists()) file.delete()
            }
            dao.deleteById(id)
            val updated = _liveStates.value.toMutableMap()
            updated.remove(id)
            _liveStates.value = updated
            checkQueueAndStart()
        }
    }

    fun clearCompleted() {
        scope.launch {
            dao.clearCompleted()
        }
    }

    private fun checkQueueAndStart() {
        scope.launch {
            val settings = _schedulerSettings.value
            val maxConcurrent = settings.maxConcurrent
            val runningCount = activeJobs.size

            if (runningCount < maxConcurrent) {
                val pending = dao.getPendingOrActiveDownloads()
                for (item in pending) {
                    if (item.status == DownloadStatus.QUEUED && !activeJobs.containsKey(item.id)) {
                        // Check Wi-Fi restriction
                        if (item.wifiOnly && !isWifiConnected()) {
                            continue
                        }
                        startDownloadInternal(item.id)
                        if (activeJobs.size >= maxConcurrent) break
                    }
                }
            }
        }
    }

    private fun startDownloadInternal(id: String) {
        if (activeJobs.containsKey(id)) return

        val pauseFlag = AtomicBoolean(false)
        val cancelFlag = AtomicBoolean(false)
        pauseFlags[id] = pauseFlag
        cancelFlags[id] = cancelFlag

        // Trigger foreground service
        DownloadService.startService(context)

        val job = scope.launch {
            val entity = dao.getDownloadById(id) ?: return@launch
            dao.updateStatus(id, DownloadStatus.CONNECTING)

            updateLiveState(id) {
                LiveDownloadState(
                    id = id,
                    downloadedBytes = entity.downloadedBytes,
                    totalBytes = entity.totalBytes,
                    speedBytesPerSec = 0L,
                    etaSeconds = 0L,
                    status = DownloadStatus.CONNECTING
                )
            }

            var downloadEntity = entity
            // If totalBytes unknown or resume unsupported, probe first
            if (downloadEntity.totalBytes <= 0 || !downloadEntity.supportsResume) {
                val probeRes = engine.probeUrl(downloadEntity.url).getOrNull()
                if (probeRes != null) {
                    downloadEntity = downloadEntity.copy(
                        totalBytes = if (probeRes.contentLength > 0) probeRes.contentLength else downloadEntity.totalBytes,
                        supportsResume = probeRes.supportsResume,
                        mimeType = probeRes.mimeType ?: downloadEntity.mimeType
                    )
                    dao.updateDownload(downloadEntity)
                }
            }

            dao.updateStatus(id, DownloadStatus.DOWNLOADING)

            var lastDbSaveTime = System.currentTimeMillis()

            engine.executeDownload(
                download = downloadEntity,
                isPaused = { pauseFlag.get() },
                isCancelled = { cancelFlag.get() },
                onProgress = { downloaded, total, speed, eta, segments ->
                    updateLiveState(id) {
                        LiveDownloadState(
                            id = id,
                            downloadedBytes = downloaded,
                            totalBytes = total,
                            speedBytesPerSec = speed,
                            etaSeconds = eta,
                            status = DownloadStatus.DOWNLOADING,
                            segments = segments
                        )
                    }

                    val now = System.currentTimeMillis()
                    if (now - lastDbSaveTime > 2500) {
                        dao.updateProgress(id, downloaded, total, speed, eta, DownloadStatus.DOWNLOADING)
                        lastDbSaveTime = now
                    }
                },
                onSuccess = { file ->
                    val total = file.length()
                    dao.updateProgress(id, total, total, 0L, 0L, DownloadStatus.COMPLETED)
                    dao.updateStatus(id, DownloadStatus.COMPLETED)
                    updateLiveState(id) {
                        it.copy(
                            downloadedBytes = total,
                            totalBytes = total,
                            speedBytesPerSec = 0L,
                            etaSeconds = 0L,
                            status = DownloadStatus.COMPLETED
                        )
                    }
                    activeJobs.remove(id)
                    checkQueueAndStart()
                },
                onError = { error ->
                    dao.updateStatus(id, DownloadStatus.FAILED, error)
                    updateLiveState(id) {
                        it.copy(speedBytesPerSec = 0L, etaSeconds = 0L, status = DownloadStatus.FAILED)
                    }
                    activeJobs.remove(id)
                    checkQueueAndStart()
                }
            )
        }

        activeJobs[id] = job
    }

    private inline fun updateLiveState(id: String, transform: (LiveDownloadState) -> LiveDownloadState) {
        val current = _liveStates.value.toMutableMap()
        val prev = current[id] ?: LiveDownloadState(id, 0L, -1L, 0L, 0L, DownloadStatus.QUEUED)
        current[id] = transform(prev)
        _liveStates.value = current
    }

    suspend fun getActiveDownloadsSnapshot(): List<DownloadEntity> {
        val activeIds = activeJobs.keys().toList()
        return activeIds.mapNotNull { dao.getDownloadById(it) }
    }

    suspend fun getQueuedCount(): Int {
        return dao.getPendingOrActiveDownloads().size
    }

    private fun getDownloadDirectory(): File {
        val external = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (external != null && external.exists()) return external
        val internal = File(context.filesDir, "Downloads")
        if (!internal.exists()) internal.mkdirs()
        return internal
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }
}
