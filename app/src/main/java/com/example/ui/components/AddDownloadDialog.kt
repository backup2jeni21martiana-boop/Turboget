package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.ProbeResult
import com.example.data.model.DownloadCategory
import com.example.ui.ProbeUiState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.SpeedGreen
import com.example.util.FormatUtils
import java.util.Calendar

@Composable
fun AddDownloadDialog(
    initialUrl: String,
    probeState: ProbeUiState,
    onProbe: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (
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
    ) -> Unit
) {
    val context = LocalContext.current
    var url by remember { mutableStateOf(initialUrl) }
    var fileName by remember { mutableStateOf("") }
    var detectedSize by remember { mutableLongStateOf(-1L) }
    var supportsResume by remember { mutableStateOf(false) }
    var mimeType by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf(DownloadCategory.OTHER) }
    var numConnections by remember { mutableIntStateOf(4) }
    var wifiOnly by remember { mutableStateOf(false) }
    var isScheduled by remember { mutableStateOf(false) }
    var scheduledHour by remember { mutableIntStateOf(2) }
    var scheduledMinute by remember { mutableIntStateOf(0) }

    // When probe state succeeds, populate file info
    LaunchedEffect(probeState) {
        if (probeState is ProbeUiState.Success) {
            val res = probeState.result
            if (fileName.isBlank() || fileName.startsWith("download_")) {
                fileName = res.fileName
            }
            detectedSize = res.contentLength
            supportsResume = res.supportsResume
            mimeType = res.mimeType
            category = DownloadCategory.fromFileName(fileName, res.mimeType)
        }
    }

    // Auto probe if initial URL passed
    LaunchedEffect(initialUrl) {
        if (initialUrl.isNotBlank() && probeState is ProbeUiState.Idle) {
            onProbe(initialUrl)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tambah Unduhan Baru", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // URL input + Paste Button
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        if (fileName.isBlank() && it.contains('/')) {
                            val candidate = it.substringAfterLast('/').substringBefore('?')
                            if (candidate.contains('.')) fileName = candidate
                        }
                    },
                    label = { Text("URL File yang akan diunduh") },
                    placeholder = { Text("https://contoh.com/file.zip") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("url_input_field"),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val item = clip.primaryClip?.getItemAt(0)
                            val text = item?.text?.toString()?.trim() ?: ""
                            if (text.startsWith("http://") || text.startsWith("https://")) {
                                url = text
                                onProbe(text)
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Tempel dari Clipboard")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Probe / Check Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { onProbe(url) },
                        enabled = url.isNotBlank() && probeState !is ProbeUiState.Loading,
                        modifier = Modifier.testTag("probe_url_button")
                    ) {
                        if (probeState is ProbeUiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menganalisis Link...")
                        } else {
                            Icon(Icons.Default.Speed, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cek Info & Resume")
                        }
                    }
                }

                // Probe Result Info Card
                when (probeState) {
                    is ProbeUiState.Success -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = SpeedGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Server Terverifikasi",
                                        fontWeight = FontWeight.Bold,
                                        color = SpeedGreen,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ukuran: ${FormatUtils.formatBytes(detectedSize)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (supportsResume) "✓ Mendukung Resume (Range Requests)" else "⚠ Resume Tidak Didukung Server",
                                    color = if (supportsResume) SpeedGreen else NeonAmber,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    is ProbeUiState.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Catatan: ${probeState.message}",
                            color = NeonAmber,
                            fontSize = 11.sp
                        )
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(12.dp))

                // File Name field
                OutlinedTextField(
                    value = fileName,
                    onValueChange = {
                        fileName = it
                        category = DownloadCategory.fromFileName(it, mimeType)
                    },
                    label = { Text("Nama File") },
                    placeholder = { Text("nama_file.ext") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("filename_input_field"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Multi-threaded Connection Parts Selector (IDM feature)
                Text(
                    text = "Jumlah Koneksi Multi-Part (Thread):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 4, 8).forEach { count ->
                        FilterChip(
                            selected = (numConnections == count),
                            onClick = { numConnections = count },
                            label = { Text("$count Part") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Wi-Fi Only Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wifi, null, modifier = Modifier.size(18.dp), tint = SkyBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Hanya Unduh Lewat Wi-Fi", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Hemat kuota data seluler", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = wifiOnly,
                        onCheckedChange = { wifiOnly = it },
                        modifier = Modifier.testTag("wifi_only_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Schedule Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp), tint = NeonAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Jadwalkan Otomatis", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Mulai di jam tertentu (cth: kuota malam)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = isScheduled,
                        onCheckedChange = { isScheduled = it },
                        modifier = Modifier.testTag("schedule_switch")
                    )
                }

                AnimatedVisibility(visible = isScheduled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text("Pilih Jam Mulai (WIB):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                1 to "01:00",
                                2 to "02:00",
                                3 to "03:00",
                                6 to "06:00"
                            ).forEach { (h, label) ->
                                FilterChip(
                                    selected = (scheduledHour == h),
                                    onClick = {
                                        scheduledHour = h
                                        scheduledMinute = 0
                                    },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalFileName = if (fileName.isNotBlank()) fileName else "file_${System.currentTimeMillis()}.bin"
                    val scheduledMillis: Long? = if (isScheduled) {
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.HOUR_OF_DAY, scheduledHour)
                        cal.set(Calendar.MINUTE, scheduledMinute)
                        cal.set(Calendar.SECOND, 0)
                        if (cal.timeInMillis <= System.currentTimeMillis()) {
                            // Schedule for tomorrow
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        cal.timeInMillis
                    } else null

                    onConfirm(
                        url.trim(),
                        finalFileName,
                        detectedSize,
                        supportsResume,
                        mimeType,
                        category,
                        numConnections,
                        scheduledMillis,
                        null,
                        wifiOnly,
                        !isScheduled
                    )
                },
                enabled = url.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("confirm_download_button")
            ) {
                Text(if (isScheduled) "Simpan ke Jadwal" else "Mulai Unduh")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
