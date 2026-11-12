package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.TurboGetApplication
import com.example.util.FormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var monitorJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val manager = (applicationContext as TurboGetApplication).downloadManager

        when (action) {
            ACTION_STOP_SERVICE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE_ALL -> {
                manager.pauseAll()
            }
            ACTION_RESUME_ALL -> {
                manager.resumeAll()
            }
        }

        startForegroundServiceNotification()
        startProgressMonitoring()
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val notification = buildNotification("TurboGet siap", "Menyiapkan unduhan...", 0, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startProgressMonitoring() {
        monitorJob?.cancel()
        val manager = (applicationContext as TurboGetApplication).downloadManager

        monitorJob = serviceScope.launch {
            while (isActive) {
                val activeDownloads = manager.getActiveDownloadsSnapshot()
                val totalSpeed = manager.totalSpeedFlow.value

                if (activeDownloads.isEmpty()) {
                    // Check if anything is queued
                    val queued = manager.getQueuedCount()
                    if (queued == 0) {
                        // All finished or paused
                        delay(2000)
                        if (manager.getActiveDownloadsSnapshot().isEmpty()) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                            break
                        }
                    }
                } else {
                    val primary = activeDownloads.first()
                    val title = if (activeDownloads.size == 1) {
                        primary.fileName
                    } else {
                        "Mengunduh ${activeDownloads.size} file (${FormatUtils.formatSpeed(totalSpeed)})"
                    }
                    val content = if (primary.totalBytes > 0) {
                        "${FormatUtils.formatBytes(primary.downloadedBytes)} / ${FormatUtils.formatBytes(primary.totalBytes)} • ${FormatUtils.formatSpeed(totalSpeed)}"
                    } else {
                        "${FormatUtils.formatBytes(primary.downloadedBytes)} • ${FormatUtils.formatSpeed(totalSpeed)}"
                    }
                    val progressPercent = if (primary.totalBytes > 0) {
                        ((primary.downloadedBytes * 100) / primary.totalBytes).toInt()
                    } else 0

                    val updatedNotif = buildNotification(title, content, progressPercent, activeDownloads.size)
                    val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    notifManager.notify(NOTIFICATION_ID, updatedNotif)
                }

                delay(1000)
            }
        }
    }

    private fun buildNotification(title: String, content: String, progress: Int, activeCount: Int): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseAllIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_PAUSE_ALL
        }
        val pendingPause = PendingIntent.getService(
            this,
            1,
            pauseAllIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, TurboGetApplication.CHANNEL_DOWNLOAD_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (progress > 0) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(100, 0, true)
        }

        if (activeCount > 0) {
            builder.addAction(android.R.drawable.ic_media_pause, "Jeda Semua", pendingPause)
        }

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        monitorJob?.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 9001
        const val ACTION_START_DOWNLOADS = "com.example.turboget.START_DOWNLOADS"
        const val ACTION_PAUSE_ALL = "com.example.turboget.PAUSE_ALL"
        const val ACTION_RESUME_ALL = "com.example.turboget.RESUME_ALL"
        const val ACTION_STOP_SERVICE = "com.example.turboget.STOP_SERVICE"

        fun startService(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOADS
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
