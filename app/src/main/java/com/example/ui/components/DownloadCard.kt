package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadCategory
import com.example.data.model.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.service.LiveDownloadState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SpeedGreen
import com.example.util.FormatUtils

@Composable
fun DownloadCard(
    download: DownloadEntity,
    liveState: LiveDownloadState?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onDelete: (deleteFile: Boolean) -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onShowDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    // Merge entity with live state
    val status = liveState?.status ?: download.status
    val downloadedBytes = liveState?.downloadedBytes ?: download.downloadedBytes
    val totalBytes = if ((liveState?.totalBytes ?: -1L) > 0) liveState!!.totalBytes else download.totalBytes
    val speed = liveState?.speedBytesPerSec ?: download.speedBytesPerSec
    val eta = liveState?.etaSeconds ?: download.etaSeconds
    val segments = liveState?.segments ?: emptyList()

    val progress = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    val isDownloading = (status == DownloadStatus.DOWNLOADING || status == DownloadStatus.CONNECTING)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onShowDetail() }
            .testTag("download_card_${download.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Row 1: Category Icon, Filename, Status Badge, More Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon
                CategoryBadge(category = download.category)

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.fileName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    StatusBadge(
                        status = status,
                        speed = speed,
                        scheduledTimeMillis = download.scheduledTimeMillis
                    )
                }

                // More Menu Button
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp).testTag("download_card_menu_${download.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opsi Lainnya",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Detail Unduhan (IDM)") },
                            leadingIcon = { Icon(Icons.Default.Info, null) },
                            onClick = {
                                showMenu = false
                                onShowDetail()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Salin Link URL") },
                            leadingIcon = { Icon(Icons.Default.Share, null) },
                            onClick = {
                                showMenu = false
                                onCopyLink()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Unduh Ulang dari Awal") },
                            leadingIcon = { Icon(Icons.Default.Refresh, null) },
                            onClick = {
                                showMenu = false
                                onRestart()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus Task & File", color = ErrorRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = ErrorRed) },
                            onClick = {
                                showMenu = false
                                onDelete(true)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar (with Segmented parts!)
            SegmentedProgressBar(
                progress = progress,
                segments = segments,
                isDownloading = isDownloading
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Progress text and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Size & ETA metrics
                Column {
                    val percentStr = (progress * 100).toInt()
                    Text(
                        text = if (totalBytes > 0) {
                            "${FormatUtils.formatBytes(downloadedBytes)} / ${FormatUtils.formatBytes(totalBytes)} ($percentStr%)"
                        } else {
                            FormatUtils.formatBytes(downloadedBytes)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    if (isDownloading && eta > 0) {
                        Text(
                            text = "Tersisa: ${FormatUtils.formatEta(eta)} • ${download.numConnections} Parts",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    } else if (download.supportsResume) {
                        Text(
                            text = "Mendukung Resume",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SpeedGreen,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                // Action buttons based on status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (status) {
                        DownloadStatus.DOWNLOADING, DownloadStatus.CONNECTING -> {
                            FilledTonalButton(
                                onClick = onPause,
                                modifier = Modifier.height(34.dp).testTag("pause_button_${download.id}")
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = "Jeda", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Jeda", fontSize = 12.sp)
                            }
                        }
                        DownloadStatus.PAUSED, DownloadStatus.QUEUED -> {
                            FilledTonalButton(
                                onClick = onResume,
                                modifier = Modifier.height(34.dp).testTag("resume_button_${download.id}")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Lanjutkan", modifier = Modifier.size(16.dp), tint = SpeedGreen)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Lanjutkan", fontSize = 12.sp, color = SpeedGreen)
                            }
                        }
                        DownloadStatus.COMPLETED -> {
                            FilledTonalButton(
                                onClick = onOpen,
                                modifier = Modifier.height(34.dp).testTag("open_button_${download.id}")
                            ) {
                                Text("Buka File", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Share, contentDescription = "Bagikan", modifier = Modifier.size(16.dp))
                            }
                        }
                        DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                            FilledTonalButton(
                                onClick = onRestart,
                                modifier = Modifier.height(34.dp).testTag("retry_button_${download.id}")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Coba Lagi", modifier = Modifier.size(16.dp), tint = ErrorRed)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Coba Lagi", fontSize = 12.sp, color = ErrorRed)
                            }
                        }
                        DownloadStatus.SCHEDULED -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NeonAmber.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp), tint = NeonAmber)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Menunggu Jadwal", fontSize = 11.sp, color = NeonAmber)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBadge(category: DownloadCategory) {
    val (icon: ImageVector, color: Color) = when (category) {
        DownloadCategory.VIDEO -> Icons.Default.VideoFile to SkyBlue
        DownloadCategory.AUDIO -> Icons.Default.AudioFile to Color(0xFFA855F7)
        DownloadCategory.DOCUMENT -> Icons.AutoMirrored.Filled.InsertDriveFile to NeonAmber
        DownloadCategory.ARCHIVE -> Icons.Default.FolderZip to SpeedGreen
        DownloadCategory.PROGRAM -> Icons.Default.Android to ElectricCyan
        else -> Icons.AutoMirrored.Filled.InsertDriveFile to Color(0xFF94A3B8)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = category.displayName,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun StatusBadge(
    status: DownloadStatus,
    speed: Long,
    scheduledTimeMillis: Long?
) {
    val (text, color) = when (status) {
        DownloadStatus.DOWNLOADING -> {
            val speedText = if (speed > 0) FormatUtils.formatSpeed(speed) else "Mengunduh..."
            "Mengunduh • $speedText" to ElectricCyan
        }
        DownloadStatus.CONNECTING -> "Menghubungkan..." to SkyBlue
        DownloadStatus.PAUSED -> "Dijeda" to NeonAmber
        DownloadStatus.QUEUED -> "Dalam Antrean" to Color(0xFF94A3B8)
        DownloadStatus.COMPLETED -> "Selesai" to SpeedGreen
        DownloadStatus.FAILED -> "Gagal" to ErrorRed
        DownloadStatus.SCHEDULED -> {
            val timeText = if (scheduledTimeMillis != null) FormatUtils.formatTime(scheduledTimeMillis) else ""
            "Terjadwal ($timeText)" to NeonAmber
        }
        DownloadStatus.CANCELLED -> "Dibatalkan" to Color(0xFF94A3B8)
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            color = color,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )
    )
}
