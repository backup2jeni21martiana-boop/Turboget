package com.example.ui.screens

import android.os.Environment
import android.os.StatFs
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadCategory
import com.example.data.model.DownloadStatus
import com.example.ui.TurboGetViewModel
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SpeedGreen
import com.example.util.FormatUtils

data class SampleDownloadItem(
    val title: String,
    val sizeText: String,
    val estimatedBytes: Long,
    val url: String,
    val fileName: String,
    val category: DownloadCategory,
    val icon: ImageVector,
    val desc: String
)

@Composable
fun PresetsAndToolsScreen(
    viewModel: TurboGetViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allDownloads by viewModel.rawDownloads.collectAsState()
    val completedCount = allDownloads.count { it.status == DownloadStatus.COMPLETED }
    var batchText by remember { mutableStateOf("") }

    val sampleFiles = remember {
        listOf(
            SampleDownloadItem(
                title = "Video Sample Big Buck Bunny",
                sizeText = "~30 MB (MP4)",
                estimatedBytes = 30_000_000L,
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                fileName = "BigBuckBunny_720p.mp4",
                category = DownloadCategory.VIDEO,
                icon = Icons.Default.VideoFile,
                desc = "Cocok untuk tes streaming dan pemutaran video langsung"
            ),
            SampleDownloadItem(
                title = "Berkas Uji Hetzner High Speed",
                sizeText = "10 MB (.bin)",
                estimatedBytes = 10_485_760L,
                url = "https://ash-speed.hetzner.com/10MB.bin",
                fileName = "Hetzner_Test_10MB.bin",
                category = DownloadCategory.ARCHIVE,
                icon = Icons.Default.FolderZip,
                desc = "Server mendukung multi-connection HTTP 206 Range resume"
            ),
            SampleDownloadItem(
                title = "Berkas Besar 100 MB Hetzner",
                sizeText = "100 MB (.bin)",
                estimatedBytes = 104_857_600L,
                url = "https://ash-speed.hetzner.com/100MB.bin",
                fileName = "LargeFile_100MB.bin",
                category = DownloadCategory.ARCHIVE,
                icon = Icons.Default.Speed,
                desc = "Uji kecepatan penuh IDM, jeda (pause) & lanjutkan (resume)"
            ),
            SampleDownloadItem(
                title = "Musik MP3 Bebas Lisensi",
                sizeText = "~6 MB (MP3)",
                estimatedBytes = 6_200_000L,
                url = "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/WFMU/Broke_For_Free/Directionless_EP/Broke_For_Free_-_01_-_Night_Owl.mp3",
                fileName = "Broke_For_Free_Night_Owl.mp3",
                category = DownloadCategory.AUDIO,
                icon = Icons.Default.AudioFile,
                desc = "Audio track MP3 stereo"
            )
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("presets_tools_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Uji Coba Unduhan Besar Langsung (1-Tap Presets)
        item(key = "sample_presets_header") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Sampel Uji Coba File Besar & Resume",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Klik satu tombol untuk langsung menguji fitur resume dan kecepatan multi-part:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )
            }
        }

        items(
            items = sampleFiles,
            key = { it.url }
        ) { sample ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sample_${sample.fileName}"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ElectricCyan.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(sample.icon, contentDescription = null, tint = ElectricCyan)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sample.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${sample.sizeText} • ${sample.desc}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledTonalButton(
                        onClick = {
                            viewModel.addDownload(
                                url = sample.url,
                                fileName = sample.fileName,
                                totalBytes = sample.estimatedBytes,
                                supportsResume = true,
                                mimeType = null,
                                category = sample.category,
                                numConnections = 4,
                                scheduledTimeMillis = null,
                                scheduledStopMillis = null,
                                wifiOnly = false,
                                startImmediately = true
                            )
                            Toast.makeText(context, "Mulai mengunduh ${sample.fileName}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Unduh", fontSize = 12.sp)
                    }
                }
            }
        }

        // Section: Batch Downloader (Unduh Sekaligus Banyak Link)
        item(key = "batch_downloader_section") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, null, tint = SkyBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Batch Downloader (Banyak Tautan Sekaligus)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tempel beberapa URL file (satu baris per link) untuk diunduh berurutan:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = batchText,
                        onValueChange = { batchText = it },
                        placeholder = { Text("https://contoh.com/file1.zip\nhttps://contoh.com/file2.mp4") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("batch_urls_input"),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val lines = batchText.lines().map { it.trim() }.filter { it.startsWith("http://") || it.startsWith("https://") }
                            if (lines.isEmpty()) {
                                Toast.makeText(context, "Tidak ada URL valid yang ditemukan", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            lines.forEach { lineUrl ->
                                val name = lineUrl.substringAfterLast('/').substringBefore('?')
                                val finalName = if (name.isNotBlank() && name.contains('.')) name else "batch_${System.currentTimeMillis()}.bin"
                                viewModel.addDownload(
                                    url = lineUrl,
                                    fileName = finalName,
                                    totalBytes = -1L,
                                    supportsResume = true,
                                    mimeType = null,
                                    category = DownloadCategory.fromFileName(finalName),
                                    numConnections = 4,
                                    scheduledTimeMillis = null,
                                    scheduledStopMillis = null,
                                    wifiOnly = false,
                                    startImmediately = true
                                )
                            }
                            batchText = ""
                            Toast.makeText(context, "${lines.size} file berhasil ditambahkan ke antrean!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.align(Alignment.End).testTag("submit_batch_button")
                    ) {
                        Text("Tambahkan Semua ke Antrean")
                    }
                }
            }
        }

        // Section: Manajemen Penyimpanan & Riwayat
        item(key = "storage_manager_section") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, null, tint = NeonAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pengelolaan Berkas & Cache",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (completedCount > 0) {
                            OutlinedButton(
                                onClick = { viewModel.clearCompleted() },
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.CleaningServices, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Bersihkan Riwayat", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total berkas selesai di daftar: $completedCount file",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
