package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppNavDestination
import com.example.model.BackupMode
import com.example.model.LogLevel
import com.example.model.MtkBrand
import com.example.model.MtkChipInfo
import com.example.model.MtkDeviceDatabase
import com.example.model.MtkDeviceModel
import com.example.model.OperationProgress
import com.example.model.PartitionEntry
import com.example.model.ServiceFunction
import com.example.model.TerminalLog
import com.example.protocol.TargetPhoneState
import com.example.viewmodel.MtkBridgeViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnlockToolFlashScreen(
    viewModel: MtkBridgeViewModel,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDestination by viewModel.currentDestination.collectAsState()
    val chipInfo by viewModel.chipInfo.collectAsState()
    val targetPhoneState by viewModel.targetPhoneState.collectAsState()
    val autoNvBackup by viewModel.autoNvBackup.collectAsState()
    val autoReboot by viewModel.autoReboot.collectAsState()
    val partitions by viewModel.partitions.collectAsState()
    val progress by viewModel.operationProgress.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val flashOptions by viewModel.flashOptions.collectAsState()
    val backupMode by viewModel.backupMode.collectAsState()
    val selectedBrand by viewModel.selectedBrand.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val scatterFileName by viewModel.scatterFileName.collectAsState()
    val detectedPorts by viewModel.detectedPorts.collectAsState()
    val isAutoSnifferActive by viewModel.isAutoSnifferActive.collectAsState()
    val activeLockedMode by viewModel.activeLockedMode.collectAsState()
    val isModeIsolationEnabled by viewModel.isModeIsolationEnabled.collectAsState()
    val backupLocation by viewModel.backupLocation.collectAsState()

    // Service function selection state
    var selectedServiceOption by remember { mutableStateOf(ServiceFunction.ERASE_FRP) }
    var selectedFastbootAction by remember { mutableStateOf("getvar:all") }
    var customFastbootCmd by remember { mutableStateOf("") }
    var selectedAdbAction by remember { mutableStateOf("devices") }
    var customAdbCmd by remember { mutableStateOf("") }

    val context = LocalContext.current
    var showStopDialog by remember { mutableStateOf(false) }
    var showPortSnifferDialog by remember { mutableStateOf(false) }
    var showBromGuideDialog by remember { mutableStateOf(false) }

    // Helper function to resolve real display name and file size from content URI
    fun resolveFileInfo(uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "file.bin"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx != -1) {
                        val displayName = cursor.getString(nameIdx)
                        if (!displayName.isNullOrBlank()) name = displayName
                    }
                    if (sizeIdx != -1) {
                        size = cursor.getLong(sizeIdx)
                    }
                }
            }
        } catch (_: Exception) {}
        return Pair(name, size)
    }

    var partitionPickingIndex by remember { mutableStateOf<Int?>(null) }
    val partitionImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val idx = partitionPickingIndex
        if (uri != null && idx != null) {
            val (fileName, fileSize) = resolveFileInfo(uri)
            viewModel.bindPartitionCustomFile(idx, fileName, if (fileSize > 0) fileSize else null)
        }
        partitionPickingIndex = null
    }

    // File pickers
    val scatterPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val (fileName, _) = resolveFileInfo(it)
                val inputStream = context.contentResolver.openInputStream(it)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                reader.close()
                viewModel.loadScatterContent(content, fileName)
            } catch (e: Exception) {
                viewModel.addLog(TerminalLog(getLogTime(), "Failed to load scatter: ${e.message}", LogLevel.ERROR))
            }
        }
    }

    val daPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val (fileName, _) = resolveFileInfo(it)
            viewModel.customDaPath.value = fileName
            viewModel.addLog(TerminalLog(getLogTime(), "Custom DA Selected: $fileName", LogLevel.INFO))
        }
    }

    val preloaderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val (fileName, _) = resolveFileInfo(it)
            viewModel.preloaderPath.value = fileName
            viewModel.addLog(TerminalLog(getLogTime(), "Preloader Selected: $fileName", LogLevel.INFO))
        }
    }

    val authPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val (fileName, _) = resolveFileInfo(it)
            viewModel.authFilePath.value = fileName
            viewModel.addLog(TerminalLog(getLogTime(), "Auth File Selected: $fileName", LogLevel.INFO))
        }
    }

    val backupFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val path = it.path ?: it.toString()
            viewModel.setCustomBackupLocation(path)
        }
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    Text("Stop Operation?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                }
            },
            text = {
                Text(
                    "Warning: Interrupting active partition flashing or formatting may risk corrupting device storage.",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1)
                )
            },
            containerColor = Color(0xFF1E293B),
            confirmButton = {
                Button(
                    onClick = {
                        showStopDialog = false
                        viewModel.cancelCurrentOperation()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Yes, Abort", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showStopDialog = false },
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Text("Cancel", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }
        )
    }

    if (showPortSnifferDialog) {
        PortSnifferDialog(
            detectedPorts = detectedPorts,
            isAutoSnifferActive = isAutoSnifferActive,
            activeLockedMode = activeLockedMode,
            isModeIsolationEnabled = isModeIsolationEnabled,
            onDismiss = { showPortSnifferDialog = false },
            onScanNow = { viewModel.refreshUsbPorts() },
            onToggleAutoSniffer = { viewModel.toggleAutoSniffer(it) },
            onToggleModeIsolation = { viewModel.toggleModeIsolation(it) },
            onConnectPort = { port ->
                viewModel.connectSpecificPort(port)
                showPortSnifferDialog = false
            }
        )
    }

    if (showBromGuideDialog) {
        BromConnectionGuideDialog(
            onDismiss = { showBromGuideDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        // 1. TOP HARDWARE & MODE STATUS BAR
        ToolTopBar(
            currentDestination = currentDestination,
            chipInfo = chipInfo,
            targetPhoneState = targetPhoneState,
            detectedPortsCount = detectedPorts.size,
            activeLockedMode = activeLockedMode,
            isModeIsolationEnabled = isModeIsolationEnabled,
            onToggleModeIsolation = { viewModel.toggleModeIsolation(it) },
            onOpenDrawer = onOpenDrawer,
            onScanPorts = { viewModel.refreshUsbPorts() },
            onOpenPortSniffer = { showPortSnifferDialog = true },
            onOpenBromGuide = { showBromGuideDialog = true },
            onTabSelected = { viewModel.navigateTo(it) }
        )

        // 2. MAIN SPLIT CONTENT: Top Settings (1/3) + Bottom Terminal Console (2/3)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            // TOP SECTION: SETTINGS MENU (Takes 1/3 of available screen, scrollable)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // TAB-SPECIFIC GROUP BOXES & CONFIGURATIONS
                    when (currentDestination) {
                        AppNavDestination.FLASH -> {
                            // GROUP BOX 1: SCATTER & LOADER FILES (Flash Tab Only)
                            GroupBox(title = "1. Scatter & Firmware Loader Files", icon = Icons.Default.FolderOpen) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PickerPill(
                                        label = if (scatterFileName.isEmpty()) "Scatter File" else scatterFileName,
                                        icon = Icons.Default.FileOpen,
                                        isLoaded = scatterFileName.isNotEmpty(),
                                        modifier = Modifier.widthIn(min = 105.dp),
                                        onClick = { scatterPickerLauncher.launch("*/*") }
                                    )
                                    PickerPill(
                                        label = if (viewModel.customDaPath.value.isEmpty()) "DA Agent" else viewModel.customDaPath.value,
                                        icon = Icons.Default.Build,
                                        isLoaded = viewModel.customDaPath.value.isNotEmpty(),
                                        modifier = Modifier.widthIn(min = 90.dp),
                                        onClick = { daPickerLauncher.launch("*/*") }
                                    )
                                    PickerPill(
                                        label = if (viewModel.preloaderPath.value.isEmpty()) "Preloader" else viewModel.preloaderPath.value,
                                        icon = Icons.Default.Build,
                                        isLoaded = viewModel.preloaderPath.value.isNotEmpty(),
                                        modifier = Modifier.widthIn(min = 90.dp),
                                        onClick = { preloaderPickerLauncher.launch("*/*") }
                                    )
                                    PickerPill(
                                        label = if (viewModel.authFilePath.value.isEmpty()) "Auth / SLA" else viewModel.authFilePath.value,
                                        icon = Icons.Default.Security,
                                        isLoaded = viewModel.authFilePath.value.isNotEmpty(),
                                        modifier = Modifier.widthIn(min = 90.dp),
                                        onClick = { authPickerLauncher.launch("*/*") }
                                    )
                                }
                            }

                            // GROUP BOX 2: FLASHING CHECKBOX OPTIONS & PARTITIONS
                            GroupBox(title = "2. Flashing Checkbox Options & Partitions", icon = Icons.Default.FlashOn) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    maxItemsInEachRow = 3
                                ) {
                                    ToolCheckbox(
                                        label = "Auto NV Backup",
                                        checked = flashOptions.readNvData,
                                        onCheckedChange = { viewModel.toggleFlashReadNvData(it) }
                                    )
                                    ToolCheckbox(
                                        label = "Auto Reboot",
                                        checked = flashOptions.autoReboot,
                                        onCheckedChange = { viewModel.toggleFlashAutoReboot(it) }
                                    )
                                    ToolCheckbox(
                                        label = "DA DL Checksum",
                                        checked = flashOptions.daDlChecksum,
                                        onCheckedChange = { viewModel.toggleFlashDaDlChecksum(it) }
                                    )
                                    ToolCheckbox(
                                        label = "Auto Sign Flash",
                                        checked = flashOptions.autoSignFlash,
                                        onCheckedChange = { viewModel.toggleFlashAutoSign(it) }
                                    )
                                    ToolCheckbox(
                                        label = "Post Unlock Flash",
                                        checked = flashOptions.flashAfterBlUnlock,
                                        onCheckedChange = { viewModel.toggleFlashAfterBlUnlock(it) }
                                    )
                                    ToolCheckbox(
                                        label = "Format All + DL",
                                        checked = flashOptions.formatAllDownload,
                                        isWarning = true,
                                        onCheckedChange = { viewModel.toggleFlashFormatAll(it) }
                                    )
                                }

                                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))

                                // Partition Selection Table Checkboxes
                                PartitionSelectionBox(
                                    partitions = partitions,
                                    onToggleAll = { viewModel.selectAllPartitions(it) },
                                    onTogglePartition = { idx, checked -> viewModel.togglePartitionSelection(idx, checked) },
                                    onPickPartitionFile = { idx ->
                                        partitionPickingIndex = idx
                                        partitionImagePickerLauncher.launch("*/*")
                                    }
                                )
                            }
                        }

                        AppNavDestination.BACKUP -> {
                            // GROUP BOX 1: BACKUP SCOPE & PARTITION SELECTION (Backup Tab Only)
                            GroupBox(title = "1. Backup Scope & Output Storage", icon = Icons.Default.FolderOpen) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Select Backup Scope:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                    ToolCheckbox(
                                        label = "Auto Reboot",
                                        checked = autoReboot,
                                        onCheckedChange = { viewModel.toggleAutoReboot(it) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    BackupOptionRow(
                                        title = "1. Full ROM Dump (All GPT Partitions)",
                                        subtitle = "Creates full raw flash dump for unbricking and restoration",
                                        isSelected = backupMode == BackupMode.FULL_FIRMWARE,
                                        onClick = { viewModel.setBackupMode(BackupMode.FULL_FIRMWARE) }
                                    )
                                    BackupOptionRow(
                                        title = "2. Stable Boot Partitions Dump",
                                        subtitle = "boot, recovery, vbmeta, dtbo, lk, spmfw, super",
                                        isSelected = backupMode == BackupMode.STABLE_FIRMWARE,
                                        onClick = { viewModel.setBackupMode(BackupMode.STABLE_FIRMWARE) }
                                    )
                                    BackupOptionRow(
                                        title = "3. NV Data & IMEI Security Dump (Recommended)",
                                        subtitle = "nvram, nvdata, persist, protect1, protect2, proinfo",
                                        isSelected = backupMode == BackupMode.NV_DATA,
                                        onClick = { viewModel.setBackupMode(BackupMode.NV_DATA) }
                                    )
                                    BackupOptionRow(
                                        title = "4. Custom Partition Selection",
                                        subtitle = "Dump only user-checked partitions in the table below",
                                        isSelected = backupMode == BackupMode.CUSTOM_PARTITIONS,
                                        onClick = { viewModel.setBackupMode(BackupMode.CUSTOM_PARTITIONS) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Backup Output Location Bar
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF0F172A),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { backupFolderPickerLauncher.launch(null) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = "Backup Storage Folder",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Backup Output Directory",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF94A3B8)
                                            )
                                            Text(
                                                text = backupLocation,
                                                fontSize = 9.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFFF1F5F9),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "Change",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                }

                                if (backupMode == BackupMode.CUSTOM_PARTITIONS && partitions.isNotEmpty()) {
                                    HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))
                                    PartitionSelectionBox(
                                        partitions = partitions,
                                        onToggleAll = { viewModel.selectAllPartitions(it) },
                                        onTogglePartition = { idx, checked -> viewModel.togglePartitionSelection(idx, checked) }
                                    )
                                }
                            }
                        }

                        AppNavDestination.SERVICE -> {
                            // GROUP BOX 1: TARGET BRAND & MODEL SELECTION (Service Tab Only)
                            GroupBox(title = "1. Target Phone Brand & Model", icon = Icons.Default.Memory) {
                                BrandModelSelector(
                                    selectedBrand = selectedBrand,
                                    selectedModel = selectedModel,
                                    onBrandSelect = { viewModel.selectBrand(it) },
                                    onModelSelect = { viewModel.selectModel(it) }
                                )
                            }

                            // GROUP BOX 2: GSM SERVICE OPERATIONS
                            GroupBox(title = "2. GSM Service Operations", icon = Icons.Default.LockOpen) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ToolCheckbox(
                                        label = "Auto NV Backup (Safety Guard)",
                                        checked = autoNvBackup,
                                        onCheckedChange = { viewModel.toggleAutoNvBackup(it) }
                                    )
                                    ToolCheckbox(
                                        label = "Auto Reboot",
                                        checked = autoReboot,
                                        onCheckedChange = { viewModel.toggleAutoReboot(it) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(3.dp))
                                Text("Choose service function to execute:", fontSize = 10.sp, color = Color(0xFF94A3B8))

                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        ServiceSelectCard(
                                            title = "Erase FRP (Google)",
                                            isSelected = selectedServiceOption == ServiceFunction.ERASE_FRP,
                                            accentColor = Color(0xFFF59E0B),
                                            modifier = Modifier.weight(1f)
                                        ) { selectedServiceOption = ServiceFunction.ERASE_FRP }

                                        ServiceSelectCard(
                                            title = "Factory Reset (Wipe)",
                                            isSelected = selectedServiceOption == ServiceFunction.FACTORY_RESET,
                                            accentColor = Color(0xFFEF4444),
                                            modifier = Modifier.weight(1f)
                                        ) { selectedServiceOption = ServiceFunction.FACTORY_RESET }

                                        ServiceSelectCard(
                                            title = "Unlock Bootloader",
                                            isSelected = selectedServiceOption == ServiceFunction.UNLOCK_BOOTLOADER,
                                            accentColor = Color(0xFF10B981),
                                            modifier = Modifier.weight(1f)
                                        ) { selectedServiceOption = ServiceFunction.UNLOCK_BOOTLOADER }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        ServiceSelectCard(
                                            title = "Relock Bootloader",
                                            isSelected = selectedServiceOption == ServiceFunction.LOCK_BOOTLOADER,
                                            accentColor = Color(0xFF64748B),
                                            modifier = Modifier.weight(1f)
                                        ) { selectedServiceOption = ServiceFunction.LOCK_BOOTLOADER }

                                        ServiceSelectCard(
                                            title = "Disable Mi Account",
                                            isSelected = selectedServiceOption == ServiceFunction.DISABLE_MI_ACCOUNT,
                                            accentColor = Color(0xFFF97316),
                                            modifier = Modifier.weight(1f)
                                        ) { selectedServiceOption = ServiceFunction.DISABLE_MI_ACCOUNT }

                                        ServiceSelectCard(
                                            title = "Restore NVRAM",
                                            isSelected = selectedServiceOption == ServiceFunction.RESTORE_NVRAM,
                                            accentColor = Color(0xFF8B5CF6),
                                            modifier = Modifier.weight(1f)
                                        ) { selectedServiceOption = ServiceFunction.RESTORE_NVRAM }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        ServiceSelectCard(
                                            title = "Read Chip Info",
                                            isSelected = selectedServiceOption == ServiceFunction.READ_INFO,
                                            accentColor = Color(0xFF06B6D4),
                                            modifier = Modifier.weight(1f)
                                        ) { selectedServiceOption = ServiceFunction.READ_INFO }

                                        ServiceSelectCard(
                                            title = "Read RPMB Keys",
                                            isSelected = selectedServiceOption == ServiceFunction.READ_RPMB,
                                            accentColor = Color(0xFF6366F1),
                                            modifier = Modifier.weight(1f)
                                        ) { selectedServiceOption = ServiceFunction.READ_RPMB }

                                        ServiceSelectCard(
                                            title = "Crash to BROM",
                                            isSelected = selectedServiceOption == ServiceFunction.CRASH_TO_BROM,
                                            accentColor = Color(0xFFEC4899),
                                            modifier = Modifier.weight(1f)
                                        ) { selectedServiceOption = ServiceFunction.CRASH_TO_BROM }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        ServiceSelectCard(
                                            title = "Bypass SLA/DAA Auth",
                                            isSelected = selectedServiceOption == ServiceFunction.BYPASS_AUTH,
                                            accentColor = Color(0xFF14B8A6),
                                            modifier = Modifier.weight(1f)
                                        ) { selectedServiceOption = ServiceFunction.BYPASS_AUTH }

                                        ServiceSelectCard(
                                            title = "Memory Health Test",
                                            isSelected = selectedServiceOption == ServiceFunction.MEMORY_TEST,
                                            accentColor = Color(0xFF3B82F6),
                                            modifier = Modifier.weight(1f)
                                        ) { selectedServiceOption = ServiceFunction.MEMORY_TEST }

                                        ServiceSelectCard(
                                            title = "Backup NVRAM & Scatter",
                                            isSelected = selectedServiceOption == ServiceFunction.BACKUP_NVRAM,
                                            accentColor = Color(0xFFA855F7),
                                            modifier = Modifier.weight(1f)
                                        ) { selectedServiceOption = ServiceFunction.BACKUP_NVRAM }
                                    }
                                }
                            }
                        }

                        AppNavDestination.FASTBOOT -> {
                            // GROUP BOX 1: FASTBOOT PRE-CONFIGURED COMMANDS (Fastboot Tab Only)
                            GroupBox(title = "1. Fastboot Mode Actions & Functions", icon = Icons.Default.Terminal) {
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FastbootOptionCard("Read Fastboot Info", "getvar:all", isSelected = (selectedFastbootAction == "getvar:all" || selectedFastbootAction == "read_info") && customFastbootCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedFastbootAction = "getvar:all"
                                            customFastbootCmd = ""
                                        }
                                        FastbootOptionCard("Flashing Unlock", "flashing unlock", isSelected = selectedFastbootAction == "flashing unlock" && customFastbootCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedFastbootAction = "flashing unlock"
                                            customFastbootCmd = ""
                                        }
                                        FastbootOptionCard("OEM Unlock", "oem unlock", isSelected = selectedFastbootAction == "oem unlock" && customFastbootCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedFastbootAction = "oem unlock"
                                            customFastbootCmd = ""
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FastbootOptionCard("Lock Bootloader", "flashing lock", isSelected = selectedFastbootAction == "flashing lock" && customFastbootCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedFastbootAction = "flashing lock"
                                            customFastbootCmd = ""
                                        }
                                        FastbootOptionCard("Erase FRP", "erase:frp", isSelected = selectedFastbootAction == "erase:frp" && customFastbootCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedFastbootAction = "erase:frp"
                                            customFastbootCmd = ""
                                        }
                                        FastbootOptionCard("Erase Config (FRP2)", "erase:config", isSelected = selectedFastbootAction == "erase:config" && customFastbootCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedFastbootAction = "erase:config"
                                            customFastbootCmd = ""
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FastbootOptionCard("Wipe Data (Reset)", "erase:userdata", isSelected = selectedFastbootAction == "erase:userdata" && customFastbootCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedFastbootAction = "erase:userdata"
                                            customFastbootCmd = ""
                                        }
                                        FastbootOptionCard("Wipe Cache", "erase:cache", isSelected = selectedFastbootAction == "erase:cache" && customFastbootCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedFastbootAction = "erase:cache"
                                            customFastbootCmd = ""
                                        }
                                        FastbootOptionCard("Wipe Metadata", "erase:metadata", isSelected = selectedFastbootAction == "erase:metadata" && customFastbootCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedFastbootAction = "erase:metadata"
                                            customFastbootCmd = ""
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FastbootOptionCard("Active Slot A", "set_active a", isSelected = selectedFastbootAction == "set_active a" && customFastbootCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedFastbootAction = "set_active a"
                                            customFastbootCmd = ""
                                        }
                                        FastbootOptionCard("Active Slot B", "set_active b", isSelected = selectedFastbootAction == "set_active b" && customFastbootCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedFastbootAction = "set_active b"
                                            customFastbootCmd = ""
                                        }
                                        FastbootOptionCard("Current Slot", "getvar:current-slot", isSelected = selectedFastbootAction == "getvar:current-slot" && customFastbootCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedFastbootAction = "getvar:current-slot"
                                            customFastbootCmd = ""
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FastbootOptionCard("Reboot System", "reboot", isSelected = selectedFastbootAction == "reboot" && customFastbootCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedFastbootAction = "reboot"
                                            customFastbootCmd = ""
                                        }
                                        FastbootOptionCard("Reboot Recovery", "reboot-recovery", isSelected = selectedFastbootAction == "reboot-recovery" && customFastbootCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedFastbootAction = "reboot-recovery"
                                            customFastbootCmd = ""
                                        }
                                        FastbootOptionCard("Reboot FastbootD", "reboot-fastboot", isSelected = selectedFastbootAction == "reboot-fastboot" && customFastbootCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedFastbootAction = "reboot-fastboot"
                                            customFastbootCmd = ""
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FastbootOptionCard("Reboot Bootloader", "reboot-bootloader", isSelected = selectedFastbootAction == "reboot-bootloader" && customFastbootCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedFastbootAction = "reboot-bootloader"
                                            customFastbootCmd = ""
                                        }
                                        FastbootOptionCard("Reboot EDL (9008)", "oem edl", isSelected = selectedFastbootAction == "oem edl" && customFastbootCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedFastbootAction = "oem edl"
                                            customFastbootCmd = ""
                                        }
                                        FastbootOptionCard("Erase NVRAM / Data", "erase:nvdata", isSelected = selectedFastbootAction == "erase:nvdata" && customFastbootCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedFastbootAction = "erase:nvdata"
                                            customFastbootCmd = ""
                                        }
                                    }
                                }
                            }

                            // GROUP BOX 2: CUSTOM FASTBOOT COMMAND
                            GroupBox(title = "2. Custom Fastboot Command", icon = Icons.Default.Terminal) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF090D16),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier.weight(1f).height(32.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 6.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (customFastbootCmd.isEmpty()) {
                                                Text(
                                                    text = "Type custom fastboot command (e.g. oem edl, getvar product, erase:userdata)",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                            BasicTextField(
                                                value = customFastbootCmd,
                                                onValueChange = { customFastbootCmd = it },
                                                textStyle = TextStyle(
                                                    color = Color(0xFFF1F5F9),
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace
                                                ),
                                                cursorBrush = SolidColor(Color(0xFF38BDF8)),
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AppNavDestination.ADB -> {
                            // GROUP BOX 1: ADB SHELL COMMANDS (ADB Tab Only)
                            GroupBox(title = "1. ADB Operations & Bypass Functions", icon = Icons.Default.Usb) {
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FastbootOptionCard("Read Full Info", "read_info", isSelected = (selectedAdbAction == "read_info" || selectedAdbAction.startsWith("getprop")) && customAdbCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedAdbAction = "read_info"
                                            customAdbCmd = ""
                                        }
                                        FastbootOptionCard("Bypass FRP (Setup)", "settings put global setup_wizard_has_run 1 && settings put secure user_setup_complete 1 && settings put global device_provisioned 1 && am start -c android.intent.category.HOME -a android.intent.action.MAIN", isSelected = selectedAdbAction.contains("setup_wizard_has_run") && customAdbCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedAdbAction = "settings put global setup_wizard_has_run 1 && settings put secure user_setup_complete 1 && settings put global device_provisioned 1 && am start -c android.intent.category.HOME -a android.intent.action.MAIN"
                                            customAdbCmd = ""
                                        }
                                        FastbootOptionCard("MoreLocale Perm", "pm grant jp.co.c_lis.ccl.morelocale android.permission.CHANGE_CONFIGURATION", isSelected = selectedAdbAction.contains("morelocale") && customAdbCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedAdbAction = "pm grant jp.co.c_lis.ccl.morelocale android.permission.CHANGE_CONFIGURATION"
                                            customAdbCmd = ""
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FastbootOptionCard("Reboot Bootloader", "reboot bootloader", isSelected = selectedAdbAction == "reboot bootloader" && customAdbCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedAdbAction = "reboot bootloader"
                                            customAdbCmd = ""
                                        }
                                        FastbootOptionCard("Reboot Recovery", "reboot recovery", isSelected = selectedAdbAction == "reboot recovery" && customAdbCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedAdbAction = "reboot recovery"
                                            customAdbCmd = ""
                                        }
                                        FastbootOptionCard("Reboot EDL (9008)", "reboot edl", isSelected = selectedAdbAction == "reboot edl" && customAdbCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedAdbAction = "reboot edl"
                                            customAdbCmd = ""
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FastbootOptionCard("Disable Mi Cloud", "pm disable-user --user 0 com.miui.cloudservice", isSelected = selectedAdbAction.contains("com.miui.cloudservice") && customAdbCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedAdbAction = "pm disable-user --user 0 com.miui.cloudservice && pm disable-user --user 0 com.xiaomi.finddevice"
                                            customAdbCmd = ""
                                        }
                                        FastbootOptionCard("Battery Health", "dumpsys battery", isSelected = selectedAdbAction == "dumpsys battery" && customAdbCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedAdbAction = "dumpsys battery"
                                            customAdbCmd = ""
                                        }
                                        FastbootOptionCard("Screen Size & DPI", "wm size && wm density", isSelected = selectedAdbAction == "wm size && wm density" && customAdbCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedAdbAction = "wm size && wm density"
                                            customAdbCmd = ""
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FastbootOptionCard("Remove Demo Mode", "am broadcast -a com.google.android.setupwizard.DEMO_MODE_EXIT", isSelected = selectedAdbAction.contains("DEMO_MODE_EXIT") && customAdbCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedAdbAction = "am broadcast -a com.google.android.setupwizard.DEMO_MODE_EXIT"
                                            customAdbCmd = ""
                                        }
                                        FastbootOptionCard("Dump Partitions", "cat /proc/partitions", isSelected = selectedAdbAction == "cat /proc/partitions" && customAdbCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedAdbAction = "cat /proc/partitions"
                                            customAdbCmd = ""
                                        }
                                        FastbootOptionCard("Reboot System", "reboot", isSelected = selectedAdbAction == "reboot" && customAdbCmd.isEmpty(), modifier = Modifier.weight(1f)) {
                                            selectedAdbAction = "reboot"
                                            customAdbCmd = ""
                                        }
                                    }
                                }
                            }

                            // GROUP BOX 2: CUSTOM ADB COMMAND
                            GroupBox(title = "2. Custom ADB Shell Terminal", icon = Icons.Default.Usb) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF090D16),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier.weight(1f).height(32.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 6.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (customAdbCmd.isEmpty()) {
                                                Text(
                                                    text = "Type custom ADB shell command (e.g. getprop, pm list packages)",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                            BasicTextField(
                                                value = customAdbCmd,
                                                onValueChange = { customAdbCmd = it },
                                                textStyle = TextStyle(
                                                    color = Color(0xFFF1F5F9),
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace
                                                ),
                                                cursorBrush = SolidColor(Color(0xFF38BDF8)),
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AppNavDestination.OTHER -> {
                            // GROUP BOX 1: ADVANCED HARDWARE TOOLS (Other Tab Only)
                            GroupBox(title = "1. Advanced Hardware & Security Tools", icon = Icons.Default.Build) {
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        ServiceSelectCard(
                                            title = "Memory & eMMC Health Test",
                                            isSelected = selectedServiceOption == ServiceFunction.MEMORY_TEST,
                                            accentColor = Color(0xFF06B6D4),
                                            modifier = Modifier.weight(1f)
                                        ) { selectedServiceOption = ServiceFunction.MEMORY_TEST }

                                        ServiceSelectCard(
                                            title = "SLA / DAA Auth Bypass",
                                            isSelected = selectedServiceOption == ServiceFunction.BYPASS_AUTH,
                                            accentColor = Color(0xFFF43F5E),
                                            modifier = Modifier.weight(1f)
                                        ) { selectedServiceOption = ServiceFunction.BYPASS_AUTH }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        ServiceSelectCard(
                                            title = "Force Preloader Crash to BROM",
                                            isSelected = selectedServiceOption == ServiceFunction.CRASH_TO_BROM,
                                            accentColor = Color(0xFF3B82F6),
                                            modifier = Modifier.weight(1f)
                                        ) { selectedServiceOption = ServiceFunction.CRASH_TO_BROM }

                                        ServiceSelectCard(
                                            title = "Reboot to System OS",
                                            isSelected = selectedServiceOption == ServiceFunction.REBOOT_SYSTEM,
                                            accentColor = Color(0xFF10B981),
                                            modifier = Modifier.weight(1f)
                                        ) { selectedServiceOption = ServiceFunction.REBOOT_SYSTEM }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. SINGLE PROMINENT START / STOP EXECUTION BUTTON
            SingleStartExecutionButton(
                currentDestination = currentDestination,
                selectedServiceOption = selectedServiceOption,
                selectedFastbootAction = if (customFastbootCmd.isNotBlank()) customFastbootCmd.trim() else selectedFastbootAction,
                selectedAdbAction = if (customAdbCmd.isNotBlank()) customAdbCmd.trim() else selectedAdbAction,
                backupMode = backupMode,
                progress = progress,
                partitionsCount = partitions.count { it.isSelectedForFlashing },
                onExecute = {
                    if (progress.isRunning) {
                        viewModel.cancelCurrentOperation()
                    } else {
                        when (currentDestination) {
                            AppNavDestination.FLASH -> viewModel.executeFlashOperation()
                            AppNavDestination.BACKUP -> viewModel.executeBackupOperation()
                            AppNavDestination.SERVICE, AppNavDestination.OTHER -> viewModel.executeServiceFunctionDirect(selectedServiceOption)
                            AppNavDestination.FASTBOOT -> {
                                val cmd = if (customFastbootCmd.isNotBlank()) customFastbootCmd.trim() else selectedFastbootAction
                                if (cmd == "getvar:all" || cmd == "read_info" || cmd.startsWith("getvar all")) {
                                    viewModel.runFastbootReadAllVars()
                                } else {
                                    viewModel.runFastbootCommand(cmd, cmd)
                                }
                            }
                            AppNavDestination.ADB -> {
                                val cmd = if (customAdbCmd.isNotBlank()) customAdbCmd.trim() else selectedAdbAction
                                if (cmd == "read_info" || cmd.startsWith("getprop")) {
                                    viewModel.runAdbReadInfo()
                                } else {
                                    viewModel.runAdbCommand(cmd, cmd)
                                }
                            }
                        }
                    }
                }
            )

            // Progress Bar & Live Telemetry (Animation, Percentage %, Transfer Speed, Data Processed, ETA)
            if (progress.isRunning || progress.percentage > 0f) {
                Spacer(modifier = Modifier.height(3.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (progress.isRunning) {
                                    CircularProgressIndicator(
                                        strokeWidth = 1.5.dp,
                                        modifier = Modifier.size(10.dp),
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                                Text(
                                    text = progress.title.ifEmpty { "Executing Task..." },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            val percentVal = (progress.percentage * 100).toInt().coerceIn(0, 100)
                            Text(
                                text = "$percentVal%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF4ADE80)
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Animated Progress Indicator
                        LinearProgressIndicator(
                            progress = { progress.percentage.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF1E293B)
                        )

                        // Real-time Speed & Data Telemetry Row
                        if (progress.speedKbPerSec > 0.0 || progress.bytesProcessed > 0L || progress.detail.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val speedDisplay = when {
                                    progress.speedKbPerSec >= 1024.0 -> String.format("⚡ %.2f MB/s", progress.speedKbPerSec / 1024.0)
                                    progress.speedKbPerSec > 0.0 -> String.format("⚡ %.1f KB/s", progress.speedKbPerSec)
                                    else -> "⚡ USB Active"
                                }
                                val processedDisplay = when {
                                    progress.totalBytes >= 1024 * 1024 * 1024 -> String.format("%.2f / %.2f GB", progress.bytesProcessed / (1024.0 * 1024 * 1024), progress.totalBytes / (1024.0 * 1024 * 1024))
                                    progress.totalBytes >= 1024 * 1024 -> String.format("%.1f / %.1f MB", progress.bytesProcessed / (1024.0 * 1024), progress.totalBytes / (1024.0 * 1024))
                                    progress.totalBytes > 0 -> "${progress.bytesProcessed / 1024} / ${progress.totalBytes / 1024} KB"
                                    else -> progress.detail
                                }
                                val etaDisplay = if (progress.estimatedSecondsRemaining > 0) "⏱ ETA: ${progress.estimatedSecondsRemaining}s" else ""

                                Text(
                                    text = speedDisplay,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFBBF24)
                                )
                                if (processedDisplay.isNotEmpty()) {
                                    Text(
                                        text = processedDisplay,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                if (etaDisplay.isNotEmpty()) {
                                    Text(
                                        text = etaDisplay,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 4. SPACIOUS LIVE TERMINAL CONSOLE (Takes 2/3 of available screen space)
            SpaciousTerminalConsole(
                logs = logs,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f),
                onClear = { viewModel.clearLogs() }
            )
        }
    }
}

// =============================================================================
// REUSABLE UI COMPONENTS (GROUP BOX, BUTTONS, TERMINAL)
// =============================================================================

@Composable
private fun GroupBox(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
        border = BorderStroke(1.dp, Color(0xFF26354A)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF1F5F9)
                )
            }
            HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(bottom = 2.dp))
            content()
        }
    }
}

@Composable
private fun ToolCheckbox(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    isWarning: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = label,
            tint = if (checked) {
                if (isWarning) Color(0xFFEF4444) else Color(0xFF38BDF8)
            } else Color(0xFF64748B),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
            color = if (checked) {
                if (isWarning) Color(0xFFFCA5A5) else Color(0xFFF1F5F9)
            } else Color(0xFF94A3B8),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SingleStartExecutionButton(
    currentDestination: AppNavDestination,
    selectedServiceOption: ServiceFunction,
    selectedFastbootAction: String,
    selectedAdbAction: String,
    backupMode: BackupMode,
    progress: OperationProgress,
    partitionsCount: Int,
    onExecute: () -> Unit
) {
    val buttonText = remember(currentDestination, selectedServiceOption, selectedFastbootAction, selectedAdbAction, backupMode, partitionsCount, progress.isRunning) {
        if (progress.isRunning) {
            "STOP ACTIVE OPERATION (ABORT)"
        } else {
            when (currentDestination) {
                AppNavDestination.FLASH -> "START FLASH ($partitionsCount Partitions Checked)"
                AppNavDestination.BACKUP -> "START BACKUP (${backupMode.shortLabel})"
                AppNavDestination.SERVICE -> "START: ${selectedServiceOption.title.uppercase()}"
                AppNavDestination.FASTBOOT -> "EXECUTE FASTBOOT: $selectedFastbootAction"
                AppNavDestination.ADB -> "EXECUTE ADB: $selectedAdbAction"
                AppNavDestination.OTHER -> "START: ${selectedServiceOption.title.uppercase()}"
            }
        }
    }

    val buttonColor by animateColorAsState(
        targetValue = if (progress.isRunning) Color(0xFFDC2626) else Color(0xFF16A34A),
        label = "btnColor"
    )

    Button(
        onClick = onExecute,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .testTag("single_start_button")
    ) {
        Icon(
            imageVector = if (progress.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = buttonText,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PartitionSelectionBox(
    partitions: List<PartitionEntry>,
    onToggleAll: (Boolean) -> Unit,
    onTogglePartition: (Int, Boolean) -> Unit,
    onPickPartitionFile: ((Int) -> Unit)? = null
) {
    val allChecked = partitions.isNotEmpty() && partitions.all { it.isSelectedForFlashing }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Scatter Partitions (${partitions.size}):",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .clickable { onToggleAll(!allChecked) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = if (allChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = "Select All",
                    tint = if (allChecked) Color(0xFF38BDF8) else Color(0xFF64748B),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (allChecked) "Uncheck All" else "Check All",
                    fontSize = 9.5.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (partitions.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF090D16),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "No Scatter / GPT Loaded. Tap 'Scatter File' above or connect device in BROM.",
                    fontSize = 9.5.sp,
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(8.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 135.dp)
                    .background(Color(0xFF090D16), RoundedCornerShape(4.dp))
                    .padding(2.dp)
            ) {
                itemsIndexed(partitions) { index, part ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTogglePartition(index, !part.isSelectedForFlashing) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (part.isSelectedForFlashing) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = part.partitionName,
                            tint = if (part.isSelectedForFlashing) Color(0xFF38BDF8) else Color(0xFF475569),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = part.partitionName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (part.isSelectedForFlashing) Color(0xFFF1F5F9) else Color(0xFF64748B),
                            modifier = Modifier.weight(1.1f)
                        )
                        Text(
                            text = part.linearStartAddrHex,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF06B6D4),
                            modifier = Modifier.weight(0.9f)
                        )
                        Text(
                            text = if (part.fileName.isNotEmpty() && part.fileName != "NONE") part.fileName else part.formattedSize,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (part.boundFilePath.isNotEmpty()) Color(0xFF10B981) else Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1.2f)
                        )
                        if (onPickPartitionFile != null) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Select File for ${part.partitionName}",
                                tint = if (part.boundFilePath.isNotEmpty()) Color(0xFF10B981) else Color(0xFF64748B),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .clickable { onPickPartitionFile(index) }
                                    .padding(1.dp)
                            )
                        }
                    }
                    if (index < partitions.size - 1) {
                        HorizontalDivider(color = Color(0xFF1E293B).copy(alpha = 0.4f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpaciousTerminalConsole(
    logs: List<TerminalLog>,
    modifier: Modifier = Modifier,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    var autoScroll by remember { mutableStateOf(true) }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty() && autoScroll) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF050811)),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            // Terminal Header Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0E1524), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                    Text("Live Console Output", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                    Text("(${logs.size} lines)", fontSize = 9.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Copy button
                    IconButton(
                        onClick = {
                            val allLogs = logs.joinToString("\n") { "[${it.timestamp}] ${it.message}" }
                            clipboardManager.setText(AnnotatedString(allLogs))
                            Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Logs", tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                    }
                    // Export/Share button
                    IconButton(
                        onClick = {
                            val allLogs = logs.joinToString("\n") { "[${it.timestamp}] ${it.message}" }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, allLogs)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Export MTK Tool Logs")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export Logs", tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                    }
                    // Clear button
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Logs", tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Spacious Monospace Log Output Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF030509), RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "Console initialized. Waiting for USB OTG connection or command...",
                        fontSize = 9.5.sp,
                        color = Color(0xFF475569),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(4.dp)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(logs) { _, log ->
                            val textColor = when (log.level) {
                                LogLevel.SUCCESS -> Color(0xFF4ADE80)
                                LogLevel.WARNING -> Color(0xFFFBBF24)
                                LogLevel.ERROR -> Color(0xFFF87171)
                                LogLevel.CYAN -> Color(0xFF22D3EE)
                                LogLevel.ACCENT -> Color(0xFF38BDF8)
                                LogLevel.RAW -> Color(0xFFCBD5E1)
                                LogLevel.AI -> Color(0xFFA855F7)
                                LogLevel.MAGENTA -> Color(0xFFE879F9)
                                else -> Color(0xFF94A3B8)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "[${log.timestamp}] ",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = log.message,
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (log.isBold) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandModelSelector(
    selectedBrand: MtkBrand,
    selectedModel: MtkDeviceModel,
    onBrandSelect: (MtkBrand) -> Unit,
    onModelSelect: (MtkDeviceModel) -> Unit
) {
    var expBrand by remember { mutableStateOf(false) }
    var expModel by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Brand
        Box(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF090D16),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth().clickable { expBrand = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedBrand.brandName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                    Text("▼", fontSize = 8.sp, color = Color(0xFF64748B))
                }
            }
            DropdownMenu(
                expanded = expBrand,
                onDismissRequest = { expBrand = false }
            ) {
                MtkDeviceDatabase.brands.forEach { b ->
                    DropdownMenuItem(
                        text = { Text(b.brandName, fontSize = 11.sp) },
                        onClick = {
                            onBrandSelect(b)
                            expBrand = false
                        }
                    )
                }
            }
        }

        // Model
        Box(modifier = Modifier.weight(1.3f)) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF090D16),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth().clickable { expModel = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedModel.modelName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9), maxLines = 1)
                    Text("▼", fontSize = 8.sp, color = Color(0xFF64748B))
                }
            }
            DropdownMenu(
                expanded = expModel,
                onDismissRequest = { expModel = false }
            ) {
                selectedBrand.models.forEach { m ->
                    DropdownMenuItem(
                        text = { Text("${m.modelName} (${m.chipset})", fontSize = 11.sp) },
                        onClick = {
                            onModelSelect(m)
                            expModel = false
                        }
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(3.dp))
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF090D16),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "BROM Guide: ${selectedModel.bromInstruction}",
                    fontSize = 9.sp,
                    color = Color(0xFFCBD5E1),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = Color(0xFF0369A1)
            ) {
                Text(
                    text = selectedModel.chipCode,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun PickerPill(
    label: String,
    icon: ImageVector,
    isLoaded: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isLoaded) Color(0xFF0C4A6E) else Color(0xFF090D16),
        border = BorderStroke(1.dp, if (isLoaded) Color(0xFF0284C7) else Color(0xFF334155)),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (isLoaded) Color(0xFF38BDF8) else Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = if (isLoaded) FontWeight.Bold else FontWeight.Normal,
                color = if (isLoaded) Color(0xFFE0F2FE) else Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BackupOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isSelected) Color(0xFF0369A1) else Color(0xFF090D16),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF26354A)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFF64748B),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Color(0xFFF1F5F9)
                )
                Text(
                    text = subtitle,
                    fontSize = 8.5.sp,
                    color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun ServiceSelectCard(
    title: String,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.25f) else Color(0xFF090D16),
        border = BorderStroke(1.dp, if (isSelected) accentColor else Color(0xFF26354A)),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = if (isSelected) accentColor else Color(0xFF64748B),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FastbootOptionCard(
    title: String,
    cmd: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.3f) else Color(0xFF090D16),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF26354A)),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF64748B),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ToolTopBar(
    currentDestination: AppNavDestination,
    chipInfo: MtkChipInfo?,
    targetPhoneState: TargetPhoneState,
    detectedPortsCount: Int,
    activeLockedMode: com.example.protocol.UsbDeviceMode?,
    isModeIsolationEnabled: Boolean,
    onToggleModeIsolation: (Boolean) -> Unit,
    onOpenDrawer: () -> Unit,
    onScanPorts: () -> Unit,
    onOpenPortSniffer: () -> Unit,
    onOpenBromGuide: () -> Unit,
    onTabSelected: (AppNavDestination) -> Unit
) {
    val navDestinations = remember {
        listOf(
            AppNavDestination.FLASH,
            AppNavDestination.BACKUP,
            AppNavDestination.SERVICE,
            AppNavDestination.FASTBOOT,
            AppNavDestination.ADB,
            AppNavDestination.OTHER
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E1524))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Upper line: Title, Scan Button, Port Status Pill, Guide Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(15.dp))
                }
                Text("MTK UnlockTool", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("v3.0", fontSize = 8.5.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Mode Lock Indicator Pill if an action is currently locking a mode
                if (activeLockedMode != null) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = Color(0xFF7C2D12),
                        border = BorderStroke(1.dp, Color(0xFFEA580C))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Mode Locked", tint = Color(0xFFFDBA74), modifier = Modifier.size(9.dp))
                            Text(
                                text = "${activeLockedMode.label} LOCKED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFEDD5)
                            )
                        }
                    }
                }

                // Quick Scan Ports Button
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.clickable { onScanPorts() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan Ports", tint = Color(0xFF38BDF8), modifier = Modifier.size(11.dp))
                        Text("Scan", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE2E8F0))
                    }
                }

                // BROM Guide Button
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.clickable { onOpenBromGuide() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "BROM Guide", tint = Color(0xFFFBBF24), modifier = Modifier.size(11.dp))
                        Text("Guide", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE2E8F0))
                    }
                }

                // Port Status & Sniffer Pill (Clickable)
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = when (targetPhoneState) {
                        is TargetPhoneState.Connected -> when (targetPhoneState.mode) {
                            com.example.protocol.UsbDeviceMode.BROM -> Color(0xFF166534)
                            com.example.protocol.UsbDeviceMode.FASTBOOT -> Color(0xFF0369A1)
                            com.example.protocol.UsbDeviceMode.ADB -> Color(0xFF0284C7)
                            com.example.protocol.UsbDeviceMode.EDL_9008 -> Color(0xFF991B1B)
                            com.example.protocol.UsbDeviceMode.META -> Color(0xFF6D28D9)
                            com.example.protocol.UsbDeviceMode.SPD_DIAG -> Color(0xFFB45309)
                            else -> Color(0xFF166534)
                        }
                        is TargetPhoneState.RequestingPermission -> Color(0xFF854D0E)
                        is TargetPhoneState.Error -> Color(0xFF991B1B)
                        else -> if (detectedPortsCount > 0) Color(0xFF0369A1) else Color(0xFF1E293B)
                    },
                    modifier = Modifier.clickable { onOpenPortSniffer() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (targetPhoneState is TargetPhoneState.Connected) Color(0xFF4ADE80) else if (detectedPortsCount > 0) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                                )
                        )
                        Text(
                            text = when (targetPhoneState) {
                                is TargetPhoneState.Connected -> "${targetPhoneState.mode.label} OK"
                                is TargetPhoneState.RequestingPermission -> "Perm Req"
                                is TargetPhoneState.Error -> "USB Err"
                                else -> if (detectedPortsCount > 0) "Ports ($detectedPortsCount)" else "Port Sniffer"
                            },
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Quick Tabs Bar (Scrollable)
        val tabScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(tabScrollState)
                .padding(vertical = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            navDestinations.forEach { dest ->
                val isSel = currentDestination == dest
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = if (isSel) Color(0xFF1D4ED8) else Color(0xFF1E293B),
                    modifier = Modifier.clickable { onTabSelected(dest) }
                ) {
                    Text(
                        text = dest.title,
                        fontSize = 9.5.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PortSnifferDialog(
    detectedPorts: List<com.example.protocol.UsbPortInfo>,
    isAutoSnifferActive: Boolean,
    activeLockedMode: com.example.protocol.UsbDeviceMode?,
    isModeIsolationEnabled: Boolean,
    onDismiss: () -> Unit,
    onScanNow: () -> Unit,
    onToggleAutoSniffer: (Boolean) -> Unit,
    onToggleModeIsolation: (Boolean) -> Unit,
    onConnectPort: (com.example.protocol.UsbPortInfo) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Sensors, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    Text("USB Port Scanner & Sniffer", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Auto Sniffer Toggle Card
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Continuous Auto-Sniffing", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                            Text(
                                "Polls USB ports every 350ms to instantly catch BROM mode",
                                fontSize = 8.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Switch(
                            checked = isAutoSnifferActive,
                            onCheckedChange = onToggleAutoSniffer,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF38BDF8),
                                checkedTrackColor = Color(0xFF0369A1),
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }
                }

                // Mode Isolation & Port Lock Toggle Card
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isModeIsolationEnabled) Color(0xFF1E1B4B) else Color(0xFF0F172A),
                    border = BorderStroke(1.dp, if (isModeIsolationEnabled) Color(0xFF6366F1) else Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(13.dp))
                                Text("Mode Isolation & Port Lock", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                            }
                            Text(
                                if (activeLockedMode != null) "ACTIVE LOCK: [${activeLockedMode.label}] (Other modes rejected)"
                                else "Prevents unexpected USB mode interference during active operations",
                                fontSize = 8.5.sp,
                                color = if (activeLockedMode != null) Color(0xFFFDBA74) else Color(0xFF94A3B8)
                            )
                        }
                        Switch(
                            checked = isModeIsolationEnabled,
                            onCheckedChange = onToggleModeIsolation,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFA5B4FC),
                                checkedTrackColor = Color(0xFF4F46E5),
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }
                }

                // Port List Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attached Ports (${detectedPorts.size})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )
                    Button(
                        onClick = onScanNow,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Rescan Ports", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Ports list
                if (detectedPorts.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF090D16),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No USB devices detected on OTG Port.", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF87171))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Connect phone via OTG cable with Volume keys held down.",
                                fontSize = 8.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(detectedPorts) { _, port ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (port.isConnected) Color(0xFF064E3B) else Color(0xFF090D16),
                                border = BorderStroke(1.dp, if (port.isConnected) Color(0xFF10B981) else Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = port.deviceName,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(2.dp),
                                                color = when (port.mode) {
                                                    com.example.protocol.UsbDeviceMode.BROM -> Color(0xFF15803D)
                                                    com.example.protocol.UsbDeviceMode.PRELOADER -> Color(0xFFB45309)
                                                    com.example.protocol.UsbDeviceMode.META -> Color(0xFF7C3AED)
                                                    com.example.protocol.UsbDeviceMode.FASTBOOT -> Color(0xFF0369A1)
                                                    com.example.protocol.UsbDeviceMode.ADB -> Color(0xFF0284C7)
                                                    com.example.protocol.UsbDeviceMode.EDL_9008 -> Color(0xFFDC2626)
                                                    com.example.protocol.UsbDeviceMode.SPD_DIAG -> Color(0xFFD97706)
                                                    else -> Color(0xFF475569)
                                                }
                                            ) {
                                                Text(
                                                    text = port.mode.label,
                                                    fontSize = 7.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "VID:PID: ${port.vidPidHex} | Perm: ${if (port.hasPermission) "Granted" else "Required"}",
                                            fontSize = 8.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    Button(
                                        onClick = { onConnectPort(port) },
                                        enabled = !port.isConnected,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF10B981),
                                            disabledContainerColor = Color(0xFF1E293B)
                                        ),
                                        shape = RoundedCornerShape(3.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text(
                                            text = if (port.isConnected) "Active" else "Claim",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (port.isConnected) Color(0xFF4ADE80) else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF1E293B),
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Close", fontSize = 10.sp, color = Color.White)
            }
        }
    )
}

@Composable
private fun BromConnectionGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                Text("MediaTek BROM Connect Guide", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GuideBrandItem(
                    brand = "Xiaomi / Redmi / POCO",
                    guide = "1. Turn off phone completely.\n2. Hold [Volume Down -] (or Vol+ & Vol- together).\n3. Insert USB OTG Cable.\n4. Release buttons immediately upon BROM Sync!"
                )
                GuideBrandItem(
                    brand = "Oppo / Realme MTK",
                    guide = "1. Turn off phone completely.\n2. Hold [Volume Up +] & [Volume Down -] together.\n3. Insert USB OTG Cable."
                )
                GuideBrandItem(
                    brand = "Vivo MTK",
                    guide = "1. Turn off phone.\n2. Hold [Volume Down -] or [Volume Up +].\n3. Plug USB OTG Cable."
                )
                GuideBrandItem(
                    brand = "Samsung MTK (A0x / A1x / A2x)",
                    guide = "1. Turn off phone completely.\n2. Hold [Volume Up +] & [Volume Down -].\n3. Insert USB OTG Cable."
                )
                GuideBrandItem(
                    brand = "Infinix / Tecno MTK",
                    guide = "1. Power off device.\n2. Hold [Volume Down -] or [Volume Up +].\n3. Plug USB OTG Cable."
                )
                GuideBrandItem(
                    brand = "Hardware TestPoint (TP to GND)",
                    guide = "For secured DAA/SLA or bricked devices:\n1. Disconnect battery.\n2. Short TestPoint (TP) pin to Motherboard GND with tweezers.\n3. Insert USB Cable, then release TP."
                )
            }
        },
        containerColor = Color(0xFF1E293B),
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Got It", fontSize = 10.sp, color = Color.White)
            }
        }
    )
}

@Composable
private fun GuideBrandItem(
    brand: String,
    guide: String
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF090D16),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(brand, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
            Spacer(modifier = Modifier.height(2.dp))
            Text(guide, fontSize = 8.5.sp, color = Color(0xFFCBD5E1), lineHeight = 12.sp)
        }
    }
}

private fun getLogTime(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
}
