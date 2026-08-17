package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppNavDestination(
    val title: String,
    val shortTitle: String,
    val subtitle: String,
    val icon: ImageVector
) {
    FLASH(
        title = "⚡ Flash Tool",
        shortTitle = "Flash",
        subtitle = "Firmware Flashing Engine & Scatter Loader",
        icon = Icons.Default.FlashOn
    ),
    BACKUP(
        title = "💾 Backup / Read",
        shortTitle = "Backup",
        subtitle = "Full ROM, Stable FW, NV & Custom Dump",
        icon = Icons.Default.FolderOpen
    ),
    SERVICE(
        title = "🔓 BROM Service",
        shortTitle = "BROM",
        subtitle = "One-Click FRP, Factory Reset & BL Unlock",
        icon = Icons.Default.LockOpen
    ),
    FASTBOOT(
        title = "🟡 Fastboot",
        shortTitle = "Fastboot",
        subtitle = "Getvar, BL Unlock, TWRP/Boot Flash & Erase FRP",
        icon = Icons.Default.FlashOn
    ),
    ADB(
        title = "🔷 ADB Mode",
        shortTitle = "ADB",
        subtitle = "Device Info, Reboot to BROM/FB & Bypass Tools",
        icon = Icons.Default.FolderOpen
    ),
    OTHER(
        title = "⚙️ Other & Tools",
        shortTitle = "Other",
        subtitle = "Memory Test, Auth Bypass & Gemini AI",
        icon = Icons.Default.AutoAwesome
    )
}

