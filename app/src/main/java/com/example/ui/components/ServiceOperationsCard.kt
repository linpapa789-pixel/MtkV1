package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OperationProgress
import com.example.model.PartitionEntry
import com.example.model.ServiceFunction
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.viewmodel.MtkBridgeViewModel

@Composable
fun ServiceOperationsCard(
    viewModel: MtkBridgeViewModel,
    modifier: Modifier = Modifier
) {
    val selectedFunction by viewModel.selectedServiceFunction.collectAsState()
    val progress by viewModel.operationProgress.collectAsState()
    val partitions by viewModel.partitions.collectAsState()
    val selectedPartIndex by viewModel.selectedPartitionIndex.collectAsState()
    val selectedPartition = partitions.getOrNull(selectedPartIndex)
    val autoNvBackup by viewModel.autoNvBackup.collectAsState()

    var showWriteConfirmationDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Service Operations",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "4. Service Operations",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Function Selector Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FunctionSelectTile(
                        title = "Read Info",
                        icon = Icons.Default.Fingerprint,
                        isSelected = selectedFunction == ServiceFunction.READ_INFO,
                        onClick = { viewModel.selectServiceFunction(ServiceFunction.READ_INFO) },
                        modifier = Modifier.weight(1f)
                    )
                    FunctionSelectTile(
                        title = "Read Partition",
                        icon = Icons.Default.Download,
                        isSelected = selectedFunction == ServiceFunction.READ_PARTITION,
                        onClick = { viewModel.selectServiceFunction(ServiceFunction.READ_PARTITION) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FunctionSelectTile(
                        title = "Backup NVRAM",
                        icon = Icons.Default.Security,
                        isSelected = selectedFunction == ServiceFunction.BACKUP_NVRAM,
                        onClick = { viewModel.selectServiceFunction(ServiceFunction.BACKUP_NVRAM) },
                        modifier = Modifier.weight(1f)
                    )
                    FunctionSelectTile(
                        title = "Flash Partition",
                        icon = Icons.Default.Upload,
                        isSelected = selectedFunction == ServiceFunction.WRITE_PARTITION,
                        isDestructive = true,
                        onClick = { viewModel.selectServiceFunction(ServiceFunction.WRITE_PARTITION) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FunctionSelectTile(
                        title = "Restore NVRAM",
                        icon = Icons.Default.Restore,
                        isSelected = selectedFunction == ServiceFunction.RESTORE_NVRAM,
                        isDestructive = true,
                        onClick = { viewModel.selectServiceFunction(ServiceFunction.RESTORE_NVRAM) },
                        modifier = Modifier.weight(1f)
                    )
                    FunctionSelectTile(
                        title = "Format Partition",
                        icon = Icons.Default.DeleteForever,
                        isSelected = selectedFunction == ServiceFunction.FORMAT_PARTITION,
                        isDestructive = true,
                        onClick = { viewModel.selectServiceFunction(ServiceFunction.FORMAT_PARTITION) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Target Context Preview
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Selected Operation: ${selectedFunction.title}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = selectedFunction.subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedPartition != null && (selectedFunction == ServiceFunction.READ_PARTITION || selectedFunction == ServiceFunction.WRITE_PARTITION || selectedFunction == ServiceFunction.FORMAT_PARTITION)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Target: [${selectedPartition.partitionIndex}] ${selectedPartition.partitionName} (${selectedPartition.partitionSizeHex})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Progress Bar if running
            if (progress.isRunning) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = progress.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${String.format("%.1f", progress.percentage)}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress.percentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = progress.detail,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Auto NV Data Backup Checkbox Option
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (autoNvBackup) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (autoNvBackup) Color(0xFFBFDBFE) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleAutoNvBackup(!autoNvBackup) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Checkbox(
                        checked = autoNvBackup,
                        onCheckedChange = { viewModel.toggleAutoNvBackup(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF2563EB),
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Auto NV Data Backup",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (autoNvBackup) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (autoNvBackup) Color(0xFFDBEAFE) else Color(0xFFE2E8F0)
                            ) {
                                Text(
                                    text = if (autoNvBackup) "IMEI Guard ON" else "OFF",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (autoNvBackup) Color(0xFF1E40AF) else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = if (autoNvBackup) "Auto-dumps nvram, nvdata & builds scatter to storage" else "Skip NV backup (Direct flash without pre-backup)",
                            fontSize = 9.sp,
                            color = if (autoNvBackup) Color(0xFF3B82F6) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Execute Button
            Button(
                onClick = {
                    if (selectedFunction.isWrite) {
                        showWriteConfirmationDialog = true
                    } else {
                        viewModel.executeActiveServiceFunction()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !progress.isRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedFunction.isWrite) Color(0xFFE11D48) else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (progress.isRunning) "Operation in Progress..." else "Execute ${selectedFunction.title}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }

    // Write safety confirmation dialog
    if (showWriteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showWriteConfirmationDialog = false },
            title = { Text("Confirm Write / Flash Operation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "You are about to execute a write operation: '${selectedFunction.title}' on partition '${selectedPartition?.partitionName ?: "Selected"}'.",
                        fontSize = 13.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("🛡️ Safety Policy:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                if (autoNvBackup) "1. Auto NV Data Backup: ENABLED (nvram/nvdata dump will run first)." else "1. Auto NV Data Backup: DISABLED (Skipping NV backup).",
                                fontSize = 11.sp,
                                color = if (autoNvBackup) Color(0xFF15803D) else Color(0xFFD97706)
                            )
                            Text("2. Post-write SHA-256 read-back checksum verification.", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWriteConfirmationDialog = false
                        viewModel.executeActiveServiceFunction()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("Proceed with Write")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWriteConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FunctionSelectTile(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    Surface(
        modifier = modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) {
            if (isDestructive) Color(0xFFFFF1F2) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) {
                if (isDestructive) Color(0xFFE11D48) else MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) {
                    if (isDestructive) Color(0xFFE11D48) else MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected && isDestructive) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
