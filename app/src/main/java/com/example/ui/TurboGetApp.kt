package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AddDownloadDialog
import com.example.ui.components.DownloadDetailDialog
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.PresetsAndToolsScreen
import com.example.ui.screens.SchedulerScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SpeedGreen
import com.example.util.FormatUtils

enum class NavigationTab(val title: String) {
    DOWNLOADS("Unduhan"),
    SCHEDULER("Penjadwal"),
    TOOLS("Kotak Alat")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurboGetApp(
    viewModel: TurboGetViewModel = viewModel(),
    initialSharedUrl: String? = null
) {
    val context = LocalContext.current
    var currentTab by remember { mutableIntStateOf(0) }

    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val selectedDetailId by viewModel.selectedDetailId.collectAsState()
    val probeState by viewModel.probeState.collectAsState()
    val totalSpeed by viewModel.totalSpeed.collectAsState()
    val activeCount by viewModel.activeCount.collectAsState()
    val rawDownloads by viewModel.rawDownloads.collectAsState()
    val liveStates by viewModel.liveStates.collectAsState()

    // Android 13+ Notification permission request
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Handle incoming shared URL
    LaunchedEffect(initialSharedUrl) {
        if (!initialSharedUrl.isNullOrBlank()) {
            viewModel.openAddDialog(initialSharedUrl)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = ElectricCyan.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TurboGet IDM",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        )
                        if (totalSpeed > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                                color = SpeedGreen.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = FormatUtils.formatSpeed(totalSpeed),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SpeedGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = (currentTab == 0),
                    onClick = { currentTab = 0 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (activeCount > 0) {
                                    Badge { Text("$activeCount") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == 0) Icons.Default.Download else Icons.Outlined.Download,
                                contentDescription = "Unduhan"
                            )
                        }
                    },
                    label = { Text("Unduhan") },
                    modifier = Modifier.testTag("nav_downloads")
                )

                NavigationBarItem(
                    selected = (currentTab == 1),
                    onClick = { currentTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 1) Icons.Default.Schedule else Icons.Outlined.Schedule,
                            contentDescription = "Penjadwal"
                        )
                    },
                    label = { Text("Penjadwal") },
                    modifier = Modifier.testTag("nav_scheduler")
                )

                NavigationBarItem(
                    selected = (currentTab == 2),
                    onClick = { currentTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 2) Icons.Default.Build else Icons.Outlined.Build,
                            contentDescription = "Kotak Alat"
                        )
                    },
                    label = { Text("Kotak Alat") },
                    modifier = Modifier.testTag("nav_tools")
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_download_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Unduhan Baru")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> DownloadsScreen(viewModel = viewModel)
                1 -> SchedulerScreen(viewModel = viewModel)
                2 -> PresetsAndToolsScreen(viewModel = viewModel)
            }
        }
    }

    // Add Download Dialog
    if (showAddDialog) {
        AddDownloadDialog(
            initialUrl = "",
            probeState = probeState,
            onProbe = { url -> viewModel.probeUrl(url) },
            onDismiss = { viewModel.closeAddDialog() },
            onConfirm = { url, fileName, totalBytes, supportsResume, mimeType, category, numConnections, schedStart, schedStop, wifiOnly, startImmediately ->
                viewModel.addDownload(
                    url = url,
                    fileName = fileName,
                    totalBytes = totalBytes,
                    supportsResume = supportsResume,
                    mimeType = mimeType,
                    category = category,
                    numConnections = numConnections,
                    scheduledTimeMillis = schedStart,
                    scheduledStopMillis = schedStop,
                    wifiOnly = wifiOnly,
                    startImmediately = startImmediately
                )
            }
        )
    }

    // Detail Dialog (IDM Inspector)
    if (selectedDetailId != null) {
        val selectedItem = rawDownloads.find { it.id == selectedDetailId }
        if (selectedItem != null) {
            DownloadDetailDialog(
                download = selectedItem,
                liveState = liveStates[selectedItem.id],
                onDismiss = { viewModel.selectDetail(null) },
                onPause = { viewModel.pause(selectedItem.id) },
                onResume = { viewModel.resume(selectedItem.id) },
                onRestart = { viewModel.restart(selectedItem.id) },
                onDelete = { deleteFile ->
                    viewModel.delete(selectedItem.id, deleteFile)
                    viewModel.selectDetail(null)
                },
                onOpen = { viewModel.openFile(context, selectedItem) },
                onCopyLink = { viewModel.copyLink(context, selectedItem.url) }
            )
        }
    }
}
