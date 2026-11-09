package com.example.deciflux.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.deciflux.model.BatchItemStatus
import com.example.deciflux.model.NormalizationConfig
import com.example.deciflux.model.VideoBatchItem
import com.example.deciflux.ui.components.BatteryOptimizationDialog
import com.example.deciflux.ui.components.PermissionRationaleDialog
import com.example.deciflux.ui.components.PresetAndSceneSheet
import com.example.deciflux.ui.components.QueueItemCard
import com.example.deciflux.ui.components.TerminalConsoleView
import com.example.deciflux.ui.theme.DecifluxCardBorder
import com.example.deciflux.ui.theme.DecifluxCardSurface
import com.example.deciflux.ui.theme.DecifluxElectricCyan
import com.example.deciflux.ui.theme.DecifluxEmeraldPeak
import com.example.deciflux.ui.theme.DecifluxErrorRed
import com.example.deciflux.ui.theme.DecifluxLuminousViolet
import com.example.deciflux.ui.theme.DecifluxSurfaceDark
import com.example.deciflux.ui.theme.DecifluxSurfaceVariantDark
import com.example.deciflux.ui.theme.DecifluxWarningAmber
import com.example.deciflux.util.FolderScanner
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenSetup: () -> Unit = {},
    viewModel: MainScreenViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ViewModel State Flows
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
    val pauseAfterCurrent by viewModel.pauseAfterCurrent.collectAsStateWithLifecycle()
    val terminalLogs by viewModel.terminalLogs.collectAsStateWithLifecycle()
    val overallProgress by viewModel.overallProgress.collectAsStateWithLifecycle()
    val currentSpeed by viewModel.currentSpeed.collectAsStateWithLifecycle()
    val currentEta by viewModel.currentEta.collectAsStateWithLifecycle()

    // Dialog & Sheet States
    var showPresetSheet by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }
    var isScanningFolder by remember { mutableStateOf(false) }

    // Check runtime permissions
    val requiredPermissions = remember {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.READ_MEDIA_VIDEO)
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        list.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (!allGranted) {
            Toast.makeText(context, "Storage permissions are required to normalize videos", Toast.LENGTH_LONG).show()
        }
    }

    fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Intake Launchers
    // 1. Single Video Picker
    val singleVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addSingleVideo(context, it) }
    }

    // 2. Multi-File Video Picker
    val multiVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addMultipleVideos(context, uris)
        }
    }

    // 3. Folder Tree Picker
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            isScanningFolder = true
            coroutineScope.launch {
                val items = FolderScanner.scanDocumentTree(context, treeUri) { scanned, found ->
                    // Progress callback
                }
                isScanningFolder = false
                if (items.isNotEmpty()) {
                    viewModel.addScannedVideos(items)
                    Toast.makeText(context, "Discovered ${items.size} video(s)", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No supported videos found in folder", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DecifluxSurfaceDark,
        topBar = {
            MainTopAppBar(
                queue = queue,
                isProcessing = isProcessing,
                overallProgress = overallProgress,
                onOpenSetup = onOpenSetup,
                onOpenBatteryDialog = { showBatteryDialog = true },
                onClearQueue = { viewModel.clearQueue() },
                onOpenPresetSheet = { showPresetSheet = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp)
        ) {
            IntakeButtonsRow(
                isScanningFolder = isScanningFolder,
                onSingleClick = {
                    if (hasPermissions()) singleVideoLauncher.launch("video/*")
                    else showPermissionDialog = true
                },
                onFilesClick = {
                    if (hasPermissions()) multiVideoLauncher.launch("video/*")
                    else showPermissionDialog = true
                },
                onFolderClick = {
                    if (hasPermissions()) folderLauncher.launch(null)
                    else showPermissionDialog = true
                }
            )

            QueueContentBox(
                queue = queue,
                isProcessing = isProcessing,
                onRemove = { viewModel.removeVideo(it) },
                onPreview = { outUri ->
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(outUri, "video/*")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "No video player found", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            TerminalConsoleView(
                logs = terminalLogs,
                isProcessing = isProcessing,
                currentSpeed = currentSpeed,
                currentEta = currentEta,
                onClearLogs = { viewModel.clearLogs() },
                modifier = Modifier.padding(bottom = 8.dp)
            )

            BatchDockedControls(
                config = config,
                isProcessing = isProcessing,
                isPaused = isPaused,
                pauseAfterCurrent = pauseAfterCurrent,
                onOpenPresetSheet = { showPresetSheet = true },
                onStart = {
                    if (queue.isEmpty()) {
                        Toast.makeText(context, "Add videos to normalize first", Toast.LENGTH_SHORT).show()
                    } else if (!hasPermissions()) {
                        showPermissionDialog = true
                    } else {
                        viewModel.startBatch(context)
                    }
                },
                onResume = { viewModel.resumeBatch(context) },
                onTogglePause = { viewModel.togglePauseAfterCurrent(context) },
                onCancel = { viewModel.cancelBatch(context) }
            )
        }
    }

    // Modal Preset & Scene Configuration Bottom Sheet
    if (showPresetSheet) {
        PresetAndSceneSheet(
            config = config,
            onConfigChange = { viewModel.updateConfig(it) },
            onDismiss = { showPresetSheet = false }
        )
    }

    // Permission Rationale Dialog
    if (showPermissionDialog) {
        PermissionRationaleDialog(
            onGrantClick = {
                showPermissionDialog = false
                permissionLauncher.launch(requiredPermissions)
            },
            onDismiss = { showPermissionDialog = false }
        )
    }

    // Battery Optimization Dialog
    if (showBatteryDialog) {
        BatteryOptimizationDialog(
            onDismiss = { showBatteryDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopAppBar(
    queue: List<VideoBatchItem>,
    isProcessing: Boolean,
    overallProgress: Float,
    onOpenSetup: () -> Unit,
    onOpenBatteryDialog: () -> Unit,
    onClearQueue: () -> Unit,
    onOpenPresetSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DecifluxSurfaceVariantDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = DecifluxElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Deciflux",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    val completedCount = queue.count { it.status == BatchItemStatus.COMPLETED }
                    val subtitleText = if (queue.isNotEmpty()) {
                        "$completedCount/${queue.size} Processed"
                    } else {
                        "Batch Audio Normalizer"
                    }
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isProcessing) DecifluxEmeraldPeak else Color(0xFF94A3B8)
                    )
                }
            }
        },
        actions = {
            if (isProcessing) {
                CircularProgressIndicator(
                    progress = { overallProgress },
                    modifier = Modifier
                        .size(28.dp)
                        .padding(end = 6.dp),
                    color = DecifluxEmeraldPeak,
                    trackColor = Color(0xFF1E293B),
                    strokeWidth = 3.dp
                )
            }

            IconButton(onClick = onOpenSetup) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Permissions & Setup",
                    tint = DecifluxElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onOpenBatteryDialog) {
                Icon(
                    imageVector = Icons.Default.BatteryAlert,
                    contentDescription = "Battery Optimization",
                    tint = DecifluxWarningAmber,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (queue.isNotEmpty() && !isProcessing) {
                IconButton(onClick = onClearQueue) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = "Clear Queue",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            IconButton(onClick = onOpenPresetSheet) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Preset Settings",
                    tint = DecifluxElectricCyan
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DecifluxSurfaceDark,
            titleContentColor = Color.White
        )
    )
}

@Composable
private fun IntakeButtonsRow(
    isScanningFolder: Boolean,
    onSingleClick: () -> Unit,
    onFilesClick: () -> Unit,
    onFolderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onSingleClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = DecifluxCardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DecifluxCardBorder),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VideoFile,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = DecifluxElectricCyan
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Single", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onFilesClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = DecifluxCardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DecifluxCardBorder),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = DecifluxElectricCyan
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Files", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onFolderClick,
                modifier = Modifier.weight(1.1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = DecifluxCardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DecifluxCardBorder),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = DecifluxLuminousViolet
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Folder", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }

        if (isScanningFolder) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = DecifluxLuminousViolet
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scanning folder for video tracks...", fontSize = 12.sp, color = DecifluxLuminousViolet)
            }
        }
    }
}

@Composable
private fun QueueContentBox(
    queue: List<VideoBatchItem>,
    isProcessing: Boolean,
    onRemove: (String) -> Unit,
    onPreview: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        if (queue.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(DecifluxCardSurface)
                        .border(1.dp, DecifluxCardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = DecifluxElectricCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Batch Queue is Empty",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Add single videos, select multiple clips, or pick an entire directory to normalize audio volumes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(queue, key = { it.id }) { item ->
                    QueueItemCard(
                        item = item,
                        isBatchRunning = isProcessing,
                        onRemove = { onRemove(item.id) },
                        onPreview = { item.outputUri?.let(onPreview) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BatchDockedControls(
    config: NormalizationConfig,
    isProcessing: Boolean,
    isPaused: Boolean,
    pauseAfterCurrent: Boolean,
    onOpenPresetSheet: () -> Unit,
    onStart: () -> Unit,
    onResume: () -> Unit,
    onTogglePause: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, DecifluxCardBorder, RoundedCornerShape(14.dp)),
        color = DecifluxCardSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Active Preset Pill
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DecifluxSurfaceVariantDark)
                    .clickable(onClick = onOpenPresetSheet)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = DecifluxElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "${config.preset.title} (${config.targetLufs.toInt()} LUFS)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val sceneStatus = if (config.sceneLevelingEnabled) {
                        "Scene Leveling: ${config.sceneLevelingIntensity.label}"
                    } else {
                        "Scene Leveling: OFF"
                    }
                    Text(
                        text = sceneStatus,
                        fontSize = 10.sp,
                        color = if (config.sceneLevelingEnabled) DecifluxLuminousViolet else Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (!isProcessing) {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DecifluxElectricCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else if (isPaused) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DecifluxEmeraldPeak,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DecifluxErrorRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onTogglePause,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (pauseAfterCurrent) DecifluxWarningAmber else Color(0xFF262D3D),
                            contentColor = if (pauseAfterCurrent) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = if (pauseAfterCurrent) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (pauseAfterCurrent) "Cancel scheduled pause" else "Pause after current video",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DecifluxErrorRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop immediately", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

