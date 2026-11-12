package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.TurboGetApplication
import com.example.data.engine.ProbeResult
import com.example.data.model.DownloadCategory
import com.example.data.model.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.service.AutoSchedulerSettings
import com.example.service.LiveDownloadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class StatusFilter(val label: String) {
    ALL("Semua"),
    ACTIVE("Mengunduh"),
    PAUSED("Dijeda"),
    SCHEDULED("Terjadwal"),
    COMPLETED("Selesai"),
    FAILED("Gagal")
}

sealed interface ProbeUiState {
    data object Idle : ProbeUiState
    data object Loading : ProbeUiState
    data class Success(val result: ProbeResult) : ProbeUiState
    data class Error(val message: String) : ProbeUiState
}

class TurboGetViewModel : ViewModel() {

    private val manager = TurboGetApplication.instance.downloadManager

    val liveStates: StateFlow<Map<String, LiveDownloadState>> = manager.liveStates
    val totalSpeed: StateFlow<Long> = manager.totalSpeedFlow
    val activeCount: StateFlow<Int> = manager.activeCountFlow
    val schedulerSettings: StateFlow<AutoSchedulerSettings> = manager.schedulerSettings

    private val _selectedCategory = MutableStateFlow(DownloadCategory.ALL)
    val selectedCategory: StateFlow<DownloadCategory> = _selectedCategory.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow(StatusFilter.ALL)
    val selectedStatusFilter: StateFlow<StatusFilter> = _selectedStatusFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _selectedDetailId = MutableStateFlow<String?>(null)
    val selectedDetailId: StateFlow<String?> = _selectedDetailId.asStateFlow()

    private val _probeState = MutableStateFlow<ProbeUiState>(ProbeUiState.Idle)
    val probeState: StateFlow<ProbeUiState> = _probeState.asStateFlow()

    val rawDownloads = manager.allDownloadsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Filtered and combined list
    val filteredDownloads: StateFlow<List<DownloadEntity>> = combine(
        rawDownloads,
        _selectedCategory,
        _selectedStatusFilter,
        _searchQuery
    ) { list, category, filter, query ->
        list.filter { item ->
            // Category filter
            val matchCategory = (category == DownloadCategory.ALL || item.category == category)

            // Status filter
            val matchStatus = when (filter) {
                StatusFilter.ALL -> true
                StatusFilter.ACTIVE -> item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.CONNECTING || item.status == DownloadStatus.QUEUED
                StatusFilter.PAUSED -> item.status == DownloadStatus.PAUSED
                StatusFilter.SCHEDULED -> item.status == DownloadStatus.SCHEDULED
                StatusFilter.COMPLETED -> item.status == DownloadStatus.COMPLETED
                StatusFilter.FAILED -> item.status == DownloadStatus.FAILED || item.status == DownloadStatus.CANCELLED
            }

            // Search query
            val matchQuery = query.isBlank() ||
                    item.fileName.contains(query, ignoreCase = true) ||
                    item.url.contains(query, ignoreCase = true)

            matchCategory && matchStatus && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCategory(category: DownloadCategory) {
        _selectedCategory.value = category
    }

    fun setStatusFilter(filter: StatusFilter) {
        _selectedStatusFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openAddDialog(initialUrl: String = "") {
        _showAddDialog.value = true
        _probeState.value = ProbeUiState.Idle
        if (initialUrl.isNotBlank()) {
            probeUrl(initialUrl)
        }
    }

    fun closeAddDialog() {
        _showAddDialog.value = false
        _probeState.value = ProbeUiState.Idle
    }

    fun selectDetail(id: String?) {
        _selectedDetailId.value = id
    }

    fun probeUrl(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _probeState.value = ProbeUiState.Loading
            val result = manager.probe(url.trim())
            result.onSuccess { probeRes ->
                _probeState.value = ProbeUiState.Success(probeRes)
            }.onFailure { err ->
                _probeState.value = ProbeUiState.Error(err.localizedMessage ?: "Gagal menganalisis URL")
            }
        }
    }

    fun addDownload(
        url: String,
        fileName: String,
        totalBytes: Long,
        supportsResume: Boolean,
        mimeType: String?,
        category: DownloadCategory,
        numConnections: Int,
        scheduledTimeMillis: Long?,
        scheduledStopMillis: Long?,
        wifiOnly: Boolean,
        startImmediately: Boolean
    ) {
        viewModelScope.launch {
            manager.addDownload(
                url = url.trim(),
                fileName = fileName.trim(),
                totalBytes = totalBytes,
                supportsResume = supportsResume,
                mimeType = mimeType,
                category = category,
                numConnections = numConnections,
                scheduledTimeMillis = scheduledTimeMillis,
                scheduledStopMillis = scheduledStopMillis,
                wifiOnly = wifiOnly,
                startImmediately = startImmediately
            )
            closeAddDialog()
        }
    }

    fun pause(id: String) = manager.pauseDownload(id)
    fun resume(id: String) = manager.resumeDownload(id)
    fun restart(id: String) = manager.restartDownload(id)
    fun delete(id: String, deleteFile: Boolean) = manager.deleteDownload(id, deleteFile)
    fun pauseAll() = manager.pauseAll()
    fun resumeAll() = manager.resumeAll()
    fun clearCompleted() = manager.clearCompleted()

    fun updateScheduler(settings: AutoSchedulerSettings) {
        manager.updateSchedulerSettings(settings)
    }

    fun copyLink(context: Context, url: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("URL Unduhan", url)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Link disalin ke clipboard", Toast.LENGTH_SHORT).show()
    }

    fun openFile(context: Context, download: DownloadEntity) {
        val file = File(download.destinationPath)
        if (!file.exists()) {
            Toast.makeText(context, "File tidak ditemukan pada penyimpanan", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val mimeType = download.mimeType ?: getMimeTypeFromExt(file.extension)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Buka file dengan"))
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak ada aplikasi untuk membuka file ini: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareFile(context: Context, download: DownloadEntity) {
        val file = File(download.destinationPath)
        if (!file.exists()) {
            Toast.makeText(context, "File tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val mimeType = download.mimeType ?: getMimeTypeFromExt(file.extension)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan file"))
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeTypeFromExt(extension: String): String {
        return when (extension.lowercase()) {
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "apk" -> "application/vnd.android.package-archive"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "txt" -> "text/plain"
            else -> "*/*"
        }
    }
}
