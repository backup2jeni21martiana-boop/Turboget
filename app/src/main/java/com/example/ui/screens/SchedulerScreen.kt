package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadStatus
import com.example.service.AutoSchedulerSettings
import com.example.ui.TurboGetViewModel
import com.example.ui.components.CategoryBadge
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SpeedGreen
import com.example.util.FormatUtils

@Composable
fun SchedulerScreen(
    viewModel: TurboGetViewModel,
    modifier: Modifier = Modifier
) {
    val schedulerSettings by viewModel.schedulerSettings.collectAsState()
    val allDownloads by viewModel.rawDownloads.collectAsState()
    val scheduledDownloads = allDownloads.filter { it.status == DownloadStatus.SCHEDULED }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("scheduler_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Scheduler Status Card
        item(key = "master_scheduler_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scheduler_master_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (schedulerSettings.enabled) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (schedulerSettings.enabled) SpeedGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (schedulerSettings.enabled) Icons.Default.AlarmOn else Icons.Default.Alarm,
                                        contentDescription = null,
                                        tint = if (schedulerSettings.enabled) SpeedGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Penjadwalan Otomatis",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (schedulerSettings.enabled) "Status: AKTIF" else "Status: NONAKTIF",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (schedulerSettings.enabled) SpeedGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        Switch(
                            checked = schedulerSettings.enabled,
                            onCheckedChange = { isEnabled ->
                                viewModel.updateScheduler(schedulerSettings.copy(enabled = isEnabled))
                            },
                            modifier = Modifier.testTag("master_scheduler_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val startFormatted = String.format("%02d:%02d", schedulerSettings.startHour, schedulerSettings.startMinute)
                    val stopFormatted = String.format("%02d:%02d", schedulerSettings.stopHour, schedulerSettings.stopMinute)

                    Text(
                        text = if (schedulerSettings.enabled) {
                            "Unduhan otomatis dimulai pukul $startFormatted WIB dan dijeda pukul $stopFormatted WIB."
                        } else {
                            "Aktifkan agar TurboGet otomatis mengunduh file besar saat jam malam tanpa Anda harus terjaga."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        // Preset Time Schedules
        item(key = "schedule_presets") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Pilihan Jadwal Cepat (Paket Malam / Off-Peak):",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SchedulePresetCard(
                        title = "Jam Malam",
                        timeRange = "01:00 - 05:00",
                        description = "Hemat Kuota Malam",
                        isSelected = (schedulerSettings.startHour == 1 && schedulerSettings.stopHour == 5),
                        onClick = {
                            viewModel.updateScheduler(
                                schedulerSettings.copy(
                                    enabled = true,
                                    startHour = 1,
                                    startMinute = 0,
                                    stopHour = 5,
                                    stopMinute = 0
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )

                    SchedulePresetCard(
                        title = "Dini Hari",
                        timeRange = "02:00 - 06:00",
                        description = "Kecepatan Maksimal",
                        isSelected = (schedulerSettings.startHour == 2 && schedulerSettings.stopHour == 6),
                        onClick = {
                            viewModel.updateScheduler(
                                schedulerSettings.copy(
                                    enabled = true,
                                    startHour = 2,
                                    startMinute = 0,
                                    stopHour = 6,
                                    stopMinute = 0
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SchedulePresetCard(
                        title = "Subuh",
                        timeRange = "04:00 - 07:00",
                        description = "Sebelum Jam Kantor",
                        isSelected = (schedulerSettings.startHour == 4 && schedulerSettings.stopHour == 7),
                        onClick = {
                            viewModel.updateScheduler(
                                schedulerSettings.copy(
                                    enabled = true,
                                    startHour = 4,
                                    startMinute = 0,
                                    stopHour = 7,
                                    stopMinute = 0
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )

                    SchedulePresetCard(
                        title = "Tengah Hari",
                        timeRange = "12:00 - 13:00",
                        description = "Jam Istirahat",
                        isSelected = (schedulerSettings.startHour == 12 && schedulerSettings.stopHour == 13),
                        onClick = {
                            viewModel.updateScheduler(
                                schedulerSettings.copy(
                                    enabled = true,
                                    startHour = 12,
                                    startMinute = 0,
                                    stopHour = 13,
                                    stopMinute = 0
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Scheduler Constraints & Parameters
        item(key = "scheduler_parameters") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pengaturan Lanjutan Penjadwal",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Wi-Fi Constraint
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, null, tint = SkyBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Hanya Mulai Saat Wi-Fi Tersambung", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("Otomatis tunggu hingga terhubung Wi-Fi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = schedulerSettings.wifiOnly,
                            onCheckedChange = { isChecked ->
                                viewModel.updateScheduler(schedulerSettings.copy(wifiOnly = isChecked))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Max Concurrent Downloads
                    Text(
                        text = "Maksimal Unduhan Bersamaan:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1 to "1 File (Stabil)", 2 to "2 File", 3 to "3 File").forEach { (count, label) ->
                            FilterChip(
                                selected = (schedulerSettings.maxConcurrent == count),
                                onClick = {
                                    viewModel.updateScheduler(schedulerSettings.copy(maxConcurrent = count))
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Section: Scheduled Items Queue
        item(key = "scheduled_items_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Daftar File Menunggu Jadwal (${scheduledDownloads.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        if (scheduledDownloads.isEmpty()) {
            item(key = "empty_scheduled") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada file yang dijadwalkan.\nSaat menambah link baru, aktifkan opsi 'Jadwalkan Otomatis'.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )
                    }
                }
            }
        } else {
            items(
                items = scheduledDownloads,
                key = { it.id }
            ) { item ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryBadge(category = item.category)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.fileName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            val schedText = if (item.scheduledTimeMillis != null) {
                                FormatUtils.formatDateTime(item.scheduledTimeMillis)
                            } else "Mengikuti Master Jadwal"
                            Text(
                                text = "Jadwal: $schedText",
                                fontSize = 11.sp,
                                color = NeonAmber
                            )
                        }
                        FilledTonalButton(
                            onClick = { viewModel.resume(item.id) },
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unduh Sekarang", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SchedulePresetCard(
    title: String,
    timeRange: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag("preset_$title"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                ElectricCyan.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, ElectricCyan) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timeRange,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
