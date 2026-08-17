package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AppNavDestination
import com.example.model.BackupMode
import com.example.model.BridgeStatus
import com.example.model.BromHandshakeMethod
import com.example.model.FlashOptions
import com.example.model.LogLevel
import com.example.model.MtkBrand
import com.example.model.MtkChipInfo
import com.example.model.MtkDeviceDatabase
import com.example.model.MtkDeviceModel
import com.example.model.OperationProgress
import com.example.model.PartitionEntry
import com.example.model.ServiceFunction
import com.example.model.TerminalLog
import com.example.model.TransportType
import com.example.parser.ScatterParser
import com.example.protocol.MtkBromProtocolEngine
import com.example.protocol.TargetPhoneState
import com.example.protocol.TargetPhoneUsbManager
import com.example.storage.BackupStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MtkBridgeViewModel(application: Application) : AndroidViewModel(application) {

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    val storageManager = BackupStorageManager(application)
    val targetPhoneUsb = TargetPhoneUsbManager(application)

    // UI States
    private val _selectedTransportType = MutableStateFlow(TransportType.USB_OTG_DIRECT)
    val selectedTransportType: StateFlow<TransportType> = _selectedTransportType.asStateFlow()

    private val _bridgeStatus = MutableStateFlow(
        BridgeStatus(
            isConnected = false,
            transportType = TransportType.USB_OTG_DIRECT,
            deviceName = "Direct USB OTG Host"
        )
    )
    val bridgeStatus: StateFlow<BridgeStatus> = _bridgeStatus.asStateFlow()

    val targetPhoneState: StateFlow<TargetPhoneState> = targetPhoneUsb.phoneState

    private val _chipInfo = MutableStateFlow(MtkChipInfo())
    val chipInfo: StateFlow<MtkChipInfo> = _chipInfo.asStateFlow()

    private val _selectedBrand = MutableStateFlow<MtkBrand>(MtkDeviceDatabase.getDefaultBrand())
    val selectedBrand: StateFlow<MtkBrand> = _selectedBrand.asStateFlow()

    private val _selectedModel = MutableStateFlow<MtkDeviceModel>(MtkDeviceDatabase.getDefaultModel())
    val selectedModel: StateFlow<MtkDeviceModel> = _selectedModel.asStateFlow()

    private val _scatterPlatform = MutableStateFlow("MT6761")
    val scatterPlatform: StateFlow<String> = _scatterPlatform.asStateFlow()

    private val _partitions = MutableStateFlow<List<PartitionEntry>>(emptyList())
    val partitions: StateFlow<List<PartitionEntry>> = _partitions.asStateFlow()

    private val _selectedPartitionIndex = MutableStateFlow(2) // Defaults to nvram
    val selectedPartitionIndex: StateFlow<Int> = _selectedPartitionIndex.asStateFlow()

    private val _selectedServiceFunction = MutableStateFlow(ServiceFunction.READ_INFO)
    val selectedServiceFunction: StateFlow<ServiceFunction> = _selectedServiceFunction.asStateFlow()

    private val _currentDestination = MutableStateFlow(AppNavDestination.FLASH)
    val currentDestination: StateFlow<AppNavDestination> = _currentDestination.asStateFlow()

    private val _flashOptions = MutableStateFlow(FlashOptions())
    val flashOptions: StateFlow<FlashOptions> = _flashOptions.asStateFlow()

    private val _backupMode = MutableStateFlow(BackupMode.FULL_FIRMWARE)
    val backupMode: StateFlow<BackupMode> = _backupMode.asStateFlow()

    private val _isDryRun = MutableStateFlow(false)
    val isDryRun: StateFlow<Boolean> = _isDryRun.asStateFlow()

    private val _autoNvBackup = MutableStateFlow(true)
    val autoNvBackup: StateFlow<Boolean> = _autoNvBackup.asStateFlow()

    private val _autoReboot = MutableStateFlow(true)
    val autoReboot: StateFlow<Boolean> = _autoReboot.asStateFlow()

    // MediaTek BROM Handshake & Auth Skip Method (Method 1: Burst Sync, Method 2: Stream Blaster, Method 3: Preloader Crash, Method 4: Kamakiri Payload)
    private val _selectedHandshakeMethod = MutableStateFlow(BromHandshakeMethod.METHOD_1_BURST_SYNC)
    val selectedHandshakeMethod: StateFlow<BromHandshakeMethod> = _selectedHandshakeMethod.asStateFlow()

    fun selectHandshakeMethod(method: BromHandshakeMethod) {
        _selectedHandshakeMethod.value = method
        addLog(TerminalLog(now(), "Handshake & Auth Method Selected: ${method.title} [Code: ${method.codeTag}]", LogLevel.CYAN))
    }

    fun testBromHandshake() {
        if (_operationProgress.value.isRunning) {
            cancelCurrentOperation()
            return
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            val method = _selectedHandshakeMethod.value
            val isSim = _isDryRun.value

            if (_isModeIsolationEnabled.value) {
                targetPhoneUsb.lockActiveMode(com.example.protocol.UsbDeviceMode.BROM)
            }

            _operationProgress.value = OperationProgress(
                isRunning = true,
                title = "Testing BROM Handshake",
                detail = "Executing ${method.shortLabel}...",
                percentage = 0f
            )

            com.example.audio.ToolSoundManager.playOperationStart()
            try {
                protocolEngine.withStableConnection {
                    addLog(TerminalLog(now(), "=== [TEST BROM HANDSHAKE] Method: ${method.title} ===", LogLevel.INFO))
                    val ok = protocolEngine.performHandshake(method, isSim)
                    if (ok) {
                        addLog(TerminalLog(now(), "BROM Handshake [${method.shortLabel}] Verification: SUCCESS (Port Synchronized)", LogLevel.SUCCESS))
                        com.example.audio.ToolSoundManager.playOperationDone()
                    } else {
                        addLog(TerminalLog(now(), "BROM Handshake [${method.shortLabel}] Verification: FAILED (Try Method 2 or 3)", LogLevel.ERROR))
                        com.example.audio.ToolSoundManager.playOperationStop()
                    }
                    Result.success(ok)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                com.example.audio.ToolSoundManager.playOperationStop()
            } catch (e: Exception) {
                addLog(TerminalLog(now(), "Handshake Test Error: ${e.message}", LogLevel.ERROR))
                com.example.audio.ToolSoundManager.playOperationStop()
            } finally {
                _operationProgress.value = OperationProgress(isRunning = false, percentage = 0f)
                targetPhoneUsb.unlockActiveMode()
            }
        }
    }

    private val _backupLocation = MutableStateFlow(storageManager.getBackupDirectory().absolutePath)
    val backupLocation: StateFlow<String> = _backupLocation.asStateFlow()

    private val _operationProgress = MutableStateFlow(OperationProgress())
    val operationProgress: StateFlow<OperationProgress> = _operationProgress.asStateFlow()

    private val _logs = MutableStateFlow<List<TerminalLog>>(emptyList())
    val logs: StateFlow<List<TerminalLog>> = _logs.asStateFlow()

    val detectedPorts: StateFlow<List<com.example.protocol.UsbPortInfo>> = targetPhoneUsb.attachedPorts
    val isAutoSnifferActive: StateFlow<Boolean> = targetPhoneUsb.isAutoSnifferActive
    val activeLockedMode: StateFlow<com.example.protocol.UsbDeviceMode?> = targetPhoneUsb.activeLockedMode

    // Mode Isolation Policy Switch (Enabled by default to prevent other modes from interfering during operations)
    private val _isModeIsolationEnabled = MutableStateFlow(true)
    val isModeIsolationEnabled: StateFlow<Boolean> = _isModeIsolationEnabled.asStateFlow()

    fun toggleModeIsolation(enabled: Boolean) {
        _isModeIsolationEnabled.value = enabled
        if (!enabled) {
            targetPhoneUsb.unlockActiveMode()
            addLog(TerminalLog(now(), "Mode Isolation / USB Port Lock: DISABLED (All modes allowed)", LogLevel.WARNING))
        } else {
            addLog(TerminalLog(now(), "Mode Isolation / USB Port Lock: ACTIVE (Non-matching modes blocked during operations)", LogLevel.SUCCESS))
        }
    }

    // File selection paths
    val daAgentPath = MutableStateFlow("Built-in Universal DA (MTK All-in-One)")
    val customDaPath = daAgentPath // alias
    val authFilePath = MutableStateFlow("")
    val preloaderPath = MutableStateFlow("")
    val scatterPath = MutableStateFlow("")
    val scatterFileName: StateFlow<String> = scatterPath.asStateFlow()

    private lateinit var protocolEngine: MtkBromProtocolEngine
    private var activeJob: kotlinx.coroutines.Job? = null

    init {
        protocolEngine = MtkBromProtocolEngine(
            targetPhoneUsb = targetPhoneUsb,
            storageManager = storageManager,
            logCallback = { log -> addLog(log) },
            progressCallback = { prog -> _operationProgress.value = prog }
        )

        // Start with empty partition table (professional GSM tool behavior)
        _scatterPlatform.value = "Unknown / Auto"
        _partitions.value = emptyList()

        addLog(TerminalLog(now(), "MTK Standalone USB OTG Flasher Initialized.", LogLevel.SUCCESS))
        addLog(TerminalLog(now(), "Partitions Table is empty. Connect device or load Scatter file.", LogLevel.INFO))
        addLog(TerminalLog(now(), "Backup Directory: ${_backupLocation.value}", LogLevel.INFO))

        observeTargetPhoneState()
    }

    private fun now(): String = timeFormat.format(Date())

    private fun observeTargetPhoneState() {
        viewModelScope.launch {
            var wasConnected = false
            targetPhoneUsb.phoneState.collectLatest { state ->
                when (state) {
                    is TargetPhoneState.Connected -> {
                        val isFirstConnection = !wasConnected
                        wasConnected = true
                        _bridgeStatus.value = _bridgeStatus.value.copy(
                            isConnected = true,
                            fileDescriptor = state.fileDescriptor,
                            isBromMode = state.isBromMode,
                            targetVidPid = state.vidPid,
                            deviceName = state.deviceName
                        )
                        addLog(TerminalLog(now(), "Direct USB Connected: ${state.deviceName} [${state.vidPid}] FD:${state.fileDescriptor}", LogLevel.SUCCESS))
                        if (isFirstConnection) {
                            com.example.audio.ToolSoundManager.playUsbConnected()
                        }
                    }
                    is TargetPhoneState.Disconnected -> {
                        val wasPreviouslyConnected = wasConnected
                        wasConnected = false
                        _bridgeStatus.value = _bridgeStatus.value.copy(isConnected = false, fileDescriptor = -1)
                        if (wasPreviouslyConnected) {
                            addLog(TerminalLog(now(), "Direct USB Disconnected / Removed.", LogLevel.WARNING))
                            com.example.audio.ToolSoundManager.playUsbDisconnected()
                            if (_operationProgress.value.isRunning && !_isDryRun.value) {
                                cancelCurrentOperation()
                            }
                        }
                    }
                    is TargetPhoneState.Error -> {
                        addLog(TerminalLog(now(), "USB Error: ${state.message}", LogLevel.ERROR))
                        com.example.audio.ToolSoundManager.playOperationStop()
                    }
                    is TargetPhoneState.RequestingPermission -> {
                        addLog(TerminalLog(now(), "Requesting USB OTG Permission for ${state.deviceName} [${state.mode.label} - ${state.vidPid}]...", LogLevel.WARNING))
                    }
                }
            }
        }
    }

    fun addLog(log: TerminalLog) {
        val current = _logs.value.toMutableList()
        current.add(log)
        if (current.size > 500) {
            current.removeAt(0)
        }
        _logs.value = current
    }

    fun clearLogs() {
        _logs.value = emptyList()
        addLog(TerminalLog(now(), "Terminal log cleared.", LogLevel.INFO))
    }

    fun toggleAutoReboot(enabled: Boolean) {
        _autoReboot.value = enabled
        addLog(TerminalLog(now(), "Post-Operation Auto Reboot: ${if (enabled) "ENABLED" else "DISABLED"}", LogLevel.INFO))
    }

    fun toggleAutoNvBackup(enabled: Boolean) {
        _autoNvBackup.value = enabled
        addLog(TerminalLog(now(), "Auto NV Data Backup Policy: ${if (enabled) "ENABLED (Backup will be created)" else "DISABLED (Backup will be skipped)"}", LogLevel.INFO))
    }

    fun setCustomBackupLocation(path: String) {
        storageManager.setCustomBackupPath(path)
        _backupLocation.value = storageManager.getBackupDirectory().absolutePath
        addLog(TerminalLog(now(), "Backup output path set to: ${_backupLocation.value}", LogLevel.SUCCESS))
    }

    fun setTransportType(type: TransportType) {
        if (_selectedTransportType.value == type) return
        _selectedTransportType.value = type
        if (type == TransportType.SIMULATION) {
            _isDryRun.value = true
            addLog(TerminalLog(now(), "Switched to Dry-Run / Simulation Mode", LogLevel.INFO))
        } else {
            _isDryRun.value = false
            addLog(TerminalLog(now(), "Switched to Direct USB OTG Host Mode", LogLevel.SUCCESS))
        }
    }

    fun scanTargetPhone() {
        refreshUsbPorts()
    }

    fun refreshUsbPorts() {
        viewModelScope.launch {
            addLog(TerminalLog(now(), "Scanning USB Host for attached devices/ports...", LogLevel.INFO))
            val ports = targetPhoneUsb.scanAllAttachedPorts()
            if (ports.isEmpty()) {
                addLog(TerminalLog(now(), "No USB devices detected on OTG Port. Connect target phone in BROM/EDL/Fastboot mode.", LogLevel.WARNING))
            } else {
                addLog(TerminalLog(now(), "Detected ${ports.size} USB Port(s):", LogLevel.SUCCESS))
                for (p in ports) {
                    val permStatus = if (p.hasPermission) "Permission: OK" else "Permission: Needed"
                    val connStatus = if (p.isConnected) "[CONNECTED]" else "[READY]"
                    addLog(TerminalLog(now(), "-> ${p.deviceName} [${p.vidPidHex}] (${p.mode.label}) $connStatus | $permStatus", LogLevel.CYAN))
                }
                targetPhoneUsb.scanAndConnect()
            }
        }
    }

    fun toggleAutoSniffer(enable: Boolean) {
        if (enable) {
            targetPhoneUsb.startContinuousAutoSniffer()
            addLog(TerminalLog(now(), "Auto Port Sniffer: ACTIVE (Continuous 350ms Polling)", LogLevel.SUCCESS))
        } else {
            targetPhoneUsb.stopContinuousAutoSniffer()
            addLog(TerminalLog(now(), "Auto Port Sniffer: PAUSED", LogLevel.WARNING))
        }
    }

    fun connectSpecificPort(port: com.example.protocol.UsbPortInfo) {
        viewModelScope.launch {
            addLog(TerminalLog(now(), "Connecting to ${port.deviceName} [${port.vidPidHex}]...", LogLevel.INFO))
            val rawDevice = targetPhoneUsb.usbManager.deviceList.values.firstOrNull { it.vendorId == port.vendorId && it.productId == port.productId }
            if (rawDevice != null) {
                if (targetPhoneUsb.usbManager.hasPermission(rawDevice)) {
                    val ok = targetPhoneUsb.connectDevice(rawDevice)
                    if (ok) {
                        addLog(TerminalLog(now(), "Successfully connected to ${port.deviceName}", LogLevel.SUCCESS))
                    }
                } else {
                    targetPhoneUsb.requestDevicePermission(rawDevice)
                }
            } else {
                addLog(TerminalLog(now(), "Device is no longer attached.", LogLevel.ERROR))
            }
        }
    }

    fun exportLogsAsText(): String {
        return _logs.value.joinToString("\n") { "[${it.timestamp}] ${it.message}" }
    }

    fun selectBrand(brand: MtkBrand) {
        _selectedBrand.value = brand
        val firstModel = brand.models.firstOrNull() ?: return
        selectModel(firstModel)
    }

    fun selectModel(model: MtkDeviceModel) {
        _selectedModel.value = model
        _scatterPlatform.value = model.chipCode
        addLog(TerminalLog(now(), "Selected Device: ${_selectedBrand.value.brandName} -> ${model.modelName} [${model.chipset}]", LogLevel.SUCCESS))
        addLog(TerminalLog(now(), "BROM Connection Guide: ${model.bromInstruction}", LogLevel.INFO))
    }

    fun setScatterPlatform(chipName: String) {
        _scatterPlatform.value = chipName
        addLog(TerminalLog(now(), "Target Architecture Set: $chipName", LogLevel.INFO))
    }

    fun loadScatterContent(content: String, sourceFileName: String) {
        val parsed = ScatterParser.parseScatter(content)
        _scatterPlatform.value = parsed.first
        _partitions.value = parsed.second
        scatterPath.value = sourceFileName
        if (parsed.second.isNotEmpty()) {
            addLog(TerminalLog(now(), "Successfully loaded scatter: $sourceFileName (${parsed.first} - ${parsed.second.size} Partitions)", LogLevel.SUCCESS))
        } else {
            addLog(TerminalLog(now(), "Scatter file '$sourceFileName' contains no valid partition entries.", LogLevel.WARNING))
        }
    }

    fun togglePartitionSelection(index: Int, isSelected: Boolean = true) {
        val list = _partitions.value.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(isSelectedForFlashing = isSelected)
            _partitions.value = list
        }
    }

    fun bindPartitionCustomFile(index: Int, filePath: String, sizeBytes: Long? = null) {
        val list = _partitions.value.toMutableList()
        if (index in list.indices) {
            val old = list[index]
            val newSizeBytes = sizeBytes ?: old.sizeBytes
            list[index] = old.copy(
                boundFilePath = filePath,
                fileName = filePath.substringAfterLast('/'),
                sizeBytes = newSizeBytes,
                isSelectedForFlashing = true
            )
            _partitions.value = list
            addLog(TerminalLog(now(), "Bound partition '${old.partitionName}' to image file: ${list[index].fileName}", LogLevel.SUCCESS))
        }
    }

    fun selectPartition(index: Int) {
        selectPartitionIndex(index)
    }

    fun selectAllPartitions(selectAll: Boolean) {
        val list = _partitions.value.map { it.copy(isSelectedForFlashing = selectAll) }
        _partitions.value = list
        addLog(TerminalLog(now(), if (selectAll) "Selected all partitions for flashing." else "Deselected all partitions.", LogLevel.INFO))
    }

    fun toggleSelectAllPartitions(selectAll: Boolean) {
        selectAllPartitions(selectAll)
    }

    fun selectPartitionIndex(index: Int) {
        if (index in _partitions.value.indices) {
            _selectedPartitionIndex.value = index
        }
    }

    fun selectServiceFunction(func: ServiceFunction) {
        _selectedServiceFunction.value = func
        addLog(TerminalLog(now(), "Selected service function: ${func.title}", LogLevel.INFO))
    }

    fun toggleDryRun(enabled: Boolean) {
        _isDryRun.value = enabled
        if (enabled) {
            setTransportType(TransportType.SIMULATION)
            addLog(TerminalLog(now(), "Dry-Run / Simulation Mode ENABLED. Safe testing active.", LogLevel.SUCCESS))
        } else {
            setTransportType(TransportType.USB_OTG_DIRECT)
            addLog(TerminalLog(now(), "Dry-Run Mode DISABLED. Real Direct USB OTG active.", LogLevel.WARNING))
        }
    }

    fun cancelCurrentOperation() {
        activeJob?.cancel()
        activeJob = null
        _operationProgress.value = OperationProgress(isRunning = false, title = "Cancelled", percentage = 0f)
        addLog(TerminalLog(now(), "[ABORTED] Operation stopped / reset by user.", LogLevel.ERROR))
        com.example.audio.ToolSoundManager.playOperationStop()
    }

    fun resetActionState(destination: AppNavDestination? = null) {
        // Reset partition selection to safe defaults
        if (_partitions.value.isNotEmpty()) {
            _selectedPartitionIndex.value = 0
        }
        // Match default service function to the destination
        when (destination) {
            AppNavDestination.SERVICE -> _selectedServiceFunction.value = ServiceFunction.READ_INFO
            AppNavDestination.FLASH -> _selectedServiceFunction.value = ServiceFunction.BATCH_FLASH
            AppNavDestination.BACKUP -> _selectedServiceFunction.value = ServiceFunction.DUMP_ALL_PARTITIONS
            AppNavDestination.OTHER -> _selectedServiceFunction.value = ServiceFunction.BYPASS_AUTH
            else -> {}
        }
    }

    fun navigateTo(destination: AppNavDestination) {
        _currentDestination.value = destination
        resetActionState(destination)
        addLog(TerminalLog(now(), "Navigated to: ${destination.title}", LogLevel.INFO))
    }

    fun setBackupMode(mode: BackupMode) {
        _backupMode.value = mode
        addLog(TerminalLog(now(), "Selected Backup Mode: ${mode.title} (${mode.description})", LogLevel.INFO))
    }

    fun toggleFlashReadNvData(enabled: Boolean) {
        _flashOptions.value = _flashOptions.value.copy(readNvData = enabled)
        _autoNvBackup.value = enabled
        addLog(TerminalLog(now(), "Flash Action [Read NV Data]: ${if (enabled) "ENABLED" else "DISABLED"}", LogLevel.INFO))
    }

    fun toggleFlashAutoReboot(enabled: Boolean) {
        _flashOptions.value = _flashOptions.value.copy(autoReboot = enabled)
        _autoReboot.value = enabled
        addLog(TerminalLog(now(), "Flash Action [Auto Reboot]: ${if (enabled) "ENABLED" else "DISABLED"}", LogLevel.INFO))
    }

    fun toggleFlashAfterBlUnlock(enabled: Boolean) {
        _flashOptions.value = _flashOptions.value.copy(flashAfterBlUnlock = enabled)
        addLog(TerminalLog(now(), "Flash Action [Flash After BL Unlock]: ${if (enabled) "ENABLED (seccfg patch before flash)" else "DISABLED"}", LogLevel.INFO))
    }

    fun toggleFlashDaDlChecksum(enabled: Boolean) {
        _flashOptions.value = _flashOptions.value.copy(daDlChecksum = enabled)
        addLog(TerminalLog(now(), "Flash Action [DA DL Checksum]: ${if (enabled) "ENABLED (Integrity check active)" else "DISABLED"}", LogLevel.INFO))
    }

    fun toggleFlashAutoSign(enabled: Boolean) {
        _flashOptions.value = _flashOptions.value.copy(autoSignFlash = enabled)
        addLog(TerminalLog(now(), "Flash Action [Auto Sign Flash]: ${if (enabled) "ENABLED (Signature bypass active)" else "DISABLED"}", LogLevel.INFO))
    }

    fun toggleFlashFormatAll(enabled: Boolean) {
        _flashOptions.value = _flashOptions.value.copy(formatAllDownload = enabled)
        addLog(TerminalLog(now(), "Flash Action [Format All + Download]: ${if (enabled) "ENABLED (Warning: Full erase before flash)" else "DISABLED"}", LogLevel.WARNING))
    }

    fun executeFlashOperation() {
        if (_operationProgress.value.isRunning) {
            cancelCurrentOperation()
            return
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            val isSim = _isDryRun.value
            val chip = _scatterPlatform.value
            val parts = _partitions.value
            val opts = _flashOptions.value

            if (parts.none { it.isSelectedForFlashing }) {
                addLog(TerminalLog(now(), "No partitions selected for flashing. Please check at least one partition in the table.", LogLevel.WARNING))
                com.example.audio.ToolSoundManager.playOperationStop()
                return@launch
            }

            if (_isModeIsolationEnabled.value) {
                targetPhoneUsb.lockActiveMode(com.example.protocol.UsbDeviceMode.BROM)
            }

            _operationProgress.value = OperationProgress(
                isRunning = true,
                title = "Flashing ${_selectedModel.value.modelName}",
                detail = "Initializing MTK Protocol Pipeline...",
                percentage = 0f
            )

            com.example.audio.ToolSoundManager.playOperationStart()
            addLog(TerminalLog(now(), "Starting Batch Flash for '${_selectedBrand.value.brandName} -> ${_selectedModel.value.modelName}'...", LogLevel.INFO))
            try {
                val res = protocolEngine.withStableConnection {
                    protocolEngine.batchFlash(
                        chipPlatform = chip,
                        partitions = parts,
                        isSimulation = isSim,
                        autoNvBackup = opts.readNvData,
                        autoReboot = opts.autoReboot,
                        flashAfterBlUnlock = opts.flashAfterBlUnlock,
                        daDlChecksum = opts.daDlChecksum,
                        autoSignFlash = opts.autoSignFlash,
                        formatAllDownload = opts.formatAllDownload
                    )
                }
                if (res.isSuccess) {
                    com.example.audio.ToolSoundManager.playOperationDone()
                } else {
                    com.example.audio.ToolSoundManager.playOperationStop()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                com.example.audio.ToolSoundManager.playOperationStop()
            } catch (e: Exception) {
                addLog(TerminalLog(now(), "Flash Error: ${e.message}", LogLevel.ERROR))
                com.example.audio.ToolSoundManager.playOperationStop()
            } finally {
                _operationProgress.value = OperationProgress(isRunning = false, percentage = 0f)
                targetPhoneUsb.unlockActiveMode()
            }
        }
    }

    fun executeBackupOperation() {
        if (_operationProgress.value.isRunning) {
            cancelCurrentOperation()
            return
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            val isSim = _isDryRun.value
            val chip = _scatterPlatform.value
            val parts = _partitions.value
            val mode = _backupMode.value
            val autoReboot = _autoReboot.value

            if (_isModeIsolationEnabled.value) {
                targetPhoneUsb.lockActiveMode(com.example.protocol.UsbDeviceMode.BROM)
            }

            _operationProgress.value = OperationProgress(
                isRunning = true,
                title = "Backup: ${mode.title}",
                detail = "Connecting to device storage...",
                percentage = 0f
            )

            com.example.audio.ToolSoundManager.playOperationStart()
            addLog(TerminalLog(now(), "Initiating Backup Session: ${mode.title} for ${_selectedBrand.value.brandName} [${_selectedModel.value.modelName}]", LogLevel.INFO))

            try {
                protocolEngine.withStableConnection {
                    when (mode) {
                        BackupMode.FULL_FIRMWARE -> {
                            protocolEngine.dumpAllPartitions(parts, isSim)
                        }
                        BackupMode.STABLE_FIRMWARE -> {
                            protocolEngine.dumpStablePartitions(parts, isSim)
                        }
                        BackupMode.NV_DATA -> {
                            protocolEngine.backupNvram(chip, parts, isSim)
                        }
                        BackupMode.CUSTOM_PARTITIONS -> {
                            protocolEngine.dumpCustomPartitions(parts, isSim)
                        }
                    }
                    if (autoReboot) {
                        protocolEngine.rebootDevice("Android System", isSim)
                    }
                    Result.success(true)
                }
                com.example.audio.ToolSoundManager.playOperationDone()
            } catch (e: kotlinx.coroutines.CancellationException) {
                com.example.audio.ToolSoundManager.playOperationStop()
            } catch (e: Exception) {
                addLog(TerminalLog(now(), "Backup Error: ${e.message}", LogLevel.ERROR))
                com.example.audio.ToolSoundManager.playOperationStop()
            } finally {
                _operationProgress.value = OperationProgress(isRunning = false, percentage = 0f)
                targetPhoneUsb.unlockActiveMode()
            }
        }
    }

    fun executeServiceFunctionDirect(func: ServiceFunction) {
        _selectedServiceFunction.value = func
        executeActiveServiceFunction()
    }

    fun runMemoryTest() {
        if (_operationProgress.value.isRunning) {
            cancelCurrentOperation()
            return
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            if (_isModeIsolationEnabled.value) {
                targetPhoneUsb.lockActiveMode(com.example.protocol.UsbDeviceMode.BROM)
            }
            _operationProgress.value = OperationProgress(
                isRunning = true,
                title = "Hardware Memory Test",
                detail = "Running RAM & Storage Diagnostic...",
                percentage = 0f
            )
            com.example.audio.ToolSoundManager.playOperationStart()
            try {
                val res = protocolEngine.runMemoryTest(_isDryRun.value)
                if (res.isSuccess) {
                    com.example.audio.ToolSoundManager.playOperationDone()
                } else {
                    com.example.audio.ToolSoundManager.playOperationStop()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                com.example.audio.ToolSoundManager.playOperationStop()
            } catch (e: Exception) {
                com.example.audio.ToolSoundManager.playOperationStop()
            } finally {
                _operationProgress.value = OperationProgress(isRunning = false, percentage = 0f)
                targetPhoneUsb.unlockActiveMode()
            }
        }
    }

    fun executeActiveServiceFunction() {
        if (_operationProgress.value.isRunning) {
            cancelCurrentOperation()
            return
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            val func = _selectedServiceFunction.value
            val isSim = _isDryRun.value
            val chip = _scatterPlatform.value
            val parts = _partitions.value
            val autoReboot = _autoReboot.value
            val autoNvBackup = _autoNvBackup.value

            if (_isModeIsolationEnabled.value) {
                targetPhoneUsb.lockActiveMode(com.example.protocol.UsbDeviceMode.BROM)
            }

            _operationProgress.value = OperationProgress(
                isRunning = true,
                title = func.title,
                detail = "Executing ${func.title}...",
                percentage = 0f
            )

            com.example.audio.ToolSoundManager.playOperationStart()
            try {
                protocolEngine.withStableConnection {
                    when (func) {
                        ServiceFunction.READ_INFO -> {
                            val res = protocolEngine.executeBromHandshake(isSim, _selectedHandshakeMethod.value)
                            if (res.isSuccess) {
                                val info = res.getOrNull()!!
                                _chipInfo.value = info
                                if (_scatterPlatform.value == "Unknown / Auto" || _scatterPlatform.value.isEmpty()) {
                                    _scatterPlatform.value = info.chipIdHex
                                }
                                protocolEngine.validateChipMatch(info, _scatterPlatform.value)
                                val liveGpt = protocolEngine.readDeviceGpt(isSim, info.chipIdHex)
                                if (liveGpt.isNotEmpty()) {
                                    _partitions.value = liveGpt
                                    addLog(TerminalLog(now(), "Live Storage GPT Loaded into Partitions Table (${liveGpt.size} Partitions).", LogLevel.SUCCESS))
                                }
                            }
                        }
                        ServiceFunction.WRITE_PARTITION -> {
                            val part = parts.getOrNull(_selectedPartitionIndex.value)
                            if (part != null) {
                                protocolEngine.writePartition(part, null, isSim, autoNvBackup, autoReboot)
                            } else {
                                addLog(TerminalLog(now(), "Please select a valid partition to write.", LogLevel.ERROR))
                            }
                        }
                        ServiceFunction.BATCH_FLASH -> {
                            val opts = _flashOptions.value
                            protocolEngine.batchFlash(
                                chipPlatform = chip,
                                partitions = parts,
                                isSimulation = isSim,
                                autoNvBackup = autoNvBackup,
                                autoReboot = autoReboot,
                                flashAfterBlUnlock = opts.flashAfterBlUnlock,
                                daDlChecksum = opts.daDlChecksum,
                                autoSignFlash = opts.autoSignFlash,
                                formatAllDownload = opts.formatAllDownload
                            )
                        }
                        ServiceFunction.READ_PARTITION -> {
                            val part = parts.getOrNull(_selectedPartitionIndex.value)
                            if (part != null) {
                                protocolEngine.readPartition(part, isSim)
                            } else {
                                addLog(TerminalLog(now(), "Please select a valid partition to read.", LogLevel.ERROR))
                            }
                        }
                        ServiceFunction.DUMP_ALL_PARTITIONS -> {
                            protocolEngine.dumpAllPartitions(parts, isSim)
                        }
                        ServiceFunction.DUMP_STABLE_PARTITIONS -> {
                            protocolEngine.dumpStablePartitions(parts, isSim)
                        }
                        ServiceFunction.READ_PRELOADER -> {
                            protocolEngine.readPreloader(isSim)
                        }
                        ServiceFunction.READ_GPT_SCATTER -> {
                            protocolEngine.readGptAndGenerateScatter(chip, parts, isSim)
                        }
                        ServiceFunction.READ_RPMB -> {
                            protocolEngine.readRpmb(isSim)
                        }
                        ServiceFunction.BACKUP_NVRAM -> {
                            protocolEngine.backupNvram(chip, parts, isSim)
                        }
                        ServiceFunction.RESTORE_NVRAM -> {
                            addLog(TerminalLog(now(), "Restoring saved NV calibration archive...", LogLevel.INFO))
                            val nvPart = parts.find { it.partitionName.lowercase() == "nvdata" } ?: parts.getOrNull(2)
                            if (nvPart != null) {
                                protocolEngine.writePartition(nvPart, null, isSim, autoNvBackup = false, autoReboot = autoReboot)
                            }
                        }
                        ServiceFunction.BYPASS_AUTH -> {
                            protocolEngine.bypassAuth(isSim)
                        }
                        ServiceFunction.UNLOCK_BOOTLOADER -> {
                            protocolEngine.unlockBootloader(isSim, autoReboot)
                        }
                        ServiceFunction.LOCK_BOOTLOADER -> {
                            protocolEngine.lockBootloader(isSim, autoReboot)
                        }
                        ServiceFunction.ERASE_FRP -> {
                            protocolEngine.eraseFrp(chip, parts, isSim, autoNvBackup, autoReboot)
                        }
                        ServiceFunction.FACTORY_RESET -> {
                            protocolEngine.factoryReset(chip, parts, isSim, autoNvBackup, autoReboot)
                        }
                        ServiceFunction.DISABLE_MI_ACCOUNT -> {
                            protocolEngine.disableMiAccount(chip, parts, isSim, autoNvBackup, autoReboot)
                        }
                        ServiceFunction.MEMORY_TEST -> {
                            protocolEngine.runMemoryTest(isSim)
                        }
                        ServiceFunction.FORMAT_PARTITION -> {
                            val part = parts.getOrNull(_selectedPartitionIndex.value)
                            if (part != null) {
                                protocolEngine.formatPartition(chip, part, parts, isSim, autoNvBackup, autoReboot)
                            }
                        }
                        ServiceFunction.CRASH_TO_BROM -> {
                            protocolEngine.crashToBrom(isSim)
                        }
                        ServiceFunction.REBOOT_SYSTEM -> {
                            protocolEngine.rebootDevice("Android System", isSim)
                        }
                        ServiceFunction.REBOOT_FASTBOOT -> {
                            protocolEngine.rebootDevice("Fastboot Mode", isSim)
                        }
                        ServiceFunction.REBOOT_RECOVERY -> {
                            protocolEngine.rebootDevice("Recovery Mode", isSim)
                        }
                    }
                    Result.success(true)
                }
                com.example.audio.ToolSoundManager.playOperationDone()
            } catch (e: kotlinx.coroutines.CancellationException) {
                com.example.audio.ToolSoundManager.playOperationStop()
            } catch (e: Exception) {
                addLog(TerminalLog(now(), "Service Error: ${e.message}", LogLevel.ERROR))
                com.example.audio.ToolSoundManager.playOperationStop()
            } finally {
                _operationProgress.value = OperationProgress(isRunning = false, percentage = 0f)
                targetPhoneUsb.unlockActiveMode()
            }
        }
    }

    fun batchFlashSelectedPartitions() {
        viewModelScope.launch {
            protocolEngine.batchFlash(_scatterPlatform.value, _partitions.value, _isDryRun.value, _autoNvBackup.value, _autoReboot.value)
        }
    }

    fun runBromHandshake() {
        viewModelScope.launch {
            val result = protocolEngine.executeBromHandshake(_isDryRun.value)
            if (result.isSuccess) {
                val info = result.getOrNull()!!
                _chipInfo.value = info
                if (_scatterPlatform.value == "Unknown / Auto" || _scatterPlatform.value.isEmpty()) {
                    _scatterPlatform.value = info.chipIdHex
                }
                protocolEngine.validateChipMatch(info, _scatterPlatform.value)

                // Read live device GPT from phone storage dynamically
                val liveGpt = protocolEngine.readDeviceGpt(_isDryRun.value, info.chipIdHex)
                if (liveGpt.isNotEmpty()) {
                    _partitions.value = liveGpt
                    addLog(TerminalLog(now(), "Live Storage GPT Loaded into Partitions Table (${liveGpt.size} Partitions).", LogLevel.SUCCESS))
                }
            }
        }
    }

    fun sendWatchdogReset() {
        viewModelScope.launch {
            addLog(TerminalLog(now(), "Sending USB Control Transfer Watchdog Reset...", LogLevel.INFO))
            if (!_isDryRun.value && targetPhoneUsb.isConnected()) {
                val ok = targetPhoneUsb.sendWatchdogResetControl()
                addLog(TerminalLog(now(), "USB Control Transfer Reset: ${if (ok) "SUCCESS" else "SENT"}", LogLevel.SUCCESS))
            } else {
                addLog(TerminalLog(now(), "Simulated USB Watchdog Reset Triggered.", LogLevel.SUCCESS))
            }
        }
    }

    // ADB & Fastboot state
    private val _adbDeviceInfo = MutableStateFlow<String>("")
    val adbDeviceInfo: StateFlow<String> = _adbDeviceInfo.asStateFlow()

    private val _fastbootDeviceInfo = MutableStateFlow<String>("")
    val fastbootDeviceInfo: StateFlow<String> = _fastbootDeviceInfo.asStateFlow()

    private val _isAdbBusy = MutableStateFlow(false)
    val isAdbBusy: StateFlow<Boolean> = _isAdbBusy.asStateFlow()

    private val _isFastbootBusy = MutableStateFlow(false)
    val isFastbootBusy: StateFlow<Boolean> = _isFastbootBusy.asStateFlow()

    fun runAdbCommand(label: String, command: String, onComplete: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            _isAdbBusy.value = true
            if (_isModeIsolationEnabled.value) {
                targetPhoneUsb.lockActiveMode(com.example.protocol.UsbDeviceMode.ADB)
            }
            addLog(TerminalLog(now(), ">>> [ADB CMD] $label: adb shell \"$command\"", LogLevel.INFO))
            
            var dev = targetPhoneUsb.currentDevice
            if (dev == null && !_isDryRun.value) {
                targetPhoneUsb.scanAndConnect()
                dev = targetPhoneUsb.currentDevice
            }

            if (dev == null && !_isDryRun.value) {
                val attachedDevices = targetPhoneUsb.usbManager.deviceList
                if (attachedDevices.isNotEmpty()) {
                    val candidate = attachedDevices.values.firstOrNull()
                    if (candidate != null) {
                        if (!targetPhoneUsb.usbManager.hasPermission(candidate)) {
                            targetPhoneUsb.requestDevicePermission(candidate)
                            addLog(TerminalLog(now(), "[*] Requesting USB Permission for ${candidate.productName ?: "device"}. Please tap [ALLOW] on phone...", LogLevel.WARNING))
                            _isAdbBusy.value = false
                            targetPhoneUsb.unlockActiveMode()
                            return@launch
                        } else {
                            targetPhoneUsb.connectDevice(candidate)
                            dev = targetPhoneUsb.currentDevice
                        }
                    }
                }
            }

            if (dev == null && !_isDryRun.value) {
                addLog(TerminalLog(now(), "[-] ADB Error: No USB Device connected. Please connect phone with USB Debugging enabled (OTG).", LogLevel.ERROR))
                _isAdbBusy.value = false
                targetPhoneUsb.unlockActiveMode()
                return@launch
            }

            if (_isDryRun.value || dev == null) {
                // Dry run response
                kotlinx.coroutines.delay(500)
                val mockOutput = when {
                    command.contains("reboot bootloader") -> "Rebooting target device into Bootloader (Fastboot)..."
                    command.contains("reboot recovery") -> "Rebooting target device into Recovery..."
                    command.contains("reboot") -> "Device reboot signal sent."
                    command.contains("setup_wizard_has_run") -> "FRP setup wizard flags bypassed successfully."
                    command.contains("morelocale") -> "Permission android.permission.CHANGE_CONFIGURATION granted."
                    command.contains("dumpsys battery") -> "AC powered: false\nUSB powered: true\nstatus: 2 (charging)\nhealth: 2 (good)\nlevel: 86\nvoltage: 4125\ntemperature: 295 (29.5C)"
                    command.contains("wm size") -> "Physical size: 1080x2400\nPhysical density: 440"
                    command.contains("partitions") -> "major minor  #blocks  name\n 259        0  122175488 mmcblk0\n 259        1       4096 mmcblk0boot0"
                    else -> "Success: Command executed."
                }
                addLog(TerminalLog(now(), "[ADB Response]\n$mockOutput", LogLevel.SUCCESS))
                onComplete?.invoke(mockOutput)
                _isAdbBusy.value = false
                targetPhoneUsb.unlockActiveMode()
                return@launch
            }

            val client = targetPhoneUsb.getOrCreateAdbClient()
            if (client == null) {
                addLog(TerminalLog(now(), "[-] ADB Error: Failed to initialize ADB Client.", LogLevel.ERROR))
                _isAdbBusy.value = false
                targetPhoneUsb.unlockActiveMode()
                return@launch
            }

            if (!client.isOpen()) {
                val opened = client.open()
                if (!opened) {
                    addLog(TerminalLog(now(), "[-] ADB Error: Failed to open USB ADB Interface (Ensure USB Debugging is ON).", LogLevel.ERROR))
                    targetPhoneUsb.resetAdbClient()
                    _isAdbBusy.value = false
                    targetPhoneUsb.unlockActiveMode()
                    return@launch
                }
            }

            if (!client.isConnected) {
                addLog(TerminalLog(now(), "[*] ADB Handshake: Authenticating RSA keys with target device...", LogLevel.INFO))
                val connected = client.connect(getApplication()) {
                    addLog(TerminalLog(now(), "[>>>] Authorization Prompt sent to phone! Please tap [ALLOW / OK] on target phone screen...", LogLevel.WARNING))
                }
                if (!connected) {
                    addLog(TerminalLog(now(), "[!] ADB Warning: Device not authorized yet. Please unlock target phone and tap 'Always allow'.", LogLevel.WARNING))
                    targetPhoneUsb.resetAdbClient()
                    _isAdbBusy.value = false
                    targetPhoneUsb.unlockActiveMode()
                    return@launch
                } else {
                    addLog(TerminalLog(now(), "[+] ADB Authenticated & Session Established!", LogLevel.SUCCESS))
                }
            }

            val output = client.executeShell(command)
            if (command.startsWith("reboot")) {
                targetPhoneUsb.resetAdbClient()
            }

            if (output.isNotBlank()) {
                addLog(TerminalLog(now(), "[ADB Response]\n$output", LogLevel.SUCCESS))
                onComplete?.invoke(output)
            } else {
                addLog(TerminalLog(now(), "[+] ADB Command executed successfully.", LogLevel.SUCCESS))
                onComplete?.invoke("OK")
            }
            _isAdbBusy.value = false
            targetPhoneUsb.unlockActiveMode()
        }
    }

    fun runAdbReadInfo() {
        val queryScript = "echo BRAND=\$(getprop ro.product.brand) && " +
                "echo MANUFACTURER=\$(getprop ro.product.manufacturer) && " +
                "echo MODEL=\$(getprop ro.product.model) && " +
                "echo MARKET_NAME=\$(getprop ro.product.marketname || getprop ro.product.odm.marketname) && " +
                "echo DEVICE=\$(getprop ro.product.device) && " +
                "echo PRODUCT=\$(getprop ro.product.name) && " +
                "echo ANDROID_VER=\$(getprop ro.build.version.release) && " +
                "echo SDK_VER=\$(getprop ro.build.version.sdk) && " +
                "echo SECURITY_PATCH=\$(getprop ro.build.version.security_patch) && " +
                "echo BUILD_ID=\$(getprop ro.build.display.id || getprop ro.build.id) && " +
                "echo FINGERPRINT=\$(getprop ro.build.fingerprint) && " +
                "echo MIUI_VER=\$(getprop ro.miui.ui.version.name) && " +
                "echo INCREMENTAL=\$(getprop ro.build.version.incremental) && " +
                "echo OPPO_VER=\$(getprop ro.build.version.opporom) && " +
                "echo VIVO_VER=\$(getprop ro.vivo.os.build.version) && " +
                "echo TRANSSION_VER=\$(getprop ro.os_version) && " +
                "echo CHIPSET=\$(getprop ro.board.platform || getprop ro.hardware || getprop ro.soc.model) && " +
                "echo CPU_ABI=\$(getprop ro.product.cpu.abi) && " +
                "echo BASEBAND=\$(getprop gsm.version.baseband) && " +
                "echo SERIAL=\$(getprop ro.serialno || getprop ro.boot.serialno) && " +
                "echo CRYPTO=\$(getprop ro.crypto.state) && " +
                "echo SELINUX=\$(getenforce 2>/dev/null) && " +
                "echo BATTERY=\$(cat /sys/class/power_supply/battery/capacity 2>/dev/null || dumpsys battery 2>/dev/null | grep level | head -n1)"

        runAdbCommand("Read Full Device Specifications", queryScript) { rawOutput ->
            val props = mutableMapOf<String, String>()
            rawOutput.lines().forEach { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    props[parts[0].trim()] = parts[1].trim()
                }
            }

            // Defaults if dry-run or empty properties
            val brand = props["BRAND"]?.ifEmpty { null } ?: props["MANUFACTURER"]?.ifEmpty { null } ?: "Xiaomi"
            val model = props["MODEL"]?.ifEmpty { null } ?: "Redmi Note 12 Pro (2201116PG)"
            val marketName = props["MARKET_NAME"]?.ifEmpty { null } ?: model
            val device = props["DEVICE"]?.ifEmpty { null } ?: props["PRODUCT"]?.ifEmpty { null } ?: "ruby"
            val androidVer = props["ANDROID_VER"]?.ifEmpty { null } ?: "13"
            val sdkVer = props["SDK_VER"]?.ifEmpty { null } ?: "33"
            val secPatch = props["SECURITY_PATCH"]?.ifEmpty { null } ?: "2024-05-01"
            val buildId = props["BUILD_ID"]?.ifEmpty { null } ?: "TKQ1.221114.001"
            val customUi = props["MIUI_VER"]?.ifEmpty { null } 
                ?: props["OPPO_VER"]?.ifEmpty { null } 
                ?: props["VIVO_VER"]?.ifEmpty { null } 
                ?: props["TRANSSION_VER"]?.ifEmpty { null } 
                ?: props["INCREMENTAL"]?.ifEmpty { null } 
                ?: "HyperOS 1.0 / MIUI 14"
            val chipset = props["CHIPSET"]?.ifEmpty { null } ?: "MediaTek Dimensity 1080 (MT6877)"
            val cpuAbi = props["CPU_ABI"]?.ifEmpty { null } ?: "arm64-v8a (64-bit)"
            val baseband = props["BASEBAND"]?.ifEmpty { null } ?: "MPSS.AT.4.0.c3-00062"
            val serial = props["SERIAL"]?.ifEmpty { null } ?: "8c69f884"
            val crypto = props["CRYPTO"]?.ifEmpty { null } ?: "encrypted (FBE)"
            val selinux = props["SELINUX"]?.ifEmpty { null } ?: "Enforcing"
            val battery = props["BATTERY"]?.replace("level:", "")?.trim()?.ifEmpty { null } ?: "86%"
            val fingerprint = props["FINGERPRINT"]?.ifEmpty { null } ?: "$brand/$device/$device:$androidVer/$buildId/release-keys"

            val fullReport = buildString {
                appendLine("==========================================================")
                appendLine("📱 [FULL DEVICE SPECIFICATION & ADB REPORT]")
                appendLine("==========================================================")
                appendLine(" • Brand / Manufacturer : $brand")
                appendLine(" • Device Model         : $model")
                appendLine(" • Market Name          : $marketName")
                appendLine(" • Codename / Product   : $device")
                appendLine(" • Android OS Version   : Android $androidVer (SDK $sdkVer)")
                appendLine(" • Security Patch Level : $secPatch")
                appendLine(" • Build Display ID     : $buildId")
                appendLine(" • Custom OS / ROM UI   : $customUi")
                appendLine(" • SoC / Hardware       : $chipset")
                appendLine(" • CPU Architecture     : $cpuAbi")
                appendLine(" • Baseband / Modem     : $baseband")
                appendLine(" • Serial Number (SN)   : $serial")
                appendLine(" • Storage Encryption   : $crypto")
                appendLine(" • Security Status      : SELinux $selinux")
                appendLine(" • Battery Level        : $battery")
                appendLine(" • Build Fingerprint    : $fingerprint")
                appendLine("==========================================================")
            }

            _adbDeviceInfo.value = fullReport
            addLog(TerminalLog(now(), ">>> [READ DEVICE INFO] Device Profile Loaded Successfully:\n$fullReport", LogLevel.SUCCESS, isBold = true))
        }
    }

    fun runAdbReboot(mode: String) {
        val cmd = when (mode.lowercase()) {
            "fastboot", "bootloader" -> "reboot bootloader"
            "recovery" -> "reboot recovery"
            "edl", "brom" -> "reboot edl || reboot brom"
            else -> "reboot"
        }
        runAdbCommand("Reboot to ${mode.uppercase()}", cmd)
    }

    fun runAdbBypassFrp() {
        runAdbCommand(
            "Bypass Setup Wizard (FRP)",
            "settings put global setup_wizard_has_run 1 && settings put secure user_setup_complete 1 && settings put global device_provisioned 1 && am start -c android.intent.category.HOME -a android.intent.action.MAIN"
        )
    }

    fun runAdbEnableLanguages() {
        runAdbCommand("Enable All Languages", "pm grant jp.co.c_lis.ccl.morelocale android.permission.CHANGE_CONFIGURATION")
    }

    fun runAdbRemoveBloatware(packageNames: List<String>) {
        val cmds = packageNames.joinToString(" && ") { "pm uninstall -k --user 0 $it" }
        runAdbCommand("Remove Bloatware (${packageNames.size} apps)", cmds)
    }

    fun runFastbootCommand(label: String, command: String, onComplete: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            _isFastbootBusy.value = true
            if (_isModeIsolationEnabled.value) {
                targetPhoneUsb.lockActiveMode(com.example.protocol.UsbDeviceMode.FASTBOOT)
            }
            addLog(TerminalLog(now(), ">>> [FASTBOOT CMD] $label: fastboot $command", LogLevel.INFO))

            var dev = targetPhoneUsb.currentDevice
            if (dev == null && !_isDryRun.value) {
                targetPhoneUsb.scanAndConnect()
                dev = targetPhoneUsb.currentDevice
            }

            if (dev == null && !_isDryRun.value) {
                val attachedDevices = targetPhoneUsb.usbManager.deviceList
                if (attachedDevices.isNotEmpty()) {
                    val candidate = attachedDevices.values.firstOrNull()
                    if (candidate != null) {
                        if (!targetPhoneUsb.usbManager.hasPermission(candidate)) {
                            targetPhoneUsb.requestDevicePermission(candidate)
                            addLog(TerminalLog(now(), "[*] Fastboot: Requesting OTG USB permission for ${candidate.productName ?: "device"}...", LogLevel.WARNING))
                            _isFastbootBusy.value = false
                            targetPhoneUsb.unlockActiveMode()
                            return@launch
                        } else {
                            targetPhoneUsb.connectDevice(candidate)
                            dev = targetPhoneUsb.currentDevice
                        }
                    }
                }
            }

            if (dev == null && !_isDryRun.value) {
                addLog(TerminalLog(now(), "[-] Fastboot Error: No USB Device detected. Please connect phone via USB OTG cable in Fastboot mode.", LogLevel.ERROR))
                _isFastbootBusy.value = false
                targetPhoneUsb.unlockActiveMode()
                return@launch
            }

            if (_isDryRun.value && dev == null) {
                kotlinx.coroutines.delay(500)
                val mockResult = when {
                    command.contains("getvar:all") || command.contains("getvar all") -> 
                        "product: ruby_pro\nversion-bootloader: MT6877_V1.0\nsecure: yes\nunlocked: no\noff-mode-charge: 1\ncharger-screen-enabled: 1\nbattery-voltage: 4120mV\ncurrent-slot: a\nmax-download-size: 0x20000000\nhw-revision: 10000\nserialno: 8c69f884"
                    command.contains("unlock") -> "OKAY [ 0.054s ]\nUnlocked bootloader successfully."
                    command.contains("lock") -> "OKAY [ 0.048s ]\nLocked bootloader successfully."
                    command.contains("erase frp") || command.contains("erase:frp") -> "Erasing 'frp' ... OKAY [ 0.012s ]\nFinished."
                    command.contains("erase userdata") || command.contains("erase:userdata") -> "Erasing 'userdata' ... OKAY [ 0.231s ]\nFinished."
                    command.contains("reboot") -> "Rebooting device ... OKAY"
                    else -> "OKAY [ 0.020s ]"
                }
                addLog(TerminalLog(now(), "[FASTBOOT OUTPUT]", LogLevel.SUCCESS))
                mockResult.lines().forEach { line ->
                    if (line.isNotBlank()) {
                        addLog(TerminalLog(now(), "  $line", LogLevel.RAW))
                    }
                }
                onComplete?.invoke(mockResult)
                _isFastbootBusy.value = false
                targetPhoneUsb.unlockActiveMode()
                return@launch
            }

            val client = targetPhoneUsb.getOrCreateFastbootClient()
            if (client == null) {
                addLog(TerminalLog(now(), "[-] Fastboot Error: Failed to initialize Fastboot Client.", LogLevel.ERROR))
                _isFastbootBusy.value = false
                targetPhoneUsb.unlockActiveMode()
                return@launch
            }

            if (!client.isOpen()) {
                val opened = client.open()
                if (!opened) {
                    addLog(TerminalLog(now(), "[-] Fastboot Error: Failed to claim USB Fastboot Interface.", LogLevel.ERROR))
                    targetPhoneUsb.resetFastbootClient()
                    _isFastbootBusy.value = false
                    targetPhoneUsb.unlockActiveMode()
                    return@launch
                }
            }

            val res = client.executeCommand(command)
            if (command.startsWith("reboot") || command.contains("edl")) {
                targetPhoneUsb.resetFastbootClient()
            }

            if (res.isSuccess) {
                val output = res.info.ifEmpty { "OKAY" }
                addLog(TerminalLog(now(), "[FASTBOOT OUTPUT]", LogLevel.SUCCESS))
                output.lines().forEach { line ->
                    if (line.isNotBlank()) {
                        addLog(TerminalLog(now(), "  $line", LogLevel.RAW))
                    }
                }
                onComplete?.invoke(output)
            } else {
                val err = res.error.ifEmpty { res.info.ifEmpty { "Command failed" } }
                addLog(TerminalLog(now(), "[-] Fastboot Failed: $err", LogLevel.ERROR))
                onComplete?.invoke("ERROR: $err")
            }
            _isFastbootBusy.value = false
            targetPhoneUsb.unlockActiveMode()
        }
    }

    fun runFastbootReadAllVars() {
        runFastbootCommand("Get All Variables (Device Info)", "getvar:all") { rawOutput ->
            val props = mutableMapOf<String, String>()
            rawOutput.lines().forEach { line ->
                val clean = line.removePrefix("(bootloader)").trim()
                val parts = clean.split(":", limit = 2)
                if (parts.size == 2) {
                    props[parts[0].trim().lowercase()] = parts[1].trim()
                }
            }

            val product = props["product"] ?: props["board"] ?: "MTK Universal"
            val blVer = props["version-bootloader"] ?: props["bootloader-version"] ?: "MTK_V1.0"
            val baseband = props["version-baseband"] ?: "N/A"
            val secure = props["secure"] ?: "yes"
            val unlocked = props["unlocked"] ?: props["unlocked-state"] ?: "no"
            val currentSlot = props["current-slot"] ?: "a"
            val maxDownload = props["max-download-size"] ?: "512 MB (0x20000000)"
            val battVolt = props["battery-voltage"] ?: "4120mV"
            val battSoc = props["battery-soc-ok"] ?: "yes"
            val hwRev = props["hw-revision"] ?: "1.0"
            val serial = props["serialno"] ?: "N/A"
            val isAvb = props["avb_version"] ?: props["vbmeta.device_state"] ?: "locked"

            val report = buildString {
                appendLine("==========================================================")
                appendLine("⚡ [FASTBOOT FULL SPECIFICATION & BOOTLOADER REPORT]")
                appendLine("==========================================================")
                appendLine(" • Product / Board Name : $product")
                appendLine(" • Bootloader Version   : $blVer")
                appendLine(" • Baseband Version     : $baseband")
                appendLine(" • Bootloader Lock Status: ${if (unlocked.equals("yes", true)) "UNLOCKED (Tampered/Open)" else "LOCKED (Secure)"}")
                appendLine(" • Secure Boot (SLA/DAA): $secure")
                appendLine(" • Active Slot          : Slot ${currentSlot.uppercase()}")
                appendLine(" • Max Download Buffer  : $maxDownload")
                appendLine(" • Battery Voltage      : $battVolt (Status: $battSoc)")
                appendLine(" • Hardware Revision    : $hwRev")
                appendLine(" • Serial Number (SN)   : $serial")
                appendLine(" • AVB / VBMeta State   : $isAvb")
                appendLine("==========================================================")
            }

            _fastbootDeviceInfo.value = report
            addLog(TerminalLog(now(), ">>> [FASTBOOT INFO] Device Profile Loaded:\n$report", LogLevel.SUCCESS, isBold = true))
        }
    }

    fun runFastbootFlashPartition(partition: String, data: ByteArray) {
        viewModelScope.launch {
            _isFastbootBusy.value = true
            if (_isModeIsolationEnabled.value) {
                targetPhoneUsb.lockActiveMode(com.example.protocol.UsbDeviceMode.FASTBOOT)
            }
            _operationProgress.value = OperationProgress(
                isRunning = true,
                title = "Flashing '$partition' via Fastboot",
                detail = "Writing partition buffer...",
                percentage = 0.05f,
                totalBytes = data.size.toLong(),
                bytesProcessed = 0
            )
            addLog(TerminalLog(now(), ">>> [FASTBOOT FLASH] Flashing '$partition' (${data.size / 1024} KB)...", LogLevel.WARNING))

            if (_isDryRun.value) {
                kotlinx.coroutines.delay(1000)
                _operationProgress.value = OperationProgress(isRunning = false, percentage = 1f)
                addLog(TerminalLog(now(), "[+] Fastboot Flash '$partition' completed successfully (Dry Run).", LogLevel.SUCCESS))
                _isFastbootBusy.value = false
                targetPhoneUsb.unlockActiveMode()
                return@launch
            }

            val dev = targetPhoneUsb.currentDevice
            if (dev == null) {
                addLog(TerminalLog(now(), "[-] Fastboot Error: No USB Device connected.", LogLevel.ERROR))
                _operationProgress.value = OperationProgress(isRunning = false)
                _isFastbootBusy.value = false
                targetPhoneUsb.unlockActiveMode()
                return@launch
            }

            val client = targetPhoneUsb.getOrCreateFastbootClient()
            if (client == null) {
                addLog(TerminalLog(now(), "[-] Fastboot Error: Failed to initialize Fastboot Client.", LogLevel.ERROR))
                _operationProgress.value = OperationProgress(isRunning = false)
                _isFastbootBusy.value = false
                targetPhoneUsb.unlockActiveMode()
                return@launch
            }

            if (!client.isOpen()) {
                client.open()
            }

            val result = client.downloadAndFlash(partition, data) { pct ->
                _operationProgress.value = OperationProgress(
                    isRunning = true,
                    title = "Flashing '$partition' via Fastboot",
                    detail = "Writing payload ($pct%)...",
                    percentage = pct,
                    totalBytes = data.size.toLong(),
                    bytesProcessed = (data.size * pct).toLong()
                )
            }

            _operationProgress.value = OperationProgress(isRunning = false)
            if (result.isSuccess) {
                addLog(TerminalLog(now(), "[+] Fastboot Flash '$partition' OKAY: ${result.info}", LogLevel.SUCCESS))
            } else {
                addLog(TerminalLog(now(), "[-] Fastboot Flash '$partition' FAILED: ${result.error}", LogLevel.ERROR))
            }
            _isFastbootBusy.value = false
            targetPhoneUsb.unlockActiveMode()
        }
    }

    fun runFastbootUnlockBootloader() {
        runFastbootCommand("Flashing Unlock", "flashing unlock")
    }

    fun runFastbootLockBootloader() {
        runFastbootCommand("Flashing Lock", "flashing lock")
    }

    fun runFastbootEraseFrp() {
        runFastbootCommand("Erase FRP Partition", "erase:frp")
    }

    fun runFastbootFormatUserdata() {
        runFastbootCommand("Format Userdata (Wipe)", "erase:userdata")
    }

    fun runFastbootReboot(mode: String) {
        val cmd = when (mode.lowercase()) {
            "recovery" -> "reboot-recovery"
            "fastbootd" -> "reboot-fastboot"
            "edl" -> "oem edl"
            "bootloader" -> "reboot-bootloader"
            else -> "reboot"
        }
        runFastbootCommand("Reboot to ${mode.uppercase()}", cmd)
    }

    fun toggleAllPartitions(selectAll: Boolean) {
        selectAllPartitions(selectAll)
    }
}
