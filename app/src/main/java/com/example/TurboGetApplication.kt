package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.service.DownloadManagerCore

class TurboGetApplication : Application() {

    lateinit var downloadManager: DownloadManagerCore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
        downloadManager = DownloadManagerCore(this)
        downloadManager.init()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_DOWNLOAD_ID,
                "Unduhan Aktif TurboGet",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menampilkan progress unduhan file yang sedang berjalan"
                setShowBadge(false)
            }

            val completedChannel = NotificationChannel(
                CHANNEL_COMPLETED_ID,
                "Unduhan Selesai",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pemberitahuan saat unduhan selesai atau gagal"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(completedChannel)
        }
    }

    companion object {
        const val CHANNEL_DOWNLOAD_ID = "turboget_active_downloads"
        const val CHANNEL_COMPLETED_ID = "turboget_completed_downloads"

        lateinit var instance: TurboGetApplication
            private set
    }
}
